package com.macoscope.gcbwatchface.service;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

public class SyncJobCreator implements JobCreator {
    @Override
    public Job create(String tag) {
        switch (tag){
            case SyncJob.TAG:{
                return new SyncJob();
            }
            default: {
                return null;
            }
        }
    }
}
