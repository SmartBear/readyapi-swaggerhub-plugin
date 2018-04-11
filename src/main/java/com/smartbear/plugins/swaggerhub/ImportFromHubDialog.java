package com.smartbear.plugins.swaggerhub;

import com.eviware.soapui.impl.rest.AbstractRestService;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.support.http.HttpClientSupport;
import com.eviware.soapui.model.workspace.Workspace;
import com.eviware.soapui.support.UISupport;
import com.google.common.io.ByteStreams;
import com.smartbear.ready.core.ApplicationEnvironment;
import com.smartbear.ready.core.Logging;
import com.smartbear.ready.core.ThreadPools;
import com.smartbear.ready.core.module.ModuleType;
import com.smartbear.swagger.OpenAPI3Importer;
import com.smartbear.swagger.Swagger2Importer;
import com.smartbear.swagger.SwaggerImporter;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.smartbear.swagger.AbstractSwaggerImporter.AUTHORIZATION_HEADER;

public class ImportFromHubDialog extends Dialog {
    public static final String SWAGGER_HUB_LOGIN = "SwaggerHubLogin";
    public static final String SWAGGER_HUB_PASSWORD = "SwaggerHubPassword";
    public static final String GET_TOKEN_URL = "https://api.swaggerhub.com/token";
    private static final String PRODUCT_ICON_PATH = UISupport.getImageResourceUrl("/ready-api-icon-16.png").toString();
    private static final int CONTENT_PANE_WIDTH = 710;
    private static final int CONTENT_PANE_HEIGHT = 525;
    private final TextField loginField = new TextField();
    private final TextField ownerField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final TextField searchField = new TextField();
    private String apiKey;
    private final CheckBox rememberCombo = new CheckBox();
    private final SwaggerHubAPITable table = new SwaggerHubAPITable();
    private final Button searchButton = new Button("Search");
    private final StackPane stackPane = new StackPane();
    private final WsdlProject project;


    public ImportFromHubDialog(WsdlProject project) {
        this.project = project;
        buildDialog();
    }


    protected void buildDialog() {
        initModality(Modality.APPLICATION_MODAL);
        setDialogPane(UISupport.getDialogPaneWithPlatformButtonOrder());
        Scene scene = getDialogPane().getScene();
        Stage stage = (Stage) scene.getWindow();
        stage.getIcons().add(new Image(PRODUCT_ICON_PATH));
        setResizable(false);
        setTitle("Import From SwaggerHub");
        String css = this.getClass().getResource("/css/swaggerhub-plugin.css").toExternalForm();
        scene.getStylesheets().add(css);
        createButtons();

        VBox vBox = new VBox();

        BorderPane root = new BorderPane();
        root.setMinSize(CONTENT_PANE_WIDTH, CONTENT_PANE_HEIGHT);
        root.setMaxSize(CONTENT_PANE_WIDTH, CONTENT_PANE_HEIGHT);
        root.setStyle("-fx-background-color: white");

        table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        BorderPane pane = new BorderPane(table);
        pane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        searchButton.setOnAction(event -> {
            populateList();
        });

        ScrollPane tableScroll = new ScrollPane(pane);
        tableScroll.setFitToHeight(true);
        tableScroll.setFitToWidth(true);
        tableScroll.getStyleClass().add("white-scroll");

        tableScroll.setMaxHeight(CONTENT_PANE_HEIGHT);
        stackPane.getChildren().add(tableScroll);
        vBox.getChildren().addAll(buildForm(), stackPane);
        root.setCenter(vBox);
        getDialogPane().setContent(root);
    }

