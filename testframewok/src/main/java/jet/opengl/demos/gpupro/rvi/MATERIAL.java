package jet.opengl.demos.gpupro.rvi;

import jet.opengl.postprocessing.shader.GLSLProgram;

// MATERIAL
//   Loaded from a simple text-file (".mtl") with 3 blocks:
//   1."Textures"
//     - "ColorTexture"
//     - "NormalTexture"
//		 - "SpecularTexture"
//   2."RenderStates"
//     - "cull" -> culling -> requires 1 additional parameter: cull mode
//     - "noDepthTest" -> disable depth-testing
//     - "noDepthMask" -> disable depth-mask
//     - "colorBlend" -> color blending -> requires 3 additional parameters: srcColorBlend/ dstColorBlend/ blendColorOp
//     - "alphaBlend" -> alpha blending -> requires 3 additional parameters: srcAlphaBlend/ dstAlphaBlend/ blendAlphaOp
//   3."Shader"
//     - "permutation" -> requires 1 additional parameter: permutation mask of shader
//     - "file" -> requires 1 additional parameter: filename of shader
// - all parameters are optional
// - order of parameters is indifferent (except in "Shader": permutation must be specified before file)
final class MATERIAL {
    DX11_TEXTURE colorTexture;
    DX11_TEXTURE normalTexture;
    DX11_TEXTURE specularTexture;
    Runnable rasterizerState;
    Runnable depthStencilState;
    Runnable blendState;
    GLSLProgram shader;
    private String name;

    boolean Load(String fileName){
        throw new UnsupportedOperationException();
    }

    // load "Textures"-block
    void LoadTextures(String filename){
        throw new UnsupportedOperationException();
    }

    // load "RenderStates"-block
    void LoadRenderStates(String filename){
        throw new UnsupportedOperationException();
    }

    // load "Shader"-block
    boolean LoadShader(String file){
        throw new UnsupportedOperationException();
    }

    String GetName() { return name; }
}
