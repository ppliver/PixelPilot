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
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * 悬浮虚拟摇杆控件 - 参考 RC-Pilot Pro 风格
 */
public class VirtualJoystick extends View {

    // 摇杆状态
    public enum State { LOCKED, DRAGGING }

    // 通道配置（Serializable 用于跨进程传递）
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

    // 全局设置（Serializable）
    public static class AppSettings implements Serializable {
        public boolean throttleSticky = false;
        public boolean autoCenterX = true;
        public int mode = 2; // 1=Mode1, 2=Mode2
    }

    // 颜色常量
    private static final int COLOR_RING_BORDER = 0xFFB87333;  // 金棕色
    private static final int COLOR_RING_FILL = 0x66222222;   // 半透明深灰
    private static final int COLOR_CROSSHAIR = 0x99FFFFFF;   // 半透明白色
    private static final int COLOR_CENTER_ICON = 0xFF00CED1; // 青色
    private static final int COLOR_PWM_TEXT = 0xFF00CED1;    // 青色
    private static final int COLOR_CH_LABEL = 0xFFDDDDDD;   // 浅灰色
    private static final int COLOR_ADJUST_BTN = 0x99333333; // 深色半透明
    private static final int COLOR_ADJUST_TEXT = 0xFFFFFFFF; // 白色

    // 摇杆参数
    private float mRadius;           // 外圆半径
    private float mInnerRadius;      // 内圆半径（摇杆头）
    private float mStickX;           // 摇杆头 X（相对圆心）
    private float mStickY;           // 摇杆头 Y（相对圆心）
    private float mCenterX;          // 摇杆中心屏幕坐标
    private float mCenterY;
    private int mDiameter = 150;     // 默认直径（像素）
    private float mOpacity = 0.85f;  // 透明度 0.0-1.0

    // 状态
    private State mState = State.LOCKED;
    private boolean mLocked = false;   // 锁定位置
    private boolean mThrottleSticky = false;
    private boolean mShowAdjustButtons = false;  // 解锁时显示 +/- 按钮
    private boolean mIsDragging = false;

    // 布局参数
    private float mMinDiameter = 120;
    private float mMaxDiameter = 350;
    private float mButtonSize = 40;  // 调整按钮尺寸

    // 通道配置（Mode2: 左=CH4/CH3, 右=CH1/CH2）
    private ChannelConfig[] mChannels = new ChannelConfig[] {
        new ChannelConfig(1, "转向"),
        new ChannelConfig(2, "俯仰"),
        new ChannelConfig(3, "油门"),
        new ChannelConfig(4, "航向")
    };
    private AppSettings mSettings = new AppSettings();

    // PWM 值
    private int[] mPwmValues = {1500, 1500, 1500, 1500};

    // UI 引用
    @Nullable
    private Button mMinusBtn, mPlusBtn;
    @Nullable
    private TextView mTitleText;

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

    // 拖拽检测
    private ScaleGestureDetector mScaleDetector;
    private float mLastTouchX, mLastTouchY;
    private long mDragStartTime = 0;
    private static final long DRAG_THRESHOLD_MS = 200;  // 长按 200ms 后进入拖拽模式
    private static final float PINCH_THRESHOLD = 0.85f;  // 缩放手势阈值

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
        updatePaintColors(mOpacity);
        
        mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFillPaint.setStyle(Paint.Style.FILL);
        updatePaintColors(mOpacity);
        
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
        mAdjustTextPaint.setColor(COLOR_ADJUST_TEXT);
        mAdjustTextPaint.setTextSize(28f);
        mAdjustTextPaint.setTextAlign(Paint.Align.CENTER);
        
        mCenterIconPath = new Path();
        
