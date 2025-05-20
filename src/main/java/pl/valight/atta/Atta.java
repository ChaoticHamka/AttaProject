package pl.valight.atta;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;

public class Atta {

    private static WebDriver driver;
    private static final String MAIN_SEARCH_INPUT_ID = "desktop-search-input";
    private static final String MAIN_SEARCH_BUTTON_ID = "desktop-search-button";

    public static void main(String[] args) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        String filePath = "ean.txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String ean;

            // Чтение строк по одной
            while ((ean = reader.readLine()) != null) {
                search(ean);
            }

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
            Element item = items.select("a").get(0);
            String href = item.attribute("href").getValue();
            get_data(href);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void get_data(String href) throws InterruptedException {
        driver.get(href);
//        Thread.sleep(5000); // можно заменить на WebDriverWait при необходимости

        // Получаем HTML после рендеринга
        String pageSource = driver.getPageSource();

        Document doc = Jsoup.parse(pageSource);

        String nameString = "";
        try {
            Elements nameElements = doc.getElementsContainingOwnText("Manufacture name");
            Element nameElement = nameElements.get(0);
            Element name = nameElement.nextElementSibling();
            nameString = name.text();
        } catch (Exception e) {
            System.out.println("Название товара по ссылке " + href + " не нашлось");
        }
    }
}
