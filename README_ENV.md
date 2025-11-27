# Android Sensor 项目运行环境说明

## 1. 项目概述

本项目是一个基于Android平台的传感器应用示例，演示如何在Android应用中使用各种传感器功能。

## 2. 开发环境要求

### 2.1 基础环境
- **操作系统**：Windows、macOS 或 Linux
- **JDK版本**：Java 11
- **Android Studio**：建议使用最新稳定版本，至少需支持Android Gradle Plugin 8.13.1

### 2.2 Android SDK要求
- **编译SDK版本**：36
- **最低SDK版本**：24 (Android 7.0 Nougat)
- **目标SDK版本**：36

### 2.3 构建工具
- **Gradle版本**：8.13
- **Android Gradle Plugin版本**：8.13.1

## 3. 项目依赖

### 3.1 核心依赖库
- **AppCompat**：1.7.1
- **Material Design**：1.13.0
- **Activity**：1.11.0
- **ConstraintLayout**：2.2.1

### 3.2 测试依赖
- **JUnit**：4.13.2
- **AndroidX Test JUnit**：1.3.0
- **Espresso Core**：3.7.0

## 4. 环境配置步骤

### 4.1 安装必要工具
1. 下载并安装最新版 [Android Studio](https://developer.android.com/studio)
2. 在Android Studio中安装Java 11
3. 安装Android SDK 36及相关工具

### 4.2 导入项目
1. 打开Android Studio
2. 选择 "Open an Existing Project"
3. 导航至项目目录并选择项目根目录

### 4.3 同步Gradle
1. Android Studio会自动检测并提示同步Gradle
2. 点击 "Sync Now" 按钮
3. 等待Gradle同步完成，确保所有依赖项都已正确下载

## 5. 运行项目

### 5.1 使用模拟器运行
1. 在Android Studio中点击AVD Manager图标
2. 创建或选择一个API级别24或更高的模拟器
3. 点击运行按钮（绿色三角形）运行应用

### 5.2 使用真机运行
1. 确保您的Android设备已启用USB调试
2. 使用USB数据线连接设备到电脑
3. 在Android Studio中选择您的设备
4. 点击运行按钮（绿色三角形）运行应用

## 6. 项目配置文件说明

### 6.1 build.gradle（项目级）
定义了项目级别的构建配置和公共插件设置。

### 6.2 app/build.gradle（模块级）
包含应用模块的具体配置，如应用ID、SDK版本、构建类型和依赖项。

### 6.3 gradle/libs.versions.toml
使用版本目录管理依赖版本，便于统一更新和维护依赖库版本。

### 6.4 gradle-wrapper.properties
配置Gradle包装器，指定使用的Gradle版本。

## 7. 注意事项

- 确保您的Android Studio已更新到支持Android Gradle Plugin 8.13.1的版本
- 如果遇到构建问题，尝试清理项目（Build > Clean Project）并重新构建
- 如需使用特定的Android设备传感器，确保在应用清单文件中请求相应权限