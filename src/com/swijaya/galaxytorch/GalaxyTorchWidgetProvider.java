package com.swijaya.galaxytorch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class GalaxyTorchWidgetProvider extends AppWidgetProvider {

    private static final String TAG = GalaxyTorchWidgetProvider.class.getSimpleName();

    @Override
    public void onUpdate(Context context,
            AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.v(TAG, "onUpdate");

        // register an event handler (for button click) in this callback
        final int n = appWidgetIds.length;
        for (int i = 0; i < n; i++) {
            int id = appWidgetIds[i];

            // create an intent to launch GalaxyTorchWidgetHelperActivity
            Intent intent = new Intent(context, GalaxyTorchActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            // get the layout for the app widget (RemoteViews); then, attach an onClick
            // listener to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            views.setOnClickPendingIntent(R.id.widgetbutton, pendingIntent);

            // tell the manager to perform an update for the current widget
            appWidgetManager.updateAppWidget(id, views);
        }
    }

}
