package LittleApp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.checkmark.R;
import com.example.checkmark.main_list.MainList;

public class TaskWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateTaskWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateTaskWidget(Context context, AppWidgetManager appWidgetManager,
                                 int appWidgetId) {
        // 这里模拟数据，实际应从数据库或其他存储获取
        String taskName = "每日运动";
        boolean isCompleted = false; // 模拟未完成状态

        // 构建RemoteViews - 使用新的小尺寸布局
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.task_widget);

        // 设置任务名称
        views.setTextViewText(R.id.task_name, taskName);

        // 设置完成状态
        if (isCompleted) {
            views.setTextViewText(R.id.completion_status, "✓"); // 更简洁的勾选标记
            views.setInt(R.id.widget_background, "setBackgroundColor",
                    context.getResources().getColor(android.R.color.holo_green_light));
        } else {
            views.setTextViewText(R.id.completion_status, "○"); // 空心圆更清晰
            views.setInt(R.id.widget_background, "setBackgroundColor",
                    context.getResources().getColor(android.R.color.white));
        }

        // 设置点击事件 - 跳转到事项详情页
        Intent intent = new Intent(context, MainList.class);
        intent.putExtra("task_name", taskName);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_background, pendingIntent);

        // 更新小组件
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}