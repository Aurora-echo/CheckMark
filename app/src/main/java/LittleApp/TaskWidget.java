// 声明这个类所在的包（相当于文件夹路径）
// 包名通常使用小写字母，用点号分隔，表示文件的组织结构
package LittleApp;

// 导入需要的Android系统类库
// 这些是Android提供的现成工具类，我们直接拿来用
import android.app.PendingIntent; // 用于创建延迟执行的Intent，比如点击小部件后要执行的操作
import android.appwidget.AppWidgetManager; // 管理桌面小部件的类，负责更新、删除小部件
import android.appwidget.AppWidgetProvider; // 小部件的基础类，我们继承它来创建自己的小部件
import android.content.ComponentName; // 用于标识组件（Activity、Service等）的名称
import android.content.Context; // 提供应用环境信息，可以获取资源、数据库等
import android.content.Intent; // 用于启动Activity或服务，相当于一个"行动指令"
import android.util.Log; // 用于在控制台输出日志信息，方便调试
import android.widget.RemoteViews; // 用于跨进程更新界面，因为小部件和App不在同一个进程

// 导入我们自己应用的资源文件
// R.java是Android自动生成的文件，包含了所有资源的ID
import com.example.checkmark.R;
// 导入我们应用的主页面Activity
import com.example.checkmark.main_list.MainList;

// 导入Java工具类
import java.util.Calendar; // 日历类，用于处理日期和时间
import java.util.Date; // 日期类，表示特定的时间点
import java.util.List; // 列表接口，用于存储多个对象

// 导入我们自己的数据库相关类
// 这些是我们之前创建的数据库操作类
import DateBaseRoom.AppDatabase; // 数据库总管理类
import DateBaseRoom.Task; // 任务数据模型类
import DateBaseRoom.TaskCompletion; // 任务完成记录数据模型类
import DateBaseRoom.TaskCompletionDao; // 任务完成记录的数据库操作类
import DateBaseRoom.TaskDao; // 任务的数据库操作类

/**
 * 任务桌面小部件类
 * 这个类负责在手机桌面上显示一个任务的小窗口
 * 用户可以快速查看任务信息和完成状态
 */
public class TaskWidget extends AppWidgetProvider {

    // 定义日志标签，用于在Logcat中区分不同的日志信息
    // TAG通常使用类名，方便查找和过滤日志
    private static String TAG = "Log.TaskWidget";

    // 定义数据库相关的静态变量
    // static表示这些变量属于类本身，而不是类的某个对象
    private static AppDatabase db;        // 数据库实例
    private static TaskDao taskDao;       // 任务表操作对象
    private static TaskCompletionDao completionDao; // 任务完成记录表操作对象

    /**
     * 当小部件需要更新时系统会自动调用这个方法
     * 比如小部件被添加到桌面、定时更新、手动刷新时都会调用
     *
     * @param context 上下文对象，提供应用环境信息
     * @param appWidgetManager 小部件管理器，用于操作小部件
     * @param appWidgetIds 需要更新的小部件ID数组（用户可能添加了多个小部件）
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 输出日志，表示开始更新小部件
        Log.i(TAG, "onUpdate(), 开始更新所有小部件");

        // 初始化数据库连接
        // 必须先初始化数据库，才能进行数据操作
        initDatabase(context);

        // 使用for循环遍历所有需要更新的小部件
        // appWidgetIds数组包含了所有当前小部件的ID
        for (int appWidgetId : appWidgetIds) {
            // 对每个小部件调用更新方法
            updateTaskWidget(context, appWidgetManager, appWidgetId);
        }
    }

    /**
     * 初始化数据库连接
     * 这个方法确保我们在操作数据库之前已经建立了连接
     *
     * @param context 上下文对象，用于获取数据库实例
     */
    private static void initDatabase(Context context) {
        // 检查数据库是否已经初始化，避免重复初始化
        if (db == null) {
            // 获取数据库实例（单例模式，确保整个应用只有一个数据库实例）
            db = AppDatabase.getInstance(context.getApplicationContext());
            // 从数据库实例中获取任务表的操作对象
            taskDao = db.taskDao();
            // 从数据库实例中获取任务完成记录表的操作对象
            completionDao = db.completionDao();

            // 输出日志，确认数据库初始化成功
            Log.i(TAG, "数据库初始化成功");
        }
    }

    /**
     * 更新单个小部件的内容和显示
     * 这是最重要的方法，负责读取数据并更新界面
     *
     * @param context 上下文对象
     * @param appWidgetManager 小部件管理器
     * @param appWidgetId 要更新的小部件的唯一ID
     */
    static void updateTaskWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // 输出日志，表示开始更新指定的小部件
        Log.i(TAG, "updateTaskWidget(), 开始更新小部件 ID: " + appWidgetId);

