import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.OkHttpClient;

import javax.swing.*;

public class downloadManger implements DownloadListener {
    private final AtomicLong downloadedBytes = new AtomicLong(0); // 用于线程安全地累加字节数
    private int saveProgress = 0;   // 随进度百分比变化，变化时带动进度条更新
    public volatile String StoppedCause = "normal"; // volatile确保多线程间可见性

    private final OkHttpClient client;
    private final File saveFile;
    private ExecutorService pool;
    private final long Len;

    private final JLabel jl;
    private final JProgressBar jpb;

    public downloadManger(OkHttpClient client, File saveFile, long Len, JLabel jl, JProgressBar jpb) {
        this.client = client;
        this.saveFile = saveFile;
        this.Len = Len;
        this.jl = jl;
        this.jpb = jpb;
    }

    // 当某个线程下载了一部分数据时调用
    @Override
    public synchronized void onProgressUpdate(long justDownloaded) {
        long totalDownloaded = downloadedBytes.addAndGet(justDownloaded); // 线程安全地累加
        double Progress = (totalDownloaded * 100.0) / Len;   // 计算百分比
        jl.setText(String.format("%.2f%%", Progress));
        int progress = (int) Progress;
        if (progress > saveProgress) {
            saveProgress = progress;
            jpb.setValue(progress);
        }
    }

    public void download(String url, int nThreads, ExecutorService pool) {
        try {
            // 计算线程数(rounds)和每个线程下载大小(offset)
            long offset = 1024 * 1024 * 100;
            offset = (Len>=5*offset)?offset:Len/nThreads;
            int rounds = (int) Math.ceil(Len*1.0/offset);

            // 创建随机读写对象
            RandomAccessFile raf = new RandomAccessFile(saveFile, "rw");
            raf.setLength(Len);  // 预填充文件（防止数据混淆）

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
                    }
                    // awaitTermination 返回了，说明所有任务都结束了（无论是完成还是被中断）

                } catch (InterruptedException e) {
                    // 这个 catch 块现在主要捕获监控线程本身被中断的情况
                    pool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exitThreads() {
        StoppedCause = "exit";
        pool.shutdownNow();
        jl.setText("0.00%");
        jpb.setValue(0);
    }
}
