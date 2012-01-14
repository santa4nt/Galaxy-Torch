package com.swijaya.galaxytorch;

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
		
		return true;
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
    
    private boolean supportsTorchMode() {
    	assert (mCamera != null);
    	Camera.Parameters params = mCamera.getParameters();
    	List<String> flashModes = params.getSupportedFlashModes();
    	return (flashModes != null) &&
    			(flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH));
    }
    
    public boolean turnCameraLED(boolean on) {
    	boolean success = false;
    	
    	// first, obtain a camera device
    	if (mCamera == null) {
			success = acquireCamera();
			if (!success) {
				Log.e(TAG, "Cannot turn camera LED " + (on ? "on" : "off"));
				return false;
			}
		}
    	
    	// check if the camera's flashlight supports torch mode
    	if (!supportsTorchMode()) {
    		// for now, bail early
    		// XXX: there might be workarounds; use specialized ITorch classes in such cases
    		Log.d(TAG, "This device does not support 'torch' mode!");
    		releaseCamera();
    		return false;
    	}
    	
    	// we've got a working torch-supported camera device now
    	mTorch = new DefaultTorch();

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
