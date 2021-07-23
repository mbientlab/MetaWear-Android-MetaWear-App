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

import android.app.Dialog;
import android.os.Bundle;
//import android.support.annotation.NonNull;
import androidx.annotation.NonNull;
//import android.support.design.widget.Snackbar;
import com.google.android.material.snackbar.Snackbar;
//import androidx.core.app.DialogFragment;
import androidx.fragment.app.DialogFragment;
//import android.support.v7.app.AlertDialog;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Led.Color;
import com.mbientlab.metawear.module.Switch;

import java.util.Locale;

import bolts.Task;

/**
 * Created by etsai on 8/22/2015.
 */
public class HomeFragment extends ModuleFragmentBase {
    private Led ledModule;
    private int switchRouteId = -1;

    public static class MetaBootWarningFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.title_warning)
                    .setPositiveButton(R.string.label_ok, null)
                    .setCancelable(false)
                    .setMessage(R.string.message_metaboot)
                    .create();
        }
    }

    public HomeFragment() {
        super(R.string.navigation_fragment_home);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.led_red_on).setOnClickListener(view1 -> {
            configureChannel(ledModule.editPattern(Color.RED));
            ledModule.play();
        });
        view.findViewById(R.id.led_green_on).setOnClickListener(view12 -> {
            configureChannel(ledModule.editPattern(Color.GREEN));
            ledModule.play();
        });
        view.findViewById(R.id.led_blue_on).setOnClickListener(view13 -> {
            configureChannel(ledModule.editPattern(Color.BLUE));
            ledModule.play();
        });
        view.findViewById(R.id.led_stop).setOnClickListener(view14 -> ledModule.stop(true));
        view.findViewById(R.id.board_rssi_text).setOnClickListener(v -> mwBoard.readRssiAsync()
                .continueWith(task -> {
                    ((TextView) view.findViewById(R.id.board_rssi_value)).setText(String.format(Locale.US, "%d dBm", task.getResult()));
                    return null;
                }, Task.UI_THREAD_EXECUTOR)
        );
        view.findViewById(R.id.board_battery_level_text).setOnClickListener(v -> mwBoard.readBatteryLevelAsync()
                .continueWith(task -> {
                    ((TextView) view.findViewById(R.id.board_battery_level_value)).setText(String.format(Locale.US, "%d", task.getResult()));
                    return null;
                }, Task.UI_THREAD_EXECUTOR)
        );
        view.findViewById(R.id.update_firmware).setOnClickListener(view15 -> mwBoard.checkForFirmwareUpdateAsync()
                .continueWith(task -> {
                    if (task.isFaulted()) {
                        Snackbar.make(getActivity().findViewById(R.id.drawer_layout), task.getError().getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
                    } else {
                        setupDfuDialog(new AlertDialog.Builder(getActivity()), task.getResult() ? R.string.message_dfu_accept : R.string.message_dfu_latest);
                    }
                    return null;
                }, Task.UI_THREAD_EXECUTOR)
        );
    }

    private void setupDfuDialog(AlertDialog.Builder builder, int msgResId) {
        builder.setTitle(R.string.title_firmware_update)
                .setPositiveButton(R.string.label_yes, (dialogInterface, i) -> fragBus.initiateDfu(null))
                .setNegativeButton(R.string.label_no, null)
                .setCancelable(false)
                .setMessage(msgResId)
                .show();
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        setupFragment(getView());
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {

    }

    @Override
    public void reconnected() {
        setupFragment(getView());
    }

    private void configureChannel(Led.PatternEditor editor) {
        final short PULSE_WIDTH= 1000;
        editor.highIntensity((byte) 31).lowIntensity((byte) 31)
                .highTime((short) (PULSE_WIDTH >> 1)).pulseDuration(PULSE_WIDTH)
                .repeatCount((byte) -1).commit();
    }

    private void setupFragment(final View v) {
        final String METABOOT_WARNING_TAG= "metaboot_warning_tag";

        if (!mwBoard.isConnected()) {
            return;
        }

        if (mwBoard.inMetaBootMode()) {
            if (getFragmentManager().findFragmentByTag(METABOOT_WARNING_TAG) == null) {
                new MetaBootWarningFragment().show(getFragmentManager(), METABOOT_WARNING_TAG);
            }
        } else {
            DialogFragment metabootWarning= (DialogFragment) getFragmentManager().findFragmentByTag(METABOOT_WARNING_TAG);
            if (metabootWarning != null) {
                metabootWarning.dismiss();
            }
        }

        mwBoard.readDeviceInformationAsync().continueWith(task -> {
            if (task.getResult() != null) {
                ((TextView) v.findViewById(R.id.manufacturer_value)).setText(task.getResult().manufacturer);
                ((TextView) v.findViewById(R.id.model_number_value)).setText(task.getResult().modelNumber);
                ((TextView) v.findViewById(R.id.serial_number_value)).setText(task.getResult().serialNumber);
                ((TextView) v.findViewById(R.id.firmware_revision_value)).setText(task.getResult().firmwareRevision);
                ((TextView) v.findViewById(R.id.hardware_revision_value)).setText(task.getResult().hardwareRevision);
                ((TextView) v.findViewById(R.id.device_mac_address_value)).setText(mwBoard.getMacAddress());
            }

            return null;
        }, Task.UI_THREAD_EXECUTOR);

        Switch switchModule;
        if ((switchModule= mwBoard.getModule(Switch.class)) != null) {
            Route oldSwitchRoute;
            if ((oldSwitchRoute = mwBoard.lookupRoute(switchRouteId)) != null) {
                oldSwitchRoute.remove();
            }

            switchModule.state().addRouteAsync(source ->
                    source.stream((data, env) -> getActivity().runOnUiThread(() -> {
                        RadioGroup radioGroup = (RadioGroup) v.findViewById(R.id.switch_radio_group);

                        if (data.value(Boolean.class)) {
                            radioGroup.check(R.id.switch_radio_pressed);
                            v.findViewById(R.id.switch_radio_pressed).setEnabled(true);
                            v.findViewById(R.id.switch_radio_released).setEnabled(false);
                        } else {
                            radioGroup.check(R.id.switch_radio_released);
                            v.findViewById(R.id.switch_radio_released).setEnabled(true);
                            v.findViewById(R.id.switch_radio_pressed).setEnabled(false);
                        }
                    }))
            ).continueWith(task -> switchRouteId = task.getResult().id());
        }

        int[] ledResIds= new int[] {R.id.led_stop, R.id.led_red_on, R.id.led_green_on, R.id.led_blue_on};
        if ((ledModule = mwBoard.getModule(Led.class)) != null) {
            for(int id: ledResIds) {
                v.findViewById(id).setEnabled(true);
            }
        } else {
            for(int id: ledResIds) {
                v.findViewById(id).setEnabled(false);
            }
        }
    }
}
