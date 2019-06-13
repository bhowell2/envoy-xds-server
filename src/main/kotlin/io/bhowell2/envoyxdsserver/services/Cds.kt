package io.bhowell2.envoyxdsserver.services

import io.bhowell2.envoyxdsserver.api.UpdateApi
import io.bhowell2.envoyxdsserver.api.UpdateServicesApi
import io.envoyproxy.envoy.api.v2.Cluster
import io.envoyproxy.envoy.api.v2.ClusterOrBuilder

/**
 * This is a simple implementation that keeps track of active clusters.
 * The 'name' parameter provided in the cluster resource is used to identify the cluster during add and remove
 * operations. If an add operation occurs when a cluster by that name already exists the two clusters will
 * be merged, with the newest cluster definition overriding the older.
 *
 * @author Blake Howell
 */
class Cds: ServiceBase<Cluster, Cluster.Builder> {

  companion object {
    const val SERVICE_TYPE_NAME = UpdateServicesApi.CDS
  }

  constructor(): super(SERVICE_TYPE_NAME)

  constructor(copy: Cds): super(copy)

  override fun getResourceKeyFromBuilder(resourceBuilder: Cluster.Builder): String {
    return resourceBuilder.name
  }

  override fun getResourceBuilder(): Cluster.Builder {
    return Cluster.newBuilder()
  }

}