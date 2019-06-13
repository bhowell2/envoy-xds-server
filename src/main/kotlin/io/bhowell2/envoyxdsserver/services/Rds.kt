package io.bhowell2.envoyxdsserver.services

import io.bhowell2.envoyxdsserver.api.RdsApi
import io.bhowell2.envoyxdsserver.api.UpdateServicesApi
import io.bhowell2.envoyxdsserver.exceptions.BadRequestException
import io.bhowell2.envoyxdsserver.jsonToMessageBuilder
import io.envoyproxy.envoy.api.v2.RouteConfiguration
import io.envoyproxy.envoy.api.v2.route.Route
import io.vertx.core.json.JsonObject

/**
 * @author Blake Howell
 */
class Rds : ServiceBase<RouteConfiguration, RouteConfiguration.Builder> {

  companion object {
    const val RESOURCE_TYPE_NAME = UpdateServicesApi.RDS
  }

  constructor(): super(RESOURCE_TYPE_NAME)

  constructor(copyFrom: Rds): super(copyFrom)

  override fun getResourceBuilder(): RouteConfiguration.Builder {
    return RouteConfiguration.newBuilder()
  }

  override fun getResourceKeyFromBuilder(resourceBuilder: RouteConfiguration.Builder): String {
    return resourceBuilder.name
  }

  override fun removeResources(updateResourceObj: JsonObject, resourcesMap: MutableMap<String, RouteConfiguration>) {
    this.removeRouteConfigurations(updateResourceObj, resourcesMap)
    this.removeRoutes(updateResourceObj, resourcesMap)
  }

  /**
   * Removes the entire RouteConfiguration if the name matches.
   */
  private fun removeRouteConfigurations(updateResourceObj: JsonObject,
                                        resourcesMap: MutableMap<String, RouteConfiguration>) {
    val removeRouteConfigs = updateResourceObj.getJsonArray(RdsApi.REMOVE)
    if (removeRouteConfigs == null || removeRouteConfigs.size() == 0) {
      return
    }
    for (i in 0 until removeRouteConfigs.size()) {
      // should be string of route configuration names (which are keys in the resourcesMap)
      resourcesMap.remove(removeRouteConfigs.getString(i))
    }
  }

  private fun routesMetadataNameMatches(route: Route, metadataName: String): Boolean {
    return route.metadata.getFilterMetadataOrDefault("name", null)?.getFieldsOrDefault("name", null)?.stringValue == metadataName
  }

  private fun getRouteMetadataName(route: Route): String? {
    if (route.hasMetadata()) {
      return route.metadata.getFilterMetadataOrDefault("name", null)?.getFieldsOrDefault("name", null)?.stringValue
    }
    return null
  }

  /**
   * Removes a route from within a RouteConfiguration's VirtualHosts.
   */
  private fun removeRoutes(updateResourceObj: JsonObject, resourcesMap: MutableMap<String, RouteConfiguration>) {
    val removeRoutes = updateResourceObj.getJsonArray(RdsApi.REMOVE_ROUTES)
    if (removeRoutes == null || removeRoutes.size() == 0) {
      return
    }
    // grab all routes to remove for each RouteConfiguration for each VirtualHost
    // allows for batching below so multiple removals and builds are not necessary
    val removeRoutesForRouteConfigVirtualHost = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
    for (i in 0 until removeRoutes.size()) {
      val removeRouteApi = removeRoutes.getJsonObject(i)
      val rcName = removeRouteApi.getString(RdsApi.RemoveRouteApi.ROUTE_CONFIG_NAME)
      val vhName = removeRouteApi.getString(RdsApi.RemoveRouteApi.VIRUTAL_HOST_NAME)
      val routeMetadataName = removeRouteApi.getString(RdsApi.RemoveRouteApi.ROUTE_METADATA_NAME)
      if (rcName == null || vhName == null || routeMetadataName == null) {
        throw BadRequestException("Invalid ${RdsApi.REMOVE_ROUTES} parameter for RDS service. No params can be null. See docs.")
      }
      val virtualHostRouteNamesToRemove = removeRoutesForRouteConfigVirtualHost[rcName] ?: mutableMapOf()
      removeRoutesForRouteConfigVirtualHost[rcName] = virtualHostRouteNamesToRemove
      val routeNamesToRemove = virtualHostRouteNamesToRemove[vhName] ?: mutableListOf()
      virtualHostRouteNamesToRemove[vhName] = routeNamesToRemove
      routeNamesToRemove.add(routeMetadataName)
    }

    // RouteConfig name to VirtualHost name
    for (removeRcEntry in removeRoutesForRouteConfigVirtualHost) {
      val routeConfig = resourcesMap[removeRcEntry.key]
      routeConfig ?: throw BadRequestException("RouteConfiguration with name ${removeRcEntry.key} does not exist. Cannot remove route from it.")
      val routeConfigBuilder = routeConfig.toBuilder()
      for (i in 0 until routeConfigBuilder.virtualHostsList.size) {
        val virtualHost = routeConfigBuilder.getVirtualHosts(i)
        val vhBuilder = virtualHost.toBuilder().clearRoutes()
        if (removeRcEntry.value.containsKey(virtualHost.name)) {
          // need to remove routes from this virtual host. go through each route in the VH and add it if it does not
          // match the metadata name
          for (route in virtualHost.routesList) {
            val routeMetadataName = getRouteMetadataName(route)
            if (routeMetadataName != null) {
              val routesToRemoveFromVirtualHost = removeRcEntry.value[virtualHost.name]
              if (routesToRemoveFromVirtualHost == null || !routesToRemoveFromVirtualHost.contains(routeMetadataName)) {
                vhBuilder.addRoutes(route)
              }
            } else {
              // doesn't have name, cannot remove it, so add it to vh
              vhBuilder.addRoutes(route)
            }
          }
        }
        routeConfigBuilder.setVirtualHosts(i, vhBuilder.build())
      }
      resourcesMap[removeRcEntry.key] = routeConfigBuilder.build()
    }

  }

