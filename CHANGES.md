# PixelPilot 修改总结

## 已完成的功能改动

### 1. 虚拟摇杆控制 (Virtual Joystick)
- ✅ 创建 `JoystickView.java` - 自定义摇杆控件
  - 支持触摸事件处理
  - 归一化输出 (-1.0 ~ 1.0)
  - 视觉反馈（外圆底座 + 摇杆头 + 十字准线）
  
- ✅ 创建 `MavlinkJoystickController.java` - MAVLink 控制器
  - UDP 发送 MAVLink RC_CHANNELS_PACKED 消息
  - 四通道控制：Roll, Pitch, Throttle, Yaw
  - 可配置目标地址和端口
  
- ✅ 集成到 `VideoActivity.java`
  - 左右双摇杆支持
  - 左摇杆：横滚 + 俯仰
  - 右摇杆：油门 + 偏航
  - 自动回中逻辑
  
- ✅ 更新布局文件 `activity_video.xml`
  - 添加左摇杆（左下角）
  - 添加右摇杆（右下角）

### 2. 移除 wfbngrtl8812 模块
- ✅ 注释掉 `settings.gradle` 中的模块引用
- ✅ 注释掉 `app/build.gradle` 中的依赖
- ✅ 移除所有 Java 导入和引用
- ✅ 注释掉相关方法调用

### 3. 移除 VPN 功能
- ✅ 从 `AndroidManifest.xml` 移除 VPN Service 声明
- ✅ 注释掉 `BIND_VPN_SERVICE` 权限
- ✅ 移除 `startVpnService()` 方法及调用
- ✅ 清理 `onPause/onResume` 中的 VPN 相关代码

### 4. 版本升级
- ✅ 版本从 0.23.0 升级到 0.24.0

## 控制映射说明

| 摇杆 | X轴 | Y轴 |
|------|-----|-----|
| 左摇杆 | Roll (横滚) | Pitch (俯仰) |
| 右摇杆 | Yaw (偏航) | Throttle (油门) |

## MAVLink 配置

- 默认目标地址：`192.168.1.1`
- 默认目标端口：`14550`
- 消息类型：`RC_CHANNELS_PACKED` (ID: 87)
- 数据范围：0 ~ 65535 (MAVLink 标准)

## 文件清单

### 新增文件
- `app/src/main/java/com/openipc/pixelpilot/JoystickView.java`
- `app/src/main/java/com/openipc/mavlink/MavlinkJoystickController.java`

### 修改文件
- `app/src/main/java/com/openipc/pixelpilot/VideoActivity.java`
- `app/src/main/res/layout/activity_video.xml`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle`
- `settings.gradle`

## Git 提交信息

```
feat: 添加虚拟摇杆控制 + 移除 wfbngrtl8812 模块 + 移除 VPN 功能
```

已推送到：`https://github.com/ppliver/PixelPilot.git`
