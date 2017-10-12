package jet.opengl.postprocessing.texture;

import java.io.IOException;

/**
 * Created by mazhen'gui on 2017/5/6.
 */

public interface NativeAPI {

    ImageData load(String filename, boolean flip) throws IOException;

    String getTextFromClipBoard();

    void saveImageFile(int width, int height, boolean hasAlpha, int[] pixels, String filepath) throws IOException;

    default boolean isSupportSaveImageFile(){ return true;}
}
