package jet.parsing;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;

public class EDiskRecord {

    public static void main(String[] args) throws IOException{
        LinkedHashSet<FileKey> oldValues = new LinkedHashSet<>();
        Path file = Paths.get("E:/thunder.txt");
        if(Files.exists(file)){
            List<String> lines = Files.readAllLines(file);
            for(String line : lines){
                String[] tokens = line.split(" ");
                oldValues.add(new FileKey(tokens[0], tokens[1]));
            }
        }
        String path = new String("F://迅雷下载".getBytes("GBK"), "utf-8");
        Path folder =  Paths.get(path);
        if(!Files.exists(folder)) {
            System.out.println("File not exists");
            return;
        }

        LinkedHashSet<FileKey> newValues = new LinkedHashSet<>();
        Files.walkFileTree(folder, EnumSet.of(FileVisitOption.FOLLOW_LINKS), 1, new ListFile(newValues));

        oldValues.addAll(newValues);
        List<String> lines = new ArrayList<>();

        for(FileKey fileKey : oldValues){
            lines.add(fileKey.toString());
        }

        System.out.println(lines);

        Files.write(file, lines);
    }

    private static final class FileKey{
        String name;
        String time;

        FileKey(String name, String time){
            this.name = name;
            this.time = time;
        }

        FileKey(){}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            FileKey fileKey = (FileKey) o;

            if(!name.equals(fileKey.name)) return false;
            if(!time.equals(fileKey.time)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (time != null ? time.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return name + " " + time;
        }
    }

    private static final class ListFile extends SimpleFileVisitor<Path> {

        LinkedHashSet<FileKey> newValues;
        ListFile(LinkedHashSet<FileKey> newValues){
            this.newValues = newValues;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            /*String path = new String("迅雷下载".getBytes("GBK"), "utf-8");
            if(dir.getFileName().equals(path)){

            }else{
                return FileVisitResult.SKIP_SUBTREE;
            }*/

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            newValues.add(new FileKey(file.getFileName().toString(), attrs.creationTime().toString()));

            return super.visitFile(file, attrs);
        }
    }
}
