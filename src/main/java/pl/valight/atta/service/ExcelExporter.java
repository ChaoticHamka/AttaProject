package pl.valight.atta.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import pl.valight.atta.model.ProductData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

public class ExcelExporter {
    private final Consumer<String> statusUpdater;

    public ExcelExporter(Consumer<String> statusUpdater) {
        this.statusUpdater = statusUpdater;
    }

    public void export(Map<String, ProductData> allData, String outputPath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Products");

        Set<String> headers = new LinkedHashSet<>();
        for (ProductData data : allData.values()) {
            headers.addAll(data.getData().keySet());
        }

        List<String> headerList = new ArrayList<>(headers);
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("URL страницы");
        for (int i = 0; i < headerList.size(); i++) {
            headerRow.createCell(i + 1).setCellValue(headerList.get(i));
        }

        int rowIdx = 1;
        for (ProductData pd : allData.values()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(pd.getUrl());
            for (int i = 0; i < headerList.size(); i++) {
                String key = headerList.get(i);
                row.createCell(i + 1).setCellValue(pd.getData().getOrDefault(key, ""));
            }
        }

        for (int i = 0; i <= headerList.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        String fileName = "AttaData " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")) + ".xlsx";
        File file = new File(outputPath, fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            workbook.write(out);
        }
        workbook.close();

        statusUpdater.accept("Файл сохранён: " + fileName);
    }
}
