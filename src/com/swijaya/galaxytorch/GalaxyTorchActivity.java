/**
 * Copyright (c) 2014 Santoso Wijaya
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
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

    private static final String TAG = GalaxyTorchActivity.class.getSimpleName();
    private static final float DIM_VALUE = 0.01f;

    private SurfaceView mCameraPreview; // should be hidden
    private ImageButton mToggleButton;

    private CameraDevice mCameraDevice; // helper object to acquire and control
                                        // the camera
    private SurfaceHolder mHolder;      // the currently ACTIVE SurfaceHolder

    // variables to hold preference values
    private boolean mOnAtActivityStart; // whether we turn on the flashlight at activity start
    private boolean mDimScreen;			// whether we dim the screen when the flashlight is on
    private boolean mUseVolumeRocker;   // whether we use the volume rocker key event as flashlight toggle
    private boolean mStrobe;            // whether the switch toggles strobe flashlight
    private boolean mUseBritishSwitch;	// whether we use the "British switch" modality for our button's sprite

    private final Lock mSurfaceLock = new ReentrantLock();
    private final Condition mSurfaceHolderIsSet = mSurfaceLock.newCondition();

    /* *** BEGIN MAIN ACTIVITY'S LIFE CYCLE CALLBACKS *** */

    /** Called when the activity is first created. */
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mToggleButton = (ImageButton) findViewById(R.id.pressbutton);
        mToggleButton.setImageResource(mUseBritishSwitch ? R.drawable.switch_button_british : R.drawable.switch_button);
        
        mToggleButton.setOnClickListener(this);
        mToggleButton.setEnabled(false);

        mCameraDevice = new CameraDevice();
        mCameraPreview = (SurfaceView) findViewById(R.id.camerapreview);

        // install a callback so we get notified when the underlying
        // surface is created and destroyed.
        SurfaceHolder holder = mCameraPreview.getHolder();
        if (holder == null) {
            // bail fast
            Log.e(TAG, "Cannot obtain surface holder. Closing activity.");
            Toast toast = Toast.makeText(getApplicationContext(),
                    R.string.err_cannot_acquire, Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

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
            if (!mCameraDevice.toggleCameraLED(false, false)) {
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
        
        // user might have changed the button's sprite modality
        mToggleButton.setImageResource(mUseBritishSwitch ? R.drawable.switch_button_british : R.drawable.switch_button);

        // when we get there from onPause(), the camera would have been released and
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
            Log.v(TAG, "Turning flashlight on at activity start...");
            new TorchToggleTask().execute(false, true);
        }
    }

    private void loadPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        mOnAtActivityStart = pref.getBoolean("onstart", false);
        Log.v(TAG, "Turn flashlight on at activity start? " + mOnAtActivityStart);
        mDimScreen = pref.getBoolean("dimscreen", true);
        Log.v(TAG, "Dim the screen when the flashlight is on? " + mDimScreen);
        mUseVolumeRocker = pref.getBoolean("userocker", false);
        Log.v(TAG, "Use volume rocker as flashlight toggle? " + mUseVolumeRocker);
        mStrobe = pref.getBoolean("strobe", false);
        Log.v(TAG, "Switch toggles strobe mode? " + mStrobe);
        mUseBritishSwitch = pref.getBoolean("usebritswitch", false);
        Log.v(TAG, "Use \"British switch\" modality for button sprite? " + mUseBritishSwitch);
    }

    /**
     * A background task worker to toggle the torch on. We have to resort to
     * using AsyncTask because onCreate() -> onStart() and
     * SurfaceHolder.Callback for the creation of the preview surface defined in
     * the former are all done within a single thread. As it happens, the
     * service's two life cycle callbacks are most likely to be called first
     * before the SurfaceHolder's. This means that when it comes time to toggle
     * the camera's LED within onStart(), the surface won't be ready. Putting
     * locks around the surface holder won't work without moving the one task in
     * a separate thread. Hence, this AsyncTask definition. When calling
     * execute() on this task, you can either provide zero or one Boolean
     * argument. The former will toggle the flashlight to the opposite state,
     * while the latter asserts a specific state to toggle into.
     * 
     * @author santa
     */
    private class TorchToggleTask extends AsyncTask<Boolean, Void, Boolean> {

        // XXX: This is almost identical to its implementation in
        // GalaxyTorchService. Might want to refactor to consolidate.

        private boolean mWasTorchOn;

        @Override
        protected void onPreExecute() {
            Log.v(TAG, "onPreExecute");
            mWasTorchOn = mCameraDevice.isFlashlightOn();
            Log.v(TAG, "Current torch state: " + (mWasTorchOn ? "on" : "off"));

            if (mWasTorchOn ^ mToggleButton.isSelected()) {
                assert (false);
                Log.wtf(TAG, "Button state does not match device state!");
            }

            mToggleButton.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            Log.v(TAG, "doInBackground");

            if (mHolder == null) {
                Log.i(TAG, "Waiting for surface holder to be created...");
                mSurfaceLock.lock();
                try {
                    while (mHolder == null) {
                        mSurfaceHolderIsSet.await();
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "InterruptedException: " + e.getLocalizedMessage());
                    return false;
                } finally {
                    mSurfaceLock.unlock();
                }
            }

            // collect parameters
            boolean toggle = !mWasTorchOn;
            boolean strobe = false;
            assert (params.length <= 2);
            if (params.length > 2) {
                Log.wtf(TAG, "Invalid parameters length");
                return false;
            } else {
                // the first parameter (if any) is strobing mode
                if (params.length >= 1) {
                    strobe = params[0];
                }

                // the second parameter is the toggle state requested
                if (params.length == 2) {
                    toggle = params[1];
                    // sanity check
                    if (!(toggle ^ mWasTorchOn)) {
                        Log.wtf(TAG, "Toggling the same state: " + (toggle ? "on" : "off"));
                        return false;
                    }
                }
            }

            // actually toggle the LED (in torch mode)
            return mCameraDevice.toggleCameraLED(toggle, strobe);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.v(TAG, "onPostExecute: " + result.toString());
            if (!result) {
                Log.e(TAG, "Cannot toggle camera LED");
            }

            // sanity check
            boolean isTorchOn = mCameraDevice.isFlashlightOn();
            Log.v(TAG, "Current torch state should be " + (mWasTorchOn ? "off" : "on")
                    + " and it is " + (isTorchOn ? "on" : "off"));
            assert (isTorchOn == !mWasTorchOn);
            if (isTorchOn == mWasTorchOn) {
                Log.e(TAG, "Current torch state after toggle did not change");
                Toast toast = Toast.makeText(getApplicationContext(),
                        R.string.err_cannot_toggle,
                        Toast.LENGTH_LONG);
                toast.show();
                // TODO: maybe try another strategy?
            }

            mToggleButton.setSelected(isTorchOn);
            mToggleButton.setEnabled(true);
            
            if (!mDimScreen)
            	return;
            
            // adjust brightness if the flashlight is ON
            float brightness;
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            if (isTorchOn) {
            	// this will lock the screen!
            	//brightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
            	brightness = DIM_VALUE;
            } else {
            	brightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            }
            Log.v(TAG, "Setting screen brightness to " + brightness);
            lp.screenBrightness = brightness;
            getWindow().setAttributes(lp);
        }

    }

    @Override
    protected void onPause() {
        // the foreground lifetime ends here (called often)
        super.onPause();
        Log.v(TAG, "onPause");

        // turn off the torch if it is on
        if (mCameraDevice.isFlashlightOn()) {
            if (!mCameraDevice.toggleCameraLED(false, false)) {
                Log.e(TAG, "Cannot toggle camera LED");
                return;
            }
            mToggleButton.setSelected(false);
            
            if (mDimScreen) {
            	WindowManager.LayoutParams lp = getWindow().getAttributes();
            	Log.v(TAG, "Setting screen brightness to " + WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
            	lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            	getWindow().setAttributes(lp);
            }
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
        Log.v(TAG, "onClick");
        new TorchToggleTask().execute(mStrobe);
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

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (!mUseVolumeRocker)
			return super.dispatchKeyEvent(event);
		
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (action == KeyEvent.ACTION_UP) {
				Log.v(TAG, "Handling volume rocker key event.");
				new TorchToggleTask().execute();
			}
			return true;
		default:
			return super.dispatchKeyEvent(event);
		}
	}

}
