package jet.parsing.cplus.type;

/**
 * This class represents the type of c++ language.
 */
public class CBaseType implements CType{
    /** The Type name. e.g "int", "float" */
    protected final String mName;

    /** The size of memory that token. */
    protected final int mSize;

    public CBaseType(String name, int size) {
        this.mName = name;
        this.mSize = size;
    }

    public String getName() {
        return mName;
    }

    public int getSize() {
        return mSize;
    }

    public boolean isPrimitive() {
        return true;
    }
}
