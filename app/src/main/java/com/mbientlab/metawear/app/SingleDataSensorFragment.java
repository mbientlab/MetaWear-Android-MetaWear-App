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

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by etsai on 8/22/2015.
 */
public abstract class SingleDataSensorFragment extends SensorFragment {
    protected String csvHeaderDataName, filenameExtraString= "";
    protected float samplingPeriod;

    private final ArrayList<Entry> sensorData= new ArrayList<>();

    protected SingleDataSensorFragment(int stringResId, String dataName, int layoutId, float samplingPeriod, float min, float max) {
        super(stringResId, layoutId, min, max);
        csvHeaderDataName= dataName;
        this.samplingPeriod= samplingPeriod;
    }

    protected SingleDataSensorFragment(int stringResId, String dataName, int layoutId, float min, float max) {
        this(stringResId, dataName, layoutId, -1, min, max);
    }

    @Override
    protected String saveData() {
        final String CSV_HEADER = String.format("time,%s%n", csvHeaderDataName);
        String filename = String.format(Locale.US, "%s_%tY%<tm%<td-%<tH%<tM%<tS%<tL.csv", getContext().getString(sensorResId), Calendar.getInstance());
        if (!filenameExtraString.isEmpty()) {
            filename+= "_" + filenameExtraString;
        }

        try {
            FileOutputStream fos = getActivity().openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(CSV_HEADER.getBytes());

            LineData data = chart.getLineData();
            List<String> chartXValues= data.getXVals();
            LineDataSet tempDataSet = data.getDataSetByIndex(0);
            if (samplingPeriod < 0) {
                for (int i = 0; i < chartXValues.size(); i++) {
                    fos.write(String.format(Locale.US, "%s,%.3f%n", chartXValues.get(i), tempDataSet.getEntryForXIndex(i).getVal()).getBytes());
                }
            } else {
                for (int i = 0; i < data.getXValCount(); i++) {
                    fos.write(String.format(Locale.US, "%.3f,%.3f%n", i * samplingPeriod, tempDataSet.getEntryForXIndex(i).getVal()).getBytes());
                }
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
            chartXValues.clear();
            sensorData.clear();
            sampleCount = 0;
        }

        LineDataSet tempDataSet = new LineDataSet(sensorData, csvHeaderDataName);
        tempDataSet.setColor(Color.MAGENTA);
        tempDataSet.setDrawCircles(false);

        LineData data= new LineData(chartXValues);
        data.addDataSet(tempDataSet);
        data.setDrawValues(false);
        chart.setData(data);
    }
}
