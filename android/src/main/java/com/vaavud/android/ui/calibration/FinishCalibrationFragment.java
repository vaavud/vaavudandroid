package com.vaavud.android.ui.calibration;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.R;
import com.vaavud.android.model.VaavudDatabase;
import com.vaavud.android.model.entity.Device;

public class FinishCalibrationFragment extends Fragment {

    private Context context;
    private static final String KEY_FIRST_TIME_SLEIPNIR = "firstTimeSleipnir";
    private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";

    public FinishCalibrationFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context =  activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Typeface robotoLight = Typeface.createFromAsset(context.getAssets(), "Roboto-Light.ttf");
        Typeface robotoRegular = Typeface.createFromAsset(context.getAssets(), "Roboto-Regular.ttf");

        View rootView = inflater.inflate(R.layout.fragment_end_calibration, container, false);

        TextView completed = (TextView) rootView.findViewById(R.id.calibration_completed);

        completed.setTypeface(robotoLight);

        Button done = (Button) rootView.findViewById(R.id.calibration_done);

        done.setTypeface(robotoRegular);
        done.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                VaavudDatabase.getInstance(context.getApplicationContext()).setPropertyAsBoolean(KEY_FIRST_TIME_SLEIPNIR, true);
                if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
                    MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Calibration Finished", null);
                }
                ((Activity)context).finish();
            }
        });

        return rootView;
    }
}
