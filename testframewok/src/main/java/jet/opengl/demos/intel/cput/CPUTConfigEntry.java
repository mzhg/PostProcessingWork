package jet.opengl.demos.intel.cput;

/**
 * Created by mazhen'gui on 2017/11/13.
 */

public class CPUTConfigEntry {
    final String szName;
    final String szValue;

    public CPUTConfigEntry() { this(null, null);}
    public CPUTConfigEntry(String name, String value){
        szName = name;
        szValue = value;
    }

    public static final CPUTConfigEntry  sNullConfigValue = new CPUTConfigEntry();

    public String NameAsString(){ return  szName;};
    public String ValueAsString(){ return szValue; }
    public boolean IsValid(){ return szName != null && !szName.isEmpty(); }
    public float ValueAsFloat() throws NumberFormatException
    {
        /*float fValue=0;
        int retVal;
        retVal=swscanf_s(szValue.c_str(), _L("%g"), &fValue ); // float (regular float, or E exponentially notated float)
        ASSERT(0!=retVal, _L("ValueAsFloat - value specified is not a float"));
        return fValue;*/

        return Float.parseFloat(szValue);
    }
    public int ValueAsInt() throws NumberFormatException
    {
        /*int nValue=0;
        int retVal;
        retVal=swscanf_s(szValue.c_str(), _L("%d"), &nValue ); // signed int (NON-hex)
        ASSERT(0!=retVal, _L("ValueAsInt - value specified is not a signed int"));
        return nValue;*/

        return Integer.parseInt(szValue);
    }
    public boolean ValueAsBool() throws NumberFormatException
    {
        if(szValue == null) return false;
        return  (szValue.compareTo("true") == 0) ||
                (szValue.compareTo("1") == 0) ||
                (szValue.compareTo("t") == 0);
    }

    public void ValueAsFloatArray(float[] pFloats, int offset, int count)throws NumberFormatException{
        String[] tokens = szValue.split(" ");
        for(int i = 0; i < count; i++){
            pFloats[offset + i] = Float.parseFloat(tokens[i]);
        }
    }
}
