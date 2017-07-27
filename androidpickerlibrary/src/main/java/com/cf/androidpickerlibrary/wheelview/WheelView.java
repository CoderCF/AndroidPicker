package com.cf.androidpickerlibrary.wheelview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import com.cf.androidpickerlibrary.R;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * 3d滚轮控件
 */
public class WheelView extends View {

    public enum ACTION { // 点击，滑翔(滑到尽头)，拖拽事件
        CLICK, FLING, DAGGLE
    }
    public enum DividerType { // 分隔线类型
        FILL, WRAP
    }

    private DividerType dividerType = DividerType.FILL;//分隔线类型

    Context context;

    Handler handler;
    private GestureDetector gestureDetector;
    OnItemSelectedListener onItemSelectedListener;

    ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mFuture;

    List<String> items;

    int textColorOut;//未选项文字颜色
    int textColorCenter;//选中项文字颜色
    int dividerColor;//分割线颜色
    int textSize;//选项的文字大小 单位为sp
    boolean isLoop;//循环滚动
    float lineSpacingMultiplier;// 条目间距倍数 可用来设置上下间距
    private int mGravity;//文字显示位置
    int initPosition;//初始化默认选中项
    int visibleItemCount;// 绘制几个条目

    Paint paintOuterText;//未选项画笔
    Paint paintCenterText;//选中项画笔
    Paint paintIndicator;//分割线画笔
    private boolean isCenterLabel = true;//附加单位是否仅仅只显示在选中项后面
    private String label;//附加单位
    int maxTextWidth;//最大的文字宽
    int maxTextHeight;//最大的文字高
    float itemHeight;//每行高度
    Typeface typeface = Typeface.MONOSPACE;//字体样式，默认是等宽字体
    float firstLineY;// 第一条线Y坐标值
    float secondLineY;//第二条线Y坐标
    float centerY;//中间label绘制的Y坐标
    float totalScrollY = 0;//滚动总高度y值
    private int selectedItem;//选中的Item是第几个
    int preCurrentIndex;
    int measuredHeight;// WheelView 控件高度
    int measuredWidth;// WheelView 控件宽度
    // 半径
    int radius = 0;
    private int mOffset = 0;
    private float previousY = 0;
    long startTime = 0;
    private static final int VELOCITY_FLING = 5;// 修改这个值可以改变滑行速度
    int widthMeasureSpec;

    private int drawCenterContentStart = 0;//中间选中文字开始绘制位置
    private int drawOutContentStart = 0;//非中间文字开始绘制位置
    private static final float SCALE_CONTENT = 0.8F;//非中间文字则用此控制高度，压扁形成3d错觉
    private float centerContentOffset ;//偏移量

    public WheelView(Context context) {
        this(context, null);
    }

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density; // 屏幕密度（0.75/1.0/1.5/2.0/3.0）

