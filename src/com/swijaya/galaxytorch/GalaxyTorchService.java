/**
 * Copyright (c) 2012 Santoso Wijaya
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.swijaya.galaxytorch;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
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

    private enum WidgetState {
        OFF     (R.drawable.widget_light),
        ON      (R.drawable.wg_light_on),
        FOCUS   (R.drawable.wg_light_focus);

        /**
         * The drawable resource associated with this widget state.
         */
        private final int mDrawRes;

        private WidgetState(int drawRes) {
            mDrawRes = drawRes;
        }

        public int getDrawable() {
            return mDrawRes;
        }
    }

    private RemoteViews createWidgetWithState(WidgetState state) {
        Context ctx = getApplicationContext();
        RemoteViews widgetViews =
                new RemoteViews(ctx.getPackageName(), R.layout.widget);
        widgetViews.setImageViewResource(R.id.widgetbutton, state.getDrawable());
        switch (state) {
        case OFF:
        case ON:
            Log.v(TAG, "Renewing pending intent for widget");
            // refresh the button's onClick pending intent
            // create an intent to launch GalaxyTorchWidgetHelperActivity
            Intent intent = new Intent(ctx, GalaxyTorchService.class);
            //intent.setAction(TORCH_TOGGLE_ACTION);
            PendingIntent pendingIntent = PendingIntent.getService(ctx, 0, intent, 0);
            widgetViews.setOnClickPendingIntent(R.id.widgetbutton, pendingIntent);
            break;
        }

        return widgetViews;
    }

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

        private boolean mWasTorchOn;

        private AppWidgetManager mAppWidgetManager;
        private ComponentName mThisWidget;

        @Override
        protected void onPreExecute() {
            Log.v(TAG, "onPreExecute");
            mWasTorchOn = mCameraDevice.isFlashlightOn();
            Log.v(TAG, "Current torch state: " + (mWasTorchOn ? "on" : "off"));

            // set widget background(s) to its focused state (drawable)
            mAppWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
            mThisWidget = new ComponentName(getApplicationContext(),
                    GalaxyTorchWidgetProvider.class);
            RemoteViews widget = createWidgetWithState(WidgetState.FOCUS);
            mAppWidgetManager.updateAppWidget(mThisWidget, widget);
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
                return false;
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
            RemoteViews widget = createWidgetWithState(
                    isTorchOn ? WidgetState.ON : WidgetState.OFF);
            mAppWidgetManager.updateAppWidget(mThisWidget, widget);

            if (isTorchOn) {
                Log.v(TAG, "We toggled on. Creating an ongoing notification and start foreground service.");
                // we've turned on the torch; bring the service to foreground and
                // and notify user
                startForeground(ONGOING_NOTIFICATION, createToggleNotification());
            } else {
                // after toggling off, kill this service
                Log.v(TAG, "We toggled off. Stopping service...");
                //stopForeground(true); // stopSelf() would also remove notification
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

    /**
     * Create a status bar notification to tell the user that we are holding
     * the camera device's resources and setting its flashlight LED's torch
     * mode on.
     */
    private Notification createToggleNotification() {
        int icon = R.drawable.lightbulb_notify;
        CharSequence tickerText = getText(R.string.notify_toggle_on);
        long when = System.currentTimeMillis();
        Context context = getApplicationContext();
        CharSequence contentTitle = getText(R.string.notify_toggle_on);
        CharSequence contentText = getText(R.string.notify_toggle_on_ext);

        Intent notificationIntent = new Intent(this, GalaxyTorchService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);

        Notification notification = new Notification(icon, tickerText, when);
        notification.setLatestEventInfo(context, contentTitle, contentText, pendingIntent);

        return notification;
    }

}
