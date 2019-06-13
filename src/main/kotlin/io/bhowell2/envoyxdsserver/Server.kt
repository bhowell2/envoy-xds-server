package io.bhowell2.envoyxdsserver

import com.google.protobuf.InvalidProtocolBufferException
import io.bhowell2.envoyxdsserver.api.GetStateApi
import io.bhowell2.envoyxdsserver.exceptions.BadRequestException
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.net.PemKeyCertOptions

/**
 * Provides basic HTTPS server to handle updating XdsController.
 */
class Server : AbstractVerticle() {

  companion object {
    const val DEFAULT_LISTEN_ADDRESS = "0.0.0.0"
    const val DEFAULT_LISTEN_PORT = 8888
  }

  lateinit var httpServer: HttpServer
  lateinit var xdsController: XdsController

  override fun start(startFuture: Future<Void>) {

    this.xdsController = XdsController.getInstance(vertx)

    val httpListenAddress = System.getProperty(XDS_HTTP_SERVER_ADDRESSS) ?: DEFAULT_LISTEN_ADDRESS
    val httpListenPort = System.getProperty(XDS_HTTP_SERVER_PORT)?.toInt() ?: DEFAULT_LISTEN_PORT
    val serverCertPath = System.getProperty(XDS_HTTP_SERVER_CERT)
    val serverPrivKey = System.getProperty(XDS_HTTP_SERVER_KEY)

    val httpOpts = HttpServerOptions().setHost(httpListenAddress)
        .setPort(httpListenPort)

    if (serverCertPath != null && serverPrivKey != null) {
      // if SSL certs are provided then use ALPN as well (clients can still use classic SSL if they dont support ALPN)
      val certOpts = PemKeyCertOptions().setCertPath(serverCertPath).setKeyPath(serverPrivKey)
      httpOpts.setPemKeyCertOptions(certOpts).setUseAlpn(true).setSsl(true)
    }

    httpServer = vertx.createHttpServer(httpOpts)
        .requestHandler(this::handleRequest)
        .listen {
          if (it.failed()) {
            startFuture.fail(it.cause())
          } else {
            startFuture.complete()
          }
        }
  }

  override fun stop(stopFuture: Future<Void>) {
    this.xdsController.shutdown()
    httpServer.close {
      if (it.failed()) {
        stopFuture.fail(it.cause())
      } else {
        stopFuture.complete()
      }
    }
  }

  private fun handleErrorResponse(request: HttpServerRequest, err: Exception) {
    handleErrorResponse(request, err, null)
  }

  private fun handleErrorResponse(request: HttpServerRequest, err: Exception, responseMessage: String?) {
    when(err) {
      is BadRequestException -> {
        val response = responseMessage ?: err.responseMessage
        request.response().setStatusCode(err.statusCode).end(response)
      }
      is InvalidProtocolBufferException -> {
        val response = responseMessage ?: err.message
        request.response().setStatusCode(400).end(response)
      }
      else -> {
        val response = responseMessage ?: "Internal server error."
        request.response().setStatusCode(500).end(response)
      }
    }
  }

  private fun handleRequest(request: HttpServerRequest) {
    try {
      when (request.method()) {
        HttpMethod.GET -> {
          when (request.path()) {
            "/state" -> {
              val groups = request.params().getAll(GetStateApi.GROUP)
              val resources = request.params().getAll(GetStateApi.RESOURCE)
              val defaults = request.params().get(GetStateApi.DEFAULTS)?.toBoolean() ?: false
              val state = this.xdsController.getState(groups, resources, defaults)
              request.response().end(state.encode())
            }
            "/resetstate" -> {
              request.response().end(this.xdsController.getResetState().encode())
            }
            else -> {
              request.response().setStatusCode(400).end("Currently the only path supported for GET requests is /state.")
            }
          }
        }
        HttpMethod.POST -> {
          if (request.path() == "/update") {
            request.bodyHandler { bodyBuff ->
              try {
                this.xdsController.update(bodyBuff.toJsonObject()) { err ->
                  if (err != null) {
                    handleErrorResponse(request, err)
                  } else {
                    request.response().end("Update successful.")
                  }
                }
              } catch (e: Exception) {
                handleErrorResponse(request, e)
              }
            }
          } else {
            request.response().setStatusCode(400).end("Currently the only path supported for POST requests is /update.")
          }
        }
        else -> {
          request.response().setStatusCode(405).end()
        }
      }
    } catch (e: Exception) {
      handleErrorResponse(request, e)
    }
  }

}
