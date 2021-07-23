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
import com.mbientlab.metawear.module.IBeacon;

import java.util.UUID;

import bolts.Task;

/**
 * Created by etsai on 8/22/2015.
 */
public class IBeaconFragment extends ModuleFragmentBase {
    private final static int[] CONFIG_WRAPPERS;

    static {
        CONFIG_WRAPPERS = new int[] {
                R.id.ibeacon_uuid_wrapper, R.id.ibeacon_major_wrapper, R.id.ibeacon_minor_wrapper, R.id.ibeacon_rx_power_wrapper,
                R.id.ibeacon_tx_power_wrapper, R.id.ibeacon_period_wrapper
        };
    }

    private IBeacon ibeaconModule;

    private UUID uuid;
    private short major, minor, period;
    private byte rxPower, txPower;

    public IBeaconFragment() {
        super(R.string.navigation_fragment_ibeacon);
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        ibeaconModule= mwBoard.getModuleOrThrow(IBeacon.class);

        ibeaconModule.readConfigAsync().continueWith(task -> {
            final int[] configEditText= new int[] {
                    R.id.ibeacon_uuid_value, R.id.ibeacon_major_value, R.id.ibeacon_minor_value, R.id.ibeacon_rx_power_value,
                    R.id.ibeacon_tx_power_value, R.id.ibeacon_period_value
            };
            Object[] values= new Object[] {task.getResult().uuid, task.getResult().major, task.getResult().minor,
                    task.getResult().rxPower, task.getResult().txPower, task.getResult().period
            };
            for (int i= 0; i < values.length; i++) {
                ((EditText) getView().findViewById(configEditText[i])).setText(values[i].toString());
            }

            return null;
        }, Task.UI_THREAD_EXECUTOR);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_ibeacon_uuid, R.string.config_desc_ibeacon_uuid));
        adapter.add(new HelpOption(R.string.config_name_ibeacon_major, R.string.config_desc_ibeacon_major));
        adapter.add(new HelpOption(R.string.config_name_ibeacon_minor, R.string.config_desc_ibeacon_minor));
        adapter.add(new HelpOption(R.string.config_name_ibeacon_rx, R.string.config_desc_ibeacon_rx));
        adapter.add(new HelpOption(R.string.config_name_ibeacon_tx, R.string.config_desc_ibeacon_tx));
        adapter.add(new HelpOption(R.string.config_name_ibeacon_period, R.string.config_desc_ibeacon_period));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_ibeacon, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View iBeaconControl= view.findViewById(R.id.ibeacon_control);

        Button enableBtn= (Button) iBeaconControl.findViewById(R.id.layout_two_button_left);
        enableBtn.setText(R.string.label_enable);
        iBeaconControl.findViewById(R.id.layout_two_button_left).setOnClickListener(innerView -> {
            TextInputLayout[] inputLayouts = new TextInputLayout[CONFIG_WRAPPERS.length];
            for (int i = 0; i < CONFIG_WRAPPERS.length; i++) {
                inputLayouts[i] = (TextInputLayout) view.findViewById(CONFIG_WRAPPERS[i]);
            }

            boolean valid = true;

            try {
                uuid = UUID.fromString(((EditText) view.findViewById(R.id.ibeacon_uuid_value)).getText().toString());
                inputLayouts[0].setError(null);
            } catch (Exception e) {
                valid = false;
                inputLayouts[0].setError(e.getLocalizedMessage());
            }

            try {
                major = Short.valueOf(((EditText) view.findViewById(R.id.ibeacon_major_value)).getText().toString());
                inputLayouts[1].setError(null);
            } catch (Exception e) {
                valid = false;
                inputLayouts[1].setError(e.getLocalizedMessage());
            }

            try {
                minor = Short.valueOf(((EditText) view.findViewById(R.id.ibeacon_minor_value)).getText().toString());
                inputLayouts[2].setError(null);
            } catch (Exception e) {
                valid = false;
                inputLayouts[2].setError(e.getLocalizedMessage());
            }

            try {
                rxPower = Byte.valueOf(((EditText) view.findViewById(R.id.ibeacon_rx_power_value)).getText().toString());
                inputLayouts[3].setError(null);
            } catch (Exception e) {
                valid = false;
                inputLayouts[3].setError(e.getLocalizedMessage());
            }

            try {
                txPower = Byte.valueOf(((EditText) view.findViewById(R.id.ibeacon_tx_power_value)).getText().toString());
                inputLayouts[4].setError(null);
            } catch (Exception e) {
                valid = false;
                inputLayouts[4].setError(e.getLocalizedMessage());
            }

            try {
                period = Short.valueOf(((EditText) view.findViewById(R.id.ibeacon_period_value)).getText().toString());
                inputLayouts[5].setError(null);
            } catch (Exception e) {
                valid = false;
                inputLayouts[5].setError(e.getLocalizedMessage());
            }

            if (valid) {
                ibeaconModule.configure()
                        .uuid(uuid)
                        .major(major)
                        .minor(minor)
                        .period(period)
                        .rxPower(rxPower)
                        .txPower(txPower)
                        .commit();
                ibeaconModule.enable();
            }
        });

        Button disableBtn= (Button) iBeaconControl.findViewById(R.id.layout_two_button_right);
        disableBtn.setText(R.string.label_disable);
        iBeaconControl.findViewById(R.id.layout_two_button_right).setOnClickListener(view1 -> ibeaconModule.disable());
    }
}
