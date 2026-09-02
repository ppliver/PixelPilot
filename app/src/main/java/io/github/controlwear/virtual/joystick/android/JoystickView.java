package io.github.controlwear.virtual.joystick.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View {

    public interface OnMoveListener {
        void onMove(int angle, int strength);
    }

    private static final int DEFAULT_COLOR_BUTTON = Color.GRAY;
    private static final int DEFAULT_COLOR_BORDER = Color.BLACK;
    private static final int DEFAULT_ALPHA_BORDER = 255;
    private static final int DEFAULT_BACKGROUND_COLOR = Color.TRANSPARENT;
    private static final int DEFAULT_WIDTH_BORDER = 3;
    private static final boolean DEFAULT_FIXED_CENTER = false;
    private static final boolean DEFAULT_AUTO_RECENTER_BUTTON = true;
    private static final boolean DEFAULT_BUTTON_STICK_TO_BORDER = false;

    private Paint mPaintCircleButton;
    private Paint mPaintCircleBorder;
    private Paint mPaintBackground;

    private int mButtonRadius;
    private int mBorderRadius;
    private int mBackgroundRadius;

    private int mPosX;
    private int mPosY;

    private int mCenterX;
    private int mCenterY;

    private boolean mFixedCenter;
    private boolean mAutoReCenterButton;
    private boolean mButtonStickToBorder;

    private Drawable buttonDrawable;

    private OnMoveListener mOnMoveListener;

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

        TypedArray styledAttributes = context.obtainStyledAttributes(
                attrs,
                R.styleable.JoystickView,
                0, 0
        );

        int buttonColor = styledAttributes.getColor(
                R.styleable.JoystickView_JV_buttonColor,
                DEFAULT_COLOR_BUTTON
        );

        int borderColor = styledAttributes.getColor(
                R.styleable.JoystickView_JV_borderColor,
                DEFAULT_COLOR_BORDER
        );

        int borderAlpha = styledAttributes.getInt(
                R.styleable.JoystickView_JV_borderAlpha,
                DEFAULT_ALPHA_BORDER
        );

        int backgroundColor = styledAttributes.getColor(
                R.styleable.JoystickView_JV_backgroundColor,
                DEFAULT_BACKGROUND_COLOR
        );

        int borderWidth = styledAttributes.getDimensionPixelSize(
                R.styleable.JoystickView_JV_borderWidth,
                DEFAULT_WIDTH_BORDER
        );

        mFixedCenter = styledAttributes.getBoolean(
                R.styleable.JoystickView_JV_fixedCenter,
                DEFAULT_FIXED_CENTER
        );

        mAutoReCenterButton = styledAttributes.getBoolean(
                R.styleable.JoystickView_JV_autoReCenterButton,
                DEFAULT_AUTO_RECENTER_BUTTON
        );

        mButtonStickToBorder = styledAttributes.getBoolean(
                R.styleable.JoystickView_JV_buttonStickToBorder,
                DEFAULT_BUTTON_STICK_TO_BORDER
        );

        buttonDrawable = styledAttributes.getDrawable(
                R.styleable.JoystickView_JV_buttonImage
        );

        styledAttributes.recycle();

        mPaintCircleButton = new Paint();
        mPaintCircleButton.setColor(buttonColor);
        mPaintCircleButton.setAntiAlias(true);

        mPaintCircleBorder = new Paint();
        mPaintCircleBorder.setColor(borderColor);
        mPaintCircleBorder.setAlpha(borderAlpha);
        mPaintCircleBorder.setStyle(Paint.Style.STROKE);
        mPaintCircleBorder.setStrokeWidth(borderWidth);
        mPaintCircleBorder.setAntiAlias(true);

        mPaintBackground = new Paint();
        mPaintBackground.setColor(backgroundColor);
        mPaintBackground.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        mCenterX = w / 2;
        mCenterY = h / 2;

        mPosX = mCenterX;
        mPosY = mCenterY;

        mBorderRadius = Math.min(w, h) / 2 - 10;
        mButtonRadius = mBorderRadius / 3;
        mBackgroundRadius = mBorderRadius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(mCenterX, mCenterY, mBackgroundRadius, mPaintBackground);
        canvas.drawCircle(mCenterX, mCenterY, mBorderRadius, mPaintCircleBorder);

        if (buttonDrawable != null) {
            Bitmap bitmap = ((BitmapDrawable) buttonDrawable).getBitmap();
            canvas.drawBitmap(bitmap, mPosX - bitmap.getWidth() / 2, mPosY - bitmap.getHeight() / 2, null);
        } else {
            canvas.drawCircle(mPosX, mPosY, mButtonRadius, mPaintCircleButton);
        }
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
}