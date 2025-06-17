package checksetting;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
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

import java.util.Calendar;
import java.util.Locale;

public class checksetting extends AppCompatActivity {
        private TextInputEditText etTaskName;
        private SwitchCompat switchReminder;
        private LinearLayout timePickerContainer;
        private Button btnTimePicker;
        private TextView tvSelectedTime;
        private String selectedTime = null;

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
                Toast.makeText(this, "请输入事项名称", Toast.LENGTH_SHORT).show();
                return;
            }

            if (needsReminder && time == null) {
                Toast.makeText(this, "请选择提醒时间", Toast.LENGTH_SHORT).show();
                return;
            }

            // 保存逻辑...
//            Intent result = new Intent();
//            result.putExtra("taskName", name);
//            result.putExtra("needsReminder", needsReminder);
//            result.putExtra("reminderTime", time);
//            setResult(RESULT_OK, result);
            Toast.makeText(this, "还没实现保存逻辑", Toast.LENGTH_SHORT).show();
            finish();
        }
}