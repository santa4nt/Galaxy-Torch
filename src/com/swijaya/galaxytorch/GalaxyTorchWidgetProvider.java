package com.swijaya.galaxytorch;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.util.Log;

public class GalaxyTorchWidgetProvider extends AppWidgetProvider {

    private static final String TAG = GalaxyTorchWidgetProvider.class.getSimpleName();

    @Override
    public void onUpdate(Context context,
            AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.v(TAG, "onUpdate");
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        // TODO Auto-generated method stub
    }

}