        if (density<1){//根据密度不同进行适配
            centerContentOffset=2.4F;
        }else if (1<=density&&density<2){
            centerContentOffset = 3.6F;
        }else if (1<=density&&density<2){
            centerContentOffset = 4.5F;
        }else if (2<=density&&density<3){
            centerContentOffset = 6.0F;
        }else if (density>=3){
            centerContentOffset= density * 2.5F;
        }

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.pickerview, 0, 0);
            mGravity = a.getInt(R.styleable.pickerview_pickerview_gravity, Gravity.CENTER);
            textColorOut = a.getColor(R.styleable.pickerview_pickerview_textColorOut, 0xFFa8a8a8);
            textColorCenter = a.getColor(R.styleable.pickerview_pickerview_textColorCenter, 0xFF2a2a2a);
            dividerColor = a.getColor(R.styleable.pickerview_pickerview_dividerColor, 0xFFd5d5d5);
            textSize = a.getDimensionPixelOffset(R.styleable.pickerview_pickerview_textSize, sp2px(context, 16));
            lineSpacingMultiplier = a.getFloat(R.styleable.pickerview_pickerview_lineSpacingMultiplier, 2.0F);
            isLoop = a.getBoolean(R.styleable.pickerview_pickerview_isLoop, false);
            initPosition = a.getInt(R.styleable.pickerview_pickerview_initPosition, -1);
            visibleItemCount = a.getInt(R.styleable.pickerview_pickerview_visibleItemCount, 7);
            a.recycle();//回收内存
        }

        judgeLineSpace();

        initLoopView(context);
    }

    /**
     * 判断间距是否在1.0-4.0之间
     */
    private void judgeLineSpace() {
        if (lineSpacingMultiplier < 1.5f) {
            lineSpacingMultiplier = 1.5f;
        } else if (lineSpacingMultiplier > 4.0f) {
            lineSpacingMultiplier = 4.0f;
        }
    }

    private void initLoopView(Context context) {
        this.context = context;
        handler = new MessageHandler(this);
        gestureDetector = new GestureDetector(context, new LoopViewGestureListener(this));
        gestureDetector.setIsLongpressEnabled(false);

        initPaints();
    }

    private void initPaints() {
        paintOuterText = new Paint();
        paintOuterText.setColor(textColorOut);
        paintOuterText.setAntiAlias(true);
        paintOuterText.setTypeface(typeface);
        paintOuterText.setTextSize(textSize);

        paintCenterText = new Paint();
        paintCenterText.setColor(textColorCenter);
        paintCenterText.setAntiAlias(true);
        //paintCenterText.setTextScaleX(1.1F);
        paintCenterText.setTypeface(typeface);
        paintCenterText.setTextSize(textSize);

        paintIndicator = new Paint();
        paintIndicator.setColor(dividerColor);
        paintIndicator.setAntiAlias(true);

        if (android.os.Build.VERSION.SDK_INT >= 11) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
    }

    private void remeasure() {//重新测量
        if (items == null) {
            return;
        }

        measureTextWidthHeight();

        //半圆的周长 = item高度乘以item数目-1
        int halfCircumference = (int) (itemHeight * (visibleItemCount - 1));
        //整个圆的周长除以PI得到直径，这个直径用作控件的总高度
        measuredHeight = (int) ((halfCircumference * 2) / Math.PI);
        //求出半径
        radius = (int) (halfCircumference / Math.PI);
        //控件宽度，这里支持weight
        measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        //计算两条横线 和 选中项画笔的基线Y位置
        firstLineY = (measuredHeight - itemHeight) / 2.0F;
        secondLineY = (measuredHeight + itemHeight) / 2.0F;
        centerY = secondLineY - (itemHeight-maxTextHeight)/2.0f - centerContentOffset;

        //初始化显示的item的position
        if (initPosition == -1) {
            if (isLoop) {
                initPosition = (items.size() + 1) / 2;
            } else {
                initPosition = 0;
            }
        }
        preCurrentIndex = initPosition;
    }

    /**
     * 计算最大length的Text的宽高度
     */
    private void measureTextWidthHeight() {
        Rect rect = new Rect();
        for (int i = 0; i < items.size(); i++) {
            String s1 = getContentText(items.get(i));
            paintCenterText.getTextBounds(s1, 0, s1.length(), rect);

            int textWidth = rect.width();

            if (textWidth > maxTextWidth) {
                maxTextWidth = textWidth;
            }
            paintCenterText.getTextBounds("\u661F\u671F", 0, 2, rect); // 星期的字符编码（以它为标准高度）

            maxTextHeight = rect.height() + 2;

        }
        itemHeight = lineSpacingMultiplier * maxTextHeight;
    }

    void smoothScroll(ACTION action) {//平滑滚动的实现
        cancelFuture();
        if (action == ACTION.FLING || action == ACTION.DAGGLE) {
            mOffset = (int) ((totalScrollY % itemHeight + itemHeight) % itemHeight);
            if ((float) mOffset > itemHeight / 2.0F) {//如果超过Item高度的一半，滚动到下一个Item去
                mOffset = (int) (itemHeight - (float) mOffset);
            } else {
                mOffset = -mOffset;
            }
        }
        //停止的时候，位置有偏移，不是全部都能正确停止到中间位置的，这里把文字位置挪回中间去
        mFuture = mExecutor.scheduleWithFixedDelay(new SmoothScrollTimerTask(this, mOffset), 0, 10, TimeUnit.MILLISECONDS);
    }

    protected final void scrollBy(float velocityY) {//滚动惯性的实现
        cancelFuture();
        mFuture = mExecutor.scheduleWithFixedDelay(new InertiaTimerTask(this, velocityY), 0, VELOCITY_FLING, TimeUnit.MILLISECONDS);
    }

    public void cancelFuture() {
        if (mFuture != null && !mFuture.isCancelled()) {
            mFuture.cancel(true);
            mFuture = null;
        }
    }

    /**
     * 设置是否循环滚动
     *
     * @param isLoop 是否循环
     */
    public final void setLoop(boolean isLoop) {
        this.isLoop = isLoop;
    }

    /**
     * 设置显示的选项个数，必须是奇数
     */
    public final void setVisibleItemCount(int count) {
        if (count % 2 == 0) {
            throw new IllegalArgumentException("must be odd");
        }
        if (count != visibleItemCount) {
            visibleItemCount = count;
        }
    }

    public final void setTypeface(Typeface font) {
        typeface = font;
        paintOuterText.setTypeface(typeface);
        paintCenterText.setTypeface(typeface);
    }

    public final void setTextSize(float size) {
        if (size > 0.0F ) {
            textSize = (int) (context.getResources().getDisplayMetrics().density * size);
            paintOuterText.setTextSize(textSize);
            paintCenterText.setTextSize(textSize);
        }
    }

    public final void setCurrentItem(int position) {
        this.initPosition = position;
        totalScrollY = 0;//回归顶部，不然重设setCurrentItem的话位置会偏移的，就会显示出不对位置的数据
        invalidate();

        if (items == null || items.isEmpty()) {
            return;
        }
        int size = items.size();
        if (position >= 0 && position < size && position != selectedItem) {
            initPosition = position;
            totalScrollY = 0;
            mOffset = 0;
            invalidate();
        }

    }

    public final void setOnItemSelectedListener(OnItemSelectedListener OnItemSelectedListener) {
        this.onItemSelectedListener = OnItemSelectedListener;
    }

    public final void setItems(List<String> items) {
        this.items = items;
        remeasure();
        invalidate();
    }

    public final void setItems(String[] items) {
        this.items = Arrays.asList(items);
    }

    public final int getCurrentItem() {
        return selectedItem;
    }

    protected final void onItemSelected() {
        if (onItemSelectedListener != null) {
            postDelayed(new OnItemSelectedRunnable(this), 200L);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (items == null || items.size() == 0) {
            return;
        }

        //可见的item数组
        String visibles[] = new String[visibleItemCount];
        //滚动的Y值高度除去每行Item的高度，得到滚动了多少个item，即change数
        int change = (int)(totalScrollY / itemHeight);

        //滚动中实际的预选中的item(即经过了中间位置的item) ＝ 滑动前的位置 ＋ 滑动相对位置
        preCurrentIndex = initPosition + change % items.size();

        if (!isLoop) {//不循环的情况
            if (preCurrentIndex < 0) {
                preCurrentIndex = 0;
            }
            if (preCurrentIndex > items.size() - 1) {
                preCurrentIndex = items.size() - 1;
            }
        } else {//循环
            if (preCurrentIndex < 0) {//举个例子：如果总数是5，preCurrentIndex ＝ －1，那么preCurrentIndex按循环来说，其实是0的上面，也就是4的位置
                preCurrentIndex = items.size() + preCurrentIndex;
            }
            if (preCurrentIndex > items.size() - 1) {//同理上面,自己脑补一下
                preCurrentIndex = preCurrentIndex - items.size();
            }
        }
        //跟滚动流畅度有关，总滑动距离与每个item高度取余，即并不是一格格的滚动，每个item不一定滚到对应Rect里的，这个item对应格子的偏移值
        float itemHeightOffset = (totalScrollY % itemHeight);

        // 设置数组中每个元素的值
        int counter = 0;
        while (counter < visibleItemCount) {
            int index = preCurrentIndex - (visibleItemCount / 2 - counter);//索引值，即当前在控件中间的item看作数据源的中间，计算出相对源数据源的index值
            //判断是否循环，如果是循环数据源也使用相对循环的position获取对应的item值，如果不是循环则超出数据源范围使用""空白字符串填充，在界面上形成空白无数据的item项
            if (isLoop) {
                index = getLoopMappingIndex(index);
                visibles[counter] = items.get(index);
            } else if (index < 0) {
                visibles[counter] = "";
            } else if (index > items.size() - 1) {
                visibles[counter] = "";
            } else {
                visibles[counter] = items.get(index);
            }
            counter++;

        }

        //绘制中间两条横线
        if (dividerType == DividerType.WRAP){//横线长度仅包裹内容
            float startX;
            float endX;

            if (TextUtils.isEmpty(label)){//隐藏Label的情况
                startX = (measuredWidth - maxTextWidth)/2 - 12;
            }else {
                startX = (measuredWidth - maxTextWidth)/4 - 12;
            }

            if (startX<=0){//如果超过了WheelView的边缘
                startX = 10;
            }
            endX = measuredWidth - startX;
            canvas.drawLine(startX, firstLineY, endX, firstLineY, paintIndicator);
            canvas.drawLine(startX, secondLineY, endX, secondLineY, paintIndicator);
        }else {
            canvas.drawLine(0.0F, firstLineY, measuredWidth, firstLineY, paintIndicator);
            canvas.drawLine(0.0F, secondLineY, measuredWidth, secondLineY, paintIndicator);
        }

        //只显示选中项Label文字的模式，并且Label文字不为空，则进行绘制
        if (!TextUtils.isEmpty(label)&& isCenterLabel) {
            //绘制文字，靠右并留出空隙
            int drawRightContentStart = measuredWidth - getTextWidth(paintCenterText, label);
            canvas.drawText(label, drawRightContentStart - centerContentOffset, centerY, paintCenterText);
        }

        counter = 0;
        while (counter < visibleItemCount) {
            canvas.save();
            // 弧长 L = itemHeight * counter - itemHeightOffset
            // 求弧度 α = L / r  (弧长/半径) [0,π]
            double radian = ((itemHeight * counter - itemHeightOffset)) / radius;
            // 弧度转换成角度(把半圆以Y轴为轴心向右转90度，使其处于第一象限及第四象限
            // angle [-90°,90°]
            float angle = (float) (90D - (radian / Math.PI) * 180D);//item第一项,从90度开始，逐渐递减到 -90度

            // 计算取值可能有细微偏差，保证负90°到90°以外的不绘制
            if (angle >= 90F || angle <= -90F) {
                canvas.restore();
            } else {
                //获取内容文字
                String contentText;

                //如果是label每项都显示的模式，并且item内容不为空、label 也不为空
                if(!isCenterLabel&&!TextUtils.isEmpty(label) &&!TextUtils.isEmpty(getContentText(visibles[counter]))){
                    contentText = getContentText(visibles[counter])+label;
                }else {
                    contentText = getContentText(visibles[counter]);
                }

                reMeasureTextSize(contentText);
                //计算开始绘制的位置
                measuredCenterContentStart(contentText);
                measuredOutContentStart(contentText);
                float translateY = (float) (radius - Math.cos(radian) * radius - (Math.sin(radian) * maxTextHeight) / 2D);
                //根据Math.sin(radian)来更改canvas坐标系原点，然后缩放画布，使得文字高度进行缩放，形成弧形3d视觉差
                canvas.translate(0.0F, translateY);
                canvas.scale(1.0F, (float) Math.sin(radian));
                if (translateY <= firstLineY && maxTextHeight + translateY >= firstLineY) {
                    // 条目经过第一条线
                    canvas.save();
                    canvas.clipRect(0, 0, measuredWidth, firstLineY - translateY);
                    //canvas.scale(1.0F, (float) Math.sin(radian) * SCALE_CONTENT);
                    canvas.drawText(contentText, drawOutContentStart, maxTextHeight, paintOuterText);
                    canvas.restore();
                    canvas.save();
                    canvas.clipRect(0, firstLineY - translateY, measuredWidth, (int) (itemHeight));
                    //canvas.scale(1.0F, (float) Math.sin(radian) * 1.0F);
                    canvas.drawText(contentText, drawCenterContentStart, maxTextHeight - centerContentOffset, paintCenterText);
                    canvas.restore();
                } else if (translateY <= secondLineY && maxTextHeight + translateY >= secondLineY) {
                    // 条目经过第二条线
                    canvas.save();
                    canvas.clipRect(0, 0, measuredWidth, secondLineY - translateY);
                    //canvas.scale(1.0F, (float) Math.sin(radian) * 1.0F);
                    canvas.drawText(contentText, drawCenterContentStart, maxTextHeight - centerContentOffset, paintCenterText);
                    canvas.restore();
                    canvas.save();
                    canvas.clipRect(0, secondLineY - translateY, measuredWidth, (int) (itemHeight));
                    //canvas.scale(1.0F, (float) Math.sin(radian) * SCALE_CONTENT);
                    canvas.drawText(contentText, drawOutContentStart, maxTextHeight, paintOuterText);
                    canvas.restore();
                } else if (translateY >= firstLineY && maxTextHeight + translateY <= secondLineY) {
                    // 中间条目
                    canvas.clipRect(0, 0, measuredWidth,   maxTextHeight);
                    //让文字居中
                    float Y = maxTextHeight - centerContentOffset;//因为圆弧角换算的向下取值，导致角度稍微有点偏差，加上画笔的基线会偏上，因此需要偏移量修正一下
                    canvas.drawText(contentText, drawCenterContentStart, Y, paintCenterText);

                    selectedItem = items.indexOf(visibles[counter]);

                } else {
                    // 其他条目
                    canvas.save();
                    canvas.clipRect(0, 0, measuredWidth, (int) (itemHeight));
                    //canvas.scale(1.0F, (float) Math.sin(radian) * SCALE_CONTENT);
                    canvas.drawText(contentText, drawOutContentStart, maxTextHeight, paintOuterText);
                    canvas.restore();
                }
                canvas.restore();
                paintCenterText.setTextSize(textSize);
            }
            counter++;
        }
    }

    /**
     * 根据文字的长度 重新设置文字的大小 让其能完全显示
     * @param contentText
     */
    private void reMeasureTextSize(String contentText) {
        Rect rect = new Rect();
        paintCenterText.getTextBounds(contentText, 0, contentText.length(), rect);
        int width = rect.width();
        int size = textSize;
        while (width > measuredWidth) {
            size--;
            //设置2条横线中间的文字大小
            paintCenterText.setTextSize(size);
            paintCenterText.getTextBounds(contentText, 0, contentText.length(), rect);
            width = rect.width();
        }
        //设置2条横线外面的文字大小
        paintOuterText.setTextSize(size);
    }


    //递归计算出对应的index
    private int getLoopMappingIndex(int index) {
        if (index < 0) {
            index = index + items.size();
            index = getLoopMappingIndex(index);
        } else if (index > items.size() - 1) {
            index = index - items.size();
            index = getLoopMappingIndex(index);
        }
        return index;
    }

    /**
     * @param item 数据源的item
     * @return 对应显示的字符串
     */
    private String getContentText(Object item) {
        if (item == null) {
            return "";
        } else if (item instanceof Integer) {
            //如果为整形则最少保留两位数.
            return String.format(Locale.getDefault(), "%02d", (int) item);
        }
        return item.toString();
    }

    private void measuredCenterContentStart(String content) {
        Rect rect = new Rect();
        paintCenterText.getTextBounds(content, 0, content.length(), rect);
        switch (mGravity) {
            case Gravity.CENTER://显示内容居中
                if (TextUtils.isEmpty(label) || !isCenterLabel) {
                    drawCenterContentStart = (int) ((measuredWidth - rect.width()) * 0.5);
                } else {//只显示中间label时，时间选择器内容偏左一点，留出空间绘制单位标签
                    drawCenterContentStart = (int) ((measuredWidth - rect.width()) * 0.25);
                }
                break;
            case Gravity.LEFT:
                drawCenterContentStart = 0;
                break;
            case Gravity.RIGHT://添加偏移量
                drawCenterContentStart = measuredWidth - rect.width() -(int)centerContentOffset;
                break;
        }
    }

    private void measuredOutContentStart(String content) {
        Rect rect = new Rect();
        paintOuterText.getTextBounds(content, 0, content.length(), rect);
        switch (mGravity) {
            case Gravity.CENTER:
                if (TextUtils.isEmpty(label) || !isCenterLabel) {
                    drawOutContentStart = (int) ((measuredWidth - rect.width()) * 0.5);
                } else {//只显示中间label时，时间选择器内容偏左一点，留出空间绘制单位标签
                    drawOutContentStart = (int) ((measuredWidth - rect.width()) * 0.25);
                }
                break;
            case Gravity.LEFT:
                drawOutContentStart = 0;
                break;
            case Gravity.RIGHT:
                drawOutContentStart = measuredWidth - rect.width()-(int)centerContentOffset;
                break;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.widthMeasureSpec = widthMeasureSpec;
        remeasure();
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean eventConsumed = gestureDetector.onTouchEvent(event);
        switch (event.getAction()) {
            //按下
            case MotionEvent.ACTION_DOWN:
                startTime = System.currentTimeMillis();
                cancelFuture();
                previousY = event.getRawY();
                break;
            //滑动中
            case MotionEvent.ACTION_MOVE:

                float dy = previousY - event.getRawY();
                previousY = event.getRawY();
                totalScrollY = totalScrollY + dy;

                // 边界处理。
                if (!isLoop) {
                    float top = -initPosition * itemHeight;
                    float bottom = (items.size() - 1 - initPosition) * itemHeight;


                    if (totalScrollY - itemHeight * 0.25 < top) {
                        top = totalScrollY - dy;
                    } else if (totalScrollY + itemHeight *  0.25 > bottom) {
                        bottom = totalScrollY - dy;
                    }

                    if (totalScrollY < top) {
                        totalScrollY = (int) top;
                    } else if (totalScrollY > bottom) {
                        totalScrollY = (int) bottom;
                    }
                }
                break;
            //完成滑动，手指离开屏幕
            case MotionEvent.ACTION_UP:

            default:
                if (!eventConsumed) {//未消费掉事件

                    /**
                     * TODO<关于弧长的计算>
                     *
                     * 弧长公式： L = α*R
                     * 反余弦公式：arccos(cosα) = α
                     * 由于之前是有顺时针偏移90度，
                     * 所以实际弧度范围α2的值 ：α2 = π/2-α    （α=[0,π] α2 = [-π/2,π/2]）
                     * 根据正弦余弦转换公式 cosα = sin(π/2-α)
                     * 代入，得： cosα = sin(π/2-α) = sinα2 = (R - y) / R
                     * 所以弧长 L = arccos(cosα)*R = arccos((R - y) / R)*R
                     */

                    float y = event.getY();
                    double L = Math.acos((radius - y) / radius) * radius;
                    //item0 有一半是在不可见区域，所以需要加上 itemHeight / 2
                    int circlePosition = (int) ((L + itemHeight / 2) / itemHeight);
                    float extraOffset = (totalScrollY % itemHeight + itemHeight) % itemHeight;
                    //已滑动的弧长值
                    mOffset = (int) ((circlePosition - visibleItemCount / 2) * itemHeight - extraOffset);

                    if ((System.currentTimeMillis() - startTime) > 120) {
                        // 处理拖拽事件
                        smoothScroll(ACTION.DAGGLE);
                    } else {
                        // 处理条目点击事件
                        smoothScroll(ACTION.CLICK);
                    }
                }
                break;
        }

        invalidate();
        return true;
    }

    /**
     * 获取Item个数
     *
     * @return item个数
     */
    public int getItemsCount() {
        return items != null ? items.size() : 0;
    }

    /**
     * 附加在右边的单位字符串
     *
     * @param label 单位
     */
    public void setLabel(String label) {
        this.label = label;
    }

    public void isCenterLabel(Boolean isCenterLabel) {
        this.isCenterLabel = isCenterLabel;
    }

    public void setGravity(int gravity) {
        this.mGravity = gravity;
    }

    public int getTextWidth(Paint paint, String str) {//计算文字宽度
        int iRet = 0;
        if (str != null && str.length() > 0) {
            int len = str.length();
            float[] widths = new float[len];
            paint.getTextWidths(str, widths);
            for (int j = 0; j < len; j++) {
                iRet += (int) Math.ceil(widths[j]);
            }
        }
        return iRet;
    }

    public void setTextColorOut(int textColorOut) {
        if (textColorOut != 0) {
            this.textColorOut = textColorOut;
            paintOuterText.setColor(textColorOut);
        }
    }

    public void setTextColorCenter(int textColorCenter) {
        if (textColorCenter != 0) {
            this.textColorCenter = textColorCenter;
            paintCenterText.setColor(textColorCenter);
        }
    }

    public void setDividerColor(int dividerColor) {
        if (dividerColor != 0) {
            this.dividerColor = dividerColor;
            paintIndicator.setColor(dividerColor);
        }
    }
    public void setDividerType(DividerType dividerType) {
        this.dividerType = dividerType;
    }

    public void setLineSpacingMultiplier(float lineSpacingMultiplier) {
        if (lineSpacingMultiplier != 0) {
            this.lineSpacingMultiplier = lineSpacingMultiplier;
            judgeLineSpace();
        }
    }

    public int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }


}