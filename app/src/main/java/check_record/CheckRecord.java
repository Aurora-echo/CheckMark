package check_record;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.checkmark.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import add_check.Add_Check;
import checksetting.checksetting;

/**
 * 详情页面Activity - 显示单个任务的详细信息和完成记录
 */
public class CheckRecord extends AppCompatActivity {

    // 视图组件
    private MaterialCalendarView calendarView;
    private FloatingActionButton fabRecord;
    private ImageView ivReminderIcon;
    private TextView tvReminderInfo, tvTaskName,tvCount;
    private Button btnSetting;

    // 数据相关
    private int  taskPosition;
    private double doubletaskId;
    private String taskName;
    private boolean needsReminder;  // 新增：是否需要提醒标志
    private String reminderTime;    // 新增：提醒时间
    private Set<CalendarDay> completedDates = new HashSet<>();
    private SharedPreferences sp;

    // 日志标签
    private static final String TAG = "Log.CheckRecord";
    private static final String SP_NAME = "CheckListInfo";

    LinkedTreeMap<String, Object> task_detail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_record);

        // 初始化视图
        initViews();

        // 获取Intent数据
        getIntentData();

        // 初始化本地存储
        sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);

        // 加载数据（优先使用Intent传递的预加载数据）
        loadData();
    }

    /**
     * 初始化所有视图组件
     */
    private void initViews() {
        // 日历视图
        calendarView = findViewById(R.id.calendarView);
        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_NONE);

        // 设置按钮
        btnSetting = findViewById(R.id.Check_Setting);
        btnSetting.setOnClickListener(v -> {
            // 创建跳转到设置页面的Intent
            Intent intent = new Intent(CheckRecord.this, checksetting.class);

            // 添加所有必要参数
            intent.putExtra("id", doubletaskId);               // 任务ID
            Log.i(TAG,"进入事项详情处放入设置intent时候的id:"+doubletaskId);
            intent.putExtra("taskName", taskName);       // 任务名称
            intent.putExtra("needsReminder", needsReminder); // 是否需要提醒
            intent.putExtra("reminderTime", reminderTime);   // 提醒时间(可能为null)

            startActivity(intent);
        });

        // 任务名称
        tvTaskName = findViewById(R.id.tv_task_name);

        // 提醒信息区域
        ivReminderIcon = findViewById(R.id.iv_reminder_icon);
        tvReminderInfo = findViewById(R.id.tv_reminder_info);

        //记录完成次数按钮
        tvCount = findViewById(R.id.tv_count);

        // 记录按钮
        fabRecord = findViewById(R.id.fab_record);
        fabRecord.setOnLongClickListener(v -> {
            recordCompletion();
            return true;
        });
        fabRecord.setOnClickListener(v -> {
            Toast.makeText(this, "请长按来完成任务记录~", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 获取Intent传递的数据
     */
    private void getIntentData() {
        taskPosition = getIntent().getIntExtra("position", -1);
        taskName = getIntent().getStringExtra("taskName");
        String taskData = getIntent().getStringExtra("taskData");
        // 指定反序列化的目标类型
        Type type = new TypeToken<LinkedTreeMap<String, Object>>() {}.getType();
        // 使用 Gson 解析 JSON
        task_detail = new Gson().fromJson(taskData, type);
        doubletaskId=getIntent().getDoubleExtra("id", -1);
        // 设置任务名称
        tvTaskName.setText(taskName);
        Log.i(TAG, "收到数据,位置:" + taskPosition + ", ID:" + doubletaskId + ", 名称:" + taskName);
        Log.i(TAG, "taskData:" + task_detail );
    }

    /**
     * 加载任务数据（优化后的加载逻辑）
     */
    private void loadData() {
        if (getIntent().hasExtra("taskData")) {
            Log.i(TAG,"从intent文件中load任务详情");
            String taskJson = getIntent().getStringExtra("taskData");
            Map<String, Object> task = new Gson().fromJson(taskJson, new TypeToken<Map<String, Object>>(){}.getType());
            //加载事项信息
            loadReminderInfo(task);
            //加载完成数据
            loadCompletedDates(task);
            //加载完成次数数据
            calculate_TaskDone_Count();
            return;
        }else{
            finish(); // 如果没有数据，则直接结束当前Activity
        }
    }

    /**
     * 从列表中查找特定ID的任务
     */
    private Map<String, Object> findTaskById(List<Map<String, Object>> tasks, int targetId) {
        for (Map<String, Object> task : tasks) {
            Object idObj = task.get("id");
            int id = idObj instanceof Double ? ((Double) idObj).intValue() : (Integer) idObj;
            if (id == targetId) {
                return task;
            }
        }
        return null;
    }

    /**
     * 加载提醒信息
     * 同时更新needsReminder和reminderTime字段
     */
    private void loadReminderInfo(Map<String, Object> task) {
        // 获取提醒设置
        needsReminder = task.containsKey("needsReminder") && (boolean) task.get("needsReminder");

        // 获取提醒时间
        reminderTime = task.containsKey("reminderTime") ? (String) task.get("reminderTime") : null;

        // 更新UI
        if (needsReminder && reminderTime != null) {
            ivReminderIcon.setVisibility(View.VISIBLE);
            tvReminderInfo.setText("每天 " + reminderTime + " 提醒");
            Log.i(TAG, "找到提醒设置: " + reminderTime);
        } else {
            ivReminderIcon.setVisibility(View.GONE);
            tvReminderInfo.setText("未设置提醒");
            Log.d(TAG, "未设置提醒");
        }
    }

    /**
     * 加载完成日期记录
     */
    private void loadCompletedDates(Map<String, Object> task) {
        List<String> records = (List<String>) task.get("completionRecords");
        if (records != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            for (String record : records) {
                try {
                    Date date = sdf.parse(record);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    completedDates.add(CalendarDay.from(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                    ));
                } catch (Exception e) {
                    Log.e(TAG, "日期解析错误", e);
                }
            }
        }

        // 设置日历装饰器
        calendarView.addDecorator(new CompletedDateDecorator());
        calendarView.invalidateDecorators();
    }

    /**
     * 计算任务完成次数
     */
    private void calculate_TaskDone_Count() {
        String tasksJson = sp.getString("tasks", "[]");
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> tasks = new Gson().fromJson(tasksJson, type);

        for (Map<String, Object> task : tasks) {
            Object idObj = task.get("id");
            int id = idObj instanceof Double ? ((Double) idObj).intValue() : (Integer) idObj;
            if (id == doubletaskId) {
                Log.i(TAG,"开始计算完成次数");
                List<String> records = (List<String>) task.get("completionRecords");
                if (records != null) {
                    int task_done_count=0;
                    for (String record : records) {
                        task_done_count = task_done_count+1;
                    }
                    Log.i(TAG,"计算完成次数结束，完成了"+task_done_count+"次");
                    tvCount.setText(""+task_done_count);   //显示完成次数
                }
            }

        }
    }

    /**
     * 记录任务完成
     */
    private void recordCompletion() {
        CalendarDay today = CalendarDay.today();
        if (!completedDates.contains(today)) {
            completedDates.add(today);
            Add_Check.addCompletionRecord(sp, taskPosition);
            calendarView.invalidateDecorators();
            Toast.makeText(this, "今天已记录完成", Toast.LENGTH_SHORT).show();
            // 设置结果表示数据已更新
            setResult(RESULT_OK);
            //这里有一点bug，记录完成后再次计算，但是task还是旧的，所以次数不会增加
            calculate_TaskDone_Count(); // 更新完成次数

            /**
             * 任务当天已完成，取消当天闹钟，设置下一天的闹钟,还没有实现，不会啊
             * 传一个时间的，不用传日期的，因为闹钟是每天提醒的
             * **/

        } else {
            Toast.makeText(this, "今日已记录过", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 日历装饰器 - 标记已完成日期
     */
    private class CompletedDateDecorator implements DayViewDecorator {
        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return completedDates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.setBackgroundDrawable(getResources().getDrawable(R.drawable.ios_button_background));
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume: 刷新Record()");
        super.onResume();
    }
}