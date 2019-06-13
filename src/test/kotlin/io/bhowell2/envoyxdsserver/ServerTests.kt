package io.bhowell2.envoyxdsserver

import io.bhowell2.envoyxdsserver.utils.CdsTestUtils.Companion.generateClusterLoadAssignmentJson
import io.bhowell2.envoyxdsserver.utils.CdsTestUtils.Companion.generateUpdateCdsJson
import io.bhowell2.envoyxdsserver.utils.EdsTestUtils.Companion.generateEdsUpdateJson
import io.bhowell2.envoyxdsserver.utils.EdsTestUtils.Companion.generateRemoveEndpointJson
import io.bhowell2.envoyxdsserver.utils.HttpTestUtils.Companion.assertStatusCode200
import io.bhowell2.envoyxdsserver.utils.UpdateServiceTestUtils.Companion.generateUpdateCdsState
import io.bhowell2.envoyxdsserver.utils.UpdateServiceTestUtils.Companion.generateUpdateEdsState
import io.bhowell2.envoyxdsserver.utils.UpdateServiceTestUtils.Companion.generateUpdateServicesJson
import io.bhowell2.envoyxdsserver.utils.UpdateTestUtils.Companion.generateUpdateRequestJson
import io.vertx.core.*
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.json.JsonObject
import io.vertx.core.net.SelfSignedCertificate
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*
import kotlin.test.Test

/**
 * @author Blake Howell
 */
@ExtendWith(VertxExtension::class)
class ServerTests {

  lateinit var httpClient: HttpClient

  init {
    val cert = SelfSignedCertificate.create()
    System.setProperty(XDS_HTTP_SERVER_CERT, cert.certificatePath())
    System.setProperty(XDS_HTTP_SERVER_KEY, cert.privateKeyPath())
    System.setProperty(XDS_GRPC_SERVER_CERT, cert.certificatePath())
    System.setProperty(XDS_GRPC_SERVER_CERT_KEY, cert.privateKeyPath())
    System.setProperty(XDS_GRPC_SERVER_CLIENT_CA, cert.certificatePath())
  }

  @BeforeEach
  fun beforeEachTest(vertx: Vertx, testContext: VertxTestContext) {
    vertx.deployVerticle(Server()) { deployRes ->
      if (deployRes.failed()) {
        testContext.failNow(deployRes.cause())
      } else {
        val clientOpts = HttpClientOptions().setDefaultHost(Server.DEFAULT_LISTEN_ADDRESS)
            .setDefaultPort(Server.DEFAULT_LISTEN_PORT)
            .setVerifyHost(false)
            .setTrustAll(true)
            .setSsl(true)
        this.httpClient = vertx.createHttpClient(clientOpts)
        testContext.completeNow()
      }
    }
  }

  // vertx is closed after each test
  @AfterEach
  fun afterEachTest(vertx: Vertx, testContext: VertxTestContext) {
    // will clear out all data after each
    XdsController.TESTING_DROP_INSTANCE_FOR_TESTING()
    testContext.completeNow()
  }

  @Test
  fun `test inform get request only works with state path`(vertx: Vertx, testContext: VertxTestContext) {
    val checkpoint = testContext.checkpoint(2)
    httpClient.get("/").handler {
      testContext.verify {
        assertEquals(400, it.statusCode())
        checkpoint.flag()
      }
    }.end()
    httpClient.get("/state").handler {
      it.bodyHandler {
        testContext.verify {
          assertEquals(JsonObject().encode(), it.toString())
          checkpoint.flag()
        }
      }
    }.end()
  }

