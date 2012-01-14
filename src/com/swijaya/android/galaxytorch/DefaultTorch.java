package com.swijaya.android.galaxytorch;

import android.hardware.Camera;

public class DefaultTorch implements ITorch {

	public boolean turnOnTorch(Camera camera) {
		Camera.Parameters params = camera.getParameters();
		params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
		camera.setParameters(params);
		return true;
	}

	public boolean turnOffTorch(Camera camera) {
		Camera.Parameters params = camera.getParameters();
		params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
		camera.setParameters(params);
		return true;
	}

}
