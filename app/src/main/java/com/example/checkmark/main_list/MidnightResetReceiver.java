package com.example.checkmark.main_list;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import DateBaseRoom.AppDatabase;
import DateBaseRoom.TaskDao;

public class MidnightResetReceiver extends BroadcastReceiver {

    private static final String TAG = "Log.MidnightResetReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,("【MidnightResetReceiver 】0点重置任务状态"));
        AppDatabase db = AppDatabase.getInstance(context);
        TaskDao taskDao = db.taskDao();

        new Thread(() -> {
            // 重置所有任务状态为未完成(0)
            taskDao.resetAllTaskStatus();
            Log.d(TAG, "【MidnightResetReceiver】所有任务状态已重置");
        }).start();
    }
}