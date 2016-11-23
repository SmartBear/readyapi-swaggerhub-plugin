package com.smartbear.plugins.swaggerhub;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.model.settings.Settings;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

public class Utils {
    public static String getSwaggerHubApiBasePath() {
        Settings settings = SoapUI.getSettings();
        String endpoint = settings.getString(SwaggerHubPreferences.SWAGGERHUB_API_BASEPATH_KEY, PluginConfig.SWAGGERHUB_API);
        if( !endpoint.endsWith("/")){
            endpoint += "/";
        }

        return endpoint + "apis";
    }

    public static void addApiKeyIfConfigured(HttpRequestBase get) {
        Settings settings = SoapUI.getSettings();
        String apiKey = settings.getString(SwaggerHubPreferences.API_KEY_KEY, null);

        if( StringUtils.isNotEmpty( apiKey )){
            get.addHeader("Authorization", apiKey );
        }
    }
}
