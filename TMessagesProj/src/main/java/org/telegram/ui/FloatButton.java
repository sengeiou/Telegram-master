package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.telegram.messenger.R;
import org.telegram.utils.MemberStatusUtil;

public class FloatButton extends RelativeLayout {
    // 悬浮栏位置
    private final static int LEFT = 0;
    private final static int RIGHT = 1;
    private final static int TOP = 3;
    private final static int BUTTOM = 4;

    private int dpi;
    private int screenHeight;
    private int screenWidth;
    private WindowManager.LayoutParams wmParams;
    private WindowManager wm;
    private float x, y;
    private float mTouchStartX;
    private float mTouchStartY;
    private boolean isScroll;
    private TimeCount time;
    private TextView floatText;
    private TextView titleText;
    private Activity activity;
    private ImageView bgImg;
//    public long millis;

    /* 定义一个倒计时的内部类 */
    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);//参数依次为总时长,和计时的时间间隔
        }

        @Override
        public void onFinish() {//计时完毕时触发
            titleText.setText("到期");
            floatText.setText("续费");
            titleText.setTextColor(0xffffffff);
            floatText.setTextColor(0xffffffff);
            titleText.setTextSize(12);
            floatText.setTextSize(12);
            MemberStatusUtil.expire = true;
            new MemberStatusUtil().runGetJson();
        }

        @Override
        public void onTick(long millisUntilFinished) {//计时过程显示
//            millis = millisUntilFinished;
            long h = millisUntilFinished / (1000*60*60);
            long m = (millisUntilFinished % (1000*60*60)) / (1000*60);
            long s = millisUntilFinished % (1000*60*60) % (1000*60) / 1000;
            String hs = h + "";
            String ms = m + "";
            String ss = s + "";
            if (h < 10){
                hs = "0" + h;
            }
            if (m < 10){
                ms = "0" + m;
            }
//            if (s < 10){
//                ss = "0" + s;
//            }
            titleText.setText("时长");
            floatText.setText(hs + " : " + ms );
            floatText.setTextSize(10);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) { //构建圆形
        int width = getMeasuredWidth();
        Paint mPaint = new Paint();
        mPaint.setARGB(1,67 ,221, 135);
        mPaint.setAntiAlias(true);
        float cirX = width / 2;
        float cirY = width / 2;
        float radius = width / 2;
        canvas.drawCircle(cirX, cirY, radius, mPaint);
        super.onDraw(canvas);
    }

    public void setMillis(long mills){
        if (mills > 0){
            time = null;
            time = new TimeCount(mills, 1000);//构造CountDownTimer对象
        }else {
            if (time != null){
                time.onFinish();
            }
            time = null;
        }
    }

    public FloatButton(Activity activity) {
        super(activity);
        this.activity = activity;
        LayoutInflater.from(activity).inflate(R.layout.view_chat, this);
        setBackgroundColor(0x00ffffff);
        bgImg = findViewById(R.id.bgImg);
        bgImg.setImageResource(R.drawable.float_btn1);
        floatText = findViewById(R.id.floatText);
        titleText = findViewById(R.id.titleText);
        titleText.setTextSize(12);
        try {
            wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
            //通过像素密度来设置按钮的大小
            dpi = dpi(dm.densityDpi);
            //屏宽
            screenWidth = wm.getDefaultDisplay().getWidth();
            //屏高
            screenHeight = wm.getDefaultDisplay().getHeight();
            //布局设置
            wmParams = new WindowManager.LayoutParams();
            // 设置window type
            wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION;
            wmParams.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明
            wmParams.gravity = Gravity.LEFT | Gravity.TOP;
            // 设置Window flag
            wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            wmParams.width = dpi + 72;
            wmParams.height = dpi + 72;
            wmParams.y = (screenHeight - dpi) >> 1;
            wm.addView(this, wmParams);
        }catch (Exception e){
            Log.e("FloatButton Exception",e+"");
        }
        hide();
    }


    /**
     * 根据密度选择控件大小
     *
     */
    private int dpi(int densityDpi) {
        if (densityDpi <= 120) {
            return 36;
        } else if (densityDpi <= 160) {
            return 48;
        } else if (densityDpi <= 240) {
            return 72;
        } else if (densityDpi <= 320) {
            return 96;
        }
        return 108;
    }

    public void show() {
        if (getVisibility() == GONE) {
            setVisibility(View.VISIBLE);
        }
        if (time != null){
            time.start();
        }else {
            titleText.setText("到期");
            floatText.setText("续费");
            titleText.setTextColor(0xffffffff);
            floatText.setTextColor(0xffffffff);
            titleText.setTextSize(12);
            floatText.setTextSize(12);
        }
    }


    public void hide() {
        if (getVisibility() == View.VISIBLE){
            setVisibility(View.GONE);
        }
    }

    public void destory() {
        hide();
        try{
            wm.removeViewImmediate(this);
        }catch (Exception e){
            Log.e("Float ERR", e.getMessage());
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 获取相对屏幕的坐标， 以屏幕左上角为原点
        x = event.getRawX();
        y = event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // setBackgroundDrawable(openDrawable);
                // invalidate();
                // 获取相对View的坐标，即以此View左上角为原点
                mTouchStartX = event.getX();
                mTouchStartY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (isScroll) {
                    updateViewPosition();
                } else {
                    // 当前不处于连续滑动状态 则滑动小于图标1/3则不滑动
                    if (Math.abs(mTouchStartX - event.getX()) > dpi / 3
                            || Math.abs(mTouchStartY - event.getY()) > dpi / 3) {
                        updateViewPosition();
                    } else {
                        break;
                    }
                }
                isScroll = true;
                break;
            case MotionEvent.ACTION_UP:
                // 拖动
                if (isScroll) {
                    autoView();
                    // setBackgroundDrawable(closeDrawable);
                    // invalidate();
                } else {
                    // 当前显示功能区，则隐藏
                    // setBackgroundDrawable(openDrawable);
                    // invalidate();

                }
                isScroll = false;
                mTouchStartX = mTouchStartY = 0;
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 自动移动位置
     */
    private void autoView() {
        // 得到view在屏幕中的位置
        int[] location = new int[2];
        getLocationOnScreen(location);
        //左侧
        if (location[0] < screenWidth / 2 - getWidth() / 2) {
            updateViewPosition(LEFT);
        } else {
            updateViewPosition(RIGHT);
        }
    }

    /**
     * 手指释放更新悬浮窗位置
     *
     */
    private void updateViewPosition(int l) {
        switch (l) {
            case LEFT:
                wmParams.x = 0;
                break;
            case RIGHT:
                int x = screenWidth - dpi;
                wmParams.x = x;
                break;
            case TOP:
                wmParams.y = 0;
                break;
            case BUTTOM:
                wmParams.y = screenHeight - dpi;
                break;
        }
        wm.updateViewLayout(this, wmParams);
    }

    // 更新浮动窗口位置参数
    private void updateViewPosition() {
        wmParams.x = (int) (x - mTouchStartX);
        //是否存在状态栏（提升滑动效果）
        // 不设置为全屏（状态栏存在） 标题栏是屏幕的1/25
        wmParams.y = (int) (y - mTouchStartY - screenHeight / 25);
        wm.updateViewLayout(this, wmParams);
    }
}
