package io.bhowell2.envoyxdsserver.api

/**
 * @author Blake Howell
 */
class SdsApi {

  companion object {
    /*
    * JsonArray of JsonObjects to add a Secret resource. If the resource already exists by the 'name' provided,
    * it will be overwritten - it will not be merged.
    * See docs: https://www.envoyproxy.io/docs/envoy/v1.10.0/api-v2/api/v2/auth/cert.proto#auth-secret
    */
    const val ADD_SECRETS = DefaultUpdateServiceApi.ADD

    /*
    * JsonArray of secret names (Strings) to remove. Removes the entire secret.
    */
    const val REMOVE = DefaultUpdateServiceApi.REMOVE

    /*
    * Message returned when the secret has set inline_bytes or inline_string (for the respective field).
    * If the user sends this back (exactly), it will be ignored. Any other values will not be ignored and
    * will be merged (or created) with the Secret name provided.
    */
    const val INLINE_BYTES_OR_STRING_RESPONSE = "Exists, but will not be shown."
  }

  /**
   * Adding a secret is a bit different than the other endpoints due to the secure nature of it.
   */
  class AddSecretApi {

  }

}