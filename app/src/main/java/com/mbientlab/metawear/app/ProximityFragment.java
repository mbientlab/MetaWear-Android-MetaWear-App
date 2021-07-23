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

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import com.mbientlab.metawear.ForcedDataProducer;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.ProximityTsl2671;
import com.mbientlab.metawear.module.ProximityTsl2671.TransmitterDriveCurrent;
import com.mbientlab.metawear.module.Timer;

import java.util.Locale;

/**
 * Created by etsai on 2/23/2016.
 */
public class ProximityFragment extends SingleDataSensorFragment {
    private static final int PROXIMITY_SAMPLE_PERIOD = 33;
    private long startTime= -1;
    private ProximityTsl2671 proximity;
    private Timer timer;
    private Timer.ScheduledTask scheduledTask;

    public ProximityFragment() {
        super(R.string.navigation_fragment_proximity, "adc", R.layout.fragment_sensor, PROXIMITY_SAMPLE_PERIOD / 1000.f, 0, 1024);
    }

    @Override
    protected void setup() {
        ForcedDataProducer proximityAdc = proximity.adc();
        proximity.configure()
                .transmitterDriveCurrent(TransmitterDriveCurrent.CURRENT_12_5MA)
                .commit();
        proximityAdc.addRouteAsync(source -> source.stream((data, env) -> {
            LineData chartData = chart.getData();

            if (startTime == -1) {
                chartData.addXValue("0");
                startTime = System.currentTimeMillis();
            } else {
                chartData.addXValue(String.format(Locale.US, "%.2f", sampleCount * samplingPeriod));
            }
            chartData.addEntry(new Entry(data.value(Integer.class), sampleCount), 0);

            sampleCount++;

            updateChart();
        })).continueWithTask(task -> {
            streamRoute = task.getResult();
            return timer.scheduleAsync(PROXIMITY_SAMPLE_PERIOD, false, proximityAdc::read);
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
    protected void boardReady() throws UnsupportedModuleException {
        proximity = mwBoard.getModuleOrThrow(ProximityTsl2671.class);
        timer = mwBoard.getModuleOrThrow(Timer.class);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {

    }

    @Override
    protected void resetData(boolean clearData) {
        super.resetData(clearData);

        if (clearData) {
            startTime= -1;
        }
    }
}
