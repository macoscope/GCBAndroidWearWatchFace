package com.macoscope.gcbwatchface

import com.eccyan.optional.Optional
import com.macoscope.gcbmodel.Event
import spock.lang.Specification

class EventsManagerSpec extends Specification {
    def "should return first upcoming event with time in future"() {
        given:
        long now = System.currentTimeMillis()
        List<Event> events = new ArrayList<>();
        events.add(new Event(0, now - 1, "", ""))
        events.add(new Event(1, now, "", ""))
        events.add(new Event(2, now + 1, "", ""))
        events.add(new Event(3, now + 2, "", ""))
        EventsManager objectUnderTest = new EventsManager()
        objectUnderTest.eventsLoaded(events)
        when:
        Optional<Event> event = objectUnderTest.getUpcomingEvent(now)
        then:
        event.isPresent()
        event.get().id == 2
    }

    def "should return first upcoming event from valid emails"() {
        given:
        long now = System.currentTimeMillis()
        List<Event> events = new ArrayList<>();
        events.add(new Event(0, now + 1, "", ""))
        events.add(new Event(1, now + 2, "", ""))
        events.add(new Event(2, now + 3, "", ""))
        events.add(new Event(3, now + 4, "", ""))
        EventsManager objectUnderTest = new EventsManager()
        objectUnderTest.eventsLoaded(events)
        when:
        Optional<Event> event = objectUnderTest.getUpcomingEvent(now)
        then:
        event.isPresent()
        event.get().id == 0
    }

    def "should return empty upcoming event when no events loaded"() {
        given:
        long now = System.currentTimeMillis()
        EventsManager objectUnderTest = new EventsManager()
        objectUnderTest.eventsLoaded(new ArrayList<>())
        when:
        Optional<Event> event = objectUnderTest.getUpcomingEvent(now)
        then:
        !event.isPresent()
    }

    def "should return empty event from invalid events list"() {
        given:
        long now = System.currentTimeMillis()
        List<Event> events = new ArrayList<>();
        events.add(new Event(0, now - 3, "", ""))
        events.add(new Event(1, now - 2, "", ""))
        events.add(new Event(2, now - 1, "", ""))
        events.add(new Event(3, now, "", ""))
        EventsManager objectUnderTest = new EventsManager()
        objectUnderTest.eventsLoaded(events)
        when:
        Optional<Event> event = objectUnderTest.getUpcomingEvent(now)
        then:
        !event.isPresent()
    }

    def "should return -1 comparing event from past with event from future"() {
        given:
        long now = System.currentTimeMillis()
        Event past = new Event(0, now - 1, "", "")
        Event future = new Event(1, now + 1, "", "")
        expect:
        EventsManager.eventComparator.compare(past, future) == -1
    }

    def "should return 1 comparing event from future with event from past"() {
        given:
        long now = System.currentTimeMillis()
        Event past = new Event(0, now - 1, "", "")
        Event future = new Event(1, now + 1, "", "")
        expect:
        EventsManager.eventComparator.compare(future, past) == 1
    }

    def "should return 0 comparing events with the same start date"() {
        given:
        long now = System.currentTimeMillis()
        Event eventOne = new Event(0, now, "", "")
        Event eventTwo = new Event(1, now, "", "")
        expect:
        EventsManager.eventComparator.compare(eventOne, eventTwo) == 0
    }
}
