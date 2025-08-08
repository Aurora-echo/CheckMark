package TestAvtivity;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.checkmark.R;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import DateBaseRoom.AppDatabase;
import DateBaseRoom.Task;
import DateBaseRoom.TaskCompletion;
import DateBaseRoom.TaskCompletionDao;
import DateBaseRoom.TaskDao;

public class TestDateActivity extends AppCompatActivity {
    private AppDatabase db;
    private TaskDao taskDao;
    private TaskCompletionDao completionDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test_date);

        //初始化数据库
        db = AppDatabase.getInstance(this);
        taskDao = db.taskDao();
        completionDao = db.completionDao();

        //实例化操作
        // 示例操作
        new Thread(() -> {
            // 1. 创建新任务
            Task task = new Task("学习Room", "学习Android Room数据库");
            taskDao.insertTask(task);

            // 2. 记录任务完成
            TaskCompletion completion = new TaskCompletion(task.taskId);
            completion.notes = "学习了基本用法";
            completion.durationMinutes = 30;
            completionDao.insertCompletion(completion);

            // 3. 查询任务完成次数
            int totalCompletions = completionDao.getCompletionCountForTask(task.taskId);

            // 4. 查询本周完成次数
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
            Date startOfWeek = calendar.getTime();

            calendar.add(Calendar.DAY_OF_WEEK, 6);
            Date endOfWeek = calendar.getTime();

            int weeklyCompletions = completionDao.getWeeklyCompletionCount(
                    task.taskId, startOfWeek, endOfWeek);

            // 5. 查询所有任务
            List<Task> allTasks = taskDao.getAllTasks();

            // 在主线程更新UI
            runOnUiThread(() -> {
                // 更新UI代码...
            });
        }).start();

    }
}


//7. 使用 LiveData 观察数据变化
//如果你想让 UI 自动更新，可以使用 LiveData：
//
//java
//// 在 DAO 中修改查询方法
//@Query("SELECT * FROM tasks")
//LiveData<List<Task>> getAllTasks();
//
//@Query("SELECT * FROM task_completions WHERE taskId = :taskId")
//LiveData<List<TaskCompletion>> getCompletionsForTask(int taskId);
//
//// 在 ViewModel 中
//public class TaskViewModel extends AndroidViewModel {
//    private AppDatabase db;
//    private LiveData<List<Task>> allTasks;
//
//    public TaskViewModel(@NonNull Application application) {
//        super(application);
//        db = AppDatabase.getInstance(application);
//        allTasks = db.taskDao().getAllTasks();
//    }
//
//    public LiveData<List<Task>> getAllTasks() {
//        return allTasks;
//    }
//}
//
//// 在 Activity/Fragment 中观察
//taskViewModel.getAllTasks().observe(this, tasks -> {
//        // 更新UI
//        adapter.setTasks(tasks);
//});