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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOption;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.NeoPixel;
import com.mbientlab.metawear.module.NeoPixel.*;

import java.util.Locale;

/**
 * Created by etsai on 8/23/2015.
 */
public class NeoPixelFragment extends ModuleFragmentBase {
    private interface PatternProgrammer {
        void program();
    }

    private NeoPixel neoPixelModule;
    private NeoPixel.Strand[] strand = new NeoPixel.Strand[3];

    private PatternProgrammer[] programmer= new PatternProgrammer[] {
            new PatternProgrammer() {
                @Override
                public void program() {
                    for(byte i= 0; i < nLeds; i++) {
                        strand[npStrand].setRgb(i, (byte)0, (byte)-1, (byte)0);
                    }
                }
            },
            new PatternProgrammer() {
                @Override
                public void program() {
                    double delta= 2 * Math.PI / nLeds;

                    for(byte i= 0; i < nLeds; i++) {
                        double step= i * delta;
                        double rRatio= Math.cos(step),
                                gRatio= Math.cos(step + 2*Math.PI/3),
                                bRatio= Math.cos(step + 4*Math.PI/3);
                        strand[npStrand].setRgb(i, (byte)((rRatio < 0 ? 0 : rRatio) * 255),
                                (byte)((gRatio < 0 ? 0 : gRatio) * 255),
                                (byte)((bRatio < 0 ? 0 : bRatio) * 255));
                    }
                }
            }
    };
    private byte npStrand= 0, dataPin= 0, nLeds= 0;
    private int orderingIndex= 0, directionIndex= 0, patternIndex= 0, speedIndex= 0;
    private short period;

