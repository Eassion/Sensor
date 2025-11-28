package com.example.sensor;

import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import android.os.Handler;
import android.os.Looper;

public class TCPClient {
    private static final String TAG = "TCPClient";
    private static final String SERVER_IP = "192.168.4.1";
    private static final int SERVER_PORT = 8080;

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ExecutorService executor;
    private TCPCallback callback;
    private boolean isConnected = false;

    // 轮询控制
    private AtomicBoolean isPolling = new AtomicBoolean(false);
    private static final int POLLING_INTERVAL = 2000; // 2秒间隔
    
    // 连接超时控制
    private static final int CONNECTION_TIMEOUT = 5000; // 5秒超时
    private static final int DELAY_BEFORE_POLLING = 3000; // 收到connected后3秒开始轮询
    private boolean connectedReceived = false;
    private boolean timeoutOccurred = false;
    private Handler timeoutHandler;

    public interface TCPCallback {
        // 修改回调：一次性返回所有数据数组
        // index 0:距离, 1:温度, 2:湿度, 3:光照, 4:色温, 5:电量
        void onAllDataReceived(float[] values);
        void onConnectionStatusChanged(boolean connected);
        void onError(String errorMessage);
        void onPollingStatusChanged(boolean polling);
        void onConnectionTimeout();
    }

    public TCPClient(TCPCallback callback) {
        this.callback = callback;
        this.executor = Executors.newFixedThreadPool(3);
        this.timeoutHandler = new Handler(Looper.getMainLooper());
    }

