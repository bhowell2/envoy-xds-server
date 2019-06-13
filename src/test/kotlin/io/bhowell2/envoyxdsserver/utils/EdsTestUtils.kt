package io.bhowell2.envoyxdsserver.utils

import io.bhowell2.envoyxdsserver.api.EdsApi
import io.bhowell2.envoyxdsserver.utils.UpdateServiceTestUtils.Companion.generateDefaultUpdateServiceJson
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

/**
 * Convenient functions to generate
 * @author Blake Howell
 */
class EdsTestUtils {
  companion object {

    /**
     * @param addClas valid ClusterLoadAssignments (CLA)
     * @param addEndpoints see [io.bhowell2.envoyxdsserver.api.EdsApi.AddEndpointApi]
     * @param removeClas list of strings that are the name of the CLAs
     * @param removeEndpoints see [io.bhowell2.envoyxdsserver.api.EdsApi.RemoveEndpointApi]
     */
    fun generateEdsUpdateJson(addClas: List<JsonObject>?, addEndpoints: List<JsonObject>?,
                              removeClas: List<String>?, removeEndpoints: List<JsonObject>?): JsonObject {
      val updateEds = generateDefaultUpdateServiceJson(addClas, removeClas)
      if (addEndpoints != null) updateEds.put(EdsApi.ADD_ENDPOINTS, JsonArray(addEndpoints))
      if (removeEndpoints != null) updateEds.put(EdsApi.REMOVE_ENDPOINTS, JsonArray(removeEndpoints))
      return updateEds
    }

    /**
     *
     */
    fun generateAddEndpointJson(clusterName: String, address: String, portValue: Int): JsonObject {
      TODO("Not implemented")
    }

    /**
     *
     */
    fun generateRemoveEndpointJson(clusterName: String, address: String, portValue: Int): JsonObject {
      return JsonObject()
          .put(EdsApi.RemoveEndpointApi.CLUSTER_NAME, clusterName)
          .put(EdsApi.RemoveEndpointApi.ADDRESS, address)
          .put(EdsApi.RemoveEndpointApi.PORT_VALUE, portValue)
    }

  }
}

