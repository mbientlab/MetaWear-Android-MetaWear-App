/*
 * Copyright 2014-2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights granted under the terms of a software
 * license agreement between the user who downloaded the software, his/her employer (which must be your
 * employer) and MbientLab Inc, (the "License").  You may not use this Software unless you agree to abide by the
 * terms of the License which can be found at www.mbientlab.com/terms.  The License limits your use, and you
 * acknowledge, that the Software may be modified, copied, and distributed when used in conjunction with an
 * MbientLab Inc, product.  Other than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this Software and/or its documentation for any
 * purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE PROVIDED "AS IS" WITHOUT WARRANTY
 * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL MBIENTLAB OR ITS LICENSORS BE LIABLE OR
 * OBLIGATED UNDER CONTRACT, NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software, contact MbientLab via email:
 * hello@mbientlab.com.
 */

package com.mbientlab.metawear.app;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
//import android.support.annotation.NonNull;
import androidx.annotation.NonNull;
//import android.support.design.widget.FloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
//import android.support.design.widget.Snackbar;
import com.google.android.material.snackbar.Snackbar;
//import androidx.core.app.DialogFragment;
import androidx.fragment.app.DialogFragment;
//import androidx.core.app.Fragment;
import androidx.fragment.app.Fragment;
//import androidx.core.app.FragmentManager;
import androidx.fragment.app.FragmentManager;
//import androidx.core.app.FragmentTransaction;
import androidx.fragment.app.FragmentTransaction;
//import androidx.core.app.LoaderManager;
import androidx.loader.app.LoaderManager;
//import androidx.core.content.CursorLoader;
import androidx.loader.content.CursorLoader;
//import androidx.core.content.Loader;
import androidx.loader.content.Loader;
//import android.support.v7.app.ActionBar;
import androidx.appcompat.app.ActionBar;
//import android.support.design.widget.NavigationView;
import com.google.android.material.navigation.NavigationView;
//import androidx.core.widget.DrawerLayout;
import androidx.drawerlayout.widget.DrawerLayout;
//import android.support.v7.app.ActionBarDrawerToggle;
import androidx.appcompat.app.ActionBarDrawerToggle;
//import android.support.v7.app.AlertDialog;
import androidx.appcompat.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.app.ModuleFragmentBase.FragmentBus;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Settings;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;
import no.nordicsemi.android.dfu.DfuBaseService;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

import static com.mbientlab.metawear.app.ScannerActivity.setConnInterval;

public class NavigationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ServiceConnection, FragmentBus, LoaderManager.LoaderCallbacks<Cursor> {
    public final static String EXTRA_BT_DEVICE= "com.mbientlab.metawear.app.NavigationActivity.EXTRA_BT_DEVICE";

    private static final int SELECT_FILE_REQ = 1, PERMISSION_REQUEST_READ_STORAGE= 2;
    private static final String EXTRA_URI = "uri", FRAGMENT_KEY= "com.mbientlab.metawear.app.NavigationActivity.FRAGMENT_KEY",
            DFU_PROGRESS_FRAGMENT_TAG= "com.mbientlab.metawear.app.NavigationActivity.DFU_PROGRESS_FRAGMENT_TAG";
    private final static Map<Integer, Class<? extends ModuleFragmentBase>> FRAGMENT_CLASSES;
    private final static Map<String, String> EXTENSION_TO_APP_TYPE;

