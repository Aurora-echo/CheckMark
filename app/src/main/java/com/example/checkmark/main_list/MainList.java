package com.example.checkmark.main_list;

// 导入必要的Android和第三方库
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.checkmark.R;
import com.example.checkmark.main_list.Reminder.NotificationHelper;
import com.example.checkmark.main_list.Reminder.ReminderService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import add_check.Add_Check;
import check_record.CheckRecord;

/**
 * 主列表Activity，显示所有待办事项
 * 主要功能：
 * 1. 显示待办事项列表
 * 2. 管理待办事项的完成状态
 * 3. 设置提醒功能
 * 4. 跳转到添加和详情页面
 */
public class MainList extends AppCompatActivity {

    // 待办事项列表数据
    private List<CheckItem> CheckList = new ArrayList<>();
    // 列表适配器
    private CheckAdapter CheckAdapter;
    // SharedPreferences用于本地存储数据
    private SharedPreferences sp;
    // SharedPreferences文件名
    private static final String SP_NAME = "CheckListInfo";
    // 存储待办事项的键名
    private static final String TASKS_KEY = "tasks";
    // 日志标签
    private static final String TAG = "Log.MainList";

    /**
     * Activity创建时调用
     * 初始化界面和基本功能
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置布局文件
        setContentView(R.layout.activity_main_list);
        Log.d(TAG, "MainList.onCreate() 执行");

        // 创建通知渠道（使用NotificationHelper中的方法）
        // 原理：Android 8.0+需要为通知创建渠道，否则通知不会显示
        //NotificationHelper.createNotificationChannel(this);

        // 初始化SharedPreferences
        // 原理：SharedPreferences是Android提供的轻量级存储，用于保存简单数据
        sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);

        // 从本地存储加载待办事项数据
        loadTasksFromSharedPreferences();

        // 检查并请求精确闹钟权限（Android 12+需要）
        // 原理：Android 12开始需要特殊权限才能设置精确闹钟
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                // 跳转到系统设置页面请求权限
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        // 为所有需要提醒的任务设置闹钟
        scheduleAllReminders();

        // 初始化RecyclerView（列表视图）
        setupRecyclerView();

        // 设置添加按钮点击事件
        setupAddButton();
    }

    /**
     * 初始化RecyclerView和适配器
     * 原理：RecyclerView是Android显示列表的高效组件
     */
    private void setupRecyclerView() {
        // 找到布局中的RecyclerView
        RecyclerView recyclerView = findViewById(R.id.todo_list);
        // 设置布局管理器（线性垂直排列）
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 创建适配器并设置点击事件
        CheckAdapter = new CheckAdapter(CheckList, new CheckAdapter.OnItemClickListener() {
            /**
             * 点击项目时跳转到详情页面
             * @param position 点击的位置
             */
            @Override
            public void onItemClick(int position) {
                // 创建跳转意图
                Intent intent = new Intent(MainList.this, CheckRecord.class);
                // 传递数据：任务ID、位置和名称
                intent.putExtra("id", CheckList.get(position).getId());
                intent.putExtra("position", position);
                intent.putExtra("taskName", CheckList.get(position).getText());
                // 启动详情Activity
                startActivity(intent);
            }

            /**
             * 点击状态图标时显示完成状态
             * @param position 点击的位置
             */
            @Override
            public void onStatusClick(int position) {
                if (CheckList.get(position).isCompleted()) {
                    Toast.makeText(MainList.this, "当日已完成~", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainList.this, "今天还没完成哦~", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 为RecyclerView设置适配器
        recyclerView.setAdapter(CheckAdapter);
    }

    /**
     * 设置添加按钮点击事件
     */
    private void setupAddButton() {
        // 找到添加按钮
        ImageButton btnAdd = findViewById(R.id.btn_add);
        // 设置点击监听器
        btnAdd.setOnClickListener(v -> {
            // 创建跳转到添加页面的意图
            Intent intent = new Intent(MainList.this, Add_Check.class);
            // 启动Activity并期待返回结果
            startActivityForResult(intent, 1);
        });
    }

    /**
     * 处理从其他Activity返回的结果
     * @param requestCode 请求码，用于区分不同的请求
     * @param resultCode 结果码，表示操作是否成功
     * @param data 返回的数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 检查是否是从添加页面返回且操作成功
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // 重新加载数据并刷新列表
            loadTasksFromSharedPreferences();
            CheckAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 从SharedPreferences加载待办事项数据
     * 原理：从本地存储读取JSON格式的数据并解析为对象列表
     */
    private void loadTasksFromSharedPreferences() {
        Log.i(TAG, "开始从SP文件中读取数据...");

        // 清空当前列表
        CheckList.clear();

        // 从SharedPreferences读取JSON字符串，如果没有数据则返回空数组JSON
        String tasksJson = sp.getString(TASKS_KEY, "[]");
        Log.d(TAG, "从SP读取的原始JSON: " + tasksJson);

        // 定义Gson解析的类型（List<Map<String, Object>>）
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        // 使用Gson解析JSON字符串为Java对象
        List<Map<String, Object>> tasks = new Gson().fromJson(tasksJson, type);

        // 创建日期格式化器，用于获取今天的日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        // 遍历所有任务数据
        for (Map<String, Object> task : tasks) {
            // 获取任务ID（兼容不同数字类型）
            Object idObj = task.get("id");
            int checkid = idObj instanceof Double
                    ? ((Double) idObj).intValue()
                    : (Integer) idObj;

            // 获取任务名称
            String name = (String) task.get("name");
            // 获取完成记录列表
            List<String> records = (List<String>) task.get("completionRecords");

            // 检查今天是否已完成
            boolean isCompletedToday = false;
            if (records != null) {
                for (String record : records) {
                    // 检查是否有以今天日期开头的记录
                    if (record.startsWith(today)) {
                        isCompletedToday = true;
                        break;
                    }
                }
            }

            Log.d(TAG, "id:" + checkid + "加载任务-名称: " + name + ", 今日完成状态: " + isCompletedToday);
            // 创建CheckItem对象并添加到列表
            CheckList.add(new CheckItem(checkid, name, isCompletedToday));
        }
    }

    /**
     * Activity恢复时调用
     * 用于刷新数据和重新设置提醒
     */
    @Override
    protected void onResume() {
        super.onResume();
        // 取消所有旧的提醒
        cancelAllReminders();
        // 重新设置所有提醒
        scheduleAllReminders();
        // 重新加载数据
        loadTasksFromSharedPreferences();
        // 通知适配器数据已更新
        if (CheckAdapter != null) {
            CheckAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 为所有需要提醒的任务设置闹钟
     * 原理：遍历所有任务，为设置了提醒的任务创建精确闹钟
     */
    private void scheduleAllReminders() {
        Log.i(TAG, "开始判断并为所有需要提醒的任务设置闹钟...");

        // 从SharedPreferences获取任务列表
        String tasksJson = sp.getString(TASKS_KEY, "[]");
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> tasks = new Gson().fromJson(tasksJson, type);

        // 遍历所有任务
        for (Map<String, Object> task : tasks) {
            try {
                // 检查任务是否需要提醒
                if (task.containsKey("needsReminder") && (boolean) task.get("needsReminder")) {
                    // 获取任务信息
                    int taskId = getTaskIdFromMap(task);
                    String taskName = (String) task.get("name");
                    String reminderTime = (String) task.get("reminderTime");

                    // 验证必要数据是否存在
                    if (taskId != -1 && taskName != null && reminderTime != null) {
                        Log.d(TAG, "需要为任务设置提醒 - ID:" + taskId +
                                ", 名称:" + taskName +
                                ", 时间:" + reminderTime);

                        // 调用ReminderService设置精确闹钟
                        ReminderService.setExactAlarm(
                                MainList.this,
                                taskId,
                                taskName,
                                reminderTime
                        );
                    } else {
                        Log.w(TAG, "任务数据不完整，跳过设置 - ID:" + taskId +
                                ", 名称:" + taskName +
                                ", 时间:" + reminderTime);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "设置任务提醒时出错", e);
            }
        }
    }

    /**
     * 取消所有任务的提醒
     * 原理：遍历所有任务，取消对应的闹钟
     */
    private void cancelAllReminders() {
        Log.d(TAG, "开始取消所有任务提醒...");

        // 从SharedPreferences获取任务列表
        String tasksJson = sp.getString(TASKS_KEY, "[]");
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> tasks = new Gson().fromJson(tasksJson, type);

        // 遍历所有任务
        for (Map<String, Object> task : tasks) {
            try {
                // 获取任务ID
                int taskId = getTaskIdFromMap(task);
                if (taskId != -1) {
                    // 调用ReminderService取消闹钟
                    ReminderService.cancelAlarm(MainList.this, taskId);
                    Log.d(TAG, "已取消任务ID " + taskId + " 的提醒");
                }
            } catch (Exception e) {
                Log.e(TAG, "取消任务提醒时出错", e);
            }
        }
    }

    /**
     * 从Map中安全获取任务ID
     * @param task 包含任务数据的Map
     * @return 任务ID，如果获取失败返回-1
     */
    private int getTaskIdFromMap(Map<String, Object> task) {
        try {
            Object idObj = task.get("id");
            // 处理不同类型的数字（Gson可能将整数转为Double）
            if (idObj instanceof Double) {
                return ((Double) idObj).intValue();
            } else if (idObj instanceof Integer) {
                return (Integer) idObj;
            }
        } catch (Exception e) {
            Log.e(TAG, "获取任务ID出错", e);
        }
        return -1;
    }
}