package com.swijaya.android.galaxytorch;

import android.hardware.Camera;

public class GalaxyCameraDevice extends CameraDevice {

	@Override
	public boolean turnCameraLED(boolean on) {
		if (mCamera == null) {
			acquireCamera();
		}
		
		assert (mCamera != null);
    	
		if (on) {
			Camera.Parameters params = mCamera.getParameters();
    		params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
    		mCamera.setParameters(params);
		} else {
			releaseCamera();
		}
		
		return true;
	}

}
