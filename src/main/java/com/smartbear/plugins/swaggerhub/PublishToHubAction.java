package com.smartbear.plugins.swaggerhub;

import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.wsdl.support.http.HttpClientSupport;
import com.eviware.soapui.model.settings.Settings;
import com.eviware.soapui.plugins.ActionConfiguration;
import com.eviware.soapui.support.Tools;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.x.dialogs.Worker;
import com.eviware.x.dialogs.XProgressDialog;
import com.eviware.x.dialogs.XProgressMonitor;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AForm;
import com.google.common.io.Files;
import com.smartbear.swagger.Swagger2Exporter;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

@ActionConfiguration(actionGroup = "RestServiceActions", separatorBefore = true)
public class PublishToHubAction extends AbstractSoapUIAction<RestService> {

    private static final Logger LOG = LoggerFactory.getLogger(PublishToHubAction.class);
    public static final String SWAGGER_HUB_API_KEY = "SwaggerHubApiKey";
    private XFormDialog dialog;

    public PublishToHubAction() {
        super("Publish to SwaggerHub", "Publishes this API to SwaggerHub");
    }

    public void perform(final RestService restService, Object o) {

        Settings settings = restService.getProject().getSettings();
        if (dialog == null) {
            dialog = ADialogBuilder.buildDialog(Form.class);
            dialog.setValue(Form.APIKEY, settings.getString(SWAGGER_HUB_API_KEY, ""));
        }

        final boolean[] finished = {false};
        while (!finished[0] && dialog.show()) {
            XProgressDialog progressDialog = UISupport.getDialogs().createProgressDialog("Publish to SwaggerHub", 0, "Importing...", false);
            try {
                progressDialog.run(new Worker.WorkerAdapter() {
                    @Override
                    public Object construct(XProgressMonitor xProgressMonitor) {
                        try {
                            finished[0] = publishApi(restService);
                        } catch (IOException e) {
                            UISupport.showErrorMessage(e);
                        }
                        return null;
                    }
                });
            } catch (Throwable e) {
                UISupport.showErrorMessage(e);
            }
        }
    }

    private boolean publishApi(RestService restService) throws IOException {
        try {
            String apikey = dialog.getValue(Form.APIKEY);
            String groupId = dialog.getValue(Form.GROUP_ID);
            String apiId = dialog.getValue(Form.API_ID);
            String versionId = dialog.getValue(Form.VERSION);
            boolean browse = dialog.getBooleanValue(Form.BROWSE);
            boolean remember = dialog.getBooleanValue(Form.REMEMBER);

            String uri = PluginConfig.SWAGGERHUB_API + "/" + groupId + "/" + apiId;
            CloseableHttpClient client = HttpClientSupport.getHttpClient();

            HttpGet get = new HttpGet(uri + "/" + versionId);
            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                if (!UISupport.confirm("API Version [" + versionId + "] already exists at SwaggerHub - Overwrite?",
                        "Publish to SwaggerHub")) {
                    return false;
                }
            }

            Swagger2Exporter exporter = new Swagger2Exporter(restService.getProject());
            String result = exporter.exportToFileSystem(Files.createTempDir().getAbsolutePath(), versionId, "json", new RestService[]{restService}, restService.getBasePath());

            LOG.info("Created temporary Swagger definition at " + result);
            HttpPost post = new HttpPost(uri);
            post.setEntity(new FileEntity(new File(result, "api-docs.json"), "application/json"));
            post.addHeader("Authorization", apikey);

            LOG.info("Posting definition to " + uri);
            response = client.execute(post);

            restService.getProject().getWorkspace().getSettings().setString(SWAGGER_HUB_API_KEY, remember ? apikey : "");

            if (response.getStatusLine().getStatusCode() == 201) {
                UISupport.showInfoMessage("API published successfully");
                if (browse) {
                    Tools.openURL(PluginConfig.SWAGGERHUB_URL + "/api/" + groupId + "/" + apiId + "/" + versionId);
                }

                Utils.sendAnalytics("ExportToSwaggerHubAction");
                return true;
            } else {
                UISupport.showErrorMessage("Failed to publish API; " + response.getStatusLine().toString());
                return false;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return true;
    }

    @AForm(name = "Publish Definition to SwaggerHub", description = "Publishes the selected REST definition to SwaggerHub (in the Swagger 2.0 format).")
    public interface Form {
        @AField(name = "Owner", description = "Your SwaggerHub account.", type = AField.AFieldType.STRING)
        public final static String GROUP_ID = "Owner";

        @AField(name = "Unique API name", description = "The API identifier at SwaggerHub (letters, digits or spaces, 3 chars min).", type = AField.AFieldType.STRING)
        public final static String API_ID = "Unique API name";

        @AField(name = "Version", description = "The version of this API.", type = AField.AFieldType.STRING)
        public final static String VERSION = "Version";

        @AField(name = "API key", description = "Your SwaggerHub API Key (from SwaggerHub's Settings page).", type = AField.AFieldType.PASSWORD)
        public final static String APIKEY = "API key";

        @AField(name = "Remember the key", description = "Save the API key for future actions.", type = AField.AFieldType.BOOLEAN)
        public final static String REMEMBER = "Remember the key";

        @AField(name = "Open after publishing", description = "Opens this API on SwaggerHub after publishing", type = AField.AFieldType.BOOLEAN)
        public final static String BROWSE = "Open after publishing";
    }
}
