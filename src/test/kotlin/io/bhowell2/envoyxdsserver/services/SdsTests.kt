package io.bhowell2.envoyxdsserver.services

import io.bhowell2.envoyxdsserver.api.SdsApi
import io.bhowell2.envoyxdsserver.utils.SdsTestUtils.Companion.generateSecret
import io.bhowell2.envoyxdsserver.utils.SdsTestUtils.Companion.generateUpdateSdsJson
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Blake Howell
 */
class SdsTests {

  @Test
  fun `should add secret`() {
    val secret = generateSecret("test", "whatever", "yo")
    val updateSds = generateUpdateSdsJson(listOf(secret), null)
    val sds = Sds()
    sds.updateService(updateSds)
    val sdsResources = sds.getResourcesResponse()
    assertEquals(1, sdsResources.size())
    assertEquals(secret.getString("name"), sdsResources.getJsonObject("test").getString("name"))
    assertEquals("yo",
                 sdsResources.getJsonObject("test").getJsonObject("tls_certificate")
                     .getJsonObject("certificate_chain").getString("inline_string"))
    assertEquals(SdsApi.INLINE_BYTES_OR_STRING_RESPONSE,
                 sdsResources.getJsonObject("test").getJsonObject("tls_certificate")
                     .getJsonObject("private_key").getString("inline_string"))
  }

  @Test
  fun `should remove secret`() {
    val secret = generateSecret("test", "whatever", "yo")
    val updateSds = generateUpdateSdsJson(listOf(secret), null)
    val sds = Sds()
    sds.updateService(updateSds)
    val sdsResources = sds.getResourcesResponse()
    assertEquals(1, sdsResources.size())
    val updateSds2 = generateUpdateSdsJson(null, listOf("test"))
    val sds2 = Sds(sds)
    sds2.updateService(updateSds2)
    assertEquals(0, sds2.getResourcesResponse().size())
  }

}