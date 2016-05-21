package com.macoscope.gcbmodel

import spock.lang.Specification

import java.util.concurrent.TimeUnit

class EventSpock extends Specification {
    def "Should return proper amount of minutes to upcoming event"() {
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
        long now = System.currentTimeMillis();
        long futureDate = now + TimeUnit.MINUTES.toMillis(1);
        Event objectUnderTest = new Event(1L, futureDate, "", "");
        expect:
        objectUnderTest.isValidRelativeTo(now)
    }

    def "Event should  be invalid if startDate is current date"() {
        given:
        long now = System.currentTimeMillis();
        Event objectUnderTest = new Event(1L, now, "", "");
        expect:
        !objectUnderTest.isValidRelativeTo(now)
    }

    def "Event should  be invalid if startDate is past date"() {
        given:
        long now = System.currentTimeMillis();
        long past = now - 1;
        Event objectUnderTest = new Event(1L, past, "", "");
        expect:
        !objectUnderTest.isValidRelativeTo(now)
    }

}
