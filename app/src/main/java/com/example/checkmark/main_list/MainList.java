package com.example.checkmark.main_list;

import static checksetting.checksetting.observeOnce;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.checkmark.R;
import com.example.checkmark.main_list.Reminder.ReminderService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import DateBaseRoom.AppDatabase;
import DateBaseRoom.Task;
import DateBaseRoom.TaskCompletionDao;
import DateBaseRoom.TaskDao;
import add_check.Add_Check;
import check_record.CheckRecord;

/**
 * 主列表Activity - 显示所有待办事项
 * 功能：显示任务列表、跳转到详情页、管理提醒设置
 */
public class MainList extends AppCompatActivity {

    // 适配器待办事项列表数据
    private List<CheckItem> CheckList = new ArrayList<>();
    // 列表适配器
    private CheckAdapter CheckAdapter;
    //数据库初始化对象
    private AppDatabase db;
    //task表实体化
    private TaskDao taskDao;
    //所有任务列表
    List<Task> allTasks=new ArrayList<>();
    private static final String TAG = "Log.MainList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_list);
        Log.d(TAG, "【onCreate】Activity创建");

        requestNotificationPermission(); // 检查通知权限
        requestAutoStartPermission();   // 检查自启动权限

        cancelMidnightReset(this); // 取消之前的0点重置任务(用于重置之前的检查)
        scheduleMidnightReset(this); // 0点重置任务

