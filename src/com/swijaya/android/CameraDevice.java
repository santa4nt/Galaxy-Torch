package com.swijaya.android;

import android.hardware.Camera;

public abstract class CameraDevice {
	
	protected Camera mCamera;
	
	public void acquireCamera() {
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
    		mCamera.release();
    		mCamera = null;
    	}
    }
    
    public abstract boolean turnCameraLED(boolean on);

}
