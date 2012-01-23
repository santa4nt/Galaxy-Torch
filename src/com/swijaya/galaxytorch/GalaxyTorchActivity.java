package com.swijaya.galaxytorch;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

public class GalaxyTorchActivity extends Activity implements View.OnClickListener {

    private final String TAG = "GalaxyTorchActivity";

    private CameraDevice mCameraDevice;
    private FrameLayout mPreviewLayout; // should be hidden
    private SurfaceView mCameraPreview;
    private ImageButton mToggleButton;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mCameraDevice = new CameraDevice();
        mPreviewLayout = (FrameLayout) findViewById(R.id.camera_preview);

        mToggleButton = (ImageButton) findViewById(R.id.pressbutton);
        mToggleButton.setOnClickListener(this);

        mCameraDevice.addFlashlightListener(new CameraDevice.FlashlightListener() {

            // when the camera device is toggled (via toggleCameraLED call),
            // this callback will set the button's state (see: button.isSelected())
            public void flashlightToggled(boolean state) {
                // this callback will be called from a background thread
                // (an AsyncTask runner), so make sure to set the button
                // state from the UI thread
                final boolean fstate = state;
                mToggleButton.post(new Runnable() {

                    public void run() {
                        mToggleButton.setSelected(fstate);
                    }
                });
            }
        });

        // as long as this activity is visible, keep the screen turned on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        boolean success = mCameraDevice.acquireCamera();
        if (!success) {
            return;
        }

        mCameraPreview = mCameraDevice.createSurfaceView(this);
        mPreviewLayout.addView(mCameraPreview);
    }

    private void removePreviewSurface() {
        assert (mPreviewLayout != null);
        if (mCameraPreview != null) {
            Log.v(TAG, "Cleaning up preview surface");
            mPreviewLayout.removeView(mCameraPreview);
            mCameraPreview = null;
        }
    }

    private class CameraToggleTask extends AsyncTask<Boolean, Boolean, Boolean> {

        private boolean mIsTorchOn;

        @Override
        protected void onPreExecute() {
            mIsTorchOn = mCameraDevice.isFlashlightOn();
            mToggleButton.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            assert (params.length == 1);
            boolean on = params[0].booleanValue();
            if (!(on ^ mIsTorchOn)) {   // sanity check
                Log.wtf(TAG, "Toggling with the same state!");
                return true;        // do nothing
            }

            if (!mIsTorchOn && mCameraPreview == null) {
                // we're toggling the torch on
                assert (mCameraPreview == null);
                boolean success = mCameraDevice.acquireCamera();
                publishProgress(success);   // alert the UI thread to update its preview layout
                if (!success)   // bail fast if we didn't succeed acquiring camera
                    return false;
                if (isCancelled()) {
                    Log.v(TAG, "Cancelled after camera resources were acquired.");
                    return false;
                }
            }
            // actually toggle the torch
            // NOTE: toggling the torch off should automatically release its resources
            return mCameraDevice.toggleCameraLED(on);
        }

        /**
         * We are using AsyncTask's onProgressUpdate callback to update the activity's
         * (invisible) camera preview layout. This callback should only be called when
         * we are toggling on the torch.
         */
        @Override
        protected void onProgressUpdate(Boolean... values) {
            assert (values.length == 1);
            boolean isCameraAcquired = values[0];
            if (!isCameraAcquired) {
                // we failed to obtain the camera's resources; alert the user
                Toast.makeText(getApplicationContext(),
                        R.string.err_cannot_acquire,
                        Toast.LENGTH_LONG).show();
                return;
            }

            assert (mCameraPreview == null);
            try {
                Log.v(TAG, "Creating surface view...");
                mCameraPreview = mCameraDevice.createSurfaceView(GalaxyTorchActivity.this);
            }
            catch (Exception e) {
                Log.e(TAG, "Cannot create surface view: " + e.getLocalizedMessage());
                cancel(true);
                Toast.makeText(getApplicationContext(),
                        R.string.err_cannot_acquire,
                        Toast.LENGTH_LONG).show();
                return;
            }

            mPreviewLayout.addView(mCameraPreview);
        }

        @Override
        protected void onCancelled() {
            mCameraDevice.releaseCamera();
            removePreviewSurface();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                Log.e(TAG, "Cannot toggle camera LED");
                // alert the user
                Toast.makeText(getApplicationContext(),
                        R.string.err_cannot_toggle,
                        Toast.LENGTH_LONG).show();
            }

            mIsTorchOn = mCameraDevice.isFlashlightOn();
            Log.v(TAG, "Current torch state should be " + (mIsTorchOn ? "on" : "off"));
            if (!mIsTorchOn) {
                // clean up preview surface after turning flashlight off
                removePreviewSurface();
            }
            mToggleButton.setEnabled(true);
        }

    }

    public void onClick(View v) {
        if (mCameraDevice == null) {
            Log.wtf(TAG, "Did not acquire camera resources before toggle!");
        }

        boolean isTorchOn = mCameraDevice.isFlashlightOn();
        Log.v(TAG, "Current torch state: " + (isTorchOn ? "on" : "off"));
        new CameraToggleTask().execute(!isTorchOn);
    }

    /*@Override
    public void onConfigurationChanged(Configuration newConfig) {
        // as per the manifest configuration, this will be called on
        // orientation change or the virtual keyboard being hidden,
        // which should not even happen at all, due to the activity's
        // orientation being set to 'portrait' mode in the manifest
        super.onConfigurationChanged(newConfig);
    }*/

    @Override
    protected void onDestroy() {
        // the entire lifetime ends here
        Log.v(TAG, "onDestroy");

        assert (mCameraPreview == null);
        assert (!mCameraDevice.isFlashlightOn());

        mCameraDevice.releaseCamera();
        removePreviewSurface();
        mCameraDevice = null;

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        // the foreground lifetime ends here (called often)
        super.onPause();
        Log.v(TAG, "onPause");

        // turn off the torch if it is on
        // XXX: toggleCameraLED() has noticeable delay! consider
        //      alternative approach to concurrently finish onPause
        //      while toggling camera torch and releasing resources
        boolean isTorchOn = mCameraDevice.isFlashlightOn();
        if (isTorchOn) {
            // toggling the camera LED off also releases resources, which
            // contain extra actions that are probably better performed
            // elsewhere in the activity life cycle model
            if (!mCameraDevice.toggleCameraLED(false)) {
                Log.e(TAG, "Cannot toggle camera LED");
            }

            // if toggle OFF did its job, this should be a no-op
            mCameraDevice.releaseCamera();

            // XXX: there is a life cycle path where onStop() wouldn't be called AFTER onPause()!
            //      in such a case, we need to do the same thing there
            removePreviewSurface();
        }
    }

    /*@Override
    protected void onResume() {
        // the foreground lifetime starts here (called often)
        super.onResume();
        Log.v(TAG, "onResume");
    }*/

    @Override
    protected void onStart() {
        // the visible timeline starts here
        super.onStart();
        Log.v(TAG, "onStart");

        assert (mCameraPreview == null);
        assert (!mCameraDevice.isFlashlightOn());
    }

    @Override
    protected void onStop() {
        // the visible timeline ends here
        super.onStop();
        Log.v(TAG, "onStop");

        // clean up after toggling OFF: preview surface
        // XXX: there is a life cycle path where onStop() wouldn't be called AFTER onPause()!
        //      in such a case, we need to do the same thing there
        removePreviewSurface();
    }

}