        // 确保数据库已经初始化
        initDatabase(context);

        // 创建RemoteViews对象，这是更新小部件界面的关键
        // 参数1：当前应用的包名，告诉系统这个界面属于哪个应用
        // 参数2：小部件的布局文件（R.layout.task_widget）
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.task_widget);

        // 先设置默认的显示内容，避免在加载数据时显示空白
        // 设置任务名称显示为"加载中..."
        views.setTextViewText(R.id.task_name, "加载中...");
        // 设置完成状态显示为空（还没有数据）
        views.setTextViewText(R.id.completion_status, "");
        // 设置背景颜色为白色
        views.setInt(R.id.widget_background, "setBackgroundColor",
                context.getResources().getColor(android.R.color.white));

        // 创建并启动一个新线程来读取数据库
        // 为什么要在新线程中读取数据库？
        // 因为数据库操作可能比较耗时，如果在主线程中进行会导致界面卡顿
        // Android不允许在主线程中进行网络或数据库等耗时操作
        new Thread(() -> {
            // 这里的代码会在后台线程中执行，不会阻塞主界面
            try {
                // 从数据库获取所有任务
                // 这是实际的数据库查询操作
                List<Task> tasks = taskDao.getAllTasks_onceTime();

                // 输出日志，显示获取到的任务数量
                // 使用三元运算符判断tasks是否为null，避免空指针异常
                Log.i(TAG, "从数据库获取到任务数量: " + (tasks != null ? tasks.size() : 0));

                // 检查是否成功获取到任务列表且列表不为空
                if (tasks != null && !tasks.isEmpty()) {
                    // 获取第一个任务（这里简单起见，总是显示第一个任务）
                    // 你可以根据需要修改这里，比如显示最重要的任务
                    Task task = tasks.get(0);

                    // 检查这个任务今天是否已经完成
                    boolean isCompletedToday = isTaskCompletedToday(task.taskId);

                    // 使用小部件管理器更新界面
                    // createRemoteViews方法会创建包含真实数据的界面
                    appWidgetManager.updateAppWidget(appWidgetId,
                            createRemoteViews(context, task.title, isCompletedToday));

                    // 输出成功日志
                    Log.i(TAG, "小部件更新成功 - 任务: " + task.title + ", 今日完成: " + isCompletedToday);

                } else {
                    // 如果没有任务，显示提示信息
                    appWidgetManager.updateAppWidget(appWidgetId,
                            createRemoteViews(context, "暂无任务", false));
                    Log.i(TAG, "数据库中没有找到任务");
                }
            } catch (Exception e) {
                // 如果发生异常（错误），在这里捕获并处理
                // 比如数据库表不存在、SQL语法错误等
                Log.e(TAG, "更新小部件时发生错误", e);

                // 出错时显示错误信息
                appWidgetManager.updateAppWidget(appWidgetId,
                        createRemoteViews(context, "加载失败", false));
            }
        }).start(); // 启动这个后台线程

        // 设置点击事件：当用户点击小部件时执行什么操作
        setClickIntent(context, views);

        // 立即更新一次小部件，显示加载状态
        // 这样用户就能立即看到"加载中..."而不是空白
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * 创建包含真实数据的RemoteViews对象
     * 这是一个辅助方法，根据任务信息创建界面
     *
     * @param context 上下文对象
     * @param taskName 要显示的任务名称
     * @param isCompleted 任务今天是否已完成
     * @return 配置好的RemoteViews对象
     */
    private static RemoteViews createRemoteViews(Context context, String taskName, boolean isCompleted) {
        // 创建新的RemoteViews对象
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.task_widget);

        // 设置任务名称文本
        // R.id.task_name是布局文件中TextView的ID
        views.setTextViewText(R.id.task_name, taskName);

        // 根据完成状态设置不同的显示效果
        if (isCompleted) {
            // 如果任务今天已完成
            // 显示"✓"符号表示已完成
            views.setTextViewText(R.id.completion_status, "✓");
            // 设置背景为浅绿色，直观表示已完成
            views.setInt(R.id.widget_background, "setBackgroundColor",
                    context.getResources().getColor(android.R.color.holo_green_light));
        } else {
            // 如果任务今天未完成
            // 显示空字符串（什么都不显示）
            views.setTextViewText(R.id.completion_status, "");
            // 设置背景为白色
            views.setInt(R.id.widget_background, "setBackgroundColor",
                    context.getResources().getColor(android.R.color.white));
        }

        // 设置点击事件
        setClickIntent(context, views);

        // 返回配置好的界面对象
        return views;
    }

    /**
     * 设置小部件的点击事件
     * 当用户点击小部件时，跳转到应用主页面
     *
     * @param context 上下文对象
     * @param views 要设置点击事件的RemoteViews对象
     */
    private static void setClickIntent(Context context, RemoteViews views) {
        // 创建Intent对象，表示要执行的动作：启动MainList页面
        Intent intent = new Intent(context, MainList.class);

        // 创建PendingIntent（延迟执行的Intent）
        // 小部件点击事件必须使用PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,           // 上下文对象
                0,                 // 请求代码（可以自定义，用于区分不同的PendingIntent）
                intent,            // 要执行的Intent
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // FLAG_UPDATE_CURRENT：如果已存在相同的PendingIntent，则更新其数据
        // FLAG_IMMUTABLE：表示这个PendingIntent创建后不能被修改（Android 12+要求）

        // 将点击事件绑定到小部件的背景视图上
        // 当用户点击小部件的任何地方都会触发这个事件
        views.setOnClickPendingIntent(R.id.widget_background, pendingIntent);
    }

    /**
     * 检查指定任务今天是否已经完成
     * 通过查询任务完成记录表来判断
     *
     * @param taskId 要检查的任务ID
     * @return true表示今天已完成，false表示未完成
     */
    private static boolean isTaskCompletedToday(int taskId) {
        try {
            // 获取Calendar实例，用于处理日期
            Calendar calendar = Calendar.getInstance();

            // 设置今天的开始时间（00:00:00 000）
            calendar.set(Calendar.HOUR_OF_DAY, 0);    // 设置小时为0（午夜）
            calendar.set(Calendar.MINUTE, 0);         // 设置分钟为0
            calendar.set(Calendar.SECOND, 0);         // 设置秒为0
            calendar.set(Calendar.MILLISECOND, 0);    // 设置毫秒为0
            Date startOfDay = calendar.getTime();     // 获取今天开始时间

            // 设置今天的结束时间（23:59:59 999）
            calendar.set(Calendar.HOUR_OF_DAY, 23);   // 设置小时为23
            calendar.set(Calendar.MINUTE, 59);        // 设置分钟为59
            calendar.set(Calendar.SECOND, 59);        // 设置秒为59
            calendar.set(Calendar.MILLISECOND, 999);  // 设置毫秒为999
            Date endOfDay = calendar.getTime();       // 获取今天结束时间

            // 查询数据库：查找指定任务在今天范围内的完成记录
            List<TaskCompletion> completions = completionDao.getCompletionsForTaskInTimeRange(
                    taskId,       // 任务ID
                    startOfDay,   // 查询开始时间（今天00:00:00）
                    endOfDay);    // 查询结束时间（今天23:59:59）

            // 如果有完成记录，返回true；否则返回false
            // completions != null 检查查询结果是否为null（避免空指针）
            // !completions.isEmpty() 检查列表是否不为空（有记录）
            return completions != null && !completions.isEmpty();

        } catch (Exception e) {
            // 如果查询过程中发生错误，输出错误日志并返回false
            Log.e(TAG, "检查任务完成状态时发生错误", e);
            return false;
        }
    }

    /**
     * 手动刷新小部件的静态方法
     * 可以在其他地方调用这个方法来强制刷新小部件
     * 比如当任务完成状态改变时调用
     *
     * @param context 上下文对象
     */
    public static void refreshWidget(Context context) {
        // 输出日志，表示开始手动刷新
        Log.i(TAG, "开始手动刷新小部件");

        // 获取小部件管理器实例
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        // 创建ComponentName对象，标识我们的小部件类
        ComponentName thisWidget = new ComponentName(context, TaskWidget.class);

        // 获取所有当前显示的小部件的ID
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        // 检查是否存在小部件，如果存在则进行更新
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            // 创建小部件实例并调用onUpdate方法
            new TaskWidget().onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    /**
     * 当第一个小部件被创建时调用
     * 比如用户第一次把这个小部件添加到桌面
     *
     * @param context 上下文对象
     */
    @Override
    public void onEnabled(Context context) {
        // 输出日志
        Log.i(TAG, "第一个小部件被用户添加到桌面");
        // 调用父类的相同方法，确保系统正常处理
        super.onEnabled(context);
    }

    /**
     * 当最后一个小部件被删除时调用
     * 比如用户删除了所有这个类型的小部件
     *
     * @param context 上下文对象
     */
    @Override
    public void onDisabled(Context context) {
        // 输出日志
        Log.i(TAG, "用户删除了最后一个小部件");
        // 调用父类的相同方法
        super.onDisabled(context);
    }
}