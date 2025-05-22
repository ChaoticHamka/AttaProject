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
import java.util.*;

public class Atta {

    private static WebDriver driver;
//    private static final String MAIN_SEARCH_INPUT_ID = "desktop-search-input";
//    private static final String MAIN_SEARCH_BUTTON_ID = "desktop-search-button";

    static Map<String, Map<String, String>> allPagesData;

    public static void main(String[] args) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        String filePath = "ean.txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String ean;

            allPagesData = new LinkedHashMap<>();

            // Чтение строк по одной
            while ((ean = reader.readLine()) != null) {
                search(ean);
            }

            saveAsExcel();

        } catch (IOException e) {
            System.out.println("Ошибка при чтении файла: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            driver.quit();
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

        allPagesData.put(href, productData);

//        String nameString = "";
//        try {
//            Elements nameElements = doc.getElementsContainingOwnText("Manufacture name");
//            Element nameElement = nameElements.get(0);
//            Element name = nameElement.nextElementSibling();
//            nameString = name.text();
//        } catch (Exception e) {
//            System.out.println("Название товара по ссылке " + href + " не нашлось");
//        }
    }

    private static void saveAsJson() throws IOException {
        // Сохраняем в JSON-файл
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter writer = new FileWriter("output.json");
        gson.toJson(allPagesData, writer);
        writer.close();

        System.out.println("Данные сохранены в output.json");
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

        // Сохраняем Excel-файл
        try (FileOutputStream fileOut = new FileOutputStream("output_with_urls.xlsx")) {
            workbook.write(fileOut);
        }
        workbook.close();

        System.out.println("Excel сохранён как output_with_urls.xlsx");
    }
}
