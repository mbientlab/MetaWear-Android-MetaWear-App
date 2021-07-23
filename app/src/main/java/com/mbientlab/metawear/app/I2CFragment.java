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
//import android.support.design.widget.Snackbar;
import com.google.android.material.snackbar.Snackbar;
//import android.support.design.widget.TextInputLayout;
import com.google.android.material.textfield.TextInputLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOption;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.SerialPassthrough;

import bolts.Task;

/**
 * Created by etsai on 3/1/2016.
 */
public class I2CFragment extends ModuleFragmentBase {
    public static String arrayToHexString(byte[] value) {
        if (value.length == 0) {
            return "";
        }

        StringBuilder builder= new StringBuilder();
        for(byte it: value) {
            builder.append(String.format("%02x", it));
        }

        return builder.toString();
    }

    public static byte[] hexStringToArray(String byteArrayString) {
        if (byteArrayString.isEmpty()) {
            return new byte[] { };
        }

        if (byteArrayString.length() % 2 != 0) {
            byteArrayString= String.format("0%s", byteArrayString);
        }

        byte[] bytes= new byte[byteArrayString.length() / 2];
        for(int i= 0, j= 0; i < byteArrayString.length(); i+= 2, j++) {
            bytes[j]= (byte) Short.parseShort(byteArrayString.substring(i, i + 2), 16);
        }

        return bytes;
    }

    private SerialPassthrough serial;

    public I2CFragment() {
        super(R.string.navigation_fragment_i2c);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        View v= inflater.inflate(R.layout.fragment_i2c, container, false);

        final EditText deviceAddrText= (EditText) v.findViewById(R.id.i2c_device_address_value),
                registerAddrText= (EditText) v.findViewById(R.id.config_name_i2c_register_value),
                nBytesText= (EditText) v.findViewById(R.id.config_name_i2c_n_bytes_value),
                i2cValueText= (EditText) v.findViewById(R.id.config_name_i2c_value);

        final TextInputLayout deviceAddrWrapper= (TextInputLayout) v.findViewById(R.id.i2c_device_address_value_wrapper),
                registerAddrWrapper= (TextInputLayout) v.findViewById(R.id.config_name_i2c_register_wrapper),
                nBytesWrapper= (TextInputLayout) v.findViewById(R.id.config_name_i2c_n_bytes_value_wrapper),
                i2cValueWrapper= (TextInputLayout) v.findViewById(R.id.config_name_i2c_value_wrapper);

        Button readBtn= (Button) v.findViewById(R.id.layout_two_button_left);
        readBtn.setText(R.string.label_read);
        readBtn.setOnClickListener(v12 -> {
            boolean valid = true;
            byte deviceAddr= 0, registerAddr= 0, nBytes= 0;

            try {
                deviceAddr= (byte) Short.parseShort(deviceAddrText.getEditableText().toString(), 16);
                deviceAddrWrapper.setError(null);
            } catch (NumberFormatException e) {
                valid= false;
                deviceAddrWrapper.setError(e.getLocalizedMessage());
            }

            try {
                registerAddr= (byte) Short.parseShort(registerAddrText.getEditableText().toString(), 16);
                registerAddrWrapper.setError(null);
            } catch (NumberFormatException e) {
                valid= false;
                registerAddrWrapper.setError(e.getLocalizedMessage());
            }

            try {
                nBytes= Byte.parseByte(nBytesText.getEditableText().toString());
                nBytesWrapper.setError(null);
            } catch (NumberFormatException e) {
                valid= false;
                nBytesWrapper.setError(e.getLocalizedMessage());
            }

            if (valid) {
                serial.readI2cAsync(deviceAddr, registerAddr, nBytes)
                        .continueWith(task -> {
                            if (task.isFaulted()) {
                                Snackbar.make(v12, task.getError().getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
                                Log.w("MetaWearApp", "Could not read I2C data", task.getError());
                            } else {
                                i2cValueText.setText(arrayToHexString(task.getResult()));
                            }
                            return null;
                        }, Task.UI_THREAD_EXECUTOR);
            }
        });

        Button writeBtn= (Button) v.findViewById(R.id.layout_two_button_right);
        writeBtn.setText(R.string.label_write);
        writeBtn.setOnClickListener(v1 -> {
            boolean valid = true;
            byte deviceAddr= 0, registerAddr= 0;
            byte[] i2cValue= null;

            try {
                deviceAddr= (byte) Short.parseShort(deviceAddrText.getEditableText().toString(), 16);
                deviceAddrWrapper.setError(null);
            } catch (NumberFormatException e) {
                valid= false;
                deviceAddrWrapper.setError(e.getLocalizedMessage());
            }

            try {
                registerAddr= (byte) Short.parseShort(registerAddrText.getEditableText().toString(), 16);
                registerAddrWrapper.setError(null);
            } catch (NumberFormatException e) {
                valid= false;
                registerAddrWrapper.setError(e.getLocalizedMessage());
            }

            try {
                i2cValue= hexStringToArray(i2cValueText.getEditableText().toString());
                i2cValueWrapper.setError(null);
            } catch (NumberFormatException e) {
                valid= false;
                i2cValueWrapper.setError(e.getLocalizedMessage());
            }

            if (valid) {
                serial.writeI2c(deviceAddr, registerAddr, i2cValue);
            }
        });

        return v;
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        serial= mwBoard.getModuleOrThrow(SerialPassthrough.class);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_i2c_device, R.string.config_desc_i2c_device));
        adapter.add(new HelpOption(R.string.config_name_i2c_register, R.string.config_desc_i2c_register));
        adapter.add(new HelpOption(R.string.config_name_i2c_n_bytes, R.string.config_desc_i2c_n_bytes));
        adapter.add(new HelpOption(R.string.config_name_i2c_value, R.string.config_desc_i2c_value));
    }
}
