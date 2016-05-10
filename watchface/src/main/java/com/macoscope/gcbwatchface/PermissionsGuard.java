package com.macoscope.gcbwatchface;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

public class PermissionsGuard {
    public static final int PERMISSION_REQUEST_READ_CALENDAR = 1;

    public boolean isCalendarPermissionsGranted(Context appContext) {
        return ActivityCompat.checkSelfPermission(appContext,
                Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestReadCalendarPermissions(Activity activity) {
        // On 23+ (M+) devices, GPS permission not granted. Request permission.
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.READ_CALENDAR},
                PERMISSION_REQUEST_READ_CALENDAR);
    }
}
