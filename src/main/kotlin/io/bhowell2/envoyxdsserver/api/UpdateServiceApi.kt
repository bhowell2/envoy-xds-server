package io.bhowell2.envoyxdsserver.api

/**
 * @author Blake Howell
 */

class UpdateServicesApi {
  companion object {
    // JsonArray of the groups (strings) to update
    const val GROUPS = "groups"
    // CDS, EDS, LDS, RDS, and SDS are all JsonObjects. See their documentation for their respective parameters
    const val CDS = "cds"
    const val EDS = "eds"
    const val LDS = "lds"
    const val RDS = "rds"
    const val SDS = "sds"
  }
}
