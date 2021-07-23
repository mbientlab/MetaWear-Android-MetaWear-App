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

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOption;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.AmbientLightLtr329;
import com.mbientlab.metawear.module.AmbientLightLtr329.*;

import java.util.Locale;

/**
 * Created by etsai on 8/22/2015.
 */
public class AmbientLightFragment extends SingleDataSensorFragment {
    private static final int LIGHT_SAMPLE_PERIOD= 50;

    private int sensorGainIndex= 0;
    private AmbientLightLtr329 alsltr329;
    private long startTime= -1;

    public AmbientLightFragment() {
        super(R.string.navigation_fragment_light, "illuminance", R.layout.fragment_sensor_config_spinner, LIGHT_SAMPLE_PERIOD / 1000.f, 1, 64000f);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) view.findViewById(R.id.config_option_title)).setText(R.string.config_name_light_gain);

        Spinner gainSelection= (Spinner) view.findViewById(R.id.config_option_spinner);
        gainSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sensorGainIndex = position;

                switch (Gain.values()[sensorGainIndex]) {
                    case LTR329_1X:
                        min = 1f;
                        max = 64000f;
                        break;
                    case LTR329_2X:
                        min = 0.5f;
                        max = 32000f;
                        break;
                    case LTR329_4X:
                        min = 0.25f;
                        max = 16000f;
                        break;
                    case LTR329_8X:
                        min = 0.125f;
                        max = 8000f;
                        break;
                    case LTR329_48X:
                        min = 0.02f;
                        max = 1300f;
                        break;
                    case LTR329_96X:
                        min = 0.01f;
                        max = 600f;
                        break;
                }

                final YAxis leftAxis = chart.getAxisLeft();
                leftAxis.setAxisMaxValue(max);
                leftAxis.setAxisMinValue(min);
                refreshChart(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ArrayAdapter<CharSequence> spinnerAdapter= ArrayAdapter.createFromResource(getContext(), R.array.values_light_gain, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gainSelection.setAdapter(spinnerAdapter);
        gainSelection.setSelection(sensorGainIndex);
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        alsltr329 = mwBoard.getModuleOrThrow(AmbientLightLtr329.class);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_light_gain, R.string.config_desc_light_gain));
    }

    @Override
    protected void setup() {
        alsltr329.configure().gain(Gain.values()[sensorGainIndex])
                .measurementRate(MeasurementRate.LTR329_RATE_50MS)
                .integrationTime(IntegrationTime.LTR329_TIME_50MS)
                .commit();
        alsltr329.illuminance().addRouteAsync(source -> {
            source.stream((data, env) -> {
                final Float lux = data.value(Float.class);

                LineData chartData = chart.getData();

                if (startTime == -1) {
                    chartData.addXValue("0");
                    startTime= System.currentTimeMillis();
                } else {
                    chartData.addXValue(String.format(Locale.US, "%.2f", sampleCount * samplingPeriod));
                }
                chartData.addEntry(new Entry(lux, sampleCount), 0);

                sampleCount++;
                updateChart();
            });
        }).continueWith(task -> {
            streamRoute = task.getResult();
            alsltr329.illuminance().start();
            return null;
        });
    }

    @Override
    protected void clean() {
        alsltr329.illuminance().stop();
    }

    @Override
    protected void resetData(boolean clearData) {
        super.resetData(clearData);

        if (clearData) {
            startTime= -1;
        }
    }
}
