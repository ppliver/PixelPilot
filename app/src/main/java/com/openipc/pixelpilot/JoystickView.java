package com.openipc.pixelpilot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * 虚拟摇杆控件 - 用于控制无人机/车/船
 * 支持左右两个摇杆，输出归一化坐标 (-1.0 ~ 1.0)
 */
public class JoystickView extends View {

    private static final String TAG = "JoystickView";

    // 摇杆尺寸
    private float outerRadius;      // 外圆半径
    private float innerRadius;      // 摇杆头半径
    private float stickMaxDist;     // 摇杆最大移动距离

    // 摇杆位置
    private float stickX;           // 摇杆头中心X
    private float stickY;           // 摇杆头中心Y
    private float centerX;          // 底座中心X
    private float centerY;          // 底座中心Y

    // 绘制画笔
    private Paint outerPaint;
    private Paint innerPaint;
    private Paint guidePaint;

    // 颜色配置
    private int outerColor = Color.argb(150, 255, 255, 255);
    private int innerColor = Color.argb(200, 255, 87, 87);
    private int guideColor = Color.argb(80, 255, 255, 255);

    // 事件回调
    private OnJoystickListener listener;

    public interface OnJoystickListener {
        /**
         * 摇杆移动时回调
         * @param normalizedX 归一化X值 (-1.0 ~ 1.0)
         * @param normalizedY 归一化Y值 (-1.0 ~ 1.0)
         * @param strength 力度 (0.0 ~ 1.0)
         * @param angle 角度 (度)
         */
        void onMove(float normalizedX, float normalizedY, float strength, float angle);
        
        void onRelease();
    }

    public JoystickView(Context context) {
        super(context);
        init(context);
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public JoystickView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 初始化画笔
        outerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerPaint.setColor(outerColor);
        outerPaint.setStyle(Paint.Style.FILL);

        innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPaint.setColor(innerColor);
        innerPaint.setStyle(Paint.Style.FILL);

        guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        guidePaint.setColor(guideColor);
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(2);

        setClickable(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 使用正方形
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        outerRadius = w / 2f * 0.9f;
        innerRadius = outerRadius * 0.35f;
        stickMaxDist = outerRadius - innerRadius;
        centerX = w / 2f;
        centerY = h / 2f;
        stickX = centerX;
        stickY = centerY;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();

                // 计算相对于中心的偏移
                float dx = x - centerX;
                float dy = y - centerY;

                // 防止除零异常
                if (stickMaxDist <= 0) {
                    return true;
                }

                // 计算距离和角度
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float angle = (float) Math.toDegrees(Math.atan2(dy, dx)) + 90f;
                if (angle < 0) angle += 360f;

                // 限制在最大范围内
                if (distance > stickMaxDist) {
                    distance = stickMaxDist;
                    dx = dx * stickMaxDist / distance;
                    dy = dy * stickMaxDist / distance;
                }

                stickX = centerX + dx;
                stickY = centerY + dy;

                // 归一化输出 (-1.0 ~ 1.0)
                float normX = dx / stickMaxDist;
                float normY = -dy / stickMaxDist; // Y轴反转，符合MAVLink习惯

                // 计算力度
                float strength = distance / stickMaxDist;

                if (listener != null) {
                    listener.onMove(normX, normY, strength, angle);
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 回弹到中心
                stickX = centerX;
                stickY = centerY;
                if (listener != null) {
                    listener.onRelease();
                }
                break;
        }
        
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 绘制外圆底座
        canvas.drawCircle(centerX, centerY, outerRadius, outerPaint);
        
        // 绘制十字准线
        canvas.drawLine(centerX - outerRadius * 0.7f, centerY, 
                       centerX + outerRadius * 0.7f, centerY, guidePaint);
        canvas.drawLine(centerX, centerY - outerRadius * 0.7f, 
                       centerX, centerY + outerRadius * 0.7f, guidePaint);
        
        // 绘制摇杆头
        canvas.drawCircle(stickX, stickY, innerRadius, innerPaint);
    }

    /**
     * 设置摇杆监听器
     */
    public void setOnJoystickListener(OnJoystickListener listener) {
        this.listener = listener;
    }

    /**
     * 获取归一化X值 (-1.0 ~ 1.0)
     */
    public float getNormalizedX() {
        return (stickX - centerX) / stickMaxDist;
    }

    /**
     * 获取归一化Y值 (-1.0 ~ 1.0)
     */
    public float getNormalizedY() {
        return -(stickY - centerY) / stickMaxDist;
    }

    /**
     * 设置摇杆颜色
     */
    public void setOuterColor(int color) {
        this.outerColor = color;
        outerPaint.setColor(color);
        invalidate();
    }

    public void setInnerColor(int color) {
        this.innerColor = color;
        innerPaint.setColor(color);
        invalidate();
    }

    /**
     * 重置摇杆到中心位置
     */
    public void reset() {
        stickX = centerX;
        stickY = centerY;
        invalidate();
        if (listener != null) {
            listener.onRelease();
        }
    }
}
