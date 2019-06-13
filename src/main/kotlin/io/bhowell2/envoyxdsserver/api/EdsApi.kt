package io.bhowell2.envoyxdsserver.api


/**
 * API for adding ClusterLoadAssignments (CLA) for EDS resources.
 * @author Blake Howell
 */
class EdsApi {

  companion object {
    /*
    * JsonArray of JsonObjects to add a CLA. The CLA does not need to be a fully valid CLA definition if one for the
    * cluster_name already exists as they will be merged (this will also merge endpoints). If only adding an endpoint
    * to an already existing CLA, consider using the opinionated 'add_endpoint' parameter instead. Since the
    * CLAs are merged, this will not remove an endpoint from a CLA. The endpoint will need to be removed with the
    * 'remove_endpoints' parameter.
    */
    const val ADD = DefaultUpdateServiceApi.ADD

    /*
    * JsonArray of JsonObjects (AddEndpointApi below) that can be used to add an endpoint to an already existing CLA.
    * See docs (or AddEndpointApi above) for more info on this parameter.
    */
    const val ADD_ENDPOINTS = "add_endpoints"

    /*
    * JsonArray of JsonObjects (RemoveEndpointApi below) specifying which endpoints to remove. It should be noted
    * that if multiple endpoints exist for the same cluster (by name), ip, and port they will ALL be removed.
    * See docs (or RemoveEndpointApi above) for more info on this parameter.
    */
    const val REMOVE_ENDPOINTS = "remove_endpoints"

    /*
    * JsonArray of Strings of the cluster_name that identifies the CLA.
    */
    const val REMOVE = DefaultUpdateServiceApi.REMOVE
  }


  /**
   * Succinct API for adding an endpoint to a CLA. CLA will be created with default values if none by the 'cluster_name'
   * is specified. This succinct API helps reduce the amount of nesting required for adding an endpoint.
   * Rather than needing to submit a fully qualified CLA object with an endpoints embedded, can simply add the
   * endpoint with the succinct API below. The succinct API will merge the endpoint with any existing endpoints that
   * match the cluster_name, address, and port value. This means that if multiple endpoints exists in the same
   * cluster (by name) with the same address and port
   * Fully qualified:
   * {
   *    "cluster_name": "whatever",
   *    "endpoints": [
   *       {
   *        "lb_endpoints": [
   *          {
   *            "endpoint": {
   *              "address": {
   *                "socket_address": {
   *                  "address": "1.2.3.4",
   *                  "port_value": 443
   *                }
   *              },
   *              "health_check_config": {
   *                "port_value": 9999
   *              }
   *            },
   *            "health_status": "UNKNOWN",
   *            "load_balancing_weight": 128,
   *            "metadata": {
   *              "key": "value",
   *              "key2": true
   *            }
   *          }
   *        ]
   *       }
   *    ]
   * }
   *
   * Succinct:
   * {
   *  "cluster_name": "whatever",
   *  "address": "1.2.3.4",
   *  "port_value": 443,
   *  "health_status": "UNKNOWN" | "HEALTHY" | "UNHEALTHY" | "DRAINING" | "TIMEOUT" | "DEGRADED",
   *  "load_balancing_weight": 128,
   *  "health_check_port_value": 9999,
   *  "metadata": {
   *    "key": "value",
   *    "key2": true
   *  }
   * }
   */
  class AddEndpointApi {
    companion object {

      // cluster_name, address, and port_value are required.

      /* cluster name of top-level CLA object */
      const val CLUSTER_NAME = "cluster_name"
      /* address of socket_address */
      const val ADDRESS = "address"
      /* port_value of socket_address */
      const val PORT_VALUE = "port_value"

      // these are not required, but available if desired

      /* metadata of lb_endpoint */
      const val METADATA = "metadata"
      /* health status of lb_endpoint */
      const val HEALTH_STATUS = "health_status"
      /* load balancing weight of the lb_endpoint */
      const val LOAD_BALANCING_WEIGHT = "load_balancing_weight"
      /* value used for health check port of lb_endpoint.endpoint (endpoint.Endpoint)*/
      const val HEALTH_CHECK_PORT_VALUE = "health_check_port_value"
    }
  }

  class RemoveEndpointApi {
    companion object {
      const val CLUSTER_NAME = AddEndpointApi.CLUSTER_NAME
      const val ADDRESS = AddEndpointApi.ADDRESS
      const val PORT_VALUE = AddEndpointApi.PORT_VALUE
      const val NAMED_PORT = "named_port"
    }
  }



}