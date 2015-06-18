package com.vaavud.android.ui.history;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vaavud.android.R;

public class ArrowLayoutView extends LinearLayout {

		private Context mContext;
//		private LinearLayout mView;

		public ArrowLayoutView(Context context) {
				super(context);
				mContext = context;

				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
				params.weight = 1;
				setOrientation(LinearLayout.VERTICAL);
				setLayoutParams(params);
				ArrowView arrowView = new ArrowView(mContext);
				LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
				arrowParams.weight = (float) 0.7;
				arrowParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
				arrowView.setLayoutParams(arrowParams);
				TextView text = new TextView(mContext);
				text.setText(mContext.getResources().getString(R.string.history_no_measurements));
				text.setTextColor(mContext.getResources().getColor(R.color.blue));
				text.setTextSize(24);
				text.setLines(2);

				TextView subtext = new TextView(mContext);
				subtext.setText(mContext.getResources().getString(R.string.history_go_to_measure));
				subtext.setTextColor(Color.BLACK);
				subtext.setTextSize(16);

				LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
				textParams.weight = (float) 0.15;
				textParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
				textParams.topMargin = 40;
				textParams.leftMargin = 20;
				textParams.rightMargin = 20;
				textParams.gravity = Gravity.CENTER_HORIZONTAL;
				text.setLayoutParams(textParams);


				LinearLayout.LayoutParams subtextParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
				subtextParams.weight = (float) 0.15;
				subtextParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
				subtextParams.topMargin = -40;
				subtextParams.leftMargin = 20;
				subtextParams.rightMargin = 20;
				subtextParams.gravity = Gravity.CENTER_HORIZONTAL;

				text.setLayoutParams(textParams);
				subtext.setLayoutParams(subtextParams);

				addView(arrowView, 0);
				addView(text, 1);
				addView(subtext, 2);

		}

		private class ArrowView extends View {

				public ArrowView(Context context) {
						super(context);
				}

				protected void onDraw(Canvas canvas) {

						Paint circlePaint = new Paint();

						int left = canvas.getWidth() / 6;
						int top = canvas.getHeight() / 15 - canvas.getHeight();
						int right = canvas.getWidth() - canvas.getWidth() / 6;
						int bottom = canvas.getHeight();
						//to draw an arrow, just lines needed, so style is only STROKE
						circlePaint.setStyle(Paint.Style.STROKE);
						circlePaint.setPathEffect(new DashPathEffect(new float[]{20, 15}, 10));

						circlePaint.setStrokeWidth(5.0F);
						circlePaint.setColor(mContext.getResources().getColor(R.color.blue));
						//create a path to draw on
						Path arrowPath = new Path();

						//create an invisible oval. the oval is for "behind the scenes" ,to set the path´
						//area. Imagine this is an egg behind your circles. the circles are in the middle of this egg
						final RectF arrowOval = new RectF();
						arrowOval.set(left, top, right, bottom);

						//add the oval to path
						arrowPath.addArc(arrowOval, -180, -90);

						canvas.drawPath(arrowPath, circlePaint);
						arrowPath.moveTo(canvas.getWidth() / 6, canvas.getHeight() / 30); //move to the center of first circle
						arrowPath.lineTo(canvas.getWidth() / 6 + canvas.getHeight() / 20, canvas.getHeight() / 10 + canvas.getHeight() / 20);
						arrowPath.moveTo(canvas.getWidth() / 6, canvas.getHeight() / 30); //move to the center of first circle
						arrowPath.lineTo(canvas.getWidth() / 6 - canvas.getHeight() / 20, canvas.getHeight() / 10 + canvas.getHeight() / 20);
						canvas.drawPath(arrowPath, circlePaint);

				}
		}
}
