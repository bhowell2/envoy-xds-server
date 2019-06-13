package io.bhowell2.envoyxdsserver.exceptions

/**
 * @author Blake Howell
 */
class BadRequestException(responseMsg: String, statusCode: Int = 400) : ResponseException(responseMsg, statusCode) {
}