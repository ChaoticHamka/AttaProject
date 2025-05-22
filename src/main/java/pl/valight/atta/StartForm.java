package pl.valight.atta;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class StartForm {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Сбор данных Атты");
            frame.setSize(600, 250);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JLabel statusLabel = new JLabel("Готов к запуску");
            JButton runButton = new JButton("Запустить сбор данных");

            // Поля выбора файлов
            JTextField inputFileField = new JTextField();
            inputFileField.setEditable(false);
            JButton browseInputButton = new JButton("Выбрать txt файл");

            JTextField outputDirField = new JTextField();
            outputDirField.setEditable(false);
            JButton browseOutputButton = new JButton("Выбрать папку");

            String desktopPath = System.getProperty("user.home") + File.separator + "Desktop";

            // Выбор входного файла (txt)
            browseInputButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser(new File(desktopPath));
                fileChooser.setDialogTitle("Выберите файл со ШК");
                int result = fileChooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    inputFileField.setText(selectedFile.getAbsolutePath());
                }
            });

            // Выбор папки для сохранения
            browseOutputButton.addActionListener(e -> {
                JFileChooser folderChooser = new JFileChooser(new File(desktopPath));
                folderChooser.setDialogTitle("Выберите папку для сохранения Excel");
                folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = folderChooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedDir = folderChooser.getSelectedFile();
                    outputDirField.setText(selectedDir.getAbsolutePath());
                }
            });

            // Обработчик кнопки
            runButton.addActionListener(e -> {
                runButton.setEnabled(false); // Блокируем кнопку на время выполнения

                String inputPath = inputFileField.getText();
                String outputPath = outputDirField.getText();

                if (inputPath.isEmpty() || outputPath.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Выберите и файл, и папку.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    runButton.setEnabled(true);
                    return;
                }

                SwingWorker<Void, String> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() {
                        try {
                            publish("Открываем браузер...");
                            Atta atta = new Atta(msg -> publish(msg), inputPath, outputPath);
                            atta.runAttaDataSearch(); // вызываем логику
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            publish("Ошибка: " + ex.getMessage());
                        }
                        return null;
                    }

                    @Override
                    protected void process(List<String> chunks) {
                        // Обновляем статус на последнее сообщение
                        String latest = chunks.get(chunks.size() - 1);
                        statusLabel.setText(latest);
                    }

                    @Override
                    protected void done() {
                        runButton.setEnabled(true); // Разблокируем кнопку
                    }
                };

                worker.execute();
            });

            // Панель для выбора исходного файла
            JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
            inputPanel.setBorder(BorderFactory.createTitledBorder("Исходный JSON-файл"));
            inputPanel.add(inputFileField, BorderLayout.CENTER);
            inputPanel.add(browseInputButton, BorderLayout.EAST);

            // Панель для выбора папки
            JPanel outputPanel = new JPanel(new BorderLayout(5, 5));
            outputPanel.setBorder(BorderFactory.createTitledBorder("Папка для сохранения Excel"));
            outputPanel.add(outputDirField, BorderLayout.CENTER);
            outputPanel.add(browseOutputButton, BorderLayout.EAST);

            // Горизонтальная панель: label слева, кнопка справа
            JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
            bottomPanel.add(statusLabel, BorderLayout.WEST);
            bottomPanel.add(runButton, BorderLayout.EAST);

            // Основной layout
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            mainPanel.add(inputPanel);
            mainPanel.add(Box.createVerticalStrut(10));
            mainPanel.add(outputPanel);
            mainPanel.add(Box.createVerticalStrut(20));
            mainPanel.add(bottomPanel);

            frame.add(mainPanel, BorderLayout.CENTER);
            frame.setLocationRelativeTo(null); // Центр экрана
            frame.setVisible(true);
        });
    }
}
