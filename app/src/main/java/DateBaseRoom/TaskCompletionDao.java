package DateBaseRoom;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.Date;
import java.util.List;

/**
 * 任务完成记录数据访问对象 (DAO)
 * 定义了对 task_completions 表的操作
 */
@Dao
public interface TaskCompletionDao {
    // 插入完成记录
    @Insert
    void insertCompletion(TaskCompletion completion);

    // 删除完成记录
    @Delete
    void deleteCompletion(TaskCompletion completion);

    // 获取任务的所有完成记录
    @Query("SELECT * FROM task_completions WHERE taskId = :taskId")
    List<TaskCompletion> getCompletionsForTask(int taskId);

    // 获取任务的完成次数
    @Query("SELECT COUNT(*) FROM task_completions WHERE taskId = :taskId")
    int getCompletionCountForTask(int taskId);

    // 获取任务本周完成次数
    @Query("SELECT COUNT(*) FROM task_completions " +
            "WHERE taskId = :taskId " +
            "AND completedAt >= :startOfWeek " +
            "AND completedAt <= :endOfWeek")
    int getWeeklyCompletionCount(int taskId, Date startOfWeek, Date endOfWeek);

    // 获取任务本月完成次数
    @Query("SELECT COUNT(*) FROM task_completions " +
            "WHERE taskId = :taskId " +
            "AND completedAt >= :startOfMonth " +
            "AND completedAt <= :endOfMonth")
    int getMonthlyCompletionCount(int taskId, Date startOfMonth, Date endOfMonth);

    // 获取最近N条完成记录
    @Query("SELECT * FROM task_completions WHERE taskId = :taskId ORDER BY completedAt DESC LIMIT :limit")
    List<TaskCompletion> getRecentCompletions(int taskId, int limit);

    // 获取任务的完成时间列表
    @Query("SELECT completedAt FROM task_completions WHERE taskId = :taskId ORDER BY completedAt DESC")
    List<Date> getCompletionTimesForTask(int taskId);
}