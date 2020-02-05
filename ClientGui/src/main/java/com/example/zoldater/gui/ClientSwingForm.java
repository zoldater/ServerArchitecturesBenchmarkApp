package com.example.zoldater.gui;

import com.example.zoldater.ClientCliApplication;
import com.example.zoldater.core.configuration.InitialConfiguration;
import com.example.zoldater.core.configuration.InitialConfigurationBuilder;
import com.example.zoldater.core.configuration.data.ValueArgumentDataBuilder;
import com.example.zoldater.core.configuration.data.VariableArgumentData;
import com.example.zoldater.core.configuration.data.VariableArgumentDataBuilder;
import com.example.zoldater.core.enums.ArchitectureTypeEnum;
import com.example.zoldater.core.enums.ArgumentTypeEnum;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

public class ClientSwingForm extends JFrame {

    private JPanel mainPanel;
    private JTextField serverAddressText;
    private JComboBox architectureTypeBox;
    private JLabel serverAddressLabel;
    private JLabel architectureLabel;
    private JLabel varTypeLabel;
    private JComboBox varTypeBox;
    private JLabel varFromLabel;
    private JTextField varFromText;
    private JLabel varToLabel;
    private JLabel varStepLabel;
    private JTextField varStepArea;
    private JTextField varToArea;
    private JTextField elementsText;
    private JTextField clientsValueText;
    private JTextField delayText;
    private JTextField requestsText;
    private JLabel elementsLabel;
    private JLabel clientsLabel;
    private JLabel requestsLabel;
    private JLabel delayLabel;
    private JButton startButton;
    private JPanel chartsPanel;

