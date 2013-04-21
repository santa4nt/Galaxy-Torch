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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class GalaxyTorchWidgetProvider extends AppWidgetProvider {

    private static final String TAG = GalaxyTorchWidgetProvider.class.getSimpleName();

    //public static final String TORCH_TOGGLE_ACTION = "com.swijaya.galaxytorch.toggle_action";
    private static Boolean KEYGUARD_SUPPORTED = null;

    private static boolean isKeyguardWidget(AppWidgetManager manager, int widgetId) {
        if (KEYGUARD_SUPPORTED != null && !KEYGUARD_SUPPORTED)
            return false;

        Bundle options;
        try {
            options = manager.getAppWidgetOptions(widgetId);
        } catch (NoSuchMethodError e) {
            KEYGUARD_SUPPORTED = false;
            return false;
        }

        int category = options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1);
        return category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
    }

    @Override
    public void onUpdate(Context context,
            AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.v(TAG, "onUpdate");

        // register an event handler (for button click) in this callback
        for (int id : appWidgetIds) {
            if (isKeyguardWidget(appWidgetManager, id)) {
                Log.v(TAG, "Keyguard widget update");
                // TODO
            }
            //else {    // for now, make keyguard (lock screen) widget works like regular home screen widget
                // create an intent to launch the service that activates the overlay and turns on the torch
                Intent intent = new Intent(context, GalaxyTorchService.class);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

                // get the layout for the app widget (RemoteViews); then, attach an onClick
                // listener to the button
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
                views.setOnClickPendingIntent(R.id.widgetbutton, pendingIntent);

                // tell the manager to perform an update for the current widget
                appWidgetManager.updateAppWidget(id, views);
            //}
        }
    }

}
