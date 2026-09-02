package com.openipc.pixelpilot.joystick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * 虚拟摇杆控件 - 支持拖拽位置、显示PWM值
 * 参考 RC-Pilot Pro 样式
 */
public class JoystickView extends View {

    public interface OnMoveListener {
        void onMove(int pwmX, int pwmY);
    }

    private Paint mPaintCircleButton;
    private Paint mPaintCircleBorder;
    private Paint mPaintBackground;
    private Paint mPaintCrosshair;
    private Paint mPaintButtonCenter;
    private Paint mPaintText;
    private Paint mPaintLabel;

    private int mButtonRadius;
    private int mBorderRadius;
    private int mBackgroundRadius;
    private int mPosX;
    private int mPosY;
    private int mCenterX;
    private int mCenterY;
    
    // 外部位置 (用于拖拽)
    private int mContainerX;
    private int mContainerY;
    private int mContainerSize;
    
    private boolean mLocked = true;
    private boolean mStickyY = false;
    private OnMoveListener mOnMoveListener;
    
    // 当前PWM值
    private int mCurrentPwmX = 1500;
    private int mCurrentPwmY = 1500;
    
    // 通道标签
    private String mLabelText = "CH";
    private int mLabelChannel = 1;

    private int buttonColor = Color.parseColor("#00CCFF");
    private int borderColor = Color.parseColor("#88FFFFFF");
    private int backgroundColor = Color.parseColor("#99000000");
    private int borderWidth = 4;

