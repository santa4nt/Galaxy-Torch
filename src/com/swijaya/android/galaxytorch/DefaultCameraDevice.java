package com.swijaya.android.galaxytorch;

import android.hardware.Camera;

public class DefaultCameraDevice extends CameraDevice {

	@Override
	protected boolean doTurnOnCameraLED() {
		Camera.Parameters params = mCamera.getParameters();
		params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
		mCamera.setParameters(params);
		return true;
	}
	
	@Override
	protected boolean doTurnOffCameraLED() {
		releaseCamera();
		return true;
	}

}
