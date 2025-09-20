package org.dyckyal.jdm;

import android.content.Context;
import android.os.Handler;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.OkHttpClient;

public class downloadManger implements DownloadListener {
    private final LogManager logManager = LogManager.getInstance();
    private final Handler uiHandler; // 添加 Handler 成员
    private final AtomicLong downloadedBytes = new AtomicLong(0); // 用于线程安全地累加字节数
    private int saveProgress = 0;   // 随进度百分比变化，变化时带动进度条更新
    public volatile String StoppedCause = "normal"; // volatile确保多线程间可见性

    private final Context context;
    private final OkHttpClient client;
    private final File saveFile;
    private ExecutorService pool;
    private final long Len;

    public downloadManger(Context context, OkHttpClient client, File saveFile, long Len, Handler uiHandler) {
        this.context = context;
        this.client = client;
        this.saveFile = saveFile;
        this.Len = Len;
        this.uiHandler = uiHandler;
    }

    // 当某个线程下载了一部分数据时调用
    @Override
    public synchronized void onProgressUpdate(long justDownloaded) {
        long totalDownloaded = downloadedBytes.addAndGet(justDownloaded); // 线程安全地累加
        double Progress = (totalDownloaded * 100.0) / Len;   // 计算百分比
        int progress = (int) Progress;
        if (progress > saveProgress) {
            saveProgress = progress;
            logManager.debug("progress", "当前下载进度为 "+progress+"%");
            // 使用 Handler 发送消息到主线程——更新进度条
            uiHandler.obtainMessage(progress).sendToTarget();
        }
        // 发送通知
        switch (StoppedCause) {
            case "normal":
                DownloadService.updateDownloadProgress(context, Progress);
                break;
            case "exit":
                DownloadService.updateDownloadFailed(context, "下载任务被用户中断");
                break;
            case "error":
                DownloadService.updateDownloadFailed(context, "网络状况不佳");
        }
    }

    public void download(String url, int nThreads) {
        try {
            // 计算线程数(rounds)和每个线程下载大小(offset)
            long offset = 1024 * 1024 * 100;
            offset = (Len>=5*offset)?offset:Len/nThreads;
            int rounds = (int) Math.ceil(Len*1.0/offset);

            // 创建随机读写对象
            RandomAccessFile raf = new RandomAccessFile(saveFile, "rw");
            raf.setLength(Len);  // 预填充文件（防止数据混淆）

            pool = Executors.newFixedThreadPool(nThreads);
            logManager.info("downloadManger", "开始执行多线程下载");
            for (int i=0; i<rounds; i++) {
                int index = i+1;
                if (i == rounds-1) {
                    pool.execute(new downloadThread(index, client, url, i*offset, Len, saveFile, this));
                } else {
                    pool.execute(new downloadThread(index, client, url, i*offset, (i+1)*offset-1, saveFile, this));
                }
            }
            pool.shutdown(); // 停止接受新任务
            new Thread(() -> {
                try {
                    if (!pool.awaitTermination(2, TimeUnit.HOURS)) {
                        pool.shutdownNow();
                        logManager.error("downloadManger", "下载超时(2 hours)");
                        return;
                    }

                    // awaitTermination 返回了，说明所有任务都结束了（无论是完成还是被中断）
                    // 现在检查是正常结束还是用户手动停止的
                    switch (StoppedCause) {
                        case "exit":
                            // 是用户手动停止的，不要合并文件，直接恢复UI
                            logManager.info("downloadManger", "下载已被用户手动停止");
                            saveFile.delete();
                            break;
                        case "error":
                            logManager.error("downloadManger", "下载失败：请检查网络状况或更换url下载地址");
                            saveFile.delete();
                            break;
                        case "normal":
                            logManager.info("downloadManger", saveFile.getName() + "文件下载完成");
                            DownloadService.updateDownloadComplete(context, saveFile.getName());
                    }
                    uiHandler.obtainMessage(0).sendToTarget();   // 恢复下载按钮
                } catch (InterruptedException e) {
                    // 这个 catch 块现在主要捕获监控线程本身被中断的情况
                    pool.shutdownNow();
                    Thread.currentThread().interrupt();
                    logManager.info("downloadManger", "下载等待过程被中断");
                }
            }).start();
        } catch (Exception e) {
            logManager.error("downloadManger", e.getMessage());
        }
    }

    public void exitThreads() {
        StoppedCause = "exit";
        pool.shutdownNow();
    }
}
