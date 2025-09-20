package org.dyckyal.jdm;

public interface DownloadListener {
    // 当下载进度更新时调用
    void onProgressUpdate(long justDownloaded);
}
