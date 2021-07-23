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
import com.mbientlab.metawear.module.HumidityBme280;
import com.mbientlab.metawear.module.Timer;

import java.util.Locale;

/**
 * Created by etsai on 2/10/2016.
 */
public class HumidityFragment extends SingleDataSensorFragment {
    private static final int HUMIDITY_SAMPLE_PERIOD = 33;

    private long startTime= -1;
    private HumidityBme280 humidity;
    private Timer timerModule;
    private Timer.ScheduledTask scheduledTask;

    public HumidityFragment() {
        super(R.string.navigation_fragment_humidity, "percentage", R.layout.fragment_sensor, HUMIDITY_SAMPLE_PERIOD / 1000.f, 0, 100);
    }

    @Override
    protected void setup() {
        ForcedDataProducer humiditValue = humidity.value();
        humiditValue.addRouteAsync(source -> source.stream((data, env) -> {
            LineData chartData = chart.getData();

            if (startTime == -1) {
                chartData.addXValue("0");
                startTime = System.currentTimeMillis();
            } else {
                chartData.addXValue(String.format(Locale.US, "%.2f", sampleCount * samplingPeriod));
            }
            chartData.addEntry(new Entry(data.value(Float.class), sampleCount), 0);

            sampleCount++;
        })).continueWithTask(task -> {
            streamRoute = task.getResult();
            return timerModule.scheduleAsync(HUMIDITY_SAMPLE_PERIOD, false, humiditValue::read);
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
        humidity = mwBoard.getModuleOrThrow(HumidityBme280.class);
        timerModule= mwBoard.getModuleOrThrow(Timer.class);
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
