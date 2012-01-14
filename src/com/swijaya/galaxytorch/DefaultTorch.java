package com.swijaya.galaxytorch;

import android.hardware.Camera;
import android.util.Log;

public class DefaultTorch implements ITorch {
	
	private static final String TAG = "DefaultTorch";

	/**
	 * Generally, the following simple steps should suffice to turn on the
	 * flashlight's torch mode.
	 */
	public boolean toggleTorch(Camera camera, boolean on) {
		setFlashMode(camera, (on ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF));
		return true;
	}
	
	private void setFlashMode(Camera camera, String mode) {
		Log.d(TAG, "Setting flash mode: " + mode);
		Camera.Parameters params = camera.getParameters();
		params.setFlashMode(mode);
		camera.setParameters(params);
	}

}