    public NeoPixelFragment() {
        super(R.string.navigation_fragment_neopixel);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_neopixel, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Spinner strandSpeeds= (Spinner) view.findViewById(R.id.neopixel_strand_speed);
        strandSpeeds.setSelection(speedIndex);
        strandSpeeds.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                speedIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Spinner rotDirection= (Spinner) view.findViewById(R.id.neopixel_rot_direction);
        rotDirection.setSelection(directionIndex);
        rotDirection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                directionIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Spinner colorOrdering= (Spinner) view.findViewById(R.id.neopixel_color_ordering);
        colorOrdering.setSelection(orderingIndex);
        colorOrdering.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                orderingIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Spinner neopixelPattern= (Spinner) view.findViewById(R.id.neopixel_pattern);
        neopixelPattern.setSelection(patternIndex);
        neopixelPattern.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                patternIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        final EditText npStrandText= (EditText) view.findViewById(R.id.neopixel_strand_value);
        npStrandText.setText(String.format(Locale.US, "%d", npStrand));

        final EditText dataPinText= (EditText) view.findViewById(R.id.neopixel_data_pin_value);
        dataPinText.setText(String.format(Locale.US, "%d", dataPin));

        final EditText nLedText= (EditText) view.findViewById(R.id.neopixel_nleds_value);
        nLedText.setText(String.format(Locale.US, "%d", nLeds));

        final EditText rotPeriodText= (EditText) view.findViewById(R.id.neopixel_rot_period_value);
        rotPeriodText.setText(String.format(Locale.US, "%d", period));

        final TextInputLayout npStrandWrapper = (TextInputLayout) view.findViewById(R.id.neopixel_strand_wrapper);

        View setupView= view.findViewById(R.id.neopixel_setup);
        Button initBtn= (Button) setupView.findViewById(R.id.layout_two_button_left);
        initBtn.setText(R.string.label_neopixel_initialize);
        initBtn.setOnClickListener(v -> {
            boolean valid = true;

            try {
                npStrand = Byte.valueOf(npStrandText.getText().toString());
                npStrandWrapper.setError(null);
            } catch (Exception e) {
                npStrandWrapper.setError(e.getLocalizedMessage());
                valid = false;
            }

            TextInputLayout dataPinWrapper = (TextInputLayout) view.findViewById(R.id.neopixel_data_pin_wrapper);
            try {
                dataPin = Byte.valueOf(dataPinText.getText().toString());
                dataPinWrapper.setError(null);
            } catch (Exception e) {
                dataPinWrapper.setError(e.getLocalizedMessage());
                valid = false;
            }

            TextInputLayout nLedWrapper = (TextInputLayout) view.findViewById(R.id.neopixel_nleds_wrapper);
            try {
                nLeds = Byte.valueOf(nLedText.getText().toString());
                nLedWrapper.setError(null);
            } catch (Exception e) {
                nLedWrapper.setError(e.getLocalizedMessage());
                valid = false;
            }

            if (valid) {
                strand[npStrand] = neoPixelModule.initializeStrand(npStrand, ColorOrdering.values()[orderingIndex], StrandSpeed.values()[speedIndex], dataPin, nLeds);
            }
        });
        Button freeBtn= (Button) setupView.findViewById(R.id.layout_two_button_right);
        freeBtn.setText(R.string.label_neopixel_free);
        freeBtn.setOnClickListener(v -> {
            try {
                npStrand = Byte.valueOf(npStrandText.getText().toString());
                strand[npStrand].free();
                npStrandWrapper.setError(null);
            } catch (Exception e) {
                npStrandWrapper.setError(e.getLocalizedMessage());
            }
        });

        View pixelView= view.findViewById(R.id.neopixel_pixel);
        Button setBtn= (Button) pixelView.findViewById(R.id.layout_two_button_left);
        setBtn.setText(R.string.label_gpio_set_output);
        setBtn.setOnClickListener(v -> {
            try {
                npStrand = Byte.valueOf(npStrandText.getText().toString());
                npStrandWrapper.setError(null);

                strand[npStrand].hold();
                programmer[patternIndex].program();
                strand[npStrand].release();
            } catch (Exception e) {
                npStrandWrapper.setError(e.getLocalizedMessage());
            }
        });
        Button clearBtn= (Button) pixelView.findViewById(R.id.layout_two_button_right);
        clearBtn.setText(R.string.label_gpio_clear_output);
        clearBtn.setOnClickListener(v -> {
            boolean valid = true;

            try {
                npStrand = Byte.valueOf(npStrandText.getText().toString());
                npStrandWrapper.setError(null);
            } catch (Exception e) {
                npStrandWrapper.setError(e.getLocalizedMessage());
                valid = false;
            }

            TextInputLayout nLedWrapper = (TextInputLayout) view.findViewById(R.id.neopixel_nleds_wrapper);
            try {
                nLeds = Byte.valueOf(nLedText.getText().toString());
                nLedWrapper.setError(null);
            } catch (Exception e) {
                nLedWrapper.setError(e.getLocalizedMessage());
                valid = false;
            }

            if (valid) {
                strand[npStrand].clear((byte) 0, (byte) (nLeds - 1));
            }
        });

        View rotateView= view.findViewById(R.id.neopixel_rotate);
        Button rotateBtn= (Button) rotateView.findViewById(R.id.layout_two_button_left);
        rotateBtn.setText(R.string.label_neopixel_rotate);
        rotateBtn.setOnClickListener(v -> {
            boolean valid = true;

            try {
                npStrand = Byte.valueOf(npStrandText.getText().toString());
                npStrandWrapper.setError(null);
            } catch (Exception e) {
                npStrandWrapper.setError(e.getLocalizedMessage());
                valid = false;
            }

            TextInputLayout periodWrapper = (TextInputLayout) view.findViewById(R.id.neopixel_rot_period_wrapper);
            try {
                period = Short.valueOf(rotPeriodText.getText().toString());
                periodWrapper.setError(null);
            } catch (Exception e) {
                periodWrapper.setError(e.getLocalizedMessage());
                valid = false;
            }

            if (valid) {
                strand[npStrand].rotate(Strand.RotationDirection.values()[directionIndex], period);
            }
        });
        Button stopBtn= (Button) rotateView.findViewById(R.id.layout_two_button_right);
        stopBtn.setText(R.string.label_neopixel_stop);
        stopBtn.setOnClickListener(v -> {
            try {
                npStrand = Byte.valueOf(npStrandText.getText().toString());
                npStrandWrapper.setError(null);
                strand[npStrand].stopRotation();
            } catch (Exception e) {
                npStrandWrapper.setError(e.getLocalizedMessage());
            }
        });
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException{
        neoPixelModule= mwBoard.getModuleOrThrow(NeoPixel.class);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_neopixel_strand, R.string.config_desc_neopixel_strand));
        adapter.add(new HelpOption(R.string.config_name_neopixel_data_pin, R.string.config_desc_neopixel_data_pin));
        adapter.add(new HelpOption(R.string.config_name_neopixel_n_leds, R.string.config_desc_neopixel_n_leds));
        adapter.add(new HelpOption(R.string.config_name_neopixel_strand_speed, R.string.config_desc_neopixel_strand_speed));
        adapter.add(new HelpOption(R.string.config_name_neopixel_color_ordering, R.string.config_desc_neopixel_color_ordering));

        adapter.add(new HelpOption(R.string.config_name_neopixel_color_pattern, R.string.config_desc_neopixel_color_pattern));

        adapter.add(new HelpOption(R.string.config_name_neopixel_rotation_direction, R.string.config_desc_neopixel_rotation_direction));
        adapter.add(new HelpOption(R.string.config_name_neopixel_rotation_period, R.string.config_desc_neopixel_rotation_period));
    }
}
