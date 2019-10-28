package jet.opengl.demos.nvidia.waves.crest.loddata;

import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.postprocessing.util.Numeric;

/** Filter object for assigning shapes to LODs. This was much more elegant with a lambda but it generated garbage. */
public class FilterWavelength implements LodDataMgr.IDrawFilter{
    public float _lodMinWavelength;
    public float _lodMaxWavelength;
    public int _lodIdx;
    public int _lodCount;
    public float _globalMaxWavelength;

    public long Filter(ILodDataInput data/*, out int isTransition*/)
    {
        float drawOctaveWavelength = data.Wavelength();
        int isTransition = 0;

        // No wavelength preference
        if (drawOctaveWavelength == 0f)
        {
//            return 1f;
            return Numeric.encode(Float.floatToIntBits(1.f), 0);
        }

        // Too small for this lod
        if (drawOctaveWavelength < _lodMinWavelength)
        {
            return 0;
        }

        // If approaching end of lod chain, start smoothly transitioning any large wavelengths across last two lods
        if (drawOctaveWavelength >= _globalMaxWavelength / 2f)
        {
            if (_lodIdx == _lodCount - 2)
            {
                isTransition = 1;
//                return 1f - OceanRenderer.Instance.ViewerAltitudeLevelAlpha();
                return Numeric.encode(Float.floatToIntBits(1f - OceanRenderer.Instance.ViewerAltitudeLevelAlpha), isTransition);
            }

            if (_lodIdx == _lodCount - 1)
            {
//                return OceanRenderer.Instance.ViewerAltitudeLevelAlpha();
                return Numeric.encode(Float.floatToIntBits(OceanRenderer.Instance.ViewerAltitudeLevelAlpha), isTransition);
            }
        }
        else if (drawOctaveWavelength < _lodMaxWavelength)
        {
            // Fits in this lod
//            return 1f;
            return Numeric.encode(Float.floatToIntBits(1.f), 0);
        }

        return 0;
    }
}
