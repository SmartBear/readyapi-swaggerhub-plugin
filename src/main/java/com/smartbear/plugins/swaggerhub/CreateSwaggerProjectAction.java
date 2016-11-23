/**
 * Copyright 2013 SmartBear Software, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smartbear.plugins.swaggerhub;

import com.eviware.soapui.impl.WorkspaceImpl;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.plugins.auto.PluginImportMethod;
import com.eviware.soapui.support.SoapUIException;

@PluginImportMethod(label = "SwaggerHub")
public class CreateSwaggerProjectAction extends ReadFromHubActionBase<WorkspaceImpl> {
    public CreateSwaggerProjectAction() {
        super("Create Swagger Project", "Creates a new Project from a Swagger definition");
    }

    @Override
    WsdlProject getProjectForModelItem(WorkspaceImpl modelItem) throws SoapUIException {
        return modelItem.createProject( "SwaggerHub APIs");
    }
}
