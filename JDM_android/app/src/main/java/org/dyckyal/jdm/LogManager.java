package org.dyckyal.jdm;

import android.os.Handler;
import android.os.Looper;

public class LogManager {
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static volatile LogManager instance;
    private LogAdapter logAdapter;

    // 私有构造函数，防止外部实例化
    private LogManager() {}

    public static LogManager getInstance() {
        if (instance == null) {
            synchronized (LogManager.class) {
                if (instance == null) {
                    instance = new LogManager();
                }
            }
        }
        return instance;
    }

    /**
     * 必须在 Activity 或 Fragment 中调用此方法来注册 LogAdapter
     * @param adapter 你的 RecyclerView 的 LogAdapter 实例
     */
    public void registerAdapter(LogAdapter adapter) {
        this.logAdapter = adapter;
    }

    /**
     * 其他类可以调用的核心方法，用于添加日志
     * @param tag 日志的标签
     * @param message 日志的内容
     * @param level 日志的级别
     */
    public void addLog(String tag, String message, LogEntry.Level level) {
        // 创建新的日志条目
        LogEntry entry = new LogEntry(tag, message, level);

        // 在主线程更新 UI
        // 将“添加日志并更新UI”这个任务 post 到主线程执行
        mainHandler.post(() -> {
            // 这里的代码保证在主线程执行
            if (logAdapter != null) {
                logAdapter.addLog(entry);
            }
        });
    }

    // 提供一些便捷方法，让调用更简单
    public void debug(String tag, String message) { addLog(tag, message, LogEntry.Level.DEBUG); }
    public void info(String tag, String message) { addLog(tag, message, LogEntry.Level.INFO); }
    public void waring(String tag, String message) { addLog(tag, message, LogEntry.Level.WARN); }
    public void error(String tag, String message) { addLog(tag, message, LogEntry.Level.ERROR); }
}
