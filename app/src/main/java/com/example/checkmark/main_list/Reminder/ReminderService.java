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
import java.util.Calendar;               // 日期时间处理
import java.util.List;                   // 列表集合
import java.util.Map;                    // 键值对集合

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
    private static final String TAG = "ReminderService";
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
        // 1. 获取电源锁唤醒设备(确保屏幕亮起)
        // FULL_WAKE_LOCK: 完全唤醒(屏幕+键盘+CPU)
        // ACQUIRE_CAUSES_WAKEUP: 强制亮屏
//        PowerManager.WakeLock wakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE))
//                .newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
//                        "CheckMark:ReminderLock");
//        wakeLock.acquire(10 * 1000); // 持有锁10秒

        try {
            // 2. 从Intent中获取任务数据
            int taskId = intent.getIntExtra("taskId", -1);
            String taskName = intent.getStringExtra("taskName");
            String reminderTime = intent.getStringExtra("reminderTime");

            // 3. 验证数据有效性
            if (taskId != -1 && taskName != null) {
                Log.d(TAG, "收到提醒: 任务ID=" + taskId + ", 名称=" + taskName);

                // 4. 使用NotificationHelper发送通知
                String message = "您当日"+ taskName+"还未完成！提醒时间："+ (reminderTime != null ? reminderTime : "");
                NotificationHelper.sendReminderNotification(context, taskId, taskName, message);

                // 5. 如果需要重复提醒，设置第二天的闹钟
                if (shouldReschedule(context, taskId) && reminderTime != null) {
                    Log.d(TAG, "设置次日重复提醒");
                    setExactAlarm(context, taskId, taskName, reminderTime);
                }
            }
        } finally {
            // 6. 释放电源锁(重要！避免耗电)
            //wakeLock.release();
        }
    }

    /**
     * 设置精确闹钟
     *
     * @param context 上下文
     * @param taskId 任务唯一ID
     * @param taskName 任务名称
     * @param reminderTime 提醒时间(HH:mm格式)
     *
     * 原理：根据Android版本使用最佳闹钟设置方法
     *      Android 6.0+: setAlarmClock(最可靠)
     *      Android 4.4+: setExact
     *      旧版本: set
     */
    public static void setExactAlarm(Context context, int taskId, String taskName, String reminderTime) {
        // 1. 检查时间格式是否有效
        if (TextUtils.isEmpty(reminderTime)) {
            Log.w(TAG, "提醒时间为空，无法设置");
            return;
        }

        try {
            // 2. 解析时间字符串为Calendar对象
            Calendar calendar = parseTime(reminderTime);
            Log.d(TAG, "设置提醒时间: " + calendar.getTime());

            // 3. 获取系统闹钟服务
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            // 4. 创建触发时发送的Intent(携带任务数据)
            Intent intent = new Intent(context, ReminderService.class);
            intent.putExtra("taskId", taskId);
            intent.putExtra("taskName", taskName);
            intent.putExtra("reminderTime", reminderTime);

            // 5. 创建PendingIntent(系统稍后执行的Intent)
            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    taskId, // 使用taskId作为请求码(确保唯一)
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // 6. 根据Android版本选择最佳设置方法
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 最佳实践：setAlarmClock会显示在系统闹钟中，且最可靠
                AlarmClockInfo info = new AlarmClockInfo(
                        calendar.getTimeInMillis(),
                        getOpenAppIntent(context)); // 点击闹钟时打开应用的Intent
                am.setAlarmClock(info, pi);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // 次优方案：精确闹钟
                am.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
            } else {
                // 兼容旧版本：普通闹钟
                am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
            }

            Log.d(TAG, "成功设置提醒: " + taskName);
        } catch (Exception e) {
            Log.e(TAG, "设置提醒失败", e);
        }
    }

    /**
     * 取消已设置的闹钟
     *
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

    /**
     * 解析时间字符串(HH:mm)为Calendar对象
     *
     * @param time 时间字符串(HH:mm格式)
     * @return 包含设置时间的Calendar对象
     * @throws Exception 时间格式错误时抛出
     */
    private static Calendar parseTime(String time) throws Exception {
        // 1. 分割小时和分钟
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        // 2. 获取当前时间的Calendar实例
        Calendar calendar = Calendar.getInstance();

        // 3. 设置指定的小时和分钟
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // 4. 如果设置的时间已过去，则设置为明天同一时间
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        return calendar;
    }

    /**
     * 创建点击系统闹钟时打开应用的Intent
     */
    private static PendingIntent getOpenAppIntent(Context context) {
        // 创建打开MainList的Intent
        Intent intent = new Intent(context, MainList.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        return PendingIntent.getActivity(
                context,
                0, // 固定请求码
                intent,
                PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * 检查任务是否需要重复提醒
     *
     * @param context 上下文
     * @param taskId 任务ID
     * @return 是否需要重新设置提醒
     */
    private boolean shouldReschedule(Context context, int taskId) {
        // 1. 从SharedPreferences获取任务数据
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String tasksJson = sp.getString(TASKS_KEY, "[]");

        try {
            // 2. 解析JSON数据为任务列表
            Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> tasks = new Gson().fromJson(tasksJson, type);

            // 3. 查找对应任务
            for (Map<String, Object> task : tasks) {
                if (getTaskId(task) == taskId) {
                    // 4. 检查needsReminder字段
                    return task.containsKey("needsReminder") && (boolean) task.get("needsReminder");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "检查重新设置时出错", e);
        }
        return false;
    }

    /**
     * 从任务Map中安全获取任务ID
     */
    private int getTaskId(Map<String, Object> task) {
        try {
            Object idObj = task.get("id");
            if (idObj instanceof Double) return ((Double) idObj).intValue();
            if (idObj instanceof Integer) return (Integer) idObj;
        } catch (Exception e) {
            Log.e(TAG, "获取任务ID出错", e);
        }
        return -1;
    }
}