package io.bhowell2.envoyxdsserver.utils

import io.bhowell2.envoyxdsserver.utils.UpdateServiceTestUtils.Companion.generateDefaultUpdateServiceJson
import io.vertx.core.json.JsonObject

/**
 * @author Blake Howell
 */
class SdsTestUtils {
  companion object {

    fun generateSecret(secretName: String, privKeyInlineString: String, certInlineString: String): JsonObject {
      val privKeyDataSource = JsonObject().put("inline_string", privKeyInlineString)
      val certChainDataSource = JsonObject().put("inline_string", certInlineString)
      val tlsCert = JsonObject().put("certificate_chain", certChainDataSource).put("private_key", privKeyDataSource)
      return  JsonObject().put("name", secretName).put("tls_certificate", tlsCert)
    }

    fun generateUpdateSdsJson(addSecrets: List<JsonObject>?, removeSecrets: List<String>?): JsonObject {
      return generateDefaultUpdateServiceJson(addSecrets, removeSecrets)
    }
  }
}

