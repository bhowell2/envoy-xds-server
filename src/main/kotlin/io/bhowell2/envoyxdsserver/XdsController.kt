package io.bhowell2.envoyxdsserver

import com.google.common.collect.ImmutableMap
import io.bhowell2.envoyxdsserver.api.UpdateApi
import io.bhowell2.envoyxdsserver.api.UpdateServicesApi
import io.bhowell2.envoyxdsserver.exceptions.BadRequestException
import io.envoyproxy.controlplane.cache.SimpleCache
import io.envoyproxy.controlplane.cache.Snapshot
import io.envoyproxy.controlplane.server.DiscoveryServer
import io.envoyproxy.envoy.api.v2.core.Node
import io.grpc.Server
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyServerBuilder
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.SslContextBuilder
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


/**
 * It was decided to make the XdsController dependent on Vertx to conserve development time. Vertx provides a convenient
 * JsonObject class as well as clustering/high availability capabilities.
 *
 * TODO: implement clustering/HA leveraging vertx
 *
 * @author Blake Howell
 */
class XdsController private constructor(val vertx: Vertx,
                                        val defaultSnapshotGroupName: String,
                                        val xdsServerListenAddress: String,
                                        val xdsServerPort: Int,
                                        val groupRetrievalFn: (Node) -> String = DEFAULT_GROUP_RETRIEVAL_FN) {

  companion object {
    const val DEFAULT_SNAPSHOT_GROUP_NAME = "__DEFAULT_CLUSTER_GROUP__"
    const val DEFAULT_XDS_SERVER_PORT = 10011
    const val DEFAULT_XDS_SERVER_LISTEN_ADDRESS = "0.0.0.0"
    val DEFAULT_GROUP_RETRIEVAL_FN: (Node) -> String = {
      it.cluster ?: DEFAULT_SNAPSHOT_GROUP_NAME
    }

    // must use volatile to avoid errors
    @Volatile private var INSTANCE: XdsController? = null

    /**
     * Call when the singleton has already been built and/or when it should be created with default values.
     */
    fun getInstance(vertx: Vertx) = getInstance(vertx,
                                                DEFAULT_SNAPSHOT_GROUP_NAME,
                                                DEFAULT_XDS_SERVER_LISTEN_ADDRESS,
                                                DEFAULT_XDS_SERVER_PORT)


    fun getInstance(vertx: Vertx, defaultSnapshotGroupName: String, xdsServerListenAddress: String, xdsServerPort: Int,
                    cacheRetrieval: (Node) -> String = { it.cluster ?: defaultSnapshotGroupName}) =
        INSTANCE ?:
        synchronized(this) {
          INSTANCE ?: XdsController(vertx, defaultSnapshotGroupName, xdsServerListenAddress,
                                    xdsServerPort, cacheRetrieval).also { INSTANCE = it }
        }

    /**
     * Used for testing so that the INSTANCE reference can be dropped so that each test can start fresh.
     * Could create an "internal" interface and all that, but kinda overkill since currently this is the only method
     * that needs to be used internally - just give method a name that no sane user would call in their app.
     */
    fun TESTING_DROP_INSTANCE_FOR_TESTING() {
      INSTANCE = null
    }

  }

  // simple FIFO unbounded non-prioritized queue
  private val queue = LinkedBlockingQueue<Runnable>()
  // wont actually use keepAliveTime since maxPoolSize is same as corePoolSize
  private val executor = ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, queue)

  // the cluster
  private val cache = SimpleCache(groupRetrievalFn)

  val discoveryServer = DiscoveryServer(this.cache)
  val grpcServer: Server

  // maps the group's name to the group's services. used to retrieve and update services for a particular group
  // note: NodeGroupServices.groupName is the key of the map.
  private var nodeGroupServices = ImmutableMap.of<String, NodeGroupServices>()

  // startup the server
  init {
    val serverPort = System.getProperty(XDS_GRPC_SERVER_PORT)?.toInt() ?: DEFAULT_XDS_SERVER_PORT
    val serverAddr = System.getProperty(XDS_GRPC_SERVER_ADDRESS) ?: DEFAULT_XDS_SERVER_LISTEN_ADDRESS
    val authorityPath = System.getProperty(XDS_GRPC_SERVER_CLIENT_CA)
    val certificatePath = System.getProperty(XDS_GRPC_SERVER_CERT)
    val certPrivKeyPath = System.getProperty(XDS_GRPC_SERVER_CERT_KEY)

    // if certs are provided then setup TLS for GRPC server
    var sslContextBuilder: SslContextBuilder? = null
    if (certificatePath != null && certPrivKeyPath != null) {
      sslContextBuilder = GrpcSslContexts.forServer(File(certificatePath), File(certPrivKeyPath))
    }

    // if authority is provided it will be used to verify client for mTLS
    if (authorityPath != null) {
      if (sslContextBuilder == null) {
        error("Cannot set client certificate authority (system property: $XDS_GRPC_SERVER_CLIENT_CA) and not set " +
              "system properties: $XDS_GRPC_SERVER_CERT and $XDS_GRPC_SERVER_CERT_KEY")
      }
      sslContextBuilder.trustManager(File(authorityPath))
          .clientAuth(ClientAuth.REQUIRE)
    }


    val serverBuilder = NettyServerBuilder.forAddress(InetSocketAddress(serverAddr, serverPort))
        .addService(this.discoveryServer.aggregatedDiscoveryServiceImpl)
        .addService(this.discoveryServer.clusterDiscoveryServiceImpl)
        .addService(this.discoveryServer.endpointDiscoveryServiceImpl)
        .addService(this.discoveryServer.listenerDiscoveryServiceImpl)
        .addService(this.discoveryServer.routeDiscoveryServiceImpl)
        .addService(this.discoveryServer.secretDiscoveryServiceImpl)

    if (sslContextBuilder != null) {
      serverBuilder.sslContext(sslContextBuilder.build())
    }

    grpcServer = serverBuilder.build()

    // should not, by default, await termination or would block vertx thread
    // that calls this (if using the default vertx http server)
    grpcServer.start()

    Runtime.getRuntime().addShutdownHook(Thread {
      grpcServer.shutdown()
    })

  }

  /**
   * Call this if you need to block the calling thread from completing.
   * (e.g., running this from main method without a loop)
   */
  fun awaitTermination() {
    grpcServer.awaitTermination()
  }

  /**
   * Call when it is desired to shutdown the GRPC server.
   */
  fun shutdown() {
    this.grpcServer.shutdown()
  }

  /**
   * Returns how many operations are in the update queue.
   * This, along with [getQueueLatency], can be used to gather some metrics for the XdsController.
   */
  fun getQueueSize(): Int {
    return queue.size
  }

  /**
   * Returns the time it took for the update operation to complete. Useful for gathering metrics.
   */
  fun getQueueLatency(completionHandler: (latencyMillis: Long, err: Exception?) -> Unit) {
    val startTime = System.currentTimeMillis()
    update(JsonObject()) { err ->
      val returnTime = System.currentTimeMillis()
      completionHandler(returnTime - startTime, err)
    }
  }

  fun generateRespForGroup(group: String, snapshot: Snapshot, includeDefauts: Boolean) {
    snapshot.clusters()
  }

  /**
   * Returns the state in JSON format as a String.
   * State is of form:
   * {
   *  "group_name": {
   *    "cds": [{...}],
   *    "eds": [{...}],
   *    ...
   *  }
   * }
   */
  fun getState(groups: List<String>?, resources: List<String>?, includeDefaults: Boolean = false): JsonObject {
    val responseBuilder = JsonObject()
    if (groups.isNullOrEmpty()) {
      // return everything
      for (entry in nodeGroupServices) {
        responseBuilder.put(entry.value.groupName, entry.value.getNodeGroupResponseJson(resources, includeDefaults))
      }
    } else {
      for (groupName in groups) {
        val nodeGroup = nodeGroupServices[groupName]
        if (nodeGroup != null) {
          responseBuilder.put(nodeGroup.groupName, nodeGroup.getNodeGroupResponseJson(resources, includeDefaults))
        }
      }
    }
    return responseBuilder
  }

  /**
   * Returns a JsonObject that can be posted to the server that will recreate all services and their resources
   * at their state when this was called.
   * The API to update a resource looks like:
   * {
   *  update_services: [
   *    {
   *      groups: ["group1", ... "groupN"],
   *      cds: {},
   *      eds: {},
   *      lds: {},
   *      rds: {},
   *      sds: {}
   *    },
   *    ...
   *    {
   *      groups: ["group2"],
   *      ...
   *    }
   *  ],
   * }
   */
  fun getResetState(): JsonObject {
    val updateServices = JsonArray()
    // map of each group's services. the key is the group's name
    for (entry in nodeGroupServices) {
      val groupServices = entry.value.getResetNodeGroupState()
      val updateServiceObj = JsonObject()
      /*
      * Create JsonObject for each position in the update services array. This must specify the groups
      * (even though it will only be one here - the current group being retrieved) and then each service's
      * resources to add.
      */
      updateServiceObj.put(UpdateServicesApi.GROUPS, JsonArray().add(entry.key))
      for (serviceEntry in groupServices) {
        updateServiceObj.put(serviceEntry.key, serviceEntry.value)
      }
      updateServices.add(updateServiceObj)
    }
    return JsonObject().put(UpdateApi.UPDATE_SERVICES, updateServices)
  }

  /**
   * Provides common parsing functionality for the remove_groups request parameter.
   * The removeOp parameter is provided with the name of the group.
   */
  private fun commonRemoveGroupParsing(updateRequest: JsonObject, removeOp: (groupName: String) -> Unit) {
    val removeGroups = updateRequest.getValue(UpdateApi.REMOVE_GROUPS)
    if (removeGroups != null) {
      if (removeGroups is JsonArray) {
        for (groupName in removeGroups) {
          if (groupName is String) {
            removeOp(groupName)
          } else {
            throw BadRequestException("${UpdateApi.REMOVE_GROUPS} was provided, but the values of the JsonArray were not Strings. See API docs.")
          }
        }
      } else {
        // may be just a string, check and allow if so. otherwise this is a bad request so throw error
        if (removeGroups is String) {
          removeOp(removeGroups)
        } else {
          throw BadRequestException("${UpdateApi.REMOVE_GROUPS} was provided but was not a JsonArray or String. See API docs.")
        }
      }
    }
  }

  /**
   * This should be run before updating groups' resources. This will ensure that the group's resources are clear
   * and any updates to the group later are fresh and not merged with the previous state.
   *
   * Note: this only removes from the tmp resources map, but does not clear the group from the cache. The cache
   * should not be cleared here because it will interfere with the transactional nature of the resources - i.e.,
   * if the resource is cleared from the cache it cannot be rolled back in the cause of a failure later on in
   * the update request. The snapshot can be cleared from the cache AFTER the other groups have been updated, but
   * before they have actually completed the update transaction.
   */
  @Throws(Exception::class)
  private fun removeGroups(updateRequest: JsonObject,
                           transactionServices: MutableMap<String, NodeGroupServices>) {
    commonRemoveGroupParsing(updateRequest) { groupName ->
      if (groupName == "*") {
        // remove all groups
        transactionServices.clear()
      } else {
        transactionServices.remove(groupName)
      }
    }
  }

  /**
   * Removes the group's snapshot from the cache.
   */
  private fun completeRemoveGroups(updateRequest: JsonObject) {
    commonRemoveGroupParsing(updateRequest) { groupName ->
      if (groupName == "*") {
        // go through cache and remove all
        this.cache.groups().forEach {
          this.cache.clearSnapshot(it)
        }
      } else {
        this.cache.clearSnapshot(groupName)
      }
    }
  }

  /**
   * Parses the update request for the groups to be updated and calls updateOp with each group and the request as well
   * as the snapshot version provided. In the case that all groups are to be updated, this uses the temporary group
   * resources map, because this is the map that should have already had groups removed if specified and it does not
   * make sense to apply an "all" operation to a group that should have been removed (so it is not recreated).
   */
  private fun commonUpdateServiceGroupsParsing(updateService: JsonObject, transactionServices: MutableMap<String, NodeGroupServices>,
                                               updateOp: (groupName: String) -> Unit) {
    val updateGroups = updateService.getValue(UpdateServicesApi.GROUPS)
    if (updateGroups != null) {
      if (updateGroups is JsonArray) {
        for (groupName in updateGroups) {
          if (groupName is String) {
            updateOp(groupName)
          } else {
            throw BadRequestException("${UpdateServicesApi.GROUPS} was provided, but the values of the JsonArray were not Strings. See API docs.")
          }
        }
      } else {
        // check if it is a single string
        if (updateGroups is String) {
          if (updateGroups.trim() == "*") {
            transactionServices.forEach {
              updateOp(it.key)
            }
          } else {
            // update just the one
            updateOp(updateGroups)
          }
        } else {
          throw BadRequestException("${UpdateServicesApi.GROUPS} was provided, but was not a JsonArray or String. See API docs.")
        }
      }
    }
  }

  /**
   * Attempts to update the resources of each group's services that were provided in the update request.
   */
  private fun beginUpdateGroupServices(updateRequest: JsonObject, updateSnapshotVersion: String,
                                       transactionServices: MutableMap<String, NodeGroupServices>) {
    val updateServices = updateRequest.getJsonArray(UpdateApi.UPDATE_SERVICES)
    if (updateServices == null || updateServices.size() == 0) {
      return
    }
    for (i in 0 until updateServices.size()) {
      val updateService = updateServices.getJsonObject(i)
      commonUpdateServiceGroupsParsing(updateService, transactionServices) { groupName ->
        var groupResources = transactionServices[groupName]
        // create group if it does not exist, otherwise make copy
        groupResources = when(groupResources) {
          null -> NodeGroupServices(groupName)
          else -> NodeGroupServices(groupResources)
        }
        groupResources.updateServices(updateService, updateSnapshotVersion)
        transactionServices[groupResources.groupName] = groupResources
      }
    }
  }

  /**
   * When all groups have been successfully removed and/or updated then can fully complete the transaction and
   * set the resources to the transaction resources (i.e., the resources as they should be after the transaction).
   */
  private fun completeUpdateServices(updateRequest: JsonObject, transactionServices: MutableMap<String, NodeGroupServices>) {
    // if successfully completed every node-group has been update
    this.nodeGroupServices = ImmutableMap.copyOf(transactionServices)
    for (entry in this.nodeGroupServices) {
      this.cache.setSnapshot(entry.value.groupName, entry.value.groupSnapshot)
    }
  }

  /**
   * Updates the XDS resources by creating or merging the updateString with the appropriate resources. This is an
   * ordered, non-blocking, implementation. Each update is run in the order it is received -- generally this should
   * not be a problem since the resources are merged and created when necessary.
   *
   * It is possible to submit an empty JSON string to retrieve the queue latency (i.e., how long it takes a request to
   * be processed).
   *
   * @param updateJson must be valid JSON string
   * @param updateCompleteHandler run after the update completes (e.g., respond to network requests).
   * a boolean will be returned indicating whether or not the
   * update was successful and an error will be returned if it was unsuccessful. if the callback code can throw an
   * exception it should be handled within the callback.
   *
   */
  fun update(updateRequest: JsonObject, updateCompleteHandler: (err: Exception?) -> Unit) {
    // adds execution to queue so it will be run in order and multiple operations will not be run at the same time
    executor.execute {
      try {

        /*
        * Make copy of current resources to allow for transactional operations. Copy is shallow,
        * which is no problem since services cannot be updated multiple times and must be copied.
        */
        val transactionGroupServices = mutableMapOf<String, NodeGroupServices>()
        transactionGroupServices.putAll(this.nodeGroupServices)

        /*
        * Remove group's resources if provided. note this only removes from the transaction group resources, but the snapshot
        * still needs to be cleared from the cache. However, cannot clear from cache till AFTER it has been ensured that
        * the request and updates are successful or else the transactional nature of the updates will not work.
        */
        removeGroups(updateRequest, transactionGroupServices)

        // update each group in list
        val version = System.currentTimeMillis().toString()
        beginUpdateGroupServices(updateRequest, version, transactionGroupServices)

        /*
        * All updates completed successfully, complete transactions.
        * First complete remove groups to make sure that the old snapshots are removed - must be done before
        * completing the group's updated resources or else they would be removed.
        * This allows for completely removing the group's resources and creating all new resources (for the same group)
        * in the same request.
        */
        completeRemoveGroups(updateRequest)
        completeUpdateServices(updateRequest, transactionGroupServices)

        // notify caller that update completed successfully
        updateCompleteHandler(null)
      } catch (err: Exception) {
        // disregard changes
        // roll back all changes.
        // notify caller that update failed and there was an error
        updateCompleteHandler(err)
      }
    }
  }

}