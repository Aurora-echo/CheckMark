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

import DateBaseRoom.AppDatabase;
import DateBaseRoom.Task;
import DateBaseRoom.TaskCompletionDao;
import DateBaseRoom.TaskDao;
import TestAvtivity.TestDateActivity;
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
    List<Task> allTasks;
    private static final String TAG = "Log.MainList";

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

        //初始化数据库
        db = AppDatabase.getInstance(this);
        taskDao = db.taskDao();
        allTasks = taskDao.getAllTasks();

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
            startActivityForResult(new Intent(MainList.this, Add_Check.class), 1);
        });
    }

    /**
     * 打开详情页面
     * @param position 点击的位置
     */
    private void openCheckRecord(int position) {
        Task taskDetai=taskDao.getTaskById(CheckList.get(position).getId());
        Intent intent = new Intent(this, CheckRecord.class);
        intent.putExtra("id", taskDetai.taskId);
        intent.putExtra("position", position);
        intent.putExtra("taskName", taskDetai.title);
        startActivityForResult(intent,1);
    }

    /**
     * 显示完成状态提示
     */
    private void showCompletionStatus(int position) {
        String message = CheckList.get(position).isCompleted() ? "当日已完成~" : "今天还没完成哦~";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 从SharedPreferences加载任务列表,并显示页面上
     */
    private void loadTasksFromSharedPreferences() {
        // 获取今天日期用于检查完成状态
        CheckList.clear();
        for (Task task : allTasks) {
            // 获取任务ID
            int id = task.taskId;
            // 检查今日是否完成
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
        for (Task task : allTasks) {
            //有needsReminder字段且值为true时，设置闹钟
            if (task.needRemind) {
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
        for (Task task : allTasks) {
            int id = task.taskId;
            if (id != -1) {
                ReminderService.cancelAlarm(this, id);
            }
        }
    }
}