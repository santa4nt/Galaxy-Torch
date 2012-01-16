package com.swijaya.galaxytorch;

import android.app.Activity;
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
            SurfaceView preview = mCameraDevice.acquireCamera(this);
            mPreviewLayout.addView(preview);
        }

        if (!mCameraDevice.toggleCameraLED(!isTorchOn)) {
            Log.e(TAG, "Cannot toggle camera LED");
        }
        Log.v(TAG, "Current torch state should be " +
                (mCameraDevice.isFlashlightOn() ? "on" : "off"));
    }

}
