package com.example.checkmark.main_list.Reminder;

// 导入必要的Android类
import android.annotation.SuppressLint;
import android.app.AlarmManager;          // 闹钟管理服务
import android.app.AlarmManager.AlarmClockInfo; // 闹钟信息类
import android.app.PendingIntent;         // 延迟执行的Intent
import android.content.BroadcastReceiver; // 广播接收器基类
import android.content.Context;           // 上下文(全局应用信息)
import android.content.Intent;            // 用于启动组件
import android.content.SharedPreferences; // 轻量级数据存储
import android.os.Build;                  // 用于检查Android版本
import android.os.PowerManager;           // 电源管理(唤醒设备)
import android.text.TextUtils;            // 文本处理工具
import android.util.Log;                  // 日志工具

// 导入项目类
import com.example.checkmark.main_list.MainList; // 主界面
import com.google.gson.Gson;             // JSON解析库
import com.google.gson.reflect.TypeToken; // JSON类型转换

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Calendar;               // 日期时间处理
import java.util.Date;
import java.util.List;                   // 列表集合
import java.util.Locale;
import java.util.Map;                    // 键值对集合

import check_record.CheckRecord;

/**
 * 提醒服务 - 处理闹钟提醒的核心类
 * 功能：
 * 1. 接收系统闹钟触发广播
 * 2. 发送高优先级通知
 * 3. 管理精确闹钟的设置和取消
 * 4. 处理每日重复提醒
 */
public class ReminderService extends BroadcastReceiver {
    // 日志标签
    private static final String TAG = "Log.ReminderService";
    // SharedPreferences文件名(存储任务数据)
    private static final String SP_NAME = "CheckListInfo";
    // 存储任务列表的键名
    private static final String TASKS_KEY = "tasks";

    /**
     * 接收闹钟触发广播 - 主入口方法
     * 当设置的闹钟时间到达时，系统会调用此方法
     *
     * @param context 广播接收的上下文
     * @param intent 携带额外数据的Intent
     *
     * 原理：系统闹钟触发 -> 唤醒设备 -> 发送通知 -> 设置下次提醒
     * 实例：设置9:00的提醒 -> 9:00系统调用此方法
     */
    @SuppressLint("MissingPermission") // 忽略权限警告(已在manifest声明)
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // 2. 从Intent中获取任务数据
            int taskId = intent.getIntExtra("taskId", -1);
            String taskName = intent.getStringExtra("taskName");
            // 获取时间戳并转换为 Date
            long timeInMillis = intent.getLongExtra("reminderTime", -1);
            Date reminderTime = new Date(timeInMillis);

