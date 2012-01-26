package com.swijaya.galaxytorch;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class GalaxyTorchWidgetReceiver extends BroadcastReceiver {

    private static final String TAG = GalaxyTorchWidgetReceiver.class.getSimpleName();

    private static CameraDevice sCameraDevice = new CameraDevice();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive");

        // toggle the torch
        boolean isFlashlightOn = sCameraDevice.isFlashlightOn();
        if (isFlashlightOn) {
            // we're turning OFF the torch
            sCameraDevice.releaseCamera();
        } else {
            // we're turning ON the torch
            if (!sCameraDevice.acquireCamera()) {
                Log.e(TAG, "Cannot acquire camera");
                Toast.makeText(context,
                        R.string.err_cannot_acquire,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success = false;
            try {
                sCameraDevice.getCamera().startPreview();
                success = sCameraDevice.toggleCameraLED(true);
            }
            catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
            if (!success) {
                Log.e(TAG, "Cannot toggle camera LED to torch mode");
                Toast.makeText(context,
                        R.string.err_cannot_toggle,
                        Toast.LENGTH_SHORT).show();
            }
        }

        // update widgets' button state
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        isFlashlightOn = sCameraDevice.isFlashlightOn();
        views.setImageViewResource(R.id.widgetbutton,
                isFlashlightOn ? R.drawable.button_on : R.drawable.button_off);
        appWidgetManager.updateAppWidget(
                new ComponentName(context, GalaxyTorchWidgetProvider.class),    // for this provider,
                views);     // set the same (remote) views to use for all widgets by said provider
    }

}
