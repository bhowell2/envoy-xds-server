package io.bhowell2.envoyxdsserver.exceptions

/**
 * @author Blake Howell
 */
class InternalServerError(responseMsg: String = "Internal server error.", statusCode: Int = 500) : ResponseException(responseMsg, statusCode) {
}