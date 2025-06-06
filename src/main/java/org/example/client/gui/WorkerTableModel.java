package org.example.client.gui;

import org.example.client.i18n.LocaleManager;
import org.example.common.model.Worker;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

public class WorkerTableModel extends AbstractTableModel {
    private List<Worker> workers = new ArrayList<>();
    private final String[] columnKeys = {"table.header.id", "table.header.name", "table.header.x", "table.header.y", "table.header.creationDate", "table.header.salary", "table.header.startDate", "table.header.endDate", "table.header.position", "table.header.orgType", "table.header.annualTurnover", "table.header.ownerId"};

    public void setWorkers(List<Worker> workers) {
        this.workers = new ArrayList<>(workers);
        fireTableDataChanged();
    }

    public Worker getWorkerAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < workers.size()) {
            return workers.get(rowIndex);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return workers.size();
    }

    @Override
    public int getColumnCount() {
        return columnKeys.length;
    }

    @Override
    public String getColumnName(int column) {
        return LocaleManager.getString(columnKeys[column]);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0 || columnIndex == 5 || columnIndex == 10 || columnIndex == 11) {
            return Long.class;
        }
        if (columnIndex == 2 || columnIndex == 3) {
            return Double.class;
        }
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= workers.size()) return null;
        Worker worker = workers.get(rowIndex);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(LocaleManager.getCurrentLocale());
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(LocaleManager.getCurrentLocale());

        return switch (columnIndex) {
            case 0 -> worker.getId();
            case 1 -> worker.getName();
            case 2 -> worker.getCoordinates().getX();
            case 3 -> worker.getCoordinates().getY();
            case 4 -> worker.getCreationDate().format(dateFormatter);
            case 5 -> worker.getSalary() != null ? worker.getSalary() : 0L;
            case 6 -> worker.getStartDate().format(dateTimeFormatter);
            case 7 -> worker.getEndDate() != null ? worker.getEndDate().format(dateTimeFormatter) : "";
            case 8 -> worker.getPosition() != null ? worker.getPosition().name() : "";
            case 9 -> worker.getOrganization().getType().name();
            case 10 ->
                    worker.getOrganization().getAnnualTurnover() != null ? worker.getOrganization().getAnnualTurnover() : 0;
            case 11 -> worker.getOwnerId();
            default -> null;
        };
    }
}