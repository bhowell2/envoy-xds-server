package io.bhowell2.envoyxdsserver

import com.google.protobuf.Message
import com.google.protobuf.MessageOrBuilder
import io.bhowell2.envoyxdsserver.api.UpdateServicesApi
import io.bhowell2.envoyxdsserver.exceptions.BadRequestException
import io.bhowell2.envoyxdsserver.services.*
import io.envoyproxy.controlplane.cache.Snapshot
import io.vertx.core.json.JsonObject

/**
 * Contains the resources for a specific node-group (or for a particular envoy instance - depends on the provided
 * groupRetrievalFn in the [XdsController]).
 * If not copied from an already existing NodeGroupResource then
 * @author Blake Howell
 */
class NodeGroupServices(val groupName: String) {

  // these will be changed if an update modifies them
  private var cds = Cds()
  private var eds = Eds()
  private var lds = Lds()
  private var rds = Rds()
  private var sds = Sds()

  // initially null, set when update completed.
  var groupSnapshot: Snapshot? = null
    private set

  /**
   * Creates copy of the NodeGroupResources provided. This allows for adjusting the values
   */
  constructor(copy: NodeGroupServices): this(copy.groupName) {
    this.cds = copy.cds
    this.eds = copy.eds
    this.lds = copy.lds
    this.rds = copy.rds
    this.sds = copy.sds
  }

  /**
   * Because the resources are
   * @param resourceUpdate
   */
  private fun <T: Message, B: MessageOrBuilder, R: ServiceBase<T,B>> updateService(resourceUpdate: JsonObject?,
                                                                                   currentService: R,
                                                                                   makeCopyOfService: (copyResource: R) -> R): R {
    if (resourceUpdate != null) {
      // resource is to be updated, must make copy of resource and then update.
      val copiedResource = makeCopyOfService(currentService)
      copiedResource.updateService(resourceUpdate)
      return copiedResource
    }
    // nothing to update for resource, return current value
    return currentService
  }

  /**
   * Takes updateServiceApi object and
   */
  fun updateServices(updateService: JsonObject, updateSnapshotVersion: String) {
    if (this.groupSnapshot != null) {
      // this ensures
      error("NodeGroupResources#updateServices should never be called twice. A copy should be made and updated.")
    }

    this.cds = updateService(updateService.getJsonObject(UpdateServicesApi.CDS), cds) { Cds(it) }
    this.eds = updateService(updateService.getJsonObject(UpdateServicesApi.EDS), eds) { Eds(it) }
    this.lds = updateService(updateService.getJsonObject(UpdateServicesApi.LDS), lds) { Lds(it) }
    this.rds = updateService(updateService.getJsonObject(UpdateServicesApi.RDS), rds) { Rds(it) }
    this.sds = updateService(updateService.getJsonObject(UpdateServicesApi.SDS), sds) { Sds(it) }

    // create snapshot for this particular node-group.
    // important to create snapshot here so that if there is an error it will be caught early
    // and abandon the transaction rather than continuing with the rest of the transaction and still having to
    // abandon it later due to some error
    this.groupSnapshot = Snapshot.create(this.cds.getSnapshotResources(),
                                         this.eds.getSnapshotResources(),
                                         this.lds.getSnapshotResources(),
                                         this.rds.getSnapshotResources(),
                                         this.sds.getSnapshotResources(),
                                         updateSnapshotVersion)
  }

  // list of all for easy iteration
  private val serviceNames = arrayOf(UpdateServicesApi.CDS, UpdateServicesApi.EDS,
                                     UpdateServicesApi.LDS, UpdateServicesApi.RDS,
                                     UpdateServicesApi.SDS)

  /**
   * Used to retrieve the appropriate service object based on the
   */
  private fun retrieveServiceByName(serviceName: String): ServiceBase<*,*> {
    return when(serviceName) {
      UpdateServicesApi.CDS -> this.cds
      UpdateServicesApi.EDS -> this.eds
      UpdateServicesApi.LDS -> this.lds
      UpdateServicesApi.RDS -> this.rds
      UpdateServicesApi.SDS -> this.sds
      else -> throw BadRequestException("Specified resource (\"$serviceName\") does not exist. " +
                                        "Only cds, eds, lds, rds, and sds are valid.")
    }
  }

  /**
   * Generates JsonObject for services for this particular node-group.
   * @param servicesToRetrieve limits the response to only include the supplied services (e.g., "eds", "sds")
   * @param includeDefaults whether or not to include the default values for a service definition.
   *
   * The returned JSON looks like:
   * {
   *    "cds": {
   *      "resource_name": {},
   *      ...
   *    },
   *    "eds": {
   *      "resource_name": {},
   *      ...
   *    },
   *    "lds": {...},
   *    "rds": {...},
   *    "sds": {...}
   * }
   */
  fun getNodeGroupResponseJson(servicesToRetrieve: List<String>?, includeDefaults: Boolean = false): JsonObject {
    val response = JsonObject()
    if (servicesToRetrieve.isNullOrEmpty()) {
      // retrieve all since no resource to retrieve were explicitly specified
      for (name in serviceNames) {
        response.put(name, retrieveServiceByName(name).getResourcesResponse(includeDefaults))
      }
    } else {
      // only retrieve resources specified in list
      for (name in servicesToRetrieve) {
        response.put(name, retrieveServiceByName(name).getResourcesResponse(includeDefaults))
      }
    }
    return response
  }

  /**
   * Returns JsonObject that can be posted to the server that will recreate this group's services
   * at the time this was called.
   * Response JSON:
   * {
   *  "cds": {
   *    "add": [{}, ... {}]
   *  },
   *  "eds": {
   *    "add": [{}, ...]
   *  },
   *  ...
   *  "sds": {...}
   * }
   */
  fun getResetNodeGroupState(): JsonObject {
    // JsonObject that contains the service name and the 'add' param for each service.
    val groupsServices = JsonObject()
    for (serviceName in serviceNames) {
      groupsServices.put(serviceName, retrieveServiceByName(serviceName).getResetServiceState())
    }
    return groupsServices
  }

}