/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jessies.dalvikexplorer;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

public class BuildWidget extends AppWidgetProvider {
  @Override public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    for (int appWidgetId : appWidgetIds) {
      updateAppWidget(context, appWidgetManager, appWidgetId);
    }
  }

  public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
    RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.build_widget);

    PendingIntent pendingIntent = PendingIntent.getActivity(context,
                                                            0 /* no requestCode */,
                                                            new Intent("android.settings.SYSTEM_UPDATE_SETTINGS"),
                                                            0 /* no flags */);
    updateViews.setOnClickPendingIntent(R.id.widget, pendingIntent);

    updateViews.setTextViewText(R.id.build_info, android.os.Build.ID);
    updateViews.setTextViewText(R.id.build_product, android.os.Build.PRODUCT);
    updateViews.setTextViewText(R.id.build_type, android.os.Build.TYPE);
    updateViews.setTextViewText(R.id.build_date, DateFormat.format("yyyy-MM-dd", android.os.Build.TIME));

    appWidgetManager.updateAppWidget(appWidgetId, updateViews);
  }
}
