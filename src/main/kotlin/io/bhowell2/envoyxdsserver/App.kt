package io.bhowell2.envoyxdsserver

import com.google.protobuf.InvalidProtocolBufferException
import io.bhowell2.envoyxdsserver.api.SdsApi
import io.envoyproxy.envoy.api.v2.Cluster
import io.envoyproxy.envoy.api.v2.RouteConfiguration
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject


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
 * Go through the route config and remove the matching route if it exists
 */
fun matchRouteName(routeConfig: RouteConfiguration, routeConfigName: String,
                   virtualHostName: String, routeName: String): Boolean {
  if (routeConfig.name == routeConfigName) {
    for (host in routeConfig.virtualHostsList) {
      if (host.name == virtualHostName) {
        for (route in host.routesList) {
          if (route.hasMetadata()) {
            if (route.metadata.containsFilterMetadata("name")) {
              val routeFilterMetadataForName = route.metadata.getFilterMetadataOrDefault("name", null)
              val nameFilterMetaField = routeFilterMetadataForName.getFieldsOrDefault("name", null)
              if (nameFilterMetaField.stringValue == routeName) {
                return true
              }
            }
          }
        }
      }
    }
  }
  return false
}

fun main() {

  val addCds = "{\n" +
               "                                      \"name\": \"cluster1\",\n" +
               "                                      \"type\": \"EDS\",\n" +
               "                                      \"connect_timeout\": \"0.25s\",\n" +
               "                                      \"http2_protocol_options\": {},\n" +
               "                                      \"upstream_connection_options\": {\n" +
               "                                        \"tcp_keepalive\": {}\n" +
               "                                      },\n" +
               "                                      \"eds_cluster_config\": {\n" +
               "                                        \"eds_config\": {\n" +
               "                                          \"api_config_source\": {\n" +
               "                                            \"api_type\": \"GRPC\",\n" +
               "                                            \"grpc_services\": [\n" +
               "                                              {\n" +
               "                                                \"envoy_grpc\": {\n" +
               "                                                  \"cluster_name\": \"xds_cluster\"\n" +
               "                                                }\n" +
               "                                              }\n" +
               "                                            ]\n" +
               "                                          }\n" +
               "                                        }\n" +
               "                                      }\n" +
               "                                    }"

  val json = JsonObject(addCds)
  println(json)

  val builder = Cluster.newBuilder()
  try {
    jsonToMessageBuilder(json, builder)
    println(builder.build())
  } catch (e: InvalidProtocolBufferException) {
    println(e.message)
    println(e.unfinishedMessage)
  }

//  val match1 = RouteMatch.newBuilder().setPrefix("/").addHeaders(HeaderMatcher.newBuilder().setName("whatever"))
//  val route1 = Route.newBuilder().setMatch(match1).build()
//
//  val match2 = RouteMatch.newBuilder().setPrefix("/").addHeaders(HeaderMatcher.newBuilder().setName("whatever"))
//  val route2 = Route.newBuilder().setMatch(match2).build()
//
//  println(route1.equals(route2))
//
//  val match3 = RouteMatch.newBuilder().setPrefix("/hey").addHeaders(HeaderMatcher.newBuilder().setName("whatever"))
//  val route3 = Route.newBuilder().setMatch(match3).build()
//
//  println(route1.equals(route3))
//
//  val meta = Metadata.newBuilder().putFilterMetadata("wtf", Struct.newBuilder().putFields("wtf", Value.newBuilder().setStringValue("wtf").build()).build())
//
//  val match4 = RouteMatch.newBuilder().setPrefix("/").addHeaders(HeaderMatcher.newBuilder().setName("diff"))
//  val route4 = Route.newBuilder().setMatch(match4).setMetadata(meta).build()
//  println(route4)
//  println(route1.equals(route4))

//  val jsonRoute = JsonObject().put("match", JsonObject().put("prefix", "/"))
//      .put("route", JsonObject().put("cluster", "whatever"))
//      .put("metadata", JsonObject().put("filter_metadata", JsonObject().put("name", JsonObject().put("name","wtf"))))
//
//  val virtualHost = JsonObject().put("name", "virtual_name")
//      .put("domains", JsonArray().add("*"))
//      .put("routes", JsonArray().add(jsonRoute))
//
//  val routeConfigJson = JsonObject().put("name", "config_name")
//      .put("virtual_hosts", JsonArray().add(virtualHost))
//
//  val routeConfigBuilder = RouteConfiguration.newBuilder()
//  jsonToMessageBuilder(routeConfigJson, routeConfigBuilder)
//  println(routeConfigBuilder.build())
//
//  val builder2 = RouteConfiguration.newBuilder()
//  jsonToMessageBuilder(routeConfigJson, builder2)
//  val config2 = routeConfigBuilder.mergeFrom(builder2.build()).build()
//  println(config2)
}

//
