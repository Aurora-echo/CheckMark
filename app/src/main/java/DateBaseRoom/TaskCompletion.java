package DateBaseRoom;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Date;

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
        ),
        indices = {@Index("taskId")} // 为外键列添加索引
)
@TypeConverters(Converters.class) // 应用类型转换器
public class TaskCompletion {
    @PrimaryKey(autoGenerate = true)
    public int completionId;

    public int taskId;            // 关联的任务ID
    public Date completedAt;      // 完成时间

    // 构造函数
    public TaskCompletion(int taskId) {
        this.taskId = taskId;
        this.completedAt = new Date();
    }
}