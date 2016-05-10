package com.macoscope.gcbwatchface;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.view.View;

/**
 * Simple Activity for displaying Calendar Permission Rationale to user.
 */
public class CalendarPermissionActivity extends WearableActivity {

    private PermissionsGuard permissionsGuard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_watch_face_permission);
        setAmbientEnabled();
        permissionsGuard = new PermissionsGuard();
    }

    public void onClickEnablePermission(View view) {
        permissionsGuard.requestReadCalendarPermissions(this);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PermissionsGuard.PERMISSION_REQUEST_READ_CALENDAR) {
            if ((grantResults.length == 1)
                    && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                finish();
            }
        }
    }
}