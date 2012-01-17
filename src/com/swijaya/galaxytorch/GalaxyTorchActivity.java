package com.swijaya.galaxytorch;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class GalaxyTorchActivity extends Activity implements View.OnClickListener {

    private final String TAG = "GalaxyTorchActivity";

    private CameraDevice mCameraDevice;
    private FrameLayout mPreviewLayout; // should be hidden
    private SurfaceView mCameraPreview;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mCameraDevice = new CameraDevice();
        mPreviewLayout = (FrameLayout) findViewById(R.id.camera_preview);

        Button button = (Button) findViewById(R.id.pressbutton);
        button.setOnClickListener(this);
    }

    public void onClick(View v) {
        boolean isTorchOn = mCameraDevice.isFlashlightOn();
        Log.v(TAG, "Current torch state: " + (isTorchOn ? "on" : "off"));

        if (!isTorchOn) {
            // we're toggling the torch ON
            assert (mCameraPreview == null);
            mCameraPreview = mCameraDevice.acquireCamera(this);
            mPreviewLayout.addView(mCameraPreview);
        } else {
            // we're toggling the torch OFF
        }

        // toggling the torch OFF should automatically release camera resources
        assert (mCameraPreview != null);
        if (!mCameraDevice.toggleCameraLED(!isTorchOn)) {
            Log.e(TAG, "Cannot toggle camera LED");
        }

        isTorchOn = mCameraDevice.isFlashlightOn();
        Log.v(TAG, "Current torch state should be " + (isTorchOn ? "on" : "off"));

        if (!isTorchOn) {
            // clean up after toggling OFF: preview surface
            Log.v(TAG, "Cleaning up preview surface");
            mPreviewLayout.removeView(mCameraPreview);
            mCameraPreview = null;
        }
    }

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// as per the manifest configuration, this will be called on
		// orientation change or the virtual keyboard being hidden,
		// which should not even happen at all, due to the activity's
		// orientation being set to 'portrait' mode in the manifest
		super.onConfigurationChanged(newConfig);
	}

}
