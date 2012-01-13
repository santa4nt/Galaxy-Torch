package com.swijaya.android;

import android.hardware.Camera;
import android.util.Log;

public abstract class CameraDevice {
	
	private static final String TAG = "CameraDevice";
	
	protected Camera mCamera;
	
	public void acquireCamera() {
		Log.d(TAG, "Acquiring camera...");
		
		assert (mCamera == null);
		
		try {
    		mCamera = Camera.open();
    		Log.d(TAG, "Camera acquired.");
    	}
    	catch (RuntimeException e) {
    		// TODO (camera does not exist or in use)
    	}
	}
    
    public void releaseCamera() {
    	if (mCamera != null) {
    		Log.d(TAG, "Releasing camera...");
    		mCamera.release();
    		mCamera = null;
    		Log.d(TAG, "Camera released.");
    	}
    }
    
    public abstract boolean turnCameraLED(boolean on);

}
