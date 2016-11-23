package com.smartbear.plugins.swaggerhub;

import com.eviware.soapui.actions.Prefs;
import com.eviware.soapui.model.settings.Settings;
import com.eviware.soapui.plugins.auto.PluginPrefs;
import com.eviware.soapui.support.components.SimpleForm;
import com.eviware.soapui.support.types.StringToStringMap;

import static com.smartbear.plugins.swaggerhub.PluginConfig.SWAGGERHUB_API;

@PluginPrefs
public class SwaggerHubPreferences implements Prefs {

    private static final String SWAGGERHUB_SERVER_LABEL = "SwaggerHub API Endpoint";
    private static final String API_KEY_LABEL = "API Key";
    static final String SWAGGERHUB_API_BASEPATH_KEY = "SwaggerHubServer";
    static final String API_KEY_KEY = "SwaggerHubApiKey";
    private SimpleForm prefsForm;

    public SwaggerHubPreferences() {
        createForm();
    }

    private void createForm() {
        prefsForm = new SimpleForm();
        prefsForm.appendTextField(SWAGGERHUB_SERVER_LABEL, "Endpoint to SwaggerHub API");
        prefsForm.appendTextField(API_KEY_LABEL, "SwaggerHub API Key for private APIs access");
    }

    @Override
    public SimpleForm getForm() {
        return prefsForm;
    }

    @Override
    public void setFormValues(Settings settings) {
        prefsForm.setValues(getValues(settings));
    }

    @Override
    public void getFormValues(Settings settings) {
        StringToStringMap stringMap = new StringToStringMap();
        prefsForm.getValues(stringMap);
        storeValues(stringMap, settings);
    }

    @Override
    public void storeValues(StringToStringMap stringToStringMap, Settings settings) {
        settings.setString(SWAGGERHUB_API_BASEPATH_KEY, stringToStringMap.get(SWAGGERHUB_SERVER_LABEL));
        settings.setString(API_KEY_KEY, stringToStringMap.get(API_KEY_LABEL));
    }

    @Override
    public StringToStringMap getValues(Settings settings) {
        StringToStringMap stringMap = new StringToStringMap();
        stringMap.put(SWAGGERHUB_SERVER_LABEL, settings.getString(SWAGGERHUB_API_BASEPATH_KEY, SWAGGERHUB_API));
        stringMap.put(API_KEY_LABEL, settings.getString(API_KEY_KEY, ""));
        return stringMap;
    }

    @Override
    public String getTitle() {
        return "SwaggerHub";
    }
}
