package io.bhowell2.envoyxdsserver.utils

import io.bhowell2.envoyxdsserver.api.DefaultUpdateServiceApi
import io.bhowell2.envoyxdsserver.api.UpdateServicesApi
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

/**
 * @author Blake Howell
 */
class UpdateServiceTestUtils {
  companion object {
    /**
     * Creates JsonObject to update a service - this is can be used to add a fully valid resource
     * (e.g., Cluster, ClusterLoadAssignment) or fully remove a resource by name.
     */
    fun generateDefaultUpdateServiceJson(add: List<JsonObject>?, remove: List<String>?): JsonObject {
      val updateService = JsonObject()
      if (add != null) updateService.put(DefaultUpdateServiceApi.ADD, JsonArray(add))
      if (remove != null) updateService.put(DefaultUpdateServiceApi.REMOVE, JsonArray(remove))
      return updateService
    }

    /**
     * Returns a JsonObject for updating a group's services. When posting to the full API endpoint (/update), this
     * should be a value in the JsonArray that is the 'update_services' parameter.
     * JsonObject of form:
     * {
     *  groups: [],
     *  cds: {
     *    // whatever update fields
     *  },
     *  eds: {},
     *  lds: {},
     *  rds: {},
     *  sds: {}
     * }
     */
    fun generateUpdateServicesJson(groups: List<String>, updateCds: JsonObject?, updateEds: JsonObject?,
                                   updateLds: JsonObject?, updateRds: JsonObject?, updateSds: JsonObject?): JsonObject {
      val retObj = JsonObject().put(UpdateServicesApi.GROUPS, JsonArray(groups))
      if (updateCds != null) {
        retObj.put(UpdateServicesApi.CDS, updateCds)
      }
      if (updateEds != null) {
        retObj.put(UpdateServicesApi.EDS, updateEds)
      }
      if (updateLds != null) {
        retObj.put(UpdateServicesApi.LDS, updateLds)
      }
      if (updateRds != null) {
        retObj.put(UpdateServicesApi.RDS, updateRds)
      }
      if (updateSds != null) {
        retObj.put(UpdateServicesApi.SDS, updateSds)
      }
      return retObj
    }

    fun generateUpdateCdsState(clusterGroups: List<String>, updateCds: JsonObject): JsonObject {
      return generateUpdateServicesJson(clusterGroups, updateCds, null, null, null, null)
    }

    fun generateUpdateEdsState(clusterGroups: List<String>, updateEds: JsonObject): JsonObject {
      return generateUpdateServicesJson(clusterGroups, null, updateEds, null, null, null)
    }

    fun generateUpdateLdsState(clusterGroups: List<String>, updateLds: JsonObject): JsonObject {
      return generateUpdateServicesJson(clusterGroups, null, null, updateLds, null, null)
    }

    fun generateUpdateRdsState(clusterGroups: List<String>, updateRds: JsonObject): JsonObject {
      return generateUpdateServicesJson(clusterGroups, null, null, null, updateRds, null)
    }

    fun generateUpdateSdsState(clusterGroups: List<String>, updateSds: JsonObject): JsonObject {
      return generateUpdateServicesJson(clusterGroups, null, null, null, null, updateSds)
    }


  }
}