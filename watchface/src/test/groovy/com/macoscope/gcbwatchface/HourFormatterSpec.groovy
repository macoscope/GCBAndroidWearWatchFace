package com.macoscope.gcbwatchface

import com.macoscope.gcbwatchface.formatter.HourFormatter
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class HourFormatterSpec extends Specification {

    @Shared
    Calendar calendar = new GregorianCalendar()

    @Unroll
    def 'should return formatted hour without +1h offset for #hourOfDay:#minute'() {
        given:
            calendar.set(2016, 3, 28, hourOfDay, minute)
            HourFormatter objectUnderTest = new HourFormatter()
        when:
            String hourToDisplay = objectUnderTest.getHourToDisplay(calendar)
        then:
            hourToDisplay == expectedHour
        where:
            hourOfDay | minute || expectedHour
            0         | 12     || "0"
            1         | 0      || "1"
            15        | 29     || "15"
    }

    @Unroll
    def 'should return formatted hour with +1h offset for #hourOfDay:#minute'() {
        given:
            calendar.set(2016, 3, 28, hourOfDay, minute)
            HourFormatter objectUnderTest = new HourFormatter()
        when:
            String hourToDisplay = objectUnderTest.getHourToDisplay(calendar)
        then:
            hourToDisplay == expectedHour
        where:
            hourOfDay | minute || expectedHour
            7         | 30     || "8"
            1         | 31     || "2"
            24        | 59     || "1"
    }
}
