package io.bhowell2.envoyxdsserver.api

/**
 * This is the top level API that is POSTed to the HTTP server (which will update the XDS Resources/Server).
 *
 * The POST request should be of the form:
 * {
 *  // will completely delete all services for any groups in this list
 *  remove_groups: ["group1", ... "groupN"],
 *  // list of services to update for a group
 *  update_services: [
 *    {
 *      groups: ["group1", ... "groupN"],
 *      cds: {},
 *      eds: {},
 *      lds: {},
 *      rds: {},
 *      sds: {}
 *    },
 *    ...
 *    {
 *      groups: ["group2"],
 *      ...
 *    }
 *  ],
 * }
 *
 * @author Blake Howell
 */

class UpdateApi {

  companion object {
    // JsonArray (or string of a single group) of groups to update with the other resources provided
    const val UPDATE_SERVICES = "update_services"

    // JsonArray (or string with name of single group) of groups to completely remove
    const val REMOVE_GROUPS = "remove_groups"
  }

}