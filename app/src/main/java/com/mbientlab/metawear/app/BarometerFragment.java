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

import static java.lang.Integer.min;

import android.content.Context;
import android.graphics.Color;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import com.mbientlab.metawear.Executors;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.BarometerBosch;
import com.mbientlab.metawear.module.BarometerBosch.*;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by etsai on 8/22/2015.
 */
public class BarometerFragment extends SensorFragment {
    private static final float BAROMETER_SAMPLE_FREQ = 26.32f, LIGHT_SAMPLE_PERIOD= 1 / BAROMETER_SAMPLE_FREQ;

    private BarometerBosch barometer;
    private float altitudeMin, altitudeMax;

    private Route altitudeRoute = null;
    private final ArrayList<Entry> altitudeData= new ArrayList<>(), pressureData= new ArrayList<>();

    public BarometerFragment() {
        super(R.string.navigation_fragment_barometer, R.layout.fragment_sensor, 80000, 110000);
        altitudeMin= -300;
        altitudeMax= 1500;
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        barometer = mwBoard.getModuleOrThrow(BarometerBosch.class);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {

    }

    @Override
    protected void setup() {
        barometer.configure()
                .pressureOversampling(OversamplingMode.ULTRA_HIGH)
                .filterCoeff(FilterCoeff.OFF)
                .standbyTime(0.5f)
                .commit();

        barometer.pressure().addRouteAsync(source -> source.stream((data, env) -> {
            LineData chartData = chart.getData();
            if (sampleCount == 0) {
                chartData.removeEntry(0,0);
                chartData.removeEntry(0,1);
            }
            if (pressureData.size() >= sampleCount) {
                chartXValues.add(String.format(Locale.US, "%.2f", sampleCount*LIGHT_SAMPLE_PERIOD));
                sampleCount++;

                updateChart();
            }
            chartData.addEntry(new Entry(sampleCount, data.value(Float.class)), 0);
        })).continueWithTask(Executors.IMMEDIATE_EXECUTOR, task -> {
            streamRoute = task.getResult();

            return barometer.altitude().addRouteAsync(source -> source.stream((data, env) -> {
                LineData chartData = chart.getData();
                if (sampleCount == 0) {
                    chartData.removeEntry(0,0);
                    chartData.removeEntry(0,1);
                }
                if (altitudeData.size() >= sampleCount) {
                    chartXValues.add(String.format(Locale.US, "%.2f", sampleCount*LIGHT_SAMPLE_PERIOD));
                    sampleCount++;

                    updateChart();
                }
                chartData.addEntry(new Entry(sampleCount, data.value(Float.class)), 1);
            }));
        }).continueWith(Executors.IMMEDIATE_EXECUTOR, task -> {
            altitudeRoute = task.getResult();

            barometer.altitude().start();
            barometer.pressure().start();
            barometer.start();
            return null;
        });
    }

    @Override
    protected void initializeChart() {
        ///< configure axis settings
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMaximum(max);
        leftAxis.setAxisMinimum(min);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setAxisMaximum(altitudeMax);
        rightAxis.setAxisMinimum(altitudeMin);
    }

    @Override
    protected void clean() {
        barometer.stop();
        barometer.altitude().stop();
        barometer.pressure().stop();

        if (altitudeRoute != null) {
            altitudeRoute.remove();
            altitudeRoute = null;
        }
    }

    @Override
    protected String saveData() {
        final String CSV_HEADER = String.format("time,pressure,altitude%n");
        String filename = String.format(Locale.US, "%s_%tY%<tm%<td-%<tH%<tM%<tS%<tL.csv", getContext().getString(sensorResId), Calendar.getInstance());

        try {
            FileOutputStream fos = getActivity().openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(CSV_HEADER.getBytes());

            LineData data = chart.getLineData();
            ILineDataSet pressureDataSet = data.getDataSetByIndex(0), altitudeDataSet = data.getDataSetByIndex(1);
            int dataLen = min(pressureDataSet.getEntryCount(), altitudeDataSet.getEntryCount());
            for (int i = 0; i < dataLen; i++) {
                fos.write(String.format(Locale.US, "%.3f,%.3f,%.3f%n", i * LIGHT_SAMPLE_PERIOD,
                        pressureDataSet.getEntryForIndex(i).getY(),
                        altitudeDataSet.getEntryForIndex(i).getY()).getBytes());
            }
            fos.close();
            return filename;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void resetData(boolean clearData) {
        if (clearData) {
            sampleCount = 0;
            chartXValues.clear();
            altitudeData.clear();
            pressureData.clear();
        }

        ArrayList<LineDataSet> spinAxisData= new ArrayList<>();

        pressureData.add(new Entry(0, 0));
        spinAxisData.add(new LineDataSet(pressureData, "pressure"));
        spinAxisData.get(0).setAxisDependency(YAxis.AxisDependency.LEFT);
        spinAxisData.get(0).setColor(Color.RED);
        spinAxisData.get(0).setDrawCircles(false);

        altitudeData.add(new Entry(0, 0));
        spinAxisData.add(new LineDataSet(altitudeData, "altitude"));
        spinAxisData.get(1).setAxisDependency(YAxis.AxisDependency.RIGHT);
        spinAxisData.get(1).setColor(Color.GREEN);
        spinAxisData.get(1).setDrawCircles(false);

        LineData data= new LineData();
        for(LineDataSet set: spinAxisData) {
            data.addDataSet(set);
        }
        data.setDrawValues(false);
        chart.setData(data);
    }
}
