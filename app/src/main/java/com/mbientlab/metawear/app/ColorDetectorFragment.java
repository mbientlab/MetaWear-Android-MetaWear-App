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

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.mbientlab.metawear.ForcedDataProducer;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOption;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.ColorTcs34725;
import com.mbientlab.metawear.module.ColorTcs34725.*;
import com.mbientlab.metawear.module.Timer;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by etsai on 2/10/2016.
 */
public class ColorDetectorFragment extends SensorFragment {
    private static final int COLOR_SAMPLE_PERIOD = 33;

    private int rangeIndex= 0;
    private long startTime= -1;
    private Timer timerModule;
    private Timer.ScheduledTask scheduledTask;
    private ColorTcs34725 colorDetector;
    private final List<List<Entry>> colorAdc;

    public ColorDetectorFragment() {
        super(R.string.navigation_fragment_color_detector, R.layout.fragment_sensor_config_spinner, 0, 1024);

        colorAdc= new ArrayList<>();
        colorAdc.add(new ArrayList<>());
        colorAdc.add(new ArrayList<>());
        colorAdc.add(new ArrayList<>());
        colorAdc.add(new ArrayList<>());
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) view.findViewById(R.id.config_option_title)).setText(R.string.config_name_light_gain);

        ArrayAdapter<CharSequence> spinnerAdapter= ArrayAdapter.createFromResource(getContext(), R.array.values_tcs34725_light_gain, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner rotationRangeSelection= (Spinner) view.findViewById(R.id.config_option_spinner);
        rotationRangeSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                rangeIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        rotationRangeSelection.setAdapter(spinnerAdapter);
        rotationRangeSelection.setSelection(rangeIndex);
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        colorDetector = mwBoard.getModuleOrThrow(ColorTcs34725.class);
        timerModule= mwBoard.getModule(Timer.class);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_light_gain, R.string.config_desc_color_gain));
    }

    @Override
    protected void setup() {
        ForcedDataProducer colorAdc = colorDetector.adc();

        colorDetector.configure()
                .gain(Gain.values()[rangeIndex])
                .commit();
        colorAdc.addRouteAsync(source -> source.stream((data, env) -> {
            LineData chartData = chart.getData();
            if (startTime == -1) {
                chartData.addXValue("0");
                startTime = System.currentTimeMillis();
            } else {
                chartData.addXValue(String.format(Locale.US, "%.2f", (sampleCount * COLOR_SAMPLE_PERIOD) / 1000.f));
            }

            ColorAdc adc= data.value(ColorAdc.class);
            chartData.addEntry(new Entry(adc.clear, sampleCount), 0);
            chartData.addEntry(new Entry(adc.red, sampleCount), 1);
            chartData.addEntry(new Entry(adc.green, sampleCount), 2);
            chartData.addEntry(new Entry(adc.blue, sampleCount), 3);
            sampleCount++;

            updateChart();
        })).continueWithTask(task -> {
            streamRoute = task.getResult();

            return timerModule.scheduleAsync(COLOR_SAMPLE_PERIOD, false, colorAdc::read);
        }).continueWith(task -> {
            scheduledTask = task.getResult();
            scheduledTask.start();
            return null;
        });
    }

    @Override
    protected void clean() {
        scheduledTask.remove();
    }

    @Override
    protected void resetData(boolean clearData) {
        if (clearData) {
            sampleCount = 0;
            chartXValues.clear();

            for(List<Entry> it: colorAdc) {
                it.clear();
            }

            startTime= -1;
        }

        final int[] lineColors= new int[] {Color.BLACK, Color.RED, Color.GREEN, Color.BLUE};
        final String[] setName= new String[] {"clear", "red", "green", "blue"};

        LineData data= new LineData(chartXValues);
        for(int i= 0; i < colorAdc.size(); i++) {
            LineDataSet dataSet= new LineDataSet(colorAdc.get(i), setName[i]);
            dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            dataSet.setColor(lineColors[i]);
            dataSet.setDrawCircles(false);
            data.addDataSet(dataSet);
        }

        data.setDrawValues(false);
        chart.setData(data);
    }

    @Override
    protected String saveData() {
        final String CSV_HEADER = String.format("time,clear,red,green,blue%n");
        String filename = String.format(Locale.US, "%s_%tY%<tm%<td-%<tH%<tM%<tS%<tL.csv", getContext().getString(sensorResId), Calendar.getInstance());

        try {
            FileOutputStream fos = getActivity().openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(CSV_HEADER.getBytes());

            LineData data = chart.getLineData();
            LineDataSet clearDataSet = data.getDataSetByIndex(0), redDataSet = data.getDataSetByIndex(1),
                    greenDataSet = data.getDataSetByIndex(2), blueDataSet = data.getDataSetByIndex(3);
            for (int i = 0; i < data.getXValCount(); i++) {
                fos.write(String.format(Locale.US, "%.3f,%.3f,%.3f,%.3f,%.3f%n", (i * COLOR_SAMPLE_PERIOD) / 1000.f,
                        clearDataSet.getEntryForXIndex(i).getVal(), redDataSet.getEntryForXIndex(i).getVal(),
                        greenDataSet.getEntryForXIndex(i).getVal(), blueDataSet.getEntryForXIndex(i).getVal()).getBytes());
            }
            fos.close();
            return filename;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
