/**
 * Copyright (c) 2012 Santoso Wijaya
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.swijaya.galaxytorch;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GalaxyTorchActivity extends Activity implements View.OnClickListener,
        SurfaceHolder.Callback {

    private final String TAG = GalaxyTorchActivity.class.getSimpleName();

    private SurfaceView mCameraPreview; // should be hidden
    private ImageButton mToggleButton;

    private CameraDevice mCameraDevice; // helper object to acquire and control
                                        // the camera
    private SurfaceHolder mHolder; // the currently ACTIVE SurfaceHolder

    private boolean mOnAtActivityStart; // a preference setting whether we turn
                                        // on the flashlight at activity start

    private final Lock mSurfaceLock = new ReentrantLock();
    private final Condition mSurfaceHolderIsSet = mSurfaceLock.newCondition();

    /* *** BEGIN MAIN ACTIVITY'S LIFE CYCLE CALLBACKS *** */

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mToggleButton = (ImageButton) findViewById(R.id.pressbutton);
        mToggleButton.setOnClickListener(this);

        mToggleButton.setEnabled(false);

        mCameraDevice = new CameraDevice();
        mCameraPreview = (SurfaceView) findViewById(R.id.camerapreview);
        // install a callback so we get notified when the underlying
        // surface is created and destroyed.
        SurfaceHolder holder = mCameraPreview.getHolder();
        holder.addCallback(this);
        // deprecated
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mToggleButton.setEnabled(true);

        // as long as this activity is visible, keep the screen turned on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        // the entire lifetime ends here
        super.onDestroy();
        Log.v(TAG, "onDestroy");

        if (mCameraDevice.isFlashlightOn()) {
            if (!mCameraDevice.toggleCameraLED(false)) {
                Log.e(TAG, "Cannot toggle camera LED");
            }
        }
        // mCameraDevice.stopPreview(); // handled in surface callback
        mCameraDevice.releaseCamera();
        mToggleButton.setSelected(false);
    }

    @Override
    protected void onStart() {
        // the visible timeline starts here
        super.onStart();
        Log.v(TAG, "onStart");

        loadPreferences();

        // when we get there from onPause(), the camera would have been released
        // and
        // now re-acquired, but that means the camera has now no surface holder
        // to flush to! so remember the state of the surface holder, and reset
        // it immediately after re-acquiring
        if (!mCameraDevice.acquireCamera()) {
            // bail fast if we cannot acquire the camera device to begin with
            // perhaps the widget (and therefore the service) is holding it,
            // or some other background service outside of our control
            Log.e(TAG, "Cannot acquire camera. Closing activity.");
            Toast toast = Toast.makeText(getApplicationContext(),
                    R.string.err_cannot_acquire, Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
        if (mHolder != null) {
            mCameraDevice.setPreviewDisplayAndStartPreview(mHolder);
        }

        if (mOnAtActivityStart) {
            if (mHolder == null) {
                // wait for the surface holder to be created
                Log.v(TAG, "Waiting for surface holder to be created...");
                mSurfaceLock.lock();
                try {
                    while (mHolder == null) {
                        mSurfaceHolderIsSet.await();
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "InterruptedException: " + e.getLocalizedMessage());
                    return;
                } finally {
                    mSurfaceLock.unlock();
                }
            }

            Log.v(TAG, "Turning flashlight on at activity start...");
            toggle(true);
        }
    }

    private void loadPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        mOnAtActivityStart = pref.getBoolean("onstart", false);
        Log.v(TAG, "Turn flashlight on at activity start? " + mOnAtActivityStart);
    }

    @Override
    protected void onPause() {
        // the foreground lifetime ends here (called often)
        super.onPause();
        Log.v(TAG, "onPause");

        // turn off the torch if it is on
        if (mCameraDevice.isFlashlightOn()) {
            if (!mCameraDevice.toggleCameraLED(false)) {
                Log.e(TAG, "Cannot toggle camera LED");
                return;
            }
            mToggleButton.setSelected(false);
        }
    }

    @Override
    protected void onStop() {
        // the visible timeline ends here
        super.onStop();
        Log.v(TAG, "onStop");

        // mCameraDevice.stopPreview();
        // don't stop preview too early; releaseCamera() does it anyway and
        // it might need the preview to toggle the torch OFF cleanly
        mCameraDevice.releaseCamera();
    }

    /* *** END MAIN ACTIVITY'S LIFE CYCLE CALLBACK *** */

    public void onClick(View v) {
        boolean isTorchOn = mCameraDevice.isFlashlightOn();
        toggle(!isTorchOn);
    }

    private void toggle(boolean on) {
        mToggleButton.setEnabled(false);
        try {
            boolean isTorchOn = mCameraDevice.isFlashlightOn();
            Log.v(TAG, "Current torch state: " + (isTorchOn ? "on" : "off"));
            if (on == isTorchOn) {
                assert (false);
                Log.wtf(TAG, "Current state is the same!");
                return;
            }

            if (isTorchOn ^ mToggleButton.isSelected()) {
                assert (false);
                Log.wtf(TAG, "Button state does not match device state!");
            }

            // actually toggle
            if (!mCameraDevice.toggleCameraLED(on)) {
                Log.e(TAG, "Cannot toggle camera LED");
            }

            // sanity check
            boolean isTorchOnAfter = mCameraDevice.isFlashlightOn();
            Log.v(TAG, "Current torch state should be " + (on ? "on" : "off")
                    + " and it is " + (isTorchOnAfter ? "on" : "off"));
            assert (on == isTorchOnAfter);
            if (on != isTorchOnAfter) {
                Log.e(TAG, "Current torch state after toggle did not change");
                Toast toast = Toast.makeText(getApplicationContext(),
                        R.string.err_cannot_toggle, Toast.LENGTH_SHORT);
                toast.show();
                // TODO: maybe try another strategy?
            }
            mToggleButton.setSelected(isTorchOnAfter);
        } finally {
            mToggleButton.setEnabled(true);
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v(TAG, "surfaceChanged");
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "surfaceCreated");
        // atomically set the surface holder and start camera preview
        mSurfaceLock.lock();
        try {
            mHolder = holder;
            mCameraDevice.setPreviewDisplayAndStartPreview(mHolder);
            mSurfaceHolderIsSet.signalAll();
        } finally {
            mSurfaceLock.unlock();
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "surfaceDestroyed");
        mCameraDevice.stopPreview();
        mHolder = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Log.v(TAG, "Settings selected");
                startActivity(new Intent(this, GalaxyTorchSettings.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
