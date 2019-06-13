package io.bhowell2.envoyxdsserver.utils

import io.bhowell2.envoyxdsserver.utils.UpdateServiceTestUtils.Companion.generateDefaultUpdateServiceJson
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

/**
 * @author Blake Howell
 */
class CdsTestUtils {
  companion object {

    /**
     * The most basic Cluster resource. Can use [generateClusterLoadAssignmentJson] to generate the load assignments.
     */
    fun generateClusterJson(clusterName: String, loadAssignments: List<JsonObject>): JsonObject {
      return JsonObject().put("name", clusterName)
          .put("load_assignment", JsonArray(loadAssignments))
    }

    /**
     * This is embedded within CDS Can be used by EDS stuff as well.
     */
    fun generateClusterLoadAssignmentJson(clusterName: String, addresses: List<String>, portValues: List<Int>): JsonObject {
      if (addresses.size != portValues.size) {
        throw Exception("Number of addresses (${addresses.size}) does not match number of ports supplied (${portValues.size}).")
      }
      val lbEndpoints = JsonArray()

      for (i in 0 until addresses.size) {
        val socketAddress = JsonObject()
        socketAddress.put("address", addresses[i])
            .put("port_value", portValues[i])

        val address = JsonObject()
        address.put("socket_address", socketAddress)

        val endpoint = JsonObject()
        endpoint.put("address", address)

        val lbEndpoint = JsonObject()
        lbEndpoint.put("endpoint", endpoint)
        lbEndpoints.add(lbEndpoint)
      }

      val endpoints = JsonObject()
      endpoints.put("lb_endpoints", lbEndpoints)

      val cla = JsonObject()
      cla.put("cluster_name", clusterName)

      val endpointsList = JsonArray()
      endpointsList.add(endpoints)
      cla.put("endpoints", endpointsList)

      return cla
    }

    fun generateClusterLoadAssignmentJson(clusterName: String, address: String, portValue: Int): JsonObject {
      return generateClusterLoadAssignmentJson(clusterName, listOf(address), listOf(portValue))
    }

    fun generateUpdateCdsJson(addClusters: List<JsonObject>?, removeClusters: List<String>?): JsonObject {
      return generateDefaultUpdateServiceJson(addClusters, removeClusters)
    }

  }
}
