package com.swijaya.android;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;

public class GalaxyTorchActivity extends Activity {
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
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