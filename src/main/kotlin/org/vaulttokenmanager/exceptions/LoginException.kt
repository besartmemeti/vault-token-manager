package org.vaulttokenmanager.exceptions

/**
 * Exception thrown when vault login process fails
 */
class LoginException(message: String, val code: Int? = -1) : Exception(message)