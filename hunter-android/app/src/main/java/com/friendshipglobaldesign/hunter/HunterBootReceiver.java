package com.friendshipglobaldesign.hunter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class HunterBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(intent==null||!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))return;
        SharedPreferences prefs=context.getSharedPreferences("hunter_prefs",Context.MODE_PRIVATE);
        if(prefs.getBoolean("alerts_enabled",false))HunterJobs.schedule(context);
    }
}
