package com.macoscope.gcbwatchface;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class HourFormatter {
    private Calendar hourCalendar;

    public HourFormatter(){
        hourCalendar = new GregorianCalendar();
    }
    /**
     * Format calendar hour to String. If time is half past function return hour with +1h offset
     * @param calendar with set time
     * @return  given calendar's hour when minutes < 30, otherwise following hour
     *          for 00:30 function return "1"
     *          for 12:16 function return "12"
     */
    public String getHourToDisplay(Calendar calendar) {
        hourCalendar.setTimeInMillis(calendar.getTimeInMillis());
        int hour = hourCalendar.get(Calendar.HOUR_OF_DAY);
        int minutes = hourCalendar.get(Calendar.MINUTE);
        if (minutes >= 30) {
            hourCalendar.add(Calendar.HOUR_OF_DAY, 1);
            hour = hourCalendar.get(Calendar.HOUR_OF_DAY);
        }
        return String.format("%d", hour);
    }

    public void setTimeZone(TimeZone timeZone){
        hourCalendar.setTimeZone(timeZone);
    }

}
