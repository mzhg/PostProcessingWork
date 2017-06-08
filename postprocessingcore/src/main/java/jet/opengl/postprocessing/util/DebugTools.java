package jet.opengl.postprocessing.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

import static jet.opengl.postprocessing.texture.TextureUtils.getTextureData;

/**
 * Created by mazhen'gui on 2017/6/8.
 */

public final class DebugTools {
    /** The red or depth component */
    public static final int RED_MASK = 0x1;
    /** The green or stencil component */
    public static final int GREEN_MASK = 0x2;
    /** The blue component */
    public static final int BLUE_MASK = 0x4;
    /** The alpha component */
    public static final int ALPHA_MASK = 0x8;

    private static final int[] RGBA_MASK = {
            RED_MASK, GREEN_MASK, BLUE_MASK, ALPHA_MASK
    };

    public static void write(ByteBuffer data, String filename, boolean append) throws IOException {
        File file = new File(filename);
        write(data, file, append);
    }

    public static void write(ByteBuffer data, File file, boolean append) throws IOException{
        File parent = file.getParentFile();
        if(!parent.exists())
            parent.mkdirs();
        @SuppressWarnings("resource")
        FileChannel out = new FileOutputStream(file, append).getChannel();
        out.write(data);
        out.close();
    }

    public static void saveTextureAsBinary(int target, int textureID, int level, String filename) throws IOException{
        write(getTextureData(target, textureID, level, true), filename, false);
    }

