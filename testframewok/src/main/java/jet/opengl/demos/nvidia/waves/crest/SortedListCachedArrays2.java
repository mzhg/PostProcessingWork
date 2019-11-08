package jet.opengl.demos.nvidia.waves.crest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jet.opengl.postprocessing.util.Numeric;

/**
 * I reallllly wanted to use a sorted list, but was getting garbage when doing foreach loop, so this
 * really dumb wrapper caches arrays for keys and values and refreshes them when Adding and Removing.
 * This is only barely a good idea when keys are not added and removed every frame, and the less they
 * are added/removed the better.
 */
final class SortedListCachedArrays2 extends LinkedHashMap<Float,Wave_GPUReadbackBase.PerLodData> {

    float[] KeyArray = Numeric.EMPTY_FLOAT;
    Wave_GPUReadbackBase.PerLodData[] ValueArray = new Wave_GPUReadbackBase.PerLodData[0];

    public void Add(Float key, Wave_GPUReadbackBase.PerLodData value)
    {
        super.put(key, value);

        RefreshArrays();
    }

    public void Remove(Float key)
    {
        super.remove(key);

        RefreshArrays();
    }

    void RefreshArrays()
    {
        if (size() != KeyArray.length) KeyArray = new float[size()];
        if (size() != ValueArray.length) ValueArray = new Wave_GPUReadbackBase.PerLodData[size()];

        int index = 0;
        Set<Map.Entry<Float, Wave_GPUReadbackBase.PerLodData>> values = entrySet();
        for(Map.Entry<Float, Wave_GPUReadbackBase.PerLodData> v : values){
            KeyArray[index] = v.getKey();
            ValueArray[index] = v.getValue();
            index++;
        }

//        Keys.CopyTo(KeyArray, 0);
//        Values.CopyTo(ValueArray, 0);
    }
}
