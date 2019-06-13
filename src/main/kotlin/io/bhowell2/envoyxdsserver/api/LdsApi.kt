package io.bhowell2.envoyxdsserver.api

/**
 * @author Blake Howell
 */
class LdsApi {

  companion object {
    /*
    * JsonArray of JsonObjects to add a Listener resource. If a Listener already exists by the 'name' provided
    * it will be merged with the existing Listener.
    * See: https://www.envoyproxy.io/docs/envoy/v1.10.0/api-v2/api/v2/lds.proto#envoy-api-msg-listener
    */
    const val ADD = DefaultUpdateServiceApi.ADD

    /*
    * JsonArray of Listener names (Strings) to remove. Removes the entire Listener by that name.
    */
    const val REMOVE = DefaultUpdateServiceApi.REMOVE
  }

}