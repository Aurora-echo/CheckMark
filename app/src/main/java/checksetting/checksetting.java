package checksetting;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.checkmark.R;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

public class checksetting extends AppCompatActivity {
        private TextInputEditText etTaskName;
        private SwitchCompat switchReminder;
        private LinearLayout timePickerContainer;
        private Button btnTimePicker;
        private TextView tvSelectedTime;
        private String selectedTime = null;
        private static final String SP_NAME = "CheckListInfo";
        private static final String TASKS_KEY = "tasks";
        private SharedPreferences sp;
        private static String TAG = "Log.checksetting";
        private double taskId;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_checksetting);

            // 初始化视图
            initViews();

            // 设置初始数据
            setupInitialData();

            // 设置监听器
            setupListeners();
        }

        private void initViews() {
            etTaskName = findViewById(R.id.et_task_name);
            switchReminder = findViewById(R.id.switch_reminder);
            timePickerContainer = findViewById(R.id.time_picker_container);
            btnTimePicker = findViewById(R.id.btn_time_picker);
            tvSelectedTime = findViewById(R.id.tv_selected_time);

            findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
            findViewById(R.id.btn_save).setOnClickListener(v -> saveSettings());
        }

        private void setupInitialData() {
            // 从Intent获取数据
            Intent intent = getIntent();
            taskId = intent.getDoubleExtra("id", -1);
            Log.i(TAG,"从事项详情给的intent取出来的id，taskId："+taskId);
            etTaskName.setText(intent.getStringExtra("taskName"));
            boolean needsReminder = intent.getBooleanExtra("needsReminder", false);
            switchReminder.setChecked(needsReminder);
            timePickerContainer.setVisibility(needsReminder ? View.VISIBLE : View.GONE);
            String time = intent.getStringExtra("reminderTime");
            if (time != null) {
                selectedTime = time;
                tvSelectedTime.setText(time);
            }
        }

        private void setupListeners() {
            // 开关监听
            switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
                timePickerContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                if (!isChecked) {
                    selectedTime = null;
                    tvSelectedTime.setText("未选择");
                }
            });
            // 时间选择按钮
            btnTimePicker.setOnClickListener(v -> showTimePickerDialog());
        }

        private void showTimePickerDialog() {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                        tvSelectedTime.setText(selectedTime);
                    },
                    // 默认显示当前时间
                    Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                    Calendar.getInstance().get(Calendar.MINUTE),
                    true // 24小时制
            );
            timePickerDialog.show();
        }

        private void saveSettings() {
            String name = etTaskName.getText().toString().trim();
            boolean needsReminder = switchReminder.isChecked();
            String time = needsReminder ? selectedTime : null;

            if (name.isEmpty()) {
                Toast.makeText(this, "名称都不写保存什么？", Toast.LENGTH_SHORT).show();
                return;
            }

            if (needsReminder && time == null) {
                Toast.makeText(this, "需要提醒又不选时间？", Toast.LENGTH_SHORT).show();
                return;
            }
            // 保存逻辑
            saveSettingsToSPfile(taskId,name,needsReminder,time);
            finish();
        }

    private void saveSettingsToSPfile(double id, String name, boolean needsReminder, String time) {
        Log.i(TAG,"开始执行保存，保存的数据是：id:"+id+",name:"+name+",needsReminder:"+needsReminder+",time:"+time);
        // 获取 SharedPreferences
        sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        String tasksJson = sp.getString(TASKS_KEY, "[]");
        try {
            // 解析 JSON 数组
            JSONArray tasksArray = new JSONArray(tasksJson);
            Log.i(TAG,"解析出来的数组，tasksArray:"+tasksArray);
            // 遍历查找匹配 ID 的任务
            for (int i = 0; i < tasksArray.length(); i++) {
                JSONObject task = tasksArray.getJSONObject(i);
                if (task.getDouble("id") == id) {
                    // 更新任务属性
                    task.put("name", name);
                    task.put("needsReminder", needsReminder);

                    // 根据 needsReminder 设置 reminderTime
                    if (needsReminder) {
                        task.put("reminderTime", time);
                    } else {
                        task.put("reminderTime", "");
                    }
                    break; // 找到后退出循环
                }
            }
            // 保存修改后的数据回 SharedPreferences
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(TASKS_KEY, tasksArray.toString());
            editor.apply();
            Toast.makeText(this, "修改成功！", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.printStackTrace();
            // 处理 JSON 解析错误
            Toast.makeText(this, "修改失败，请联系大神", Toast.LENGTH_SHORT).show();
        }
    }
}