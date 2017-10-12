package jet.opengl.postprocessing.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by mazhen'gui on 2017/4/6.
 */

public interface FileLoader {

    public static FileLoader g_DefaultFileLoader = new FileLoader() {
        @Override
        public InputStream open(String filename) throws IOException {
            return new FileInputStream(new File(filename));
        }

        @Override
        public String getCanonicalPath(String file) throws IOException {
            return new File(file).getCanonicalPath();
        }

        @Override
        public boolean exists(String file) {
            return new File(file).exists();
        }
    };

    public InputStream open(String file) throws IOException;
    public String getCanonicalPath(String file) throws IOException;
    public boolean exists(String file);
}
