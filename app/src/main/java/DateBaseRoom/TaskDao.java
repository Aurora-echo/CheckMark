package DateBaseRoom;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * 任务数据访问对象 (DAO)
 * 定义了对 tasks 表的操作
 */
@Dao
public interface TaskDao {
    // 插入新任务
    @Insert
    void insertTask(Task task);

    // 更新任务
    @Update
    void updateTask(Task task);

    // 删除任务
    @Delete
    void deleteTask(Task task);

    // 获取所有任务
    @Query("SELECT * FROM tasks")
    LiveData<List<Task>> getAllTasks();

    // 根据ID获取任务
    @Query("SELECT * FROM tasks WHERE taskId = :taskId")
    Task getTaskById(int taskId);

    // 获取未完成任务
    @Query("SELECT * FROM tasks WHERE status != 2")
    List<Task> getIncompleteTasks();

    // 获取已完成任务
    @Query("SELECT * FROM tasks WHERE status = 2")
    List<Task> getCompletedTasks();
}