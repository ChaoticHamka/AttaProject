package pl.valight.atta.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import pl.valight.atta.model.ProductData;

import java.util.*;
import java.util.function.Consumer;

public class HtmlDataExtractor {
    private final Consumer<String> statusUpdater;

    public HtmlDataExtractor(Consumer<String> statusUpdater) {
        this.statusUpdater = statusUpdater;
    }

    public String search(WebDriver driver, String ean) {
        try {
            driver.get("https://bankoflamps.com/search/?search=" + ean);
            Document doc = Jsoup.parse(driver.getPageSource());
            Elements items = doc.getElementsByAttributeValue("data-ean", ean);
            Element item = items.select("a").first();
            return item != null ? item.attr("href") : null;
        } catch (Exception e) {
            statusUpdater.accept("Ошибка поиска: " + e.getMessage());
            return null;
        }
    }

    public ProductData extract(WebDriver driver, String url) {
        Map<String, String> data = new LinkedHashMap<>();
        driver.get(url);
        Document doc = Jsoup.parse(driver.getPageSource());

        data.put("EAN", doc.selectFirst(".ean").text());
        data.put("Код производителя", doc.selectFirst(".mpn").text());

        for (Element row : doc.select("table tr")) {
            Elements cols = row.select("td");
            if (cols.size() >= 2) {
                data.put(cols.get(0).text().trim(), cols.get(1).text().trim());
            }
        }

        try {
            Elements imgs = doc.select("#product-main-sm-slider img");
            StringBuilder links = new StringBuilder();
            for (Element img : imgs) {
                links.append(img.attr("src").replace("64x64", "600x600")).append("|");
            }
            data.put("Картинки", links.toString().replaceAll("\\|$", ""));
        } catch (Exception e) {
            statusUpdater.accept("Картинки не найдены: " + e.getMessage());
        }

        return new ProductData(url, data);
    }
}