        // 缩放手势检测
        mScaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                if (!mLocked && !mIsDragging) {
                    float factor = detector.getScaleFactor();
                    adjustSize(factor * mDiameter);
                    return true;
                }
                return false;
            }
        });
        
        updateLayout();
    }

    private void updatePaintColors(float opacity) {
        int alpha = (int)(opacity * 255);
        int borderAlpha = (int)(0.9f * alpha);
        
        mBorderPaint.setColor(Color.argb(borderAlpha, 184, 115, 51)); // 金棕色
        mFillPaint.setColor(Color.argb(alpha, 30, 30, 30)); // 深灰半透明
    }

    private void updateLayout() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        
        mRadius = mDiameter / 2f;
        mInnerRadius = mRadius * 0.35f;
        
        // 居中布局
        mCenterX = w / 2f;
        mCenterY = h / 2f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateLayout();
        // 如果有按钮，重新定位
        if (mMinusBtn != null && mPlusBtn != null) {
            positionButtons();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        
        drawJoystick(canvas);
        
        if (mShowAdjustButtons && mState == State.DRAGGING) {
            drawAdjustButtons(canvas);
        }
        
        drawPwmDisplay(canvas);
        drawChannelLabels(canvas);
        
        if (mState == State.DRAGGING && !mLocked) {
            drawDragHint(canvas);
        }
    }

    private void drawJoystick(Canvas canvas) {
        int cx = (int)mCenterX;
        int cy = (int)mCenterY;
        float r = mRadius;
        float stickRadius = mInnerRadius;
        
        // 计算摇杆头位置
        float normX = mStickX / r;
        float normY = mStickY / r;
        float len = Math.min(1.0f, (float)Math.sqrt(normX*normX + normY*normY));
        float stickX = cx + normX * r * len;
        float stickY = cy + normY * r * len;
        
        // 绘制外圆边框
        canvas.drawCircle(cx, cy, r, mBorderPaint);
        
        // 绘制填充背景
        canvas.drawCircle(cx, cy, r, mFillPaint);
        
        // 绘制十字准线
        float crosshairAlpha = (int)(0.5f * 255 * mOpacity);
        mCrosshairPaint.setColor(Color.argb(crosshairAlpha, 255, 255, 255));
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
    }

    private void drawAdjustButtons(Canvas canvas) {
        int cx = (int)mCenterX;
        int cy = (int)mCenterY;
        float r = mRadius;
        
        // 左按钮（缩小）
        float btnX1 = cx - r - mButtonSize / 2f;
        float btnY = cy;
        canvas.drawCircle(btnX1, btnY, mButtonSize / 2f, mAdjustBtnPaint);
        canvas.drawText("-", btnX1, btnY + mButtonSize / 6f, mAdjustTextPaint);
        
        // 右按钮（放大）
        float btnX2 = cx + r + mButtonSize / 2f;
        canvas.drawCircle(btnX2, btnY, mButtonSize / 2f, mAdjustBtnPaint);
        canvas.drawText("+", btnX2, btnY + mButtonSize / 6f, mAdjustTextPaint);
        
        // 上按钮（可选）
        float btnYUp = cy - r - mButtonSize / 2f;
        float btnXUp = cx;
        canvas.drawCircle(btnXUp, btnYUp, mButtonSize / 2f, mAdjustBtnPaint);
        canvas.drawText("▲", btnXUp, btnYUp + mButtonSize / 3f, mAdjustTextPaint);
    }

    private void drawPwmDisplay(Canvas canvas) {
        int cx = (int)mCenterX;
        int cy = (int)mCenterY;
        String pwmText = "CH1:" + mPwmValues[0] + " CH2:" + mPwmValues[1];
        mPwmTextPaint.setAlpha((int)(0.8f * 255 * mOpacity));
        canvas.drawText(pwmText, cx, cy + mRadius + 30f, mPwmTextPaint);
    }

    private void drawChannelLabels(Canvas canvas) {
        int cx = (int)mCenterX;
        int cy = (int)mCenterY;
        float r = mRadius;
        
        mChLabelPaint.setAlpha((int)(0.7f * 255 * mOpacity));
        
        // 顶部标签
        canvas.drawText("CH3", cx, cy - r - 10f, mChLabelPaint);
        // 底部标签
        canvas.drawText("CH1", cx, cy + r + 25f, mChLabelPaint);
        // 左侧标签
        canvas.drawText("CH4", cx - r - 20f, cy + 6f, mChLabelPaint);
        // 右侧标签
        canvas.drawText("CH2", cx + r + 20f, cy + 6f, mChLabelPaint);
    }

    private void drawDragHint(Canvas canvas) {
        int cx = (int)mCenterX;
        int cy = (int)mCenterY;
        String hint = "拖动调整";
        mChLabelPaint.setAlpha((int)(0.6f * 255 * mOpacity));
        canvas.drawText(hint, cx, cy + mRadius + 55f, mChLabelPaint);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int action = event.getActionMasked();
        
        // 处理缩放手势
        if (!mLocked) {
            mScaleDetector.onTouchEvent(event);
        }
        
        // 处理拖拽和点击
        float x = event.getX();
        float y = event.getY();
        
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = x;
                mLastTouchY = y;
                mDragStartTime = System.currentTimeMillis();
                mIsDragging = false;
                
                // 检查是否点击了调整按钮
                if (mState == State.DRAGGING && !mLocked) {
                    checkAdjustButtonTap(x, y);
                    if (mIsDragging) return true;
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                // 检查是否长时间按住（进入拖拽位置模式）
                if (mState == State.LOCKED && !mLocked) {
                    long pressDuration = System.currentTimeMillis() - mDragStartTime;
                    if (pressDuration > DRAG_THRESHOLD_MS) {
                        enterDraggingState();
                    }
                }
                
                if (mState == State.DRAGGING && mIsDragging) {
                    float dx = x - mLastTouchX;
                    float dy = y - mLastTouchY;
                    
                    // 移动摇杆位置
                    mCenterX += dx;
                    mCenterY += dy;
                    
                    // 边界限制
                    mCenterX = Math.max(mRadius, Math.min(getWidth() - mRadius, mCenterX));
                    mCenterY = Math.max(mRadius, Math.min(getHeight() - mRadius, mCenterY));
                    
                    // 更新按钮位置
                    positionButtons();
                    
                    invalidate();
                } else if (mState == State.LOCKED) {
                    // 正常摇杆操作
                    handleStickMovement(x, y);
                }
                mLastTouchX = x;
                mLastTouchY = y;
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mState == State.LOCKED) {
                    resetStick();
                }
                break;
        }
        
        return true;
    }

    private void handleStickMovement(float touchX, float touchY) {
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
        if (mAutoCenterX) {
            mStickX = 0;
        }
        if (!mThrottleSticky) {
            mStickY = 0;
        }
        updatePwmValues();
        invalidate();
    }

    private void updatePwmValues() {
        float centerX = mChannels[0].min + (mChannels[0].max - mChannels[0].min) / 2f;
        float range = (mChannels[0].max - mChannels[0].min) / 2f * 0.7f;
        
        // CH1 (X轴): 转向
        int ch1 = (int)(centerX + mStickX * range * (mChannels[0].invert ? -1 : 1) + mChannels[0].trim);
        mPwmValues[0] = Math.max(mChannels[0].min, Math.min(mChannels[0].max, ch1));
        
        // CH2 (Y轴): 俯仰
        int ch2 = (int)(centerX + mStickY * range * (mChannels[1].invert ? -1 : 1) + mChannels[1].trim);
        mPwmValues[1] = Math.max(mChannels[1].min, Math.min(mChannels[1].max, ch2));
        
        // CH3 (油门): 固定在中点或根据粘性油门调整
        mPwmValues[2] = mThrottleSticky ? (int)(centerX + mStickY * range) : 1500;
        
        // CH4 (航向): 根据 X 轴调整
        int ch4 = (int)(centerX + mStickX * range * (mChannels[3].invert ? -1 : 1) + mChannels[3].trim);
        mPwmValues[3] = Math.max(mChannels[3].min, Math.min(mChannels[3].max, ch4));
        
        if (mListener != null) {
            mListener.onChannelsChanged(mPwmValues);
        }
    }

    private void checkAdjustButtonTap(float x, float y) {
        float r = mRadius;
        float btnDist = mButtonSize;
        
        // 左按钮
        float btnX1 = mCenterX - r - btnDist / 2f;
        float btnY = mCenterY;
        if (Math.abs(x - btnX1) < btnDist / 2f && Math.abs(y - btnY) < btnDist / 2f) {
            adjustSize(mDiameter * 0.9f);
            mIsDragging = true;
            return;
        }
        
        // 右按钮
        float btnX2 = mCenterX + r + btnDist / 2f;
        if (Math.abs(x - btnX2) < btnDist / 2f && Math.abs(y - btnY) < btnDist / 2f) {
            adjustSize(mDiameter * 1.1f);
            mIsDragging = true;
            return;
        }
    }

    private void adjustSize(float newDiameter) {
        newDiameter = Math.max(mMinDiameter, Math.min(mMaxDiameter, newDiameter));
        mDiameter = (int)newDiameter;
        mRadius = mDiameter / 2f;
        mInnerRadius = mRadius * 0.35f;
        positionButtons();
        invalidate();
    }

    private void positionButtons() {
        if (mMinusBtn != null && mPlusBtn != null && mTitleText != null) {
            int btnHalfSize = (int)(mButtonSize / 2);
            int r = mDiameter / 2;
            
            // 左按钮
            int left = (int)(mCenterX - r - mButtonSize);
            int top = (int)(mCenterY - mButtonSize / 2);
            mMinusBtn.layout(left, top, left + mButtonSize, top + mButtonSize);
            
            // 右按钮
            left = (int)(mCenterX + r);
            mPlusBtn.layout(left, top, left + mButtonSize, top + mButtonSize);
            
            // 标题文字
            int titleLeft = (int)(mCenterX - 50);
            int titleTop = (int)(mCenterY - mRadius - 40);
            mTitleText.layout(titleLeft, titleTop, titleLeft + 100, titleTop + 30);
        }
    }

    private void enterDraggingState() {
        if (!mLocked) {
            mState = State.DRAGGING;
            mShowAdjustButtons = true;
            positionButtons();
            invalidate();
        }
    }

    public void exitDraggingState() {
        mState = State.LOCKED;
        mShowAdjustButtons = false;
        mIsDragging = false;
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
        updatePaintColors(mOpacity);
        invalidate();
    }

    public void setLocked(boolean locked) {
        this.mLocked = locked;
        if (locked) {
            exitDraggingState();
        }
    }

    public void setThrottleSticky(boolean sticky) {
        this.mThrottleSticky = sticky;
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

    // 监听器
    @Nullable
    private OnChannelsChangedListener mListener;

    public interface OnChannelsChangedListener {
        void onChannelsChanged(int[] pwmValues);
    }

    public void setOnChannelsChangedListener(OnChannelsChangedListener listener) {
        this.mListener = listener;
    }

    // Getter 方法
    public float getRadius() { return mRadius; }
    public float getDiameter() { return mDiameter; }
    public int[] getPwmValues() { return mPwmValues.clone(); }
    public ChannelConfig[] getChannels() { return mChannels.clone(); }
    public AppSettings getSettings() { return new AppSettings(mSettings); }
    public boolean isLocked() { return mLocked; }
    public boolean isThrottleSticky() { return mThrottleSticky; }
    public float getOpacity() { return mOpacity; }

    // 内部类用于拷贝设置
    private static class AppSettings {
        public boolean throttleSticky;
        public boolean autoCenterX;
        public int mode;
        
        public AppSettings(AppSettings other) {
            this.throttleSticky = other.throttleSticky;
            this.autoCenterX = other.autoCenterX;
            this.mode = other.mode;
        }
        
        public AppSettings() {
            this.autoCenterX = true;
            this.mode = 2;
        }
    }
}
