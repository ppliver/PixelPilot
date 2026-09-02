package com.openipc.pixelpilot.virtualjoystick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * 悬浮虚拟摇杆控件 - 简化版
 */
public class VirtualJoystick extends View {

    // 通道配置
    public static class ChannelConfig implements Serializable {
        public int id;
        public String name;
        public int min = 1000;
        public int max = 2000;
        public int trim = 0;
        public boolean invert = false;
        
        public ChannelConfig() {
            this.id = 0;
            this.name = "CH";
        }
        
        public ChannelConfig(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    // 全局设置
    public static class AppSettings implements Serializable {
        public boolean throttleSticky = false;
        public boolean autoCenterX = true;
        public int mode = 2;
    }

    // 颜色常量
    private static final int COLOR_RING_BORDER = 0xFFB87333;
    private static final int COLOR_CROSSHAIR = 0x99FFFFFF;
    private static final int COLOR_CENTER_ICON = 0xFF00CED1;
    private static final int COLOR_PWM_TEXT = 0xFF00CED1;
    private static final int COLOR_CH_LABEL = 0xFFDDDDDD;

    // 摇杆参数
    private float mRadius = 75f;
    private float mInnerRadius = 26f;
    private float mStickX = 0;
    private float mStickY = 0;
    private float mCenterX = 0;
    private float mCenterY = 0;
    private int mDiameter = 150;
    private float mOpacity = 0.85f;

    // 状态
    private boolean mLocked = false;
    private boolean mThrottleSticky = false;

    // 通道配置
    private ChannelConfig[] mChannels = new ChannelConfig[] {
        new ChannelConfig(1, "转向"),
        new ChannelConfig(2, "俯仰"),
        new ChannelConfig(3, "油门"),
        new ChannelConfig(4, "航向")
    };
    private AppSettings mSettings = new AppSettings();

    // PWM 值
    private int[] mPwmValues = {1500, 1500, 1500, 1500};

    // 画笔
    private Paint mBorderPaint;
    private Paint mFillPaint;
    private Paint mCrosshairPaint;
    private Paint mStickPaint;
    private Paint mCenterIconPaint;
    private Paint mPwmTextPaint;
    private Paint mChLabelPaint;
    private Path mCenterIconPath;

    public VirtualJoystick(@NonNull Context context) {
        super(context);
        init(context);
    }

    public VirtualJoystick(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VirtualJoystick(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setFocusableInTouchMode(true);
        setFocusable(true);
        
        // 初始化画笔
        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(4f);
        mBorderPaint.setColor(Color.argb((int)(0.9f * 255 * mOpacity), 184, 115, 51));
        
        mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(Color.argb((int)(mOpacity * 255), 30, 30, 30));
        
        mCrosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCrosshairPaint.setColor(Color.argb((int)(0.6f * 255), 255, 255, 255));
        mCrosshairPaint.setStrokeWidth(2f);
        
        mStickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStickPaint.setColor(Color.parseColor("#40FFFFFF"));
        
        mCenterIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCenterIconPaint.setColor(COLOR_CENTER_ICON);
        mCenterIconPaint.setStyle(Paint.Style.FILL);
        
        mPwmTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPwmTextPaint.setColor(COLOR_PWM_TEXT);
        mPwmTextPaint.setTextSize(28f);
        mPwmTextPaint.setTextAlign(Paint.Align.CENTER);
        
        mChLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mChLabelPaint.setColor(COLOR_CH_LABEL);
        mChLabelPaint.setTextSize(22f);
        mChLabelPaint.setTextAlign(Paint.Align.CENTER);
        
        mCenterIconPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 居中
        mCenterX = w / 2f;
        mCenterY = h / 2f;
        mRadius = mDiameter / 2f;
        mInnerRadius = mRadius * 0.35f;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        
        int cx = (int)mCenterX;
        int cy = (int)mCenterY;
        float r = mRadius;
        float stickRadius = mInnerRadius;
        
        // 计算摇杆头位置
        float normX = mStickX / r;
        float normY = mStickY / r;
        float dist = (float)Math.sqrt(normX*normX + normY*normY);
        if (dist > 1.0f) {
            normX /= dist;
            normY /= dist;
        }
        float stickX = cx + normX * r;
        float stickY = cy + normY * r;
        
        // 绘制外圆边框
        mBorderPaint.setAlpha((int)(0.9f * 255 * mOpacity));
        canvas.drawCircle(cx, cy, r, mBorderPaint);
        
        // 绘制填充背景
        mFillPaint.setAlpha((int)(mOpacity * 255));
        canvas.drawCircle(cx, cy, r, mFillPaint);
        
        // 绘制十字准线
        mCrosshairPaint.setAlpha((int)(0.6f * 255 * mOpacity));
        canvas.drawLine(cx - r * 0.8f, cy, cx + r * 0.8f, cy, mCrosshairPaint);
        canvas.drawLine(cx, cy - r * 0.8f, cx, cy + r * 0.8f, mCrosshairPaint);
        
        // 绘制摇杆头
        canvas.drawCircle(stickX, stickY, stickRadius, mStickPaint);
        canvas.drawCircle(stickX, stickY, stickRadius, mBorderPaint);
        
        // 绘制中心四向箭头图标
        float iconSize = stickRadius * 0.6f;
        Path path = mCenterIconPath;
        path.reset();
        
        // 上箭头
        path.moveTo(stickX, stickY - iconSize);
        path.lineTo(stickX - iconSize * 0.4f, stickY - iconSize * 0.2f);
        path.lineTo(stickX + iconSize * 0.4f, stickY - iconSize * 0.2f);
        path.close();
        
        // 下箭头
        path.moveTo(stickX, stickY + iconSize);
        path.lineTo(stickX - iconSize * 0.4f, stickY + iconSize * 0.2f);
        path.lineTo(stickX + iconSize * 0.4f, stickY + iconSize * 0.2f);
        path.close();
        
        // 左箭头
        path.moveTo(stickX - iconSize, stickY);
        path.lineTo(stickX - iconSize * 0.2f, stickY - iconSize * 0.4f);
        path.lineTo(stickX - iconSize * 0.2f, stickY + iconSize * 0.4f);
        path.close();
        
        // 右箭头
        path.moveTo(stickX + iconSize, stickY);
        path.lineTo(stickX + iconSize * 0.2f, stickY - iconSize * 0.4f);
        path.lineTo(stickX + iconSize * 0.2f, stickY + iconSize * 0.4f);
        path.close();
        
        mCenterIconPaint.setAlpha((int)(0.9f * 255 * mOpacity));
        canvas.drawPath(path, mCenterIconPaint);
        
        // 绘制 PWM 显示
        mPwmTextPaint.setAlpha((int)(0.8f * 255 * mOpacity));
        String pwmText = "CH1:" + mPwmValues[0] + " CH2:" + mPwmValues[1];
        canvas.drawText(pwmText, cx, cy + r + 30f, mPwmTextPaint);
        
        // 绘制通道标签
        mChLabelPaint.setAlpha((int)(0.7f * 255 * mOpacity));
        canvas.drawText("CH3", cx, cy - r - 10f, mChLabelPaint);
        canvas.drawText("CH1", cx, cy + r + 25f, mChLabelPaint);
        canvas.drawText("CH4", cx - r - 20f, cy + 6f, mChLabelPaint);
        canvas.drawText("CH2", cx + r + 20f, cy + 6f, mChLabelPaint);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                handleTouch(x, y);
                return true;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                resetStick();
                return true;
        }
        
        return super.onTouchEvent(event);
    }

    private void handleTouch(float touchX, float touchY) {
        float dx = touchX - mCenterX;
        float dy = touchY - mCenterY;
        
        // 限制在圆内
        float dist = (float)Math.sqrt(dx*dx + dy*dy);
        if (dist > mRadius) {
            dx = dx * mRadius / dist;
            dy = dy * mRadius / dist;
        }
        
        mStickX = dx;
        mStickY = mThrottleSticky ? mStickY : dy;
        
        updatePwmValues();
        invalidate();
    }

    private void resetStick() {
        if (mSettings.autoCenterX) {
            mStickX = 0;
        }
        if (!mSettings.throttleSticky) {
            mStickY = 0;
        }
        updatePwmValues();
        invalidate();
    }

    private void updatePwmValues() {
        float centerX = 1500f;
        float range = 500f * 0.7f;
        
        // CH1 (转向)
        int ch1 = (int)(centerX + mStickX * range * (mChannels[0].invert ? -1 : 1) + mChannels[0].trim);
        mPwmValues[0] = Math.max(mChannels[0].min, Math.min(mChannels[0].max, ch1));
        
        // CH2 (俯仰)
        int ch2 = (int)(centerX + mStickY * range * (mChannels[1].invert ? -1 : 1) + mChannels[1].trim);
        mPwmValues[1] = Math.max(mChannels[1].min, Math.min(mChannels[1].max, ch2));
        
        // CH3 (油门)
        mPwmValues[2] = mSettings.throttleSticky ? (int)(centerX + mStickY * range) : 1500;
        
        // CH4 (航向)
        int ch4 = (int)(centerX + mStickX * range * (mChannels[3].invert ? -1 : 1) + mChannels[3].trim);
        mPwmValues[3] = Math.max(mChannels[3].min, Math.min(mChannels[3].max, ch4));
        
        if (mListener != null) {
            mListener.onChannelsChanged(mPwmValues);
        }
    }

    // 公开 API
    public void setChannelConfig(ChannelConfig[] channels) {
        mChannels = channels;
    }

    public void setSettings(AppSettings settings) {
        mSettings = settings;
    }

    public void setOpacity(float opacity) {
        this.mOpacity = Math.max(0f, Math.min(1f, opacity));
        mBorderPaint.setColor(Color.argb((int)(0.9f * 255 * mOpacity), 184, 115, 51));
        mFillPaint.setColor(Color.argb((int)(mOpacity * 255), 30, 30, 30));
        invalidate();
    }

    public void setLocked(boolean locked) {
        this.mLocked = locked;
    }

    public void setThrottleSticky(boolean sticky) {
        this.mThrottleSticky = sticky;
        mSettings.throttleSticky = sticky;
    }

    public void setPosition(float x, float y) {
        mCenterX = x;
        mCenterY = y;
        invalidate();
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
    }

    @Nullable
    private OnChannelsChangedListener mListener;

    public interface OnChannelsChangedListener {
        void onChannelsChanged(int[] pwmValues);
    }

    public void setOnChannelsChangedListener(OnChannelsChangedListener listener) {
        this.mListener = listener;
    }

    // Getter
    public float getRadius() { return mRadius; }
    public float getDiameter() { return mDiameter; }
    public int[] getPwmValues() { return mPwmValues.clone(); }
    public boolean isLocked() { return mLocked; }
    public float getOpacity() { return mOpacity; }
}
