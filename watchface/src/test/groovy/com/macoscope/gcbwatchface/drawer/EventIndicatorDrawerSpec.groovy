package com.macoscope.gcbwatchface.drawer

import spock.lang.Specification
import spock.lang.Unroll

class EventIndicatorDrawerSpec extends Specification {
    @Unroll
    def "for #minutes minutes should return #expectedAngle angle"() {
        given:
            EventIndicatorDrawer objectUnderTest = new EventIndicatorDrawer()
        when:
            float angle = objectUnderTest.getStartAngle(minutes)
        then:
            angle == expectedAngle
        where:
            minutes || expectedAngle
            0       || -60
            4       || -60
            27      || 90
            30      || 120
            55      || 270
    }
}
