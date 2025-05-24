package pl.valight.atta.util;

public class TextUtil {
    public static String getProgressMessage(int current, int total) {
        String verb = getVerbForm(current);
        String noun = getWordForm(current, "строка", "строки", "строк");
        return String.format("%s %d %s из %d", verb, current, noun, total);
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
            case 1: return one;
            case 2:
            case 3:
            case 4: return few;
            default: return many;
        }
    }
}
