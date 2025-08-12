package DateBaseRoom;

import androidx.room.TypeConverter;
import java.util.Date;

/**
 * 数据库类型转换器
 * 作用：让 Room 能够存储 Date 和 boolean 类型
 */
public class Converters {

    // ================== Date 转换 ==================
    /**
     * 将 Long 时间戳转换为 Date
     */
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    /**
     * 将 Date 转换为 Long 时间戳
     */
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    // ================== boolean 转换（可选） ==================
    /**
     * 将 boolean 转换为 Integer（0=false, 1=true）
     * 注：Room 默认支持 boolean 存储为 INTEGER，此方法仅作示例
     */
    @TypeConverter
    public static Integer booleanToInt(Boolean value) {
        return (value == null) ? null : (value ? 1 : 0);
    }

    /**
     * 将 Integer 转换为 boolean
     */
    @TypeConverter
    public static Boolean intToBoolean(Integer value) {
        return (value == null) ? null : value != 0;
    }
}