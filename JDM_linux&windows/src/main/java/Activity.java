import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Activity extends JFrame {
    private OkHttpClient client;
    private String filename;
    private long Len;
    private ExecutorService pool;
    private downloadManger manger;

    public Activity() {
        JFrame frame = new  JFrame("JavaDownloadMannger");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridBagLayout()); // 使用 GridBagLayout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // 设置组件间距

        // 第一行：标签和输入框
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1; // 标签占 1 份宽度
        frame.add(new JLabel("下载地址:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 4; // 输入框占 4 份宽度
        JTextField textField1 = new JTextField();
        frame.add(textField1, gbc);

        gbc.gridx = 2;
        gbc.weightx = 1;
        JButton button = new JButton("开始下载");
        frame.add(button, gbc);

        // 第二行：标签和输入框
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1;
        frame.add(new JLabel("文件名称:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 4;
        JTextField textField2 = new JTextField();
        frame.add(textField2, gbc);

        gbc.gridx = 2;
        gbc.weightx = 1;
        JLabel fileSize = new JLabel();
        frame.add(fileSize, gbc);

        // 第三行：标签和下拉框
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1;
        frame.add(new JLabel("使用线程:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 4;
        String[] options = {"4", "5", "6", "7", "8", "9", "10", "11", "12"};
        JComboBox<String> comboBox = new JComboBox<>(options);
        frame.add(comboBox, gbc);

        gbc.gridx = 2;
        gbc.weightx = 1;
        frame.add(new JLabel("线程"), gbc);

        // 第四行：标签和进度条
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1;
        frame.add(new JLabel("下载进度:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 3;
        JProgressBar progressBar = new JProgressBar();
        progressBar.setValue(0); // 示例进度值
        frame.add(progressBar, gbc);

        gbc.gridx = 2;
        gbc.weightx = 1;
        JLabel progressLabel = new JLabel("0.00%");
        frame.add(progressLabel, gbc);

        // 设置输入框逻辑
        textField1.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                client = new OkHttpClient();
                String url = textField1.getText();
                // 2. 创建并启动一个后台线程来执行网络请求
                new Thread(() -> {
                    try {
                        // --- 这部分代码现在在后台线程中执行，不会阻塞UI ---
                        Request request = new Request.Builder()
                                .url(url)
                                .build();

                        // execute() 是同步阻塞的，但现在它在后台线程，所以是安全的
                        Response response = client.newCall(request).execute();
                        if (!response.isSuccessful()) {
                            return;
                        }

                        // 获取文件名和文件大小
                        filename = response.header("Content-Disposition", "null");
                        if (filename.equals("null")) {
                            String timeStamp = new SimpleDateFormat("yyyy-MMdd-HHmm.").format(new Date());
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
                        textField2.setText(filename);
                        Len = Long.parseLong(response.header("Content-Length", "0"));
                        if (Len == 0) {
                            System.err.println("所填url地址并非下载地址或不支持多线程下载");
                            return;
                        }
                        final String fileSizeMb = (Len / 1024 / 1024) + "MB"; // 注意单位是MB，不是KB
                        fileSize.setText(fileSizeMb);
                    } catch (Exception ex) {
                        fileSize.setText("地址为空或不可下载");
                    }
                }).start();
            }
        });
        // 设置按键逻辑
        button.addActionListener(e -> {
            switch (button.getText()) {
                case "开始下载":
                    button.setText("终止下载");
                    String selected = (String) comboBox.getSelectedItem();
                    int n = Integer.parseInt(selected);
                    pool = Executors.newFixedThreadPool(n);
                    File downloadFolder = new File(System.getProperty("user.home"), "Downloads");
                    manger = new downloadManger(client, new File(downloadFolder,textField2.getText()), Len, progressLabel, progressBar);
                    manger.download(textField1.getText(), n, pool);
                    break;
                case "终止下载":
                    button.setText("开始下载");
                    manger.StoppedCause = "exit";
                    pool.shutdownNow();
                    progressLabel.setText("0.00%");
                    progressBar.setValue(0);
            }
        });

        //设置窗口是否可见
        frame.setBounds(400,400,600,200);    //设置容器大小
        frame.setLocationRelativeTo(null); // 居中显示
        frame.setVisible(true);
    }

    public static void main(String[] agrs) {
        new Activity();    //创建一个实例化对象
    }
}
