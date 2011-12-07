package com.matburt.mobileorg.Services;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.NodeEditActivity;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.Node;
import com.matburt.mobileorg.Parsing.OrgFileParser;

public class MobileOrgWidget extends AppWidgetProvider {
    public static String WIDGET_CAPTURE = "WIDGET_CAPTURE";

   @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // To prevent any ANR timeouts, we perform the update in a service
       context.startService(new Intent(context,
                                       MobileOrgWidgetService.class));
    }

    public static class MobileOrgWidgetService extends Service {

    	@Override
        public void onStart(Intent intent, int startId) {
            RemoteViews updateViews = this.genUpdateDisplay(this);
            ComponentName thisWidget = new ComponentName(this,
                                                         MobileOrgWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        public RemoteViews genUpdateDisplay(Context context) {
            RemoteViews updateViews = null;
            updateViews = new RemoteViews(context.getPackageName(),
                                          R.layout.widget);
            
            Intent intent = new Intent(context, NodeEditActivity.class);
            intent.setAction(NodeEditActivity.ACTIONMODE_CREATE);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            updateViews.setOnClickPendingIntent(R.id.widget, pendingIntent);

            MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
            OrgFileParser ofp = new OrgFileParser(getBaseContext(), appInst);

			Node agendaNode = ofp.parseFile("agendas.org", null);
			if (agendaNode != null) {
				Node todoNode = agendaNode.findChildNode("Today");
				if (todoNode != null) {
					todoNode = todoNode.getChildren().get(0);
					if (todoNode != null) {
						String widgetBuffer = "";
						for (Node child : todoNode.getChildren()) {
							widgetBuffer = widgetBuffer + child.name + "\n";
						}
						updateViews.setTextViewText(R.id.message, widgetBuffer);
					}
				}
			}
            return updateViews;
        }

        
    	public void click() {
    		Log.d("mobileorg", "click de click!");
    	}

        public String getStorageLocation(Context context) {
            SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            return appPrefs.getString("storageMode", "");
        }

        @Override
        public IBinder onBind(Intent intent) {
            // We don't need to bind to this service
            return null;
        }
    }
}