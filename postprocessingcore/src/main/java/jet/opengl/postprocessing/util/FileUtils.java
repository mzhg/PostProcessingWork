package jet.opengl.postprocessing.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * Created by mazhen'gui on 2017/3/28.
 */

public class FileUtils {

    public static FileLoader g_IntenalFileLoader = FileLoader.g_DefaultFileLoader;

    private FileUtils(){}

    public static StringBuilder loadText(InputStream in, boolean igoreComment, String charset, LineFilter filter) throws IOException {
        return loadText(new InputStreamReader(in, charset == null ? Charset.defaultCharset() : Charset.forName(charset)), igoreComment, filter);
    }

    public interface LineFilter {
        String filter(String line);
    }

    public static void setIntenalFileLoader(FileLoader fileLoader){
        if(fileLoader == null)
            throw new NullPointerException("fileLoader is null!");

        g_IntenalFileLoader = fileLoader;
    }

    public static StringBuilder loadText(Reader in, boolean igoreComment, LineFilter filter)throws IOException{
        StringBuilder out;
        if(igoreComment){
            StringBuilder buf = new StringBuilder(64);
            CommentFilter cf = new CommentFilter(in);
            String line;
            while((line = cf.nextLine()) != null){
                if(filter != null)
                    line = filter.filter(line);

                buf.append(line).append('\n');
            }

            out = buf;
        }else{
            out = loadText(in);
        }
        return out;
    }

    public static InputStream open(String file) throws FileNotFoundException {
        return g_IntenalFileLoader.open(file);
    }

    public static ByteBuffer loadNative(String filepath) throws IOException{
        try(InputStream inputStream = g_IntenalFileLoader.open(filepath)){
            ByteBuffer buf = ByteBuffer.allocateDirect(inputStream.available()).order(ByteOrder.nativeOrder());

            if(inputStream instanceof FileInputStream){
                FileChannel in = ((FileInputStream)inputStream).getChannel();
                in.read(buf);
                in.close();
                buf.flip();
            }else{
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);
                buf.put(bytes).flip();
            }

            return buf;
        }
    }

    public static byte[] loadBytes(String file) throws  IOException{
        try(InputStream inputStream = open(file)){
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            return bytes;
        }
    }

    public static StringBuilder loadText(Reader in) throws IOException{
        String line;
        BufferedReader reader;
        if(in instanceof BufferedReader)
            reader = (BufferedReader)in;
        else
            reader = new BufferedReader(in);

        StringBuilder sb = new StringBuilder(64);
        while((line = reader.readLine()) != null)
            sb.append(line).append('\n');

        sb.setLength(sb.length() - 1);
        return sb;
    }

    /**
     * Load the text from the classpath, the 'filename' have in the form of aaa/bbb/ccc.ext
     */
    public static final StringBuilder loadTextFromClassPath(String filename){
        InputStream input = ClassLoader.getSystemResourceAsStream(filename);
        if(input == null) return null;

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuilder sb = new StringBuilder(Math.max(1, input.available()));
            String s;

            while ((s = in.readLine()) != null)
                sb.append(s).append('\n');
            in.close();
            input.close();
            return sb;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
