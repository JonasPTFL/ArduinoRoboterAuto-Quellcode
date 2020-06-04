package de.pollpeter.jonas.rasteuerung;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatSeekBar;

/*
* VerticalSeekBar class, got from StackOverflow post:
* https://stackoverflow.com/a/28397530/9610875
* posted by the user "Jawad Zeb" on 08.02.2015
* Full link to the StackOverflow question:
* https://stackoverflow.com/questions/3333658/how-to-make-a-vertical-seekbar-in-android
*
* */
public class VerticalSeekBar extends AppCompatSeekBar {

    public VerticalSeekBar(Context context) {
        super(context);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    public synchronized void setProgress(int progress)  // it is necessary for calling setProgress on click of a button
    {
        super.setProgress(progress);
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }
    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    protected void onDraw(Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(), 0);

        super.onDraw(c);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
                onSizeChanged(getWidth(), getHeight(), 0, 0);
                break;

            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }
}