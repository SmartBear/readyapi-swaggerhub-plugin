package com.smartbear.plugins.swaggerhub;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.support.http.HttpClientSupport;
import com.eviware.soapui.plugins.ActionConfiguration;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormOptionsField;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AForm;
import com.google.common.io.ByteStreams;
import com.smartbear.swagger.Swagger2Importer;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@ActionConfiguration(actionGroup = "EnabledWsdlProjectActions", afterAction = "ExportSwaggerAction")
public class ReadFromHubAction extends AbstractSoapUIAction<WsdlProject> {

    private final static Logger LOG = LoggerFactory.getLogger(ReadFromHubAction.class);
    private XFormDialog dialog;
    List<ApiDescriptor> apis = new ArrayList<>();

    public ReadFromHubAction() {
        super("Import from SwaggerHub", "Reads an API from the SwaggerHub");
    }

    public void perform(WsdlProject wsdlProject, Object o) {

        if (dialog == null) {
            dialog = ADialogBuilder.buildDialog(Form.class);
            dialog.getFormField( Form.SEARCH ).setProperty("action", new SearchAction());
        }

        try {
            XFormOptionsField field = populateList();

            if (dialog.show()) {

                int[] indexes = field.getSelectedIndexes();
                if (indexes.length == 0) {
                    UISupport.showErrorMessage("You need to select at least on API");
                } else {
                    Swagger2Importer importer = new Swagger2Importer(wsdlProject);
                    for (int c = 0; c < indexes.length; c++) {
                        String swaggerUrl = apis.get(indexes[c]).swaggerUrl;
                        System.out.println("Attempting to import Swagger from [" + swaggerUrl + "]");
                        importer.importSwagger(swaggerUrl);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private XFormOptionsField populateList() throws IOException {
        apis.clear();
        String uri = PluginConfig.SWAGGERHUB_API + "?limit=50";

        String query = dialog.getValue( Form.QUERY );
        if(StringUtils.isNotBlank(query)){
            uri += "&" + URLEncoder.encode( query.trim());
        }

        LOG.debug( "Reading APIs from uri" );

        HttpGet get = new HttpGet(uri);
        HttpResponse response = HttpClientSupport.getHttpClient().execute(get);

        LOG.debug("Got APIs, parsing..." );

        apis.addAll(new ApisJsonImporter().importApis(
                new String(ByteStreams.toByteArray(response.getEntity().getContent()))));

        XFormOptionsField field = (XFormOptionsField) dialog.getFormField(Form.APIS);
        field.setOptions(apis.toArray());
        return field;
    }

    @AForm(name = "Import Swagger Definition", description = "Imports a Swagger definition from the SwaggerHub")
    public interface Form {
        @AField(name = "Query", description = "keywords to search on", type = AField.AFieldType.STRING)
        public final static String QUERY = "Query";

        @AField(name = "APIs", description = "Select which Swagger APIs to import", type = AField.AFieldType.MULTILIST)
        public final static String APIS = "APIs";

        @AField( name= "Search", description = "Update list of APIs", type = AField.AFieldType.ACTION )
        public final static String SEARCH = "Search";
    }

    private class SearchAction extends AbstractAction {

        public SearchAction(){
            super("Search");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                UISupport.setHourglassCursor();
                populateList();
            } catch (IOException e1) {
                UISupport.showErrorMessage(e1);
            }
            finally {
                UISupport.resetCursor();
            }
        }
    }
}
