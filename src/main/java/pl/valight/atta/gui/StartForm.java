package pl.valight.atta.gui;

import pl.valight.atta.service.AttaRunner;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StartForm {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Сбор данных Атты");

            // Кнопка запуска
            JButton runButton = new JButton("Запустить сбор данных");

            // Метки статуса
            JLabel statusLabel = new JLabel("Готов к запуску");
            JLabel timeEstimateLabel = new JLabel();

            JProgressBar progressBar = new JProgressBar();

            frame.setSize(600, 300);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

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
                fileChooser.setSelectedFile(new File(desktopPath + File.separator + "ean.txt"));

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
                folderChooser.setSelectedFile(new File(desktopPath));
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

                    private long startTime;
                    private int totalLines = 0;

                    @Override
                    protected Void doInBackground() {
                        try {
                            publish("Открываем браузер...");

                            startTime = System.currentTimeMillis();

                            // Сначала читаем все строки в список, исключая пустые
                            List<String> validEans = new ArrayList<>();
                            try (BufferedReader reader = new BufferedReader(new FileReader(inputPath))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    line = line.trim();
                                    if (!line.isEmpty()) {
                                        validEans.add(line);
                                    }
                                }
                                totalLines = validEans.size();
                            } catch (IOException e) {
                                publish("status:" + "Ошибка при чтении файла: " + e.getMessage());
                            }

                            AttaRunner runner = new AttaRunner(
                                    msg -> publish(msg),
                                    (msg, processed) -> {
                                        publish("status:" + msg);
                                        publish("progress:" + processed);

                                        long elapsed = System.currentTimeMillis() - startTime;
                                        if (processed > 0) {
                                            long totalEstimated = elapsed * totalLines / processed;
                                            long eta = totalEstimated - elapsed;
                                            long minutes = eta / 60000;
                                            long seconds = (eta / 1000) % 60;
                                            publish(String.format("eta:Осталось примерно %d мин. %d сек.", minutes, seconds));
                                        }
                                    }
                            );
                            runner.run(inputPath, outputPath);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            publish("Ошибка: " + ex.getMessage());
                        }
                        return null;
                    }

                    @Override
                    protected void process(List<String> chunks) {
                        for (String msg : chunks) {
                            if (msg.startsWith("status:")) {
                                statusLabel.setText(msg.substring(7));
                            } else if (msg.startsWith("progress:")) {
                                int processed = Integer.parseInt(msg.substring(9));
                                int percent = (int) ((processed * 100.0) / totalLines);
                                progressBar.setValue(percent);
                            } else if (msg.startsWith("eta:")) {
                                timeEstimateLabel.setText(msg.substring(4));
                            } else {
                                // Показываем все остальные сообщения как статус
                                statusLabel.setText(msg);
                            }
                        }
                    }

                    @Override
                    protected void done() {
                        runButton.setEnabled(true); // Разблокируем кнопку
                        timeEstimateLabel.setText(""); // Очищаем поле с оценкой времени
                    }

                    private int countLines(File file) {
                        int lines = 0;
                        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                            while (reader.readLine() != null) lines++;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return lines;
                    }
                };

                worker.execute();
            });

            // Панель для выбора исходного файла
            JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
            inputPanel.setBorder(BorderFactory.createTitledBorder("TXT-файл со штрихкодами"));
            inputPanel.add(inputFileField, BorderLayout.CENTER);
            inputPanel.add(browseInputButton, BorderLayout.EAST);

            // Панель для выбора папки
            JPanel outputPanel = new JPanel(new BorderLayout(5, 5));
            outputPanel.setBorder(BorderFactory.createTitledBorder("Папка для сохранения Excel"));
            outputPanel.add(outputDirField, BorderLayout.CENTER);
            outputPanel.add(browseOutputButton, BorderLayout.EAST);

            // Панель с двумя метками слева
            JPanel labelPanel = new JPanel();
            labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));
            labelPanel.add(statusLabel);
            labelPanel.add(Box.createVerticalStrut(5)); // небольшое расстояние между строками
            labelPanel.add(timeEstimateLabel);

            // Нижняя панель: слева метки, справа кнопка
            JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
            bottomPanel.add(labelPanel, BorderLayout.WEST);
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

            progressBar.setStringPainted(true);

            mainPanel.add(Box.createVerticalStrut(10));
            mainPanel.add(progressBar);
            mainPanel.add(Box.createVerticalStrut(5));

            frame.setVisible(true);
        });
    }
}