  @Test
  fun `should return correct state`(vertx: Vertx, testContext: VertxTestContext) {
    /*
    * {
    *   "eds": {
    *     "add": [{cluster load assignment 1}, {cluster load assm. 2}]
    *   }
    * }
    * */
    val cla = generateClusterLoadAssignmentJson("cluster1", listOf("1.2.3.4", "1.1.1.1"), listOf(9999, 9999))
    val cla2 = generateClusterLoadAssignmentJson("cluster1", listOf("2.2.2.2", "3.3.3.3"), listOf(9999, 9999))
    val addEdsResources = generateEdsUpdateJson(listOf(cla, cla2), null, null, null)
    val postObj = generateUpdateRequestJson(listOf(generateUpdateEdsState(listOf("group1"), addEdsResources)), null)

    val postUpdateState = Future.future<HttpClientResponse>()

    httpClient.post("/update").handler(postUpdateState::complete).end(postObj.encode())

    postUpdateState.compose { updateResp ->
      assertStatusCode200(testContext, updateResp)
      val getStateFuture = Future.future<HttpClientResponse>()
      httpClient.get("/state").handler(getStateFuture::complete).end()
      getStateFuture
    }.compose { stateAfterPost ->
      assertStatusCode200(testContext, stateAfterPost)
      val postUpdateRemoveEndpoint = Future.future<HttpClientResponse>()
      stateAfterPost.bodyHandler { bodyBuff ->
        testContext.verify {
          val group1ClusterResources = bodyBuff.toJsonObject().getJsonObject("group1")
          assertNotNull(group1ClusterResources)
          assertEquals(1, group1ClusterResources.getJsonObject("eds").size())
          assertEquals(2, group1ClusterResources.getJsonObject("eds").getJsonObject("cluster1").getJsonArray("endpoints").size())
          // each lb_endpoint has 2 endpoints within it
          assertEquals(2, group1ClusterResources.getJsonObject("eds").getJsonObject("cluster1").getJsonArray("endpoints")
              .getJsonObject(0).getJsonArray("lb_endpoints").size())

          // remove one of the CLA endpoints entirely
          val removeEndpoint = generateRemoveEndpointJson("cluster1", "1.2.3.4", 9999)

          val updateEdsState = generateUpdateEdsState(listOf("group1"), generateEdsUpdateJson(null, null, null, listOf(removeEndpoint)))
          httpClient.post("/update").handler(postUpdateRemoveEndpoint::complete).end(generateUpdateRequestJson(listOf(updateEdsState), null).encode())
        }
      }
      postUpdateRemoveEndpoint
    }.compose { postRemoveResp ->
      assertStatusCode200(testContext, postRemoveResp)
      val getStateFuture = Future.future<HttpClientResponse>()
      httpClient.get("/state").handler(getStateFuture::complete).end()
      getStateFuture
    }.setHandler { getStateAfterRemoveRespAsyncRes ->
      if (getStateAfterRemoveRespAsyncRes.succeeded()) {
        val resp = getStateAfterRemoveRespAsyncRes.result()
        assertStatusCode200(testContext, resp)
        resp.bodyHandler { bodyBuff ->
          testContext.verify {
            val group1ClusterResources = bodyBuff.toJsonObject().getJsonObject("group1")
            assertNotNull(group1ClusterResources)
            assertEquals(1, group1ClusterResources.getJsonObject("eds").size())
            assertEquals(2, group1ClusterResources.getJsonObject("eds").getJsonObject("cluster1").getJsonArray("endpoints").size())
            // one of the lb_endpoints has 1 endpoint and the other should still have 2, order may not be guaranteed, so
            // test for both
            assertTrue {
              if (group1ClusterResources.getJsonObject("eds").getJsonObject("cluster1").getJsonArray("endpoints")
                      .getJsonObject(0).getJsonArray("lb_endpoints").size() == 1)
                group1ClusterResources.getJsonObject("eds").getJsonObject("cluster1").getJsonArray("endpoints")
                    .getJsonObject(1).getJsonArray("lb_endpoints").size() == 2
              else
                group1ClusterResources.getJsonObject("eds").getJsonObject("cluster1").getJsonArray("endpoints")
                    .getJsonObject(1).getJsonArray("lb_endpoints").size() == 1 &&
                group1ClusterResources.getJsonObject("eds").getJsonObject("cluster1").getJsonArray("endpoints")
                    .getJsonObject(0).getJsonArray("lb_endpoints").size() == 2
            }
            testContext.completeNow()
          }
        }
      } else {
        testContext.failNow(getStateAfterRemoveRespAsyncRes.cause())
      }
      Future.succeededFuture<Unit>()
    }
  }


