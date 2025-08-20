package check_record;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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
import java.util.Random;
import java.util.Set;

import DateBaseRoom.AppDatabase;
import DateBaseRoom.DateUtils;
import DateBaseRoom.Task;
import DateBaseRoom.TaskCompletion;
import DateBaseRoom.TaskCompletionDao;
import DateBaseRoom.TaskDao;
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
    private TextView tvReminderInfo, tvTaskName,tvCount,tvMonthCount,tvWeekCount,tvRandom;
    private ImageButton btnSetting;

    // 数据相关
    private int  taskid;
    private String taskName;
    private boolean needsReminder;
    private Date reminderTime;    // 新增：提醒时间
    private Set<CalendarDay> completedDates = new HashSet<>();
    // 日志标签
    private static final String TAG = "Log.CheckRecord";
    private TaskCompletionDao completionDao; // 数据库操作对象
    private TaskDao taskDao;
    private AppDatabase db;
    //任务完成日期列表数组
    List<Date> taskCompletionTimes;
    private DateUtils dateUtils = new DateUtils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_record);

        db = AppDatabase.getInstance(this);
        completionDao = db.completionDao();
        taskDao = db.taskDao();

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
            if(needsReminder){
                long long_reminderTime = reminderTime.getTime();
                intent.putExtra("long_reminderTime", long_reminderTime);   // 提醒时间(可能为null)
            }
            startActivity(intent);
        });

        // 任务名称
        tvTaskName = findViewById(R.id.tv_task_name);

        // 提醒信息区域
        ivReminderIcon = findViewById(R.id.iv_reminder_icon);
        tvReminderInfo = findViewById(R.id.tv_reminder_info);
        tvRandom =findViewById(R.id.tv_ramdom);

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
        Log.i(TAG,"【getShowData】intent中的数据，taskName="+taskName+",taskid="+taskid+",needsReminder="+needsReminder+",reminderTime="+reminderTime);
        //从数据库获取任务完成记录
        new Thread(() ->{
            taskCompletionTimes = completionDao.getCompletionTimesForTask(taskid);
        }).start();
    }

    /**
     * 加载任务数据（优化后的加载逻辑）
     */
    private void loadData() {
        // 加载任务名称
        tvTaskName.setText(taskName);
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
        Log.i(TAG,"【loadReminderInfo】needsReminder:"+needsReminder);
        Log.i(TAG,"【loadReminderInfo】reminderTime:"+reminderTime);
        // 获取提醒设置
        if( needsReminder && reminderTime != null) {
            ivReminderIcon.setVisibility(View.VISIBLE);
            SimpleDateFormat reminderTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String reminderTimeFormat_hhmm = reminderTimeFormat.format(reminderTime);
            tvReminderInfo.setText("每天 " + reminderTimeFormat_hhmm + " 提醒");
        } else {
            ivReminderIcon.setVisibility(View.GONE);
            tvReminderInfo.setText("未设置提醒");
        }
    }

    /**
     * 加载完成日期记录
     */
    private void loadCompletedDates(List<Date> taskCompletionTimes) {
        Log.i(TAG,"【loadCompletedDates】taskCompletionTimes:"+taskCompletionTimes);
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
        new Thread(() -> {
            int total_Completions = completionDao.getCompletionCountForTask(taskid);
            int month_Completions = completionDao.getMonthlyCompletionCount(taskid, dateUtils.getStartOfMonth(), dateUtils.getEndOfMonth());
            int week_Completions = completionDao.getWeeklyCompletionCount(taskid, dateUtils.getStartOfWeek(), dateUtils.getEndOfWeek());

            runOnUiThread(() -> {
                tvCount.setText(String.valueOf(total_Completions));
                tvMonthCount.setText(String.valueOf(month_Completions));
                tvWeekCount.setText(String.valueOf(week_Completions));
                showRandomTv();
            });
        }).start();
    }

    /**
     * 记录任务完成
     */
    private void recordCompletion() {
        CalendarDay today = CalendarDay.today();
        if (!completedDates.contains(today)) {
            completedDates.add(today);
            TaskCompletion completion_record = new TaskCompletion(taskid);
            new Thread(() -> {
                completionDao.insertCompletion(completion_record); // 插入完成记录数据库表
                taskDao.updateTaskStatus(taskid); // 更新任务状态
                runOnUiThread(() -> {
                    calendarView.invalidateDecorators();
                    Toast.makeText(this, "今天已记录完成", Toast.LENGTH_SHORT).show();
                    calculate_TaskDone_Count(); // 更新完成次数
                });
            }).start();

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
        super.onResume();
        Log.i(TAG, "【onResume】读取数据库获取新的数据");
        new Thread(() -> {
            Log.i(TAG,"【onResume】taskid:"+taskid);
            Task task = taskDao.getTaskById_Task_NotLaveData(taskid); // 通过id获取任务信息
            runOnUiThread(()->{
                if(task != null){
                    Log.i(TAG,"【onResume】task:"+task);
                    taskName = task.title;
                    needsReminder = task.needRemind;
                    reminderTime = task.remindTime;
                    // 更新UI
                    tvTaskName.setText(taskName);
                    loadReminderInfo(needsReminder, reminderTime);
                } else {
                    Log.i(TAG,"【onResume】task为空");}
            });
        }).start();
    }

    private  void showRandomTv() {
        String[] Random_List = {
                "坚持就是胜利，永不放弃！",
                "每一天都是新的开始，加油！",
                "梦想不会逃跑，会逃跑的永远是自己",
                "努力不一定成功，但放弃一定失败",
                "心若向阳，无畏悲伤",
                "越努力，越幸运",
                "行动是成功的阶梯",
                "今天的努力，明天的实力",
                "相信自己，你能做到",
                "没有失败，只有暂时停止成功",
                "路虽远，行则将至",
                "事虽难，做则必成",
                "只要路是对的，就不怕远",
                "成功在于坚持，坚持就是胜利",
                "付出总有回报，只是时间问题",
                "机会总是留给有准备的人",
                "做最好的自己，不负韶华",
                "生命不息，奋斗不止",
                "青春无悔，奋斗最美",
                "梦想还是要有的，万一实现了",
                "不为失败找借口，只为成功找方法",
                "态度决定高度，习惯成就人生",
                "细节决定成败，态度决定一切",
                "每天进步一点点，成功离你不远",
                "困难像弹簧，你弱它就强",
                "不经历风雨，怎么见彩虹",
                "失败是成功之母，总结是成功之父",
                "心中有阳光，脚下有力量",
                "与其临渊羡鱼，不如退而结网",
                "世上无难事，只怕有心人",
                "宝剑锋从磨砺出，梅花香自苦寒来",
                "吃得苦中苦，方为人上人",
                "不经一番寒彻骨，怎得梅花扑鼻香",
                "山重水复疑无路，柳暗花明又一村",
                "只要功夫深，铁杵磨成针",
                "天生我材必有用，千金散尽还复来",
                "长风破浪会有时，直挂云帆济沧海",
                "千淘万漉虽辛苦，吹尽狂沙始到金",
                "会当凌绝顶，一览众山小",
                "欲穷千里目，更上一层楼",
                "少壮不努力，老大徒伤悲",
                "黑发不知勤学早，白首方悔读书迟",
                "书山有路勤为径，学海无涯苦作舟",
                "读书破万卷，下笔如有神",
                "纸上得来终觉浅，绝知此事要躬行",
                "问渠那得清如许，为有源头活水来",
                "时间就像海绵里的水，挤挤总会有的",
                "合理安排时间，就等于节约时间",
                "今日事今日毕，勿将今事待明日",
                "一寸光阴一寸金，寸金难买寸光阴",
                "时间就是生命，时间就是速度",
                "逝者如斯夫，不舍昼夜",
                "光阴似箭，日月如梭",
                "莫等闲，白了少年头，空悲切",
                "盛年不重来，一日难再晨",
                "及时当勉励，岁月不待人",
                "三更灯火五更鸡，正是男儿读书时",
                "少年易老学难成，一寸光阴不可轻",
                "未觉池塘春草梦，阶前梧叶已秋声",
                "花有重开日，人无再少年",
                "明日复明日，明日何其多",
                "我生待明日，万事成蹉跎",
                "世人若被明日累，春去秋来老将至",
                "朝看水东流，暮看日西坠",
                "百年明日能几何？请君听我明日歌",
                "志不强者智不达，言不信者行不果",
                "志当存高远，慕先贤，绝情欲",
                "老骥伏枥，志在千里",
                "烈士暮年，壮心不已",
                "燕雀安知鸿鹄之志哉",
                "大鹏一日同风起，扶摇直上九万里",
                "古之立大事者，不惟有超世之才",
                "亦必有坚忍不拔之志",
                "壮心未与年俱老，死去犹能作鬼雄",
                "生当作人杰，死亦为鬼雄",
                "位卑未敢忘忧国，事定犹须待阖棺",
                "人生自古谁无死？留取丹心照汗青",
                "千磨万击还坚劲，任尔东西南北风",
                "粉身碎骨浑不怕，要留清白在人间",
                "不要人夸颜色好，只留清气满乾坤",
                "落红不是无情物，化作春泥更护花",
                "苟利国家生死以，岂因祸福避趋之",
                "我自横刀向天笑，去留肝胆两昆仑",
                "横眉冷对千夫指，俯首甘为孺子牛",
                "春蚕到死丝方尽，蜡炬成灰泪始干",
                "衣带渐宽终不悔，为伊消得人憔悴",
                "沉舟侧畔千帆过，病树前头万木春",
                "山不在高，有仙则名。水不在深，有龙则灵",
                "谈笑有鸿儒，往来无白丁",
                "出淤泥而不染，濯清涟而不妖",
                "路漫漫其修远兮，吾将上下而求索",
                "亦余心之所善兮，虽九死其犹未悔",
                "天行健，君子以自强不息",
                "地势坤，君子以厚德载物",
                "海纳百川，有容乃大；壁立千仞，无欲则刚",
                "非淡泊无以明志，非宁静无以致远"
        };
        int randomIndex = new Random().nextInt(Random_List.length);
        tvRandom.setText(Random_List[randomIndex]);
    }
}