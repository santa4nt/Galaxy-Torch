package com.swijaya.galaxytorch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraDevice {

    private static final String TAG = "CameraDevice";

    private Camera mCamera;
    private CameraDevice.Torch mTorch;
    private boolean mIsFlashlightOn;
    private boolean mIsPreviewStarted;
    private final List<FlashlightListener> mFlashlightListeners;

    public CameraDevice() {
        mFlashlightListeners = new ArrayList<FlashlightListener>();
    }

    public boolean isFlashlightOn() {
        return mIsFlashlightOn;
    }

    private void setFlashlightOn(boolean on) {
        mIsFlashlightOn = on;
        for (FlashlightListener listener : mFlashlightListeners) {
            listener.flashlightToggled(on);
        }
    }

    protected Camera getCamera() {
        return mCamera;
    }

    public void addFlashlightListener(FlashlightListener listener) {
        mFlashlightListeners.add(listener);
    }

    public boolean removeFlashlightListener(FlashlightListener listener) {
        return mFlashlightListeners.remove(listener);
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
                setFlashlightOn(false);
            }
            if (mIsPreviewStarted) {
                mCamera.stopPreview();
                mIsPreviewStarted = false;
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
     * Pre-condition: the camera device, and its associated resources, has
     *                been acquired and set up
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
            Log.d(TAG, "This device does not support 'torch' mode");
            releaseCamera();
            return false;
        }

        // we've got a working torch-supported camera device now
        mTorch = new DefaultTorch();

        boolean success = false;
        Log.v(TAG, "Turning " + (on ? "on" : "off") + " camera LED...");
        success = mTorch.toggleTorch(mCamera, on);
        if (success) {
            setFlashlightOn(on);
            if (!on) {
                // when we are turning off the flashlight, also release camera
                releaseCamera();
            }
        }

        return success;
    }

    public SurfaceView createSurfaceView(Context context) {
        return new CameraPreview(context);
    }

    protected class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

        private static final String TAG = "CameraPreview";

        private final SurfaceHolder mHolder;

        public CameraPreview(Context context) {
            super(context);

            Log.v(TAG, "Setting up camera preview...");

            // install a callback so we get notified when the underlying
            // surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // tell the camera where to draw the preview
            Log.v(TAG, "surface created");
            assert (!mIsPreviewStarted);
            Camera camera = getCamera();
            if (camera == null) {
                Log.wtf(TAG, "!! surfaceCreated called with NULL camera");
                return;
            }
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            }
            catch (IOException e) {
                Log.e(TAG, "Error setting camera preview: " + e.getLocalizedMessage());
            }
            mIsPreviewStarted = true;
            Log.v(TAG, "preview started");
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty; take care of releasing camera preview in client code
            Log.v(TAG, "surface destroyed");
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // for changing preview (rotate, etc?); make sure to stop the preview
            // prior to resizing or reformatting
            Log.v(TAG, "surface changed");
        }

    }

    public interface Torch {

        public boolean toggleTorch(Camera camera, boolean on);

    }

    public interface FlashlightListener {

        public void flashlightToggled(boolean state);

    }

}
