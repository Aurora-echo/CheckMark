package check_record;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.checkmark.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
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

public class CheckRecord extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private FloatingActionButton fabRecord;
    private int taskid;
    private String taskName;
    private int taskPosition;
    private Set<CalendarDay> completedDates = new HashSet<>();
    private SharedPreferences sp;
    private static final String TAG = "Log.CheckRecord--------->>>>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_record);

        // 初始化SharedPreferences (使用与主列表相同的SP文件)
        sp = getSharedPreferences("CheckListInfo", MODE_PRIVATE);

        // 获取传递的事项信息
        taskPosition = getIntent().getIntExtra("position", -1);
        taskName = getIntent().getStringExtra("taskName");
        taskid = getIntent().getIntExtra("id",-1);
        Log.i(TAG, "intent获取的数据，taskPosition："+taskPosition+",taskName:"+taskName+"taskid:"+taskid);

        TextView tvTaskName = findViewById(R.id.tv_task_name);
        tvTaskName.setText(taskName);

        initCalendarView();
        initRecordButton();
        loadCompletedDates();
    }

    //初始化日历识图
    private void initCalendarView() {
        calendarView = findViewById(R.id.calendarView);
        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_NONE);
        calendarView.addDecorator(new CompletedDateDecorator());
    }

    // 初始化完成记录按钮
    private void initRecordButton() {
        fabRecord = findViewById(R.id.fab_record);

        fabRecord.setOnLongClickListener(v -> {
            recordCompletion();
            return true;
        });

        fabRecord.setOnClickListener(v -> {
            Toast.makeText(this, "长按记录完成", Toast.LENGTH_SHORT).show();
        });
    }

    // 从SP文件中加载数据
    private void loadCompletedDates() {
        Log.i(TAG, "开始根据taskid加载数据: " + taskid);
        // 直接读取SP文件
        String tasksJson = sp.getString("tasks", "[]");
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> tasks = new Gson().fromJson(tasksJson, type);

        // 通过taskid查找任务，而不是position
        boolean found = false;
        for (Map<String, Object> task : tasks) {
            // 修复：先获取Double，再转为int
            Object idObj = task.get("id");
            int checkid = idObj instanceof Double
                    ? ((Double) idObj).intValue()
                    : (Integer) idObj;

            if (checkid == taskid) {
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
                            e.printStackTrace();
                        }
                    }
                }
                found = true;
                break;
            }
        }

        if (!found) {
            Log.w(TAG, "未找到ID为" + taskid + "的任务记录");
        }

        calendarView.invalidateDecorators();
    }

    private void recordCompletion() {
        CalendarDay today = CalendarDay.today();
        if (!completedDates.contains(today)) {
            completedDates.add(today);
            // 使用Add_Check中的方法更新记录
            Add_Check.addCompletionRecord(sp, taskPosition);
            calendarView.invalidateDecorators();
            Toast.makeText(this, "已记录完成", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "今日已记录过", Toast.LENGTH_SHORT).show();
        }
    }

    private class CompletedDateDecorator implements DayViewDecorator {
        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return completedDates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.setBackgroundDrawable(getResources().getDrawable(R.drawable.calendar_highlight));
        }
    }
}