    public ClientSwingForm(String title) {
        super(title);
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();

        InitialConfigurationBuilder configurationBuilder = new InitialConfigurationBuilder();
        VariableArgumentDataBuilder variableArgumentDataBuilder = new VariableArgumentDataBuilder();
        ValueArgumentDataBuilder valueElementsCountBuilder = new ValueArgumentDataBuilder();
        ValueArgumentDataBuilder valueClientsCountBuilder = new ValueArgumentDataBuilder();
        ValueArgumentDataBuilder valueRequestDelayBuilder = new ValueArgumentDataBuilder();
        ValueArgumentDataBuilder valueRequestPerClientBuilder = new ValueArgumentDataBuilder();

        variableArgumentDataBuilder.setArgumentTypeEnum(ArgumentTypeEnum.ARRAY_ELEMENTS);
        variableArgumentDataBuilder.setArgumentTypeEnum(ArgumentTypeEnum.ARRAY_ELEMENTS);
        elementsText.setEnabled(false);

        configurationBuilder.setArchitectureType(ArchitectureTypeEnum.ONLY_THREADS_ARCH);

        serverAddressText.getDocument().addDocumentListener((SimpleDocumentListener) e -> configurationBuilder.setServerAddress(serverAddressText.getText()));
        architectureTypeBox.addActionListener(e -> {
            String arch = (String) architectureTypeBox.getSelectedItem();
            if (arch == null) {
                return;
            }
            switch (arch) {
                case "Blocking with threads":
                    configurationBuilder.setArchitectureType(ArchitectureTypeEnum.ONLY_THREADS_ARCH);
                    break;
                case "Blocking with pool":
                    configurationBuilder.setArchitectureType(ArchitectureTypeEnum.WITH_EXECUTORS_ARCH);
                    break;
                case "Non blocking":
                    configurationBuilder.setArchitectureType(ArchitectureTypeEnum.NON_BLOCKING_ARCH);
                    break;
                default:
                    throw new RuntimeException("Bad configuration in UI!");
            }
        });
        varTypeBox.addActionListener(e -> {
            String varArgTypeStr = (String) varTypeBox.getSelectedItem();
            if (varArgTypeStr == null) {
                return;
            }
            switch (varArgTypeStr) {
                case "Elements in array (N)":
                    elementsText.setEnabled(false);
                    clientsValueText.setEnabled(true);
                    delayText.setEnabled(true);
                    variableArgumentDataBuilder.setArgumentTypeEnum(ArgumentTypeEnum.ARRAY_ELEMENTS);
                    break;
                case "Clients simultaneously (M)":
                    clientsValueText.setEnabled(false);
                    elementsText.setEnabled(true);
                    delayText.setEnabled(true);
                    variableArgumentDataBuilder.setArgumentTypeEnum(ArgumentTypeEnum.CLIENTS_NUMBER);
                    break;
                case "Delay between requests (D)":
                    delayText.setEnabled(false);
                    elementsText.setEnabled(true);
                    clientsValueText.setEnabled(true);
                    variableArgumentDataBuilder.setArgumentTypeEnum(ArgumentTypeEnum.DELTA_MS);
                    break;
                default:
                    throw new RuntimeException("Bad configuration in UI!");
            }
        });
        varStepArea.getDocument().addDocumentListener((SimpleDocumentListener) e -> variableArgumentDataBuilder.setStep(Integer.parseInt(varStepArea.getText())));
        varFromText.getDocument().addDocumentListener((SimpleDocumentListener) e -> variableArgumentDataBuilder.setFrom(Integer.parseInt(varFromText.getText())));
        varToArea.getDocument().addDocumentListener((SimpleDocumentListener) e -> variableArgumentDataBuilder.setTo(Integer.parseInt(varToArea.getText())));
        elementsText.getDocument().addDocumentListener((SimpleDocumentListener) e -> {
            valueElementsCountBuilder.setArgumentTypeEnum(ArgumentTypeEnum.ARRAY_ELEMENTS);
            valueElementsCountBuilder.setValue(Integer.parseInt(elementsText.getText()));
        });
        clientsValueText.getDocument().addDocumentListener((SimpleDocumentListener) e -> {
            valueClientsCountBuilder.setArgumentTypeEnum(ArgumentTypeEnum.CLIENTS_NUMBER);
            valueClientsCountBuilder.setValue(Integer.parseInt(clientsValueText.getText()));
        });
        delayText.getDocument().addDocumentListener((SimpleDocumentListener) e -> {
            valueRequestDelayBuilder.setArgumentTypeEnum(ArgumentTypeEnum.DELTA_MS);
            valueRequestDelayBuilder.setValue(Integer.parseInt(delayText.getText()));
        });
        requestsText.getDocument().addDocumentListener((SimpleDocumentListener) e -> {
            valueRequestPerClientBuilder.setArgumentTypeEnum(ArgumentTypeEnum.REQUESTS_PER_CLIENT);
            valueRequestPerClientBuilder.setValue(Integer.parseInt(requestsText.getText()));
        });

        startButton.addActionListener(e -> {
            configurationBuilder.setRequestsPerClient(valueRequestPerClientBuilder.createValueArgumentData());
            VariableArgumentData variableArgumentData = variableArgumentDataBuilder.createVariableArgumentData();
            configurationBuilder.setVariableArgumentData(variableArgumentData);
            switch (variableArgumentData.getArgumentTypeEnum()) {
                case ARRAY_ELEMENTS:
                    configurationBuilder.setValueArgumentData1(valueClientsCountBuilder.createValueArgumentData());
                    configurationBuilder.setValueArgumentData2(valueRequestDelayBuilder.createValueArgumentData());
                    break;
                case CLIENTS_NUMBER:
                    configurationBuilder.setValueArgumentData1(valueElementsCountBuilder.createValueArgumentData());
                    configurationBuilder.setValueArgumentData2(valueRequestDelayBuilder.createValueArgumentData());
                case DELTA_MS:
                    configurationBuilder.setValueArgumentData1(valueElementsCountBuilder.createValueArgumentData());
                    configurationBuilder.setValueArgumentData2(valueClientsCountBuilder.createValueArgumentData());
                default:
                    throw new RuntimeException("Bad configuration received!");
            }
            InitialConfiguration initialConfiguration = configurationBuilder.createInitialConfiguration();
            List<XYChart> xyCharts = ClientCliApplication.startAndCollectCharts(initialConfiguration);
            new SwingWrapper<>(xyCharts).displayChartMatrix();
        });
    }

