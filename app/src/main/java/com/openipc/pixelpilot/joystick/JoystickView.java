package com.openipc.pixelpilot.joystick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View {

    public interface OnMoveListener {
        void onMove(int angle, int strength);
    }

    private static final int DEFAULT_COLOR_BUTTON = Color.parseColor("#00CCFF");
    private static final int DEFAULT_COLOR_BORDER = Color.parseColor("#88FFFFFF");
    private static final int DEFAULT_ALPHA_BORDER = 128;
    private static final int DEFAULT_BACKGROUND_COLOR = Color.parseColor("#99000000");
    private static final int DEFAULT_WIDTH_BORDER = 4;

    private Paint mPaintCircleButton;
    private Paint mPaintCircleBorder;
    private Paint mPaintBackground;
    private Paint mPaintCrosshair;
    private Paint mPaintButtonCenter;

    private int mButtonRadius;
    private int mBorderRadius;
    private int mBackgroundRadius;
    private int mPosX;
    private int mPosY;
    private int mCenterX;
    private int mCenterY;
    private boolean mAutoReCenterButton = true;
    private OnMoveListener mOnMoveListener;
    private int buttonColor = DEFAULT_COLOR_BUTTON;
    private int borderColor = DEFAULT_COLOR_BORDER;
    private int backgroundColor = DEFAULT_BACKGROUND_COLOR;
    private int borderWidth = DEFAULT_WIDTH_BORDER;
    private int borderAlpha = DEFAULT_ALPHA_BORDER;

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
        mPaintCircleButton = new Paint();
        mPaintCircleButton.setColor(buttonColor);
        mPaintCircleButton.setAntiAlias(true);
        mPaintCircleButton.setAlpha(200);

        mPaintCircleBorder = new Paint();
        mPaintCircleBorder.setColor(borderColor);
        mPaintCircleBorder.setAlpha(borderAlpha);
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
        mPaintButtonCenter.setAlpha(150);

        setClickable(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        mCenterX = w / 2;
        mCenterY = h / 2;
        mPosX = mCenterX;
        mPosY = mCenterY;
        mBorderRadius = Math.min(w, h) / 2 - 15;
        mButtonRadius = mBorderRadius / 3;
        mBackgroundRadius = mBorderRadius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(mCenterX, mCenterY, mBackgroundRadius, mPaintBackground);

        int crossStart = mBorderRadius / 3;
        int crossEnd = mBorderRadius * 2 / 3;
        canvas.drawLine(mCenterX - crossEnd, mCenterY, mCenterX - crossStart, mCenterY, mPaintCrosshair);
        canvas.drawLine(mCenterX + crossStart, mCenterY, mCenterX + crossEnd, mCenterY, mPaintCrosshair);
        canvas.drawLine(mCenterX, mCenterY - crossEnd, mCenterX, mCenterY - crossStart, mPaintCrosshair);
        canvas.drawLine(mCenterX, mCenterY + crossStart, mCenterX, mCenterY + crossEnd, mPaintCrosshair);

        canvas.drawCircle(mCenterX, mCenterY, mBorderRadius, mPaintCircleBorder);
        canvas.drawCircle(mPosX, mPosY, mButtonRadius, mPaintCircleButton);
        canvas.drawCircle(mPosX, mPosY, mButtonRadius / 3, mPaintButtonCenter);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float dx = x - mCenterX;
                float dy = y - mCenterY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance > mBorderRadius) {
                    dx = (float) (dx * mBorderRadius / distance);
                    dy = (float) (dy * mBorderRadius / distance);
                }
                mPosX = mCenterX + (int) dx;
                mPosY = mCenterY + (int) dy;
                invalidate();
                if (mOnMoveListener != null) {
                    int angle = (int) (Math.toDegrees(Math.atan2(dy, dx)) + 360) % 360;
                    int strength = (int) (distance * 100 / mBorderRadius);
                    mOnMoveListener.onMove(angle, strength);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mAutoReCenterButton) {
                    mPosX = mCenterX;
                    mPosY = mCenterY;
                    invalidate();
                }
                break;
        }
        return true;
    }

    public void setOnMoveListener(OnMoveListener listener) {
        mOnMoveListener = listener;
    }

    public void setButtonColor(int color) {
        this.buttonColor = color;
        mPaintCircleButton.setColor(color);
        invalidate();
    }

    public void setBorderColor(int color) {
        this.borderColor = color;
        mPaintCircleBorder.setColor(color);
        invalidate();
    }

    public void setBorderAlpha(int alpha) {
        this.borderAlpha = alpha;
        mPaintCircleBorder.setAlpha(alpha);
        invalidate();
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        mPaintBackground.setColor(color);
        invalidate();
    }

    public void setBorderWidth(float width) {
        this.borderWidth = (int) width;
        mPaintCircleBorder.setStrokeWidth(width);
        invalidate();
    }
}
