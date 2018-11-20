package jet.opengl.demos.nvidia.volumelight;

import jet.opengl.postprocessing.util.DebugTools;

public class ResultCompare {

    public static void main(String[] args) {
        final String root = "E:\\textures\\VolumetricLighting\\";
        final String result_root = "E:\\textures\\VolumetricLightingResult\\";

        String filenames[] = {
            "Composite_GL",
            "CopyDepthGL",
            "Direction_Light_DS_GL0",
            "Direction_Light_DS_GL1",
            "Direction_Light_DS_GL2",
            "Direction_Light_DS_GL3",
            "Directional_DS_GL",
            "Directional_GL",
            "pLightLUT_P_1_GL",
            "pLightLUT_S1_GL",
            "pLightLUT_S2_GL",
                "SpotLight_DS_GL0",
            "SpotLight_DS_GL1",
            "SpotLight_DS_GL2",
            "SpotLight_GL",
            "SpotLight_DS_GL",
            "UpdateMediumLUTGL",
                "ShadowMap"
        };

        for(int i = 0; i < filenames.length; i++){
            String src = filenames[i];
            String str1 = root + src + ".txt";
            String str2 = root + src + "_2.txt";
            String result = result_root + src + "_Result.txt";

            DebugTools.fileCompare(str1, str2, result);
        }
    }
}
