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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOption;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.module.Gpio.PullMode;
import com.mbientlab.metawear.module.Timer;

import java.util.Locale;

/**
 * Created by etsai on 8/21/2015.
 */
public class GpioFragment extends SingleDataSensorFragment {
    private static final byte READ_ADC= 0, READ_ABS_REF= 1, READ_DIGITAL= 2, DEFAULT_GPIO_PIN= 0;
    private static final int GPIO_SAMPLE_PERIOD= 33;
    private static final int[] CONTROL_RES_IDS= {
            R.id.sample_control,
            R.id.gpio_digital_up, R.id.gpio_digital_down, R.id.gpio_digital_none,
            R.id.gpio_output_set, R.id.gpio_output_clear
    };

    private byte gpioPin= DEFAULT_GPIO_PIN;
    private int readMode= 0;
    private Gpio gpio;
    private Timer timer;
    private Timer.ScheduledTask scheduledTask;
    private long startTime= -1;

    private final Subscriber gpioSubscriber = (data, env) -> {
        final Short gpioValue = data.value(Short.class);

        LineData chartData = chart.getData();
        if (startTime == -1) {
            chartData.addXValue("0");
            startTime= System.currentTimeMillis();
        } else {
            chartData.addXValue(String.format(Locale.US, "%.2f", sampleCount * samplingPeriod));
        }

        chartData.addEntry(new Entry(gpioValue, sampleCount), 0);

        sampleCount++;

        updateChart();
    };

    public GpioFragment() {
        super(R.string.navigation_fragment_gpio, "adc", R.layout.fragment_gpio, GPIO_SAMPLE_PERIOD / 1000.f, 0, 1023);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Spinner accRangeSelection= (Spinner) view.findViewById(R.id.gpio_read_mode);
        accRangeSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                readMode = position;
                YAxis leftAxis = chart.getAxisLeft();

                switch (readMode) {
                    case READ_ADC:
                        max = 1023;
                        leftAxis.setAxisMaxValue(max);
                        csvHeaderDataName = "adc";
                        break;
                    case READ_ABS_REF:
                        max = 3.f;
                        leftAxis.setAxisMaxValue(max);
                        csvHeaderDataName = "abs reference";
                        break;
                    case READ_DIGITAL:
                        max = 1;
                        leftAxis.setAxisMaxValue(max);
                        csvHeaderDataName = "digital";
                        break;
                }

                leftAxis.setAxisMaxValue(max);
                refreshChart(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ArrayAdapter<CharSequence> spinnerAdapter= ArrayAdapter.createFromResource(getContext(), R.array.values_gpio_read_mode, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accRangeSelection.setAdapter(spinnerAdapter);
        accRangeSelection.setSelection(readMode);

        EditText gpioPinText= (EditText) view.findViewById(R.id.gpio_pin_value);
        gpioPinText.setText(String.format(Locale.US, "%d", gpioPin));
        gpioPinText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final TextInputLayout gpioTextWrapper = (TextInputLayout) view.findViewById(R.id.gpio_pin_wrapper);

                try {
                    gpioPin = Byte.valueOf(s.toString());
                    gpioTextWrapper.setError(null);
                    for (int id : CONTROL_RES_IDS) {
                        view.findViewById(id).setEnabled(true);
                    }
                } catch (Exception e) {
                    gpioTextWrapper.setError(e.getLocalizedMessage());
                    for (int id : CONTROL_RES_IDS) {
                        view.findViewById(id).setEnabled(false);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        view.findViewById(R.id.gpio_digital_up).setOnClickListener(v -> gpio.pin(gpioPin).setPullMode(PullMode.PULL_UP));
        view.findViewById(R.id.gpio_digital_down).setOnClickListener(v -> gpio.pin(gpioPin).setPullMode(PullMode.PULL_DOWN));
        view.findViewById(R.id.gpio_digital_none).setOnClickListener(v -> gpio.pin(gpioPin).setPullMode(PullMode.NO_PULL));

        Button setDoBtn= (Button) view.findViewById(R.id.gpio_output_set);
        setDoBtn.setText(R.string.value_gpio_output_set);
        setDoBtn.setOnClickListener(v -> gpio.pin(gpioPin).setOutput());

        Button clearDoBtn= (Button) view.findViewById(R.id.gpio_output_clear);
        clearDoBtn.setText(R.string.value_gpio_output_clear);
        clearDoBtn.setOnClickListener(v -> gpio.pin(gpioPin).clearOutput());
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        gpio = mwBoard.getModuleOrThrow(Gpio.class);
        timer = mwBoard.getModuleOrThrow(Timer.class);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_gpio_pin, R.string.config_desc_gpio_pin));
        adapter.add(new HelpOption(R.string.config_name_gpio_read_mode, R.string.config_desc_gpio_read_mode));
        adapter.add(new HelpOption(R.string.config_name_output_control, R.string.config_desc_output_control));
        adapter.add(new HelpOption(R.string.config_name_pull_mode, R.string.config_desc_pull_mode));
    }

    @Override
    protected void setup() {
        switch(readMode) {
            case READ_ADC:
                gpio.pin(gpioPin).analogAdc().addRouteAsync(source -> source.stream(gpioSubscriber));
                break;
            case READ_ABS_REF:
                gpio.pin(gpioPin).analogAbsRef().addRouteAsync(source -> source.stream((data, env) -> {
                    final Float voltage = data.value(Float.class);

                    LineData chartData = chart.getData();
                    if (startTime == -1) {
                        chartData.addXValue("0");
                        startTime= System.currentTimeMillis();
                    } else {
                        chartData.addXValue(String.format(Locale.US, "%.2f", sampleCount * samplingPeriod));
                    }

                    chartData.addEntry(new Entry(voltage, sampleCount), 0);

                    sampleCount++;
                }));
                break;
            case READ_DIGITAL:
                gpio.pin(gpioPin).digital().addRouteAsync(source -> source.stream(gpioSubscriber));
                break;
        }
        filenameExtraString= String.format(Locale.US, "%s_pin_%d", csvHeaderDataName, gpioPin);
        timer.scheduleAsync(GPIO_SAMPLE_PERIOD, false, () -> {
            switch(readMode) {
                case READ_ADC:
                    gpio.pin(gpioPin).analogAdc().read();
                    break;
                case READ_ABS_REF:
                    gpio.pin(gpioPin).analogAbsRef().read();
                    break;
                case READ_DIGITAL:
                    gpio.pin(gpioPin).digital().read();
                    break;
                default:
                    throw new RuntimeException("Unrecognized read mode: " + readMode);
            }
        }).continueWith(task -> {
            scheduledTask = task.getResult();
            scheduledTask.start();

            return null;
        });
    }

    @Override
    protected void clean() {
        scheduledTask.remove();
    }

    @Override
    protected void resetData(boolean clearData) {
        super.resetData(clearData);

        if (clearData) {
            startTime= -1;
        }
    }
}
