package com.swijaya.galaxytorch;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

public class GalaxyTorchActivity extends Activity implements View.OnClickListener, SurfaceHolder.Callback {

    private final String TAG = GalaxyTorchActivity.class.getSimpleName();

    private SurfaceView mCameraPreview;     // should be hidden
    private ImageButton mToggleButton;

    private CameraDevice mCameraDevice;     // helper object to acquire and control the camera

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
        //mCameraDevice.stopPreview();  // handled in surface callback
        mCameraDevice.releaseCamera();
        mToggleButton.setSelected(false);
    }

    @Override
    protected void onStart() {
        // the visible timeline starts here
        super.onStart();
        Log.v(TAG, "onStart");

        mCameraDevice.acquireCamera();
        //mCameraDevice.startPreview(); // handled in surface callback
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
    protected void onResume() {
        // the foreground lifetime starts here (called often)
        super.onResume();
        Log.v(TAG, "onResume");
        mCameraDevice.startPreview();
    }

    @Override
    protected void onStop() {
        // the visible timeline ends here
        super.onStop();
        Log.v(TAG, "onStop");

        //mCameraDevice.stopPreview();
        // don't stop preview too early; releaseCamera() does it anyway and
        // it might need the preview to toggle the torch OFF cleanly
        mCameraDevice.releaseCamera();
    }

    /*@Override
    public void onConfigurationChanged(Configuration newConfig) {
        // as per the manifest configuration, this will be called on
        // orientation change or the virtual keyboard being hidden,
        // which should not even happen at all, due to the activity's
        // orientation being set to 'portrait' mode in the manifest
        super.onConfigurationChanged(newConfig);
    }*/

    /* *** END MAIN ACTIVITY'S LIFE CYCLE CALLBACK *** */

    public void onClick(View v) {
        mToggleButton.setEnabled(false);

        try {
            boolean isTorchOn = mCameraDevice.isFlashlightOn();
            Log.v(TAG, "Current torch state: " + (isTorchOn ? "on" : "off"));
            if (isTorchOn ^ mToggleButton.isSelected()) {
                assert (false);
                Log.wtf(TAG, "Button state does not match device state!");
            }

            // actually toggle the LED (in torch mode)
            if (!mCameraDevice.toggleCameraLED(!isTorchOn)) {
                Log.e(TAG, "Cannot toggle camera LED");
            }

            // sanity check
            boolean isTorchOnAfter = mCameraDevice.isFlashlightOn();
            Log.v(TAG, "Current torch state should be " + (isTorchOn ? "off" : "on")
                    + " and it is " + (isTorchOnAfter ? "on" : "off"));
            assert (isTorchOnAfter == !isTorchOn);
            if (isTorchOnAfter == isTorchOn) {
                Log.e(TAG, "Current torch state after toggle did not change");
                Toast.makeText(getApplicationContext(),
                        R.string.err_cannot_toggle,
                        Toast.LENGTH_LONG).show();
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
        mCameraDevice.setPreviewDisplay(holder);
        mCameraDevice.startPreview();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "surfaceDestroyed");
        mCameraDevice.stopPreview();
    }

}
