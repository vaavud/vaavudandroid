package com.vaavud.android.ui.measure;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vaavud.android.R;
import com.vaavud.android.model.entity.DirectionUnit;
import com.vaavud.android.model.entity.LatLng;
import com.vaavud.android.model.entity.SpeedUnit;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * TODO: document your custom view class.
 */
public class SharingView extends LinearLayout {

		private static final String TAG = "Vaavud:SharingView";
		private final TextView unitText;
		private ImageView arrowView;
		private TextView meanText;
		private TextView maxText;
		private TextView directionText;


		public SharingView(Context context, Float currentActualValueMS, Float currentDirection, Float currentMeanValueMS, Float currentMaxValueMS, XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer, SpeedUnit currentUnit, DirectionUnit currentDirectionUnit, LatLng location, String locationName) {
				super(context);
				LayoutInflater.from(context).inflate(
								R.layout.view_sharing,
								this,
								true);

//				mChartView = ChartFactory.getCubeLineChartView(context, dataset, renderer, 0.3f);

//				((TextView) findViewById(R.id.meanLabeltext)).setText(getResources().getString(R.string.heading_average).toUpperCase());
//				((TextView) findViewById(R.id.maxLabelText)).setText(getResources().getString(R.string.heading_max).toUpperCase());
//				((TextView) findViewById(R.id.unitLabelText)).setText(getResources().getString(R.string.heading_unit).toUpperCase());
				((TextView) findViewById(R.id.date_view)).setText(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));

				TextView directionLabelText = ((TextView) findViewById(R.id.directionLabelText));

//				directionLabelText.setText(getResources().getString(R.string.direction_unit).toUpperCase());
				((TextView) findViewById(R.id.locationShare)).setText(locationName);

				meanText = (TextView) findViewById(R.id.meanTextShare);
				maxText = (TextView) findViewById(R.id.maxTextShare);
				directionText = (TextView) findViewById(R.id.directionTextShare);
				unitText = (TextView) findViewById(R.id.unitTextShare);
				arrowView = (ImageView) findViewById(R.id.image_arrow);
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
						directionText.setVisibility(View.GONE);
						directionLabelText.setVisibility(View.GONE);
						arrowView.setVisibility(View.GONE);
						this.removeView(directionLabelText);
						this.removeView(directionText);
						this.removeView(arrowView);
				}
				unitText.setText(currentUnit.getDisplayName(context));
				meanText.setText(currentUnit.format(currentMeanValueMS));
				maxText.setText(currentUnit.format(currentMaxValueMS));


//				LinearLayout layout = (LinearLayout) findViewById(R.id.chartShare);
//				layout.addView(mChartView);
//				mChartView.setBackgroundColor(context.getResources().getColor(R.color.lightgray));
//				mChartView.repaint();
				ImageView mapView = (ImageView) findViewById(R.id.mapShare);
				if (location!=null) {
						mapView.setScaleType(ImageView.ScaleType.FIT_XY);
						mapView.setImageBitmap(getGoogleMapThumbnail(location.getLatitude(), location.getLongitude()));
				}else{
						this.removeView(mapView);
				}
//				450.00 x 1080
				}


		private Bitmap getGoogleMapThumbnail(double lati, double longi) {

				int height = 250;
				int width = 1080;
				String iconUrl = "http://vaavud.com/appgfx/SmallWindMarker.png";
				String mapUrl = "http://maps.google.com/maps/api/staticmap";
				String markers = "icon:" + iconUrl + "|shadow:false|" + lati + "," + longi;

				try {
						markers = URLEncoder.encode(markers, "utf-8");
				} catch (UnsupportedEncodingException e) {
						// shouldn't happen
				}
				mapUrl += "?markers=" + markers + "&zoom=15&size=" + width + "x" + height + "&sensor=true";

				Bitmap bmp = null;

				try {
						URL url = new URL(mapUrl);
						URLConnection urlConnection = url.openConnection();
						InputStream in = new BufferedInputStream(urlConnection.getInputStream());
						bmp = BitmapFactory.decodeStream(in);
						in.close();
				} catch (IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (ClientProtocolException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}

				return bmp;
		}


}
