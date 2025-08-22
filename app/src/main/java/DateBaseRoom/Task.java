package DateBaseRoom;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Date;

/**
 * 任务实体类
 * 对应数据库中的 tasks 表
 */
@Entity(tableName = "tasks")
@TypeConverters(Converters.class) // 应用类型转换器
public class Task {
    @PrimaryKey(autoGenerate = true)
    public int taskId;
    public String title;          // 任务标题
    public int status = 0;        // 状态 (0=未完成, 1=已完成)
    public Date createdAt;        // 创建时间
    public Date updatedAt;        // 更新时间
    public boolean needRemind; //是否需要提醒
    public Date remindTime; //提醒时间，选填

    // 构造函数
    public Task(String title, boolean needRemind, Date remindTime) {
        this.title = title;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.needRemind = needRemind;
        this.remindTime = remindTime;
    }

    public void setTitle(String title) {
        this.title=title;
    }

    public void setneedRemind(boolean needRemind) {
        this.needRemind=needRemind;
    }

    public void setremindTime(Date remindTime) {
        this.remindTime=remindTime;
    }

}