package io.bhowell2.envoyxdsserver

import com.google.gson.JsonParser
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import io.vertx.core.json.JsonObject

/**
 * Convenience functions to convert to/from protobuf.
 * @author Blake Howell
 */

fun jsonToMessageBuilder(json: JsonObject, builder: Message.Builder) {
  JsonFormat.parser().ignoringUnknownFields().merge(json.toString(), builder)
}

@Throws(Exception::class)
fun messageToJsonString(message: Message, includeDefaultValues: Boolean = false): String {
  return if (includeDefaultValues)
    JsonFormat.printer().includingDefaultValueFields().preservingProtoFieldNames().print(message)
  else
    JsonFormat.printer().preservingProtoFieldNames().print(message)
}

fun messageToJsonObject(message: Message, includeDefaultValues: Boolean = false): JsonObject {
  return if (includeDefaultValues)
    JsonObject(JsonFormat.printer().includingDefaultValueFields().preservingProtoFieldNames().print(message))
  else
    JsonObject(JsonFormat.printer().preservingProtoFieldNames().print(message))
}

@Throws(Exception::class)
fun messagesToJsonStrings(messageOrBuilderList: List<Message>): List<String> {
  return messageOrBuilderList.map { messageToJsonString(it) }
}
