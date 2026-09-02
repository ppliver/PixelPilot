package com.openipc.pixelpilot.virtualjoystick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * 虚拟摇杆控件 - 支持双摇杆、拖拽位置、透明度调节
 * 参考 RC-Pilot Pro 设计
 */
public class VirtualJoystick extends View {

    public interface OnMoveListener {
        void onMove(int pwmX, int pwmY);
    }

    // 摇杆状态
    private static final int STATE_DRAGGING_STICK = 1;
    private static final int STATE_DRAGGING_CONTAINER = 2;

    // 绘制参数
    private Paint mPaintBackground;
    private Paint mPaintCrosshair;
    private Paint mPaintBorder;
    private Paint mPaintButton;
    private Paint mPaintButtonCenter;
    private Paint mPaintText;
    private Paint mPaintLabel;

    // 尺寸
    private int mSize;           // 控件总大小
    private int mCenterX;
    private int mCenterY;
    private int mBorderRadius;
    private int mButtonRadius;

    // 摇杆按钮位置
    private float mStickX;
    private float mStickY;
    private float mMaxTravel;

    // 位置偏移（用于拖拽）
    private float mOffsetX;
    private float mOffsetY;

    // 触摸追踪
    private int mActivePointerId = -1;
    private int mTouchState = 0;
    private float mDownX;
    private float mDownY;
    private float mStartOffsetX;
    private float mStartOffsetY;
    private float mStartStickX;
    private float mStartStickY;

    // 配置
    private boolean mLocked = true;
    private boolean mStickyY = false;
    private int mChannel = 1;
    private float mOpacity = 1.0f;
    private int mButtonColor = Color.parseColor("#00CCFF");
    private int mBorderColor = Color.parseColor("#88FFFFFF");
    private int mBackgroundColor = Color.parseColor("#99000000");

    // PWM值
    private int mCurrentPwmX = 1500;
    private int mCurrentPwmY = 1500;

    // 监听器
    private OnMoveListener mListener;

    public VirtualJoystick(Context context) {
        super(context);
        init();
    }

