package bits.dc;

import java.util.HashMap;
import java.util.Map;

public class WordCounter {

    public static void main(String[] args) {
        String theText = "This is some sample text. This text has some words repeated. repeated words.";
        Map<String, Integer> wordCounts = countTheWords(theText);
        printTheCounts(wordCounts);
    }

    // Method to count the words in a given string
    public static Map<String, Integer> countTheWords(String text) {
        Map<String, Integer> counts = new HashMap<>();
        String[] words = text.toLowerCase().split("[\\s.,;:]+"); // Split by common delimiters

        for (String w : words) {
            if (w != null && !w.isEmpty()) {
                if (counts.containsKey(w)) {
                    counts.put(w, counts.get(w) + 1);
                } else {
                    counts.put(w, 1);
                }
            }
        }
        return counts;
    }

    // Method to print the word counts
    public static void printTheCounts(Map<String, Integer> counts) {
        if (counts != null && !counts.isEmpty()) {
            System.out.println("Word Counts:");
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        } else {
            System.out.println("No words found or count is empty.");
        }
    }
}