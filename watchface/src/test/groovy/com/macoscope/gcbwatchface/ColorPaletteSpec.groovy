package com.macoscope.gcbwatchface

import spock.lang.Shared
import spock.lang.Specification

class ColorPaletteSpec extends Specification {
    @Shared
    ColorPalette objectUnderTest = new ColorPalette()

    void setup() {
        objectUnderTest.colorLipstick = 0xe3325c
        objectUnderTest.colorGreenBlue = 0x22daa8
    }

    def "should return colorLipstick color for 30 minutes"() {
        expect:
            objectUnderTest.getHourColor(30) == 0xe3325c
    }

    def "should return colorGreenBlue color for 0 minutes"() {
        expect:
            objectUnderTest.getHourColor(0) == 0x22daa8
    }

    def "should return colorGreenBlue color for 29 minutes"() {
        expect:
            objectUnderTest.getHourColor(29) == 0x22daa8
    }

    def "should return colorLipstick color for 59 minutes"() {
        expect:
            objectUnderTest.getHourColor(59) == 0xe3325c
    }

}
