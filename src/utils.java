import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
            for(int j = 0; j < keySize - 1; j++) {
                String s = keyIterator.next();
                bw.write("\"" + s + "\":[");
                List<Long> list = map.get(s);
                int size = map.get(s).size();
                for(int i = 0; i < size - 1; i++) {
                    bw.write("" + list.get(i)/(10^9) + ",");
                }
                bw.write("" + list.get(size - 1)/(10^9) + "],");
            }
            String s = keyIterator.next();
            bw.write("\"" + s + "\":[");
            List<Long> list = map.get(s);
            int size = map.get(s).size();
            for(int i = 0; i < size - 1; i++) {
                bw.write("" + list.get(i)/(10^9) + ",");
            }
            bw.write("" + list.get(size - 1)/(10^9) + "]}");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
