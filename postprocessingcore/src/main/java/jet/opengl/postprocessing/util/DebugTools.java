package jet.opengl.postprocessing.util;

import org.lwjgl.util.vector.Matrix2f;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.WritableVector;

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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.NativeAPI;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;

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

        try (FileChannel out = new FileOutputStream(file, append).getChannel()){
            out.write(data);
            out.close();
        }
    }

    public static void write(String data, String filename){
        try (BufferedWriter out = new BufferedWriter(new FileWriter(filename))){
            out.write(data);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static byte[] loadBytes(String filename){
        try(FileInputStream inputStream = new FileInputStream(filename)){
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            return bytes;
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
        write(TextureUtils.getTextureData(target, textureID, level, true), filename, false);
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
        gl.glBindTexture(target, textureID);
        ByteBuffer result = TextureUtils.getTextureData(target, textureID, level, true);
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

    public final static void saveBinary(ByteBuffer data, String filename){
        saveBinary(data, filename, false);
    }

    public final static void saveBinary(ByteBuffer data, String filename, boolean append){
        File file = new File(filename);
        saveBinary(data, file, append);
    }

    public final static void saveBinary(ByteBuffer data, File file, boolean append){
        File parent = file.getParentFile();
        if(!parent.exists())
            parent.mkdirs();

        try (FileChannel out = new FileOutputStream(file, append).getChannel()){
            out.write(data);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static void saveTextureAsImageFile(Texture2D texture, String outputfile) throws IOException {
        NativeAPI nativeAPI = GLFuncProviderFactory.getGLFuncProvider().getNativeAPI();
        if(nativeAPI == null)
            return;

        ByteBuffer pixels = TextureUtils.getTextureData(texture.getTarget(), texture.getTexture(), 0, false);
        int width = texture.getWidth();
        int height = texture.getHeight();

        int[] out_pixels = new int[width * height];
        for(int i = height - 1; i >= 0 ; i--){  // flipping
            for(int j = 0; j < width; j++){
                int idx = j + i * width;
                out_pixels[idx] = getPixel(pixels, texture.getFormat());
            }
        }

        nativeAPI.saveImageFile(width, height, true, out_pixels, outputfile);
    }

    private static int getPixel(ByteBuffer src, int internalFormat){
        switch (internalFormat){
            case GLenum.GL_RGBA8:
            case GLenum.GL_RGBA:
            {
                int r = Numeric.unsignedByte(src.get()),   // red
                    g = Numeric.unsignedByte(src.get()),   // green
                    b = Numeric.unsignedByte(src.get()),   // blue
                    a = Numeric.unsignedByte(src.get());    // alpha
                return Numeric.makeRGBA(
                        b,g,r,a
                );
            }
            case GLenum.GL_RGB8:
            case GLenum.GL_RGB:
            {
                return Numeric.makeRGBA(
                        Numeric.unsignedByte(src.get()),   // red
                        Numeric.unsignedByte(src.get()),   // green
                        Numeric.unsignedByte(src.get()),   // blue
                        255                                // alpha
                );
            }

            default:
                throw new RuntimeException("Unsupport format: " + TextureUtils.getFormatName(internalFormat));
        }
    }

    public static StringBuilder compareObjects(Object a, Object b){
        try {
            return _compareObjects(a,b);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static StringBuilder _compareObjects(Object a, Object b) throws IllegalAccessException {
        if(a.getClass() != b.getClass())
            throw new IllegalArgumentException("The types of the given objects doesn't match");

        Class<?> clazz = a.getClass();
        List<Field> fields = getFields(clazz);

        StringBuilder out = new StringBuilder(256);
        for(Field field : fields){
            field.setAccessible(true);
            if(Modifier.isStatic(field.getModifiers()))
                continue;

            Class<?> type = field.getType();
            boolean isArray = type.isArray();
            Class<?> componentType = type.getComponentType();
            String fieldName = field.getName();
            Class<?> actualType = isArray ? componentType : type;

            if(actualType.isPrimitive() || ClassType.accept(actualType)){
                Object aValue = field.get(a);
                Object bValue = field.get(b);

                if(aValue == null || bValue == null)
                    continue;

                int arraySize = isArray ? Array.getLength(aValue) : 1;
                for(int i = 0; i < arraySize; i++){
                    Object aSubValue = isArray ? Array.get(aValue, i) : aValue;
                    Object bSubValue = isArray ? Array.get(bValue, i) : bValue;

                    if(aSubValue.equals(bSubValue) == false){
                        out.append(fieldName);
                        if(isArray){
                            out.append('[').append(i).append(']');
                        }
                        out.append(": ");
                        out.append(aSubValue.toString());
                        out.append("---");
                        out.append(bSubValue.toString());
                        out.append('\n');
                    }
                }
            }
        }

        if(out.length() == 0){
            out.append("The two object is same complete.");
        }

        return out;
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

        int struct_size = measureStructSize(types);
        while(pixels.hasRemaining()){
            String str = formatBinary(pixels, types, struct_size);
            out.append(str);
            out.append(' ');
            count++;

            if(count == width){
                count = 0;
                out.newLine();
            }
        }
    }

    private static int measureStructSize(List<ClassType> types){
        int totalBytes = 0;

        for(ClassType type : types){
            final int type_bytes = ClassType.sizeof(type.clazz);
            if(type_bytes < 0)
                throw  new IllegalStateException("Inner error!!!");
            totalBytes += type_bytes * type.arraySize;
        }

        return totalBytes;
    }

    private static int measureStructSize(List<ClassType> types, final int stride){
        int totalBytes = 0;

        int remain_bytes = stride;
        ClassType lastType = null;
        for(ClassType type : types){
            final int type_bytes = ClassType.sizeof(type.clazz);
            if(type_bytes < 0)
                throw  new IllegalStateException("Inner error!!!");

            if(type.arraySize > 1){
                int array_bytes = type.arraySize * type_bytes;
                if(array_bytes > remain_bytes && remain_bytes < stride && (remain_bytes % type_bytes) != 0){ // remain_bytes must be > 0
                    if (lastType != null){
                        if(lastType.arraySize > 1){
                            lastType.arrayPadding = remain_bytes;
                        }else{
                            lastType.padding = remain_bytes;
                        }
                    }else{
                        throw new IllegalStateException("Inner Error!!!");
                    }

                    totalBytes += remain_bytes;
                    remain_bytes = stride;
                }

                type.padding = stride % type_bytes;
                int array_token_bytes = (type_bytes + type.padding) * type.arraySize;

                // TODO may be have problem here.
                totalBytes += array_token_bytes;
                if(remain_bytes != stride){
                    remain_bytes = (array_token_bytes - remain_bytes) % stride;
                }

            }else{  // Not an array
                if (type_bytes <= remain_bytes) {
                    totalBytes += type_bytes;
                    remain_bytes -= type_bytes;

                    if (remain_bytes == 0)
                        remain_bytes = stride;
                } else {  // type_bytes > remain_bytes
                    if (lastType != null){
                        if(lastType.arraySize > 1){
                            lastType.arrayPadding = remain_bytes;
                        }else{
                            lastType.padding = remain_bytes;
                        }
                    }else{
                        throw new IllegalStateException("Inner Error!!!");
                    }

                    totalBytes += remain_bytes;
                    remain_bytes = stride;

                    totalBytes += type_bytes;
                    remain_bytes -= type_bytes;

                    if (remain_bytes == 0)
                        remain_bytes = stride;
                }
            }

            lastType = type;
        }

        return Numeric.divideAndRoundUp(totalBytes, stride) * stride;
    }

    private static String formatBinary(ByteBuffer pixel, List<ClassType> types, int align_bytes){
        StringBuilder out = new StringBuilder();
        out.append('[');
        int start_pos = pixel.position();
        for(ClassType type : types){
            int count = type.arraySize;
            for(int i = 0; i < count; i++) {
                if (type.clazz == boolean.class || type.clazz == int.class) {
                    out.append(pixel.getInt()).append(',');
                } else if (type.clazz == float.class) {
                    out.append(_To(pixel.getFloat())).append(',');
                } else if (type.clazz == Vector2f.class) {
                    out.append(_To(pixel.getFloat())).append(',');
                    out.append(_To(pixel.getFloat())).append(',');
                } else if (type.clazz == Vector3f.class) {
                    out.append(_To(pixel.getFloat())).append(',');
                    out.append(_To(pixel.getFloat())).append(',');
                    out.append(_To(pixel.getFloat())).append(',');
                } else if (type.clazz == Vector4f.class || type.clazz == Matrix2f.class) {
                    out.append(_To(pixel.getFloat())).append(',');
                    out.append(_To(pixel.getFloat())).append(',');
                    out.append(_To(pixel.getFloat())).append(',');
                    out.append(_To(pixel.getFloat())).append(',');
                }else if (type.clazz == Matrix3f.class){
                    for(int j = 0; j < 9; j++){
                        out.append(_To(pixel.getFloat())).append(',');
                    }
                }else if (type.clazz == Matrix4f.class){
                    for(int j = 0; j < 16; j++){
                        out.append(_To(pixel.getFloat())).append(',');
                    }
                }

                if(type.padding != 0){
                    pixel.position(pixel.position() + type.padding);
                }
            }
            if(type.arrayPadding != 0){
                pixel.position(pixel.position() + type.arrayPadding);
            }
        }

        int end_pos = pixel.position();
        if(end_pos - start_pos > align_bytes){
            throw  new IllegalStateException(String.format("Inner Error: start_pos = %d, end_pos = %d, align_bytes = %d.\n", start_pos, end_pos, align_bytes));
        }else if(end_pos - start_pos < align_bytes){
            pixel.position(start_pos + align_bytes);
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

        private static final int[] SIZES = new int[ACCETS.length];

        private static final Comparator<Class<?>> CLASS_COMPARATOR  = /*(a, b )-> a.getName().compareTo(b.getName())*/
            new Comparator<Class<?>>() {
                @Override
                public int compare(Class<?> a, Class<?> b) {
                    return a.getName().compareTo(b.getName());
                }
            };
        static {
            Arrays.sort(ACCETS, CLASS_COMPARATOR);
            for(int i = 0; i < ACCETS.length; i++){
                Class<?> type = ACCETS[i];
                int size = 0;
                if(type == int.class || type == boolean.class || type == float.class){
                    size = 4;
                }else if(type == Vector2f.class){
                    size = Vector2f.SIZE;
                }else if (type == Vector3f.class){
                    size = Vector3f.SIZE;
                }else if (type == Vector4f.class){
                    size = Vector4f.SIZE;
                }else if (type == Matrix2f.class){
                    size = Matrix2f.SIZE;
                }else if (type == Matrix3f.class){
                    size = Matrix3f.SIZE;
                }else if(type == Matrix4f.class){
                    size = Matrix4f.SIZE;
                }

                SIZES[i] = size;
            }
        }

        static int sizeof(Class<?> type){
            int idx = Arrays.binarySearch(ACCETS, type, CLASS_COMPARATOR);
            return idx >= 0 ? SIZES[idx] : -1;
        }

        static boolean accept(Class<?> clazz){
            return Arrays.binarySearch(ACCETS, clazz, CLASS_COMPARATOR) >=0;
        }

        Class<?> clazz;
        int arraySize = 1;
        int padding;
        int arrayPadding;

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
        if(GLFuncProviderFactory.getGLFuncProvider().getHostAPI() == GLAPI.ANDROID)
            return;

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
        ByteBuffer result = TextureUtils.getTextureData(target, textureID, level, true);
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
        if(f - Math.ceil(f) == 0.0 && f < Integer.MAX_VALUE){
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
        boolean equals(String src, String dst, CompareResult result, float igoreValue);
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
            if(srcMissToken == null)
                srcMissToken = "";
            srcMissTokens.add(srcMissToken);

            if(dstMissToken == null)
                dstMissToken = "";
            dstMissTokens.add(dstMissToken);

            missTokenAtIndexs.add(index);
        }
    }

    private static String mkToken(float[] a){
        StringBuilder s = new StringBuilder(32);
        for(float v : a){
            s.append(_To(v)).append(',');
        }

        if(s.length() > 0)
            s.setLength(s.length() - 1);
        return s.toString();
    }

    private static String mkToken(HashSet<Integer> a){
        StringBuilder s = new StringBuilder(32);
        for(int v : a){
            s.append(_To(v)).append(',');
        }

        if(s.length() > 0)
            s.setLength(s.length() - 1);
        return s.toString();
    }

    public static final LineCompare PIXELS_COMPARE = new LineCompare() {
        final List<float[]> srcValues = new ArrayList<float[]>();
        final List<float[]> dstValues = new ArrayList<float[]>();

        public boolean equals(String src, String dst, CompareResult result, float igoreValue) {
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

                int length = Math.min(fsrcValues.length, fdstValues.length);
                int igoreCount = 0;
                for(int j = 0; j < length; j ++){
                    float ogl_value = fsrcValues[j];
                    float dx_value = fdstValues[j];
                    if(!Numeric.isClose(ogl_value, dx_value, 0.1f)){  // not same
                        result.add(mkToken(fsrcValues), mkToken(fdstValues), i);
                        break;
                    }else if(ogl_value == igoreValue){
                        igoreCount ++;
                    }
                }

                if(igoreCount == length){
                    result.lineTokens --;
                }
            }

            return result.isEmpty();
        }
    };

    public static final LineCompare INDICES = new LineCompare() {
        private final HashSet<Integer> srcValues = new HashSet<>();
        private final HashSet<Integer> dstValues = new HashSet<>();
        private final HashSet<Integer> tempValues = new HashSet<>();

        public boolean equals(String src, String dst, CompareResult result, float igoreValue) {
            result.clear();
            tempValues.clear();

            extractValues(src, srcValues);
            extractValues(dst, dstValues);
            result.lineTokens = Math.max(srcValues.size(), dstValues.size());
            for(Integer i : srcValues){
                if(dstValues.contains(i)){
                    tempValues.add(i);
                }
            }

            srcValues.removeAll(tempValues);
            dstValues.removeAll(tempValues);

            if(!srcValues.isEmpty() || !dstValues.isEmpty()){
                result.add(mkToken(srcValues), mkToken(dstValues), -1);
            }

            return result.isEmpty();
        }
    };

    public static final LineCompare ANGLE_COMPARE = new LineCompare() {
        final List<float[]> srcValues = new ArrayList<float[]>();
        final List<float[]> dstValues = new ArrayList<float[]>();

        private final Vector3f srcVec = new Vector3f();
        private final Vector3f dstVec = new Vector3f();

        public boolean equals(String src, String dst, CompareResult result, float igoreValue) {
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

    static void extractValues(String line, Collection<Integer> values){
        if(line == null) return;
        StringTokenizer tokenizer = new StringTokenizer(line, " \t[]");
        while (tokenizer.hasMoreElements()){
            values.add(Integer.parseInt(tokenizer.nextToken()));
        }
    }

    public static int[] loadIntegerList(String filename){
        try (BufferedReader in = new BufferedReader(new FileReader(filename))){
            StackInt ints = new StackInt(1024);
            String line;
            List<Integer> lineInts = new ArrayList<>();
            while ((line = in.readLine()) != null){
                extractValues(line, lineInts);

                for(int i = 0; i < lineInts.size(); i++){
                    ints.push(lineInts.get(i));
                }

                lineInts.clear();
            }

            return ints.toArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static float parseFloat(String token){
        if(token.contains(".#QNAN")){
            return Float.NaN;
        }else if(token.contains(".#INF")){
            return Float.POSITIVE_INFINITY;
        }

        return Float.parseFloat(token);
    }

    public static void saveStencilAsImage(String filename, String img_filename, HashMap<Integer, Integer> colorMap){
        NativeAPI nativeAPI = GLFuncProviderFactory.getGLFuncProvider().getNativeAPI();
        if(nativeAPI == null || !nativeAPI.isSupportSaveImageFile()){
            return;
        }
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

//            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            int[] stencilValues = allValues.getData();
            int[] out_pixels = new int[width * height];
            for(int i = 0; i < height; i++){
                for(int j = 0; j < width; j++){
                    int idx = j + i * width;
                    int stencil = stencilValues[idx];
                    Integer color = colorMap.get(stencil);
                    if(color == null){
                        color = 0xFF000000;
                    }

//                    image.setRGB(j, i, color.getRGB());
                    out_pixels[idx] = color;
                }
            }
            nativeAPI.saveImageFile(width, height, false, out_pixels, img_filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void fileCompareIntegerSets(String srcFile, String dstFile, String outputFile){
        try {
            BufferedReader srcIn = new BufferedReader(new FileReader(srcFile));
            BufferedReader dstIn = new BufferedReader(new FileReader(dstFile));
            BufferedWriter resultOut = new BufferedWriter(new FileWriter(outputFile));

            HashSet<Integer> srcValues = new HashSet<>(64);
            HashSet<Integer> dstValues = new HashSet<>(64);
            HashSet<Integer> tempValues = new HashSet<>(64);

            while (true){
                String srcLine = srcIn.readLine();
                String dstLine = dstIn.readLine();

                if(srcLine == null && dstLine == null){
                    break;
                }

                extractValues(srcLine, srcValues);
                extractValues(dstLine, dstValues);
            }

            int count = Math.max(srcValues.size(), dstValues.size());
            for(Integer i : srcValues){
                if(dstValues.contains(i)){
                    tempValues.add(i);
                }
            }

            srcValues.removeAll(tempValues);
            dstValues.removeAll(tempValues);

            float percent = 1.f -  Math.max(srcValues.size(), dstValues.size()) / (float)count;
            System.out.println("");

            System.out.println("tokenMissMachNumber = "+ (count - Math.min(srcValues.size(), dstValues.size())));
            System.out.println("Correct rate: "+ percent);

            if(percent != 1.f){
                String token0 = mkToken(srcValues);
                String token1 = mkToken(dstValues);

                resultOut.append(token0);
                resultOut.append('\n');
                resultOut.append(token1).append('\n');
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

    public static void fileCompare(String srcFile, String dstFile, String outputFile, float igoreValue){
        fileCompare(srcFile, dstFile, outputFile, null, igoreValue);
    }

    public static void fileCompare(String srcFile, String dstFile, String outputFile){
        fileCompare(srcFile, dstFile, outputFile, null, -1);
    }

    public static void fileCompare(String srcFile, String dstFile, String outputFile, LineCompare compare, float igoreValue){
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

                if(!compare.equals(srcLine, dstLine, lineResult, igoreValue)){
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

                if(lineResult.lineTokens < 0)
                    throw new IllegalArgumentException("Inner Error!");
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

    public static String getTextFromClipBoard(){
        return GLFuncProviderFactory.getGLFuncProvider().getNativeAPI().getTextFromClipBoard();
    }

    public static CharSequence covertTextToJavaStringFromClipBoard(String varname){
        return covertTextToJavaString(varname, getTextFromClipBoard());
    }

    public static CharSequence covertTextToJavaString(String varname, String text){
        StringBuilder sb = new StringBuilder();
        StringTokenizer token = new StringTokenizer(text, "\n");
        sb.append("final String ").append(varname).append(" = ").append('\n');
        while(token.hasMoreTokens()){
            String str = token.nextToken();
            if(isEmpty(str)) continue;

            sb.append('"');
            sb.append(str);
            sb.append("\\n\" + \n");
        }
        int idx = sb.length() - 3;
        sb.replace(idx, idx + 2, ";");
        return sb;
    }

    public static void main(String[] args) {
        System.out.println(covertTextToJavaStringFromClipBoard("cl_kenel"));
    }

    private static final boolean isEmpty(String str){
        if(str == null || str.length() ==0)
            return true;

        for(int i = 0; i < str.length(); i++){
            char c = str.charAt(i);
            if(c != ' ' && c != '\t' && c != '\n')
                return false;
        }

        return true;
    }

    public static void genStoreBytebuffer(Class<?> clazz){
        StringBuilder sb = new StringBuilder();
        Field[] fields = clazz.getDeclaredFields();
        for(Field field : fields){
            if((field.getModifiers() & Modifier.STATIC) != 0)
                // igore the static field.
                continue;

            final Class<?> type = field.getType();
            final String name = StringUtils.filterCharater(field.getName(), "[]");

            if(type.isArray()){
                Class<?> cmpClass = type.getComponentType();
                String typename = cmpClass.getSimpleName();
                final String upperCase = Character.toUpperCase(typename.charAt(0)) + typename.substring(1);
                if(cmpClass == byte.class){
                    sb.append("buf.put(").append(name).append(");\n");
                }else if(cmpClass.isPrimitive()){
                    sb.append("for(int i = 0; i < ").append(name).append(".length; i++)\n");
                    sb.append("\t").append("buf.put").append(upperCase).append('(').append(name).append("[i]);\n");
                }else if(/*hasInterFace(cmpClass, Readable.class)*/  ClassType.accept(cmpClass)){
                    sb.append("for(int i = 0; i < ").append(name).append(".length; i++)\n");
                    sb.append("\t").append(name).append("[i]").append(".store(buf);\n");
                }else{
                    // igoren the other types.
                    System.out.println("Unable to resolve the type: " + cmpClass.getName());
                }
            }else if(type.isPrimitive()){ //primitive type
                String typename = type.getSimpleName();
                final String upperCase = Character.toUpperCase(typename.charAt(0)) + typename.substring(1);
                if(type == byte.class){
//					sb.append(name).append(" = buf.get();\n");
                    sb.append("buf.put(").append(name).append(");\n");
                }else{
//					sb.append(name).append(" = buf.get").append(upperCase).append("();\n");
                    sb.append("buf.put").append(upperCase).append('(').append(name).append(");\n");
                }
            }else{
                sb.append(name).append(".store(buf);\n");
            }
        }

        System.out.println(sb);
    }

    public static void genLoadBytebuffer(Class<?> clazz){
        StringBuilder sb = new StringBuilder();
        sb.append("int old_pos = buf.position();\n");
        Field[] fields = clazz.getDeclaredFields();
        for(Field field : fields){
            if((field.getModifiers() & Modifier.STATIC) != 0)
                // igore the static field.
                continue;

            final Class<?> type = field.getType();
            final String name = StringUtils.filterCharater(field.getName(), "[]");

            if(type.isArray()){
                Class<?> cmpClass = type.getComponentType();
                String typename = cmpClass.getSimpleName();
                final String upperCase = Character.toUpperCase(typename.charAt(0)) + typename.substring(1);
                if(cmpClass == byte.class){
                    sb.append("buf.get(").append(name).append(");\n");
                }else if(cmpClass.isPrimitive()){
                    sb.append("for(int i = 0; i < ").append(name).append(".length; i++)\n");
                    sb.append("\t").append(name).append("[i] = buf.get").append(upperCase).append("();\n");
                }else { // Assume this type has implemented Writeable interface
                    sb.append("for(int i = 0; i < ").append(name).append(".length; i++)\n");
                    sb.append("\t").append(name).append("[i].load(buf);\n");
                }
            }else if(type.isPrimitive()){ //primitive type
                String typename = type.getSimpleName();
                final String upperCase = Character.toUpperCase(typename.charAt(0)) + typename.substring(1);
                if(type == byte.class){
                    sb.append(name).append(" = buf.get();\n");
                }else{
                    sb.append(name).append(" = buf.get").append(upperCase).append("();\n");
                }
            }else{
                sb.append(name).append(".load(buf);\n");
            }
        }

        sb.append("\nbuf.position(old_pos);\n");
        System.out.println(sb);
    }

    public static void genStructCopy(Class<?> clazz){
//        String template = FileUtils.loadTextFromClassPath(CodeGen.class, "gen_template.txt").toString();
        String template = "public %s() {}\n" +
                "\t\n" +
                "\tpublic %s(%s o) {\n" +
                "\t\tset(o);\n" +
                "\t}\n" +
                "\t\n" +
                "\tpublic void set(%s o){\n" +
                "\t\t##\n" +
                "\t}";

        StringBuilder out = new StringBuilder(512);
        List<String> lines = new ArrayList<>();

        parseClass(clazz, lines);

        boolean first = true;
        for(String line : lines){
            if(!first){
                out.append("\t\t");
            }else{
                first = false;
            }

            out.append(line).append('\n');
        }

        String className = clazz.getSimpleName();
        template = String.format(template, className, className, className, className);
        System.out.println(template.replace("##", out));
    }

    public static void genMKVec(Class<?> clazz){
        Field[] fields = clazz.getDeclaredFields();
        String vec_pattern = "MK_VEC(%s)";
        String mat_pattern = "MK_MAT(%s)";
        Object obj = null;
//		String template =

        StringBuilder out = new StringBuilder();
        try {
            obj = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
//			e.printStackTrace();
        }

        for(Field f: fields){
            f.setAccessible(true);
            Class<?> type = f.getType();
            if(type.isArray()){
                if(obj == null)
                    continue;

                Class<?> cmpType = type.getComponentType();
                int length = -1;
                try {
                    length = Array.getLength(f.get(obj));
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }


            }else{
                if(type == Matrix4f.class){
                    System.out.println(String.format(mat_pattern, f.getName()));
                }else{
                    System.out.println(String.format(vec_pattern, f.getName()));
                }
            }


        }
    }

    public static void loadTextDataTo(String filename, Object obj){
        Properties properties = new Properties();

        try(FileReader reader = new FileReader(filename)){
            properties.load(reader);

            _loadTextDataTo(properties, obj, "");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }

    private static void _loadTextDataTo(Properties properties, Object obj, String prefix) throws IllegalArgumentException, IllegalAccessException{
        List<Field> fields = getFields(obj.getClass());

        for(Field field : fields){
            if(Modifier.isStatic(field.getModifiers()))
                continue;

            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            String	 fieldName = field.getName();
            if(fieldType.isArray() && !fieldType.isPrimitive()){
                Object   fieldValue = field.get(obj);
                int length = Array.getLength(fieldValue);
                for(int i = 0; i < length; i++){
                    String _prefix = fieldName + '[' + i + ']';
                    _loadTextDataTo(properties, fieldValue, _prefix);
                }
            }else{
                parsePropertyAndSetValue(obj, field, properties,prefix);
            }
        }
    }

    private static void parsePropertyAndSetValue(Object obj, Field field, Properties properties, String prefix) throws IllegalArgumentException, IllegalAccessException{
        Class<?> fieldType = field.getType();
        String	 fieldName = field.getName();
        int      valueCount = 1;

        if(prefix.length() > 0){
            fieldName = prefix +'.'+ fieldName;
        }

        if(fieldType.isArray()){ // primitve array.
            valueCount = Array.getLength(field.get(obj));
            Object[] values = parsePropertyValues(properties.getProperty(fieldName), fieldType, valueCount);
            if(fieldType == int.class){
                int[] numberValues = new int[valueCount];
                for(int i = 0; i <valueCount;i++ ) numberValues[i] = (Integer)values[i];
                field.set(obj, numberValues);
            }else if(fieldType == float.class){
                float[] numberValues = new float[valueCount];
                for(int i = 0; i <valueCount;i++ ) numberValues[i] = (Float)values[i];
                field.set(obj, numberValues);
            }else if(fieldType == boolean.class){
                boolean[] numberValues = new boolean[valueCount];
                for(int i = 0; i <valueCount;i++ ) numberValues[i] = (Boolean)values[i];
                field.set(obj, numberValues);
            }else{
                throw new RuntimeException("Inner Error: Unhandle the type of " + fieldType.getComponentType());
            }

        }else{
            if(fieldType.isPrimitive()){
                String strValue = properties.getProperty(fieldName);
                if(strValue == null)
                    throw new NullPointerException("Can't find field: " + fieldName);
                Object[] values = parsePropertyValues(strValue, fieldType, valueCount);
                field.set(obj, values[0]);
            }else{  // not the primitve type. E.g Vector2f,
                if(fieldType == Matrix4f.class){
                    float[] rows0 = parsePropertyValuesAsFloat(getProperty(properties, fieldName+"[0]"), fieldType, 4);
                    float[] rows1 = parsePropertyValuesAsFloat(getProperty(properties, fieldName+"[1]"), fieldType, 4);
                    float[] rows2 = parsePropertyValuesAsFloat(getProperty(properties, fieldName+"[2]"), fieldType, 4);
                    float[] rows3 = parsePropertyValuesAsFloat(getProperty(properties, fieldName+"[3]"), fieldType, 4);

                    Matrix4f matValue = (Matrix4f)field.get(obj);
                    matValue.setRow(0, rows0[0], rows0[1], rows0[2], rows0[3]);
                    matValue.setRow(1, rows1[0], rows1[1], rows1[2], rows1[3]);
                    matValue.setRow(2, rows2[0], rows2[1], rows2[2], rows2[3]);
                    matValue.setRow(3, rows3[0], rows3[1], rows3[2], rows3[3]);
                }else if(hasInterFace(fieldType, WritableVector.class)){
                    String strValue = properties.getProperty(fieldName);
                    if(strValue == null){
                        throw new NullPointerException("Can't find field: " + fieldName);
                    }

                    float[] values = parsePropertyValuesAsFloat(strValue, fieldType, 4);
                    WritableVector vector = (WritableVector)field.get(obj);

                    for(int i = 0; i < vector.getCount(); i++){
                        vector.setValue(i, values[i]);
                    }
                }else{  // other data type.
                    _loadTextDataTo(properties, field.get(obj), prefix);
                }

            }
        }
    }

    private static String getProperty(Properties props, String key){
        String strValue = props.getProperty(key);
        if(strValue == null){
            throw new NullPointerException("Can't find field: " + key);
        }

        return strValue;
    }

    private enum PropertyType{
        INT, FLOAT, BOOLEAN
    }

    private static float[] parsePropertyValuesAsFloat(String strValue, Class<?> type, int count){
        Object[] values = parsePropertyValues(strValue, type, count);
        float[] floatValues = new float[count];
        for(int i = 0; i < count; i++){
            if(values[i] != null)
                floatValues[i] = (Float)values[i];
        }

        return floatValues;
    }

    private static Object[] parsePropertyValues(String strValue, Class<?> type, int count){
        Object[] returnedValues = new Object[count];
        StringTokenizer tokens =new StringTokenizer(strValue, "(), \n");
        count = 0;

        // true is number, false is boolean value.
        PropertyType propertyType = PropertyType.FLOAT;
        if(type == int.class ){
            propertyType = PropertyType.INT;
        }else if(type == boolean.class){
            propertyType = PropertyType.BOOLEAN;
        }

        while(tokens.hasMoreTokens()){
            if(propertyType == PropertyType.INT){
                returnedValues[count++] = Integer.parseInt(tokens.nextToken());
            }else if(propertyType == PropertyType.FLOAT){
                returnedValues[count++] = Float.parseFloat(tokens.nextToken());
            }else if(propertyType == PropertyType.BOOLEAN){
                String token = tokens.nextToken();
                if(token.equalsIgnoreCase("true")){
                    returnedValues[count++] = Boolean.TRUE;
                }else if(token.equalsIgnoreCase("false")){
                    returnedValues[count++] = Boolean.FALSE;
                }else { // is a number
                    returnedValues[count++] = Float.parseFloat(token) != 0;
                }
            }
        }

        return returnedValues;
    }

    public static void genSafeDelete(Class<?> clazz){
        Field[] fields = clazz.getDeclaredFields();

        StringBuilder out = new StringBuilder(512);
        for(int i = 0; i < fields.length; i++){
            Field field = fields[i];

            parseFiled(field, out);
        }

        System.out.println(out);
    }

    public static void genToString(Class<?> clazz){
        Field[] fields = clazz.getDeclaredFields();

        StringBuilder out = new StringBuilder(512);
        out.append("StringBuilder sb = new StringBuilder();\n");
        out.append("sb.append(\"").append(clazz.getSimpleName()).append(":\\n\");\n");

        String pattern = "sb.append(\"%s = \").append(%s).append('\\n');\n";
        String pattern_array = "sb.append(\"%s = \").append(Arrays.toString(%s)).append('\\n');\n";
        for(int i = 0; i < fields.length; i++){
            Field field = fields[i];
            if(Modifier.isStatic(field.getModifiers()))
                continue;

            out.append(String.format(field.getType().isArray()? pattern_array: pattern, field.getName(), field.getName()));
        }

        out.append("return sb.toString();\n");
        System.out.println(out);
    }

    public enum GLObject {
        Texture,
        Buffer,
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface GLType {
        public GLObject type();
    }

    private static void parseFiled(Field field, StringBuilder output){
        final String int_array_pattern =
                "for(int i = 0; i < %s.length; i++){\n"+
                        "\tif(%s[i] != 0){\n"+
                        "\t\tgl.glDelete%s(%s[i]);\n"+
                        "\t%s[i] = 0;\n"+
                        "\t}\n"+
                        "}\n";

        final String int_pattern =
                "if(%s != 0){\n"+
                        "\tGL15.glDelete%ss(%s);\n"+
                        "\t%s = 0;\n"+
                        "}\n";

        final String obj_pattern =
                "if(%s != null){\n"+
                        "\t%s.dispose();\n"+
                        "\t%s = null;\n"+
                        "}\n";

        final String obj_array_pattern =
                "for(int i = 0; i < %s.length; i++){\n"+
                        "\tif(%s[i] != null){\n"+
                        "\t\t%s[i].dispose();\n"+
                        "\t\t%s[i] = null;\n"+
                        "\t}\n" +
                        "}\n";

        String varname = field.getName();
        Class<?> fieldType = field.getType();
        GLType type = field.getDeclaredAnnotation(GLType.class);
        if(type != null){
            if(fieldType.isArray()){
                Class<?> componentType = fieldType.getComponentType();
                if(componentType == int.class){
                    String result = String.format(int_array_pattern, varname,varname,type.type().name(), varname, varname);
                    output.append(result).append('\n');
                }else {
                    Class<?> interfaces[] = componentType.getInterfaces();
                    if(hasInterFace(interfaces, Disposeable.class)){
                        String result = String.format(obj_array_pattern, varname,varname,varname, varname);
                        output.append(result).append('\n');
                    }else{
                        System.err.println("UnkownType: " + varname + ", "+componentType.getName());
                    }
                }
            }else{
                if(fieldType == int.class){
                    String result = String.format(int_pattern, varname,type.type().name(), varname, varname);
                    output.append(result).append('\n');
                }else {
                    if(hasInterFace(fieldType, Disposeable.class)){
                        String result = String.format(obj_pattern, varname,varname,varname);
                        output.append(result).append('\n');
                    }else{
                        System.err.println("UnkownType: " + varname + ", "+fieldType.getName());
                    }
                }
            }
        }else{ // Type is null
            if(fieldType.isArray()){
                Class<?> componentType = fieldType.getComponentType();
//				Class<?> interfaces[] = componentType.getInterfaces();
                if(hasInterFace(componentType, Disposeable.class)){
                    String result = String.format(obj_array_pattern, varname,varname,varname, varname);
                    output.append(result).append('\n');
                }else{
//					System.err.println("UnkownType: " + varname + ", "+componentType.getName());
                }
            }else{
                if(hasInterFace(fieldType, Disposeable.class)){
                    String result = String.format(obj_pattern, varname,varname,varname);
                    output.append(result).append('\n');
                }else{
//					System.err.println("UnkownType: " + varname + ", "+fieldType.getName());
                }
            }
        }

    }

    public static boolean hasInterFace(Class<?>[] types, Class<?> _interface){
        for(Class<?> type : types){
            if(hasInterFace(type, _interface))
                return true;
        }

        return false;
    }

    public static boolean hasInterFace(Class<?> type, Class<?> _interface){
        Class<?> parent = type;
        while(parent != null){
            Class<?>[] interfaces = parent.getInterfaces();
            if(CommonUtil.contain(interfaces, _interface)){
                return true;
            }

            for(Class<?> i : interfaces){
                if(hasInterFace(i, _interface))
                    return true;
            }

            parent = parent.getSuperclass();
        }

        return false;
    }

    private static void parseClass(Class<?> clazz, List<String> lines){
        String[] array = {
                "java.lang.Integer", "java.lang.Byte", "java.lang.Boolean",
                "java.lang.Character", "java.lang.Float", "java.lang.Double",
                "java.lang.Short", "java.lang.String","java.lang.Long"
        };

        Arrays.sort(array);
        Field[] fields = clazz.getDeclaredFields();
        for(Field field : fields){
            if((field.getModifiers() & Modifier.STATIC) != 0)
                // igore the static field.
                continue;

            final Class<?> type = field.getType();
            final String name = StringUtils.filterCharater(field.getName(), "[]");
            final boolean isFinal = (field.getModifiers() & Modifier.FINAL) != 0;
            if(type.isPrimitive() || type == String.class || (type.isInterface() && type != List.class) || type.isEnum()){
                lines.add(String.format("%s = o.%s;", name, name));
            }else{
                if(type.isArray()){ // The field is a array.
                    Class<?> compType = type.getComponentType();
                    if(compType.isPrimitive() || compType == String.class || compType.isInterface()){
                        if(!isFinal){
                            lines.add(String.format("if(o.%s != null){", name));
                            lines.add(String.format("\t%s = new %s[o.%s.length];", name, compType.getSimpleName(), name));
                            lines.add(String.format("\t%s = Arrays.copyOf(o.%s, o.%s.length);", name, name, name));
                            lines.add("}");
                        }else{
                            lines.add(String.format("System.arraycopy(o.%s, 0, %s, 0, o.%s.length);", name, name, name));
                        }
                    }else if(compType.isArray()){  // The field ia two dimension array.
                        if(!isFinal){
                            lines.add(String.format("if(o.%s != null){", name));
                            lines.add(String.format("\t%s = new %s[o.%s.length][];", name, compType.getSimpleName(), name));
                            lines.add(String.format("\tfor(int i = 0; i < o.%s.length; i++){", name));
                            lines.add(String.format("\t\tif(o.%s[i] != null)", name));
                            lines.add(String.format("\t\t\t%s[i] = Arrays.copyOf(o.%s[i], o.%s[i].length);", name, name, name));
                            lines.add("\t}");
                            lines.add("}");
                        }else{
                            lines.add(String.format("for(int i = 0; i < o.%s.length; i++)", name));
                            lines.add(String.format("\tSystem.arraycopy(o.%s[i], 0, %s[i], 0, o.%s[i].length);", name,name,name));
                        }
                    }else{
                        // object array.
                        if(!isFinal){
                            lines.add(String.format("if(o.%s != null){", name));
                            lines.add(String.format("\t%s = Arrays.copyOf(o.%s, o.%s.length);", name, name, name));
                        }else{
                            String simpleName = compType.getSimpleName();
//							lines.add(String.format("System.arraycopy(o.%s, 0, %s, 0, o.%s.length);", name, name, name));
                            lines.add(String.format("for(int i = 0; i < o.%s.length; i++)", name));
                            lines.add(String.format("\t%s[i] = new %s(o.%s[i]);", name, simpleName, name));
                        }
                    }
                }else{
                    boolean isList = false;
                    if(type == List.class || type == ArrayList.class || type == LinkedList.class){
                        isList = true;
                    }

                    if(!isList){
                        Class<?>[] interfaces = type.getInterfaces();
                        for(Class<?> inter : interfaces){
                            if(inter == Collection.class){
                                isList = true;
                                break;
                            }
                        }
                    }

                    if(isList){
                        String genricType = null;
                        String simpleName = null;
                        String typename = field.getGenericType().getTypeName();
                        int i1 = typename.indexOf('<');
                        if(i1 > 0){
                            int i2 = typename.lastIndexOf('>');
                            genricType = typename.substring(i1 + 1, i2);
                            int last  =genricType.lastIndexOf('.');
                            simpleName = genricType.substring(last + 1);
                        }
                        lines.add(String.format("%s.clear();", name));
                        if(genricType == null || Arrays.binarySearch(array, genricType) >=0){
                            lines.add(String.format("%s.addAll(o.%s);", name, name));
                        }else{
                            lines.add(String.format("for(int i = 0; i < o.%s.size(); i++)", name));
                            lines.add(String.format("\t%s.add(new %s(o.%s.get(i)));", name, simpleName, name));
                        }
                    }else{
                        if(isFinal){
                            lines.add(String.format("%s.set(o.%s);", name, name));
                        }else{
                            lines.add(String.format("%s = o.%s;", name, name));
                        }
                    }
                }
            }
        }
    }

    public static Field getField(Object obj, String filedName) {
        Class<? extends Object> objectType = obj.getClass();
        return getField(objectType, filedName);
    }

    /** Get all of the fields that declared in the class. */
    public static List<Field> getFields(Class<?> objectType){
        List<Field> fields = new ArrayList<Field>();

        while (objectType != null) {
            for (Field f : objectType.getDeclaredFields())
                fields.add(f);

            objectType = objectType.getSuperclass();
        }

        return fields;
    }

    public static Field getField(Class<?> objectType, String filedName) {

        boolean found = false;

        while (objectType != null) {
            for (Field f : objectType.getDeclaredFields())
                if (f.getName().equals(filedName)) {
                    found = true;
                    break;
                }

            if (found)
                break;
            else
                objectType = objectType.getSuperclass();
        }

        if (found) {
            try {
                Field field = objectType.getDeclaredField(filedName);

                try {
                    field.setAccessible(true);
                    return field;
                } catch (java.security.AccessControlException e) {
                    e.printStackTrace();
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("No such field named '" + filedName
                    + "' in the class " + objectType.getName());
        }
        return null;
    }
}
