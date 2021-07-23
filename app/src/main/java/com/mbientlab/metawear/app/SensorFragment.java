/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.core.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.Switch;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.mbientlab.metawear.Route;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by etsai on 8/19/2015.
 */
public abstract class SensorFragment extends ModuleFragmentBase {
    protected final ArrayList<String> chartXValues= new ArrayList<>();
    protected LineChart chart;
    protected int sampleCount;
    protected long prevUpdate = -1;

    protected float min, max;
    protected Route streamRoute = null;

    private byte globalLayoutListenerCounter= 0;
    private final int layoutId;

    private final Handler chartHandler= new Handler();

    protected SensorFragment(int sensorResId, int layoutId, float min, float max) {
        super(sensorResId);
        this.layoutId= layoutId;
        this.min= min;
        this.max= max;
    }

    protected void updateChart() {
        long current = Calendar.getInstance().getTimeInMillis();
        if (prevUpdate == -1 || (current - prevUpdate) >= 33) {
            chartHandler.post(() -> {
                chart.getData().notifyDataChanged();
                chart.notifyDataSetChanged();

                moveViewToLast();
            });

            prevUpdate = current;
        }
    }
    private void moveViewToLast() {
        chart.setVisibleXRangeMinimum(120);
        chart.setVisibleXRangeMaximum(120);
        chart.moveViewToX(Math.max(0f, chartXValues.size() - 1));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        View v= inflater.inflate(layoutId, container, false);
        final View scrollView = v.findViewById(R.id.scrollView);
        if (scrollView != null) {
            globalLayoutListenerCounter= 1;
            scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    LineChart.LayoutParams params = chart.getLayoutParams();
                    params.height = scrollView.getHeight();
                    chart.setLayoutParams(params);

                    globalLayoutListenerCounter--;
                    if (globalLayoutListenerCounter < 0) {
                        scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            });
        }

        return v;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chart = (LineChart) view.findViewById(R.id.data_chart);

        initializeChart();
        resetData(false);
        chart.invalidate();
        chart.setDescription(null);

        Button clearButton= (Button) view.findViewById(R.id.layout_two_button_left);
        clearButton.setOnClickListener(view1 -> refreshChart(true));
        clearButton.setText(R.string.label_clear);

        ((Switch) view.findViewById(R.id.sample_control)).setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                moveViewToLast();
                setup();
            } else {
                chart.setVisibleXRangeMinimum(1);
                chart.setVisibleXRangeMaximum(sampleCount);
                clean();
                if (streamRoute != null) {
                    streamRoute.remove();
                    streamRoute = null;
                }
            }
        });

        Button saveButton= (Button) view.findViewById(R.id.layout_two_button_right);
        saveButton.setText(R.string.label_save);
        saveButton.setOnClickListener(view12 -> {
            String filename = saveData();

            if (filename != null) {
                File dataFile = getActivity().getFileStreamPath(filename);
                Uri contentUri = FileProvider.getUriForFile(getActivity(), "com.mbientlab.metawear.app.fileprovider", dataFile);

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, filename);
                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                startActivity(Intent.createChooser(intent, "Saving Data"));
            }
        });
    }

    protected void refreshChart(boolean clearData) {
        chart.resetTracking();
        chart.clear();
        resetData(clearData);
        chart.invalidate();
        chart.fitScreen();
    }

    protected void initializeChart() {
        ///< configure axis settings
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setStartAtZero(false);
        leftAxis.setAxisMaxValue(max);
        leftAxis.setAxisMinValue(min);
        chart.getAxisRight().setEnabled(false);
    }

    protected abstract void setup();
    protected abstract void clean();
    protected abstract String saveData();
    protected abstract void resetData(boolean clearData);
}
