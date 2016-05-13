package com.macoscope.gcbwtachface.service;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import java.util.concurrent.TimeUnit;

public class SyncJobScheduler {

    public void scheduleNewSyncJob(long intervalInMinutes, long calendarId){
        cancelAllScheduledJobs();

        PersistableBundleCompat extras = new PersistableBundleCompat();
        extras.putLong(SyncJob.KEY_MINUTES, intervalInMinutes);
        extras.putLong(SyncJob.KEY_CALENDAR_ID, calendarId);

        int jobId = new JobRequest.Builder(SyncJob.TAG)
                .setPeriodic(TimeUnit.MINUTES.toMillis(intervalInMinutes))
                .setPersisted(true)
                .setExtras(extras)
                .build()
                .schedule();

    }

    public void cancelAllScheduledJobs(){
        JobManager.instance().cancelAllForTag(SyncJob.TAG);
    }

}
