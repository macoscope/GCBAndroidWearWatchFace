package com.macoscpoe.gcbmodel

import spock.lang.Specification

import java.util.concurrent.TimeUnit

class EventSpock extends Specification {
    def "Should return proper amount of minutes to upcoming event"() {
        given:
        long now = System.currentTimeMillis()
        long timeAfter25minFromNow = now + TimeUnit.MINUTES.toMillis(25);
        Event.TimeProvider timeProvider = Stub(Event.TimeProvider)
        timeProvider.currentTime() >> now
        Event objectUnderTest = new Event(1L, timeAfter25minFromNow, "", "");
        objectUnderTest.setTimeProvider(timeProvider)
        when:
        long minutesToEvent = objectUnderTest.getMinutesToEvent(now)
        then:
        minutesToEvent == 25
    }

    def "Event with startDate set to 1 minute from now should be valid"() {
        given:
        long now = System.currentTimeMillis();
        long futureDate = now + TimeUnit.MINUTES.toMillis(1);
        Event.TimeProvider timeProvider = Stub(Event.TimeProvider)
        timeProvider.currentTime() >> now
        Event objectUnderTest = new Event(1L, futureDate, "", "");
        objectUnderTest.setTimeProvider(timeProvider)
        expect:
        objectUnderTest.isValid()
    }

    def "Event should  be invalid if startDate is current date"() {
        given:
        long now = System.currentTimeMillis();
        Event.TimeProvider timeProvider = Stub(Event.TimeProvider)
        timeProvider.currentTime() >> now
        Event objectUnderTest = new Event(1L, now, "", "");
        objectUnderTest.setTimeProvider(timeProvider)
        expect:
        !objectUnderTest.isValid()
    }

    def "Event should  be invalid if startDate is past date"() {
        given:
        long now = System.currentTimeMillis();
        long past = now - 1;
        Event.TimeProvider timeProvider = Stub(Event.TimeProvider)
        timeProvider.currentTime() >> now
        Event objectUnderTest = new Event(1L, past, "", "");
        objectUnderTest.setTimeProvider(timeProvider)
        expect:
        !objectUnderTest.isValid()
    }

}
