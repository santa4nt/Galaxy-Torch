package com.swijaya.galaxytorch;

import android.app.Activity;
import android.os.Bundle;
//import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

public class GalaxyTorchActivity extends Activity implements View.OnClickListener {

    //private final String TAG = "GalaxyTorchActivity";

    private CameraDevice mCameraDevice;
    private FrameLayout mPreviewLayout; // should be hidden
    private SurfaceView mCameraPreview;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mCameraDevice = new CameraDevice();
        mPreviewLayout = (FrameLayout) findViewById(R.id.camera_preview);

        final ImageButton button = (ImageButton) findViewById(R.id.pressbutton);
        button.setOnClickListener(this);
        
        mCameraDevice.addFlashlightListener(new CameraDevice.FlashlightListener() {
			
        	// when the camera device is toggled (via toggleCameraLED call),
        	// this callback will set the button's state (see: button.isSelected())
			public void flashlightToggled(boolean state) {
				button.setSelected(state);
			}
		});

        // as long as this activity is visible, keep the screen turned on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    
    private void removePreviewSurface() {
        assert (mPreviewLayout != null);
        if (mCameraPreview != null) {
	        //Log.v(TAG, "Cleaning up preview surface");
	        mPreviewLayout.removeView(mCameraPreview);
	        mCameraPreview = null;
        }
    }

    public void onClick(View v) {
        boolean isTorchOn = mCameraDevice.isFlashlightOn();
        //Log.v(TAG, "Current torch state: " + (isTorchOn ? "on" : "off"));

        if (!isTorchOn) {
            // we're toggling the torch ON
            assert (mCameraPreview == null);
            mCameraPreview = mCameraDevice.acquireCamera(this);
            mPreviewLayout.addView(mCameraPreview);
        } else {
            // we're toggling the torch OFF
        }

        // toggling the torch OFF should automatically release camera resources
        assert (mCameraPreview != null);
        if (!mCameraDevice.toggleCameraLED(!isTorchOn)) {
            //Log.e(TAG, "Cannot toggle camera LED");
        }

        isTorchOn = mCameraDevice.isFlashlightOn();
        //Log.v(TAG, "Current torch state should be " + (isTorchOn ? "on" : "off"));

        if (!isTorchOn) {
            // clean up after toggling OFF: preview surface
            removePreviewSurface();
        }
    }

    /*@Override
    public void onConfigurationChanged(Configuration newConfig) {
        // as per the manifest configuration, this will be called on
        // orientation change or the virtual keyboard being hidden,
        // which should not even happen at all, due to the activity's
        // orientation being set to 'portrait' mode in the manifest
        super.onConfigurationChanged(newConfig);
    }*/

    @Override
    protected void onDestroy() {
        // the entire lifetime ends here
        super.onDestroy();
        //Log.v(TAG, "onDestroy");
        
        assert (mCameraPreview == null);
        assert (!mCameraDevice.isFlashlightOn());
        
        mCameraDevice.releaseCamera();
        mCameraDevice = null;
    }

    @Override
    protected void onPause() {
        // the foreground lifetime ends here (called often)
        super.onPause();
        //Log.v(TAG, "onPause");

        // turn off the torch if it is on
        // XXX: toggleCameraLED() has noticeable delay! consider
        //      alternative approach to concurrently finish onPause
        //      while toggling camera torch and releasing resources
        boolean isTorchOn = mCameraDevice.isFlashlightOn();
        if (isTorchOn) {
            // toggling the camera LED off also releases resources, which
            // contain extra actions that are probably better performed
            // elsewhere in the activity life cycle model
            if (!mCameraDevice.toggleCameraLED(false)) {
                //Log.e(TAG, "Cannot toggle camera LED");
            }
            
            // if toggle OFF did its job, this should be a no-op
            mCameraDevice.releaseCamera();
            
            // XXX: there is a life cycle path where onStop() wouldn't be called AFTER onPause()!
            //      in such a case, we need to do the same thing there
            removePreviewSurface();
        }
    }

    /*@Override
    protected void onResume() {
        // the foreground lifetime starts here (called often)
        super.onResume();
        Log.v(TAG, "onResume");
    }*/

    @Override
    protected void onStart() {
        // the visible timeline starts here
        super.onStart();
        //Log.v(TAG, "onStart");

        assert (mCameraPreview == null);
        assert (!mCameraDevice.isFlashlightOn());
    }

    @Override
    protected void onStop() {
        // the visible timeline ends here
        super.onStop();
        //Log.v(TAG, "onStop");

        // clean up after toggling OFF: preview surface
        // XXX: there is a life cycle path where onStop() wouldn't be called AFTER onPause()!
        //      in such a case, we need to do the same thing there
        removePreviewSurface();
    }

}