    static {
        Map<Integer, Class<? extends ModuleFragmentBase>> tempMap= new LinkedHashMap<>();
        tempMap.put(R.id.nav_home, HomeFragment.class);
        tempMap.put(R.id.nav_accelerometer, AccelerometerFragment.class);
        tempMap.put(R.id.nav_barometer, BarometerFragment.class);
        tempMap.put(R.id.nav_color_detector, ColorDetectorFragment.class);
        tempMap.put(R.id.nav_gpio, GpioFragment.class);
        tempMap.put(R.id.nav_gyro, GyroFragment.class);
        tempMap.put(R.id.nav_haptic, HapticFragment.class);
        tempMap.put(R.id.nav_humidity, HumidityFragment.class);
        tempMap.put(R.id.nav_ibeacon, IBeaconFragment.class);
        tempMap.put(R.id.nav_i2c, I2CFragment.class);
        tempMap.put(R.id.nav_light, AmbientLightFragment.class);
        tempMap.put(R.id.nav_magnetometer, MagnetometerFragment.class);
        tempMap.put(R.id.nav_neopixel, NeoPixelFragment.class);
        tempMap.put(R.id.nav_proximity, ProximityFragment.class);
        tempMap.put(R.id.nav_sensor_fusion, SensorFusionFragment.class);
        tempMap.put(R.id.nav_settings, SettingsFragment.class);
        tempMap.put(R.id.nav_temperature, TemperatureFragment.class);
        FRAGMENT_CLASSES= Collections.unmodifiableMap(tempMap);

        EXTENSION_TO_APP_TYPE= new HashMap<>();
        EXTENSION_TO_APP_TYPE.put("hex", DfuBaseService.MIME_TYPE_OCTET_STREAM);
        EXTENSION_TO_APP_TYPE.put("bin", DfuBaseService.MIME_TYPE_OCTET_STREAM);
        EXTENSION_TO_APP_TYPE.put("zip", DfuBaseService.MIME_TYPE_ZIP);
    }

    public static class ReconnectDialogFragment extends DialogFragment implements  ServiceConnection {
        private static final String KEY_BLUETOOTH_DEVICE= "com.mbientlab.metawear.app.NavigationActivity.ReconnectDialogFragment.KEY_BLUETOOTH_DEVICE";

        private ProgressDialog reconnectDialog = null;
        private BluetoothDevice btDevice= null;
        private MetaWearBoard currentMwBoard= null;

        public static ReconnectDialogFragment newInstance(BluetoothDevice btDevice) {
            Bundle args= new Bundle();
            args.putParcelable(KEY_BLUETOOTH_DEVICE, btDevice);

            ReconnectDialogFragment newFragment= new ReconnectDialogFragment();
            newFragment.setArguments(args);

            return newFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            btDevice= getArguments().getParcelable(KEY_BLUETOOTH_DEVICE);
            getActivity().getApplicationContext().bindService(new Intent(getActivity(), BtleService.class), this, BIND_AUTO_CREATE);

            reconnectDialog = new ProgressDialog(getActivity());
            reconnectDialog.setTitle(getString(R.string.title_reconnect_attempt));
            reconnectDialog.setMessage(getString(R.string.message_wait));
            reconnectDialog.setCancelable(false);
            reconnectDialog.setCanceledOnTouchOutside(false);
            reconnectDialog.setIndeterminate(true);
            reconnectDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.label_cancel), (dialogInterface, i) -> {
                currentMwBoard.disconnectAsync();
                getActivity().finish();
            });

