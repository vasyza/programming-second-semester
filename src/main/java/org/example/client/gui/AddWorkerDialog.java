package org.example.client.gui;

import org.example.client.i18n.LocaleManager;
import org.example.common.model.*;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

public class AddWorkerDialog extends JDialog {

    private boolean okPressed = false;
    private Worker resultWorker = null;

    private JTextField nameField, startDateField, endDateField;
    private JSpinner xSpinner, ySpinner, salarySpinner, annualTurnoverSpinner;
    private JComboBox<Position> positionComboBox;
    private JComboBox<OrganizationType> orgTypeComboBox;

    public AddWorkerDialog(Frame owner, String title, Worker workerToUpdate) {
        super(owner, title, true);
        setupUI(workerToUpdate);
    }

    private void setupUI(Worker worker) {
        setLayout(new BorderLayout(10, 10));
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;

        nameField = new JTextField(15);
        xSpinner = new JSpinner(new SpinnerNumberModel(0.0, null, null, 0.1));
        ySpinner = new JSpinner(new SpinnerNumberModel(-71.0, -71.99, null, 0.1));
        salarySpinner = new JSpinner(new SpinnerNumberModel(1, 1, null, 1000));
        startDateField = new JTextField("yyyy-MM-ddTHH:mm:ss", 15);
        endDateField = new JTextField("yyyy-MM-ddTHH:mm:ssZ[ZoneId]", 15);
        positionComboBox = new JComboBox<>(Position.values());
        annualTurnoverSpinner = new JSpinner(new SpinnerNumberModel(1, 1, null, 100));
        orgTypeComboBox = new JComboBox<>(OrganizationType.values());

        gbc.gridx = 0; gbc.gridy = 0; fieldsPanel.add(new JLabel("Имя:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; fieldsPanel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; fieldsPanel.add(new JLabel("Координата X:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; fieldsPanel.add(xSpinner, gbc);
        gbc.gridx = 0; gbc.gridy = 2; fieldsPanel.add(new JLabel("Координата Y (> -72):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; fieldsPanel.add(ySpinner, gbc);
        gbc.gridx = 0; gbc.gridy = 3; fieldsPanel.add(new JLabel("Зарплата (>0):"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; fieldsPanel.add(salarySpinner, gbc);
        gbc.gridx = 0; gbc.gridy = 4; fieldsPanel.add(new JLabel("Дата начала (гггг-ММ-ддТЧЧ:мм:сс):"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; fieldsPanel.add(startDateField, gbc);
        gbc.gridx = 0; gbc.gridy = 5; fieldsPanel.add(new JLabel("Дата окончания (опционально):"), gbc);
        gbc.gridx = 1; gbc.gridy = 5; fieldsPanel.add(endDateField, gbc);
        gbc.gridx = 0; gbc.gridy = 6; fieldsPanel.add(new JLabel("Должность:"), gbc);
        gbc.gridx = 1; gbc.gridy = 6; fieldsPanel.add(positionComboBox, gbc);
        gbc.gridx = 0; gbc.gridy = 7; fieldsPanel.add(new JLabel("Годовой оборот (>0):"), gbc);
        gbc.gridx = 1; gbc.gridy = 7; fieldsPanel.add(annualTurnoverSpinner, gbc);
        gbc.gridx = 0; gbc.gridy = 8; fieldsPanel.add(new JLabel("Тип организации:"), gbc);
        gbc.gridx = 1; gbc.gridy = 8; fieldsPanel.add(orgTypeComboBox, gbc);

        if (worker != null) {
            fillFields(worker);
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton(LocaleManager.getString("dialog.button.ok"));
        JButton cancelButton = new JButton(LocaleManager.getString("dialog.button.cancel"));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        okButton.addActionListener(e -> onOK());
        cancelButton.addActionListener(e -> dispose());

        add(fieldsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    private void fillFields(Worker worker) {
        nameField.setText(worker.getName());
        xSpinner.setValue(worker.getCoordinates().getX());
        ySpinner.setValue(worker.getCoordinates().getY());
        if (worker.getSalary() != null) salarySpinner.setValue(worker.getSalary());
        startDateField.setText(worker.getStartDate().toString());
        if (worker.getEndDate() != null) endDateField.setText(worker.getEndDate().toString());
        if (worker.getPosition() != null) positionComboBox.setSelectedItem(worker.getPosition());
        if (worker.getOrganization().getAnnualTurnover() != null) annualTurnoverSpinner.setValue(worker.getOrganization().getAnnualTurnover());
        orgTypeComboBox.setSelectedItem(worker.getOrganization().getType());
    }

    private void onOK() {
        try {
            String name = nameField.getText();
            if (name.trim().isEmpty()) throw new IllegalArgumentException("Имя не может быть пустым.");

            Coordinates coordinates = new Coordinates(((Number) xSpinner.getValue()).floatValue(), ((Number) ySpinner.getValue()).doubleValue());
            Long salary = ((Number) salarySpinner.getValue()).longValue();
            LocalDateTime startDate = LocalDateTime.parse(startDateField.getText());
            ZonedDateTime endDate = endDateField.getText().trim().isEmpty() ? null : ZonedDateTime.parse(endDateField.getText());
            Position position = (Position) positionComboBox.getSelectedItem();
            Integer annualTurnover = ((Number) annualTurnoverSpinner.getValue()).intValue();
            OrganizationType orgType = (OrganizationType) orgTypeComboBox.getSelectedItem();

            Organization org = new Organization(annualTurnover, orgType);
            this.resultWorker = new Worker(name, coordinates, salary, startDate, endDate, position, org);
            this.okPressed = true;
            dispose();

        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Неверный формат даты: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка валидации: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isOkPressed() {
        return okPressed;
    }

    public Worker getWorker() {
        return resultWorker;
    }
}