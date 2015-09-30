package com.smartbear.plugins.swaggerhub;

import com.eviware.soapui.plugins.PluginAdapter;
import com.eviware.soapui.plugins.PluginConfiguration;
import com.eviware.soapui.plugins.PluginDependencies;
import com.eviware.soapui.plugins.PluginDependency;

@PluginConfiguration(groupId = "com.smartbear.plugins", name = "SwaggerHub ReadyAPI Plugin", version = "1.0.0",
        autoDetect = true, description = "Integrates Ready API with SwaggerHub",
        infoUrl = "")
@PluginDependencies({
        @PluginDependency(groupId = "com.smartbear.soapui.plugins", name = "Swagger Plugin", minimumVersion = "2.1.3"),
})
public class PluginConfig extends PluginAdapter {

    public final static String SWAGGERHUB_URL = "https://swaggerhub.com";
    public final static String SWAGGERHUB_API = "https://api.swaggerhub.com/apis";
}
