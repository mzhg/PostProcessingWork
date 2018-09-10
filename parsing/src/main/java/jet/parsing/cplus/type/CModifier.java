package jet.parsing.cplus.type;

public final class CModifier {
    public static final int PUBLIC = 1;
    public static final int PRIVATE = 2;
    public static final int PROTECTED = 4;
    public static final int STATIC = 8;
    public static final int CONST = 16;

    public static final int EXTERN = 32;
    public static final int INLINE = 64;
    public static final int MUTABLE = 128;
    public static final int OPERATOR = 256;
    public static final int REGISTER = 512;
    public static final int VIRTUAL = 1024;
    public static final int VOLATILE = 2048;

    public static boolean isPublic(int var){
        return (var & PUBLIC) != 0;
    }

    public static boolean isPrivate(int var){
        return (var & PRIVATE) != 0;
    }

    public static boolean isProtected(int var){
        return (var & PROTECTED) != 0;
    }

    public static boolean isStatic(int var){
        return (var & STATIC) != 0;
    }
}
