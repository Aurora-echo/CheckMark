package com.example.checkmark.main_list;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "Log.BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                MainList.scheduleMidnightReset(context);
                Log.d(TAG, "【BootReceiver】设备重启后已重新设置午夜重置任务");
            }, 10000);
        }
    }
}