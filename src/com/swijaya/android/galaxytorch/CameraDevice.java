package com.swijaya.android.galaxytorch;

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
    	}
    }
    
    public boolean turnCameraLED(boolean on) {
    	if (mCamera == null) {
			acquireCamera();
		}
    	
		assert (mCamera != null);
		Log.d(TAG, "Turning " + (on ? "on" : "off") + " camera LED...");
		
		boolean success;
		if (on) {
			success = doTurnOnCameraLED();
		} else {
			success = doTurnOffCameraLED();
		}
		
		return success;
    }
    
    protected abstract boolean doTurnOnCameraLED();
    
    protected abstract boolean doTurnOffCameraLED();

}
