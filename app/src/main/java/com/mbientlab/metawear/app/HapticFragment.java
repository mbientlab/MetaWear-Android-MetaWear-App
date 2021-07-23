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
//import android.support.design.widget.TextInputLayout;
import com.google.android.material.textfield.TextInputLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOption;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.Haptic;

/**
 * Created by etsai on 8/24/2015.
 */
public class HapticFragment extends ModuleFragmentBase {
    private Haptic hapticModule;

    public HapticFragment() {
        super(R.string.navigation_fragment_haptic);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        View v= inflater.inflate(R.layout.fragment_haptic, container, false);

        final EditText dutyCycleText= (EditText) v.findViewById(R.id.duty_cycle_value),
                pulseWidthText= (EditText) v.findViewById(R.id.pulse_width_value);

        final TextInputLayout dutyCycleWrapper= (TextInputLayout) v.findViewById(R.id.duty_cycle_value_wrapper),
                pulseWidthWrapper= (TextInputLayout) v.findViewById(R.id.pulse_width_value_wrapper);

        Button startMotorBtn= (Button) v.findViewById(R.id.layout_two_button_left);
        startMotorBtn.setText(R.string.label_haptic_start_motor);
        startMotorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float dutyCycle = 0;
                short pulseWidth = 0;
                boolean valid = true;

                try {
                    dutyCycle = Float.valueOf(dutyCycleText.getText().toString());
                    dutyCycleWrapper.setError(null);
                } catch (Exception e) {
                    valid = false;
                    dutyCycleWrapper.setError(e.getLocalizedMessage());
                }

                try {
                    pulseWidth = Short.valueOf(pulseWidthText.getText().toString());
                    pulseWidthWrapper.setError(null);
                } catch (Exception e) {
                    valid = false;
                    pulseWidthWrapper.setError(e.getLocalizedMessage());
                }

                if (valid) {
                    dutyCycleText.setError(null);
                    pulseWidthText.setError(null);
                    hapticModule.startMotor(dutyCycle, pulseWidth);
                }
            }
        });

        Button startBuzzerBtn= (Button) v.findViewById(R.id.layout_two_button_right);
        startBuzzerBtn.setText(R.string.label_haptic_start_buzzer);
        startBuzzerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    hapticModule.startBuzzer(Short.valueOf(pulseWidthText.getText().toString()));
                } catch (Exception e) {
                    pulseWidthText.setError(e.getLocalizedMessage());
                }
            }
        });

        return v;
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        hapticModule= mwBoard.getModuleOrThrow(Haptic.class);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_haptic_duty_cycle, R.string.config_desc_haptic_duty_cycle));
        adapter.add(new HelpOption(R.string.config_name_haptic_pulse_width, R.string.config_desc_haptic_pulse_width));
    }
}
