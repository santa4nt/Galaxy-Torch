package com.swijaya.galaxytorch;

import java.io.IOException;

import android.hardware.Camera;
import android.util.Log;

public class GalaxyTorch implements ITorch {
	
	private final static String TAG = "GalaxyTorch";

	public boolean toggleTorch(Camera camera, boolean on) {
		Camera.Parameters params = camera.getParameters();
		if (on) {
			params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			camera.setParameters(params);
			try {
				camera.setPreviewDisplay(null);
			} catch (IOException e) {
				Log.e(TAG, "Cannot set preview display: " + e.getLocalizedMessage());
				return false;
			}
			camera.startPreview();
		} else {
			params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			camera.setParameters(params);
			camera.stopPreview();
		}
		
		return true;
	}

}
