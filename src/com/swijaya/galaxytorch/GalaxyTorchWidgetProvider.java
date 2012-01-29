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

    //public static final String TORCH_TOGGLE_ACTION = "com.swijaya.galaxytorch.toggle_action";

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
            Intent intent = new Intent(context, GalaxyTorchService.class);
            //intent.setAction(TORCH_TOGGLE_ACTION);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

            // get the layout for the app widget (RemoteViews); then, attach an onClick
            // listener to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            views.setOnClickPendingIntent(R.id.widgetbutton, pendingIntent);

            // tell the manager to perform an update for the current widget
            appWidgetManager.updateAppWidget(id, views);
        }
    }

}
