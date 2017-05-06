package jet.opengl.postprocessing.texture;

import java.io.IOException;

/**
 * Created by mazhen'gui on 2017/5/6.
 */

public interface ImageLoader {

    ImageData load(String filename, boolean flip) throws IOException;
}
