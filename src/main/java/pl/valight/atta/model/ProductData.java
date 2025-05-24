package pl.valight.atta.model;

import java.util.Map;

public class ProductData {
    private final String url;
    private final Map<String, String> data;

    public ProductData(String url, Map<String, String> data) {
        this.url = url;
        this.data = data;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getData() {
        return data;
    }
}
