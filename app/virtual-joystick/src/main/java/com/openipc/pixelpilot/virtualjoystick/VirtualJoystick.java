package com.openipc.pixelpilot.virtualjoystick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * 悬浮虚拟摇杆控件
 */
public class VirtualJoystick extends View {

    // 状态枚举
    public enum State { LOCKED, DRAGGING }

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
    private static final int COLOR_ADJUST_BTN = 0x99333333;

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
    private State mState = State.LOCKED;
    private boolean mLocked = true;  // 默认锁定
    private boolean mThrottleSticky = false;
    private boolean mShowAdjustButtons = false;
    private boolean mIsDragging = false;
    private boolean mAutoCenterX = true;

    // 布局参数
    private float mMinDiameter = 120;
    private float mMaxDiameter = 350;
    private float mButtonSize = 40;

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
    private Paint mAdjustBtnPaint;
    private Paint mAdjustTextPaint;
    private Path mCenterIconPath;

    // 缩放手势检测
    private ScaleGestureDetector mScaleDetector;
    private float mLastTouchX, mLastTouchY;
    private long mDragStartTime = 0;
    private static final long DRAG_THRESHOLD_MS = 200;  // 长按200ms进入拖拽模式

    // 拖拽偏移
    private float mDragOffsetX, mDragOffsetY;
    private boolean mIsDraggingContainer = false;

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
        
        mAdjustBtnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAdjustBtnPaint.setColor(COLOR_ADJUST_BTN);
        mAdjustBtnPaint.setStyle(Paint.Style.FILL);
        
        mAdjustTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAdjustTextPaint.setColor(Color.WHITE);
        mAdjustTextPaint.setTextSize(28f);
        mAdjustTextPaint.setTextAlign(Paint.Align.CENTER);
        
        mCenterIconPath = new Path();
        
