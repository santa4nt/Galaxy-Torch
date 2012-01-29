package com.swijaya.galaxytorch;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
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

    private static final int ONGOING_NOTIFICATION = 1;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * This callback implementations should be called from a thread separate to
     * the task that needs to be done in onStart().
     * 
     * The reason is that the service's life cycle callback onStart() will need
     * to wait for the surface holder to be created, and these callbacks also
     * happen to be called by the system on the same GUI thread as the service's.
     * 
     * @author santa
     *
     */
    private class SurfaceKeeper implements SurfaceHolder.Callback {

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

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate: service starting");

        mCameraDevice = new CameraDevice();
        mCameraDevice.acquireCamera();

        createOverlay();    // this gives us the surface view the camera device needs
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(new SurfaceKeeper());
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

        // remove the overlay
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.removeView(mOverlay);
        mOverlay = null;
        mSurfaceView = null;
    }

    /**
     * A background task worker to toggle the torch on.
     * 
     * We have to resort to using AsyncTask because onCreate() -> onStart()
     * and SurfaceHolder.Callback for the creation of the preview surface
     * defined in the former are all done within a single thread.
     * 
     * As it happens, the service's two life cycle callbacks are most likely
     * to be called first before the SurfaceHolder's. This means that when
     * it comes time to toggle the camera's LED within onStart(), the surface
     * won't be ready. Putting locks around the surface holder won't work
     * without moving the one task in a separate thread. Hence, this
     * AsyncTask definition.
     * 
     * @author santa
     *
     */
    private class TorchToggleTask extends AsyncTask<Void, Void, Boolean> {

        private final AppWidgetManager mAppWidgetManager =
                AppWidgetManager.getInstance(getApplicationContext());
        private ComponentName mThisWidget;
        private boolean mWasTorchOn;

        @Override
        protected void onPreExecute() {
            Log.v(TAG, "onPreExecute");
            mWasTorchOn = mCameraDevice.isFlashlightOn();
            Log.v(TAG, "Current torch state: " + (mWasTorchOn ? "on" : "off"));
            mThisWidget = new ComponentName(getApplicationContext(),
                    GalaxyTorchWidgetProvider.class);
            // set widget background(s) to its pressed state (drawable)
            RemoteViews widgetViews =
                    new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget);
            widgetViews.setImageViewResource(R.id.widgetbutton, R.drawable.lightbulb_widget_on);    // TODO: make an intermediary state
            mAppWidgetManager.updateAppWidget(mThisWidget, widgetViews);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Log.v(TAG, "doInBackground");
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

            // actually toggle the LED (in torch mode)
            return mCameraDevice.toggleCameraLED(!mWasTorchOn);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.v(TAG, "onPostExecute: " + result.toString());
            if (!result) {
                Log.e(TAG, "Cannot toggle camera LED");
            }

            // sanity check
            boolean isTorchOn = mCameraDevice.isFlashlightOn();
            Log.v(TAG, "Current torch state should be " + (mWasTorchOn ? "off" : "on")
                    + " and it is " + (isTorchOn ? "on" : "off"));
            assert (isTorchOn == !mWasTorchOn);
            if (isTorchOn == mWasTorchOn) {
                Log.e(TAG, "Current torch state after toggle did not change");
                Toast toast = Toast.makeText(getApplicationContext(),
                        R.string.err_cannot_toggle,
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();
                // TODO: maybe try another strategy?
            }

            // set widget button(s) image to its appropriate state (drawable)
            RemoteViews widgetViews =
                    new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget);
            widgetViews.setImageViewResource(R.id.widgetbutton,
                    isTorchOn ? R.drawable.lightbulb_widget_on : R.drawable.lightbulb_widget_off);
            mAppWidgetManager.updateAppWidget(mThisWidget, widgetViews);

            if (isTorchOn) {
                Log.v(TAG, "We toggled on. Creating an ongoing notification and start foreground service.");
                // we've turned on the torch; bring the service to foreground and
                // and notify user
                Notification notification = new Notification(
                        R.drawable.lightbulb_notify,
                        getText(R.string.notify_toggle_on),
                        System.currentTimeMillis());
                Intent notificationIntent = new Intent(GalaxyTorchService.this,
                        GalaxyTorchService.class);
                PendingIntent pendingIntent = PendingIntent.getService(
                        GalaxyTorchService.this, 0, notificationIntent, 0);
                notification.setLatestEventInfo(
                        GalaxyTorchService.this,
                        getText(R.string.notify_toggle_on),
                        getText(R.string.notify_toggle_on_ext),
                        pendingIntent);
                startForeground(ONGOING_NOTIFICATION, notification);
            } else {
                // after toggling off, kill this service
                Log.v(TAG, "We toggled off. Stopping service...");
                //stopForeground(true); // should be done through stopSelf() already
                stopSelf();
            }
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        new TorchToggleTask().execute();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Create a surface view overlay (for the camera's preview surface).
     */
    private void createOverlay() {
        assert (mOverlay == null);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,  // technically automatically set by FLAG_NOT_FOCUSABLE
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM;

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mOverlay = (LinearLayout) inflater.inflate(R.layout.overlay, null);
        mSurfaceView = (SurfaceView) mOverlay.findViewById(R.id.surfaceview);

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(mOverlay, params);
    }

}
