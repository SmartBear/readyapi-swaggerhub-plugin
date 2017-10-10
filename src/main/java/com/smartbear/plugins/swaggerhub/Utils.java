package com.smartbear.plugins.swaggerhub;

import com.smartbear.analytics.Analytics;

import java.util.HashMap;
import java.util.Map;

import static com.smartbear.analytics.AnalyticsManager.Category.CUSTOM_PLUGIN_ACTION;

public class Utils {
    public static void sendAnalytics(String action) {
        Map<String, String> params = new HashMap();
        params.put("SourceModule", "");
        params.put("ProductArea", "MainMenu");
        params.put("Type", "REST");
        params.put("Source", "SwaggerHub");
        Analytics.getAnalyticsManager().trackAction(CUSTOM_PLUGIN_ACTION, action, params);
    }
}
