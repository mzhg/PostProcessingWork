package jet.opengl.demos.nvidia.waves.crest.helpers;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;

public final class TextureArrayHelpers {

    /*private final String ClearToBlackShaderName = "ClearToBlack";
    private static int krnl_ClearToBlack = -1;
    private static GLSLProgram ClearToBlackShader;
    private static int sp_LD_TexArray_Target = 0; //Shader.PropertyToID("_LD_TexArray_Target");*/

    // This is used as alternative to Texture2D.blackTexture, as using that
    // is not possible in some shaders.
    public static Texture2D BlackTextureArray;

    static //TextureArrayHelpers()
    {
        if (BlackTextureArray == null)
        {
            /*BlackTextureArray = new Texture2DArray(
                    Texture2D.blackTexture.width, Texture2D.blackTexture.height,
                    OceanRenderer.Instance.CurrentLodCount,
                    Texture2D.blackTexture.format,
                    false,
                    false

            );*/


            /*for (int textureArrayIndex = 0; textureArrayIndex < OceanRenderer.Instance.CurrentLodCount(); textureArrayIndex++)
            {
                Graphics.CopyTexture(Texture2D.blackTexture, 0, 0, BlackTextureArray, textureArrayIndex, 0);
            }
*/
//            BlackTextureArray.setName("Black Texture2DArray");
        }

//        ClearToBlackShader = Resources.Load<ComputeShader>(ClearToBlackShaderName);
//        krnl_ClearToBlack = ClearToBlackShader.FindKernel(ClearToBlackShaderName);
    }



    // Unity 2018.* does not support blitting to texture arrays, so have
    // implemented a custom version to clear to black
    public static void ClearToBlack(TextureGL dst)
    {
        /*ClearToBlackShader.SetTexture(krnl_ClearToBlack, sp_LD_TexArray_Target, dst);
        ClearToBlackShader.Dispatch(
                krnl_ClearToBlack,
                OceanRenderer.Instance.LodDataResolution / PropertyWrapperCompute.THREAD_GROUP_SIZE_X,
                OceanRenderer.Instance.LodDataResolution / PropertyWrapperCompute.THREAD_GROUP_SIZE_Y,
                dst.volumeDepth
        );*/

        final int format = TextureUtils.measureFormat(dst.getFormat());
        final int type = TextureUtils.measureDataType(dst.getFormat());
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        for(int i = 0; i < dst.getMipLevels(); i++){
            gl.glClearTexImage(dst.getTexture(), i, format, type, null);
        }
    }
}