  @Test
  fun `should return group resources`(testContext: VertxTestContext) {
    // try to get resources for a specific group only
    val cla = generateClusterLoadAssignmentJson("cluster1", listOf("1.2.3.4", "1.1.1.1"), listOf(9999, 9999))
    val cla2 = generateClusterLoadAssignmentJson("cluster1", listOf("2.2.2.2", "3.3.3.3"), listOf(9999, 9999))
    val addEdsResources = generateEdsUpdateJson(listOf(cla, cla2), null, null, null)
    val postObj = generateUpdateRequestJson(listOf(generateUpdateEdsState(listOf("group1"), addEdsResources)), null)

    val postUpdateState = Future.future<HttpClientResponse>()

    httpClient.post("/update").handler(postUpdateState::complete).end(postObj.encode())
    postUpdateState.compose { res ->
      assertStatusCode200(testContext, res)
      val getFuture = Future.future<HttpClientResponse>()
      httpClient.get("/state?group=groupnoexist").handler(getFuture::complete).end()
      getFuture
    }.compose { res ->
      assertStatusCode200(testContext, res)

      res.bodyHandler {
        testContext.verify {
          // empty json response
          assertNull(it.toJsonObject().getJsonObject("groupnoexist"))
        }
      }

      val getFuture = Future.future<HttpClientResponse>()
      httpClient.get("/state?group=group1").handler(getFuture::complete).end()
      getFuture
    }.compose { res ->
      assertStatusCode200(testContext, res)

      res.bodyHandler {
        testContext.verify {
          // empty json response
          val group1 = it.toJsonObject().getJsonObject("group1")
          assertNotNull(group1)
          assertNotNull(group1.getJsonObject("cds"))
          assertNotNull(group1.getJsonObject("eds"))
          assertNotNull(group1.getJsonObject("eds").getJsonObject("cluster1"))
          assertNotNull(group1.getJsonObject("lds"))
          assertNotNull(group1.getJsonObject("rds"))
          assertNotNull(group1.getJsonObject("sds"))

        }
      }

      val getFuture = Future.future<HttpClientResponse>()
      httpClient.get("/state?group=group1&resource=eds").handler(getFuture::complete).end()
      getFuture
    }.setHandler { asyncRes ->
      if (asyncRes.succeeded()) {
        val resp = asyncRes.result()
        assertStatusCode200(testContext, resp)

        resp.bodyHandler {
          testContext.verify {
            // empty json response
            assertNotEquals("{}", it.toString())
            val group1 = it.toJsonObject().getJsonObject("group1")
            assertNotNull(group1)
            assertNull(group1.getJsonObject("cds"))
            assertNotNull(group1.getJsonObject("eds"))
            assertNull(group1.getJsonObject("lds"))
            assertNull(group1.getJsonObject("rds"))
            assertNull(group1.getJsonObject("sds"))

            testContext.completeNow()
          }
        }
      } else {
        testContext.failNow(asyncRes.cause())
      }
    }
  }

  // TODO test invalid protocol buffer exception response message - should return the message, not internal server err.
  @Test
  fun `should return protocol exception message with status 400`(testContext: VertxTestContext) {
    val cds = JsonObject().put("name", "whatever")
        .put("type", "DNE") // does not exist. should return error
    val updateCdsServices = generateUpdateCdsState(listOf("group1", "group2"), generateUpdateCdsJson(listOf(cds), null))
    val postObj = generateUpdateRequestJson(listOf(updateCdsServices), null)
    val postUpdateFuture = Future.future<HttpClientResponse>()
    httpClient.post("/update").handler(postUpdateFuture::complete).end(postObj.encode())
    postUpdateFuture.setHandler{ asyncRes ->
      if (asyncRes.succeeded()) {
        val resp = asyncRes.result()
        resp.bodyHandler { bodyBuff ->
          val bodyMsg = bodyBuff.toString()
          testContext.verify {
            assertEquals(400, resp.statusCode())
            assertTrue { bodyMsg.contains("type") }
            testContext.completeNow()
          }
        }
      } else {
        testContext.failNow(asyncRes.cause())
      }
    }
  }

