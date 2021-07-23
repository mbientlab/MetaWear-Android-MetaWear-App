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
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Settings;

import bolts.Task;

/**
 * Created by etsai on 8/22/2015.
 */
public class SettingsFragment extends ModuleFragmentBase {
    private final static int[] CONFIG_WRAPPERS;

    static {
        CONFIG_WRAPPERS = new int[] {
                R.id.settings_ad_name_wrapper, R.id.settings_ad_interval_wrapper, R.id.settings_ad_timeout_wrapper, R.id.settings_tx_power_wrapper
        };
    }

    private Settings settingsModule;
    private Debug debugModule;

    private String deviceName;
    private int adInterval;
    private short timeout;
    private byte txPower;

    public SettingsFragment() {
        super(R.string.navigation_fragment_settings);
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        debugModule= mwBoard.getModuleOrThrow(Debug.class);
        settingsModule= mwBoard.getModuleOrThrow(Settings.class);

        settingsModule.readBleAdConfigAsync()
                .continueWith(task -> {
                    final int[] configEditText= new int[] {
                            R.id.settings_ad_name_value, R.id.settings_ad_interval_value, R.id.settings_ad_timeout_value, R.id.settings_tx_power_value
                    };
                    Object[] values= new Object[] {task.getResult().deviceName, task.getResult().interval, task.getResult().timeout, task.getResult().txPower};

                    for (int i= 0; i < values.length; i++) {
                        ((EditText) getView().findViewById(configEditText[i])).setText(values[i].toString());
                    }

                    return null;
                }, Task.UI_THREAD_EXECUTOR);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_ad_device_name, R.string.config_desc_ad_device_name));
        adapter.add(new HelpOption(R.string.config_name_ad_timeout, R.string.config_desc_ad_timeout));
        adapter.add(new HelpOption(R.string.config_name_ad_tx, R.string.config_desc_ad_tx));
        adapter.add(new HelpOption(R.string.config_name_ad_interval, R.string.config_desc_ad_interval));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View settingsControl= view.findViewById(R.id.settings_control);

        Button enableBtn= (Button) settingsControl.findViewById(R.id.layout_two_button_left);
        enableBtn.setText(R.string.label_save);
        enableBtn.setOnClickListener(innerView -> {
            TextInputLayout[] inputLayouts = new TextInputLayout[CONFIG_WRAPPERS.length];
            for (int i = 0; i < CONFIG_WRAPPERS.length; i++) {
                inputLayouts[i] = (TextInputLayout) view.findViewById(CONFIG_WRAPPERS[i]);
            }

            boolean valid = true;
            deviceName = ((EditText) view.findViewById(R.id.settings_ad_name_value)).getText().toString();

            try {
                adInterval = Integer.valueOf(((EditText) view.findViewById(R.id.settings_ad_interval_value)).getText().toString());
                inputLayouts[1].setError(null);
            } catch (Exception e) {
                valid = false;
                inputLayouts[1].setError(e.getLocalizedMessage());
            }

            try {
                timeout = Short.valueOf(((EditText) view.findViewById(R.id.settings_ad_timeout_value)).getText().toString());
                inputLayouts[2].setError(null);
            } catch (Exception e) {
                valid = false;
                inputLayouts[2].setError(e.getLocalizedMessage());
            }

            try {
                txPower = Byte.valueOf(((EditText) view.findViewById(R.id.settings_tx_power_value)).getText().toString());
                inputLayouts[3].setError(null);
            } catch (Exception e) {
                valid = false;
                inputLayouts[3].setError(e.getLocalizedMessage());
            }

            if (valid) {
                settingsModule.editBleAdConfig()
                        .deviceName(deviceName)
                        .timeout((byte) (timeout & 0xff))
                        .interval((short) (adInterval & 0xffff))
                        .txPower(txPower)
                        .commit();
            }
        });

        Button disableBtn= (Button) settingsControl.findViewById(R.id.layout_two_button_right);
        disableBtn.setText(R.string.label_clear);
        disableBtn.setOnClickListener(view1 -> debugModule.resetAfterGc());
    }
}
