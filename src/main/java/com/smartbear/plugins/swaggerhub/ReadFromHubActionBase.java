package com.smartbear.plugins.swaggerhub;

import com.eviware.soapui.analytics.Analytics;
import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.support.http.HttpClientSupport;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.support.SoapUIException;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.soapui.support.types.StringList;
import com.eviware.x.dialogs.Worker;
import com.eviware.x.dialogs.XProgressDialog;
import com.eviware.x.dialogs.XProgressMonitor;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormField;
import com.eviware.x.form.XFormFieldListener;
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
import org.jdesktop.swingx.JXTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

abstract public class ReadFromHubActionBase<T extends ModelItem> extends AbstractSoapUIAction<T> {

    private final static Logger LOG = LoggerFactory.getLogger(ReadFromHubActionBase.class);
    private XFormDialog dialog;
    private JXTable apisList;
    private ApisTableModel apisTableModel = new ApisTableModel();

    public ReadFromHubActionBase(String name, String description) {
        super(name, description);
    }

    abstract WsdlProject getProjectForModelItem(T modelItem) throws SoapUIException;

    public void perform(final T modelItem, Object o) {

        if (dialog == null) {
            dialog = ADialogBuilder.buildDialog(Form.class);
            dialog.getFormField(Form.SEARCH).setProperty("action", new SearchAction());

            dialog.getFormField(Form.APIS).setProperty("component", buildApisListComponent());
            updateVersions();

            final XFormOptionsField versionsField = ((XFormOptionsField) dialog.getFormField(Form.VERSIONS));
            versionsField.addFormFieldListener(new XFormFieldListener() {
                @Override
                public void valueChanged(XFormField xFormField, String s, String s1) {
                   dialog.getActionsList().getActionAt(0).setEnabled(versionsField.getSelectedIndexes().length>0);
                }
            });
        }

        try {
            populateApiList();

            while (dialog.show()) {

                XFormOptionsField versionsField = ((XFormOptionsField) dialog.getFormField(Form.VERSIONS));

                final int[] versions = versionsField.getSelectedIndexes();
                final int apiIndex = apisList.getSelectedRow();

                if (versions.length == 0 || apiIndex == -1) {
                    UISupport.showErrorMessage("Select an API version to import");
                } else {
                    final List<RestService> result = Lists.newArrayList();
                    final XProgressDialog progressDialog = UISupport.getDialogs().createProgressDialog(
                            "Importing definition from SwaggerHub", 0, "Importing...", false);

                    progressDialog.run(new Worker.WorkerAdapter() {
                        @Override
                        public Object construct(XProgressMonitor xProgressMonitor) {
                            try {
                                result.addAll(importApis(getProjectForModelItem(modelItem), apiIndex, versions));
                            } catch (SoapUIException e) {
                                UISupport.showErrorMessage(e);
                            }
                            return null;
                        }
                    });

                    if (!result.isEmpty()) {
                        UISupport.selectAndShow(result.get(0));
                        break;
                    }
                }
            }

        } catch (Exception e) {
            UISupport.showErrorMessage(e);
        }
    }

    private JComponent buildApisListComponent() {
        apisList = new JXTable(apisTableModel);
        apisList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateVersions();
            }
        });

        return new JScrollPane(apisList);
    }

    private void updateVersions() {
        int index = apisList.getSelectedRow();
        XFormOptionsField versionsField = ((XFormOptionsField) dialog.getFormField(Form.VERSIONS));

        StringList versions = new StringList();
        if (index >= 0) {

            for (String v : apisTableModel.getApiAtRow(index).versions) {
                if (v.startsWith("*")) {
                    v = v.substring(1).trim() + " - published";
                }

                versions.add(v);
            }
        }

        versionsField.setOptions(versions.toStringArray());
    }

    private List<RestService> importApis(WsdlProject wsdlProject, int apiIndex, int [] versions) {

        ApiDescriptor descriptor = apisTableModel.getApiAtRow(apiIndex);

        Swagger2Importer importer = new Swagger2Importer(wsdlProject);
        List<RestService> result = Lists.newArrayList();
        String swaggerUrl = descriptor.swaggerUrl;

        for( int versionIndex : versions ) {

            String version = descriptor.versions[versionIndex];
            if (version.startsWith("*")) {
                version = version.substring(1).trim();
            }

            String url = swaggerUrl.substring(0, swaggerUrl.lastIndexOf('/')) + "/" + version;

            System.out.println("Attempting to import Swagger from [" + url + "]");
            Collections.addAll(result, importer.importSwagger(url));
        }

        Analytics.trackAction("ImportFromSwaggerHubAction");

        return result;
    }

    private void populateApiList() throws IOException {

        String uri = Utils.getSwaggerHubApiBasePath() + "?limit=50";

        String query = dialog.getValue(Form.QUERY);
        if (StringUtils.isNotBlank(query)) {
            uri += "&query=" + URLEncoder.encode(query.trim());
        }

        LOG.debug("Reading APIs from uri");

        HttpGet get = new HttpGet(uri);
        Utils.addApiKeyIfConfigured(get);

        HttpResponse response = HttpClientSupport.getHttpClient().execute(get);

        LOG.debug("Got APIs, parsing...");

        apisTableModel.setApis( new ApisJsonImporter().importApis(
                new String(ByteStreams.toByteArray(response.getEntity().getContent()))));
    }

    @AForm(name = "Import Definition From SwaggerHub", description = "Imports a Swagger definition from SwaggerHub.")
    public interface Form {
        @AField(name = "Search in API name", description = "A substring to search for in API names.", type = AField.AFieldType.STRING)
        public final static String QUERY = "Search in API name";

        @AField(name = "Search", description = "Update the Available APIs list.", type = AField.AFieldType.ACTION)
        public final static String SEARCH = "Search";

        @AField(name = "Available APIs", description = "Select the APIs to import.", type = AField.AFieldType.COMPONENT)
        public final static String APIS = "Available APIs";

        @AField(name = "Versions", description = "Select the versions of this API to import.", type = AField.AFieldType.MULTILIST)
        public final static String VERSIONS = "Versions";
    }

    private class SearchAction extends AbstractAction {

        public SearchAction() {
            super("Search");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                UISupport.setHourglassCursor();
                populateApiList();
            } catch (IOException e1) {
                UISupport.showErrorMessage(e1);
            } finally {
                UISupport.resetCursor();
            }
        }
    }
}
