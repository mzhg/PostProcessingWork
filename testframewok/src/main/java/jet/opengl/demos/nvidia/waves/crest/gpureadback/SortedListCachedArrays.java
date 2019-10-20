package jet.opengl.demos.nvidia.waves.crest.gpureadback;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jet.opengl.postprocessing.util.Numeric;

/// <summary>
/// I reallllly wanted to use a sorted list, but was getting garbage when doing foreach loop, so this
/// really dumb wrapper caches arrays for keys and values and refreshes them when Adding and Removing.
/// This is only barely a good idea when keys are not added and removed every frame, and the less they
/// are added/removed the better.
/// </summary>
final class SortedListCachedArrays extends LinkedHashMap<Float,GPUReadbackBase.PerLodData> {

    public float[] KeyArray = Numeric.EMPTY_FLOAT;
    public GPUReadbackBase.PerLodData[] ValueArray = new GPUReadbackBase.PerLodData[0];

    public void Add(Float key, GPUReadbackBase.PerLodData value)
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
        if (size() != ValueArray.length) ValueArray = new GPUReadbackBase.PerLodData[size()];

        int index = 0;
        Set<Map.Entry<Float, GPUReadbackBase.PerLodData>> values = entrySet();
        for(Map.Entry<Float, GPUReadbackBase.PerLodData> v : values){
            KeyArray[index] = v.getKey();
            ValueArray[index] = v.getValue();
            index++;
        }

//        Keys.CopyTo(KeyArray, 0);
//        Values.CopyTo(ValueArray, 0);
    }
}
