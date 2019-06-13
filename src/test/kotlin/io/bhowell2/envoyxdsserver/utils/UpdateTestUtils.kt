package io.bhowell2.envoyxdsserver.utils

import io.bhowell2.envoyxdsserver.api.UpdateApi
import io.bhowell2.envoyxdsserver.api.UpdateServicesApi
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

/**
 * @author Blake Howell
 */
class UpdateTestUtils {
  companion object {

    fun generateUpdateRequestJson(updateServices: List<JsonObject>?, removeGroups: List<String>?): JsonObject {
      val updateRequest = JsonObject()
      if (updateServices != null) updateRequest.put(UpdateApi.UPDATE_SERVICES, JsonArray(updateServices))
      if (removeGroups != null) updateRequest.put(UpdateApi.REMOVE_GROUPS, JsonArray(removeGroups))
      return updateRequest
    }

  }
}
