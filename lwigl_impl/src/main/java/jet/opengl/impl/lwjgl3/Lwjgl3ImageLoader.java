package jet.opengl.impl.lwjgl3;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import jet.opengl.postprocessing.texture.ImageData;
import jet.opengl.postprocessing.texture.ImageLoader;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/5/6.
 */

final class Lwjgl3ImageLoader implements ImageLoader{

    @Override
    public ImageData load(String filename, boolean flip) throws IOException {
//        final int[] formats = {
//                GL11.GL_RED, GL30.GL_RG, GL11.GL_RGB, GL11.GL_RGBA
//        };

        final int[] internalFormats = {
                GL30.GL_R8, GL30.GL_RG8, GL11.GL_RGB8, GL11.GL_RGBA8
        };

        final int[] _x = new int[1];
        final int[] _y = new int[1];
        final int[] _comp = new int[1];
//        STBImage.stbi_info(filename, _x, _y, _comp);
        ByteBuffer native_data = FileUtils.loadNative(filename);
        STBImage.stbi_info_from_memory(native_data, _x, _y, _comp);
        IntBuffer x,y,comp;
        ByteBuffer source = BufferUtils.createByteBuffer(12);
        x = source.asIntBuffer(); source.position(4);
        y = source.asIntBuffer(); source.position(8);
        comp = source.asIntBuffer();
        STBImage.stbi_set_flip_vertically_on_load(flip);

        ByteBuffer pixels = STBImage.stbi_load_from_memory(native_data, x, y, comp, _comp[0]);
        if(pixels == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "Loading image '" + filename + "' failed: " + STBImage.stbi_failure_reason());
            return null;
        }

        ImageData imageData = new ImageData();
        imageData.width = _x[0];
        imageData.height = _y[0];
        imageData.depth = 1;
        imageData.internalFormat = internalFormats[_comp[0] - 1];
        imageData.pixels = pixels;

        return imageData;
    }
}
