package DateBaseRoom;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

/**
 * 任务实体类
 * 对应数据库中的 tasks 表
 */
@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey(autoGenerate = true)
    public int taskId;

    public String title;          // 任务标题
    public String description;    // 任务描述
    public int priority = 1;      // 优先级 (1=低, 2=中, 3=高)
    public int status = 0;        // 状态 (0=未开始, 1=进行中, 2=已完成)
    public Date createdAt;        // 创建时间
    public Date updatedAt;        // 更新时间
    public Date dueDate;          // 截止日期(可选)

    // 构造函数
    public Task(String title, String description) {
        this.title = title;
        this.description = description;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
}