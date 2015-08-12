package com.vaavud.android.ui.measure;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vaavud.android.R;
import com.vaavud.android.model.entity.DirectionUnit;
import com.vaavud.android.model.entity.SpeedUnit;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

/**
 * TODO: document your custom view class.
 */
public class SharingView extends RelativeLayout {

		private GraphicalView mChartView;
		private ImageView arrowView;
		private TextView meanText;
		private TextView actualText;
		private TextView maxText;
		private TextView directionText;


		public SharingView(Context context, Float currentActualValueMS, Float currentDirection, Float currentMeanValueMS, Float currentMaxValueMS, XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer, SpeedUnit currentUnit, DirectionUnit currentDirectionUnit) {
				super(context);
				LayoutInflater.from(context).inflate(
								R.layout.view_sharing,
								this,
								true);

				mChartView = ChartFactory.getCubeLineChartView(context, dataset, renderer, 0.3f);

				((TextView) findViewById(R.id.meanLabeltext)).setText(getResources().getString(R.string.heading_average).toUpperCase());
				((TextView) findViewById(R.id.actualLabelText)).setText(getResources().getString(R.string.heading_current).toUpperCase());
				((TextView) findViewById(R.id.maxLabelText)).setText(getResources().getString(R.string.heading_max).toUpperCase());
				((TextView) findViewById(R.id.unitLabelText)).setText(getResources().getString(R.string.heading_unit).toUpperCase());
				TextView directionLabelText = ((TextView) findViewById(R.id.directionLabelText));
				directionLabelText.setText(getResources().getString(R.string.direction_unit).toUpperCase());


				meanText = (TextView) findViewById(R.id.meanTextShare);
				actualText = (TextView) findViewById(R.id.actualTextShare);
				maxText = (TextView) findViewById(R.id.maxTextShare);
				directionText = (TextView) findViewById(R.id.directionTextShare);
				arrowView = (ImageView) findViewById(R.id.arrowViewShare);

				if (currentDirection != null) {
						Bitmap arrow = BitmapFactory.decodeResource(context.getResources(), R.drawable.wind_arrow);
						if (arrow == null) return;
						Matrix matrix = new Matrix();
						matrix.postRotate(currentDirection);
						directionText.setText(currentDirectionUnit.format(currentDirection));
						Bitmap rotatedBitmap = Bitmap.createBitmap(arrow, 0, 0,
										arrow.getWidth(),
										arrow.getHeight(), matrix, true);
						arrowView.setImageBitmap(rotatedBitmap);
						arrowView.setScaleType(ImageView.ScaleType.CENTER);

				} else {
						directionText.setVisibility(View.INVISIBLE);
						directionLabelText.setVisibility(View.INVISIBLE);
				}

				actualText.setText(currentUnit.format(currentActualValueMS));
				meanText.setText(currentUnit.format(currentMeanValueMS));
				maxText.setText(currentUnit.format(currentMaxValueMS));


				LinearLayout layout = (LinearLayout) findViewById(R.id.chartShare);
				layout.addView(mChartView);
				mChartView.setBackgroundColor(context.getResources().getColor(R.color.lightgray));
				mChartView.repaint();


		}


}
