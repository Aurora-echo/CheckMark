package DateBaseRoom;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

/**
 * Room 数据库类
 * 包含所有实体和DAO
 */
@Database(
        entities = {Task.class, TaskCompletion.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    // 获取任务DAO
    public abstract TaskDao taskDao();

    // 获取完成记录DAO
    public abstract TaskCompletionDao completionDao();

    // 单例模式获取数据库实例
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "task_database.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}