package com.macoscope.gcbwtachface;

import android.app.Application;

import com.evernote.android.job.JobManager;
import com.macoscope.gcbwtachface.service.SyncJobCreator;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        JobManager.create(this).addJobCreator(new SyncJobCreator());
    }
}
