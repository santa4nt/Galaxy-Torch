package com.swijaya.android;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class GalaxyTorchActivity extends Activity implements OnClickListener {
	
	private CameraDevice mCameraDevice;
	private boolean mIsTorchOn;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mCameraDevice = new GalaxyCameraDevice();
        mIsTorchOn = false;
        
        Button button = (Button) findViewById(R.id.pressbutton);
        button.setOnClickListener(this);
    }
    
    public void onClick(View v) {
		mCameraDevice.turnCameraLED(!mIsTorchOn);
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