  /**
   * Add full RouteConfigurations if provided and add individual routes to a route configuration if provided.
   */
  override fun addResources(updateResourceObj: JsonObject, resourcesMap: MutableMap<String, RouteConfiguration>) {
    super.addResources(updateResourceObj, resourcesMap)
    this.addRoutes(updateResourceObj, resourcesMap)
  }


  /**
   * Adds the route to the RouteConfiguration's VirtualHost's routes.
   */
  private fun addRoutes(updateResourceObj: JsonObject, resourcesMap: MutableMap<String, RouteConfiguration>) {
    val addRoutes = updateResourceObj.getJsonArray(RdsApi.ADD_ROUTES)
    if (addRoutes == null || addRoutes.size() == 0) {
      return
    }
    // go through each route to add and aggregate them for the specified RouteConfiguration and VirtualHost
    // this will reduce the number of times the RouteConfiguration needs to be built
    val addRoutesToRouteConfigVirtualHost = mutableMapOf<String, MutableMap<String, MutableList<JsonObject>>>()
    // should be JsonObject with values of AddRouteApi
    for (i in 0 until addRoutes.size()) {
      val routeToAdd = addRoutes.getJsonObject(i)
      val routeConfigName = routeToAdd.getString(RdsApi.AddRouteApi.ROUTE_CONFIG_NAME)
      val virtualHostName = routeToAdd.getString(RdsApi.AddRouteApi.VIRTUAL_HOST_NAME)
      val route = routeToAdd.getJsonObject(RdsApi.AddRouteApi.ROUTE)
      // if all any one of these is null cannot add using this api parameter
      if (routeConfigName == null || virtualHostName == null || route == null) {
        throw BadRequestException("Invalid ${RdsApi.ADD_ROUTES} parameter for RDS service. No params can be null. See docs.")
      }
      // virtual hosts for a route configuration (by name)
      val virtualHostsRoutes = addRoutesToRouteConfigVirtualHost[routeConfigName] ?: mutableMapOf()
      // must assign it if it was not (will re-assign to same thing if it already existed)
      addRoutesToRouteConfigVirtualHost[routeConfigName] = virtualHostsRoutes
      // routes for a given virtual host (by name)
      val routesList = virtualHostsRoutes[virtualHostName] ?: mutableListOf()
      virtualHostsRoutes[virtualHostName] = routesList
      routesList.add(route)
    }

    for (routeConfigNameToVirtualHost in addRoutesToRouteConfigVirtualHost) {
      val routeConfig = resourcesMap[routeConfigNameToVirtualHost.key]
      routeConfig ?: throw BadRequestException("RouteConfiguration with name ${routeConfigNameToVirtualHost.key} does not exist. Cannot add a route to it..")

      // The route configuration exists. Get the
      val routeConfigBuilder = routeConfig.toBuilder()
      for (i in 0 until routeConfigBuilder.virtualHostsList.size) {
        val virtualHost = routeConfigBuilder.getVirtualHosts(i)
        val virtualHostRoutesToAdd = routeConfigNameToVirtualHost.value[virtualHost.name]
        if (virtualHostRoutesToAdd != null) {
          val vhBuilder = virtualHost.toBuilder()
          for (routeJson in virtualHostRoutesToAdd) {
            val routeBuilder = Route.newBuilder()
            jsonToMessageBuilder(routeJson, routeBuilder)
            vhBuilder.addRoutes(routeBuilder.build())
          }
          routeConfigBuilder.setVirtualHosts(i, vhBuilder.build())
        }
      }
      resourcesMap[routeConfigNameToVirtualHost.key] = routeConfigBuilder.build()
    }
  }

}