            // 3. 验证数据有效性
            if (taskId != -1 && taskName != null) {
                Log.d(TAG, "【onReceive】收到提醒: 任务ID=" + taskId + ", 名称=" + taskName);

                // 4. 使用NotificationHelper发送通知
                String message = taskName+" 任务还没有完成！不要忘记了！";
                NotificationHelper.sendReminderNotification(context, taskId, taskName, message);

                // 5. 如果需要重复提醒，设置第二天的闹钟
                if (reminderTime != null) {
                    Log.d(TAG, "【onReceive】设置次日重复提醒,下一个提醒时间为："+reminderTime);
                    setExactAlarm(context, taskId, taskName, reminderTime);
                } else {
                    Log.d(TAG, "【onReceive】不需要设置次日重复提醒");
                }
            }
        } catch (Exception e) {
            Log.e(TAG,"【onReceive】发送通知出错："+e.getMessage());
        }
    }


    /**
     * 设置精确的闹钟提醒
     *
     * @param context     上下文对象
     * @param taskId      任务ID（用于区分不同闹钟）
     * @param taskName    任务名称（用于日志和通知显示）
     * @param reminderTime 提醒时间（Date对象，只使用时分部分）
     */
    public static void setExactAlarm(Context context, int taskId, String taskName, Date reminderTime) {
        // 1. 记录方法调用日志
        Log.i(TAG, "【setExactAlarm】开始设置闹钟。任务ID:" + taskId + ", 任务名称:" + taskName +
                ", 原始提醒时间:" + reminderTime);

        // 2. 参数有效性检查
        if (reminderTime == null) {
            Log.w(TAG, "⚠️ 提醒时间为null，取消设置闹钟");
            return;
        }

        try {
            // 3. 创建Calendar实例并设置时间
            // 注意：这里只使用reminderTime的小时和分钟部分
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(reminderTime);

            // 4. 获取当前时间的Calendar实例用于比较
            Calendar now = Calendar.getInstance();

            // 5. 将提醒时间的年月日设置为当前日期
            // 这样我们只比较时间部分（时分秒）
            calendar.set(Calendar.YEAR, now.get(Calendar.YEAR));
            calendar.set(Calendar.MONTH, now.get(Calendar.MONTH));
            calendar.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            // 6. 确保秒和毫秒为0（精确到分钟）
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // 7. 智能日期调整：如果时间已过，设置为明天同一时间
            if (calendar.before(now)) {
                Log.d(TAG, "⏰ 提醒时间已过当前时间，自动调整为明天");
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            // 8. 记录最终设置的提醒时间
            Log.d(TAG, "✅ 最终设置的提醒时间: " + calendar.getTime());

            // 9. 获取系统闹钟服务
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "❌ 获取AlarmManager失败");
                return;
            }

            // 10. 创建启动广播的Intent
            Intent intent = new Intent(context, ReminderService.class);
            intent.putExtra("taskId", taskId);          // 传递任务ID
            intent.putExtra("taskName", taskName);      // 传递任务名称
            intent.putExtra("reminderTime", reminderTime.getTime()); // 传递原始时间戳

            // 11. 创建PendingIntent
            // 使用FLAG_UPDATE_CURRENT更新现有Intent
            // 使用FLAG_IMMUTABLE适配Android 12+
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskId,  // 使用taskId作为requestCode，确保唯一性
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // 12. 根据不同Android版本设置闹钟
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 12.1 Android 6.0+ 使用setAlarmClock，会在状态栏显示闹钟图标
                AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(
                        calendar.getTimeInMillis(),
                        getOpenAppPendingIntent(context)  // 点击闹钟通知时打开应用的Intent
                );
                alarmManager.setAlarmClock(info, pendingIntent);
                Log.d(TAG, "🔔 使用setAlarmClock API设置闹钟");
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // 12.2 Android 4.4+ 使用setExact
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                Log.d(TAG, "⏰ 使用setExact API设置闹钟");
            } else {
                // 12.3 旧版本使用set
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                Log.d(TAG, "🕰️ 使用set API设置闹钟");
            }

            // 13. 记录设置成功的日志
            Log.i(TAG, "🎉 成功设置闹钟 - 任务ID:" + taskId + ", 触发时间:" + calendar.getTime());

        } catch (Exception e) {
            // 14. 捕获并记录异常
            Log.e(TAG, "❌ 设置闹钟失败 - 任务ID:" + taskId, e);
        }
    }

    /**
     * 获取打开应用的PendingIntent
     */
    private static PendingIntent getOpenAppPendingIntent(Context context) {
        Intent intent = new Intent(context, CheckRecord.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * 取消已设置的闹钟
     * @param context 上下文
     * @param taskId 要取消的任务ID
     */
    public static void cancelAlarm(Context context, int taskId) {
        // 1. 获取系统闹钟服务
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // 2. 创建与设置时相同的Intent
        Intent intent = new Intent(context, ReminderService.class);

        // 3. 获取对应的PendingIntent(FLAG_NO_CREATE表示不创建新的)
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);

        // 4. 如果存在则取消
        if (pi != null) {
            am.cancel(pi);  // 取消闹钟
            pi.cancel();    // 取消PendingIntent
            Log.d(TAG, "已取消提醒: " + taskId);
        }
    }
}