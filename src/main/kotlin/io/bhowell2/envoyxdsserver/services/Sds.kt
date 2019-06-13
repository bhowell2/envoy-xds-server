package io.bhowell2.envoyxdsserver.services

import io.bhowell2.envoyxdsserver.api.SdsApi
import io.bhowell2.envoyxdsserver.api.UpdateApi
import io.bhowell2.envoyxdsserver.api.UpdateServicesApi
import io.envoyproxy.envoy.api.v2.auth.*
import io.vertx.core.json.JsonObject

/**
 * @author Blake Howell
 */
class Sds : ServiceBase<Secret, Secret.Builder> {

  companion object {
    const val RESOURCE_TYPE_NAME = UpdateServicesApi.SDS
  }

  constructor(): super(RESOURCE_TYPE_NAME)

  constructor(copyFrom: Sds): super(copyFrom)

  override fun getResourceBuilder(): Secret.Builder {
    return Secret.newBuilder()
  }

  /**
   * Returns Secret.name. The name is used to retrieve the secret when using SDS.
   */
  override fun getResourceKeyFromBuilder(resourceBuilder: Secret.Builder): String {
    return resourceBuilder.name
  }

  override fun addResources(updateResourceObj: JsonObject, resourcesMap: MutableMap<String, Secret>) {
    super.addResources(updateResourceObj, resourcesMap)
  }


  private fun replaceInlineStringOrBytesFromObj(jsonObject: JsonObject?) {
    val inlineString = jsonObject?.getString("inline_string")
    if (inlineString != null && inlineString != "") {
      jsonObject.put("inline_string", SdsApi.INLINE_BYTES_OR_STRING_RESPONSE)
    }
    // check inline bytes
    val inlineBytes = jsonObject?.getString("inline_bytes")
    if (inlineBytes != null && inlineBytes != "") {
      jsonObject.put("inline_bytes", SdsApi.INLINE_BYTES_OR_STRING_RESPONSE)
    }
  }

  /**
   * Removes inline string/bytes for a certificate. This makes sure the private key is not sent in plaintext
   * back to the user (even if it's over TLS). Currently not allowing user to opt into/out of this.
   */
  override fun generateResourceResponse(resource: Secret, includeDefaultValues: Boolean): JsonObject {
    val resp = super.generateResourceResponse(resource, includeDefaultValues)
    replaceInlineStringOrBytesFromObj(resp.getJsonObject("tls_certificate")?.getJsonObject("private_key"))
    return resp
  }



}