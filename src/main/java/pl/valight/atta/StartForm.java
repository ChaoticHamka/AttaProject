package pl.valight.atta;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class StartForm {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Сбор данных Атты");
            frame.setSize(450, 180);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JLabel statusLabel = new JLabel("Готов к запуску");
            JButton runButton = new JButton("Запустить сбор данных");

            runButton.addActionListener(e -> {
                runButton.setEnabled(false); // Блокируем кнопку на время выполнения

                SwingWorker<Void, String> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() {
                        try {
                            publish("Открываем браузер...");
                            Atta.runAttaDataSearch(msg -> publish(msg)); // вызываем логику
                            publish("Готово! Файл создан.");
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

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(runButton);
            panel.add(Box.createVerticalStrut(10));
            panel.add(statusLabel);

            frame.add(panel, BorderLayout.CENTER);
            frame.setLocationRelativeTo(null); // Центр на экране
            frame.setVisible(true);
        });
    }
}
