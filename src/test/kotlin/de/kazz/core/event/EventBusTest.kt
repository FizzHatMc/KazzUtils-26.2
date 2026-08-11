package de.kazz.core.event

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for the annotation-driven [EventBus].
 */
class EventBusTest {

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        EventBus.clear()
    }

    private data class TestEvent(val value: String) : Event
    private data class AnotherEvent(val number: Int) : Event

    private class TestListener {
        val received = mutableListOf<Event>()

        @SubscribeEvent
        fun onTestEvent(event: TestEvent) {
            received.add(event)
        }

        @SubscribeEvent
        fun onAnotherEvent(event: AnotherEvent) {
            received.add(event)
        }
    }

    @Test
    fun `test register and post event`() {
        val listener = TestListener()
        EventBus.register(listener)

        val event = TestEvent("hello")
        EventBus.post(event)

        assertEquals(1, listener.received.size)
        assertEquals(event, listener.received[0])
    }

    @Test
    fun `test multiple listeners receive same event`() {
        val listener1 = TestListener()
        val listener2 = TestListener()
        EventBus.register(listener1)
        EventBus.register(listener2)

        EventBus.post(TestEvent("test"))

        assertEquals(1, listener1.received.size)
        assertEquals(1, listener2.received.size)
    }

    @Test
    fun `test unregister removes listener`() {
        val listener = TestListener()
        EventBus.register(listener)
        EventBus.unregister(listener)

        EventBus.post(TestEvent("should not be received"))

        assertTrue(listener.received.isEmpty())
    }

    @Test
    fun `test multiple event types`() {
        val listener = TestListener()
        EventBus.register(listener)

        EventBus.post(TestEvent("hello"))
        EventBus.post(AnotherEvent(42))

        assertEquals(2, listener.received.size)
        assertTrue(listener.received[0] is TestEvent)
        assertTrue(listener.received[1] is AnotherEvent)
    }

    @Test
    fun `test posting with no listeners returns false`() {
        val handled = EventBus.post(TestEvent("nobody listening"))
        assertFalse(handled)
    }

    @Test
    fun `test posting with listeners returns true`() {
        val listener = TestListener()
        EventBus.register(listener)

        val handled = EventBus.post(TestEvent("listening"))
        assertTrue(handled)
    }

    @Test
    fun `test clear removes all listeners`() {
        val listener = TestListener()
        EventBus.register(listener)
        EventBus.clear()

        EventBus.post(TestEvent("cleared"))
        assertTrue(listener.received.isEmpty())
    }
}