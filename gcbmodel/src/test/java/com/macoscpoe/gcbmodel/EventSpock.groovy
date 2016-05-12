package com.macoscpoe.gcbmodel

import spock.lang.Specification

import java.util.concurrent.TimeUnit

class EventSpock extends Specification {
    def "Event.getMinutesToEvent() should return proper amount of minutes to upcoming event"() {
        given:
        long now = System.currentTimeMillis()
        long timeAfter25minFromNow = now + TimeUnit.MINUTES.toMillis(25);
        Event objectUnderTest = new Event(1L, timeAfter25minFromNow, "", "");
        when:
        long minutesToEvent = objectUnderTest.getMinutesToEvent(now)
        then:
        minutesToEvent == 25
    }

    def "Event with startDate set to 1 minute from now should be valid"() {
        given:
        long futureDate = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        Event objectUnderTest = new Event(1L, futureDate, "", "");
        then:
        objectUnderTest.isValid()
    }

    def "Event should  be invalid if startDate is current date"() {
        given:
        long now = System.currentTimeMillis();
        Event objectUnderTest = new Event(1L, now, "", "");
        then:
        !objectUnderTest.isValid()
    }

    def "Event should  be invalid if startDate is past date"() {
        given:
        long past = System.currentTimeMillis() - 1;
        Event objectUnderTest = new Event(1L, past, "", "");
        then:
        !objectUnderTest.isValid()
    }
}
