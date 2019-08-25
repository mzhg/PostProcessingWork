package jet.opengl.demos.Unreal4.distancefield;

import jet.opengl.postprocessing.util.Numeric;

public class FDistanceFieldAOParameters {
    float GlobalMaxOcclusionDistance;
    float ObjectMaxOcclusionDistance;
    float Contrast;

    FDistanceFieldAOParameters(float InOcclusionMaxDistance, float InContrast, boolean GAOGlobalDistanceField, float GAOGlobalDFStartDistance){
        Contrast = Numeric.clamp(InContrast, .01f, 2.0f);
        InOcclusionMaxDistance = Numeric.clamp(InOcclusionMaxDistance, 2.0f, 3000.0f);

        if (GAOGlobalDistanceField)
        {
//            extern float GAOGlobalDFStartDistance;
            ObjectMaxOcclusionDistance = Math.min(InOcclusionMaxDistance, GAOGlobalDFStartDistance);
            GlobalMaxOcclusionDistance = InOcclusionMaxDistance >= GAOGlobalDFStartDistance ? InOcclusionMaxDistance : 0;
        }
        else
        {
            ObjectMaxOcclusionDistance = InOcclusionMaxDistance;
            GlobalMaxOcclusionDistance = 0;
        }
    }
}