        // 缩放手势检测
        mScaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                if (!mLocked && !mIsDraggingContainer) {
                    float factor = detector.getScaleFactor();
                    adjustSize(factor * mDiameter);
                    return true;
                }
                return false;
            }
        });

        updateLayout();
    }

    private void updateLayout() {
        int w = getWidth();
        int h = getHeight();
        if (w > 0 && h > 0) {
            mCenterX = w / 2f;
            mCenterY = h / 2f;
            mRadius = mDiameter / 2f;
            mInnerRadius = mRadius * 0.35f;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 居中
        if (mCenterX == 0) {
            mCenterX = w / 2f;
            mCenterY = h / 2f;
        }
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
        
        // 计算摇杆头位置（最大行程 = radius * 0.7）
        float maxTravel = r * 0.7f;
        float normX = mStickX / maxTravel;
        float normY = mStickY / maxTravel;
        float dist = (float)Math.sqrt(normX*normX + normY*normY);
        if (dist > 1.0f) {
            normX /= dist;
            normY /= dist;
        }
        float stickX = cx + normX * maxTravel;
        float stickY = cy + normY * maxTravel;
        
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
        
        // 绘制调整按钮（解锁状态下）
        if (mShowAdjustButtons && !mLocked) {
            drawAdjustButtons(canvas, cx, cy, r);
            // 绘制提示文字
            mChLabelPaint.setAlpha((int)(0.6f * 255 * mOpacity));
            canvas.drawText("拖动调整", cx, cy + r + 55f, mChLabelPaint);
        }
    }

    private void drawAdjustButtons(Canvas canvas, int cx, int cy, float r) {
        // 左按钮（缩小）
        float btnX1 = cx - r - mButtonSize / 2f;
        float btnY = cy;
        canvas.drawCircle(btnX1, btnY, mButtonSize / 2f, mAdjustBtnPaint);
        canvas.drawText("-", btnX1, btnY + mButtonSize / 6f, mAdjustTextPaint);
        
        // 右按钮（放大）
        float btnX2 = cx + r + mButtonSize / 2f;
        canvas.drawCircle(btnX2, btnY, mButtonSize / 2f, mAdjustBtnPaint);
        canvas.drawText("+", btnX2, btnY + mButtonSize / 6f, mAdjustTextPaint);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int action = event.getActionMasked();
        
        // 处理缩放手势
        if (!mLocked) {
            mScaleDetector.onTouchEvent(event);
        }
        
        float x = event.getX();
        float y = event.getY();
        
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = x;
                mLastTouchY = y;
                mDragStartTime = System.currentTimeMillis();
                mIsDraggingContainer = false;
                
                // 检查是否点击了调整按钮
                if (!mLocked && mShowAdjustButtons) {
                    float r = mRadius;
                    float btnX1 = mCenterX - r - mButtonSize / 2f;
                    float btnX2 = mCenterX + r + mButtonSize / 2f;
                    float btnY = mCenterY;
                    
                    if (Math.abs(x - btnX1) < mButtonSize / 2f && Math.abs(y - btnY) < mButtonSize / 2f) {
                        adjustSize(mDiameter * 0.9f);
                        return true;
                    }
                    if (Math.abs(x - btnX2) < mButtonSize / 2f && Math.abs(y - btnY) < mButtonSize / 2f) {
                        adjustSize(mDiameter * 1.1f);
                        return true;
                    }
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (!mLocked) {
                    // 检查是否长时间按住（进入拖拽容器模式）
                    long pressDuration = System.currentTimeMillis() - mDragStartTime;
                    if (pressDuration > DRAG_THRESHOLD_MS) {
                        mIsDraggingContainer = true;
                    }
                    
                    if (mIsDraggingContainer) {
                        float dx = x - mLastTouchX;
                        float dy = y - mLastTouchY;
                        
                        mCenterX += dx;
                        mCenterY += dy;
                        
                        // 边界限制
                        mCenterX = Math.max(mRadius, Math.min(getWidth() - mRadius, mCenterX));
                        mCenterY = Math.max(mRadius, Math.min(getHeight() - mRadius, mCenterY));
                        
                        invalidate();
                    } else {
                        // 正常摇杆操作
                        handleStickMovement(x, y);
                    }
                } else {
                    handleStickMovement(x, y);
                }
                mLastTouchX = x;
                mLastTouchY = y;
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mLocked || !mIsDraggingContainer) {
                    resetStick();
                }
                mIsDraggingContainer = false;
                break;
        }
        
        return true;
    }

    private void handleStickMovement(float touchX, float touchY) {
        float maxTravel = mRadius * 0.7f;
        float dx = touchX - mCenterX;
        float dy = touchY - mCenterY;
        
        // 限制在范围内
        float dist = (float)Math.sqrt(dx*dx + dy*dy);
        if (dist > maxTravel) {
            dx = dx * maxTravel / dist;
            dy = dy * maxTravel / dist;
        }
        
        mStickX = dx;
        mStickY = mThrottleSticky ? mStickY : dy;
        
        updatePwmValues();
        invalidate();
    }

    private void resetStick() {
        if (mAutoCenterX) {
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

    private void adjustSize(float newDiameter) {
        newDiameter = Math.max(mMinDiameter, Math.min(mMaxDiameter, newDiameter));
        mDiameter = (int)newDiameter;
        mRadius = mDiameter / 2f;
        mInnerRadius = mRadius * 0.35f;
        invalidate();
    }

    private void enterDraggingState() {
        if (!mLocked) {
            mShowAdjustButtons = true;
            invalidate();
        }
    }

    private void exitDraggingState() {
        mShowAdjustButtons = false;
        mIsDraggingContainer = false;
        invalidate();
    }

    // 公开 API
    public void setChannelConfig(ChannelConfig[] channels) {
        mChannels = channels;
    }

    public void setSettings(AppSettings settings) {
        mSettings = settings;
        this.mThrottleSticky = settings.throttleSticky;
        this.mAutoCenterX = settings.autoCenterX;
    }

    public void setOpacity(float opacity) {
        this.mOpacity = Math.max(0f, Math.min(1f, opacity));
        mBorderPaint.setColor(Color.argb((int)(0.9f * 255 * mOpacity), 184, 115, 51));
        mFillPaint.setColor(Color.argb((int)(mOpacity * 255), 30, 30, 30));
        invalidate();
    }

    public void setLocked(boolean locked) {
        this.mLocked = locked;
        if (locked) {
            exitDraggingState();
        } else {
            enterDraggingState();
        }
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
    public int getCenterX() { return (int)mCenterX; }
    public int getCenterY() { return (int)mCenterY; }
}
