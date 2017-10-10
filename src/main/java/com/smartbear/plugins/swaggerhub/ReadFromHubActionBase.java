package com.smartbear.plugins.swaggerhub;

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
import com.smartbear.ready.core.module.ModuleType;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.jdesktop.swingx.JXList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract public class ReadFromHubActionBase<T extends ModelItem> extends AbstractSoapUIAction<T> {

    private final static Logger LOG = LoggerFactory.getLogger(ReadFromHubActionBase.class);
    private XFormDialog dialog;
    private DefaultListModel apis = new DefaultListModel();
    private JXList apisList;

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
                    dialog.getActionsList().getActionAt(0).setEnabled(versionsField.getSelectedIndexes().length > 0);
                }
            });
        }

        try {
            populateList();

            while (dialog.show()) {

                XFormOptionsField versionsField = ((XFormOptionsField) dialog.getFormField(Form.VERSIONS));

                final int[] versions = versionsField.getSelectedIndexes();
                final int apiIndex = apisList.getSelectedIndex();
                final WsdlProject project = getProjectForModelItem(modelItem);

                if (versions.length == 0 || apiIndex == -1) {
                    UISupport.showErrorMessage("Select an API version to import");
                } else {
                    final List<Object> result = Lists.newArrayList();
                    final XProgressDialog progressDialog = UISupport.getDialogs().createProgressDialog(
                            "Importing definition from SwaggerHub", 0, "Importing...", false);

                    progressDialog.run(new Worker.WorkerAdapter() {
                        @Override
                        public Object construct(XProgressMonitor xProgressMonitor) {
                            result.addAll(importApis(project, apiIndex, versions));

                            return null;
                        }
                    });
                    if (!result.isEmpty()) {
                        UISupport.selectAndShow(project, ModuleType.PROJECTS.getId());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            UISupport.showErrorMessage(e);
        }
    }

    private List<Object> importApis(WsdlProject wsdlProject, int apiIndex, int[] versions) {
        List<Object> result = new ArrayList<>();
        try {
            Class importerClass = Class.forName("com.smartbear.swagger.Swagger2Importer");
            Object importer = importerClass.getConstructor(WsdlProject.class).newInstance(wsdlProject);
            Method method = importerClass.getMethod("importSwagger", String.class);

            ApiDescriptor descriptor = (ApiDescriptor) apis.get(apiIndex);
            String swaggerUrl = descriptor.swaggerUrl;

            for (int versionIndex : versions) {
                String version = descriptor.versions[versionIndex];
                if (version.startsWith("*")) {
                    version = version.substring(1).trim();
                }

                String url = swaggerUrl.substring(0, swaggerUrl.lastIndexOf('/')) + "/" + version;
                System.out.println("Attempting to import Swagger from [" + url + "]");
                Collections.addAll(result, method.invoke(importer, swaggerUrl));
            }
        } catch (Exception e) {
            LOG.error(e.toString());
        }
        return result;
    }

    private JComponent buildApisListComponent() {
        apisList = new JXList(apis);
        apisList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateVersions();
            }
        });

        return new JScrollPane(apisList);
    }

    private void updateVersions() {
        int index = apisList.getSelectedIndex();
        XFormOptionsField versionsField = ((XFormOptionsField) dialog.getFormField(Form.VERSIONS));

        StringList versions = new StringList();
        if (index >= 0) {

            for (String v : ((ApiDescriptor) apis.get(index)).versions) {
                if (v.startsWith("*")) {
                    v = v.substring(1).trim() + " - published";
                }

                versions.add(v);
            }
        }

        versionsField.setOptions(versions.toStringArray());
    }

    private void populateList() throws IOException {
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

        List<ApiDescriptor> descriptors = new ApisJsonImporter().importApis(
                new String(ByteStreams.toByteArray(response.getEntity().getContent())));

        for (ApiDescriptor descriptor : descriptors) {
            apis.addElement(descriptor);
        }
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
                populateList();
            } catch (IOException e1) {
                UISupport.showErrorMessage(e1);
            } finally {
                UISupport.resetCursor();
            }
        }
    }
}