    public JoystickView(Context context) {
        super(context);
        init(context, null);
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public JoystickView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setClickable(true);
        
        // 摇杆按钮渐变效果使用纯色替代
        mPaintCircleButton = new Paint();
        mPaintCircleButton.setColor(buttonColor);
        mPaintCircleButton.setAntiAlias(true);
        mPaintCircleButton.setAlpha(220);

        mPaintCircleBorder = new Paint();
        mPaintCircleBorder.setColor(borderColor);
        mPaintCircleBorder.setStyle(Paint.Style.STROKE);
        mPaintCircleBorder.setStrokeWidth(borderWidth);
        mPaintCircleBorder.setAntiAlias(true);

        mPaintBackground = new Paint();
        mPaintBackground.setColor(backgroundColor);
        mPaintBackground.setAntiAlias(true);

        mPaintCrosshair = new Paint();
        mPaintCrosshair.setColor(Color.parseColor("#44FFFFFF"));
        mPaintCrosshair.setStrokeWidth(2);
        mPaintCrosshair.setAntiAlias(true);

        mPaintButtonCenter = new Paint();
        mPaintButtonCenter.setColor(Color.parseColor("#FFFFFF"));
        mPaintButtonCenter.setAntiAlias(true);
        mPaintButtonCenter.setAlpha(180);

        mPaintText = new Paint();
        mPaintText.setColor(Color.parseColor("#00FFFF"));
        mPaintText.setTextSize(28);
        mPaintText.setAntiAlias(true);
        mPaintText.setTextAlign(Paint.Align.CENTER);

        mPaintLabel = new Paint();
        mPaintLabel.setColor(Color.parseColor("#AAAAAA"));
        mPaintLabel.setTextSize(18);
        mPaintLabel.setAntiAlias(true);
        mPaintLabel.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
        
        if (mContainerSize == 0) {
            mContainerSize = size;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        
        mCenterX = w / 2;
        mCenterY = h / 2;
        mPosX = mCenterX;
        mPosY = mCenterY;
        
        mBorderRadius = Math.min(w, h) / 2 - 12;
        mButtonRadius = mBorderRadius / 3;
        mBackgroundRadius = mBorderRadius;
        
        // 更新外部位置
        if (mContainerX == 0 && mContainerY == 0) {
            mContainerX = getX();
            mContainerY = getY();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 如果不在中心，先平移画布
        float drawX = mContainerX + mCenterX;
        float drawY = mContainerY + mCenterY;
        
        canvas.save();
        canvas.translate(drawX - mCenterX, drawY - mCenterY);
        
        // 绘制半透明背景
        canvas.drawCircle(mCenterX, mCenterY, mBackgroundRadius, mPaintBackground);
        
        // 绘制十字准星
        int crossStart = mBorderRadius / 3;
        int crossEnd = mBorderRadius * 2 / 3;
        canvas.drawLine(mCenterX - crossEnd, mCenterY, mCenterX - crossStart, mCenterY, mPaintCrosshair);
        canvas.drawLine(mCenterX + crossStart, mCenterY, mCenterX + crossEnd, mCenterY, mPaintCrosshair);
        canvas.drawLine(mCenterX, mCenterY - crossEnd, mCenterX, mCenterY - crossStart, mPaintCrosshair);
        canvas.drawLine(mCenterX, mCenterY + crossStart, mCenterX, mCenterY + crossEnd, mPaintCrosshair);
        
        // 绘制外边框
        canvas.drawCircle(mCenterX, mCenterY, mBorderRadius, mPaintCircleBorder);
        
        // 绘制摇杆按钮 (渐变效果)
        canvas.drawCircle(mPosX, mPosY, mButtonRadius, mPaintCircleButton);
        canvas.drawCircle(mPosX, mPosY, mButtonRadius / 3, mPaintButtonCenter);
        
        // 绘制 PWM 值显示
        String pwmText = mCurrentPwmX + " | " + mCurrentPwmY;
        canvas.drawText(pwmText, mCenterX, mCenterY + mBorderRadius + 35, mPaintText);
        
        // 绘制通道标签
        String label = "CH" + mLabelChannel;
        canvas.drawText(label, mCenterX, mCenterY - mBorderRadius - 15, mPaintLabel);
        
        // 解锁时的提示
        if (!mLocked) {
            Paint hintPaint = new Paint();
            hintPaint.setColor(Color.parseColor("#AAFFAA00"));
            hintPaint.setTextSize(24);
            hintPaint.setAntiAlias(true);
            hintPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("拖动调整", mCenterX, mCenterY + 8, hintPaint);
        }
        
        canvas.restore();
    }

    private float mDownX, mDownY;
    private boolean isDraggingContainer;
    private boolean isDraggingStick;
    private float mStartContainerX, mStartContainerY;
    private float mStartStickOffsetX, mStartStickOffsetY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                mDownY = event.getY();
                
                if (!mLocked) {
                    isDraggingContainer = true;
                    mStartContainerX = mContainerX;
                    mStartContainerY = mContainerY;
                } else {
                    isDraggingStick = true;
                    mStartStickOffsetX = mPosX - mCenterX;
                    mStartStickOffsetY = mPosY - mCenterY;
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - mDownX;
                float dy = event.getY() - mDownY;
                
                if (isDraggingContainer && !mLocked) {
                    mContainerX = Math.max(0, Math.min(getWidth() - mContainerSize, mStartContainerX + dx));
                    mContainerY = Math.max(0, Math.min(getHeight() - mContainerSize, mStartContainerY + dy));
                    setX(mContainerX);
                    setY(mContainerY);
                } else if (isDraggingStick) {
                    float stickDx = mStartStickOffsetX + dx;
                    float stickDy = mStartStickOffsetY + dy;
                    float dist = (float) Math.sqrt(stickDx * stickDx + stickDy * stickDy);
                    
                    if (dist > mBorderRadius * 0.7) {
                        stickDx = stickDx * mBorderRadius * 0.7f / dist;
                        stickDy = stickDy * mBorderRadius * 0.7f / dist;
                    }
                    
                    mPosX = mCenterX + (int) stickDx;
                    mPosY = mCenterY + (int) stickDy;
                    
                    // 计算PWM值 (1000-2000, 中心1500)
                    mCurrentPwmX = Math.round(1500 + (stickDx / (mBorderRadius * 0.7f)) * 500);
                    mCurrentPwmY = Math.round(1500 - (stickDy / (mBorderRadius * 0.7f)) * 500);
                    mCurrentPwmX = Math.max(1000, Math.min(2000, mCurrentPwmX));
                    mCurrentPwmY = Math.max(1000, Math.min(2000, mCurrentPwmY));
                    
                    if (mOnMoveListener != null) {
                        mOnMoveListener.onMove(mCurrentPwmX, mCurrentPwmY);
                    }
                }
                invalidate();
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDraggingStick) {
                    isDraggingStick = false;
                    
                    // 松开后回中 (Y轴可选粘性)
                    if (!mStickyY) {
                        mPosX = mCenterX;
                        mPosY = mCenterY;
                        mCurrentPwmX = 1500;
                        mCurrentPwmY = 1500;
                    } else {
                        // 粘性油门：保持Y值，X回中
                        mCurrentPwmX = 1500;
                    }
                    
                    if (mOnMoveListener != null) {
                        mOnMoveListener.onMove(mCurrentPwmX, mCurrentPwmY);
                    }
                }
                isDraggingContainer = false;
                break;
        }
        
        return true;
    }

    // Getters and Setters
    public void setOnMoveListener(OnMoveListener listener) {
        mOnMoveListener = listener;
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

    public void setChannelLabel(int channel) {
        this.mLabelChannel = channel;
        invalidate();
    }

    public int getPwmX() {
        return mCurrentPwmX;
    }

    public int getPwmY() {
        return mCurrentPwmY;
    }

    public void setPosition(int x, int y) {
        mContainerX = x;
        mContainerY = y;
        setX(x);
        setY(y);
        invalidate();
    }

    public int getPositionX() {
        return mContainerX;
    }

    public int getPositionY() {
        return mContainerY;
    }

    public void setSize(int size) {
        mContainerSize = size;
        layoutParams.width = size;
        layoutParams.height = size;
        requestLayout();
    }

    public int getSize() {
        return mContainerSize;
    }
}
