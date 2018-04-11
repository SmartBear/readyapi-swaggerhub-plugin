package com.smartbear.plugins.swaggerhub;

import com.eviware.soapui.plugins.PluginAdapter;
import com.eviware.soapui.plugins.PluginConfiguration;
import com.eviware.soapui.plugins.PluginDependencies;
import com.eviware.soapui.plugins.PluginDependency;

@PluginConfiguration(groupId = "com.smartbear.plugins", name = "SwaggerHub ReadyAPI Plugin", version = "1.2.0",
        autoDetect = true, description = "Integrates Ready API with SwaggerHub",
        infoUrl = "")
public class PluginConfig extends PluginAdapter {

    public final static String SWAGGERHUB_URL = "https://swaggerhub.com";
    public final static String SWAGGERHUB_API = "https://api.swaggerhub.com/apis";
}
