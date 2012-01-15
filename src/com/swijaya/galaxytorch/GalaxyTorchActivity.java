package com.swijaya.galaxytorch;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.swijaya.galaxytorch.R.id;

public class GalaxyTorchActivity extends Activity implements View.OnClickListener {
	
	private final String TAG = "GalaxyTorchActivity";
	
	private CameraDevice mCameraDevice;
	private boolean mIsTorchOn;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mCameraDevice = new CameraDevice();
        mIsTorchOn = false;
        
        Button button = (Button) findViewById(R.id.pressbutton);
        button.setOnClickListener(this);
        
        SurfaceView cameraPreview = mCameraDevice.acquireCamera(this);
        FrameLayout preview = (FrameLayout) findViewById(id.camera_preview);
        preview.addView(cameraPreview);
    }
    
    public void onClick(View v) {
    	Log.d(TAG, "I'm being pressed! Current state: " + (mIsTorchOn ? "on" : "off"));
		if (mCameraDevice.toggleCameraLED(!mIsTorchOn)) {
			mIsTorchOn = !mIsTorchOn;
			Log.d(TAG, "Flashlight should be " + (mIsTorchOn ? "on" : "off"));
		} else {
			Log.d(TAG, "Could not turn on flashlight.");
		}
	}
    
}