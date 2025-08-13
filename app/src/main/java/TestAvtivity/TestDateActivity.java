package TestAvtivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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
    private static  final String TAG="MTING";
    Button btn_commit_date,btn_add_record;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test_date);

        btn_commit_date = findViewById(R.id.commit_date);
        btn_add_record = findViewById(R.id.commit_date2);

        //初始化数据库
        db = AppDatabase.getInstance(this);
        taskDao = db.taskDao();
        completionDao = db.completionDao();

        btn_commit_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 示例操作
                new Thread(() -> {
                    // 1. 创建新任务
                    boolean needRemind = true;
                    Date remindTime=new Date("2025/08/12 20:00");
                    Task task = new Task("学习Room",needRemind,remindTime);
                    taskDao.insertTask(task);

                    // 2. 查询所有任务
                    List<Task> allTasks = taskDao.getAllTasks();
                    Log.i(TAG,"创建完，所有的任务："+allTasks.toString());

                    //3.修改任务
                    Task updateTask = taskDao.getTaskById(2);
                    updateTask.setTitle("学习Room-3");
                    taskDao.updateTask(updateTask);

                    // 4. 查询所有任务
                    List<Task> allTasks_2 = taskDao.getAllTasks();
                    Log.i(TAG,"更新完，所有的任务："+allTasks_2.toString());

                    // 在主线程更新UI
                    runOnUiThread(() -> {
                        // 更新UI代码...
                    });
                }).start();
            }
        });

        btn_add_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(() ->{
                    // 5. 插入记录任务完成
                    TaskCompletion completion = new TaskCompletion(2);
                    completionDao.insertCompletion(completion);

                    // 6. 查询任务完成次数
                    int totalCompletions = completionDao.getCompletionCountForTask(2);
                    Log.i(TAG,"id为2的任务，完成了："+totalCompletions+"次");

                    //7. 查询任务完成的时间记录
                    List<Date> taskCompletionTimes = completionDao.getCompletionTimesForTask(2);
                    for(Date completion_time:taskCompletionTimes){
                        Log.d(TAG, "完成时间: " + completion_time);
                    }

                }).start();
            }
        });
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