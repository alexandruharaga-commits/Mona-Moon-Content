package com.friendshipglobaldesign.hunter;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

public final class HunterJobs {
    public static final int WATCH_JOB_ID=42042;
    private static final long PERIOD_MS=15L*60L*1000L;
    private HunterJobs(){}

    public static boolean schedule(Context context){
        try{
            JobScheduler scheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if(scheduler==null)return false;
            JobInfo info=new JobInfo.Builder(WATCH_JOB_ID,new ComponentName(context,HunterWatchService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD_MS)
                .setPersisted(false)
                .build();
            return scheduler.schedule(info)==JobScheduler.RESULT_SUCCESS;
        }catch(Throwable t){return false;}
    }

    public static void cancel(Context context){
        try{
            JobScheduler scheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if(scheduler!=null)scheduler.cancel(WATCH_JOB_ID);
        }catch(Throwable ignored){}
    }
}
