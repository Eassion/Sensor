# 修改TCP客户端连接逻辑

## 需求分析
1. 移除开始自动监测按钮，连接后自动开始计时
2. 服务端会在5s内返回"connected"，收到后等待3秒开始轮询
3. 若5s内没收到消息，不开始轮询，并弹窗提示"请检查wifi是否连接ESP32"

## 修改内容

### 1. TCPClient.java
- 添加连接超时检测机制（5秒）
- 修改接收逻辑，处理"connected"消息
- 添加收到"connected"后3秒延迟开始轮询的逻辑
- 添加超时回调

### 2. MainActivity.java
- 移除开始自动监测按钮的相关逻辑
- 修改连接状态处理，添加5s超时检测
- 添加弹窗提示

### 3. activity_main.xml
- 移除开始自动监测和停止监测按钮

## 实现步骤
1. 修改TCPClient.java，添加超时检测、connected消息处理和3秒延迟轮询
2. 修改MainActivity.java，移除相关按钮逻辑，添加超时处理
3. 修改activity_main.xml，移除不需要的按钮
4. 测试连接逻辑是否符合需求

## 具体修改点

### TCPClient.java
- 添加`connectedReceived`标志
- 添加5s超时定时器
- 修改`processReceivedBuffer`方法，处理"connected"消息
- 添加3秒延迟开始轮询的逻辑
- 添加超时回调

### MainActivity.java
- 移除`btnStartPolling`和`btnStopPolling`的引用
- 移除相关点击事件处理
- 添加超时处理逻辑
- 修改连接状态更新逻辑

### activity_main.xml
- 移除包含`btnStartPolling`和`btnStopPolling`的LinearLayout
- 调整UI布局