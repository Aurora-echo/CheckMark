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
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.example.checkmark.R;
import com.example.checkmark.main_list.MainList;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import DateBaseRoom.AppDatabase;
import DateBaseRoom.Task;
import DateBaseRoom.TaskDao;
import check_record.CheckRecord;

public class checksetting extends AppCompatActivity {
        private TextInputEditText etTaskName;
        private SwitchCompat switchReminder;
        private LinearLayout timePickerContainer;
        private Button btnTimePicker;
        private TextView tvSelectedTime;
        private String selectedTime = null;
        private static String TAG = "Log.checksetting";
        private int taskId;
        private TaskDao taskDao;
        private AppDatabase db;
        Date timeDate = null; // 用于存储解析后的时间

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_checksetting);

            //数据库初始化
            db = AppDatabase.getInstance(this);
            taskDao = db.taskDao();

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

            findViewById(R.id.btn_delete).setOnClickListener(v -> deleteSaveToSPfile(taskId));
            findViewById(R.id.btn_save).setOnClickListener(v -> saveSettings());
        }

        private void setupInitialData() {
            // 从Intent获取数据
            Intent intent = getIntent();
            taskId = intent.getIntExtra("id", -1);
            Log.i(TAG,"【setupInitialData】从事项详情给的intent取出来的id，taskId："+taskId);
            etTaskName.setText(intent.getStringExtra("taskName"));
            boolean needsReminder = intent.getBooleanExtra("needsReminder", false);
            if (needsReminder){
                switchReminder.setChecked(needsReminder);
                timePickerContainer.setVisibility(View.VISIBLE);
            }
            long long_reminderTime = intent.getLongExtra("long_reminderTime",-1);
            if(long_reminderTime != -1){
                String time = new Date(long_reminderTime).toString();
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
                        Log.i(TAG,"【showTimePickerDialog】选择的时间："+selectedTime);
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
            saveSettingsToDataBase(taskId,name,needsReminder,time);
        }

    private void saveSettingsToDataBase(int id, String name, boolean needsReminder, String time) {
        Log.i(TAG, "【saveSettingsToDataBase】开始执行修改保存，保存的数据是：id:" + id + ",name:" + name + ",needsReminder:" + needsReminder + ",time:" + time);

        if (needsReminder && time != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                timeDate = sdf.parse(time); // 字符串 → Date
            } catch (ParseException e) {
                Log.e(TAG, "【saveSettingsToDataBase】时间格式解析失败：" + timeDate, e);
            }
        }

        observeOnce(taskDao.getTaskById(id), this, updateTask -> {
            if (updateTask != null) {
                Log.i(TAG, "找到任务，开始更新（仅触发一次）");
                updateTask.setTitle(name);
                updateTask.setneedRemind(needsReminder);
                updateTask.setremindTime(timeDate);
                // 子线程更新数据库
                new Thread(() -> {
                    taskDao.updateTask(updateTask);
                    this.onDestroy(); // 保存成功后关闭页面
                }).start();
            } else {
                Toast.makeText(this, "更新失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteSaveToSPfile(int id){
        Log.i(TAG, "开始执行删除，要删除的任务ID: " + id);
        Task DeleteTask = taskDao.getTaskById(id).getValue(); // 获取要更新的任务
        taskDao.deleteTask(DeleteTask);
    }

    /**
     * 观察 LiveData 但只触发一次回调（避免循环）
     * @param liveData 要观察的 LiveData
     * @param owner LifecycleOwner（Activity/Fragment）
     * @param observer 数据回调
     */
    public static <T> void observeOnce(LiveData<T> liveData, LifecycleOwner owner, Observer<T> observer) {
        liveData.observe(owner, new Observer<T>() {
            @Override
            public void onChanged(T data) {
                if (data != null) {
                    // 1. 先回调给外部 observer（处理数据）
                    observer.onChanged(data);
                    // 2. 立即移除观察者，防止再次触发
                    liveData.removeObserver(this);
                }
            }
        });
    }

}