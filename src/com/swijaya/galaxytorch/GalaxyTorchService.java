package com.swijaya.galaxytorch;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.Toast;

public class GalaxyTorchService extends Service {

    private static final String TAG = GalaxyTorchService.class.getSimpleName();

    private CameraDevice mCameraDevice;

    private LinearLayout mOverlay;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;

    private final Lock mSurfaceLock = new ReentrantLock();
    private final Condition mSurfaceHolderIsSet = mSurfaceLock.newCondition();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate: service starting");

        mCameraDevice = new CameraDevice();
        mCameraDevice.acquireCamera();

        createOverlay();    // this gives us the surface view the camera device needs
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {

            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.v(TAG, "(overlay) surfaceDestroyed");
            }

            public void surfaceCreated(SurfaceHolder holder) {
                Log.v(TAG, "(overlay) surfaceCreated");
                if (mCameraDevice == null) {
                    Log.w(TAG, "surfaceCreated: Camera device has not been instantiated");
                    return;
                }

                // atomically set the surface holder and start camera preview
                mSurfaceLock.lock();
                try {
                    mHolder = holder;
                    mCameraDevice.setPreviewDisplayAndStartPreview(holder);
                    mSurfaceHolderIsSet.signalAll();
                }
                finally {
                    mSurfaceLock.unlock();
                }
            }

            public void surfaceChanged(SurfaceHolder holder, int format, int width,
                    int height) {
                Log.v(TAG, "(overlay) surfaceChanged");
            }
        });
        // deprecated
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy: service destroyed");

        if (mCameraDevice.isFlashlightOn()) {
            Log.w(TAG, "Flashlight still on");
            if (!mCameraDevice.toggleCameraLED(false)) {
                Log.e(TAG, "Cannot toggle camera LED");
            }
        }
        //mCameraDevice.stopPreview();  // handled in surface callback
        mCameraDevice.releaseCamera();
        mCameraDevice = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");

        RemoteViews widgetViews =
                new RemoteViews(getPackageName(), R.layout.widget);
        widgetViews.setImageViewResource(R.id.widgetbutton, R.drawable.button_pressed);

        // wait until the surface view overlay is created
        mSurfaceLock.lock();
        try {
            while (mHolder == null) {
                mSurfaceHolderIsSet.await();
            }
        }
        catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException: " + e.getLocalizedMessage());
        }
        finally {
            mSurfaceLock.unlock();
        }

        boolean isTorchOn = mCameraDevice.isFlashlightOn();
        Log.v(TAG, "Current torch state: " + (isTorchOn ? "on" : "off"));

        // actually toggle the LED (in torch mode)
        if (!mCameraDevice.toggleCameraLED(!isTorchOn)) {
            Log.e(TAG, "Cannot toggle camera LED");
        }

        // sanity check
        boolean isTorchOnAfter = mCameraDevice.isFlashlightOn();
        Log.v(TAG, "Current torch state should be " + (isTorchOn ? "off" : "on")
                + " and it is " + (isTorchOnAfter ? "on" : "off"));
        assert (isTorchOnAfter == !isTorchOn);
        if (isTorchOnAfter == isTorchOn) {
            Log.e(TAG, "Current torch state after toggle did not change");
            Toast toast = Toast.makeText(getApplicationContext(),
                    R.string.err_cannot_toggle,
                    Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();
            // TODO: maybe try another strategy?
        }

        widgetViews.setImageViewResource(R.id.widgetbutton,
                isTorchOnAfter ? R.drawable.button_on : R.drawable.button_off);

        if (!isTorchOnAfter) {
            // after toggling off, kill this service
            stopSelf();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Create a surface view overlay (for the camera's preview surface).
     */
    private void createOverlay() {
        assert (mOverlay == null);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.FILL_PARENT, 150,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM;

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mOverlay = (LinearLayout) inflater.inflate(R.layout.overlay, null);
        mSurfaceView = (SurfaceView) mOverlay.findViewById(R.id.surfaceview);

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(mOverlay, params);
    }

}
