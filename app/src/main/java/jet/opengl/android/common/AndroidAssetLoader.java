package jet.opengl.android.common;

import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;

import jet.opengl.postprocessing.util.FileLoader;
import jet.opengl.postprocessing.util.FileUtils;

/**
 * Created by mazhen'gui on 2017/10/12.
 */
public class AndroidAssetLoader implements FileLoader {

    private AssetManager manager;

    public AndroidAssetLoader(AssetManager manager){
        this.manager = manager;
    }

    @Override
    public InputStream open(String file) throws IOException {
        return manager.open(file);
    }

    @Override
    public String getCanonicalPath(String file) throws IOException {
        return file;
    }

    @Override
    public boolean exists(String file) {
        if(file == null || file.length() == 0)
            return false;

        String parent = FileUtils.getParent(file);
        String filename = FileUtils.getFile(file);
        try {
            String[] names = manager.list(parent);
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(filename.trim())) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public String resolvePath(String file) {
        return file;
    }
}
