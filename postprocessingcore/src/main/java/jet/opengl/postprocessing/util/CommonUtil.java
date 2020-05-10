package jet.opengl.postprocessing.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.Disposeable;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public final class CommonUtil {

    private CommonUtil(){}

    public static boolean equals(Object a, Object b){
        if(a == null && b == null) {
            return true;
        }else if(a == null || b== null){
            return false;
        }else{
            return  a== b || a.equals(b);
        }
    }

    public static boolean equals(Object[] a, Object[] b){
        if(a == null && b == null){
            return true;
        }else if(a == null || b == null){
            return false;
        }else{
            int lenA = a.length;
            int lenB = b.length;
            if(lenA != lenB){
                return false;
            }

            if(a == b)
                return true;

            for(int i = 0; i < lenA; i++){
                if(!equals(a[i], b[i])){
                    return false;
                }
            }

            return true;
        }
    }

    public static<T> T[] toArray(T...args){
        return args;
    }

    public static int[] toInts(int...args){
        return args;
    }

    public static void safeRelease(Disposeable res){
        if(res != null){
            res.dispose();
        }
    }

    public static final boolean contain(Object[] a, Object o){
        for(int i = 0; i < a.length; i++){
            if(equals(a[i], o))
                return true;
        }

        return false;
    }

    public static long equal_range(List<?> values, Object var){
        int start = -1;
        int end = -1;

        for(int i = 0; i < values.size(); i++){
            Object obj = values.get(i);
            boolean isEqauled = equals(obj, var);
            if(start == -1){
                if(isEqauled)
                    start = i;
            }

            if(end == -1 || (i - end) == 1){
                if(isEqauled)
                    end = i;
            }else{
                break;
            }
        }

        return Numeric.encode(start, start >=0? (end + 1) : end);
    }

    public static String toString(int[][] a){
        if(a == null || a.length == 0)
            return "[]";

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < a.length; i++){
            for(int j = 0; a[i] != null && j < a[i].length; j++){
                sb.append(a[i][j]);
                sb.append(',');
            }
        }

        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public static int length(Object[] a){
        return a != null ? a.length : 0;
    }

    @SuppressWarnings("unchecked")
    public static<T> T[] initArray(T[] arr){
        if(arr == null)
            return null;

        Class<?> clazz = arr.getClass().getComponentType();
        try {
            for(int i = 0; i < arr.length; i++)
                if(arr[i] == null)
                    arr[i] = (T) clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return arr;
    }

    // https://blog.csdn.net/xuhailiang0816/article/details/78403041
    public static Class<?> getActualTypeArgument(Class<?> clazz) {
        return getActualTypeArgument(clazz, 0);
    }

    public static Class<?> getActualTypeArgument(Class<?> clazz, int index) {
        Class<?> entitiClass = null;
        Type genericSuperclass = clazz.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            Type[] actualTypeArguments = ((ParameterizedType) genericSuperclass)
                    .getActualTypeArguments();
            if (actualTypeArguments != null && actualTypeArguments.length > 0) {
                entitiClass = (Class<?>) actualTypeArguments[index];
            }
        }

        return entitiClass;
    }

    public static Class<?>[] getActualTypeArguments(Class<?> clazz) {
        Class<?>[] entitiClass = null;
        Type genericSuperclass = clazz.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            Type[] actualTypeArguments = ((ParameterizedType) genericSuperclass)
                    .getActualTypeArguments();
            if (actualTypeArguments != null && actualTypeArguments.length > 0) {
                entitiClass = new Class[actualTypeArguments.length];
                for(int i = 0; i < entitiClass.length; i++)
                    entitiClass[i] = (Class<?>) actualTypeArguments[i];
            }
        }

        return entitiClass;
    }

    public static final int SIZE_OF_PUBLIC = 1 << 0;
    public static final int SIZE_OF_DEFAULT = 1 << 1;
    public static final int SIZE_OF_PROTECTED = 1 << 2;
    public static final int SIZE_OF_PRIVATE = 1 << 3;
    public static final int SIZE_OF_SUPER = 1 << 4;
    public static final int SIZE_OF_ALL = SIZE_OF_PUBLIC | SIZE_OF_DEFAULT|SIZE_OF_PROTECTED | SIZE_OF_PRIVATE | SIZE_OF_SUPER;


    public static int sizeof(Object obj){
        return sizeof(obj, SIZE_OF_ALL);
    }

    public static int sizeof(Object obj, int flags){
        try {
            return sizeofImpl(obj, flags);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return -1;
    }

    private static int sizeofImpl(Object obj, int flags) throws IllegalArgumentException, IllegalAccessException{
        Class<?> clazz = obj.getClass();
        List<Field> parsedFields = new ArrayList<Field>();
        collectionFields(clazz, parsedFields, flags);

        int totalBytes = 0;
        for(Field f : parsedFields){
            totalBytes += parseFieldBytes(obj, f, flags);
        }

        return totalBytes;
    }

    private static void collectionFields(Class<?> clazz, List<Field> parsedFields, int flags){
        Field[] declaredFields = clazz.getDeclaredFields();
        for(Field f : declaredFields){
            int modifiers = f.getModifiers() & (~Modifier.FINAL);
            if(Modifier.isStatic(modifiers)){
                continue;
            }

            if((flags & SIZE_OF_PUBLIC) != 0 &&  Modifier.isPublic(modifiers)){
                parsedFields.add(f);
            }else if((flags & SIZE_OF_DEFAULT) != 0 && modifiers == 0){
                parsedFields.add(f);
            }else if((flags & SIZE_OF_PROTECTED) != 0 && Modifier.isProtected(modifiers)){
                parsedFields.add(f);
            }else if((flags & SIZE_OF_PRIVATE) != 0 && Modifier.isPrivate(modifiers)){
                parsedFields.add(f);
            }
        }

        if((flags & SIZE_OF_SUPER) != 0){
            Class<?> superClazz = clazz.getSuperclass();
            if(superClazz != null && superClazz != Object.class){
                collectionFields(superClazz, parsedFields, flags);
            }
        }
    }

    public static int sizeof(Class<?> clazz){
        return sizeof(clazz, SIZE_OF_ALL);
    }

    public static int sizeof(Class<?> clazz, int flags){
        try {
            return sizeofImpl(clazz, flags);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static int sizeofImpl(Class<?> clazz, int flags) throws IllegalArgumentException, IllegalAccessException{
        List<Field> parsedFields = new ArrayList<Field>();
        collectionFields(clazz, parsedFields, flags);

        int totalBytes = 0;
        for(Field f : parsedFields){
            int bytes = parseFieldBytes(f, flags);
            totalBytes += bytes;
//			System.out.println(f.getName() + ": " + bytes);
        }

        return totalBytes;
    }

    private static int parseFieldBytes(Field field, int flags) throws IllegalArgumentException, IllegalAccessException{
        Class<?> type = field.getType();
        if(type.isArray()){
            // We don't known the array length.
            return 0;
        }else{
            if(type.isPrimitive()){
                return sizeofPrimitive(type);
            }else{
                return sizeofImpl(type, flags);
            }
        }
    }

    private static int parseFieldBytes(Object obj,Field field, int flags) throws IllegalArgumentException, IllegalAccessException{
        Class<?> type = field.getType();
        field.setAccessible(true);
        if(type.isArray()){
            Object arrayValue = field.get(obj);
            if(arrayValue == null){
                return 0;   // null object take zero bytes.
            }else{
                int length = Array.getLength(arrayValue);
                if(length == 0){
                    return 0;  // also zero bytes.
                }else{
                    int sizeInBytes = 0;
                    for(int i = 0; i < length; i++){
                        Object value = Array.get(arrayValue, i);
                        if(value != null){
                            sizeInBytes += sizeofImpl(value, flags);
                        }
                    }

                    return sizeInBytes;
                }
            }

        }else{
            if(type.isPrimitive()){
                return sizeofPrimitive(type);
            }else{
                Object value = field.get(obj);
                return value != null ? sizeofImpl(value, flags) : 4;
            }
        }
    }

    public static int sizeofPrimitive(Class<?> clazz){
        if(clazz == int.class || clazz == float.class){
            return 4;
        }else if(clazz == long.class || clazz == double.class){
            return 8;
        }else if(clazz == short.class || clazz == char.class){
            return 2;
        }else if(clazz == byte.class || clazz == boolean.class){
            return 1;
        }else
            throw new IllegalArgumentException(clazz.getName() + " is not primitive type");
    }
}
