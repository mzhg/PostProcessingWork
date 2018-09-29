package jet.opengl.impl.lwjgl3;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import jet.opengl.postprocessing.texture.ImageData;
import jet.opengl.postprocessing.texture.NativeAPI;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/5/6.
 */

final class Lwjgl3ImageLoader implements NativeAPI {

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
        imageData.width = x.get(0);
        imageData.height = y.get(0);
        imageData.depth = 1;
        imageData.internalFormat = internalFormats[_comp[0] - 1];
        imageData.pixels = pixels;

        return imageData;
    }

    @Override
    public String getTextFromClipBoard() {
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        try {
            return (String) t.getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    @Override
    public void saveImageFile(int width, int height, boolean hasAlpha, int[] pixels, String img_filename) throws IOException{
        BufferedImage image = new BufferedImage(width, height, hasAlpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);

        for(int i = 0; i < height ; i++){
            for(int j = 0; j < width; j++){
                int idx = j + i * width;
                int value = pixels[idx];
                image.setRGB(j, i, value);
            }
        }

        String ext = hasAlpha ? "png" : "jpg";
        int dot = img_filename.lastIndexOf('.');
        if(dot >= 0){
            img_filename = img_filename.substring(0, dot + 1) + ext;
        }else{
            img_filename = img_filename + '.' + ext;
        }
        File _imageFile = new File(img_filename);
        _imageFile.getParentFile().mkdirs();
        ImageIO.write(image, ext, _imageFile);
    }
}
