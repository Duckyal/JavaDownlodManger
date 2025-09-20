package org.dyckyal.jdm;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class DownloadService extends JobIntentService {

    // 必须为 JobIntentService 定义一个唯一的 job ID
    private static final String CHANNEL_ID = "download_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final LogManager logManger = LogManager.getInstance();


    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        // 注意：这个服务现在只负责处理通知，真正的下载逻辑在你的线程里。
        // 所以这个 onHandleWork 方法可能不会被我们直接用来下载。
        // 我们主要使用它的静态方法来更新UI。
        // 如果你把下载逻辑移到这里，那就在这里实现。
        // 但根据你的需求，我们继续使用静态方法模式。
    }

    public static void createNotificationChannel(Context context) {
        // 只在 Android 8.0 及以上版本创建渠道
        CharSequence name = "下载通知"; // 渠道名称，用户在系统设置中能看到
        String description = "显示文件下载的进度和结果"; // 渠道描述
        int importance = NotificationManager.IMPORTANCE_DEFAULT; // <--- 重点检查这里！

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        // 对于下载进度通知，可以允许灯光和振动
        channel.enableLights(true);
        channel.setLightColor(Color.RED);
        channel.enableVibration(true);

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
            logManger.debug("DownloadService", "下载进度通知服务已创建");
        }
    }

    /**
     * 更新下载进度
     * @param context 上下文
     * @param progress 当前进度 (0-100)
     */
    public static void updateDownloadProgress(Context context, double progress) {

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            logManger.waring("DownloadService", "通知权限未授予，无法显示下载进度。");
            return;
        }

        @SuppressLint("DefaultLocale") NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("正在下载")
                .setContentText(String.format("进度: %.2f%%", progress))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(false); // 下载进行中的通知不可被用户划掉

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }

    /**
     * 通知用户下载已完成
     * @param context 上下文
     * @param fileName 已下载文件的名称
     */
    public static void updateDownloadComplete(Context context, String fileName) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            logManger.waring("DownloadService", "通知权限未授予，无法显示下载完成消息。");
            return;
        }

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("下载完成")
                .setContentText(fileName + " 已下载完成")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setOngoing(false); // 下载完成后，通知可以被划掉

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }

    /**
     * 通知用户下载失败，并提供重试选项
     * @param context 上下文
     * @param reason 失败的原因
     */
    public static void updateDownloadFailed(Context context, String reason) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            logManger.waring("DownloadService", "通知权限未授予，无法显示下载失败消息。");
            return;
        }

        // 构建失败通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("出现异常")
                .setContentText("原因: " + reason)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // 失败通知优先级可以高一些
                .setOngoing(false); // 失败通知也可以被划掉

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }
}