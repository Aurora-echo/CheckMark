package DateBaseRoom;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import android.content.Context;

/**
 * Room 数据库类
 * 包含所有实体和DAO
 */
// 声明这是一个Room数据库类，包含所有实体和DAO
@Database(
        // 定义数据库包含的实体类：Task和TaskCompletion
        entities = {Task.class, TaskCompletion.class},
        // 数据库版本号，升级时需要增加
        version = 1,
        // 不导出数据库模式信息到文件
        exportSchema = false
)
@TypeConverters(Converters.class) // 全局应用类型转换器
public abstract class AppDatabase extends RoomDatabase {
    // 使用volatile关键字确保多线程环境下的可见性
    private static volatile AppDatabase INSTANCE;

    // 声明获取TaskDao的抽象方法，Room会自动实现
    public abstract TaskDao taskDao();

    // 声明获取TaskCompletionDao的抽象方法，Room会自动实现
    public abstract TaskCompletionDao completionDao();

    // 使用单例模式获取数据库实例
    public static AppDatabase getInstance(Context context) {
        // 第一次检查实例是否存在（避免不必要的同步）
        if (INSTANCE == null) {
            // 同步代码块，确保线程安全
            synchronized (AppDatabase.class) {
                // 第二次检查实例是否存在（双重检查锁定）
                if (INSTANCE == null) {
                    // 使用Room的databaseBuilder创建数据库实例
                    INSTANCE = Room.databaseBuilder(
                            // 使用应用上下文防止内存泄漏
                            context.getApplicationContext(),
                            // 指定数据库类
                            AppDatabase.class,
                            // 数据库文件名
                            "CheckMark.db"
                    ).build(); // 构建数据库实例
                }
            }
        }
        // 返回单例实例
        return INSTANCE;
    }
}