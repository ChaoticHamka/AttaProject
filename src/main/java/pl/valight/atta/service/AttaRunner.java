package pl.valight.atta.service;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AttaRunner {
    private final Consumer<String> statusUpdater;
    private final BiConsumer<String, Integer> progressReporter;

    public AttaRunner(Consumer<String> statusUpdater, BiConsumer<String, Integer> progressReporter) {
        this.statusUpdater = statusUpdater;
        this.progressReporter = progressReporter;
    }

    public void run(String inputPath, String outputPath) {
        AttaService service = new AttaService(statusUpdater, (msg, progress) -> {
            statusUpdater.accept("status:" + msg);
            progressReporter.accept(msg, progress);
        });
        service.process(inputPath, outputPath);
    }
}