    private void createButtons() {
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

        Button okButton = (Button) getDialogPane().lookupButton(okButtonType);
        okButton.getStyleClass().add("ok-button");

        okButton.setOnAction(event -> handleOk());
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            SwaggerHubAPITableModel model = (SwaggerHubAPITableModel) table.getSelectionModel().getSelectedItem();
            if (model == null) {
                buildAlert().showAndWait();
                event.consume();
            }
        });

        Button cancelButton = (Button) getDialogPane().lookupButton(cancelButtonType);
        cancelButton.getStyleClass().add("cancel-button");
    }

    private void populateList() {
        ThreadPools.getThreadPool().execute(() -> {
            ProgressIndicator progressIndicator = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
            Platform.runLater(() -> {
                stackPane.getChildren().add(progressIndicator);
                table.clearTable();
            });
            boolean importPrivate = false;
            String login = loginField.getText();
            String password = passwordField.getText();
            String owner = ownerField.getText();
            String searchQuery = searchField.getText();

            String uri;

            if (rememberCombo.isSelected()) {
                Workspace workspace = ApplicationEnvironment.getWorkspace();
                workspace.getSettings().setString(SWAGGER_HUB_LOGIN, loginField.getText());
                workspace.getSettings().setString(SWAGGER_HUB_PASSWORD, passwordField.getText());
            }

            if (StringUtils.isNotEmpty(login) && StringUtils.isNotEmpty(password) && StringUtils.isNotEmpty(owner)) {
                uri = PluginConfig.SWAGGERHUB_API + "/" + owner;
                try {
                    getApiKey(password, login);
                } catch (Exception e) {
                    return;
                }
                importPrivate = true;
            } else {
                uri = PluginConfig.SWAGGERHUB_API + "?limit=50";
            }

            if (StringUtils.isNotEmpty(searchQuery)) {
                if (importPrivate) {
                    uri += "?query=" + searchQuery;
                } else {
                    uri += "&query=" + searchQuery;
                }
            }

            try {
                HttpGet get = new HttpGet(uri);
                if (importPrivate) {
                    get.setHeader(AUTHORIZATION_HEADER, apiKey);
                }
                HttpResponse response = HttpClientSupport.getHttpClient().execute(get);

                List<ApiDescriptor> descriptors = new ApisJsonImporter().importApis(
                        new String(ByteStreams.toByteArray(response.getEntity().getContent())));

                Platform.runLater(() -> {
                    for (ApiDescriptor descriptor : descriptors) {
                        table.addDescription(descriptor);
                    }
                });
            } catch (Exception e) {
                Logging.logError(e);
            } finally {
                Platform.runLater(() -> stackPane.getChildren().remove(progressIndicator));
            }
        });
    }

    private List<AbstractRestService> importApis() {
        List<AbstractRestService> result = new ArrayList<>();
        try {
            SwaggerHubAPITableModel model = (SwaggerHubAPITableModel) table.getSelectionModel().getSelectedItem();
            ApiDescriptor descriptor = model.getDescriptor();
            String swaggerUrl = descriptor.swaggerUrl;

            SwaggerImporter importer;

            if (descriptor.oasVersion.equals("3.0.0")) {
                importer = new OpenAPI3Importer(project, "application/json", false);
            } else {
                importer = new Swagger2Importer(project, "application/json", false);
            }

            int selectedVersion = model.getVersionCombo().getSelectionModel().getSelectedIndex();

            String version = descriptor.versions[selectedVersion];
            if (version.startsWith("*") || version.startsWith("-")) {
                version = version.substring(1).trim();
            }

            String url = swaggerUrl.substring(0, swaggerUrl.lastIndexOf('/')) + "/" + version;
            System.out.println("Attempting to import Swagger from [" + url + "]");
            if (descriptor.isPrivate) {
                CollectionUtils.addAll(result, importer.importSwagger(swaggerUrl, apiKey));
            } else {
                Collections.addAll(result, importer.importSwagger(swaggerUrl));
            }
        } catch (Exception e) {
            Logging.logError(e);
        }
        return result;
    }

    private void handleOk() {
        List<AbstractRestService> result = importApis();
        if (CollectionUtils.isNotEmpty(result)) {
            UISupport.selectAndShow(result.get(0), ModuleType.PROJECTS.getId());
            close();
        }
    }

    private GridPane buildForm() {
        GridPane gridPane = new GridPane();
        gridPane.setVgap(8);
        gridPane.setHgap(8);
        gridPane.setPadding(new Insets(8, 0, 8, 0));

        Label loginLabel = createLabel("Login");
        gridPane.add(loginLabel, 0, 0);
        gridPane.add(loginField, 1, 0);

        Label passwordLabel = createLabel("Password");
        gridPane.add(passwordLabel, 0, 1);
        gridPane.add(passwordField, 1, 1);

        Label ownerLabel = createLabel("Owner of APIs");
        gridPane.add(ownerLabel, 0, 2);
        gridPane.add(ownerField, 1, 2);

        Label rememberLabel = createLabel("Remember me");

        gridPane.add(rememberLabel, 0, 3);
        gridPane.add(rememberCombo, 1, 3);

        Label searchLabel = new Label("Search");
        gridPane.add(searchLabel, 0, 4);
        gridPane.add(searchField, 1, 4);
        gridPane.add(searchButton, 2, 4);

        rememberCombo.setSelected(true);

        setTooltip("Specify your SwaggerHub login", loginField);
        setTooltip("Specify your SwaggerHub password", passwordField);
        setTooltip("Specify owner of your APIs", ownerField);
        setTooltip("Check to save your login and password in workspace", rememberCombo);
        setTooltip("Searches on owner, name, swagger.info.title and swagger.info.description of all APIs", searchField);

        Workspace workspace = ApplicationEnvironment.getWorkspace();
        passwordField.setText(workspace.getSettings().getString(SWAGGER_HUB_PASSWORD, ""));
        loginField.setText(workspace.getSettings().getString(SWAGGER_HUB_LOGIN, ""));

        passwordField.setPrefColumnCount(25);
        loginField.setPrefColumnCount(25);

        return gridPane;
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("default-text");
        return label;
    }

    private void setTooltip(String text, Control component) {
        Tooltip tooltip = new Tooltip(text);
        component.setTooltip(tooltip);
    }

    private void getApiKey(String password, String login) throws Exception {
        String jsonString = "";
        try {
            jsonString = new JSONObject()
                    .put("password", password)
                    .put("username", login).toString();
            HttpPost httpPost = new HttpPost(GET_TOKEN_URL);
            StringEntity params = new StringEntity(jsonString);
            httpPost.setHeader("content-type", "application/json");
            httpPost.setEntity(params);
            HttpResponse response = HttpClientSupport.getHttpClient().execute(httpPost);
            String jsonResponse = new String(ByteStreams.toByteArray(response.getEntity().getContent()));
            JSONObject jsonObject = new JSONObject(jsonResponse);
            String extractedApiKey = jsonObject.getString("token");

            if (StringUtils.isNotEmpty(extractedApiKey)) {
                apiKey = extractedApiKey;
            } else {
                throw new Exception();
            }

        } catch (Exception e) {
            Logging.logError(e, "Cannot retrieve API Key. Please check your credentials");
            throw e;
        }
    }

    private Alert buildAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING, "Please select API for import", ButtonType.OK);
        alert.setHeaderText("Select API");
        alert.setTitle("Select API");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white");
        dialogPane.getStyleClass().add("default-text");
        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.getIcons().add(new Image(PRODUCT_ICON_PATH));

        return alert;
    }
}
