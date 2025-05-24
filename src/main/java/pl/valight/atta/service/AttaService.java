package pl.valight.atta.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import pl.valight.atta.model.ProductData;
import pl.valight.atta.util.FileUtil;
import pl.valight.atta.util.TextUtil;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

public class AttaService {

    private final WebDriver driver;
    private final HtmlDataExtractor extractor;
    private final ExcelExporter exporter;
    private final Consumer<String> statusUpdater;
    private final ProgressReporter reporter;

    public interface ProgressReporter {
        void report(String message, int progress);
    }

    public AttaService(Consumer<String> statusUpdater, ProgressReporter reporter) {
        this.statusUpdater = statusUpdater;
        this.reporter = reporter;
        this.extractor = new HtmlDataExtractor(statusUpdater);
        this.exporter = new ExcelExporter(statusUpdater);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized", "--headless=new");
        this.driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    public void process(String inputPath, String outputPath) {
        try {
            List<String> eans = FileUtil.readValidLines(inputPath);
            int total = eans.size();
            int current = 0;

            Map<String, ProductData> allData = new LinkedHashMap<>();

            for (String ean : eans) {
                String url = extractor.search(driver, ean);
                if (url != null) {
                    ProductData data = extractor.extract(driver, url);
                    allData.put(url, data);
                }
                current++;
                reporter.report(TextUtil.getProgressMessage(current, total), current);
            }

            exporter.export(allData, outputPath);
        } catch (Exception e) {
            statusUpdater.accept("Ошибка: " + e.getMessage());
        } finally {
            driver.quit();
        }
    }
}
