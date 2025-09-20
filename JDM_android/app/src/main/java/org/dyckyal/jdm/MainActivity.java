package org.dyckyal.jdm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private final LogManager logManager = LogManager.getInstance();
    private LogAdapter logAdapter;
    @SuppressLint("StaticFieldLeak")
    public ProgressBar progressBar;
    private EditText etUrl, fileName;
    private Button startDownload, stopDownload;
    private Spinner nThreads;
    private final OkHttpClient client = new OkHttpClient();
    private downloadManger manger;
    private final File saveFolderPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "JDM_download");
    private String filename;
    private long Len;
    private static final int REQUEST_STORAGE_PERMISSION = 100;  // 存储权限授权码

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.bar_name);    // 设置bar标题
        DownloadService.createNotificationChannel(this);

        etUrl = findViewById(R.id.et_url);
        fileName = findViewById(R.id.download_name);
        progressBar = findViewById(R.id.progressBar);
        startDownload = findViewById(R.id.startDownload);
        stopDownload = findViewById(R.id.stopDownload);
        nThreads = findViewById(R.id.spinner_nThreads);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // 创建 LogAdapter 实例
        logAdapter = new LogAdapter(recyclerView);
        // 设置给 RecyclerView
        recyclerView.setAdapter(logAdapter);
        // 将 LogAdapter 注册到 LogManager
        logManager.registerAdapter(logAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!saveFolderPath.exists()) {
            saveFolderPath.mkdirs();
        }

        // 检查通知权限是否已经授予
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "请打开通知权限", Toast.LENGTH_SHORT).show();
            // 创建一个 Intent，跳转到应用的详情设置页面
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }

        startDownload.setOnClickListener(v -> {
            addTask();
            logManager.debug("debug", "显现停止按钮");
            startDownload.setVisibility(RecyclerView.GONE);
            stopDownload.setVisibility(RecyclerView.VISIBLE);
        });
        stopDownload.setOnClickListener(v -> {
            manger.exitThreads();
            logManager.debug("debug", "显现开始按钮");
            startDownload.setVisibility(RecyclerView.VISIBLE);
            stopDownload.setVisibility(RecyclerView.GONE);
        });

        // 监听输入框变化
        etUrl.addTextChangedListener(new TextWatcher(){
            @Override
            public void afterTextChanged(Editable s) {
                // 文本变化后（移开输入焦点）
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 文本变化前
                Toast.makeText(getApplicationContext(), "url输入完成后请回车确定", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 文本变化时
                // 1. 获取用户输入的 URL
                final String url = s.toString();
                if (url.isEmpty()) {
                    return; // 如果输入为空，则不执行任何操作
                } else if (!url.startsWith("http")) {
                    logManager.error("urlCheck", "所填url地址并非完整url或不是url");
                }

                // 2. 创建并启动一个后台线程来执行网络请求
                new Thread(() -> {
                    try {
                        // --- 这部分代码现在在后台线程中执行，不会阻塞UI ---
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url(url)
                                .build();

                        // execute() 是同步阻塞的，但现在它在后台线程，所以是安全的
                        Response response = client.newCall(request).execute();
                        if (!response.isSuccessful()) {
                            logManager.error("response", "请求失败");
                            return;
                        }

                        // 获取文件名和文件大小
                        filename = response.header("Content-Disposition", "null");
                        if (filename.equals("null")) {
                            @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyy-MMdd-HHmm.").format(new Date());
                            String fileType = response.header("Content-Type");
                            if (fileType != null) {
                                String[] type = fileType.split("/");
                                switch (type[0]) {
                                    case "text":    // 文本类型
                                        if (type[1].equals("plain")) {
                                            filename = timeStamp + "text";
                                        } else if (type[1].equals("javascript")) {
                                            filename = timeStamp + "js";
                                        } else {
                                            filename = timeStamp + type[1];
                                        }
                                        break;
                                    case "image":    // 图片类型
                                        filename = timeStamp + type[1];
                                        break;
                                    case "audio":    // 音频类型
                                        filename = timeStamp + "mp3";
                                        break;
                                    case "video":    // 视频类型
                                        filename = timeStamp + "mp4";
                                        break;
                                    case "application":    // apk、压缩包类型
                                        switch (type[1]) {
                                            case "zip":
                                                filename = timeStamp + "zip";
                                                break;
                                            case "x-rar-compressed":
                                                filename = timeStamp + "rar";
                                                break;
                                            case "x-tar":
                                                filename = timeStamp + "tar";
                                                break;
                                            case "vnd.android.package-archive":
                                                filename = timeStamp + "apk";
                                                break;
                                        }
                                }
                            }
                        } else {
                            // 1. 尝试提取 filename*（带编码的文件名）
                            if (filename.contains("filename*=")) {
                                int startIndex = filename.indexOf("''") + 2;
                                filename = filename.substring(startIndex);
                            }
                            // 2. 如果没有 filename*，尝试提取 filename（可能带双引号）
                            else if (filename.contains("filename=")) {
                                int startIndex = filename.indexOf("filename=") + "filename=".length();
                                filename = filename.substring(startIndex);

                                // 去除可能的双引号
                                if (filename.startsWith("\"")) {
                                    filename = filename.substring(1);
                                }
                                if (filename.endsWith("\"")) {
                                    filename = filename.substring(0, filename.length() - 1);
                                }
                            }
                        }
                        Len = Long.parseLong(response.header("Content-Length", "0"));
                        if (Len == 0) {
                            logManager.error("urlCheck", "所填url地址并非下载地址或不支持多线程下载");
                            return;
                        }
                        final String fileSizeMb = "文件总大小为:" + (Len / 1024 / 1024) + "MB"; // 注意单位是MB，不是KB
                        logManager.info("urlCheck", fileSizeMb);

                        // 3. 使用 runOnUiThread 切换回主线程来更新UI
                        runOnUiThread(() -> {
                            // 这里的代码会在主线程执行
                            if (filename.equals("null")) {
                                fileName.setHint("请手动添加文件名(包括后缀)");
                            } else {
                                fileName.setText(filename);
                            }
                            nThreads.setSelection(0);
                        });
                    } catch (Exception e) {
                        logManager.error("urlCheck", e.getMessage());
                    }
                }).start();
            }
        });
    }

    private void addTask() {
        if (!hasStoragePermission()) {
            requestStoragePermission();
        }
        // 获取线程数
        String threadsStr = nThreads.getSelectedItem().toString();
        int n = Integer.parseInt(threadsStr);
        // 获取文件下载对象
        String filename = fileName.getText().toString();
        if (filename.split("\\.").length < 2){
            logManager.error("addTask", "未检测到文件名后缀");
            return;
        }
        File saveFile = new File(saveFolderPath, filename);

        manger = new downloadManger(this, client, saveFile, Len, mainHandler);
        manger.download(etUrl.getText().toString(), n);
    }

    public boolean hasStoragePermission() {
        // 检查存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33)
            int readImages = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES);
            int readVideo = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO);
            int readAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO);
            return readImages == PackageManager.PERMISSION_GRANTED &&
                    readVideo == PackageManager.PERMISSION_GRANTED &&
                    readAudio == PackageManager.PERMISSION_GRANTED;
        } else {
            int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            return readPermission == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        // 请求存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13
            requestPermissions(
                    new String[]{
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO
                    },
                    REQUEST_STORAGE_PERMISSION
            );
        } else { // Android 6.0 以上
            requestPermissions(
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQUEST_STORAGE_PERMISSION
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 响应存储权限申请
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予
                Toast.makeText(this, "存储权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "存储权限被拒绝，请手动开启权限", Toast.LENGTH_SHORT).show();
                // 引导用户去设置页面开启权限
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }
    }

    public Handler mainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            int state = msg.what;
            // 这个方法会在主线程中被调用
            if (msg.what == 0) {
                startDownload.setVisibility(RecyclerView.VISIBLE);
                stopDownload.setVisibility(RecyclerView.GONE);
            } else {
                // 我们用 what 来代表这是一个进度更新消息
                progressBar.setProgress(state); // 更新进度条
            }
        }
    };

    // 在 onDestroy 中清理资源 ---
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除 Handler 所有未处理的消息和回调，防止在 Activity 销毁后处理消息
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        // 如果下载管理器存在，也确保它被清理
        if (manger != null) {
            manger.exitThreads();
            manger = null;
        }
        logAdapter.clearLogs();
        DownloadService.updateDownloadFailed(this, "应用被异常终止");
    }
}
