package jet.opengl.demos.nvidia.waves.crest;

final class Wave_Length_Filter implements Wave_DrawFilter{
    public float _lodMinWavelength;
    public float _lodMaxWavelength;
    public int _lodIdx;
    public int _lodCount;
    public float _globalMaxWavelength;

    @Override
    public void Filter(Wave_LodData_Input data,Wave_CDClipmap clipmap, Wave_FilterData result) {
        float drawOctaveWavelength = data.wavelength();
        result.isTransition = false;

        // No wavelength preference
        if (drawOctaveWavelength == 0f)
        {
//            return 1f;
            result.weight = 1f;
            return;
        }

        // Too small for this lod
        if (drawOctaveWavelength < _lodMinWavelength)
        {
//            return 0f;
            result.weight = 0f;
            return;
        }

        // If approaching end of lod chain, start smoothly transitioning any large wavelengths across last two lods
        if (drawOctaveWavelength >= _globalMaxWavelength / 2f)
        {
            if (_lodIdx == _lodCount - 2)
            {
                result.isTransition = true;
//                return 1f - OceanRenderer.Instance.ViewerAltitudeLevelAlpha;
                result.weight = 1f - clipmap.getViewerAltitudeLevelAlpha();
                return;
            }

            if (_lodIdx == _lodCount - 1)
            {
//                return OceanRenderer.Instance.ViewerAltitudeLevelAlpha;
                result.weight = clipmap.getViewerAltitudeLevelAlpha();
                return;
            }
        }
        else if (drawOctaveWavelength < _lodMaxWavelength)
        {
            // Fits in this lod
//            return 1f;
            result.weight = 1f;
            return;
        }

//        return 0f;
        result.weight = 0f;
        return;
    }
}
