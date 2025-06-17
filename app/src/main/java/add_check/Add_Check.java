package add_check;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Add_Check extends AppCompatActivity {

    private TextInputEditText etTaskName;
    private CheckBox cbDailyReminder;
    private LinearLayout layoutTimePicker;
    private Button btnSelectTime;
    private TextView tvSelectedTime;
    private Button btnConfirm;

    private SharedPreferences sp;
    private static final String SP_NAME = "CheckListInfo";
    private static final String TASKS_KEY = "tasks";
    private static final String TAG = "Log.Add_Check";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_check);

        // 初始化SharedPreferences
        sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);

        // 初始化视图
        etTaskName = findViewById(R.id.et_task_name);
        cbDailyReminder = findViewById(R.id.cb_daily_reminder);
        layoutTimePicker = findViewById(R.id.layout_time_picker);
        btnSelectTime = findViewById(R.id.btn_select_time);
        tvSelectedTime = findViewById(R.id.tv_selected_time);
        btnConfirm = findViewById(R.id.btn_confirm);

        // 勾选框状态变化监听
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
            etTaskName.setError("请输入事项名称");
            return;
        }

        boolean needsReminder = cbDailyReminder.isChecked();
        String reminderTime = needsReminder ? tvSelectedTime.getText().toString() : "";

        // 检查是否需要提醒但未选择时间
        if (needsReminder && reminderTime.isEmpty()) {
            Toast.makeText(this, "请选择提醒时间", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建任务对象
        Map<String, Object> task = new HashMap<>();
        task.put("id",getNextTaskId(this));
        task.put("name", taskName);
        task.put("needsReminder", needsReminder);
        task.put("reminderTime", reminderTime);
        task.put("completionRecords", new ArrayList<String>()); // 用于记录完成时间点

        // 获取现有任务列表
        List<Map<String, Object>> tasks = getTasksFromSharedPreferences();
        tasks.add(task);

        // 保存到SharedPreferences
        saveTasksToSharedPreferences(tasks);

        // 保存数据成功后
        Intent resultIntent = new Intent();
        setResult(RESULT_OK, resultIntent);  // 必须设置RESULT_OK
        Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
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