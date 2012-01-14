package com.swijaya.android.galaxytorch;

import java.util.List;

import android.hardware.Camera;
import android.util.Log;

public class CameraDevice {
	
	private static final String TAG = "CameraDevice";
	
	private Camera mCamera;
	private ITorch mTorch;
	private boolean mIsFlashlightOn;
	
	public boolean isFlashlightOn() {
		return mIsFlashlightOn;
	}

	/**
	 * Acquire the default camera object (it should support a flashlight).
	 * Subclasses can override this method for a more specialized usage of
	 * its camera hardware, if necessary.
	 * 
	 * @return success in opening and acquiring a camera device's resources
	 */
	public boolean acquireCamera() {
		Log.d(TAG, "Acquiring camera...");
		assert (mCamera == null);
		try {
    		mCamera = Camera.open();
    	}
    	catch (RuntimeException e) {
    		Log.e(TAG, e.getLocalizedMessage());
    	}
		
		if (mCamera == null) {
			Log.e(TAG, "Failed to open camera.");
			return false;
		}
		
		// make sure the device supports torch mode
		Camera.Parameters params = mCamera.getParameters();
		List<String> flashModes = params.getSupportedFlashModes();
		boolean supportsTorchMode = (flashModes != null) &&
				(flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH));
		// bail early if we don't	// XXX: there might be workarounds; use specialized ITorch classes in such cases
		if (!supportsTorchMode) {
			Log.d(TAG, "This device does not support 'torch' mode!");
			releaseCamera();
		} else {
			mTorch = new DefaultTorch();
		}
		
		return supportsTorchMode;
	}
    
    public void releaseCamera() {
    	if (mCamera != null) {
    		Log.d(TAG, "Releasing camera...");
    		if (mIsFlashlightOn) {
    			// attempt to cleanly turn off the torch (in case keeping a
    			// "torch" on is a hackery) prior to release
    			mTorch.turnTorch(mCamera, false);
    			mIsFlashlightOn = false;
    		}
    		mCamera.release();
    		mCamera = null;
    		mTorch = null;
    	}
    }
    
    public boolean turnCameraLED(boolean on) {
    	boolean success = false;
    	
    	// first, obtain a camera device (with torch support)
    	if (mCamera == null) {
			success = acquireCamera();
			if (!success) {
				Log.e(TAG, "Cannot turn camera LED " + (on ? "on" : "off"));
				return success;
			}
		}
    	
    	// we've got a working torch-supported camera device now
    	assert (mTorch != null);

		Log.d(TAG, "Turning " + (on ? "on" : "off") + " camera LED...");
		success = mTorch.turnTorch(mCamera, on);
		if (success) {
			mIsFlashlightOn = on;
			if (!on) {
				// when we are turning off the flashlight, also release camera
				releaseCamera();
			}
		}
		
		return success;
    }

}