  @Test
  fun `should reset state`(testContext: VertxTestContext) {
    // create state for multiple groups.
    // get the current state.
    // get the reset state JSON.
    // clear the state.
    // post the reset state
    // get state and assure it matches the state as retrieved above before the clear and reset

    val addCluster = generateUpdateCdsJson(listOf(JsonObject().put("name", "cluster1")), null)

    /*
    * {
    *   "eds": {
    *     "add": [{cluster load assignment 1}, {cluster load assm. 2}]
    *   }
    * }
    * */
    val cla = generateClusterLoadAssignmentJson("cluster1", listOf("1.2.3.4", "1.1.1.1"), listOf(9999, 9999))
    val cla2 = generateClusterLoadAssignmentJson("cluster1", listOf("2.2.2.2", "3.3.3.3"), listOf(9999, 9999))
    val addEdsResources = generateEdsUpdateJson(listOf(cla, cla2), null, null, null)
    // set services for 2 groups
    val postObj =
        generateUpdateRequestJson(listOf(generateUpdateServicesJson(listOf("group1", "group2"),
                                                                    addCluster, addEdsResources,
                                                                    null, null, null)), null)

    val postUpdateState = Future.future<HttpClientResponse>()

    httpClient.post("/update").handler(postUpdateState::complete).end(postObj.encode())

    postUpdateState.compose { resp ->
      val stateFuture = Future.future<JsonObject>()
      httpClient.get("/state").handler {
        it.bodyHandler {
          stateFuture.complete(it.toJsonObject())
        }
      }.end()
      stateFuture
    }.setHandler { stateAsyncRes ->
      if (stateAsyncRes.failed()) {
        testContext.failNow(stateAsyncRes.cause())
      }
      val resetStateFuture = Future.future<JsonObject>()
      httpClient.get("/resetstate").handler {
        it.bodyHandler { body ->
          resetStateFuture.complete(body.toJsonObject())
        }
      }.end()

      resetStateFuture.setHandler { resetStateAsyncResp ->
        if (resetStateAsyncResp.failed()) {
          testContext.failNow(resetStateAsyncResp.cause())
        }
        // have current state and have reset state, now reset server state
        val postRemoveAllGroupsFuture = Future.future<HttpClientResponse>()
        httpClient.post("/update").handler(postRemoveAllGroupsFuture::complete)
            .end(generateUpdateRequestJson(null, listOf("group1", "group2")).encode())

        postRemoveAllGroupsFuture.compose { removeAllGroupsResp ->
          assertStatusCode200(testContext, removeAllGroupsResp)
          val postUpdateStateFuture = Future.future<HttpClientResponse>()
          httpClient.post("/update").handler(postUpdateStateFuture::complete).end(resetStateAsyncResp.result().encode())
          postUpdateStateFuture
        }.compose { updateStateResp ->
          assertStatusCode200(testContext, updateStateResp)
          val getStateFuture = Future.future<HttpClientResponse>()
          httpClient.get("/state").handler(getStateFuture::complete).end()
          getStateFuture
        }.setHandler { stateAfterResetAsyncResult ->
          if (stateAfterResetAsyncResult.succeeded()) {
            stateAfterResetAsyncResult.result().bodyHandler { body ->
              testContext.verify {
                assertEquals(stateAsyncRes.result(), body.toJsonObject())
                testContext.completeNow()
              }
            }
          } else {
            testContext.failNow(stateAfterResetAsyncResult.cause())
          }
        }
      }
    }

  }


  @Test
  fun `should remove group`(testContext: VertxTestContext) {
    // create state for multiple groups.
    // get the current state.
    // get the reset state JSON.
    // clear the state.
    // post the reset state
    // get state and assure it matches the state as retrieved above before the clear and reset

    val addCluster = generateUpdateCdsJson(listOf(JsonObject().put("name", "cluster1")), null)

    /*
    * {
    *   "eds": {
    *     "add": [{cluster load assignment 1}, {cluster load assm. 2}]
    *   }
    * }
    * */
    val cla = generateClusterLoadAssignmentJson("cluster1", listOf("1.2.3.4", "1.1.1.1"), listOf(9999, 9999))
    val cla2 = generateClusterLoadAssignmentJson("cluster1", listOf("2.2.2.2", "3.3.3.3"), listOf(9999, 9999))
    val addEdsResources = generateEdsUpdateJson(listOf(cla, cla2), null, null, null)
    // set services for 2 groups
    val postObj =
        generateUpdateRequestJson(listOf(generateUpdateServicesJson(listOf("group1", "group2"),
                                                                    addCluster, addEdsResources,
                                                                    null, null, null)), null)

    val postUpdateState = Future.future<HttpClientResponse>()

    httpClient.post("/update").handler(postUpdateState::complete).end(postObj.encode())

    postUpdateState.compose { resp ->
      assertStatusCode200(testContext, resp)
      val stateFuture = Future.future<JsonObject>()
      httpClient.get("/state").handler {
        it.bodyHandler {
          stateFuture.complete(it.toJsonObject())
        }
      }.end()
      stateFuture
    }.setHandler { stateAsyncRes ->
      if (stateAsyncRes.failed()) {
        testContext.failNow(stateAsyncRes.cause())
      }
      testContext.verify {
        // should have 2 groups
        assertEquals(2, stateAsyncRes.result().size())
      }
      val postRemoveGroupRespFuture = Future.future<HttpClientResponse>()
      httpClient.post("/update").handler(postRemoveGroupRespFuture::complete)
          .end(generateUpdateRequestJson(null, listOf("group1")).encode())

      postRemoveGroupRespFuture.setHandler { postRemoveAsyncRes ->
        if (postRemoveAsyncRes.failed()) {
          testContext.failNow(postRemoveAsyncRes.cause())
        }
        // get the state
        httpClient.get("/state").handler {
          it.bodyHandler {
            testContext.verify {
              assertNotEquals(stateAsyncRes.result(), it.toJsonObject())
              assertEquals(1, it.toJsonObject().size())
              testContext.completeNow()
            }
          }
        }.end()
      }

    }

  }

  // TODO test add and remove from multiple groups

}