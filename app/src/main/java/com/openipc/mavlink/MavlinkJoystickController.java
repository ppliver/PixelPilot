package com.openipc.mavlink;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * MAVLink 摇杆控制器
 * 将摇杆数据转换为 MAVLink RC_CHANNELS 消息并通过 UDP 发送
 */
public class MavlinkJoystickController {

    private static final String TAG = "MavlinkJoystick";

    // MAVLink 常量
    private static final byte MAVLINK_VERSION = 2;
    private static final byte MAVLINK_IFLAG = 0x03;
    private static final int MAVLINK_MAX_PACKET_LEN = 2000;
    
    // MAVLink 消息 ID
    private static final byte MAVLINK_MSG_ID_RC_CHANNELS_PACKED = 87;
    private static final byte MAVLINK_MSG_ID_RC_CHANNELS = 35;

    // 摇杆通道映射 (默认四通道)
    private static final int RC_CH1 = 0;  // Roll (横滚)
    private static final int RC_CH2 = 1;  // Pitch (俯仰)
    private static final int RC_CH3 = 2;  // Throttle (油门)
    private static final int RC_CH4 = 3;  // Yaw (偏航)

    // UDP 目标地址 (可通过设置修改)
    private String targetAddress = "192.168.1.1";
    private int targetPort = 14550;

    private DatagramSocket socket;
    private volatile boolean enabled = false;

    // 摇杆值 (归一化 -1.0 ~ 1.0 -> MAVLink 0 ~ 65535)
    private float[] rcValues = new float[18];

    public MavlinkJoystickController(Context context) {
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            Log.d(TAG, "UDP socket created for MAVLink joystick");
        } catch (IOException e) {
            Log.e(TAG, "Failed to create UDP socket", e);
        }
    }

    /**
     * 设置UDP目标地址和端口
     */
    public void setTarget(String address, int port) {
        this.targetAddress = address;
        this.targetPort = port;
    }

    /**
     * 启用/禁用摇杆控制
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        Log.d(TAG, "Joystick control " + (enabled ? "enabled" : "disabled"));
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 发送摇杆数据
     * @param roll 横滚 (-1.0 ~ 1.0)
     * @param pitch 俯仰 (-1.0 ~ 1.0)
     * @param throttle 油门 (0.0 ~ 1.0, 建议底部保持在0.5)
     * @param yaw 偏航 (-1.0 ~ 1.0)
     */
    public void sendJoystickData(float roll, float pitch, float throttle, float yaw) {
        if (!enabled || socket == null) {
            return;
        }

        // 更新摇杆值
        rcValues[RC_CH1] = normalizeToMavlink(roll, -1.0f, 1.0f);
        rcValues[RC_CH2] = normalizeToMavlink(pitch, -1.0f, 1.0f);
        rcValues[RC_CH3] = normalizeToMavlink(throttle, 0.0f, 1.0f);
        rcValues[RC_CH4] = normalizeToMavlink(yaw, -1.0f, 1.0f);

        // 发送 MAVLink RC_CHANNELS 消息
        sendRcChannelsMessage();
    }

    /**
     * 单摇杆模式 (左摇杆控制横滚+俯仰，右摇杆控制油门+偏航)
     */
    public void sendLeftJoystick(float x, float y) {
        sendJoystickData(x, -y, rcValues[RC_CH3] / 65535.0f, rcValues[RC_CH4]);
    }

    public void sendRightJoystick(float x, float y) {
        sendJoystickData(rcValues[RC_CH1] / 65535.0f, rcValues[RC_CH2] / 65535.0f, 
                        normalizeToMavlink(y, -1.0f, 1.0f), x);
    }

    /**
     * 发送 MAVLink RC_CHANNELS_PACKED 消息 (更紧凑)
     */
    private void sendRcChannelsMessage() {
        try {
            // MAVLink 2 头部
            byte[] header = new byte[12];
            header[0] = (byte) 0xFE; // 魔数
            header[1] = (byte) 8;    // 长度
            header[2] = (byte) 0x01; // 序列号 (简单递增)
            header[3] = (byte) 0x00; // 系统ID
            header[4] = (byte) 0x01; // 组件ID
            header[5] = MAVLINK_MSG_ID_RC_CHANNELS_PACKED;
            
            // 负载：8个通道的16位值 + rssi
            ByteBuffer buffer = ByteBuffer.allocate(8 * 2 + 1);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < 8; i++) {
                buffer.putShort((short) rcValues[i]);
            }
            buffer.put((byte) 255); // RSSI
            
            byte[] payload = buffer.array();
            
            // 计算CRC (简化版本，实际应使用MAVLink CRC算法)
            byte[] crc = new byte[]{(byte) 0xAA, (byte) 0xBB};
            
            // 计算检查符
            byte checksum = calculateChecksum(header, payload);
            
            // 组合完整消息
            byte[] message = new byte[header.length + payload.length + 2]; // +2 for checksum and length
            System.arraycopy(header, 0, message, 0, header.length);
            System.arraycopy(payload, 0, message, header.length, payload.length);
            message[message.length - 2] = checksum;
            message[message.length - 1] = 0; // signing microsecond tail (简化)
            
            InetAddress target = InetAddress.getByName(targetAddress);
            DatagramPacket packet = new DatagramPacket(message, message.length, target, targetPort);
            socket.send(packet);
            
            Log.d(TAG, "Sent RC_CHANNELS_PACKED to " + targetAddress + ":" + targetPort);
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to send MAVLink message", e);
        }
    }

    /**
     * 简单的checksum计算 (实际项目应使用MAVLink官方CRC)
     */
    private byte calculateChecksum(byte[] header, byte[] payload) {
        byte checksum = 0;
        for (byte b : header) {
            checksum ^= b;
        }
        for (byte b : payload) {
            checksum ^= b;
        }
        return checksum;
    }

    /**
     * 将值归一化到 MAVLink 范围 (0 ~ 65535)
     */
    private int normalizeToMavlink(float value, float min, float max) {
        float normalized = (value - min) / (max - min);
        normalized = Math.max(0f, Math.min(1f, normalized));
        return (int) (normalized * 65535f);
    }

    /**
     * 关闭资源
     */
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            socket = null;
        }
        Log.d(TAG, "Joystick controller closed");
    }
}
