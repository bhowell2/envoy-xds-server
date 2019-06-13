package io.bhowell2.envoyxdsserver.utils

import io.vertx.core.http.HttpClientResponse
import io.vertx.junit5.VertxTestContext
import kotlin.test.assertEquals

/**
 *
 * @author Blake Howell
 */
class HttpTestUtils {
  companion object {
    fun assertStatusCode200(vertxTestContext: VertxTestContext, req: HttpClientResponse) {
      vertxTestContext.verify { assertEquals(200, req.statusCode()) }
    }
  }
}