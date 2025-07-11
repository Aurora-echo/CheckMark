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

public class Add_Check extends AppCompatActivity {

    private TextInputEditText etTaskName;
    private CheckBox cbDailyReminder;
    private LinearLayout layoutTimePicker;
    private Button btnSelectTime;
    private TextView tvSelectedTime,tvTitleName;
    private Button btnConfirm;

    private SharedPreferences sp;
    private static final String SP_NAME = "CheckListInfo";
    private static final String TASKS_KEY = "tasks";
    private static final String TAG = "Log.Add_Check";
    private int clickCount = 0; //点击计时器


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
        tvTitleName = findViewById(R.id.tv_card_title);

        //tvTitleName的点击事件，连续点击五次，触发导入数据
        tvTitleName.setOnClickListener(v -> {
            clickCount++;
            if (clickCount >= 5) {
                clickCount = 0;
                loadTask();
                Toast.makeText(this, "开始导入数据", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "还需点击 " + (5 - clickCount) + " 次", Toast.LENGTH_SHORT).show();
                // 3秒内不继续点击就重置计数器
                tvTitleName.postDelayed(() -> clickCount = 0, 3000);
            }
        });

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
        String reminderTime = needsReminder ? tvSelectedTime.getText().toString() : "";
        Log.i(TAG, "needsReminder: " + needsReminder);
        Log.i(TAG, "reminderTime: " + reminderTime);
        // 检查是否需要提醒但未选择时间
        if (needsReminder && reminderTime.isEmpty()) {
            Toast.makeText(this, "不是说要提醒吗，时间呢？", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建任务对象
        Map<String, Object> task = new HashMap<>();
        task.put("id",getNextTaskId(this));
        task.put("name", taskName);
        task.put("needsReminder", needsReminder);
        task.put("reminderTime", reminderTime);
        task.put("completionRecords", new ArrayList<String>());

        // 获取现有任务列表
        List<Map<String, Object>> tasks = getTasksFromSharedPreferences();
        tasks.add(task);

        // 保存到SharedPreferences
        saveTasksToSharedPreferences(tasks);

        // 保存数据成功后
        Intent resultIntent = new Intent();
        setResult(RESULT_OK, resultIntent);  // 必须设置RESULT_OK
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

    /**
     * 导入原始数据的方法
     * */
    private void loadTask(){
        List<Map<String, Object>> testTasks = new ArrayList<>();
        // 清空现有ID计数器（从1开始）
        //SharedPreferences idSp = getSharedPreferences("task_ids", MODE_PRIVATE);
        //idSp.edit().putInt("last_id", 0).apply();

        // 任务1：吃维生素（有提醒）
        Map<String, Object> task1 = new HashMap<>();
        task1.put("id", getNextTaskId(this));
        task1.put("name", "吃维生素");
        task1.put("needsReminder", true);
        task1.put("reminderTime", "21:00");
        task1.put("completionRecords", Arrays.asList(
                "2025-06-17 15:13:15", "2025-06-18 14:18:48", "2025-06-19 15:02:55",
                "2025-06-20 14:12:58", "2025-06-21 23:55:38", "2025-06-22 23:02:09",
                "2025-06-23 13:30:19", "2025-06-24 13:44:22", "2025-06-25 13:55:45",
                "2025-06-26 15:58:15", "2025-06-27 08:53:54", "2025-06-29 14:59:59",
                "2025-06-30 21:13:19", "2025-07-01 14:37:26", "2025-07-02 18:07:43",
                "2025-07-03 14:07:37", "2025-07-04 11:42:56", "2025-07-06 17:25:17",
                "2025-07-07 09:07:26", "2025-07-08 13:39:37", "2025-07-09 14:06:02",
                "2025-07-10 13:33:56"
        ));
        testTasks.add(task1);

        // 任务2：运动（无提醒）
        Map<String, Object> task2 = new HashMap<>();
        task2.put("id", getNextTaskId(this));
        task2.put("name", "运动");
        task2.put("needsReminder", false);
        task2.put("reminderTime", "");
        task2.put("completionRecords", Arrays.asList(
                "2025-06-26 15:58:21", "2025-07-02 18:07:49", "2025-07-03 16:59:12",
                "2025-07-04 16:45:54", "2025-07-06 23:14:33", "2025-07-10 15:41:21"
        ));
        testTasks.add(task2);

        // 任务3：放松（无提醒）
        Map<String, Object> task3 = new HashMap<>();
        task3.put("id", getNextTaskId(this));
        task3.put("name", "放松");
        task3.put("needsReminder", false);
        task3.put("reminderTime", "");
        task3.put("completionRecords", Arrays.asList(
                "2025-06-22 23:02:05", "2025-06-30 21:13:24", "2025-07-06 23:14:37"
        ));
        testTasks.add(task3);

        // 保存测试数据
        saveTasksToSharedPreferences(testTasks);

        Toast.makeText(this, "原始数据已导入", Toast.LENGTH_SHORT).show();

        // 打印验证数据
        Log.d(TAG, "当前任务数: " + testTasks.size());
        for (Map<String, Object> task : testTasks) {
            Log.d(TAG, String.format(Locale.getDefault(),
                    "任务: %s (ID:%d), 完成次数: %d",
                    task.get("name"),
                    ((Number)task.get("id")).intValue(),
                    ((List<?>)task.get("completionRecords")).size()));
        }
    }

}