    public static void main(String[] args) {
        JFrame frame = new ClientSwingForm("GUI App");
        frame.setVisible(true);
    }

    @FunctionalInterface
    public interface SimpleDocumentListener extends DocumentListener {
        void update(DocumentEvent e);

        @Override
        default void insertUpdate(DocumentEvent e) {
            update(e);
        }

        @Override
        default void removeUpdate(DocumentEvent e) {
            update(e);
        }

        @Override
        default void changedUpdate(DocumentEvent e) {
            update(e);
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(11, 2, new Insets(0, 0, 0, 0), -1, -1));
        serverAddressLabel = new JLabel();
        serverAddressLabel.setText("Server Address");
        mainPanel.add(serverAddressLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serverAddressText = new JTextField();
        mainPanel.add(serverAddressText, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        varTypeLabel = new JLabel();
        varTypeLabel.setText("Variable Type");
        mainPanel.add(varTypeLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        varTypeBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Elements in array (N)");
        defaultComboBoxModel1.addElement("Clients simultaneously (M)");
        defaultComboBoxModel1.addElement("Delay between requests (D)");
        varTypeBox.setModel(defaultComboBoxModel1);
        mainPanel.add(varTypeBox, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startButton = new JButton();
        startButton.setText("Начать");
        mainPanel.add(startButton, new com.intellij.uiDesigner.core.GridConstraints(10, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        architectureLabel = new JLabel();
        architectureLabel.setText("Architecture");
        mainPanel.add(architectureLabel, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        architectureTypeBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("Blocking with threads");
        defaultComboBoxModel2.addElement("Blocking with pool");
        defaultComboBoxModel2.addElement("Non blocking");
        architectureTypeBox.setModel(defaultComboBoxModel2);
        mainPanel.add(architectureTypeBox, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        varToLabel = new JLabel();
        varToLabel.setText("To");
        mainPanel.add(varToLabel, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clientsLabel = new JLabel();
        clientsLabel.setText("Clients (M)");
        mainPanel.add(clientsLabel, new com.intellij.uiDesigner.core.GridConstraints(7, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        requestsLabel = new JLabel();
        requestsLabel.setText("Requests (X)");
        mainPanel.add(requestsLabel, new com.intellij.uiDesigner.core.GridConstraints(9, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        varToArea = new JTextField();
        mainPanel.add(varToArea, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        clientsValueText = new JTextField();
        mainPanel.add(clientsValueText, new com.intellij.uiDesigner.core.GridConstraints(7, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        requestsText = new JTextField();
        mainPanel.add(requestsText, new com.intellij.uiDesigner.core.GridConstraints(9, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        elementsLabel = new JLabel();
        elementsLabel.setText("Elements (N)");
        mainPanel.add(elementsLabel, new com.intellij.uiDesigner.core.GridConstraints(6, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        elementsText = new JTextField();
        mainPanel.add(elementsText, new com.intellij.uiDesigner.core.GridConstraints(6, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        delayLabel = new JLabel();
        delayLabel.setText("Delay (Δ)");
        mainPanel.add(delayLabel, new com.intellij.uiDesigner.core.GridConstraints(8, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        delayText = new JTextField();
        mainPanel.add(delayText, new com.intellij.uiDesigner.core.GridConstraints(8, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        varFromText = new JTextField();
        mainPanel.add(varFromText, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        varFromLabel = new JLabel();
        varFromLabel.setText("From");
        mainPanel.add(varFromLabel, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        varStepLabel = new JLabel();
        varStepLabel.setText("Step");
        mainPanel.add(varStepLabel, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        varStepArea = new JTextField();
        mainPanel.add(varStepArea, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}