package com.example.checkmark.main_list;

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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import TestAvtivity.TestDateActivity;
import add_check.Add_Check;
import check_record.CheckRecord;

/**
 * 主列表Activity - 显示所有待办事项
 * 功能：显示任务列表、跳转到详情页、管理提醒设置
 */
public class MainList extends AppCompatActivity {

    // 待办事项列表数据
    private List<CheckItem> CheckList = new ArrayList<>();
    // 列表适配器
    private CheckAdapter CheckAdapter;
    // 本地存储工具
    private SharedPreferences sp;
    // 常量定义
    private static final String SP_NAME = "CheckListInfo";
    private static final String TASKS_KEY = "tasks";
    private static final String TAG = "Log.MainList";
    List<Map<String, Object>> tasks; //一个包含了所有任务的列表

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_list);
        Log.d(TAG, "Activity创建");

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

        // 初始化本地存储
        sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);

        // 检查精确闹钟权限(Android 12+需要)
        checkExactAlarmPermission();

        // 初始化界面
        initViews();

        // 加载数据
        loadTasksFromSharedPreferences();
    }

    /**
     * 检查精确闹钟权限
     * 原理：Android 12+需要特殊权限才能设置精确闹钟
     */
    private void checkExactAlarmPermission() {
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
            //startActivityForResult(new Intent(MainList.this, Add_Check.class), 1);
            startActivityForResult(new Intent(MainList.this, TestDateActivity.class), 1);
        });

    }

    /**
     * 打开详情页面
     * @param position 点击的位置
     * 优化点：预先加载任务数据并传递，避免详情页重复解析
     */
    private void openCheckRecord(int position) {
        Map<String, Object> task = getTaskFromSharedPrefs(CheckList.get(position).getId());
        Intent intent = new Intent(this, CheckRecord.class);
        intent.putExtra("id", (double) task.get("id"));
        intent.putExtra("position", position);
        intent.putExtra("taskName", (String) task.get("name"));
        // 如果没找到任务数据，直接传递序列化后的JSON
        if (task != null) {
            Log.i(TAG,"TASK的类型："+task.getClass());
            intent.putExtra("taskData", new Gson().toJson(task));
        }
        startActivityForResult(intent,1);
    }

    /**
     * 从SharedPreferences获取单个任务数据
     * 传入taskid获取事项详细信息
     */
    private Map<String, Object> getTaskFromSharedPrefs(int taskId) {
        for (Map<String, Object> task : tasks) {
            Object idObj = task.get("id");
            int id = idObj instanceof Double ? ((Double) idObj).intValue() : (Integer) idObj;
            if (id == taskId) {
                Log.i(TAG,"点击了列表某项，获取到详情发过去,task:"+task);
                return task;
            }
        }
        return null;
    }

    /**
     * 显示完成状态提示
     */
    private void showCompletionStatus(int position) {
        String message = CheckList.get(position).isCompleted() ? "当日已完成~" : "今天还没完成哦~";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 从SharedPreferences加载任务列表
     */
    private void loadTasksFromSharedPreferences() {
        String tasksJson = sp.getString(TASKS_KEY, "[]");
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        tasks = new Gson().fromJson(tasksJson, type);
        // 获取今天日期用于检查完成状态
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
        CheckList.clear();
        for (Map<String, Object> task : tasks) {
            // 解析任务ID
            Object idObj = task.get("id");
            int id = idObj instanceof Double ? ((Double) idObj).intValue() : (Integer) idObj;
            // 检查今日是否完成
            boolean isCompletedToday = checkIfCompletedToday(task, today);
            // 添加到列表
            CheckList.add(new CheckItem(id, (String) task.get("name"), isCompletedToday));
        }
        // 刷新列表
        if (CheckAdapter != null) {
            CheckAdapter.notifyDataSetChanged();
        }
        // 设置提醒
        scheduleAllReminders();
    }

    /**
     * 检查任务今天是否已完成
     */
    private boolean checkIfCompletedToday(Map<String, Object> task, String today) {
        List<String> records = (List<String>) task.get("completionRecords");
        if (records != null) {
            for (String record : records) {
                if (record.startsWith(today)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 为所有需要提醒的任务设置闹钟
     */
    private void scheduleAllReminders() {
        String tasksJson = sp.getString(TASKS_KEY, "[]");
        List<Map<String, Object>> tasks = new Gson().fromJson(tasksJson,
                new TypeToken<List<Map<String, Object>>>(){}.getType());

        for (Map<String, Object> task : tasks) {
            //有needsReminder字段且值为true时，设置闹钟
            if (task.containsKey("needsReminder") && (boolean) task.get("needsReminder")) {
                //拿到事项的id
                Object idObj = task.get("id");
                int id = idObj instanceof Double ? ((Double) idObj).intValue() : (Integer) idObj;
                //拿到事项的名称
                String name = (String) task.get("name");
                //拿到事项的提醒时间
                String time = (String) task.get("reminderTime");

                if (id != -1 && name != null && time != null) {
                    ReminderService.setExactAlarm(this, id, name, time);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Log.i(TAG, "onActivityResult: 返回，执行刷新页面");
            loadTasksFromSharedPreferences(); // 刷新列表
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume: 执行，取消所有提醒，重新设置所有提醒");
        super.onResume();
        cancelAllReminders();
        scheduleAllReminders(); // 确保提醒设置正确
        loadTasksFromSharedPreferences(); // 刷新列表
    }

    /**
     * 取消所有提醒
     */
    private void cancelAllReminders() {
        String tasksJson = sp.getString(TASKS_KEY, "[]");
        List<Map<String, Object>> tasks = new Gson().fromJson(tasksJson,
                new TypeToken<List<Map<String, Object>>>(){}.getType());

        for (Map<String, Object> task : tasks) {
            Object idObj = task.get("id");
            int id = idObj instanceof Double ? ((Double) idObj).intValue() : (Integer) idObj;
            if (id != -1) {
                ReminderService.cancelAlarm(this, id);
            }
        }
    }
}