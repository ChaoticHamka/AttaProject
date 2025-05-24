package pl.valight.atta.util;

import java.io.*;
import java.util.*;

public class FileUtil {
    public static List<String> readValidLines(String path) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    result.add(line.trim());
                }
            }
        }
        return result;
    }
}
