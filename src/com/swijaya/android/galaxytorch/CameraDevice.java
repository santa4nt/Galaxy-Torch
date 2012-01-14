package com.swijaya.android.galaxytorch;

import java.util.List;

import android.hardware.Camera;
import android.util.Log;

public abstract class CameraDevice {
	
	private static final String TAG = "CameraDevice";
	
	private Camera mCamera;
	private boolean mFlashlightOn;
	
	protected Camera getCamera() {
		return mCamera;
	}
	
	protected void setCamera(Camera camera) {
		mCamera = camera;
	}
	
	/**
	 * Reflects the current state of the flashlight.
	 * 
	 * @return whether the current state of the flashlight is on
	 */
	public boolean isFlashlightOn() {
		return mFlashlightOn;
	}

	/**
	 * Acquire the default camera object (it should support a flashlight).
	 * Subclasses can override this method for a more specialized usage of
	 * its camera hardware, if necessary.
	 * 
	 * If it does choose to do so, use setCamera() method to set an
	 * associated camera object.
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
		// bail early if we don't
		if (!supportsTorchMode) {
			Log.d(TAG, "This device does not support 'torch' mode!");
			releaseCamera();
		}
		
		return supportsTorchMode;
	}
    
    public void releaseCamera() {
    	if (mCamera != null) {
    		Log.d(TAG, "Releasing camera...");
    		mCamera.release();
    		mCamera = null;
    	}
    }
    
    public boolean turnCameraLED(boolean on) {
    	boolean success = false;
    	
    	if (mCamera == null) {
			acquireCamera();
		}
    	
    	assert (getCamera() != null);	// contractual pre-condition for the abstract method about to be called below
		Log.d(TAG, "Turning " + (on ? "on" : "off") + " camera LED...");
		if (on) {
			success = doTurnOnCameraLED();
		} else {
			success = doTurnOffCameraLED();
		}
		
		if (success) {
			mFlashlightOn = on;
		}
		
		return success;
    }
    
    /**
     * Concrete subclasses must override this method to turn on its
     * flashlight. It could be assumed that an instance of Camera object
     * has been instantiated successfully when this method is called.
     * 
     * @return whether the flashlight was successfully turned on
     */
    protected abstract boolean doTurnOnCameraLED();
    
    /**
     * Concrete subclasses must override this method to turn off its
     * flashlight. It could be assumed that an instance of Camera object
     * has been previously instantiated and, when this method finishes,
     * it will be released.
     * 
     * @return whether the flashlight was successfully turned off
     */
    protected abstract boolean doTurnOffCameraLED();

}
