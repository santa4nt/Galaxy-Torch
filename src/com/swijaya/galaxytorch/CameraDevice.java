package com.swijaya.galaxytorch;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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
	 * @param context the activity that invoked this method
	 * @return a SurfaceView object attached to the camera
	 */
	public SurfaceView acquireCamera(Context context) {
		Log.v(TAG, "Acquiring camera...");
		assert (mCamera == null);
		try {
    		mCamera = Camera.open();
    	}
    	catch (RuntimeException e) {
    		Log.e(TAG, e.getLocalizedMessage());
    	}
		
		if (mCamera == null) {
			Log.e(TAG, "Failed to open camera.");
			return null;
		}
		
		return new CameraPreview(context, mCamera);
	}
    
    public void releaseCamera() {
    	if (mCamera != null) {
    		Log.v(TAG, "Releasing camera...");
    		if (mIsFlashlightOn) {
    			// attempt to cleanly turn off the torch (in case keeping a
    			// "torch" on is a hackery) prior to release
    			mTorch.toggleTorch(mCamera, false);
    			mIsFlashlightOn = false;
    		}
    		mCamera.release();
    		mCamera = null;
    		mTorch = null;
    	}
    }
    
    private boolean supportsTorchMode() {
    	Camera.Parameters params = mCamera.getParameters();
    	List<String> flashModes = params.getSupportedFlashModes();
    	return (flashModes != null) &&
    			(flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH));
    }
    
    /**
     * Toggle the camera device's flashlight LED in a continuous manner.
     * Pre-condition: the camera device has been acquired
     * Post-condition: the camera device will be released if this method
     *                 is called with parameter on == false
     * 
     * @param on whether to toggle the flashlight LED on (true) or off (false)
     * @return operation success
     */
    public boolean toggleCameraLED(boolean on) {
    	assert (mCamera != null);
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

    	boolean success = false;
		Log.d(TAG, "Turning " + (on ? "on" : "off") + " camera LED...");
		success = mTorch.toggleTorch(mCamera, on);
		if (success) {
			mIsFlashlightOn = on;
			if (!on) {
				// when we are turning off the flashlight, also release camera
				releaseCamera();
			}
		}
		
		return success;
    }
    
    protected class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    	
    	private static final String TAG = "CameraPreview";
    		
    	private SurfaceHolder mHolder;
    	private Camera mCamera;
    	
    	public CameraPreview(Context context, Camera camera) {
    		super(context);
    		mCamera = camera;
    		
    		// install a callback so we get notified when the underlying
    		// surface is created and destroyed.
    		mHolder = getHolder();
    		mHolder.addCallback(this);
    		// deprecated
    		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    	}
    	
    	public void surfaceCreated(SurfaceHolder holder) {
    		// tell the camera where to draw the preview
    		try {
    			mCamera.setPreviewDisplay(holder);
    			mCamera.startPreview();
    		}
    		catch (IOException e) {
    			Log.e(TAG, "Error setting camera preview: " + e.getLocalizedMessage());
    		}
    	}
    	
    	public void surfaceDestroyed(SurfaceHolder holder) {
    		// empty; take care of releasing camera preview in client code
    	}
    	
    	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    		// for changing preview (rotate, etc?); make sure to stop the preview
    		// prior to resizing or reformatting
    	}
    	
    }

}
