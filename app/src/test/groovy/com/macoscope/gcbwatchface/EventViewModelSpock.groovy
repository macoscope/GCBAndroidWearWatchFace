package com.macoscope.gcbwatchface

import spock.lang.Specification

class EventViewModelSpock extends Specification {

    def "EventViewModel.getMinutes() should return proper amount of minutes to defined event start date"() {
        given:
            long now = System.currentTimeMillis()
            int minutes = 25
            long timeOffset = 1000 * 60 * minutes
            Date date = new Date(now + timeOffset);
            EventViewModel objectUnderTest = new EventViewModel("Event Name", date, "Room 1")
        when:
            long offset = objectUnderTest.getMinutesToEvent(now)
        then:
            offset == minutes
    }

    def "EventViewModel.getHourMinutes() should return proper minutes amount for defined event date"() {
        given:
            Date date = new Date("Tue, 22 Jun 1999 12:16:00 GMT-0500")
            EventViewModel objectUnderTest = new EventViewModel("Event Name", date, "Room 1")
        when:
            int minutes = objectUnderTest.getHourMinutes()
        then:
            minutes == 16
    }
}
