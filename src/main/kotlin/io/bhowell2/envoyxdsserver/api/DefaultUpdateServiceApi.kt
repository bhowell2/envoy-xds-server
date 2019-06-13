package io.bhowell2.envoyxdsserver.api

/**
 * This is the default resource API. It provides some basic structure for add and remove operations for a resource.
 * Some resources may have other possible parameters (e.g., endpoints allows for removing a single endpoint with
 * the default 'remove' parameter, but also allows to remove the entire ClusterLoadAssignment with 'remove_cla'.)
 *
 * It should be assumed that for all resources REMOVE operations will be performed before ADD operations unless
 * otherwise specified.
 *
 * @author Blake Howell
 */
class DefaultUpdateServiceApi {

  companion object {

    /*
    * JsonArray of JsonObjects that can be parsed to create the desired resource
    * (e.g., a valid Cluster object). This is used to fully create the resource.
    * When the resource's name matches an existing resource they will be merged,
    * with the latest update overriding any conflicting values.
    * Each resource will have this parameter to fully create a resource, but the
    * resource may also have other parameters to add to the resource.
    */
    const val ADD = "add"
    /*
    * JsonArray of Strings that resolve to the name of the resource to remove.
    * This is used to fully remove the resource. Resources may contain more
    * expressive remove parameters, see their respective documentation.
    */
    const val REMOVE = "remove"
  }

}