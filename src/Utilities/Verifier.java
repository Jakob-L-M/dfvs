package Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

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
            return res.equals("OK");

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static void verifyAllSolutions() {
        int counter = 0;
        System.out.println("Running Verifier...");
        for (File f : Objects.requireNonNull(new File("instances/solutions/").listFiles())) {
            String name = f.getName();
            if (verify(name)) {
                counter++;
            } else {
                System.out.println("Instance " + name + " is not correct!");
            }
        }
        System.out.println("All (other) " + counter + " instances yield a valid solution.");
    }
}
