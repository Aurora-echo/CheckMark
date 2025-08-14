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
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import DateBaseRoom.DateUtils;
import DateBaseRoom.TaskCompletion;
import DateBaseRoom.TaskCompletionDao;
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
    private TextView tvReminderInfo, tvTaskName,tvCount,tvMonthCount,tvWeekCount;
    private Button btnSetting;

    // 数据相关
    private int  taskPosition,taskid;
    private String taskName;
    private boolean needsReminder;
    private Date reminderTime;    // 新增：提醒时间
    private Set<CalendarDay> completedDates = new HashSet<>();
    // 日志标签
    private static final String TAG = "Log.CheckRecord";
    private TaskCompletionDao completionDao; // 数据库操作对象
    //任务完成日期列表数组
    List<Date> taskCompletionTimes;
    private DateUtils dateUtils = new DateUtils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_record);
        // 初始化视图
        initViews();
        // 获取Intent数据
        getShowData();
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
            intent.putExtra("id", taskid);               // 任务ID
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
        tvMonthCount = findViewById(R.id.tv_Month_Count);
        tvWeekCount = findViewById(R.id.tv_Week_Count);

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
    private void getShowData() {
        //获取点击列表的序号
        taskPosition = getIntent().getIntExtra("position", -1);
        //获取任务名称（从intent）
        taskName = getIntent().getStringExtra("taskName");
        //获取任务的id
        taskid=getIntent().getIntExtra("id",-1);
        //获取任务是否需要提醒
        needsReminder = getIntent().getBooleanExtra("needRemind", false);
        if(needsReminder){
            //获取提醒时间,从intent取出long类型再转回Date类型
            long reminderTime_long = getIntent().getLongExtra("remindTime", -1);
            if (reminderTime_long != -1) {
                reminderTime = new Date(reminderTime_long);
            }
        }
        //从数据库获取任务完成记录
        new Thread(() ->{
            taskCompletionTimes = completionDao.getCompletionTimesForTask(taskid);
        }).start();
    }

    /**
     * 加载任务数据（优化后的加载逻辑）
     */
    private void loadData() {
        //加载事项提醒信息
        loadReminderInfo(needsReminder,reminderTime);
        //加载完成日期数据
        loadCompletedDates(taskCompletionTimes);
        //加载完成次数数据
        calculate_TaskDone_Count();
        return;
    }

    /**
     * 加载提醒信息
     * 同时更新needsReminder和reminderTime字段
     */
    private void loadReminderInfo(boolean needsReminder,Date reminderTime) {
        // 获取提醒设置
        if( needsReminder && reminderTime != null) {
            ivReminderIcon.setVisibility(View.VISIBLE);
            tvReminderInfo.setText("每天 " + reminderTime + " 提醒");
        } else {
            ivReminderIcon.setVisibility(View.GONE);
            tvReminderInfo.setText("未设置提醒");
        }
    }

    /**
     * 加载完成日期记录
     */
    private void loadCompletedDates(List<Date> taskCompletionTimes) {
        if (taskCompletionTimes != null) {
            for (Date record : taskCompletionTimes) {
                try {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(record);
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
        int total_Completions = completionDao.getCompletionCountForTask(taskid);
        tvCount.setText(""+total_Completions);   //总共完成次数
        int month_Completions = completionDao.getMonthlyCompletionCount(taskid,dateUtils.getStartOfMonth(), dateUtils.getEndOfMonth());
        tvMonthCount.setText(""+month_Completions); //本月完成次数
        int week_Completions = completionDao.getWeeklyCompletionCount(taskid,dateUtils.getStartOfWeek(), dateUtils.getEndOfWeek());
        tvWeekCount.setText(""+week_Completions);   //本周完成次数
    }

    /**
     * 记录任务完成
     */
    private void recordCompletion() {
        CalendarDay today = CalendarDay.today();
        if (!completedDates.contains(today)) {
            completedDates.add(today);
            TaskCompletion completion_record = new TaskCompletion(taskid);
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