    public static void saveTextureAsText(int target, int textureID, int level, String filename) throws IOException{
        final int flags = RED_MASK | GREEN_MASK | BLUE_MASK | ALPHA_MASK;
        saveTextureAsText(target, textureID, level, filename, flags);
    }
    public static void saveTextureAsText(int target, int textureID, int level, String filename, int flags) throws IOException{
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        ByteBuffer result = getTextureData(target, textureID, level, true);
        int internalFormat = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_INTERNAL_FORMAT);
        int width = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_WIDTH);

        saveTexelsAsText(result, internalFormat, width, filename, flags);
    }

    public static void saveTexelsAsText(ByteBuffer pixels, int format, int width, String filename) throws IOException{
        saveTexelsAsText(pixels, format, width, filename, RED_MASK | GREEN_MASK | BLUE_MASK | ALPHA_MASK);
    }

    public static void saveTexelsAsText(ByteBuffer pixels, int format, int width, String filename, int flags) throws IOException{
        File file = new File(filename);
        File parent = file.getParentFile();
        parent.mkdirs();

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        int count = 0;
        while(pixels.hasRemaining()){
            String str = getTexelToString(pixels, format, flags);
            writer.append(str);
            writer.append(' ');
            count++;

            if(count == width){
                count = 0;
                writer.newLine();
            }
        }

        writer.flush();
        writer.close();
    }

    public static void saveTextureAsText(int target, int textureID, int level, String filename, int cmpSize, int dataType) throws IOException{
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        ByteBuffer result = getTextureData(target, textureID, level, true);
        int width = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_WIDTH);

        File file = new File(filename);
        File parent = file.getParentFile();
        parent.mkdirs();

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        int count = 0;
//		int totalCount = 0;
        while(result.hasRemaining()){
            writer.append('[');
            for(int i = 0;i < cmpSize; i++){
                switch (dataType) {
                    case GLenum.GL_BYTE:
                        writer.append(Integer.toString(result.get()));
                        break;
                    case GLenum.GL_UNSIGNED_BYTE:
                        writer.append(Integer.toString(result.get()&0xFF));
                        break;
                    case GLenum.GL_SHORT:
                        writer.append(Integer.toString(result.getShort()));
                        break;
                    case GLenum.GL_UNSIGNED_SHORT:
                        writer.append(Integer.toString(result.getShort()&0xFFFF));
                        break;
                    case GLenum.GL_INT:
                    case GLenum.GL_UNSIGNED_INT:
                        writer.append(Integer.toString(result.getInt()));
                        break;
                    case GLenum.GL_FLOAT:
                        writer.append(Float.toString(result.getFloat()));
                        break;
                    default:
                        break;
                }

                if(i != cmpSize - 1){
                    writer.append(',');
                }
            }

            writer.append(']');
            writer.append(' ');
            count++;
//			totalCount++;

            if(count == width){
                count = 0;
                writer.newLine();
            }

//			System.out.println("TotalCount: " + totalCount);
        }

        writer.flush();
        writer.close();
    }

    private static String getTexelToString(ByteBuffer pixels, int internalFormat, int flags){
        switch (internalFormat) {
            case GLenum.GL_R8:
            {
                float r = (float)(pixels.get() & 0xFF) / 0xFF;
                return _To(flags, r);

            }
            case GLenum.GL_R8_SNORM:
            {
                float r = (float)pixels.get() / Byte.MAX_VALUE;
                return _To(flags, r);

            }
            case GLenum.GL_R16:
            {
                float r = (float)(pixels.getShort() & 0xFFFF) / 0xFFFF;
                return _To(flags, r);
            }
            case GLenum.GL_R16_SNORM :
            {
                float r = (float)pixels.getShort() / Short.MAX_VALUE;
                return _To(flags, r);
            }
            case GLenum.GL_RG8:
            {
                float r = (float)(pixels.get() & 0xFF) / 0xFF;
                float g = (float)(pixels.get() & 0xFF) / 0xFF;
                return _To(flags, r, g);

            }
            case GLenum.GL_RG8_SNORM:
            {
                float r = (float)pixels.get() / Byte.MAX_VALUE;
                float g = (float)pixels.get() / Byte.MAX_VALUE;
                return _To(flags, r, g);

            }
            case GLenum.GL_RG16:
            {
                float r = (float)(pixels.getShort() & 0xFFFF) / 0xFFFF;
                float g = (float)(pixels.getShort() & 0xFFFF) / 0xFFFF;
                return _To(flags, r, g);

            }
            case GLenum.GL_RG16_SNORM:
            {
                float r = (float)pixels.getShort() / Short.MAX_VALUE;
                float g = (float)pixels.getShort() / Short.MAX_VALUE;
                return _To(flags, r, g);

            }
            case GLenum.GL_R3_G3_B2:
            {
                final int redMask = 0b111;
                final int greenMask = 0b111;
                final int blueMask = 0b11;

                int pixel = pixels.get() & 0xFF;
                float r = (float)(pixel & redMask) / redMask;
                float g = (float)((pixel >> 3) & greenMask) / greenMask;
                float b = (float)((pixel >> 6) & blueMask) / blueMask;

                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGB4:
            {
                final int mask = 0b1111;
                int pixel = pixels.getShort() & 0xFFFF;
                float r = (float)(pixel & mask) / mask;
                float g = (float)((pixel >> 4) & mask) / mask;
                float b = (float)((pixel >> 8) & mask) / mask;
                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGB5:
            {
                final int mask = 0b11111;
                int pixel = pixels.getShort() & 0xFFFF;
                float r = (float)(pixel & mask) / mask;
                float g = (float)((pixel >> 5) & mask) / mask;
                float b = (float)((pixel >> 10) & mask) / mask;
                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGB8:
            {
                final int mask = 0xFF;
                float r = (float)(pixels.get() & mask) / mask;
                float g = (float)(pixels.get() & mask) / mask;
                float b = (float)(pixels.get() & mask) / mask;
                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGB8_SNORM:
            {
                final int mask = Byte.MAX_VALUE;
                float r = (float)pixels.get() / mask;
                float g = (float)pixels.get() / mask;
                float b = (float)pixels.get() / mask;
                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGB10:
            {
                final int mask = 0b1111111111;
                int pixel = pixels.getInt();
                float r = (float)(pixel & mask) / mask;
                float g = (float)((pixel >> 10) & mask) / mask;
                float b = (float)((pixel >> 20) & mask) / mask;
                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGB12:
            {
//			final int mask = 0b111111111111;
//			int pixel = pixels.getInt();
//			float r = (float)(pixel & mask) / mask;
//			float g = (float)((pixel >> 10) & mask) / mask;
//			float b = (float)((pixel >> 20) & mask) / mask;
//			return "[" + r + "," + g + "," + b + "]";

                throw new RuntimeException("Parse GL_RGB12 failed!");
            }
            case GLenum.GL_RGB16:
            {
                final int mask = 0xFFFF;
                float r = (float)(pixels.getShort() & mask) / mask;
                float g = (float)(pixels.getShort() & mask) / mask;
                float b = (float)(pixels.getShort() & mask) / mask;
                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGB16_SNORM:
            {
                final int mask = Short.MAX_VALUE;
                float r = (float)pixels.getShort() / mask;
                float g = (float)pixels.getShort() / mask;
                float b = (float)pixels.getShort() / mask;
                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGBA2:
            {
                final int mask = 0b11;
                int pixel = pixels.get() & 0xFF;
                float r = (float)(pixel & mask) / mask;
                float g = (float)((pixel >> 2) & mask) / mask;
                float b = (float)((pixel >> 4) & mask) / mask;
                float a = (float)((pixel >> 6) & mask) / mask;
                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_RGBA4:
            {
                final int mask = 0b1111;
                int pixel = pixels.getShort() & 0xFFFF;
                float r = (float)(pixel & mask) / mask;
                float g = (float)((pixel >> 4) & mask) / mask;
                float b = (float)((pixel >> 8) & mask) / mask;
                float a = (float)((pixel >> 12) & mask) / mask;
                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_RGB5_A1:
            {
                final int mask = 0b11111;
                int pixel = pixels.getShort() & 0xFFFF;
                float r = (float)(pixel & mask) / mask;
                float g = (float)((pixel >> 5) & mask) / mask;
                float b = (float)((pixel >> 10) & mask) / mask;
                float a = (float)((pixel >> 15) & 0b1) / 0b1;
                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_RGBA8:
            {
                final int mask = 0xFF;
                float r = (float)(pixels.get() & mask) / mask;
                float g = (float)(pixels.get() & mask) / mask;
                float b = (float)(pixels.get() & mask) / mask;
                float a = (float)(pixels.get() & mask) / mask;
                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_RGBA8_SNORM:
            {
                final int mask = Byte.MAX_VALUE;
                float r = (float)pixels.get() / mask;
                float g = (float)pixels.get() / mask;
                float b = (float)pixels.get() / mask;
                float a = (float)pixels.get() / mask;
                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_RGB10_A2:
            {
                final int mask = 0b1111111111;
                int pixel = pixels.getInt();
                float r = (float)(pixel & mask) / mask;
                float g = (float)((pixel >> 10) & mask) / mask;
                float b = (float)((pixel >> 20) & mask) / mask;
                float a = (float)((pixel >> 30) & 0b11) / 0b11;
                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_RGB10_A2UI:
            {
                final int mask = 0b1111111111;
                int pixel = pixels.getInt();
                int r = pixel & mask;
                int g = ((pixel >> 10) & mask);
                int b = ((pixel >> 20) & mask);
                int a = ((pixel >> 30) & 0b11);
                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_RGBA12:
            {
                final int mask = 0b111111111111;
                int pixel = pixels.getShort();  // RG
                int r = pixel & mask;
                int g = ((pixel >> 12) & mask);

                pixel = pixels.getShort();  // BA
                int b = ((pixel >> 0) & mask);
                int a = ((pixel >> 12) & mask);
                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_RGBA16:
            {
                final int mask = 0xFFFF;
                float r = (float)(pixels.getShort() & mask) / mask;
                float g = (float)(pixels.getShort() & mask) / mask;
                float b = (float)(pixels.getShort() & mask) / mask;
                float a = (float)(pixels.getShort() & mask) / mask;
                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_SRGB8:
            case GLenum.GL_SRGB8_ALPHA8:
            {
                throw new RuntimeException("Unspport the SRGB format");
            }
            case GLenum.GL_R16F:
            {
                float r = Numeric.convertHFloatToFloat(pixels.getShort());
                return _To(flags, r);
            }
            case GLenum.GL_RG16F:
            {
                float r = Numeric.convertHFloatToFloat(pixels.getShort());
                float g = Numeric.convertHFloatToFloat(pixels.getShort());

                return _To(flags, r, g);
            }
            case GLenum.GL_RGB16F:
            {
                float r = Numeric.convertHFloatToFloat(pixels.getShort());
                float g = Numeric.convertHFloatToFloat(pixels.getShort());
                float b = Numeric.convertHFloatToFloat(pixels.getShort());

                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGBA16F:
            {
                float r = Numeric.convertHFloatToFloat(pixels.getShort());
                float g = Numeric.convertHFloatToFloat(pixels.getShort());
                float b = Numeric.convertHFloatToFloat(pixels.getShort());
                float a = Numeric.convertHFloatToFloat(pixels.getShort());

                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_R32F:
            {
                float r = pixels.getFloat();
                return _To(flags, r);
            }
            case GLenum.GL_RG32F:
            {
                float r = pixels.getFloat();
                float g = pixels.getFloat();

                return _To(flags, r, g);
            }
            case GLenum.GL_RGB32F:
            {
                float r = pixels.getFloat();
                float g = pixels.getFloat();
                float b = pixels.getFloat();

                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGBA32F:
            {
                float r = pixels.getFloat();
                float g = pixels.getFloat();
                float b = pixels.getFloat();
                float a = pixels.getFloat();

                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_R11F_G11F_B10F:
            {
                throw new RuntimeException("Unspport the GL_R11F_G11F_B10F format");
            }
            case GLenum.GL_RGB9_E5:
            {
                throw new RuntimeException("Unspport the GL_RGB9_E5 format");
            }
            case GLenum.GL_R8I:
            {
                int r = pixels.get();
                return _To(flags, r);
            }
            case GLenum.GL_R8UI:
            {
                int r = pixels.get() & 0xFF;
                return _To(flags, r);
            }
            case GLenum.GL_R16I:
            {
                int r = pixels.getShort();
                return _To(flags, r);
            }
            case GLenum.GL_R16UI:
            {
                int r = pixels.getShort() & 0xFFFF;
                return _To(flags, r);
            }
            case GLenum.GL_R32I:
            {
                int r = pixels.getInt();
                return _To(flags, r);
            }
            case GLenum.GL_R32UI:
            {
                long r = Numeric.unsignedInt(pixels.getInt());
                return _To(flags, r);
            }
            case GLenum.GL_RG8I:
            {
                int r = pixels.get();
                int g = pixels.get();

                return _To(flags, r, g);
            }
            case GLenum.GL_RG8UI:
            {
                int r = pixels.get() & 0xFF;
                int g = pixels.get() & 0xFF;

                return _To(flags, r, g);
            }
            case GLenum.GL_RG16I:
            {
                int r = pixels.getShort();
                int g = pixels.getShort();

                return _To(flags, r, g);
            }
            case GLenum.GL_RG16UI:
            {
                int r = pixels.getShort() & 0xFFFF;
                int g = pixels.getShort() & 0xFFFF;

                return _To(flags, r, g);
            }
            case GLenum.GL_RG32I:
            {
                int r = pixels.getInt();
                int g = pixels.getInt();

                return _To(flags, r, g);
            }
            case GLenum.GL_RG32UI:
            {
                long r = Numeric.unsignedInt(pixels.getInt());
                long g = Numeric.unsignedInt(pixels.getInt());

                return _To(flags, r, g);
            }
            case GLenum.GL_RGB8I:
            {
                int r = pixels.get();
                int g = pixels.get();
                int b = pixels.get();

                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGB8UI:
            {
                int r = pixels.get() & 0xFF;
                int g = pixels.get() & 0xFF;
                int b = pixels.get() & 0xFF;

                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGB16I:
            {
                int r = pixels.getShort();
                int g = pixels.getShort();
                int b = pixels.getShort();

                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGB16UI:
            {
                int r = pixels.getShort() & 0xFFFF;
                int g = pixels.getShort() & 0xFFFF;
                int b = pixels.getShort() & 0xFFFF;

                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGB32I:
            {
                int r = pixels.getInt();
                int g = pixels.getInt();
                int b = pixels.getInt();

                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGB32UI:
            {
                long r = Numeric.unsignedInt(pixels.getInt());
                long g = Numeric.unsignedInt(pixels.getInt());
                long b = Numeric.unsignedInt(pixels.getInt());

                return _To(flags, r, g, b);
            }
            case GLenum.GL_RGBA8I:
            {
                int r = pixels.get();
                int g = pixels.get();
                int b = pixels.get();
                int a = pixels.get();

                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_RGBA8UI:
            {
                int r = pixels.get() & 0xFF;
                int g = pixels.get() & 0xFF;
                int b = pixels.get() & 0xFF;
                int a = pixels.get() & 0xFF;

                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_RGBA16I:
            {
                int r = pixels.getShort();
                int g = pixels.getShort();
                int b = pixels.getShort();
                int a = pixels.getShort();

                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_RGBA16UI:
            {
                int r = pixels.getShort() & 0xFFFF;
                int g = pixels.getShort() & 0xFFFF;
                int b = pixels.getShort() & 0xFFFF;
                int a = pixels.getShort() & 0xFFFF;

                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_RGBA32I:
            {
                int r = pixels.get();
                int g = pixels.get();
                int b = pixels.get();
                int a = pixels.get();

                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_RGBA32UI:
            {
                long r = Numeric.unsignedInt(pixels.getInt());
                long g = Numeric.unsignedInt(pixels.getInt());
                long b = Numeric.unsignedInt(pixels.getInt());
                long a = Numeric.unsignedInt(pixels.getInt());

                return _To(flags, r, g, b, a);
            }
            case GLenum.GL_DEPTH_COMPONENT16:
            {
                float r = (float)(pixels.getShort() & 0xFFFF) / 0xFFFF;
                return _To(flags, r);
            }
            case GLenum.GL_DEPTH_COMPONENT24:
            {
                final int mask = 0xFFFFFF;
                int pixel = Numeric.makeRGBA(pixels.get()&0xFF, pixels.get()&0xFF, pixels.get()&0xFF, 0);
                float r = (float)(pixel & mask) / mask;
                return _To(flags, r);
            }
            case GLenum.GL_DEPTH_COMPONENT32:
            {
                double  r = (double)(Numeric.unsignedInt(pixels.getInt())) / 0xFFFFFFFFL;
                return _To(flags, (float)r);
            }
            case GLenum.GL_DEPTH_COMPONENT32F:
            {
                float r = pixels.getFloat();
                return _To(flags, r);
            }
            case GLenum.GL_DEPTH24_STENCIL8:
            {
                final int mask = 0xFFFFFF;
                int g = pixels.get() & 0xFF;
                int pixel = Numeric.makeRGBA(pixels.get()&0xFF, pixels.get()&0xFF, pixels.get()&0xFF, 0);
                float r = (float)(pixel & mask) / mask;
                return _ToSD(flags, r, g);
            }
            case GLenum.GL_DEPTH32F_STENCIL8:
            {
                float r = pixels.getFloat();
                int g = pixels.get() & 0xFF;  // TODO
                return _ToSD(flags, r, g);
            }

            default:
                throw new IllegalArgumentException("Unkown internalFormat: " + internalFormat);
        }
    }

    private static String _To(int flags, long... rgba){
        int count = 0;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int length = rgba.length;
        for(int i = 0; i < length; i++){
            if((flags & RGBA_MASK[i]) != 0){
                count++;
                sb.append(rgba[i]).append(',');
            }
        }

        if(count > 0){
            sb.setLength(sb.length() - 1);
            sb.append(']');
            return sb.toString();
        }else{
            return "";
        }
    }

    private static String _To(int flags, float... rgba){
        int count = 0;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int length = rgba.length;
        for(int i = 0; i < length; i++){
            if((flags & RGBA_MASK[i]) != 0){
                count++;
                sb.append(rgba[i]).append(',');
            }
        }

        if(count > 0){
            sb.setLength(sb.length() - 1);
            sb.append(']');
            return sb.toString();
        }else{
            return "";
        }
    }

    private static String _To(int flags, int... rgba){
        int count = 0;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int length = rgba.length;
        for(int i = 0; i < length; i++){
            if((flags & RGBA_MASK[i]) != 0){
                count++;
                sb.append(rgba[i]).append(',');
            }
        }

        if(count > 0){
            sb.setLength(sb.length() - 1);
            sb.append(']');
            return sb.toString();
        }else{
            return "";
        }
    }

    private static String _ToSD(int flags, float r, int g){
        int count = 0;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        if((flags & RGBA_MASK[0]) != 0){
            count++;
            sb.append(r).append(',');
        }

        if((flags & RGBA_MASK[1]) != 0){
            count++;
            sb.append(g).append(',');
        }

        if(count > 0){
            sb.setLength(sb.length() - 1);
            sb.append(']');
            return sb.toString();
        }else{
            return "";
        }
    }
}
