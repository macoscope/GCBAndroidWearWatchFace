package com.macoscope.gcbwatchface.drawer

import spock.lang.Specification
import spock.lang.Unroll

class FaceDrawerSpec extends Specification {
    @Unroll
    def "for #minutes minutes should return #expectedAngle angle"() {
        given:
            FaceDrawer objectUnderTest = new FaceDrawer()
        when:
            float angle = objectUnderTest.getSwapAngle(minutes)
        then:
            angle == expectedAngle
        where:
            minutes || expectedAngle
            0       || -330
            4       || -330
            27      || -180
            30      || 180
            55      || 330
    }
}
