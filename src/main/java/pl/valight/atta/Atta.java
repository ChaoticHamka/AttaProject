package pl.valight.atta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

public class Atta {

    @FunctionalInterface
    public interface ProgressReporter {
        void report(String status, int processedCount);
    }

    private static WebDriver driver;
//    private static final String MAIN_SEARCH_INPUT_ID = "desktop-search-input";
//    private static final String MAIN_SEARCH_BUTTON_ID = "desktop-search-button";

    static Map<String, Map<String, String>> allPagesData;

    private static Consumer<String> statusUpdater;
    private static String inputPath;
    private static String outputPath;
    private final ProgressReporter reporter;

    public Atta(Consumer<String> statusUpdater, ProgressReporter reporter, String inputPath, String outputPath) {
        this.statusUpdater = statusUpdater;
        this.reporter = reporter;
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    public void runAttaDataSearch() {

        allPagesData = new LinkedHashMap<>();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--headless=new");

        statusUpdater.accept("Открываем браузер...");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        int numberEAN = 0;
        int totalEAN;

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

            totalEAN = validEans.size();

            for (String ean : validEans) {
                search(ean);
                numberEAN++;

                String verb = getVerbForm(numberEAN); // Обработана / Обработано / Обработаны
                String noun = getWordForm(numberEAN, "строка", "строки", "строк"); // строка / строки / строк
//                statusUpdater.accept(verb + " " + numberEAN + " " + noun + " из " + totalEAN);
                reporter.report(verb + " " + numberEAN + " " + noun + " из " + totalEAN, numberEAN);
            }
            saveAsExcel();

        } catch (IOException e) {
            statusUpdater.accept("Ошибка при чтении файла: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            driver.quit();
        }
    }

    private static String getVerbForm(int number) {
        int n = number % 100;
        if (n == 1 || (n % 10 == 1 && n != 11)) return "Обработана";
        if ((n % 10 >= 2 && n % 10 <= 4) && !(n >= 12 && n <= 14)) return "Обработаны";
        return "Обработано";
    }

    private static String getWordForm(int number, String one, String few, String many) {
        int n = number % 100;
        if (n >= 11 && n <= 14) return many;
        switch (number % 10) {
            case 1:
                return one;
            case 2:
            case 3:
            case 4:
                return few;
            default:
                return many;
        }
    }

    private static void search(String ean) throws InterruptedException {
        try {
            driver.get("https://bankoflamps.com/search/?search=" + ean);
//        Thread.sleep(5000); // можно заменить на WebDriverWait при необходимости

            // Получаем HTML после рендеринга
            String pageSource = driver.getPageSource();

            Document doc = Jsoup.parse(pageSource);
            Elements items = doc.getElementsByAttributeValue("data-ean", ean);
            Element item = items.select("a").getFirst();
            String href = item.attribute("href").getValue();
            get_data(href);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void get_data(String href) {
        driver.get(href);
//        Thread.sleep(5000); // можно заменить на WebDriverWait при необходимости

        // Получаем HTML после рендеринга
        String pageSource = driver.getPageSource();

        Document doc = Jsoup.parse(pageSource);

        Map<String, String> productData = new LinkedHashMap<>();

        productData.put("EAN", doc.getElementsByClass("ean").getFirst().text());
        productData.put("Код производителя", doc.getElementsByClass("mpn").getFirst().text());

        for (Element table : doc.select("table")) {
            for (Element row : table.select("tr")) {
                Elements cols = row.select("td");
                if (cols.size() >= 2) {
                    String key = cols.get(0).text().trim();
                    String value = cols.get(1).text().trim();
                    productData.put(key, value);
                }
            }
        }

        String pictureLinks = "";

        try {
            try {
                Elements pictures = doc.getElementById("product-main-sm-slider").getElementsByTag("img");
                for (Element picture : pictures) {
                    pictureLinks += picture.attribute("src").getValue().replace("64x64", "600x600") + "|";
                }
                pictureLinks = pictureLinks.substring(0, pictureLinks.length() - 1);
            } catch (Exception e) {
                Elements picture = doc.getElementById("lg-slide").getElementsByTag("img");
                pictureLinks = picture.getFirst().attribute("src").getValue();
            }
        } catch (Exception e) {
            statusUpdater.accept("Ошибка при извлечении картинок: " + e.getMessage());
        }

        productData.put("Картинки", pictureLinks);

        allPagesData.put(href, productData);

//        String nameString = "";
//        try {
//            Elements nameElements = doc.getElementsContainingOwnText("Manufacture name");
//            Element nameElement = nameElements.get(0);
//            Element name = nameElement.nextElementSibling();
//            nameString = name.text();
//        } catch (Exception e) {
//            statusUpdater.accept("Название товара по ссылке " + href + " не нашлось");
//        }
    }

    private static void saveAsJson() throws IOException {
        // Сохраняем в JSON-файл
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter writer = new FileWriter("output.json");
        gson.toJson(allPagesData, writer);
        writer.close();

        statusUpdater.accept("Данные сохранены в output.json");
    }

    private static void saveAsExcel() throws IOException {

        // Собираем все возможные ключи из всех таблиц
        Set<String> allKeys = new LinkedHashSet<>();
        for (Map<String, String> page : allPagesData.values()) {
            allKeys.addAll(page.keySet());
        }
        List<String> headerKeys = new ArrayList<>(allKeys);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Pages");

        // Заголовок (первая строка)
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("URL страницы");
        for (int i = 0; i < headerKeys.size(); i++) {
            headerRow.createCell(i + 1).setCellValue(headerKeys.get(i));
        }

        // Данные по страницам
        int rowIndex = 1;
        for (Map.Entry<String, Map<String, String>> entry : allPagesData.entrySet()) {
            String url = entry.getKey();
            Map<String, String> pageData = entry.getValue();

            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(url); // URL в первую колонку

            for (int i = 0; i < headerKeys.size(); i++) {
                String key = headerKeys.get(i);
                String value = pageData.getOrDefault(key, "");
                row.createCell(i + 1).setCellValue(value);
            }
        }

        // Автоподгон ширины колонок
        for (int i = 0; i <= headerKeys.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        statusUpdater.accept("Создание Excel-файла...");

        // Текущее время
        LocalDateTime now = LocalDateTime.now();
        // Формат: год-месяц-день-часы-минуты
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
        // Применяем формат
        String formattedDateTime = now.format(formatter);

        String fileExcelName = "AttaData " + formattedDateTime + ".xlsx";

        // Сохраняем Excel-файл
        try (FileOutputStream fileOut = new FileOutputStream(outputPath + File.separator + fileExcelName)) {
            workbook.write(fileOut);
            statusUpdater.accept("Excel сохранён как " + fileExcelName);
        } catch (Exception e) {
            e.printStackTrace();
            statusUpdater.accept("Ошибка при сохранении Excel: " + e.getMessage());
        }

        workbook.close();
    }
}