        // 获取当前日期和时间显示在左上角和右上角
        // 在Activity中设置日期时间
        TextView tvDay = findViewById(R.id.tv_day);
        TextView tvMonth = findViewById(R.id.tv_month);
        TextView tvYear = findViewById(R.id.tv_year);
        TextView tvTime = findViewById(R.id.tv_time);

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.ENGLISH);
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.ENGLISH);
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.ENGLISH);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

        tvDay.setText(dayFormat.format(new Date()));
        tvMonth.setText(monthFormat.format(new Date()));
        tvYear.setText(yearFormat.format(new Date()));
        tvTime.setText(timeFormat.format(new Date()));

        //初始化数据库
        db = AppDatabase.getInstance(this);
        taskDao = db.taskDao();
        taskDao.getAllTasks().observe(this, tasks -> {
            allTasks = tasks;
            Log.i(TAG, "【onCreate】allTasks：" + tasks);
            // 检查精确闹钟权限(Android 12+需要)
            checkExactAlarmPermission();
            // 初始化界面
            initViews();
            //加载数据
            loadTasksFromDatabase();
        });
    }

    /**
     * 检查精确闹钟权限
     * 原理：Android 12+需要特殊权限才能设置精确闹钟
     */
    private void checkExactAlarmPermission() {
        Log. i(TAG, "【checkExactAlarmPermission】检查精确闹钟权限");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                // 跳转到系统设置页面请求权限
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
            }
        }
    }

    /**
     * 初始化所有视图组件
     */
    private void initViews() {
        // 初始化RecyclerView
        RecyclerView recyclerView = findViewById(R.id.todo_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 设置适配器
        CheckAdapter = new CheckAdapter(CheckList, new CheckAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                openCheckRecord(position);
            }

            @Override
            public void onStatusClick(int position) {
                showCompletionStatus(position);
            }
        });

        recyclerView.setAdapter(CheckAdapter);

        // 设置添加按钮
        ImageButton btnAdd = findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(v -> {
            startActivityForResult(new Intent(MainList.this, Add_Check.class), 1);
        });
    }

    /**
     * 打开任务详情页面
     * @param position 列表中的位置索引
     */
    private void openCheckRecord(int position) {
        // 打印调用栈，用于调试（原有代码保留）
        Log.d(TAG, "【openCheckRecord】openCheckRecord called from: ", new Throwable());

        // 使用observeOnce方法替代普通observe（核心修改点）
        // 这样可以确保LiveData回调只执行一次，避免重复触发
        observeOnce(taskDao.getTaskById(CheckList.get(position).getId()), this, taskDetai -> {
            // 保留原有日志输出
            Log.i(TAG,"【openCheckRecord】LiveData正在运行...");

            // 以下保持原有逻辑完全不变
            Intent intent = new Intent(this, CheckRecord.class);
            intent.putExtra("id", taskDetai.taskId);
            intent.putExtra("taskName", taskDetai.title);
            intent.putExtra("needRemind",taskDetai.needRemind);

            // 保持原有时间处理逻辑
            if(taskDetai.needRemind){
                //需要把date转为long放进intent（原有注释保留）
                long remindTime_long = taskDetai.remindTime.getTime();
                intent.putExtra("remindTime",remindTime_long);
            }

            // 保持原有跳转逻辑
            startActivityForResult(intent,1);
        });
    }

    /**
     * 显示完成状态提示
     */
    private void showCompletionStatus(int position) {
        String message = CheckList.get(position).isCompleted() ? "当日已完成" : "今天还没完成";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 从数据库加载任务列表,并显示页面上
     */
    private void loadTasksFromDatabase() {
        Log.i(TAG,"【loadTasksFromDatabase】开始从allTasks加载任务显示");
        if (allTasks == null) {
            Log.w(TAG, "警告：allTasks为null");
            return;
        }
        // 获取今天日期用于检查完成状态
        CheckList.clear();
        for (Task task : allTasks) {
            // 获取任务ID
            int id = task.taskId;
            //该处有问题，需要修改，Task表不会改变status状态
            boolean isCompletedToday = (task.status==0) ? false:true ;
            // 添加到列表
            CheckList.add(new CheckItem(id, task.title, isCompletedToday));
        }
        // 刷新列表
        if (CheckAdapter != null) {
            CheckAdapter.notifyDataSetChanged();
        }
        // 设置提醒
        scheduleAllReminders();
    }

    /**
     * 为所有需要提醒的任务设置闹钟
     */
    private void scheduleAllReminders() {
        for (Task task : allTasks) {
            //有needsReminder字段且值为true时 + status状态为0（未完成的）——> 任务设置闹钟
            if (task.needRemind && task.status == 0) {
                Log.i(TAG,"【scheduleAllReminders】找到需要提醒的任务，taskId：" + task.taskId+",taskname:"+task.title);
                if (task.taskId != -1 && task.title != null && task.remindTime != null) {
                    ReminderService.setExactAlarm(this, task.taskId, task.title, task.remindTime);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Log.i(TAG, "【onActivityResult】onActivityResult: 返回，执行刷新页面");
            loadTasksFromDatabase(); // 刷新列表
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "【onResume】 执行，取消所有提醒，重新设置所有提醒");
        super.onResume();
        if(!allTasks.isEmpty()){
            cancelAllReminders();
            scheduleAllReminders(); // 确保提醒设置正确
        }
        loadTasksFromDatabase(); // 刷新列表
    }

    /**
     * 取消所有提醒
     */
    private void cancelAllReminders() {
        for (Task task : allTasks) {
            int id = task.taskId;
            if (id != -1) {
                ReminderService.cancelAlarm(this, id);
            }
        }
    }

    /**
     * 检查通知权限
     * */
    private boolean areNotificationsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            return manager.areNotificationsEnabled();
        }
        return true; // Android 13以下默认有权限
    }

    /**
     * 引导用户去配置通知权限
     * */
    private void requestNotificationPermission() {
        Log.d(TAG, "【requestNotificationPermission】检查通知权限");
        if (!areNotificationsEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("开启通知权限")
                    .setMessage("请允许通知权限，确保及时接收提醒（锁屏/横幅）")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        // 跳转到应用通知设置页
                        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                        startActivity(intent);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }else{
            Log.d(TAG, "【requestNotificationPermission】通知权限已打开");
        }
    }

    /**
     * 引导用户开始自启动
     * */
    private void requestAutoStartPermission() {
        Log.d(TAG, "【requestAutoStartPermission】检查自启动权限");
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        if (manufacturer.contains("huawei") || manufacturer.contains("xiaomi") || manufacturer.contains("oppo")) {
            new AlertDialog.Builder(this)
                    .setTitle("允许自启动")
                    .setMessage("请在系统设置中允许应用自启动和关联启动，避免提醒失效")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        // 跳转到品牌特定的自启动设置页
                        try {
                            Intent intent = new Intent();
                            if (manufacturer.contains("huawei")) {
                                intent.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"));
                            } else if (manufacturer.contains("xiaomi")) {
                                intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.appmanager.ApplicationsDetailsActivity"));
                            } else if (manufacturer.contains("oppo")) {
                                intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
                            }
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(this, "无法跳转，请手动在设置中开启", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    /**
     * 0点重置所有任务状态
     * */
    public static void scheduleMidnightReset(Context context) {
        Log.i(TAG,"【scheduleMidnightReset】0点重置所有任务状态定时任务");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, MidnightResetReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 24);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    /**
     * 取消可能存在的旧的定时任务，用于设置新任务前执行
     * */
    public static void cancelMidnightReset(Context context) {
        Log.i(TAG,"【requestAutoStartPermission】取消之前的任务");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, MidnightResetReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

}