package io.bhowell2.envoyxdsserver.services

import io.bhowell2.envoyxdsserver.api.EdsApi
import io.bhowell2.envoyxdsserver.api.UpdateApi
import io.bhowell2.envoyxdsserver.api.UpdateServicesApi
import io.bhowell2.envoyxdsserver.exceptions.InternalServerError
import io.envoyproxy.envoy.api.v2.ClusterLoadAssignment
import io.envoyproxy.envoy.api.v2.endpoint.LbEndpoint
import io.envoyproxy.envoy.api.v2.endpoint.LocalityLbEndpoints
import io.vertx.core.json.JsonObject

/**
 * EDS resources are identified by their cluster/service name, address, and port - these are joined together with ':'.
 * @author Blake Howell
 */
class Eds : ServiceBase<ClusterLoadAssignment, ClusterLoadAssignment.Builder> {

  companion object {
    const val RESOURCE_TYPE_NAME = UpdateServicesApi.EDS
    private const val REMOVE_ENDPOINT_ADDR_PORT_SEPARATOR = ":"
  }

  constructor() : super(RESOURCE_TYPE_NAME)

  constructor(copyFrom: Eds) : super(copyFrom)

  override fun getResourceBuilder(): ClusterLoadAssignment.Builder {
    return ClusterLoadAssignment.newBuilder()
  }

  override fun getResourceKeyFromBuilder(resourceBuilder: ClusterLoadAssignment.Builder): String {
    return resourceBuilder.clusterName
  }

  private val removeFullClusterLoadAssmRegex = Regex(".*!!")
  private val removeEndpointFromLoadAssmRegex = Regex("^.*:.+:.+$")


  override fun addResources(updateResourceObj: JsonObject, resourcesMap: MutableMap<String, ClusterLoadAssignment>) {
    super.addResources(updateResourceObj, resourcesMap)
    // TODO: need to implement custom add for add_endpoints API param
  }


  /**
   * Will add the endpoint to the Cluster name specified. If the cluster does not exist it will be created with
   * the default values and the name supplied here.
   */
  private fun addEndpoint(updateResourceObj: JsonObject,
                          resourcesMap: MutableMap<String, ClusterLoadAssignment>) {
    // TODO implement
    val addEndpoints = updateResourceObj.getJsonArray(EdsApi.ADD_ENDPOINTS)
    if (addEndpoints == null || addEndpoints.size() == 0) {
      return
    }
    println("Need to implement add endpoint succinct...!!!")
    throw InternalServerError("Not implemented!")
  }

  override fun removeResources(updateResourceObj: JsonObject, resourcesMap: MutableMap<String, ClusterLoadAssignment>) {
    // remove full cluster load assignments first if they are specified
    removeClusterLoadAssignments(updateResourceObj, resourcesMap)
    removeEndpoints(updateResourceObj, resourcesMap)
  }

  private fun removeClusterLoadAssignments(updateResourceObj: JsonObject,
                                           resourcesMap: MutableMap<String, ClusterLoadAssignment>) {
    val removeClas = updateResourceObj.getJsonArray(EdsApi.REMOVE)
    if (removeClas == null || removeClas.size() == 0) {
      return
    }
    for (claName in removeClas) {
      resourcesMap.remove(claName)
    }
  }


  private fun removeEndpoints(updateResourceObj: JsonObject, resourcesMap: MutableMap<String, ClusterLoadAssignment>) {
    val removeEndpoints = updateResourceObj.getJsonArray(EdsApi.REMOVE_ENDPOINTS)
    if (removeEndpoints == null || removeEndpoints.size() == 0) {
      return
    }
    // group all endpoints with same cluster name so the removal can be batched
    val removeClustersEndpoints = mutableMapOf<String, MutableSet<String>>()
    for (i in 0 until removeEndpoints.size()) {
      val endpoint = removeEndpoints.getJsonObject(i)
      val clusterName = endpoint.getString(EdsApi.RemoveEndpointApi.CLUSTER_NAME)
      var currentSet = removeClustersEndpoints[clusterName]
      if (currentSet == null) {
        currentSet = mutableSetOf()
      }
      val removeEndpointStr = endpoint.getString(EdsApi.RemoveEndpointApi.ADDRESS) +
                              REMOVE_ENDPOINT_ADDR_PORT_SEPARATOR +
                              (endpoint.getInteger(EdsApi.RemoveEndpointApi.PORT_VALUE) ?: endpoint.getString(EdsApi.RemoveEndpointApi.NAMED_PORT))
      currentSet.add(removeEndpointStr)
      removeClustersEndpoints[clusterName] = currentSet
    }
    // now go through each cluster and remove the endpoints
    /*
    * ClusterLoadAssignment looks like:
    *
    * load_assignment:
    *   cluster_name: cluster1
    *   endpoints:                                # are LocalityLbEndpoints
    *     - locality: {}
    *       load_balancing_weight:                # int between 1 and 128
    *       priority:                             # 0-N (0 being lowest)
    *       lb_endpoints:
    *         - health_status: HEALTHY            # whatever desired. only certain values valid. see docs.
    *           load_balancing_weight:            # int between 1 and 128
    *           metadata: {}
    *           endpoint:                         # endpoint or endpoint_name required. endpoint_name points to named endpoint in cluster load assignment
    *             health_check_config: {}
    *             address:                        # see docs for requirements
    *               socket_address:
    *                 address: 1.1.1.1
    *                 port_value: 443
    *
    *   named_endpoints:
    *     key: {value}
    *     ...
    *   policy: {}
    *
    * */
    for (entry in removeClustersEndpoints) {
      val loadAssm = resourcesMap[entry.key]
      if (loadAssm != null) {
        val localityLbEndpointsToKeep = mutableListOf<LocalityLbEndpoints>()
        // endpoints:
        for (localityLbEndpoints in loadAssm.endpointsList) {
          val endpointsToKeep = mutableListOf<LbEndpoint>()
          // lb_endpoints:
          for (lbEndpoint in localityLbEndpoints.lbEndpointsList) {
            // port value or a named port could be set, check for both
            val addrPortValue = lbEndpoint.endpoint.address.socketAddress.address +
                                REMOVE_ENDPOINT_ADDR_PORT_SEPARATOR +
                                lbEndpoint.endpoint.address.socketAddress.portValue
            val addrNamedPort = lbEndpoint.endpoint.address.socketAddress.address +
                                REMOVE_ENDPOINT_ADDR_PORT_SEPARATOR +
                                lbEndpoint.endpoint.address.socketAddress.namedPort
            if (!(entry.value.contains(addrPortValue) || entry.value.contains(addrNamedPort))) {
              endpointsToKeep.add(lbEndpoint)
            }
          }
          // there were some lb endpoints left in the LocalityLbEndpoints list, keep it
          if (endpointsToKeep.isNotEmpty()) {
            localityLbEndpointsToKeep.add(localityLbEndpoints.toBuilder().clearLbEndpoints().addAllLbEndpoints(endpointsToKeep).build())
          }
        }
        resourcesMap[entry.key] = loadAssm.toBuilder().clearEndpoints().addAllEndpoints(localityLbEndpointsToKeep).build()
      }
    }
  }

}