package io.github.teouk.fomo.data.repository

import io.github.teouk.fomo.data.remote.TicketmasterService
import io.github.teouk.fomo.data.remote.dto.EmbeddedEventsDto
import io.github.teouk.fomo.data.remote.dto.EventDto
import io.github.teouk.fomo.data.remote.dto.EventSearchResponseDto
import io.github.teouk.fomo.model.EventFilter
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class EventRepositoryTest {
    /** A hand-written stand-in for Ticketmaster. No network, and it counts the calls. */
    private class FakeTicketmasterService : TicketmasterService {
        var searchCalls = 0
        var detailCalls = 0
        var failure: Exception? = null
        var events = listOf(EventDto(id = "e1", name = "The Harbour Lights"))

        override suspend fun searchEvents(query: Map<String, String>): EventSearchResponseDto {
            searchCalls++
            failure?.let { throw it }
            return EventSearchResponseDto(embedded = EmbeddedEventsDto(events))
        }

        override suspend fun getEvent(eventId: String): EventDto {
            detailCalls++
            failure?.let { throw it }
            return EventDto(id = eventId, name = "Loaded by id")
        }
    }

    /** A clock the test moves by hand, so cache expiry needs no real waiting. */
    private class MutableClock(var now: Instant) : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
    }

    private val service = FakeTicketmasterService()
    private val clock = MutableClock(Instant.parse("2026-11-11T12:00:00Z"))
    private val scope = TestScope()
    private val swansea = EventFilter(latitude = 51.6214, longitude = -3.9436)

    private fun repository(hasApiKey: Boolean = true) =
        EventRepository(service, hasApiKey, clock, StandardTestDispatcher(scope.testScheduler))

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    @Test
    fun searchEvents_success_returnsMappedEvents() = scope.runTest {
        val outcome = repository().searchEvents(swansea)
        assertEquals("The Harbour Lights", (outcome as Outcome.Success).data.single().name)
    }

    @Test
    fun searchEvents_sameFilterWithinTtl_usesCache() = scope.runTest {
        val repository = repository()
        repository.searchEvents(swansea)
        clock.now += EventRepository.CACHE_TTL - Duration.ofSeconds(1)
        repository.searchEvents(swansea)
        assertEquals(1, service.searchCalls)
    }

    @Test
    fun searchEvents_afterTtl_requestsAgain() = scope.runTest {
        val repository = repository()
        repository.searchEvents(swansea)
        clock.now += EventRepository.CACHE_TTL
        repository.searchEvents(swansea)
        assertEquals(2, service.searchCalls)
    }

    @Test
    fun searchEvents_differentFilter_requestsAgain() = scope.runTest {
        val repository = repository()
        repository.searchEvents(swansea)
        repository.searchEvents(swansea.copy(keyword = "jazz"))
        assertEquals(2, service.searchCalls)
    }

    @Test
    fun searchEvents_forceRefresh_skipsCache() = scope.runTest {
        val repository = repository()
        repository.searchEvents(swansea)
        repository.searchEvents(swansea, forceRefresh = true)
        assertEquals(2, service.searchCalls)
    }

    @Test
    fun searchEvents_noEmbeddedSection_returnsEmptyList() = scope.runTest {
        service.events = emptyList()
        val outcome = repository().searchEvents(swansea)
        assertTrue((outcome as Outcome.Success).data.isEmpty())
    }

    @Test
    fun searchEvents_noConnection_returnsOffline() = scope.runTest {
        service.failure = IOException("no route to host")
        assertEquals(Outcome.Failure(DataError.OFFLINE), repository().searchEvents(swansea))
    }

    @Test
    fun searchEvents_http401_returnsInvalidApiKey() = scope.runTest {
        service.failure = httpError(401)
        assertEquals(Outcome.Failure(DataError.INVALID_API_KEY), repository().searchEvents(swansea))
    }

    @Test
    fun searchEvents_http429_returnsRateLimited() = scope.runTest {
        service.failure = httpError(429)
        assertEquals(Outcome.Failure(DataError.RATE_LIMITED), repository().searchEvents(swansea))
    }

    @Test
    fun searchEvents_http500_returnsServer() = scope.runTest {
        service.failure = httpError(500)
        assertEquals(Outcome.Failure(DataError.SERVER), repository().searchEvents(swansea))
    }

    @Test
    fun searchEvents_missingApiKey_failsWithoutCallingService() = scope.runTest {
        val outcome = repository(hasApiKey = false).searchEvents(swansea)
        assertEquals(Outcome.Failure(DataError.INVALID_API_KEY), outcome)
        assertEquals(0, service.searchCalls)
    }

    @Test
    fun getEvent_afterSearch_isServedFromCache() = scope.runTest {
        val repository = repository()
        repository.searchEvents(swansea)
        val outcome = repository.getEvent("e1")
        assertEquals("The Harbour Lights", (outcome as Outcome.Success).data.name)
        assertEquals(0, service.detailCalls)
    }

    @Test
    fun getEvent_notCached_loadsById() = scope.runTest {
        val outcome = repository().getEvent("e9")
        assertEquals("Loaded by id", (outcome as Outcome.Success).data.name)
        assertEquals(1, service.detailCalls)
    }

    @Test
    fun getEvent_http404_returnsNotFound() = scope.runTest {
        service.failure = httpError(404)
        assertEquals(Outcome.Failure(DataError.NOT_FOUND), repository().getEvent("gone"))
    }
}
