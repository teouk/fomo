package io.github.teouk.fomo.data.repository

/**
 * The result of loading data: either [Success] with the data or [Failure] with a reason.
 *
 * A `sealed interface` has a fixed set of subtypes, so a `when` over it must handle
 * every case or the code does not compile. Compared with throwing exceptions, the
 * caller cannot forget the failure case.
 */
sealed interface Outcome<out T> {
    data class Success<T>(val data: T) : Outcome<T>

    data class Failure(val error: DataError) : Outcome<Nothing>
}

/** Why loading failed, in terms the UI can turn into a message. */
enum class DataError {
    /** No connection, or the request timed out. */
    OFFLINE,

    /** No Data Thistle token in local.properties, or the token was rejected. */
    INVALID_API_KEY,

    /** The daily or per-second quota was used up. */
    RATE_LIMITED,

    /** The event no longer exists. */
    NOT_FOUND,

    /** The API had a problem, or sent something we could not read. */
    SERVER,
}
