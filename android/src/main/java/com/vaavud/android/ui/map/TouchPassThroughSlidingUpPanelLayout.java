package com.vaavud.android.ui.map;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class TouchPassThroughSlidingUpPanelLayout extends SlidingUpPanelLayout {

	public TouchPassThroughSlidingUpPanelLayout(Context context) {
		super(context);
	}

	public TouchPassThroughSlidingUpPanelLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public TouchPassThroughSlidingUpPanelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mSlideOffset == 1F) { // collapsed
        	return false;
        }
        else {
            return super.onInterceptTouchEvent(ev);
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mSlideOffset == 1F) { // collapsed
        	return false;
        }
        else {
            return super.onTouchEvent(ev);
        }
    }
}