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
import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOption;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.AccelerometerMma8452q;

/**
 * Created by etsai on 8/19/2015.
 */
public class AccelerometerFragment extends ThreeAxisChartFragment {
    private static final float[] MMA845Q_RANGES= {2.f, 4.f, 8.f}, BOSCH_RANGES = {2.f, 4.f, 8.f, 16.f};
    private static final float INITIAL_RANGE= 2.f, ACC_FREQ= 50.f;

    private Spinner accRangeSelection;
    private Accelerometer accelerometer = null;
    private int rangeIndex= 0;

    public AccelerometerFragment() {
        super("acceleration", R.layout.fragment_sensor_config_spinner,
                R.string.navigation_fragment_accelerometer, -INITIAL_RANGE, INITIAL_RANGE);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) view.findViewById(R.id.config_option_title)).setText(R.string.config_name_acc_range);

        accRangeSelection= (Spinner) view.findViewById(R.id.config_option_spinner);
        accRangeSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                rangeIndex = position;

                final YAxis leftAxis = chart.getAxisLeft();
                if (accelerometer instanceof AccelerometerBosch) {
                    leftAxis.setAxisMaxValue(BOSCH_RANGES[rangeIndex]);
                    leftAxis.setAxisMinValue(-BOSCH_RANGES[rangeIndex]);
                } else if (accelerometer instanceof AccelerometerMma8452q) {
                    leftAxis.setAxisMaxValue(MMA845Q_RANGES[rangeIndex]);
                    leftAxis.setAxisMinValue(-MMA845Q_RANGES[rangeIndex]);
                }

                refreshChart(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        fillRangeAdapter();
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException{
        accelerometer = mwBoard.getModuleOrThrow(Accelerometer.class);

        fillRangeAdapter();
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_acc_range, R.string.config_desc_acc_range));
    }

    @Override
    protected void setup() {
        Accelerometer.ConfigEditor<?> editor = accelerometer.configure();

        editor.odr(ACC_FREQ);
        if (accelerometer instanceof AccelerometerBosch) {
            editor.range(BOSCH_RANGES[rangeIndex]);
        } else if (accelerometer instanceof AccelerometerMma8452q) {
            editor.range(MMA845Q_RANGES[rangeIndex]);
        }
        editor.commit();

        samplePeriod= 1 / accelerometer.getOdr();

        final AsyncDataProducer producer = accelerometer.packedAcceleration() == null ?
                accelerometer.packedAcceleration() :
                accelerometer.acceleration();
        producer.addRouteAsync(source -> source.stream((data, env) -> {
            final Acceleration value = data.value(Acceleration.class);
            addChartData(value.x(), value.y(), value.z(), samplePeriod);
        })).continueWith(task -> {
            streamRoute = task.getResult();
            producer.start();
            accelerometer.start();

            return null;
        });
    }

    @Override
    protected void clean() {
        accelerometer.stop();

        (accelerometer.packedAcceleration() == null ?
                accelerometer.packedAcceleration() :
                accelerometer.acceleration()
        ).stop();
    }

    private void fillRangeAdapter() {
        ArrayAdapter<CharSequence> spinnerAdapter= null;
        if (accelerometer instanceof AccelerometerBosch) {
            spinnerAdapter= ArrayAdapter.createFromResource(getContext(), R.array.values_bmi160_acc_range, android.R.layout.simple_spinner_item);
        } else if (accelerometer instanceof AccelerometerMma8452q) {
            spinnerAdapter= ArrayAdapter.createFromResource(getContext(), R.array.values_mma8452q_acc_range, android.R.layout.simple_spinner_item);
        }

        if (spinnerAdapter != null) {
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            accRangeSelection.setAdapter(spinnerAdapter);
        }
    }
}
