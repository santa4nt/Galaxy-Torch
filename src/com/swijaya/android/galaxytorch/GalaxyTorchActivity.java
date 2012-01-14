package com.swijaya.android.galaxytorch;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class GalaxyTorchActivity extends Activity implements OnClickListener {
	
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
    }
    
    public void onClick(View v) {
    	Log.d(TAG, "I'm being pressed! Current state: " + (mIsTorchOn ? "on" : "off"));
		if (mCameraDevice.turnCameraLED(!mIsTorchOn)) {
			mIsTorchOn = !mIsTorchOn;
			Log.d(TAG, "Flashlight should be " + (mIsTorchOn ? "on" : "off"));
		} else {
			Log.d(TAG, "Could not turn on flashlight.");
		}
	}

	/** Check if this device's feature list includes a camera. */
    private boolean hasFeatureCamera(Context context) {
    	return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }
    
    /** Check if this device's feature list includes a camera flashlight. */
    private boolean hasFeatureCameraFlashlight(Context context) {
    	return hasFeatureCamera(context) &&
    			context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }
    
}