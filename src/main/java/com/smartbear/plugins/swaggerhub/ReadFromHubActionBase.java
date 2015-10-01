package com.smartbear.plugins.swaggerhub;

import com.eviware.soapui.analytics.Analytics;
import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.wsdl.AbstractWsdlModelItem;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.support.http.HttpClientSupport;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.support.SoapUIException;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.x.dialogs.Worker;
import com.eviware.x.dialogs.XProgressDialog;
import com.eviware.x.dialogs.XProgressMonitor;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormOptionsField;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AForm;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.smartbear.swagger.Swagger2Importer;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract public class  ReadFromHubActionBase<T extends ModelItem> extends AbstractSoapUIAction<T> {

    private final static Logger LOG = LoggerFactory.getLogger(ReadFromHubActionBase.class);
    private XFormDialog dialog;
    List<ApiDescriptor> apis = new ArrayList<>();

    public ReadFromHubActionBase(String name, String description) {
        super(name, description);
    }

    abstract WsdlProject getProjectForModelItem( T modelItem ) throws SoapUIException;

    public void perform( final T modelItem, Object o) {

        if (dialog == null) {
            dialog = ADialogBuilder.buildDialog(Form.class);
            dialog.getFormField(Form.SEARCH).setProperty("action", new SearchAction());
        }

        try {
            XFormOptionsField field = populateList();

            if (dialog.show()) {

                final int[] indexes = field.getSelectedIndexes();
                if (indexes.length == 0) {
                    UISupport.showErrorMessage("Select one or more APIs to import.");
                } else {
                    final List<RestService> result = Lists.newArrayList();
                    final XProgressDialog progressDialog = UISupport.getDialogs().createProgressDialog(
                            "Importing definition" + (indexes.length == 1?"":"s") + " from SwaggerHub", 0, "Importing...", false);

                    progressDialog.run(new Worker.WorkerAdapter() {
                        @Override
                        public Object construct(XProgressMonitor xProgressMonitor) {
                            try {
                                result.addAll(importApis(getProjectForModelItem(modelItem), indexes));
                            } catch (SoapUIException e) {
                                UISupport.showErrorMessage(e);
                            }
                            return null;
                        }
                    });

                    if( !result.isEmpty()){
                        UISupport.selectAndShow( result.get(0));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<RestService> importApis(WsdlProject wsdlProject, int[] indexes) {
        Swagger2Importer importer = new Swagger2Importer(wsdlProject);
        List<RestService> result = Lists.newArrayList();
        for (int c = 0; c < indexes.length; c++) {
            String swaggerUrl = apis.get(indexes[c]).swaggerUrl;
            System.out.println("Attempting to import Swagger from [" + swaggerUrl + "]");
            Collections.addAll(result, importer.importSwagger(swaggerUrl));
            Analytics.trackAction("ImportFromSwaggerHubAction");
        }

        return result;
    }

    private XFormOptionsField populateList() throws IOException {
        apis.clear();
        String uri = PluginConfig.SWAGGERHUB_API + "?limit=50";

        String query = dialog.getValue(Form.QUERY);
        if (StringUtils.isNotBlank(query)) {
            uri += "&query=" + URLEncoder.encode(query.trim());
        }

        LOG.debug("Reading APIs from uri");

        HttpGet get = new HttpGet(uri);
        HttpResponse response = HttpClientSupport.getHttpClient().execute(get);

        LOG.debug("Got APIs, parsing...");

        apis.addAll(new ApisJsonImporter().importApis(
                new String(ByteStreams.toByteArray(response.getEntity().getContent()))));

        XFormOptionsField field = (XFormOptionsField) dialog.getFormField(Form.APIS);
        field.setOptions(apis.toArray());
        return field;
    }

    @AForm(name = "Import Definition From SwaggerHub", description = "Imports a Swagger definition from SwaggerHub.")
    public interface Form {
        @AField(name = "Search in API name", description = "A substring to search for in API names.", type = AField.AFieldType.STRING)
        public final static String QUERY = "Search in API name";

        @AField(name = "Search", description = "Update the Available APIs list.", type = AField.AFieldType.ACTION)
        public final static String SEARCH = "Search";

        @AField(name = "Available APIs", description = "Select the APIs to import.", type = AField.AFieldType.MULTILIST)
        public final static String APIS = "Available APIs";
    }

    private class SearchAction extends AbstractAction {

        public SearchAction() {
            super("Search");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                UISupport.setHourglassCursor();
                populateList();
            } catch (IOException e1) {
                UISupport.showErrorMessage(e1);
            } finally {
                UISupport.resetCursor();
            }
        }
    }
}
