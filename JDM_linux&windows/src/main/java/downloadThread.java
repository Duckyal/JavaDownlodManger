import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class downloadThread extends Thread {
    private final int index;
    private final String url;
    private final OkHttpClient client;
    private final long start, end;
    private final File saveFile;
    private final DownloadListener listener; // 持有监听器引用

    public downloadThread(int index, OkHttpClient client, String url, long start, long end, File saveFile, DownloadListener listener) {
        this.index = index;
        this.url = url;
        this.start = start;
        this.end = end;
        this.saveFile = saveFile;
        this.client = client;
        this.listener = listener;
    }

    @Override
    public void run() {
        Request request = new Request.Builder().url(url).header("Range", "bytes=" + start + "-" + end).build();   // 设置请求范围
        for (int i = 0; i < 4; i++) {
            if (((downloadManger) listener).StoppedCause.equals("exit") ||
                    ((downloadManger) listener).StoppedCause.equals("error")) {
                return;
            }
            try {
                Response response = client.newCall(request).execute();
                InputStream is = response.body().byteStream();

                RandomAccessFile raf = new RandomAccessFile(saveFile, "rw");
                raf.seek(start);
                byte[] buffer = new byte[8192]; // 8KB 缓冲区
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1 && !Thread.currentThread().isInterrupted()) {
                    raf.write(buffer, 0, bytesRead);
                    // 关键步骤：每次读取到数据后，立即通知监听器
                    listener.onProgressUpdate(bytesRead);
                    i = 0;  // 有下载便重置重新下载次数
                }
//                String fileSize = ((end-start) > 1024*1024) ? String.format("%s MB", Math.round(100.*(end-start)/1024/1024)/100.) :  // 显示单位MB
//                                ((end-start) > 1024) ? String.format("%s kb", Math.round(100.*(end-start)/1024)/100.) : String.format("%s byte", Math.round(100.*(end-start))/100.);   // 显示单位kb : b
                break;
            } catch (Exception e) {
                if (i == 3) {
                    ((downloadManger) listener).StoppedCause = "exit";
                } else {
                    if (!((downloadManger) listener).StoppedCause.equals("exit")) {
                        System.err.println("下载线程" + index + String.format(": 出现异常，尝试第%s次重新下载", i + 1));
                    }
                }
            }
        }
    }
}