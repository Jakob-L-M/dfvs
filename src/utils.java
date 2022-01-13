import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
                String[] temp = line.split("\\s+");
                res.put(temp[0], Integer.valueOf(temp[1]));
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static Map<String, Integer> loadSolSizes3() {
        Map<String, Integer> res = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("instances/opt-sol3.txt"));
            String line = br.readLine();
            while (line != null) {
                String[] temp = line.split("\\s+");
                try {
                    res.put(temp[0], Integer.valueOf(temp[1]));
                } catch (NumberFormatException e) {
                    // catching timeout instances
                    res.put(temp[0], -1);
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

}