    private void ensureExecutor() {
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newFixedThreadPool(3);
        }
    }

    public void connect() {
        ensureExecutor();
        connectedReceived = false;
        timeoutOccurred = false;
        
        executor.execute(() -> {
            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                isConnected = true;

                if (callback != null) {
                    callback.onConnectionStatusChanged(true);
                }

                // 启动连接超时定时器
                startConnectionTimeoutTimer();
                
                startReceiving();
                Log.d(TAG, "Connected to server successfully");

            } catch (IOException e) {
                Log.e(TAG, "Connection error: " + e.getMessage());
                if (callback != null) {
                    callback.onError("连接失败: " + e.getMessage());
                }
                disconnect();
            }
        });
    }
    
    private void startConnectionTimeoutTimer() {
        timeoutHandler.postDelayed(() -> {
            if (!connectedReceived && isConnected) {
                timeoutOccurred = true;
                Log.e(TAG, "Connection timeout: did not receive 'connected' message within 5 seconds");
                if (callback != null) {
                    callback.onConnectionTimeout();
                }
                disconnect();
            }
        }, CONNECTION_TIMEOUT);
    }
    
    private void handleConnectedMessage() {
        if (!connectedReceived) {
            connectedReceived = true;
            Log.d(TAG, "Received 'connected' message from server");
            
            // 取消超时定时器
            timeoutHandler.removeCallbacksAndMessages(null);
            
            // 3秒后开始轮询
            timeoutHandler.postDelayed(() -> {
                if (isConnected && !timeoutOccurred) {
                    startPolling();
                }
            }, DELAY_BEFORE_POLLING);
        }
    }

    // 开始轮询：每2秒请求一次所有数据
    public void startPolling() {
        ensureExecutor();
        if (!isConnected) {
            if (callback != null) {
                callback.onError("未连接到服务器，无法开始轮询");
            }
            return;
        }

        if (isPolling.get()) {
            return;
        }

        isPolling.set(true);
        if (callback != null) {
            callback.onPollingStatusChanged(true);
        }

        executor.execute(() -> {
            while (isPolling.get() && isConnected) {
                try {
                    // 发送请求所有数据的指令
                    byte[] request = buildRequestAllFrame();

                    Log.d(TAG, "Sending poll request (Get All Data)");
                    outputStream.write(request);
                    outputStream.flush();

                    // 等待2秒
                    Thread.sleep(POLLING_INTERVAL);

                } catch (IOException e) {
                    Log.e(TAG, "Polling send error: " + e.getMessage());
                    if (callback != null) {
                        callback.onError("轮询发送失败: " + e.getMessage());
                    }
                    stopPolling();
                    disconnect();
                    break;
                } catch (InterruptedException e) {
                    Log.d(TAG, "Polling interrupted");
                    break;
                }
            }
            Log.d(TAG, "Polling loop exited");
        });
    }

    public void stopPolling() {
        if (isPolling.compareAndSet(true, false)) {
            if (callback != null) {
                callback.onPollingStatusChanged(false);
            }
            Log.d(TAG, "Polling stopped");
        }
    }

    // 构建请求帧：请求所有数据
    // 协议：AB BA 00 FF CD DC (FF代表请求所有)
    private byte[] buildRequestAllFrame() {
        byte[] frame = new byte[6];
        frame[0] = (byte) 0xAB;
        frame[1] = (byte) 0xBA;
        frame[2] = 0x00;
        frame[3] = (byte) 0xFF; // 特殊标志：请求所有
        frame[4] = (byte) 0xCD;
        frame[5] = (byte) 0xDC;
        return frame;
    }

    private void startReceiving() {
        executor.execute(() -> {
            byte[] buffer = new byte[1024];
            StringBuilder receivedDataBuilder = new StringBuilder();

            while (isConnected && socket != null && !socket.isClosed()) {
                try {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1) break;

                    if (bytesRead > 0) {
                        String stringData = new String(buffer, 0, bytesRead);
                        receivedDataBuilder.append(stringData);
                        processReceivedBuffer(receivedDataBuilder);
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Receive error: " + e.getMessage());
                    if (isConnected && callback != null) {
                        callback.onError("接收错误: " + e.getMessage());
                    }
                    break;
                }
            }
            stopPolling();
            disconnect();
        });
    }

    // 处理接收到的数据，解析格式：ABBA<v1>,<v2>,<v3>,<v4>,<v5>,<v6>CDDC
    private void processReceivedBuffer(StringBuilder buffer) {
        String data = buffer.toString();
        
        // 检查是否包含"connected"消息
        if (data.contains("connected")) {
            handleConnectedMessage();
            // 移除"connected"消息，避免重复处理
            buffer.delete(0, buffer.indexOf("connected") + "connected".length());
            data = buffer.toString();
        }

        int startIndex = data.indexOf("ABBA");
        while (startIndex != -1) {
            int endIndex = data.indexOf("CDDC", startIndex + 4);
            if (endIndex != -1) {
                // 提取 ABBA 和 CDDC 之间的内容
                String content = data.substring(startIndex + 4, endIndex).trim();
                Log.d(TAG, "Parsing frame content: " + content);

                try {
                    // 解析逗号分隔的数据
                    String[] parts = content.split(",");
                    if (parts.length == 6) {
                        float[] values = new float[6];
                        for (int i = 0; i < 6; i++) {
                            values[i] = Float.parseFloat(parts[i].trim());
                        }

                        // 回调数据
                        if (callback != null) {
                            callback.onAllDataReceived(values);
                        }
                    } else {
                        Log.w(TAG, "数据格式错误，期望6个参数，实际收到: " + parts.length);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "数据解析异常: " + e.getMessage());
                }

                // 从缓冲区移除已处理的数据
                buffer.delete(0, endIndex + 4);
                data = buffer.toString();
            } else {
                // 不完整的帧，保留数据等待下一次读取
                if (startIndex > 0) {
                    buffer.delete(0, startIndex);
                }
                break;
            }
            startIndex = data.indexOf("ABBA");
        }
    }

    public void disconnect() {
        stopPolling();
        isConnected = false;
        
        // 清理超时定时器
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
        
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Disconnect error: " + e.getMessage());
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (callback != null) {
            callback.onConnectionStatusChanged(false);
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }

    public boolean isPolling() {
        return isPolling.get();
    }

    public void destroy() {
        disconnect();
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}