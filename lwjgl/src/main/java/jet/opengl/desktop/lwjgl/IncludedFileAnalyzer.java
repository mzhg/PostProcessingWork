package jet.opengl.desktop.lwjgl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import jet.opengl.postprocessing.util.FileUtils;

public class IncludedFileAnalyzer implements FilenameFilter {

    private final String mRootPath;

    private final TreeMap<File, HFile> mAnalyzedFiles = new TreeMap<>();
    private final TreeMap<File, List<HFile>> mFirstLevelIncludeFiles = new TreeMap<>();

    public IncludedFileAnalyzer(String rootPath){
        mRootPath = rootPath;

        File path = new File(rootPath);
        try {
            foundAllFiles(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        analyzingIncludeConfigs();
    }

    private void addFile(File file, String includeFile, boolean system){
        if(includeFile.endsWith(".cpp") || includeFile.endsWith(".CPP"))
            return;

        File includedFile = new File(file.getParentFile(), includeFile);
        system |= (!includedFile.exists());

        List<HFile> includedFiles = mFirstLevelIncludeFiles.get(file);
        if(includedFiles  == null){
            includedFiles = new ArrayList<>();
            mFirstLevelIncludeFiles.put(file, includedFiles);
        }

        includedFiles.add(new HFile(includedFile, system));


//        mAllFiles.put(filename, new HFile(newFile, system));
    }

    private static String getFilename(File file){
        return file.getName();

//        String filename = file.getAbsolutePath();
//        filename = filename.replace(mRootPath, "");  // relative to the root path.
//        return filename;
    }

    private void analyzingIncludeConfigs(){
        Analyzer analyzer = new Analyzer();

        for(File everyFile : mFirstLevelIncludeFiles.keySet()){
            analyzer.push(everyFile);

            List<HFile> includedFiles = mFirstLevelIncludeFiles.get(everyFile);
            for(HFile includeFile : includedFiles){
                if(!includeFile.isSystem){
                    checkConfig(includeFile.file, analyzer);
                }
            }

            if(analyzer.isInvalid){
                System.out.println(analyzer.toString());
            }

            analyzer.clear();
        }
    }

    private static void toString(List<String> tokens){
        StringBuilder sb = new StringBuilder("");
        for(String s : tokens)
            sb.append(s);

        System.err.println(sb.toString());
    }

    private boolean checkConfig(File file,  Analyzer analyzer){
       if(analyzer.push(file)){
           return true;
       }

        List<HFile> includedFiles = mFirstLevelIncludeFiles.get(file);
        if(includedFiles == null){
//            tokens.remove(index);
            analyzer.pop(1);
            return false;
        }

        for(HFile includeFile : includedFiles){
            if(!includeFile.isSystem)
            {
                int size = analyzer.size();
                if(checkConfig(includeFile.file, analyzer))
                    return true;
                else{
                    int count = analyzer.size()-size;
                    analyzer.pop(count);
                }
            }
        }

        analyzer.pop(1);
        return false;
    }


    private void analyzingIncludes(){
        for(File everyFile : mFirstLevelIncludeFiles.keySet()){
            List<HFile> includedFiles = mFirstLevelIncludeFiles.get(everyFile);
            List<ParsedFile> parsedFiles = new ArrayList<>();

            int level = 0;
            for(HFile file : includedFiles){
                int _level = foundAllIncludes(file, parsedFiles, level);
                level = Math.max(_level, level);
            }

            List<List<HFile>> parsedIncludeFiles = new ArrayList<>();
            for(int i = 0; i < level + 1; i++ )
                parsedIncludeFiles.add(new ArrayList<>());

            for(ParsedFile file :  parsedFiles){
                parsedIncludeFiles.get(file.level).add(file.hfile);
            }
        }
    }

    private int foundAllIncludes(HFile includeFile,  List<ParsedFile> parsedFiles, int level){
        parsedFiles.add(new ParsedFile(includeFile, level));

        List<HFile> includedFiles = mFirstLevelIncludeFiles.get(includeFile.file);
        if(includedFiles == null)
            return level;

        for(HFile file : includedFiles){
            level = foundAllIncludes(file, parsedFiles,  level+1);
        }

        return level;
    }

    private void foundAllFiles(final File path) throws IOException {
        final FileUtils.LineFilter readLine = line->
        {
            line = line.trim();
            final String tag = "#include";
            if(line.startsWith(tag)){
                int start = line.indexOf('\"', tag.length());
                int end = -1;

                if(start > 0){
                    end = line.indexOf('\"', start + 1);

                    if(end < start){
                        System.err.println("Invalid token: " + line + " at the file :" + path.getAbsolutePath());

                        return "";
                    }

                    addFile(path, line.substring(start + 1, end), false);
                }

                /*start = line.indexOf('<', tag.length());
                if(start > 0){
                    end = line.indexOf('>', start + 1);

                    if(end < start){
                        System.err.println("Invalid token: " + line);

                        return "";
                    }

                    addFile(path, line.substring(start + 1, end), true);
                }*/
            }

            return "";
        };

        if(path.isFile()){
            if(path.canRead() && path.getName().endsWith(".h")){
                FileUtils.loadText(new FileInputStream(path), true, "GBK", readLine);
            }
        }else{
            File[] subFiles = path.listFiles(this);

            for(File file : subFiles){
                foundAllFiles(file);
            }
        }
    }

    @Override
    public boolean accept(File file, String s) {
        if(file.isDirectory())
            return true;

        return s.endsWith(".h");
    }

    private static final class ParsedFile{
        HFile hfile;
        int level;

        ParsedFile(HFile file, int level){
            hfile = file;
            this.level = level;
        }
    }

    private final static class Analyzer{
        final TreeSet<File> files = new TreeSet<>();
        final ArrayList<File> fileList = new ArrayList<>();

        boolean isInvalid = false;

        boolean push(File file){
            boolean contained = files.contains(file);
            fileList.add(file);
            files.add(file);

            isInvalid |= contained;

            return contained;
        }

        void pop(int count){
            for(int i = 0; i < count; i++){
                File last = fileList.remove(fileList.size()-1);
                files.remove(last);
            }

            if(!fileList.isEmpty()){
            }
        }

        int size() { return fileList.size();}

        void clear(){
            files.clear();
            fileList.clear();

            isInvalid = false;
        }

        boolean isInvalid() { return isInvalid;}

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            if(isInvalid){
                sb.append("Confilidens: ");
            }

            for(File file : fileList){
                sb.append(getFilename(file)).append("->");
            }

            sb.setLength(sb.length()-2);

            return sb.toString();
        }
    }

    private final class HFile{
        final File file;
        boolean isSystem;


        HFile(File file, boolean system){
            this.file = file;
            this.isSystem = system;
        }

        boolean isValid(){
            return false;
        }
    }

    public static void main(String[] args){
        new IncludedFileAnalyzer("E:\\workspace\\VSProjects\\GraphicsWork\\GraphicsWork");
    }
}
