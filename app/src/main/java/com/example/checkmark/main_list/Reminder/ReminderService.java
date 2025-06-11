package com.example.checkmark.main_list.Reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReminderService extends BroadcastReceiver {
    private static final String TAG = "Log.ReminderService--------->>>>";
    private static final String SP_NAME = "CheckListInfo";
    private static final String TASKS_KEY = "tasks";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "ReminderService.onReceive() 被调用");
        checkAndSendReminders(context);
    }

    public static void scheduleDailyCheck(Context context) {
        Log.d(TAG, "到通知这儿了");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderService.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // 设置每天凌晨12:05检查一次
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1); // 加一分钟，确保不会在当前时间立即触发
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 5);
        calendar.set(Calendar.SECOND, 0);

        // 如果已经过了这个时间，就设置到明天
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent);
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent);
        }

        Log.d(TAG, "Daily check scheduled");
    }

    private void checkAndSendReminders(Context context) {
        Log.d(TAG, "到通知这儿了");
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String tasksJson = sp.getString(TASKS_KEY, "[]");
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> tasks = new Gson().fromJson(tasksJson, type);

        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        for (Map<String, Object> task : tasks) {
            boolean needsReminder = task.containsKey("needsReminder") && (boolean) task.get("needsReminder");
            if (!needsReminder) continue;

            String reminderTime = (String) task.get("reminderTime");
            String taskName = (String) task.get("name");

            // 检查是否已过提醒时间且未完成
            if (isTimePassed(reminderTime, currentHour, currentMinute)) {
                List<String> records = (List<String>) task.get("completionRecords");
                boolean isCompletedToday = checkCompletionToday(records);

                if (!isCompletedToday) {
                    sendNotification(context, taskName);
                }
            }
        }
    }

    private boolean isTimePassed(String timeStr, int currentHour, int currentMinute) {
        Log.d(TAG, "到通知这儿了");
        String[] parts = timeStr.split(":");
        int reminderHour = Integer.parseInt(parts[0]);
        int reminderMinute = Integer.parseInt(parts[1]);

        if (currentHour > reminderHour) return true;
        if (currentHour == reminderHour && currentMinute >= reminderMinute) return true;
        return false;
    }

    private boolean checkCompletionToday(List<String> records) {
        Log.d(TAG, "到通知这儿了");
        if (records == null || records.isEmpty()) return false;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        for (String record : records) {
            if (record.startsWith(today)) {
                return true;
            }
        }
        return false;
    }

    private void sendNotification(Context context, String taskName) {
        Log.d(TAG, "到通知这儿了");
        // 这里实现通知发送逻辑
        NotificationHelper.showNotification(
                context,
                "待办事项提醒",
                "您的待办事项 \"" + taskName + "\" 还未完成，请及时处理！"
        );
    }
}