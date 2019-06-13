package io.bhowell2.envoyxdsserver.utils

import io.bhowell2.envoyxdsserver.api.RdsApi
import io.bhowell2.envoyxdsserver.utils.UpdateServiceTestUtils.Companion.generateDefaultUpdateServiceJson
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

/**
 * @author Blake Howell
 */
class RdsTestUtils {
  companion object {

    /**
     * Generates Route json object of form:
     * {
     *  "match": {
     *    "prefix": matchPrefix
     *  },
     *  "route": {
     *    "cluster": routeClusterName
     *  },
     *  "metadata": {
     *    "filter_metadata": {
     *      "name": {
     *        "name": metadataName
     *      }
     *    }
     *  }
     * }
     */
    fun generateRouteJson(matchPrefix: String, routeClusterName: String, metadataName: String): JsonObject {
      return JsonObject().put("match", JsonObject().put("prefix", matchPrefix))
          .put("route", JsonObject().put("cluster", routeClusterName))
          .put("metadata", JsonObject().put("filter_metadata", JsonObject().put("name", JsonObject().put("name", metadataName))))
    }

    /**
     * Generates VirtualHost JSON of form:
     * {
     *  "name": vhName,
     *  "domains": [...],
     *  "routes": [...]
     * }
     */
    fun generateVirtualHostJson(vhName: String, domains: List<String>, routes: List<JsonObject>): JsonObject {
      return JsonObject().put("name", vhName)
          .put("domains", JsonArray(domains))
          .put("routes", JsonArray(routes))
    }

    /**
     * Generates RouteConfiguration JSON of form:
     * {
     *  "name": routeConfigName,
     *  "virtual_hosts": [...]
     * }
     */
    fun generateAddRouteConfigJson(routeConfigName: String, virtualHosts: List<JsonObject>): JsonObject {
      return JsonObject().put("name", routeConfigName)
          .put("virtual_hosts", JsonArray(virtualHosts))
    }

    /**
     * Returns JsonObject used to update RDS.
     */
    fun generateUpdateRdsJson(addRouteConfig: List<JsonObject>?, addRoutes: List<JsonObject>?,
                              removeRouteConfig: List<String>?, removeRoute: List<JsonObject>?): JsonObject {
      val updateRds = generateDefaultUpdateServiceJson(addRouteConfig, removeRouteConfig)
      if (addRoutes != null) {
        updateRds.put(RdsApi.ADD_ROUTES, JsonArray(addRoutes))
      }
      if (removeRoute != null) {
        updateRds.put(RdsApi.REMOVE_ROUTES, JsonArray(removeRoute))
      }
      return updateRds
    }

    /**
     * Generates AddRouteApi JSON of form:
     * {
     *  "route_config_name": routeConfigName,
     *  "virtual_host_name": virtualHostName,
     *  "route": route
     * }
     */
    fun generateAddIndividualRouteJson(routeConfigName: String, virtualHostName: String, route: JsonObject): JsonObject {
      return JsonObject().put(RdsApi.AddRouteApi.ROUTE_CONFIG_NAME, routeConfigName)
          .put(RdsApi.AddRouteApi.VIRTUAL_HOST_NAME, virtualHostName)
          .put(RdsApi.AddRouteApi.ROUTE, route)
    }

    /**
     * Generates RemoveRouteApi JSON of form:
     * {
     *  "route_config_name": routeConfigName,
     *  "virtual_host_name": virtualHostName,
     *  "metadata_name": metadataName
     * }
     */
    fun generateRemoveIndividualRouteJson(routeConfigName: String, virtualHostName: String, metadataName: String): JsonObject {
      return JsonObject().put(RdsApi.RemoveRouteApi.ROUTE_CONFIG_NAME, routeConfigName)
          .put(RdsApi.RemoveRouteApi.VIRUTAL_HOST_NAME, virtualHostName)
          .put(RdsApi.RemoveRouteApi.ROUTE_METADATA_NAME, metadataName)
    }

  }
}
