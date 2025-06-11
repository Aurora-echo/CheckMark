package check_record;

import android.content.SharedPreferences;
import android.os.Bundle;
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
    private String taskName;
    private int taskPosition;
    private Set<CalendarDay> completedDates = new HashSet<>();
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_record);

        // 初始化SharedPreferences (使用与主列表相同的SP文件)
        sp = getSharedPreferences("CheckListInfo", MODE_PRIVATE);

        // 获取传递的事项信息
        taskPosition = getIntent().getIntExtra("position", -1);
        taskName = getIntent().getStringExtra("taskName");

        TextView tvTaskName = findViewById(R.id.tv_task_name);
        tvTaskName.setText(taskName);

        initCalendarView();
        initRecordButton();
        loadCompletedDates();
    }

    private void initCalendarView() {
        calendarView = findViewById(R.id.calendarView);
        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_NONE);
        calendarView.addDecorator(new CompletedDateDecorator());
    }

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

    private void loadCompletedDates() {
        // 直接读取SP文件
        String tasksJson = sp.getString("tasks", "[]");
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> tasks = new Gson().fromJson(tasksJson, type);

        if (taskPosition >= 0 && taskPosition < tasks.size()) {
            Map<String, Object> task = tasks.get(taskPosition);
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
                                calendar.get(Calendar.MONTH), // 已经是从0开始
                                calendar.get(Calendar.DAY_OF_MONTH)
                        ));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
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