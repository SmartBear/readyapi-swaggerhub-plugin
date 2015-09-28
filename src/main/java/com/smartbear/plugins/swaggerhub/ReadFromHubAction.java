package com.smartbear.plugins.swaggerhub;

import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.plugins.ActionConfiguration;
import com.eviware.soapui.support.SoapUIException;

@ActionConfiguration(actionGroup = "EnabledWsdlProjectActions", afterAction = "ExportSwaggerAction")
public class ReadFromHubAction extends ReadFromHubActionBase<WsdlProject> {

    public ReadFromHubAction() {
        super("Import from SwaggerHub", "Reads an API from SwaggerHub");
    }

    @Override
    WsdlProject getProjectForModelItem(WsdlProject modelItem) throws SoapUIException {
        return modelItem;
    }
}
