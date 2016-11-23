package com.smartbear.plugins.swaggerhub;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class ApisTableModel extends AbstractTableModel {

    private List<ApiDescriptor> apis;

    public ApisTableModel() {
    }

    public void setApis( List<ApiDescriptor> apis )
    {
        this.apis = apis;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return apis == null ? 0 : apis.size();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getColumnName(int column) {
        switch (column){
            case 0 : return "Owner";
            case 1 : return "Name";
            case 2 : return "Description";
            case 3 : return "Versions";
        }

        return null;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        if( apis == null ){
            return null;
        }

        ApiDescriptor api = apis.get( rowIndex );

        switch (columnIndex){
            case 0 : return api.owner;
            case 1 : return api.name;
            case 2 : return api.description;
            case 3 : return String.valueOf( api.versions.length );
        }

        return null;
    }

    public ApiDescriptor getApiAtRow(int apiIndex) {
        return apis.get( apiIndex );
    }
}
