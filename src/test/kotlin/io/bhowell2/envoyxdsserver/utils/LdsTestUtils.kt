package io.bhowell2.envoyxdsserver.utils

import io.bhowell2.envoyxdsserver.api.LdsApi
import io.bhowell2.envoyxdsserver.utils.UpdateServiceTestUtils.Companion.generateDefaultUpdateServiceJson
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

/**
 * @author Blake Howell
 */
class LdsTestUtils {
  companion object {
    /**
     * Very opinionated listener resource generator.
     */
    fun generateAddListenerJson(name: String, address: String, port: Int): JsonObject {
      val listener = JsonObject()
      val listenerAddress = JsonObject().put("socket_address",
                                             JsonObject().put("address", address)
                                                 .put("port_value", port))
      val runtimeFraction = JsonObject().put("default_value", JsonObject().put("numerator", 100)
          .put("denominator", "HUNDRED"))
      val routeMatch = JsonObject().put("prefix", "/")
          .put("runtime_fraction", runtimeFraction)
      val route = JsonObject().put("match", routeMatch)
      val virtualHost = JsonObject().put("name", "ahost")
          .put("domains", "*")
          .put("routes", JsonArray().add(route))
      val routeConfig = JsonObject().put("virtual_hosts", virtualHost)
      val conManagerConfig = JsonObject().put("stat_prefix", "http_test")
          .put("route_config", routeConfig)
      val httpConManFilter = JsonObject().put("name", "envoy.http_connection_manager")
          .put("config", conManagerConfig)
      val filterChains = JsonArray().add(JsonObject().put("filters", JsonArray().add(httpConManFilter)))
      listener.put("name", name)
          .put("address", listenerAddress)
          .put("filter_chains", filterChains)
      return listener
    }

    fun generateUpdateLdsJson(addListeners: List<JsonObject>?, removeListeners: List<String>?): JsonObject {
      return generateDefaultUpdateServiceJson(addListeners, removeListeners)
    }

  }
}
