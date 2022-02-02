import java.io.*;
import java.util.*;

public class utils {

    public static void MapToJSON(Map<String, List<Long>> map, String PATH) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(PATH));
            bw.write("{");
            int keySize = map.keySet().size();
            Iterator<String> keyIterator = map.keySet().iterator();
            for (int j = 0; j < keySize - 1; j++) {
                String s = keyIterator.next();
                bw.write("\"" + s + "\":[");
                List<Long> list = map.get(s);
                int size = map.get(s).size();
                for (int i = 0; i < size - 1; i++) {
                    bw.write("" + list.get(i) / (10 ^ 9) + ",");
                }
                bw.write("" + list.get(size - 1) / (10 ^ 9) + "],");
            }
            String s = keyIterator.next();
            bw.write("\"" + s + "\":[");
            List<Long> list = map.get(s);
            int size = map.get(s).size();
            for (int i = 0; i < size - 1; i++) {
                bw.write("" + list.get(i) / (10 ^ 9) + ",");
            }
            bw.write("" + list.get(size - 1) / (10 ^ 9) + "]}");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public static Map<String, Integer> loadSolSizes() {
        Map<String, Integer> res = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("instances/optimal_solution_sizes.txt"));
            String line = br.readLine();
            while (line != null) {
                String pref;
                if (line.contains("synth")) {
                    pref = "synthetic/";
                } else {
                    pref = "complex/";
                }
                String[] temp = line.split("\\s+");
                res.put(pref + temp[0], Integer.valueOf(temp[1]));
                line = br.readLine();
            }

            br = new BufferedReader(new FileReader("instances/opt-sol3.txt"));
            line = br.readLine();
            while (line != null) {
                String[] temp = line.split("\\s+");
                String pref;
                if (line.contains("synth")) {
                    pref = "synthetic3/";
                } else {
                    pref = "complex3/";
                }
                try {
                    res.put(pref + temp[0], Integer.valueOf(temp[1]));
                } catch (NumberFormatException e) {
                    // catching timeout instances
                    res.put(pref + temp[0], -1);
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static Model loadMatrix(String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line = br.readLine();
            List<List<Double>> currentMatrix = new ArrayList<>();
            List<List<List<Double>>> weights = new ArrayList<>();
            List<List<Double>> biases = new ArrayList<>();
            boolean inMatrix = false;
            while (line != null) {
                if (line.length() <= 1) {
                    inMatrix = !inMatrix;
                    if (!inMatrix) {
                        weights.add(currentMatrix);
                        currentMatrix = new ArrayList<>();
                    }
                    line = br.readLine();
                    continue;
                }
                String[] splits = line.split(", ");
                List<Double> row = new ArrayList<>();
                for (String s : splits) {
                    row.add(Double.valueOf(s));
                }
                if (inMatrix) {
                    currentMatrix.add(row);
                } else {
                    biases.add(row);
                }
                line = br.readLine();
            }
            return new Model(weights, biases, 0.1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, List<Integer>> loadSolutions() {
        Map<String, List<Integer>> res = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("graph-metadata/solutions_week2.csv"));
            br.readLine(); // skip column name line

            String line = br.readLine();

            while (line != null) {
                String s = line.substring(line.lastIndexOf(';') + 1);
                String name = line.substring(0, line.indexOf(';'));
                s = s.replace("[", "").replace("]", "");
                String[] splits = s.split(", ");
                List<Integer> sol = new ArrayList<>();
                for (String split : splits) {
                    sol.add(Integer.valueOf(split));
                }
                res.put(name, sol);
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }

}
