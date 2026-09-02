package com.openipc.pixelpilot.virtualjoystick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * 虚拟摇杆控件 - 参考 RC-Pilot Pro 样式
 */
public class VirtualJoystick extends View {

    public interface OnMoveListener {
        void onMove(int pwmX, int pwmY);
    }

    private static final int STATE_DRAGGING_STICK = 1;
    private static final int STATE_DRAGGING_CONTAINER = 2;
    private static final int STATE_RESIZING = 3;

    private Paint mPaintBackground;
    private Paint mPaintBorder;
    private Paint mPaintCrosshair;
    private Paint mPaintButton;
    private Paint mPaintText;
    private Paint mPaintLabel;
    private Paint mPaintHint;
    private Paint mPaintArrow;

    private int mSize;
    private int mCenterX;
    private int mCenterY;
    private int mBorderRadius;
    private int mButtonRadius;
    private float mMaxTravel;

    private float mStickX;
    private float mStickY;
    private float mOffsetX;
    private float mOffsetY;

    private int mActivePointerId = -1;
    private int mTouchState = 0;
    private float mDownX;
    private float mDownY;
    private float mStartOffsetX;
    private float mStartOffsetY;
    private float mStartStickX;
    private float mStartStickY;
    private int mStartSize;

    private boolean mLocked = true;
    private boolean mStickyY = false;
    private int mChannel = 1;
    private float mOpacity = 0.9f;

    private int mCurrentPwmX = 1500;
    private int mCurrentPwmY = 1500;

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
        
        mPaintBackground = new Paint();
        mPaintBackground.setColor(Color.parseColor("#CC1a1a1a"));
        mPaintBackground.setAntiAlias(true);
        
        mPaintBorder = new Paint();
        mPaintBorder.setColor(Color.parseColor("#D4A574"));
        mPaintBorder.setStyle(Paint.Style.STROKE);
        mPaintBorder.setStrokeWidth(6);
        mPaintBorder.setAntiAlias(true);
        
        mPaintCrosshair = new Paint();
        mPaintCrosshair.setColor(Color.parseColor("#44FFFFFF"));
        mPaintCrosshair.setStrokeWidth(2);
        mPaintCrosshair.setAntiAlias(true);
        
        mPaintButton = new Paint();
        mPaintButton.setColor(Color.parseColor("#00CCFF"));
        mPaintButton.setAntiAlias(true);
        mPaintButton.setAlpha(220);
        
        mPaintText = new Paint();
        mPaintText.setColor(Color.parseColor("#00FFFF"));
        mPaintText.setTextSize(26);
        mPaintText.setAntiAlias(true);
        mPaintText.setTextAlign(Paint.Align.CENTER);
        
        mPaintLabel = new Paint();
        mPaintLabel.setColor(Color.parseColor("#AAAAAA"));
        mPaintLabel.setTextSize(16);
        mPaintLabel.setAntiAlias(true);
        mPaintLabel.setTextAlign(Paint.Align.CENTER);
        
        mPaintHint = new Paint();
        mPaintHint.setColor(Color.parseColor("#FFAA00"));
        mPaintHint.setTextSize(22);
        mPaintHint.setAntiAlias(true);
        mPaintHint.setTextAlign(Paint.Align.CENTER);
        
        mPaintArrow = new Paint();
        mPaintArrow.setColor(Color.parseColor("#FFD700"));
        mPaintArrow.setAntiAlias(true);
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
        mBorderRadius = Math.min(w, h) / 2 - 15;
        mButtonRadius = mBorderRadius / 3;
        mMaxTravel = mBorderRadius * 0.7f;
        
        resetStick();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int bgAlpha = (int)(204 * mOpacity);
        mPaintBackground.setAlpha(bgAlpha);
        
        // 绘制半透明背景
        canvas.drawCircle(mCenterX, mCenterY, mBorderRadius, mPaintBackground);
        
        // 绘制十字准星
        int crossStart = mBorderRadius / 4;
        int crossEnd = mBorderRadius * 3 / 4;
        canvas.drawLine(mCenterX - crossEnd, mCenterY, mCenterX - crossStart, mCenterY, mPaintCrosshair);
        canvas.drawLine(mCenterX + crossStart, mCenterY, mCenterX + crossEnd, mCenterY, mPaintCrosshair);
        canvas.drawLine(mCenterX, mCenterY - crossEnd, mCenterX, mCenterY - crossStart, mPaintCrosshair);
        canvas.drawLine(mCenterX, mCenterY + crossStart, mCenterX, mCenterY + crossEnd, mPaintCrosshair);
        
        // 绘制外边框
        canvas.drawCircle(mCenterX, mCenterY, mBorderRadius, mPaintBorder);
        
