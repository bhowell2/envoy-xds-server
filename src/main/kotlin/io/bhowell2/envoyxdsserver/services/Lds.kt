package io.bhowell2.envoyxdsserver.services

import io.bhowell2.envoyxdsserver.api.UpdateApi
import io.bhowell2.envoyxdsserver.api.UpdateServicesApi
import io.envoyproxy.envoy.api.v2.Listener
import io.envoyproxy.envoy.api.v2.ListenerOrBuilder

/**
 * Listeners are pretty complex, so there has been no work done to simplify their creation. A fully valid
 * (JSON) Listener should be submitted and it will be created or merged by the name.
 * @author Blake Howell
 */
class Lds : ServiceBase<Listener, Listener.Builder> {

  companion object {
    const val RESOURCE_TYPE_NAME = UpdateServicesApi.LDS
  }

  constructor() : super(RESOURCE_TYPE_NAME)

  constructor(copyFrom: Lds): super(copyFrom)

  override fun getResourceBuilder(): Listener.Builder {
    return Listener.newBuilder()
  }

  override fun getResourceKeyFromBuilder(resourceBuilder: Listener.Builder): String {
    return resourceBuilder.name
  }

}