package DateBaseRoom;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import java.util.Date;

import DateBaseRoom.Task;

/**
 * 任务完成记录实体类
 * 对应数据库中的 task_completions 表
 */
@Entity(
        tableName = "task_completions",
        foreignKeys = @ForeignKey(
                entity = Task.class,
                parentColumns = "taskId",
                childColumns = "taskId",
                onDelete = ForeignKey.CASCADE
        )
)
public class TaskCompletion {
    @PrimaryKey(autoGenerate = true)
    public int completionId;

    public int taskId;            // 关联的任务ID
    public Date completedAt;      // 完成时间
    public String notes;          // 备注(可选)
    public Integer durationMinutes; // 完成耗时(分钟，可选)

    // 构造函数
    public TaskCompletion(int taskId) {
        this.taskId = taskId;
        this.completedAt = new Date();
    }
}