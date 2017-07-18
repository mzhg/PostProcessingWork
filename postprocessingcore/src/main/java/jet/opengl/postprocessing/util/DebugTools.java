package jet.opengl.postprocessing.util;

import org.lwjgl.util.vector.Matrix2f;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

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

    public static final FileFilter CPLUS_FILTER = new FileExtFilter("h", "cpp", "hpp", "c");
    public static final FileFilter JAVA_FILTER = new FileExtFilter("java");

    public static final class FileExtFilter implements FileFilter {

        String[] extensions;
        public FileExtFilter(String ...extensions) {
            this.extensions = extensions;
        }

        @Override
        public boolean accept(File pathname) {
            if(pathname.isDirectory()) return true;
            String name = pathname.getName();
            int dot = name.lastIndexOf('.');
            if(dot < 0)
                return false;

            String ext = name.substring(dot + 1);
            return CommonUtil.contain(extensions, ext);
        }

    }

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

    public static ByteBuffer loadBinary(String filename){
        try(FileInputStream inputStream = new FileInputStream(filename)){
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);

            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(bytes.length);
            byteBuffer.put(bytes).flip();
            return byteBuffer;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void convertBinaryToText(String source, int internalFormat, int width, String destion) throws IOException {
        saveTexelsAsText(loadBinary(source), internalFormat, width, destion);
    }

    public static void saveTextureAsBinary(int target, int textureID, int level, String filename) throws IOException{
        write(getTextureData(target, textureID, level, true), filename, false);
    }

    public static void saveTextureAsText(int target, int textureID, int level, String filename) throws IOException{
        final int flags = RED_MASK | GREEN_MASK | BLUE_MASK | ALPHA_MASK;
        saveTextureAsText(target, textureID, level, filename, flags);
    }

    public static void saveBufferAsText(int target, int buffer, int internalFormat, int width, String filename) throws IOException{
        final int flags = RED_MASK | GREEN_MASK | BLUE_MASK | ALPHA_MASK;
        saveBufferAsText(target, buffer, internalFormat, width, filename, flags);
    }

    public static void saveBufferAsText(int target, int buffer, int internalFormat, int width, String filename, int flags) throws IOException{
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBindBuffer(target, buffer);
        int size = gl.glGetBufferParameteri(target, GLenum.GL_BUFFER_SIZE);
        ByteBuffer data = BufferUtils.createByteBuffer(size);
        gl.glGetBufferSubData(target, 0, size, data);

        saveTexelsAsText(data, internalFormat, width, filename, flags);
    }

    public static void saveBufferAsText(int target, int buffer, Class<?> internalFormat, int width, String filename) throws IOException{
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBindBuffer(target, buffer);
        int size = gl.glGetBufferParameteri(target, GLenum.GL_BUFFER_SIZE);
        ByteBuffer data = BufferUtils.createByteBuffer(size);
        gl.glGetBufferSubData(target, 0, size, data);

        saveTexelsAsText(data, internalFormat, width, filename);
    }

    public static void saveTextureAsText(int target, int textureID, int level, String filename, int flags) throws IOException{
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        ByteBuffer result = getTextureData(target, textureID, level, true);
        int internalFormat = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_INTERNAL_FORMAT);
        int width = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_WIDTH);

        saveTexelsAsText(result, internalFormat, width, filename, flags);
    }

    public static void saveTexelsAsText(ByteBuffer pixels, int internalFormat, int width, String filename) throws IOException{
        saveTexelsAsText(pixels, internalFormat, width, filename, RED_MASK | GREEN_MASK | BLUE_MASK | ALPHA_MASK);
    }

    public static void saveTexelsAsText(ByteBuffer pixels, int internalFormat, int width, String filename, int flags) throws IOException{
        File file = new File(filename);
        File parent = file.getParentFile();
        parent.mkdirs();

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        int count = 0;
        while(pixels.hasRemaining()){
            String str = getTexelToString(pixels, internalFormat, flags);
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

    public static void saveTexelsAsText(ByteBuffer pixels, Class<?> internalFormat, int width, String filename) throws IOException, IllegalArgumentException{
        File file = new File(filename);
        File parent = file.getParentFile();
        parent.mkdirs();

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        _saveTexelsAsText(pixels, internalFormat, width, writer);
        writer.flush();
        writer.close();
    }

    public static Object newInstance(Class<?> clazz)  {
        Constructor<?> constructor = null;
        try {
            constructor =  clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            constructor = null;
        }

        if(constructor != null){
            constructor.setAccessible(true);
        }

        try {
            return constructor != null ? constructor.newInstance() : null;
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }
    }

    private static void _saveTexelsAsText(ByteBuffer pixels, Class<?> internalFormat, int width,BufferedWriter out) throws IOException, IllegalArgumentException{
        int count = 0;
        List<ClassType> types = new ArrayList<>();

        Object object = newInstance(internalFormat);
        try {
            flatType(object, internalFormat, types);
        } catch (IllegalAccessException e) {
//            e.printStackTrace();
        } catch (InstantiationException e) {
//            e.printStackTrace(); Don't the throw exception if couldn't acess the default construct of the class.
        }

        while(pixels.hasRemaining()){
            String str = formatBinary(pixels, types);
            out.append(str);
            out.append(' ');
            count++;

            if(count == width){
                count = 0;
                out.newLine();
            }
        }
    }

    private static String formatBinary(ByteBuffer pixel, List<ClassType> types){
        StringBuilder out = new StringBuilder();
        out.append('[');
        for(ClassType type : types){
            int count = type.arraySize;
            for(int i = 0; i < count; i++) {
                if (type.clazz == boolean.class || type.clazz == int.class) {
                    out.append(pixel.getInt()).append(',');
                } else if (type.clazz == float.class) {
                    out.append(pixel.getFloat()).append(',');
                } else if (type.clazz == Vector2f.class) {
                    out.append(pixel.getFloat()).append(',');
                    out.append(pixel.getFloat()).append(',');
                } else if (type.clazz == Vector3f.class) {
                    out.append(pixel.getFloat()).append(',');
                    out.append(pixel.getFloat()).append(',');
                    out.append(pixel.getFloat()).append(',');
                } else if (type.clazz == Vector4f.class || type.clazz == Matrix2f.class) {
                    out.append(pixel.getFloat()).append(',');
                    out.append(pixel.getFloat()).append(',');
                    out.append(pixel.getFloat()).append(',');
                    out.append(pixel.getFloat()).append(',');
                }else if (type.clazz == Matrix3f.class){
                    for(int j = 0; j < 9; j++){
                        out.append(pixel.getFloat()).append(',');
                    }
                }else if (type.clazz == Matrix4f.class){
                    for(int j = 0; j < 16; j++){
                        out.append(pixel.getFloat()).append(',');
                    }
                }
            }
        }

        out.setLength(out.length() - 1);
        out.append(']');
        return out.toString();
    }

    private static final class ClassType{
        private static final Class<?>[] ACCETS = {
                boolean.class,
          int.class, float.class, Vector2f.class, Vector3f.class, Vector4f.class,
                Matrix2f.class, Matrix3f.class, Matrix4f.class
        };

        static boolean accept(Class<?> clazz){
            for(Class<?> src : ACCETS){
                if(src == clazz)
                    return true;
            }

            return false;
        }

        Class<?> clazz;
        int arraySize = 1;

        ClassType(Class<?> clazz, int arraySize){
            this.clazz = clazz;
            this.arraySize = arraySize;
        }

        ClassType(Class<?> clazz){
            this.clazz = clazz;
        }
    }

    private static void flatType(Object obj, Class<?> clazz, List<ClassType> types) throws IllegalAccessException, InstantiationException {
        if(ClassType.accept(clazz)){
            types.add(new ClassType(clazz));
            return;
        }

        Field[] fields = clazz.getDeclaredFields();
        for(Field field : fields){
            if(Modifier.isStatic(field.getModifiers()))
                continue;

            field.setAccessible(true);
            final Class<?> type = field.getType();
            if(type.isArray() && obj != null){
                final int arrayLength = Array.getLength(field.get(obj));
                if(ClassType.accept(type.getComponentType())){
                    types.add(new ClassType(type, arrayLength));
                }else{
                    final int pos = types.size();
                    Object object = Array.get(field.get(obj), 0);
                    if(object == null){
                        object = type.getComponentType().newInstance();
                    }
                    flatType(object, type.getComponentType(), types);
                    final int added=types.size() - pos;
                    if(added  ==0)
                        throw new IllegalStateException();

                    ClassType[] newTypes = new ClassType[added];
                    for(int i = pos; i < types.size(); i++){
                        newTypes[i-pos] = types.get(i);
                    }

                    // add the newTypes to the tail repeatly
                    for(int i = 1; i < arrayLength; i++){
                        for(ClassType _type : newTypes){
                            types.add(_type);
                        }
                    }
                }
            }else if(!type.isArray()){ // Not the array
                if(ClassType.accept(type)){  // primitve type
                    types.add(new ClassType(type));
                }else if(type.isPrimitive()){
                    throw new IllegalArgumentException("Unsupport the type of " + type.getName());
                }else{
                    // continue to flat the self-def type
                    flatType(obj != null ? field.get(obj) : null, type , types);
                }
            }else{
                // TODO
                throw new IllegalArgumentException("We can't get the length of the array: " + field.getName() + ", if the obj not provided!!!");
            }
        }
    }

    public static void saveErrorShaderSource(CharSequence source){
        final String filename = "error_shader.txt";
        File file = new File(filename);
//        File parent = file.getParentFile();
//        if(parent != null)
//            parent.mkdirs();

        try {
            FileWriter writer = new FileWriter(file);
            writer.append(source);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                sb.append(_To(rgba[i])).append(',');
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

    private static String _To(float f){
        if(f - Math.ceil(f) == 0.0){
            return Integer.toString((int)f);
        }else if(Math.abs(f) < 1e-7){
            return "0";
        }else{
            return Float.toString(f);
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

    public static int getCodeLineNumbers(String folder, FileFilter filter){
        return getCodeLineNumbers(new File(folder), filter);
    }

    public static int getCodeLineNumbers(File root, FileFilter filter){
        int count = 0;

        File[] files = root.listFiles(filter);
        for(File f: files){
            if(f.isDirectory()){
                count += getCodeLineNumbers(f, filter);
            }else{
                try {
                    BufferedReader in = new BufferedReader(new FileReader(f));
                    while(in.readLine() != null){
                        count ++;
                    }
                    in.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return count;
    }

    public static void searchTextFromFiles(File root, FileFilter filter, String text){
        File[] files = root.listFiles(filter);
        for(File f: files){
            if(f.isDirectory()){
                searchTextFromFiles(f, filter, text);
            }else{
                try {
                    BufferedReader in = new BufferedReader(new FileReader(f));
                    String line;
                    while((line = in.readLine()) != null){
                        if(line.contains(text)){
                            System.out.println(f.getAbsolutePath());
                            break;
                        }
                    }
                    in.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public interface LineCompare{
        boolean equals(String src, String dst, CompareResult result);
    }

    private static final class CompareResult{
        final List<String> srcMissTokens = new ArrayList<String>();
        final List<String> dstMissTokens = new ArrayList<String>();
        final List<Integer> missTokenAtIndexs = new ArrayList<Integer>();

        int lineTokens;

        void clear() {
            srcMissTokens.clear();
            dstMissTokens.clear();
            missTokenAtIndexs.clear();
        }

        int size() { return srcMissTokens.size();}
        boolean isEmpty() { return srcMissTokens.isEmpty();}

        void add(String srcMissToken, String dstMissToken, int index){
            srcMissTokens.add(srcMissToken);
            dstMissTokens.add(dstMissToken);
            missTokenAtIndexs.add(index);
        }
    }

    private static String mkToken(float[] a){
        StringBuilder s = new StringBuilder(32);
        for(float v : a){
            s.append(_To(v)).append(',');
        }
        s.setLength(s.length() - 1);
        return s.toString();
    }

    public static final LineCompare PIXELS_COMPARE = new LineCompare() {
        final List<float[]> srcValues = new ArrayList<float[]>();
        final List<float[]> dstValues = new ArrayList<float[]>();

        public boolean equals(String src, String dst, CompareResult result) {
            result.clear();

            extractValues(src, srcValues);
            extractValues(dst, dstValues);
            result.lineTokens = srcValues.size();

//			if(srcValues.size() != dstValues.size()){
//				return false;
//			}

            for(int i = 0; i < srcValues.size(); i++){
                float[] fsrcValues = srcValues.get(i);
                float[] fdstValues = dstValues.get(i);

                for(int j = 0; j < fsrcValues.length; j ++){
                    float ogl_value = fsrcValues[j];
                    float dx_value = fdstValues[j];
                    if(!Numeric.isClose(ogl_value, dx_value, 0.01f)){
                        result.add(mkToken(fsrcValues), mkToken(fdstValues), i);
                        break;
                    }
                }
            }

            return result.isEmpty();
        }
    };

    public static final LineCompare ANGLE_COMPARE = new LineCompare() {
        final List<float[]> srcValues = new ArrayList<float[]>();
        final List<float[]> dstValues = new ArrayList<float[]>();

        private final Vector3f srcVec = new Vector3f();
        private final Vector3f dstVec = new Vector3f();

        public boolean equals(String src, String dst, CompareResult result) {
            result.clear();

            extractValues(src, srcValues);
            extractValues(dst, dstValues);
            result.lineTokens = srcValues.size();

//			if(srcValues.size() != dstValues.size()){
//				return false;
//			}

            for(int i = 0; i < srcValues.size(); i++){
                float[] fsrcValues = srcValues.get(i);
                float[] fdstValues = dstValues.get(i);

                int length = Math.max(3, fsrcValues.length);
                for(int j = 0; j < length; j ++){
                    float ogl_value = fsrcValues[j];
                    float dx_value = fdstValues[j];

                    srcVec.setValue(j, ogl_value);
                    dstVec.setValue(j, dx_value);
                }

                double degress = Math.toDegrees(Vector3f.angle(srcVec, dstVec));
                if(degress < 0.0 || degress > 2.0){
                    result.add(mkToken(fsrcValues), mkToken(fdstValues), i);
                }
            }

            return result.isEmpty();
        }
    };

    public static void extractValues(String line, List<float[]> values){
        int start = 0;
        int end = 0;

        if(line == null)
            return;

        // 4 of elements enough for us.
        final StackFloat tokens = new StackFloat(4);

        int token_index = 0;
        while(true){
            tokens.clear();
            start = line.indexOf('[', end);
            if(start < 0) break;

            end = line.indexOf(']', start);
            if(end < 0) break;

//			System.out.println("(S,E) = (" +start + ", " + end + ")");
            int index = line.indexOf(',', start);
            int prev  = start + 1;
            while(index > 0 && index < end){
                String token = line.substring(prev, index);
//				System.out.println("Token = " + token);
                tokens.push(parseFloat(token));

                prev = index + 1;
                index = line.indexOf(',', prev);
            }

            tokens.push(parseFloat(line.substring(prev, end)));

            float[] arr;
            if(values.size() > token_index){
                arr = values.get(token_index);
                if(arr == null || arr.length != tokens.size()){
                    arr = new float[tokens.size()];
                    values.set(token_index, arr);
                }
            }else{
                arr = new float[tokens.size()];
                values.add(arr);
            }

            System.arraycopy(tokens.getData(), 0, arr, 0, arr.length);
            token_index++;
        }

        while(values.size() > token_index){
            values.remove(values.size() - 1);
        }
    }

    private static float parseFloat(String token){
        if(token.contains(".#QNAN")){
            return Float.NaN;
        }else if(token.contains(".#INF")){
            return Float.POSITIVE_INFINITY;
        }

        return Float.parseFloat(token);
    }

    public static void saveStencilAsImage(String filename, String img_filename, HashMap<Integer, Color> colorMap){
        try(BufferedReader in = new BufferedReader(new FileReader(filename))){
            String line;
            final List<float[]> dsValues = new ArrayList<float[]>();
            StackInt allValues = new StackInt(1280 * 720);

            int width = 0;
            int height = 0;
            while((line = in.readLine()) != null){
                extractValues(line, dsValues);

                for(float[] ds : dsValues){
                    if(ds.length > 0)
                        allValues.push((int)ds[1]);
                    else
                        allValues.push((int)ds[0]);
                }

                height ++;

                if(width == 0){
                    width = dsValues.size();
                }else if(width != dsValues.size()){
                    throw new IllegalArgumentException();
                }
            }

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            int[] stencilValues = allValues.getData();
            for(int i = 0; i < height; i++){
                for(int j = 0; j < width; j++){
                    int idx = j + i * width;
                    int stencil = stencilValues[idx];
                    Color color = colorMap.get(stencil);
                    if(color == null){
                        color = Color.BLACK;
                    }

                    image.setRGB(j, i, color.getRGB());
                }
            }

            int dot = img_filename.lastIndexOf('.');
            if(dot >= 0){
                img_filename = img_filename.substring(0, dot + 1) + "jpg";
            }else{
                img_filename = img_filename + ".jpg";
            }

            ImageIO.write(image, "jpg", new File(img_filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void fileCompare(String srcFile, String dstFile, String outputFile){
        fileCompare(srcFile, dstFile, outputFile, null);
    }

    public static void fileCompare(String srcFile, String dstFile, String outputFile, LineCompare compare){
        if(compare == null){
            compare = PIXELS_COMPARE;
        }

        try {
            BufferedReader srcIn = new BufferedReader(new FileReader(srcFile));
            BufferedReader dstIn = new BufferedReader(new FileReader(dstFile));
            BufferedWriter resultOut = new BufferedWriter(new FileWriter(outputFile));

            final CompareResult lineResult = new CompareResult();
            int lineNumber = 1;
            int lineMissMatchNumer = 0;
            int tokenMissMachNumber = 0;
            int totalTokens = 0;

            int xmin = Integer.MAX_VALUE, xmax = -1;
            int ymin = Integer.MAX_VALUE, ymax = -1;

            while(true){
                String srcLine = srcIn.readLine();
                String dstLine = dstIn.readLine();

                if(srcLine == null && dstLine == null){
                    break;
                }

                if(!compare.equals(srcLine, dstLine, lineResult)){
                    tokenMissMachNumber += lineResult.size();

                    resultOut.write(String.valueOf(lineNumber));
                    resultOut.write(": ");

                    final int COLS = 7;
                    for(int i = 0; i < lineResult.size(); i++){
                        if((i > 0) && (i % COLS == 0)){
                            resultOut.append('\n').append('\t');  // new line.
                        }

                        resultOut.append('[');
                        resultOut.append(lineResult.missTokenAtIndexs.get(i).toString());
                        resultOut.append(':');
                        resultOut.append('(').append(lineResult.srcMissTokens.get(i)).append(')');
                        resultOut.append('-');
                        resultOut.append('(').append(lineResult.dstMissTokens.get(i)).append(')');
                        resultOut.append(']');
                    }

                    resultOut.append('\n');
                    lineMissMatchNumer ++;

                    xmin = Math.min(xmin, lineResult.missTokenAtIndexs.get(0));
                    ymin = Math.min(ymin, lineNumber);

                    xmax = Math.max(xmax, lineResult.missTokenAtIndexs.get(lineResult.size() - 1));
                    ymax = Math.max(ymax, lineNumber);
                }

                totalTokens += lineResult.lineTokens;
                lineNumber++;
            }

            System.out.println("LineMissMatchNumer = "+ lineMissMatchNumer);
            System.out.println("tokenMissMachNumber = "+ tokenMissMachNumber);
            System.out.println("Correct rate: "+ ((float)(totalTokens - tokenMissMachNumber)/totalTokens));

            if(tokenMissMachNumber < 0 ){
                System.out.println("Left_TOP: (" + xmin + ", " + ymin + ")");
                System.out.println("RIGHT_BOTTOM: (" + xmax + ", " + ymax + ")");
            }
            srcIn.close();
            dstIn.close();
            resultOut.flush();
            resultOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
