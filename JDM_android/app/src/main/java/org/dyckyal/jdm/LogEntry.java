package org.dyckyal.jdm;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogEntry {
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    private final String timestamp;
    private final String tag;
    private final String message;
    private final Level level;

    public LogEntry(String tag, String message, Level level) {
        this.tag = tag;
        this.message = message;
        this.level = level;
        // 使用 SimpleDateFormat 生成格式化的时间戳
        this.timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    // Getter 方法
    public String getTimestamp() { return timestamp; }
    public String getTag() { return tag; }
    public String getMessage() { return message; }
    public Level getLevel() { return level; }
}
