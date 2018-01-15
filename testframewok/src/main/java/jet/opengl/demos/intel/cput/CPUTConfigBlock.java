package jet.opengl.demos.intel.cput;

/**
 * Created by mazhen'gui on 2017/11/13.
 */

public class CPUTConfigBlock {
    final CPUTConfigEntry[] mpValues = new CPUTConfigEntry[64];
    CPUTConfigEntry mName;
    String          mszName;
    int             mnValueCount;

    public CPUTConfigEntry AddValue(String szName, String szValue){
        // TODO: What should we do if it already exists?
        /*CPUTConfigEntry pEntry = mpValues[mnValueCount++];
        pEntry.szName  = szName.toLowerCase();
        pEntry.szValue = szValueLower;
        return pEntry;*/

        CPUTConfigEntry entry = new CPUTConfigEntry(szName.toLowerCase(), szValue.toLowerCase());
        mpValues[mnValueCount++] = entry;
        return entry;
    }

    public CPUTConfigEntry GetValue(int nValueIndex){
        if(nValueIndex < 0 || nValueIndex >= mnValueCount)
        {
            return null;
        }
        return mpValues[nValueIndex];
    }

    public CPUTConfigEntry GetValueByName(String szName){
        szName = szName.toLowerCase();
        for(int i = 0; i < mnValueCount; i++){
            if(mpValues[i].szName.equals(szName)){
                return mpValues[i];
            }
        }

        return CPUTConfigEntry.sNullConfigValue;
    }

    public String GetName() { return mszName;}
    public int GetNameValue() {return mName.ValueAsInt();}
    public int ValueCount(){ return mnValueCount;}
    public boolean IsValid() { return mnValueCount > 0; }
}
