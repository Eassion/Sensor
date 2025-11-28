package com.example.sensor;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TCPClient.TCPCallback {

    private TCPClient tcpClient;
    private Handler mainHandler;

    private TextView tvStatus, tvLog, tvPollingStatus;
    private TextView tvDistance, tvTemperature, tvHumidity, tvIlluminance, tvColorTemp, tvBattery;
    private Button btnConnect, btnDisconnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());
        initializeViews();
        setupClickListeners();

        recreateTcpClient();
    }

    private void recreateTcpClient() {
        if (tcpClient != null) {
            tcpClient.destroy();
        }
        tcpClient = new TCPClient(this);
    }

    private void initializeViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvLog = findViewById(R.id.tvLog);
        tvPollingStatus = findViewById(R.id.tvPollingStatus);

        tvDistance = findViewById(R.id.tvDistance);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvIlluminance = findViewById(R.id.tvIlluminance);
        tvColorTemp = findViewById(R.id.tvColorTemp);
        tvBattery = findViewById(R.id.tvBattery);

        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);

        updatePollingStatus(false);
    }

    private void setupClickListeners() {
        btnConnect.setOnClickListener(v -> connectToServer());
        btnDisconnect.setOnClickListener(v -> disconnectFromServer());
    }

    private void connectToServer() {
        addLog("正在连接服务器...");
        recreateTcpClient();
        tcpClient.connect();
    }

    private void disconnectFromServer() {
        addLog("断开服务器连接");
        if (tcpClient != null) {
            tcpClient.stopPolling();
            tcpClient.disconnect();
        }
    }

    // 接收到包含所有数据的回调
    @Override
    public void onAllDataReceived(float[] values) {
        mainHandler.post(() -> {
            // 确保数据长度正确
            if (values.length < 6) return;

            // 更新界面
            tvDistance.setText(String.format("%.2f", values[0]));
            tvTemperature.setText(String.format("%.2f", values[1]));
            tvHumidity.setText(String.format("%.2f", values[2]));
            tvIlluminance.setText(String.format("%.2f", values[3]));
            tvColorTemp.setText(String.format("%.0f", values[4])); // 色温通常没有小数
            tvBattery.setText(String.format("%.0f", values[5]));    // 电量通常没有小数

            // 记录简要日志
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            addLog(time + " 数据已刷新");
        });
    }

    @Override
    public void onConnectionStatusChanged(boolean connected) {
        mainHandler.post(() -> {
            if (connected) {
                tvStatus.setText("状态: 已连接");
                tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                addLog("服务器连接成功");

                btnConnect.setEnabled(false);
                btnDisconnect.setEnabled(true);
            } else {
                tvStatus.setText("状态: 未连接");
                tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                addLog("服务器连接断开");

                btnConnect.setEnabled(true);
                btnDisconnect.setEnabled(false);
                updatePollingStatus(false);
            }
        });
    }

    @Override
    public void onPollingStatusChanged(boolean polling) {
        mainHandler.post(() -> updatePollingStatus(polling));
    }

    private void updatePollingStatus(boolean polling) {
        if (polling) {
            tvPollingStatus.setText("监测状态: 运行中");
            tvPollingStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            tvPollingStatus.setText("监测状态: 已停止");
            tvPollingStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
    }

    @Override
    public void onError(String errorMessage) {
        mainHandler.post(() -> {
            addLog("错误: " + errorMessage);
            showToast(errorMessage);
        });
    }
    
    @Override
    public void onConnectionTimeout() {
        mainHandler.post(() -> {
            addLog("连接超时: 未收到服务器确认消息");
            showToast("请检查wifi是否连接ESP32");
        });
    }

    private void addLog(String message) {
        mainHandler.post(() -> {
            String currentText = tvLog.getText().toString();
            // 限制日志长度，防止过长
            if (currentText.length() > 2000) {
                currentText = currentText.substring(0, 2000);
            }
            String newText = message + "\n" + currentText;
            tvLog.setText(newText);
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tcpClient != null) {
            tcpClient.destroy();
            tcpClient = null;
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}