        // 绘制摇杆按钮
        canvas.drawCircle(mCenterX + mStickX, mCenterY + mStickY, mButtonRadius, mPaintButton);
        
        // 绘制四向箭头指示器
        drawArrow(canvas, mCenterX, mCenterY - mBorderRadius + 30, 0);
        drawArrow(canvas, mCenterX, mCenterY + mBorderRadius - 30, 180);
        drawArrow(canvas, mCenterX - mBorderRadius + 30, mCenterY, 270);
        drawArrow(canvas, mCenterX + mBorderRadius - 30, mCenterY, 90);
        
        // 绘制通道标签
        canvas.drawText("CH" + mChannel, mCenterX, mCenterY - mBorderRadius - 10, mPaintLabel);
        
        // 绘制 PWM 值
        canvas.drawText(mCurrentPwmX + " | " + mCurrentPwmY, mCenterX, mCenterY + mBorderRadius + 30, mPaintText);
        
        // 解锁提示和调整按钮
        if (!mLocked) {
            canvas.drawText("拖动调整", mCenterX, mCenterY + 8, mPaintHint);
            drawResizeButtons(canvas);
        }
    }

    private void drawArrow(Canvas canvas, float cx, float cy, float angle) {
        Path path = new Path();
        float size = 10;
        path.moveTo(cx, cy - size);
        path.lineTo(cx - size * 0.6f, cy + size * 0.5f);
        path.lineTo(cx + size * 0.6f, cy + size * 0.5f);
        path.close();
        
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(angle, cx, cy);
        path.transform(matrix);
        
        canvas.drawPath(path, mPaintArrow);
    }

    private void drawResizeButtons(Canvas canvas) {
        int btnY = mCenterY - mBorderRadius - 45;
        int btnRadius = 18;
        
        Paint btnBg = new Paint();
        btnBg.setColor(Color.parseColor("#CC1E3D59"));
        btnBg.setAntiAlias(true);
        
        Paint btnBorder = new Paint();
        btnBorder.setColor(Color.parseColor("#FF6B9FFF"));
        btnBorder.setStyle(Paint.Style.STROKE);
        btnBorder.setStrokeWidth(2);
        btnBorder.setAntiAlias(true);
        
        Paint btnText = new Paint();
        btnText.setColor(Color.WHITE);
        btnText.setTextSize(20);
        btnText.setAntiAlias(true);
        btnText.setTextAlign(Paint.Align.CENTER);
        btnText.setFakeBoldText(true);
        
        // 缩小按钮
        canvas.drawCircle(mCenterX - 30, btnY, btnRadius, btnBg);
        canvas.drawCircle(mCenterX - 30, btnY, btnRadius, btnBorder);
        canvas.drawText("-", mCenterX - 30, btnY + 7, btnText);
        
        // 放大按钮
        canvas.drawCircle(mCenterX + 30, btnY, btnRadius, btnBg);
        canvas.drawCircle(mCenterX + 30, btnY, btnRadius, btnBorder);
        canvas.drawText("+", mCenterX + 30, btnY + 7, btnText);
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
                    
                    if (!mLocked && isResizeButtonClicked(mDownX, mDownY)) {
                        mTouchState = STATE_RESIZING;
                    } else if (!mLocked) {
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
                    mOffsetX = Math.max(0, Math.min(getWidth() - mSize, mStartOffsetX + dx));
                    mOffsetY = Math.max(0, Math.min(getHeight() - mSize, mStartOffsetY + dy));
                    setTranslationX(mOffsetX);
                    setTranslationY(mOffsetY);
                } else if (mTouchState == STATE_RESIZING && !mLocked) {
                    int newSize = mStartSize + (int)dx;
                    newSize = Math.max(120, Math.min(350, newSize));
                    setSize(newSize);
                    mStartSize = newSize;
                } else if (mTouchState == STATE_DRAGGING_STICK) {
                    float newStickX = mStartStickX + dx;
                    float newStickY = mStartStickY + dy;
                    
                    float dist = (float) Math.sqrt(newStickX * newStickX + newStickY * newStickY);
                    if (dist > mMaxTravel) {
                        newStickX = newStickX * mMaxTravel / dist;
                        newStickY = newStickY * mMaxTravel / dist;
                    }
                    
                    mStickX = newStickX;
                    mStickY = newStickY;
                    
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

    private boolean isResizeButtonClicked(float x, float y) {
        float btnY = mCenterY - mBorderRadius - 45;
        float btnRadius = 18;
        
        float distLeft = (float) Math.sqrt(Math.pow(x - (mCenterX - 30), 2) + Math.pow(y - btnY, 2));
        float distRight = (float) Math.sqrt(Math.pow(x - (mCenterX + 30), 2) + Math.pow(y - btnY, 2));
        
        return distLeft <= btnRadius || distRight <= btnRadius;
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
