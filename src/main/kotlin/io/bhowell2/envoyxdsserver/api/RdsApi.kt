package io.bhowell2.envoyxdsserver.api

/**
 * @author Blake Howell
 */
class RdsApi {

  companion object {
    /*
    * JsonArray of JsonObjects that can be used to create or merge a RouteConfiguration object.
    * RouteConfigurations are merged on name.
    */
    const val ADD = DefaultUpdateServiceApi.ADD

    /*
    * JsonArray of JsonObjects (see AddRouteApi) to add a route to a RouteConfiguration's VirtualHost's routes.
    * */
    const val ADD_ROUTES = "add_routes"

    /*
    * JsonArray of RouteConfiguration.name (Strings) to remove the entire route configuration.
    * This may be the simplest solution if the user has all of their necessary routes stored and can
    * resubmit them each time.
    */
    const val REMOVE = DefaultUpdateServiceApi.REMOVE

    /*
    * JsonArray of JsonObjects (see RemoveRouteApi) to remove a route from a RouteConfiguration's VirtualHosts.
    */
    const val REMOVE_ROUTES = "remove_routes"
  }

  /**
   * Used to add a route to a RouteConfiguration's VirtualHost without specifying the entire
   * RouteConfiguration. This will find the RouteConfiguration and VirutalHost supplied and add
   * the route to the VirtualHost's routes.
   */
  class AddRouteApi {
    companion object {
      const val ROUTE_CONFIG_NAME = "route_config_name"
      const val VIRTUAL_HOST_NAME = "virtual_host_name"
      /* Fully valid route. See https://www.envoyproxy.io/docs/envoy/v1.10.0/api-v2/api/v2/route/route.proto#envoy-api-msg-route-route */
      const val ROUTE = "route"
    }
  }

  /**
   * Used to remove an individual route from a RouteConfiguration.
   */
  class RemoveRouteApi {
    companion object {
      /* Name of the RouteConfiguration */
      const val ROUTE_CONFIG_NAME = "route_config_name"
      /* Name of the virtual host within the RouteConfiguration */
      const val VIRUTAL_HOST_NAME = "virtual_host_name"
      /* This value should exist in the Route's metadata.filter_metadata.name.name */
      const val ROUTE_METADATA_NAME = "metadata_name"
    }
  }

}