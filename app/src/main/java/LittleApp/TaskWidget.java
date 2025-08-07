// 声明这个类所在的包（相当于文件夹路径）
package LittleApp;

// 导入需要的Android类库
import android.app.PendingIntent; // 用于延迟执行的Intent
import android.appwidget.AppWidgetManager; // 管理桌面小部件的类
import android.appwidget.AppWidgetProvider; // 小部件的基础类
import android.content.Context; // 提供应用环境信息
import android.content.Intent; // 用于启动Activity或服务
import android.util.Log;
import android.widget.RemoteViews; // 用于跨进程更新界面

// 导入我们自己的资源文件（R.java自动生成）
import com.example.checkmark.R;
// 导入我们应用的主列表Activity
import com.example.checkmark.main_list.MainList;

// 定义一个桌面小部件类，继承自AppWidgetProvider
public class TaskWidget extends AppWidgetProvider {

    private static String TAG="Log.TaskWidget";
    private static final String SP = "CheckListInfo";
    private static final String SELECT_KEY = "selectTaskId";

    // 当小部件需要更新时系统会自动调用这个方法
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.i(TAG,"onUpdate(),更新所有小部件");
        // 遍历所有该类型的小部件ID（用户可能添加了多个）
        for (int appWidgetId : appWidgetIds) {
            // 调用更新单个小部件的方法
            updateTaskWidget(context, appWidgetManager, appWidgetId);
        }
    }

    // 更新单个小部件的静态方法（可以被其他类调用）
    static void updateTaskWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.i(TAG,"updateTaskWidget(),更新小部件");

        // 设置一个示例任务名称
        String taskName = "每日运动";
        // 设置任务是否完成的标志（false表示未完成）
        boolean isCompleted = true;

        // 创建RemoteViews对象（用于跨进程更新界面）
        // 参数1：当前应用的包名
        // 参数2：小部件的布局文件（R.layout.task_widget）
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.task_widget);

        // 设置任务名称文本（R.id.task_name是布局中的TextView）
        views.setTextViewText(R.id.task_name, taskName);

        // 根据任务完成状态设置不同的显示效果
        if (isCompleted) {
            // 如果已完成，显示"✓"符号
            views.setTextViewText(R.id.completion_status, "✓");
            // 设置背景为浅绿色
            views.setInt(R.id.widget_background, "setBackgroundColor",
                    context.getResources().getColor(android.R.color.holo_green_light));
        } else {
            // 如果未完成，显示"○"符号
            views.setTextViewText(R.id.completion_status, "○");
            // 设置背景为白色
            views.setInt(R.id.widget_background, "setBackgroundColor",
                    context.getResources().getColor(android.R.color.white));
        }

        // 设置点击事件：当用户点击小部件时跳转到MainList页面
        Intent intent = new Intent(context, MainList.class);
        // 传递任务名称给目标Activity
        intent.putExtra("task_name", taskName);
        // 创建PendingIntent（延迟执行的Intent）
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0, // 请求代码（可以自定义）
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // 将点击事件绑定到小部件的背景视图上
        views.setOnClickPendingIntent(R.id.widget_background, pendingIntent);

        // 通知系统更新这个小部件
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}