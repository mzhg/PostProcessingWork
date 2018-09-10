package jet.parsing.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FileUtils {
    private FileUtils(){}

    public static String loadFile(String filename){
        Path file = Paths.get(filename);
        if(!Files.exists(file))
            throw new IllegalArgumentException("Couldn't find the file: " + filename);

        try {
            BufferedReader in = Files.newBufferedReader(file);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null){
                sb.append(line);
            }

            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