            return reconnectDialog;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            currentMwBoard= ((BtleService.LocalBinder) service).getMetaWearBoard(btDevice);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) { }
    }
    public static class DfuProgressFragment extends DialogFragment {
        private ProgressDialog dfuProgress= null;

        public static DfuProgressFragment newInstance(int messageStringId) {
            Bundle bundle= new Bundle();
            bundle.putInt("message_string_id", messageStringId);

            DfuProgressFragment newFragment= new DfuProgressFragment();
            newFragment.setArguments(bundle);
            return newFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            dfuProgress= new ProgressDialog(getActivity());
            dfuProgress.setTitle(getString(R.string.title_firmware_update));
            dfuProgress.setCancelable(false);
            dfuProgress.setCanceledOnTouchOutside(false);
            dfuProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dfuProgress.setProgress(0);
            dfuProgress.setMax(100);
            dfuProgress.setMessage(getString(getArguments().getInt("message_string_id")));
            return dfuProgress;
        }

        public void updateProgress(int newProgress) {
            if (dfuProgress != null) {
                dfuProgress.setProgress(newProgress);
            }
        }
    }

    private static String getExtension(String path) {
        int i= path.lastIndexOf('.');
        return i >= 0 ? path.substring(i + 1) : null;
    }

    private final String RECONNECT_DIALOG_TAG= "reconnect_dialog_tag";
    private final Handler taskScheduler = new Handler();
    private BluetoothDevice btDevice;
    private MetaWearBoard mwBoard;
    private Fragment currentFragment= null;
    private Uri fileStreamUri;
    private String fileName;
    private DfuServiceInitiator starter;

    private final DfuProgressListener dfuProgressListener= new DfuProgressListenerAdapter() {
        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            ((DfuProgressFragment) getSupportFragmentManager().findFragmentByTag(DFU_PROGRESS_FRAGMENT_TAG)).updateProgress(percent);
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            ((DialogFragment) getSupportFragmentManager().findFragmentByTag(DFU_PROGRESS_FRAGMENT_TAG)).dismiss();
            Snackbar.make(NavigationActivity.this.findViewById(R.id.drawer_layout), R.string.message_dfu_success, Snackbar.LENGTH_LONG).show();
            resetConnectionStateHandler(5000L);
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            ((DialogFragment) getSupportFragmentManager().findFragmentByTag(DFU_PROGRESS_FRAGMENT_TAG)).dismiss();
            Snackbar.make(NavigationActivity.this.findViewById(R.id.drawer_layout), R.string.error_dfu_aborted, Snackbar.LENGTH_LONG).show();
            resetConnectionStateHandler(5000L);
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            ((DialogFragment) getSupportFragmentManager().findFragmentByTag(DFU_PROGRESS_FRAGMENT_TAG)).dismiss();
            Snackbar.make(NavigationActivity.this.findViewById(R.id.drawer_layout), message, Snackbar.LENGTH_LONG).show();
            resetConnectionStateHandler(5000L);
        }
    };

    private Continuation<Void, Void> reconnectResult= task -> {
        ((DialogFragment) getSupportFragmentManager().findFragmentByTag(RECONNECT_DIALOG_TAG)).dismiss();

        if (task.isCancelled()) {
            finish();
        } else {
            setConnInterval(mwBoard.getModule(Settings.class));
            ((ModuleFragmentBase) currentFragment).reconnected();
        }

        return null;
    };

    private void attemptReconnect() {
        attemptReconnect(0);
    }
    private void attemptReconnect(long delay) {
        ReconnectDialogFragment dialogFragment= ReconnectDialogFragment.newInstance(btDevice);
        dialogFragment.show(getSupportFragmentManager(), RECONNECT_DIALOG_TAG);

        if (delay != 0) {
            taskScheduler.postDelayed(() -> ScannerActivity.reconnect(mwBoard).continueWith(reconnectResult), delay);
        } else {
            ScannerActivity.reconnect(mwBoard).continueWith(reconnectResult);
        }
    }

    @Override
    public BluetoothDevice getBtDevice() {
        return btDevice;
    }

    @Override
    public void resetConnectionStateHandler(long delay) {
        attemptReconnect(delay);
    }

    private boolean addMimeType(Object path) {
        if (path instanceof File) {
            File filePath= (File) path;

            String mimeType= EXTENSION_TO_APP_TYPE.get(getExtension(filePath.getAbsolutePath()));
            if (mimeType == null) {
                mimeType= EXTENSION_TO_APP_TYPE.get(getExtension(fileName));
            }
            if (mimeType.equals(DfuService.MIME_TYPE_OCTET_STREAM)) {
                starter.setBinOrHex(DfuBaseService.TYPE_APPLICATION, filePath.getAbsolutePath());
            } else {
                starter.setZip(filePath.getAbsolutePath());
            }
        } else if (path instanceof Uri) {
            Uri uriPath = (Uri) path;

            String mimeType= EXTENSION_TO_APP_TYPE.get(getExtension(uriPath.toString()));
            if (mimeType == null) {
                mimeType= EXTENSION_TO_APP_TYPE.get(getExtension(fileName));
            }
            if (mimeType.equals(DfuService.MIME_TYPE_OCTET_STREAM)) {
                starter.setBinOrHex(DfuBaseService.TYPE_APPLICATION, uriPath);
            } else {
                starter.setZip(uriPath);
            }
        } else if (path != null) {
            return false;
        }

        return true;
    }

    @Override
    public void initiateDfu(final Object path) {
        starter = new DfuServiceInitiator(btDevice.getAddress())
                .setDeviceName(btDevice.getName())
                .setKeepBond(false)
                .setForceDfu(true);

        // Init packet is required by Bootloader/DFU from SDK 7.0+ if HEX or BIN file is given above.
        // In case of a ZIP file, the init packet (a DAT file) must be included inside the ZIP file.
        //service.putExtra(NordicDfuService.EXTRA_INIT_FILE_PATH, mInitFilePath);
        //service.putExtra(NordicDfuService.EXTRA_INIT_FILE_URI, mInitFileStreamUri);

        if (!addMimeType(path)) {
            Snackbar.make(NavigationActivity.this.findViewById(R.id.drawer_layout), R.string.error_firmware_path_type, Snackbar.LENGTH_LONG).show();
            return;
        }

        taskScheduler.post(() -> {
            if (path == null) {
                DfuProgressFragment.newInstance(R.string.message_dfu).show(getSupportFragmentManager(), DFU_PROGRESS_FRAGMENT_TAG);

                Capture<File> firmwareCapture = new Capture<>();
                mwBoard.downloadLatestFirmwareAsync()
                        .onSuccessTask(task -> {
                            firmwareCapture.set(task.getResult());
                            return mwBoard.inMetaBootMode() ? mwBoard.disconnectAsync() : mwBoard.getModule(Debug.class).jumpToBootloaderAsync();
                        })
                        .continueWith(task -> {
                            if (task.isFaulted()) {
                                Snackbar.make(NavigationActivity.this.findViewById(R.id.drawer_layout), task.getError().getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
                            } else {
                                if (addMimeType(firmwareCapture.get())) {
                                    starter.start(this, DfuService.class);
                                } else {
                                    ((DialogFragment) getSupportFragmentManager().findFragmentByTag(DFU_PROGRESS_FRAGMENT_TAG)).dismiss();
                                    Snackbar.make(NavigationActivity.this.findViewById(R.id.drawer_layout), R.string.error_firmware_path_type, Snackbar.LENGTH_LONG).show();
                                }
                            }
                            return null;
                        }, Task.UI_THREAD_EXECUTOR);
            } else {
                DfuProgressFragment.newInstance(R.string.message_manual_dfu).show(getSupportFragmentManager(), DFU_PROGRESS_FRAGMENT_TAG);
                (mwBoard.inMetaBootMode() ? mwBoard.disconnectAsync() : mwBoard.getModule(Debug.class).jumpToBootloaderAsync())
                        .continueWith(ignored -> starter.start(this, DfuService.class));
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        getApplicationContext().unbindService(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> ((ModuleFragmentBase) currentFragment).showHelpDialog());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            onNavigationItemSelected(navigationView.getMenu().findItem(R.id.nav_home));
        } else {
            currentFragment= getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_KEY);
        }

        btDevice= getIntent().getParcelableExtra(EXTRA_BT_DEVICE);
        getApplicationContext().bindService(new Intent(this, BtleService.class), this, BIND_AUTO_CREATE);

        DfuServiceListenerHelper.registerProgressListener(this, dfuProgressListener);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (currentFragment != null) {
            getSupportFragmentManager().putFragment(outState, FRAGMENT_KEY, currentFragment);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode != RESULT_OK)
            return;

        fileStreamUri= null;
        switch (requestCode) {
            case SELECT_FILE_REQ:
                // and read new one
                final Uri uri = data.getData();
                /*
                 * The URI returned from application may be in 'file' or 'content' schema.
                 * 'File' schema allows us to create a File object and read details from if directly.
                 *
                 * Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
                 */
                if (uri.getScheme().equals("file")) {
                    // the direct path to the file has been returned
                    initiateDfu(new File(uri.getPath()));
                } else if (uri.getScheme().equals("content")) {
                    fileStreamUri= uri;

                    // file name and size must be obtained from Content Provider
                    final Bundle bundle = new Bundle();
                    bundle.putParcelable(EXTRA_URI, uri);
                    getSupportLoaderManager().restartLoader(0, bundle, this);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            mwBoard.disconnectAsync();
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {
            case R.id.action_reset:
                if (!mwBoard.inMetaBootMode()) {
                    mwBoard.getModule(Debug.class).resetAsync()
                            .continueWith(ignored -> {
                                attemptReconnect(0);
                                return null;
                            });
                    Snackbar.make(findViewById(R.id.drawer_layout), R.string.message_soft_reset, Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(findViewById(R.id.drawer_layout), R.string.message_no_soft_reset, Snackbar.LENGTH_LONG).show();
                }
                return true;
            case R.id.action_disconnect:
                if (!mwBoard.inMetaBootMode()) {
                    Settings.BleConnectionParametersEditor editor = mwBoard.getModule(Settings.class).editBleConnParams();
                    if (editor != null) {
                        editor.maxConnectionInterval(125f)
                                .commit();
                    }
                    mwBoard.getModule(Debug.class).disconnectAsync();
                } else {
                    mwBoard.disconnectAsync();
                }

                finish();
                return true;
            case R.id.action_manual_dfu:
                if (checkLocationPermission()) {
                    startContentSelectionIntent();
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction= fragmentManager.beginTransaction();
        if (currentFragment != null) {
            transaction.detach(currentFragment);
        }

        String fragmentTag= FRAGMENT_CLASSES.get(id).getCanonicalName();
        currentFragment= fragmentManager.findFragmentByTag(fragmentTag);

        if (currentFragment == null) {
            try {
                currentFragment= FRAGMENT_CLASSES.get(id).getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Cannot instantiate fragment", e);
            }

            transaction.add(R.id.container, currentFragment, fragmentTag);
        }

        transaction.attach(currentFragment).commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(item.getTitle());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mwBoard= ((BtleService.LocalBinder) service).getMetaWearBoard(btDevice);
        mwBoard.onUnexpectedDisconnect(status -> attemptReconnect());
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri uri = args.getParcelable(EXTRA_URI);
        /*
         * Some apps, f.e. Google Drive allow to select file that is not on the device. There is no "_data" column handled by that provider. Let's try to obtain all columns and than check
         * which columns are present.
	     */
        //final String[] projection = new String[] { MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATA };
        return new CursorLoader(this, uri, null /*all columns, instead of projection*/, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToNext()) {
            /*
             * Here we have to check the column indexes by name as we have requested for all. The order may be different.
             */
            fileName = data.getString(data.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)/* 0 DISPLAY_NAME */);
            //final int fileSize = data.getInt(data.getColumnIndex(MediaStore.MediaColumns.SIZE) /* 1 SIZE */);

            final int dataIndex = data.getColumnIndex(MediaStore.MediaColumns.DATA);
            if (dataIndex != -1) {
                initiateDfu(new File(data.getString(dataIndex /*2 DATA */)));
            } else {
                initiateDfu(fileStreamUri);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * Code for content selection adapted from the nRF Toolbox app by Nordic Semiconductor
     * https://play.google.com/store/apps/details?id=no.nordicsemi.android.nrftoolbox&hl=en
     */
    private void startContentSelectionIntent() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*")
                .addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, SELECT_FILE_REQ);
    }

    @TargetApi(23)
    private boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission code taken from Radius Networks
            // http://developer.radiusnetworks.com/2015/09/29/is-your-beacon-app-ready-for-android-6.html

            // Android M Permission check
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_request_permission);
            builder.setMessage(R.string.permission_read_external_storage);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(dialog -> requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_STORAGE));
            builder.show();
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_READ_STORAGE: {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(findViewById(R.id.drawer_layout), R.string.message_permission_denied, Snackbar.LENGTH_LONG).show();
                } else {
                    startContentSelectionIntent();
                }
            }
        }
    }
}