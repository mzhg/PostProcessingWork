package jet.opengl.postprocessing.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by mazhen'gui on 2017/4/6.
 */

public interface FileLoader {

    public static FileLoader g_DefaultFileLoader = new FileLoader() {
        @Override
        public InputStream open(String filename) throws FileNotFoundException {
            return new FileInputStream(new File(filename));
        }

        @Override
        public String getParent(String filename) {
            return new File(filename).getParent();
        }

        @Override
        public String getCanonicalPath(String file) throws IOException {
            return new File(file).getCanonicalPath();
        }
    };

    public InputStream open(String file) throws FileNotFoundException;
    public String getParent(String file);
    public String getCanonicalPath(String file) throws IOException;
}
