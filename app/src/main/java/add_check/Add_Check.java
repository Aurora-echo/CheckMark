package add_check;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.checkmark.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import DateBaseRoom.Task;
import DateBaseRoom.TaskDao;

public class Add_Check extends AppCompatActivity {

    private TextInputEditText etTaskName;
    private CheckBox cbDailyReminder;
    private LinearLayout layoutTimePicker;
    private Button btnSelectTime;
    private TextView tvSelectedTime;
    private Button btnConfirm;
    private static final String TAG = "Log.Add_Check";
    private TaskDao taskDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_check);

        //任务名称输入框
        etTaskName = findViewById(R.id.et_task_name);
        //每日提醒勾选框
        cbDailyReminder = findViewById(R.id.cb_daily_reminder);
        //时间选择器
        layoutTimePicker = findViewById(R.id.layout_time_picker);
        //时间选择按钮
        btnSelectTime = findViewById(R.id.btn_select_time);

        tvSelectedTime = findViewById(R.id.tv_selected_time);
        //提交按钮
        btnConfirm = findViewById(R.id.btn_confirm);

        // “每日未完成提醒”勾选框状态变化监听
        cbDailyReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutTimePicker.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                tvSelectedTime.setText("");
            }
        });

        // 时间选择按钮点击事件
        btnSelectTime.setOnClickListener(v -> showTimePickerDialog());

        // 确认按钮点击事件
        btnConfirm.setOnClickListener(v -> saveTaskToSharedPreferences());
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    tvSelectedTime.setText(timeStr);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        ).show();
    }

    private void saveTaskToSharedPreferences() {
        String taskName = etTaskName.getText().toString().trim();
        if (taskName.isEmpty()) {
            Toast.makeText(this, "请正确填写任务名称", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean needsReminder = cbDailyReminder.isChecked();
        String timeStr = tvSelectedTime.getText().toString(); // 获取显示的 "HH:mm" 格式字符串
        Date reminderTime = null;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        reminderTime = sdf.parse(timeStr);
        Log.i(TAG, "needsReminder: " + needsReminder);
        Log.i(TAG, "reminderTime: " + reminderTime);
        // 检查是否需要提醒但未选择时间
        if (needsReminder && reminderTime.isEmpty()) {
            Toast.makeText(this, "不是说要提醒吗，时间呢？", Toast.LENGTH_SHORT).show();
            return;
        }

        //插入新任务
        Task task = new Task(taskName,needsReminder,reminderTime);
        taskDao.insertTask(task);
        Toast.makeText(this, "事件已记录", Toast.LENGTH_SHORT).show();
        finish();
    }

    private List<Map<String, Object>> getTasksFromSharedPreferences() {
        String tasksJson = sp.getString(TASKS_KEY, "[]");
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        return new Gson().fromJson(tasksJson, type);
    }

    private void saveTasksToSharedPreferences(List<Map<String, Object>> tasks) {
        String tasksJson = new Gson().toJson(tasks);
        sp.edit().putString(TASKS_KEY, tasksJson).apply();
    }

    // 添加完成记录的方法（可在其他地方调用）
    public static void addCompletionRecord(SharedPreferences sp, int taskIndex) {
        List<Map<String, Object>> tasks = new Gson().fromJson(
                sp.getString(TASKS_KEY, "[]"),
                new TypeToken<List<Map<String, Object>>>(){}.getType()
        );

        if (taskIndex >= 0 && taskIndex < tasks.size()) {
            Map<String, Object> task = tasks.get(taskIndex);
            List<String> records = (List<String>) task.get("completionRecords");
            if (records == null) {
                records = new ArrayList<>();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            records.add(sdf.format(new Date()));

            task.put("completionRecords", records);
            new Gson().toJson(tasks);
            sp.edit().putString(TASKS_KEY, new Gson().toJson(tasks)).apply();
        }
    }

    public synchronized int getNextTaskId(Context context) {
        SharedPreferences sp = context.getSharedPreferences("task_ids", MODE_PRIVATE);
        int id = sp.getInt("last_id", 0) + 1;
        sp.edit().putInt("last_id", id).apply();
        return id;
    }
}