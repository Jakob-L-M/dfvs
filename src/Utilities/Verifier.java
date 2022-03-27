package Utilities;

import java.io.*;
import java.util.*;

public class Verifier {

    public static boolean verify(String instanceName) {
        Runtime rt = Runtime.getRuntime();
        String[] commands = {"instances/verifier.exe", "instances/instances/" + instanceName,
                "instances/solutions/" + instanceName};

        try {
            Process proc = rt.exec(commands);
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            String res = stdInput.readLine();
            return res != null && res.equals("OK");

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static void verifyAndUpdate() {

        Map<String, Integer> best_known = loadBestKnown();

        Map<Integer, List<String>> diffMap = new TreeMap<>();

        List<String> incorrectInstances = new ArrayList<>();

        int counter = 0;
        System.out.print("Running Verifier... 00.00%");

        File[] files = Objects.requireNonNull(new File("instances/solutions/").listFiles());

        int numFiles = files.length;

        for (File f : files) {
            String name = f.getName();
            if (verify(name)) {
                int diff = updateBestKnown(best_known, name);

                if (!diffMap.containsKey(diff)) {
                    diffMap.put(diff, new ArrayList<>());
                }
                diffMap.get(diff).add(name);
            } else {
                incorrectInstances.add(name);
            }
            counter++;

            double percent = utils.round((double)(counter*100)/numFiles, 2);

            System.out.print("\b\b\b\b\b");
            if (percent >= 10.0) System.out.print("\b");

            System.out.print(String.format("%.2f", percent) + "%");
        }

        System.out.println("\n" + (numFiles - incorrectInstances.size()) + " instances yield a valid solution.");

        if (!incorrectInstances.isEmpty()) {
            System.out.println("\nIncorrect Instances:");
            incorrectInstances.forEach(System.out::println);
            System.out.println();
        }

        saveBestKnown(best_known);

        evaluateDifferenceMap(diffMap);
    }

    private static void saveBestKnown(Map<String, Integer> best_known) {

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("instances/best_known.txt"));

            for (String instance : best_known.keySet()) {
                bw.write(instance + " " + best_known.get(instance) + "\n");
            }

            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints the pace-score as well as the best and worse 10 instances
     * @param diffMap the differences of the current solutions and the best known solutions
     */
    private static void evaluateDifferenceMap(Map<Integer, List<String>> diffMap) {
        int mapSize = diffMap.size();
        List<Integer> keys = new ArrayList<>(diffMap.keySet());

        // best 10:
        int i = 0;
        int k = 0;
        System.out.println("\nBest Instances:");
        while (i < 10) {
            for (int currentList = 0; i < 10 && currentList < diffMap.get(keys.get(k)).size(); i++, currentList++) {
                int key = keys.get(k);
                String instance = diffMap.get(k).get(currentList);

                if (key >= 0) {
                    System.out.println("+" + key + " " + instance);
                }
                else {
                    System.out.println(key + " " + instance);
                }
            }
            k++;
        }

        // worst 10:
        System.out.println("\nWorst Instances:");
        i = 0;
        k = mapSize - 1;
        while (i < 10) {
            int key = keys.get(k);
            for (int currentList = 0; i < 10 && currentList < diffMap.get(key).size(); i++, currentList++) {

                String instance = diffMap.get(k).get(currentList);

                if (key >= 0) {
                    System.out.println("+" + key + " " + instance);
                }
                else {
                    System.out.println(key + " " + instance);
                }
            }
            k--;
        }
    }

    private static int updateBestKnown(Map<String, Integer> best_known, String name) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("instances/solutions/" + name));
            int lines = 0;
            while (reader.readLine() != null) lines++;
            reader.close();

            int bestK = best_known.getOrDefault(name, Integer.MAX_VALUE);

            if (bestK > lines) {
                best_known.put(name, lines);
            }

            // we do not want a new instance to ruin our stats.
            if (bestK == Integer.MAX_VALUE) {
                return 0;
            }

            // return difference
            return lines - bestK;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static Map<String, Integer> loadBestKnown() {
        Map<String, Integer> bestKnown = new HashMap<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader("instances/best_known.txt"));

            String line;

            while ((line = br.readLine()) != null) {
                String[] inst = line.split(" ");
                bestKnown.put(inst[0], Integer.parseInt(inst[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bestKnown;

    }

    public static void main(String[] args) {
        verifyAndUpdate();
    }
}
