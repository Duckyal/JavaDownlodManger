package org.dyckyal.jdm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
    private final RecyclerView recyclerView;
    private final List<LogEntry> logEntries = new ArrayList<>();

    public LogAdapter(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false); // 假设你有一个 item_log.xml 布局
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogEntry entry = logEntries.get(position);
        holder.timestampTextView.setText(entry.getTimestamp());
        holder.tagTextView.setText(entry.getTag());
        holder.messageTextView.setText(entry.getMessage());

        // (可选) 根据日志级别设置不同颜色
        switch (entry.getLevel()) {
            case ERROR:
                holder.messageTextView.setTextColor(0xFFFF0000); // 红色
                break;
            case WARN:
                holder.messageTextView.setTextColor(0xFFFFA500); // 橙色
                break;
            case INFO:
                holder.messageTextView.setTextColor(0xFF000000); // 黑色
                break;
            case DEBUG:
                holder.messageTextView.setTextColor(0xFF808080); // 灰色
                break;
        }
    }

    @Override
    public int getItemCount() {
        return logEntries.size();
    }

    /**
     * 这个方法是关键，允许外部添加新的日志条目并自动刷新UI
     * @param entry 新的日志条目
     */
    public void addLog(LogEntry entry) {
        logEntries.add(entry);
        // 通知适配器在列表末尾插入了一个新项
        notifyItemInserted(logEntries.size() - 1);
        // 自动滚动到最新日志
         if (recyclerView != null) {
             recyclerView.smoothScrollToPosition(logEntries.size() - 1);
         }
    }

    public void clearLogs() {
        logEntries.clear();
        notifyDataSetChanged();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView timestampTextView, tagTextView, messageTextView;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampTextView = itemView.findViewById(R.id.timestamp);
            tagTextView = itemView.findViewById(R.id.tag);
            messageTextView = itemView.findViewById(R.id.message);
        }
    }
}
