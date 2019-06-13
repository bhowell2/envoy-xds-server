package io.bhowell2.envoyxdsserver.api

/**
 * @author Blake Howell
 */

class CdsApi {

  companion object {
    /*
     * JsonArray of JsonObjects to add a Cluster resource. If a cluster already exists by the 'name' provided
     * it will be merged with the existing Cluster.
     * See: https://www.envoyproxy.io/docs/envoy/v1.10.0/api-v2/api/v2/cds.proto#cluster
     */
    const val ADD = DefaultUpdateServiceApi.ADD

    /*
    * JsonArray of the Cluster names (Strings) to remove. Removes the entire Cluster.
    */
    const val REMOVE = DefaultUpdateServiceApi.REMOVE
  }

}
