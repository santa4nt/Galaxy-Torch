package com.swijaya.galaxytorch;

import java.util.List;

import android.hardware.Camera;
import android.util.Log;

public class CameraDevice {

    public interface Torch {

        public boolean toggleTorch(Camera camera, boolean on);

    }

    private static final String TAG = CameraDevice.class.getSimpleName();

    private Camera mCamera;
    private CameraDevice.Torch mTorch;
    private boolean mIsFlashlightOn;
    private boolean mIsPreviewStarted;

    public boolean isFlashlightOn() {
        return mIsFlashlightOn;
    }

    private void postFlashlightState(boolean on) {
        mIsFlashlightOn = on;
    }

    protected Camera getCamera() {
        return mCamera;
    }

    /**
     * Acquire the default camera object (it should support a flashlight).
     * Subclasses can override this method for a more specialized usage of
     * its camera hardware, if necessary.
     * 
     * @param context the activity that invoked this method
     * @return whether the method was successful
     */
    public boolean acquireCamera() {
        Log.v(TAG, "Acquiring camera...");
        assert (mCamera == null);
        try {
            mCamera = Camera.open();
        }
        catch (RuntimeException e) {
            Log.e(TAG, "Failed to open camera: " + e.getLocalizedMessage());
        }

        return (mCamera != null);
    }

    public void releaseCamera() {
        if (mCamera != null) {
            Log.v(TAG, "Releasing camera...");
            if (mIsFlashlightOn) {
                // attempt to cleanly turn off the torch (in case keeping a
                // "torch" on is a hackery) prior to release
                mTorch.toggleTorch(mCamera, false);
                postFlashlightState(false);
            }
            stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void stopPreview() {
        if (mIsPreviewStarted) {
            mCamera.stopPreview();
            mIsPreviewStarted = false;
        }
    }

    public void startPreview() {
        if (!mIsPreviewStarted) {
            mCamera.startPreview();
            mIsPreviewStarted = true;
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
     * Pre-condition: the camera device, and its associated resources, has
     *                been acquired and set up
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
            Log.d(TAG, "This device does not support 'torch' mode");
            return false;
        }

        // we've got a working torch-supported camera device now
        mTorch = new DefaultTorch();

        boolean success = false;
        Log.v(TAG, "Turning " + (on ? "on" : "off") + " camera LED...");
        success = mTorch.toggleTorch(mCamera, on);
        if (success) {
            postFlashlightState(on);
        }

        return success;
    }

}
