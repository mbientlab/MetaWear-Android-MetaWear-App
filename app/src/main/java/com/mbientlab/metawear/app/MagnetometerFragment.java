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

import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.data.MagneticField;
import com.mbientlab.metawear.module.MagnetometerBmm150;

/**
 * Created by etsai on 1/12/2016.
 */
public class MagnetometerFragment extends ThreeAxisChartFragment {
    private static final float B_FIELD_RANGE= 2500f, MAG_ODR= 25.f;

    private MagnetometerBmm150 magnetometer = null;

    public MagnetometerFragment() {
        super("field", R.layout.fragment_sensor,R.string.navigation_fragment_magnetometer, -B_FIELD_RANGE, B_FIELD_RANGE, MAG_ODR);
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        magnetometer = mwBoard.getModuleOrThrow(MagnetometerBmm150.class);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {

    }

    @Override
    protected void setup() {
        magnetometer.configure()
                .outputDataRate(MagnetometerBmm150.OutputDataRate.ODR_25_HZ)
                .commit();

        final float period = 1 / MAG_ODR;
        final AsyncDataProducer producer = magnetometer.packedMagneticField() == null ?
                magnetometer.packedMagneticField() :
                magnetometer.magneticField();
        producer.addRouteAsync(source -> source.stream((data, env) -> {
            final MagneticField value = data.value(MagneticField.class);
            addChartData(value.x() * 1000000f, value.y() * 1000000f, value.z() * 1000000f, period);
        })).continueWith(task -> {
            streamRoute = task.getResult();

            magnetometer.magneticField().start();
            magnetometer.start();

            return null;
        });
    }

    @Override
    protected void clean() {
        magnetometer.stop();
        (magnetometer.packedMagneticField() == null ?
                magnetometer.packedMagneticField() :
                magnetometer.magneticField()
        ).stop();
    }
}
