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
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.Gyro.Range;
import com.mbientlab.metawear.module.Gyro.OutputDataRate;
//import com.mbientlab.metawear.module.GyroBmi160;
//import com.mbientlab.metawear.module.GyroBmi270;

/**
 * Created by etsai on 8/19/2015.
 */
public class GyroFragment extends ThreeAxisChartFragment {
    private static final float[] AVAILABLE_RANGES= {125.f, 250.f, 500.f, 1000.f, 2000.f};
    private static final float INITIAL_RANGE= 125.f, GYR_ODR= 25.f;

    private Gyro gyro = null;
    private int rangeIndex= 0;

    public GyroFragment() {
        super("rotation", R.layout.fragment_sensor_config_spinner,
                R.string.navigation_fragment_gyro, -INITIAL_RANGE, INITIAL_RANGE, GYR_ODR);
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        gyro = mwBoard.getModuleOrThrow(Gyro.class);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_gyro_range, R.string.config_desc_gyro_range));
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final YAxis leftAxis = chart.getAxisLeft();

        ((TextView) view.findViewById(R.id.config_option_title)).setText(R.string.config_name_gyro_range);

        Spinner rotationRangeSelection= (Spinner) view.findViewById(R.id.config_option_spinner);
        rotationRangeSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                rangeIndex = position;
                leftAxis.setAxisMaxValue(AVAILABLE_RANGES[rangeIndex]);
                leftAxis.setAxisMinValue(-AVAILABLE_RANGES[rangeIndex]);

                refreshChart(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ArrayAdapter<CharSequence> spinnerAdapter= ArrayAdapter.createFromResource(getContext(), R.array.values_gyro_range, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rotationRangeSelection.setAdapter(spinnerAdapter);
        rotationRangeSelection.setSelection(rangeIndex);
    }

    @Override
    protected void setup() {
        Range[] values = Range.values();
        gyro.configure()
                .odr(OutputDataRate.ODR_25_HZ)
                .range(values[values.length - rangeIndex - 1])
                .commit();

        final float period = 1 / GYR_ODR;
        final AsyncDataProducer producer = gyro.packedAngularVelocity() == null ?
                gyro.packedAngularVelocity() :
                gyro.angularVelocity();
        producer.addRouteAsync(source -> source.stream((data, env) -> {
            final AngularVelocity value = data.value(AngularVelocity.class);
            addChartData(value.x(), value.y(), value.z(), period);
            updateChart();
        })).continueWith(task -> {
            streamRoute = task.getResult();

            gyro.angularVelocity().start();
            gyro.start();

            return null;
        });
    }

    @Override
    protected void clean() {
        gyro.stop();

        (gyro.packedAngularVelocity() == null ?
                gyro.packedAngularVelocity() :
                gyro.angularVelocity()
        ).stop();
    }
}
