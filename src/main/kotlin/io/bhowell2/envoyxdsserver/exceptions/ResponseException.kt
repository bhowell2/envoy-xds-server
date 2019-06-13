package io.bhowell2.envoyxdsserver.exceptions

/**
 * @author Blake Howell
 */
abstract class ResponseException(val responseMessage: String, val statusCode: Int) : Exception(responseMessage) {
}