    public VirtualJoystick(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VirtualJoystick(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        
        // 背景
        mPaintBackground = new Paint();
        mPaintBackground.setColor(mBackgroundColor);
        mPaintBackground.setAntiAlias(true);
        
        // 十字准星
        mPaintCrosshair = new Paint();
        mPaintCrosshair.setColor(Color.parseColor("#44FFFFFF"));
        mPaintCrosshair.setStrokeWidth(2);
        mPaintCrosshair.setAntiAlias(true);
        
        // 边框
        mPaintBorder = new Paint();
        mPaintBorder.setColor(mBorderColor);
        mPaintBorder.setStyle(Paint.Style.STROKE);
        mPaintBorder.setStrokeWidth(4);
        mPaintBorder.setAntiAlias(true);
        
        // 按钮
        mPaintButton = new Paint();
        mPaintButton.setColor(mButtonColor);
        mPaintButton.setAntiAlias(true);
        mPaintButton.setAlpha(220);
        
        // 按钮中心
        mPaintButtonCenter = new Paint();
        mPaintButtonCenter.setColor(Color.WHITE);
        mPaintButtonCenter.setAntiAlias(true);
        mPaintButtonCenter.setAlpha(180);
        
        // PWM显示文字
        mPaintText = new Paint();
        mPaintText.setColor(Color.parseColor("#00FFFF"));
        mPaintText.setTextSize(28);
        mPaintText.setAntiAlias(true);
        mPaintText.setTextAlign(Paint.Align.CENTER);
        
        // 通道标签
        mPaintLabel = new Paint();
        mPaintLabel.setColor(Color.parseColor("#AAAAAA"));
        mPaintLabel.setTextSize(18);
        mPaintLabel.setAntiAlias(true);
        mPaintLabel.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        mSize = Math.min(width, height);
        
        setMeasuredDimension(mSize, mSize);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        
        mCenterX = w / 2;
        mCenterY = h / 2;
        mBorderRadius = Math.min(w, h) / 2 - 12;
        mButtonRadius = mBorderRadius / 3;
        mMaxTravel = mBorderRadius * 0.7f;
        
        // 重置摇杆到中心
        resetStick();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 应用透明度
        int bgAlpha = (int)(255 * mOpacity);
        mPaintBackground.setAlpha(bgAlpha);
        int borderAlpha = (int)((mBorderColor >> 24) * mOpacity);
        mPaintBorder.setAlpha(borderAlpha);
        
        // 绘制半透明背景
        canvas.drawCircle(mCenterX, mCenterY, mBorderRadius, mPaintBackground);
        
        // 绘制十字准星
        int crossStart = mBorderRadius / 3;
        int crossEnd = mBorderRadius * 2 / 3;
        canvas.drawLine(mCenterX - crossEnd, mCenterY, mCenterX - crossStart, mCenterY, mPaintCrosshair);
        canvas.drawLine(mCenterX + crossStart, mCenterY, mCenterX + crossEnd, mCenterY, mPaintCrosshair);
        canvas.drawLine(mCenterX, mCenterY - crossEnd, mCenterX, mCenterY - crossStart, mPaintCrosshair);
        canvas.drawLine(mCenterX, mCenterY + crossStart, mCenterX, mCenterY + crossEnd, mPaintCrosshair);
        
        // 绘制外边框
        canvas.drawCircle(mCenterX, mCenterY, mBorderRadius, mPaintBorder);
        
        // 绘制摇杆按钮
        canvas.drawCircle(mStickX + mCenterX, mStickY + mCenterY, mButtonRadius, mPaintButton);
        canvas.drawCircle(mStickX + mCenterX, mStickY + mCenterY, mButtonRadius / 3, mPaintButtonCenter);
        
        // 绘制 PWM 值
        String pwmText = mCurrentPwmX + " | " + mCurrentPwmY;
        canvas.drawText(pwmText, mCenterX, mCenterY + mBorderRadius + 35, mPaintText);
        
        // 绘制通道标签
        String label = "CH" + mChannel;
        canvas.drawText(label, mCenterX, mCenterY - mBorderRadius - 15, mPaintLabel);
        
        // 解锁提示
        if (!mLocked) {
            Paint hintPaint = new Paint();
            hintPaint.setColor(Color.parseColor("#AAFFAA00"));
            hintPaint.setTextSize(24);
            hintPaint.setAntiAlias(true);
            hintPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("拖动调整", mCenterX, mCenterY + 8, hintPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        int pointerId = event.getPointerId(pointerIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (mActivePointerId == -1) {
                    mActivePointerId = pointerId;
                    mDownX = event.getX(pointerIndex);
                    mDownY = event.getY(pointerIndex);
                    
                    if (!mLocked) {
                        mTouchState = STATE_DRAGGING_CONTAINER;
                        mStartOffsetX = mOffsetX;
                        mStartOffsetY = mOffsetY;
                    } else {
                        mTouchState = STATE_DRAGGING_STICK;
                        mStartStickX = mStickX;
                        mStartStickY = mStickY;
                    }
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId != pointerId) break;
                
                float dx = event.getX(pointerIndex) - mDownX;
                float dy = event.getY(pointerIndex) - mDownY;
                
                if (mTouchState == STATE_DRAGGING_CONTAINER && !mLocked) {
                    // 拖拽整个容器
                    mOffsetX = Math.max(0, Math.min(getWidth() - mSize, mStartOffsetX + dx));
                    mOffsetY = Math.max(0, Math.min(getHeight() - mSize, mStartOffsetY + dy));
                    setTranslationX(mOffsetX);
                    setTranslationY(mOffsetY);
                } else if (mTouchState == STATE_DRAGGING_STICK) {
                    // 拖拽摇杆按钮
                    float newStickX = mStartStickX + dx;
                    float newStickY = mStartStickY + dy;
                    
                    float dist = (float) Math.sqrt(newStickX * newStickX + newStickY * newStickY);
                    if (dist > mMaxTravel) {
                        newStickX = newStickX * mMaxTravel / dist;
                        newStickY = newStickY * mMaxTravel / dist;
                    }
                    
                    mStickX = newStickX;
                    mStickY = newStickY;
                    
                    // 计算PWM值
                    mCurrentPwmX = Math.round(1500 + (mStickX / mMaxTravel) * 500);
                    mCurrentPwmY = Math.round(1500 - (mStickY / mMaxTravel) * 500);
                    mCurrentPwmX = Math.max(1000, Math.min(2000, mCurrentPwmX));
                    mCurrentPwmY = Math.max(1000, Math.min(2000, mCurrentPwmY));
                    
                    if (mListener != null) {
                        mListener.onMove(mCurrentPwmX, mCurrentPwmY);
                    }
                }
                invalidate();
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mActivePointerId == pointerId) {
                    if (mTouchState == STATE_DRAGGING_STICK) {
                        // 松开摇杆
                        if (!mStickyY) {
                            resetStick();
                        } else {
                            mStickX = 0;
                            mCurrentPwmX = 1500;
                        }
                        
                        if (mListener != null) {
                            mListener.onMove(mCurrentPwmX, mCurrentPwmY);
                        }
                    }
                    mTouchState = 0;
                    mActivePointerId = -1;
                }
                break;
        }
        
        return true;
    }

    private void resetStick() {
        mStickX = 0;
        mStickY = 0;
        mCurrentPwmX = 1500;
        mCurrentPwmY = 1500;
        invalidate();
    }

    // Getters and Setters
    public void setOnMoveListener(OnMoveListener listener) {
        this.mListener = listener;
    }

    public void setLocked(boolean locked) {
        this.mLocked = locked;
        invalidate();
    }

    public boolean isLocked() {
        return mLocked;
    }

    public void setStickyY(boolean sticky) {
        this.mStickyY = sticky;
    }

    public void setChannel(int channel) {
        this.mChannel = channel;
        invalidate();
    }

    public void setOpacity(float opacity) {
        this.mOpacity = Math.max(0.1f, Math.min(1.0f, opacity));
        invalidate();
    }

    public float getOpacity() {
        return mOpacity;
    }

    public void setButtonColor(int color) {
        this.mButtonColor = color;
        invalidate();
    }

    public void setBorderColor(int color) {
        this.mBorderColor = color;
        invalidate();
    }

    public void setBackgroundColor(int color) {
        this.mBackgroundColor = color;
        invalidate();
    }

    public void setSize(int size) {
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp != null) {
            mSize = size;
            lp.width = size;
            lp.height = size;
            requestLayout();
        }
    }

    public int getSize() {
        return mSize;
    }

    public int getPwmX() {
        return mCurrentPwmX;
    }

    public int getPwmY() {
        return mCurrentPwmY;
    }

    public void setPosition(float x, float y) {
        mOffsetX = x;
        mOffsetY = y;
        setTranslationX(x);
        setTranslationY(y);
    }

    public float getPositionX() {
        return mOffsetX;
    }

    public float getPositionY() {
        return mOffsetY;
    }
}
