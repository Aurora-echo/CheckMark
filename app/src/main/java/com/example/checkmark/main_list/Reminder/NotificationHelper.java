package com.example.checkmark.main_list.Reminder;

// 导入必要的Android类
import android.app.Notification;          // 通知类
import android.app.NotificationChannel;   // 通知渠道类(Android 8.0+)
import android.app.NotificationManager;  // 通知管理器
import android.app.PendingIntent;        // 延迟执行的Intent
import android.content.Context;          // 上下文(全局应用信息)
import android.content.Intent;           // 用于启动Activity
import android.os.Build;                 // 用于检查Android版本
import android.util.Log;                 // 日志工具

// 导入兼容库
import androidx.core.app.NotificationCompat; // 兼容旧版的通知构建器

// 导入项目资源
import com.example.checkmark.R;          // 项目资源(R.drawable等)
import check_record.CheckRecord;         // 详情页面Activity

/**
 * 通知帮助类 - 封装所有通知相关操作
 * 功能：
 * 1. 创建通知渠道(Android 8.0+必需)
 * 2. 发送待办事项提醒通知
 * 3. 处理通知点击跳转到详情页
 */
public class NotificationHelper {

    // 通知渠道ID - 所有通知都发到这个渠道
    private static final String CHANNEL_ID = "reminder_channel";

    // 基础通知ID - 加上任务ID确保每个通知有唯一ID
    private static final int NOTIFICATION_ID_BASE = 1001;

    // 日志标签 - 用于调试
    private static final String TAG = "Log.NotificationHelper";

    /**
     * 发送提醒通知 - 主功能方法
     *
     * @param context   上下文(通常传入Activity或Application)
     * @param taskId    任务唯一ID(数据库或SP中的ID)
     * @param taskName  任务名称(显示在通知中)
     * @param message   提醒内容(显示在通知中)
     *
     * 原理：构建通知 -> 设置点击行为 -> 显示通知
     * 实例：当闹钟触发时调用此方法显示提醒
     *       sendReminderNotification(this, 123, "健身", "该去健身房了");
     */
    public static void sendReminderNotification(Context context, int taskId, String taskName, String message) {
        // 1. 创建点击通知后要跳转的Intent
        // 跳转到CheckRecord(详情页)，并带上任务数据
        Intent intent = new Intent(context, CheckRecord.class);
        intent.putExtra("id", taskId);       // 传递任务ID
        intent.putExtra("taskName", taskName); // 传递任务名称

        // 设置标志位：在新任务栈中打开，并清除之前的同类Activity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // 2. 创建PendingIntent(系统稍后执行的Intent)
        // 参数说明：
        // context - 上下文
        // taskId - 请求码(确保唯一性)
        // intent - 要执行的Intent
        // FLAG_UPDATE_CURRENT - 如果已存在则更新
        // FLAG_IMMUTABLE - Android 12+要求(禁止修改)
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 3. 确保通知渠道已创建(Android 8.0+需要)
        createNotificationChannel(context);

        // 4. 构建通知对象(使用兼容库支持旧版本)
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.checkmack) // 小图标(状态栏显示)
                .setContentTitle("任务提醒")     // 通知标题
                .setContentText(message)           // 通知内容
                .setPriority(NotificationCompat.PRIORITY_HIGH) // 高优先级(弹出+声音)
                .setContentIntent(pendingIntent)    // 设置点击后的行为
                .setAutoCancel(true)               // 点击后自动消失
                .build();

        // 5. 获取系统通知服务
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 6. 显示通知
        // NOTIFICATION_ID_BASE + taskId 确保每个任务通知ID唯一
        manager.notify(NOTIFICATION_ID_BASE + taskId, notification);

        Log.d(TAG, "已发送通知: 任务ID=" + taskId + ", 内容=" + message);
    }

    /**
     * 创建通知渠道(Android 8.0+必需)
     *
     * @param context 上下文
     *
     * 原理：Android 8.0开始，通知必须分类到渠道中
     *      渠道创建后用户可以在系统设置中管理
     * 实例：在应用启动时调用一次即可
     */
    private static void createNotificationChannel(Context context) {
        Log.i(TAG, "检查Android版本，准备创建通知渠道");

        // 只有Android 8.0(Oreo)及以上版本需要渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 渠道名称(用户可见)
            CharSequence name = "任务未完成提醒";
            // 渠道描述(用户可见)
            String description = "对用户设置了每日提醒的任务进行提醒";
            // 重要性级别(高:弹出通知+发出声音)
            int importance = NotificationManager.IMPORTANCE_HIGH;

            // 创建渠道对象
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // 可选设置(根据需要取消注释)
            // channel.enableLights(true);       // 启用指示灯
            // channel.setLightColor(Color.RED); // 设置指示灯颜色
            channel.enableVibration(true);   // 启用震动
            channel.setVibrationPattern(new long[]{100,200,100}); // 震动模式

            // 获取通知管理器
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            // 创建渠道
            manager.createNotificationChannel(channel);

            Log.i(TAG, "通知渠道创建完成: " + CHANNEL_ID);
        } else {
            Log.i(TAG, "Android版本低于8.0，无需创建通知渠道");
        }
    }
}