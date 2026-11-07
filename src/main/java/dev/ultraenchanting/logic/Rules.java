package dev.ultraenchanting.logic;

import java.text.Normalizer;
import java.util.*;

/** Pure logic: no server, filesystem or world access. */
public final class Rules {
    public static final int MAX_LEVEL = 100_000_000;
    private Rules() {}
    public static int level(String input) {
        if (input == null || !input.matches("[0-9]{1,9}")) throw new IllegalArgumentException("integer required");
        long n = Long.parseLong(input);
        if (n < 1 || n > MAX_LEVEL) throw new IllegalArgumentException("level out of range");
        return (int) n;
    }
    public static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT)
            .replaceAll("\\p{M}", "").replaceAll("[^\\p{L}\\p{N}]", "");
    }
    public static int score(String query, Collection<String> terms) {
        String q = normalize(query);
        if (q.isEmpty()) return 1;
        int best = 0;
        for (String term : terms) {
            String t = normalize(term);
            int s = t.equals(q) ? 100 : t.startsWith(q) ? 80 : t.contains(q) ? 60 :
                q.length() >= 4 && distance(q, t) <= (q.length() >= 8 ? 2 : 1) ? 30 : 0;
            best = Math.max(best, s);
        }
        return best;
    }
    public static int distance(String a, String b) {
        int[] row = new int[b.length()+1];
        for (int j=0;j<row.length;j++) row[j]=j;
        for (int i=1;i<=a.length();i++) {
            int diagonal=row[0]; row[0]=i;
            for (int j=1;j<=b.length();j++) {
                int old=row[j];
                row[j]=Math.min(Math.min(row[j]+1,row[j-1]+1),diagonal+(a.charAt(i-1)==b.charAt(j-1)?0:1));
                diagonal=old;
            }
        }
        return row[b.length()];
    }
    public static Optional<String> conflict(String selected, Set<String> existing, Map<String, Set<String>> graph) {
        return existing.stream().sorted().filter(id -> !id.equals(selected) &&
            (graph.getOrDefault(selected, Set.of()).contains(id) || graph.getOrDefault(id, Set.of()).contains(selected))).findFirst();
    }
    public static Map<String,Integer> replace(Map<String,Integer> old, String id, int value) {
        level(Integer.toString(value));
        Map<String,Integer> copy = new TreeMap<>(old); copy.put(id,value); return copy;
    }
    public static double bounded(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min,Math.min(max,value)) : min;
    }
}