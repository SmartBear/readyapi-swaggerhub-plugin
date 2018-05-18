package com.smartbear.plugins.swaggerhub;

import com.eviware.soapui.support.MessageSupport;
import com.smartbear.ready.logging.PacketsTableView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class SwaggerHubAPITable extends TableView {
    private ObservableList<SwaggerHubAPITableModel> tableModels = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    private static final MessageSupport messages = MessageSupport.getMessages(PacketsTableView.class);

    public SwaggerHubAPITable() {
        configure();
    }

    private void configure() {
        setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        getStyleClass().add("text-12px");

        TableColumn name = new TableColumn("Name");
        name.setCellValueFactory(new PropertyValueFactory<SwaggerHubAPITableModel, String>("name"));

        TableColumn desc = new TableColumn(messages.get("Description"));
        desc.setCellValueFactory(new PropertyValueFactory<SwaggerHubAPITableModel, String>("descr"));

        TableColumn oasVersion = new TableColumn("OAS Version");
        oasVersion.setCellValueFactory(new PropertyValueFactory<SwaggerHubAPITableModel, String>("oasVersion"));

        TableColumn visibility = new TableColumn("Visibility");
        visibility.setCellValueFactory(new PropertyValueFactory<SwaggerHubAPITableModel, String>("visibility"));

        TableColumn owner = new TableColumn("Owner");
        owner.setCellValueFactory(new PropertyValueFactory<SwaggerHubAPITableModel, String>("owner"));

        TableColumn versions = new TableColumn("Versions");
        versions.setCellValueFactory(new PropertyValueFactory<SwaggerHubAPITableModel, Object>("versions"));

        setItems(tableModels);
        getColumns().addAll(name, owner, desc, oasVersion, visibility, versions);

        setPlaceholder(new Label("No content in the table"));
    }

    public void clearTable() {
        tableModels.clear();
    }

    public void addDescription(ApiDescriptor descriptor) {
        tableModels.add(new SwaggerHubAPITableModel(descriptor));
    }
}
