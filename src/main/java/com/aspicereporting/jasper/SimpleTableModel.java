package com.aspicereporting.jasper;

import javax.swing.table.AbstractTableModel;

public class SimpleTableModel extends AbstractTableModel {
    private String[] columnNames;

    private Object[][] data;

    public SimpleTableModel(int rows, int columns)
    {
        this.columnNames = new String[columns];
        this.data = new Object[rows][columns];
    }

    public int getColumnCount()
    {
        return this.columnNames.length;
    }

    public String getColumnName(int columnIndex)
    {
        return this.columnNames[columnIndex];
    }

    public int getRowCount()
    {
        return this.data.length;
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
        return this.data[rowIndex][columnIndex];
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public void setData(Object[][] data) {
        this.data = data;
    }
}
