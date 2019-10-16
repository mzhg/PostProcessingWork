package jet.opengl.demos.nvidia.waves.crest.helpers;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.demos.nvidia.waves.crest.CommandBuffer;
import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.TextureGL;

public class PropertyWrapperCompute implements IPropertyWrapper {
    private CommandBuffer _commandBuffer = null;
    GLSLProgram _computeShader = null;
    int _computeKernel = -1;

    private GLFuncProvider gl;

    public void Initialise(
            CommandBuffer commandBuffer,
            GLSLProgram computeShader, int computeKernel
    )
    {
        _commandBuffer = commandBuffer;
        _computeShader = computeShader;
        _computeKernel = computeKernel;

        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    public void SetFloat(int param, float value) { _commandBuffer.SetComputeFloatParam(_computeShader, param, value); }
    public void SetFloatArray(int param, float[] value) { _commandBuffer.SetGlobalFloatArray(param, value); }
    public void SetInt(int param, int value) { _commandBuffer.SetComputeIntParam(_computeShader, param, value); }
    public void SetTexture(int param, TextureGL value) { _commandBuffer.SetComputeTextureParam(_computeShader, _computeKernel, param, value); }
    public void SetVector(int param, ReadableVector4f value) { _commandBuffer.SetComputeVectorParam(_computeShader, param, value); }
    public void SetVectorArray(int param, Vector4f[] value) { _commandBuffer.SetComputeVectorArrayParam(_computeShader, param, value); }
    public void SetMatrix(int param, Matrix4f value) { _commandBuffer.SetComputeMatrixParam(_computeShader, param, value); }

    // NOTE: these MUST match the values in OceanLODData.hlsl
    // 64 recommended as a good common minimum: https://www.reddit.com/r/GraphicsProgramming/comments/aeyfkh/for_compute_shaders_is_there_an_ideal_numthreads/
    public final int THREAD_GROUP_SIZE_X = 8;
    public final int THREAD_GROUP_SIZE_Y = 8;
    public void DispatchShader()
    {
        /*_commandBuffer.DispatchCompute(
                _computeShader, _computeKernel,
                OceanRenderer.Instance.LodDataResolution / THREAD_GROUP_SIZE_X,
                OceanRenderer.Instance.LodDataResolution / THREAD_GROUP_SIZE_Y,
                1
        );*/


        _computeShader.enable();
        gl.glDispatchCompute(OceanRenderer.Instance.LodDataResolution() / THREAD_GROUP_SIZE_X,
                OceanRenderer.Instance.LodDataResolution() / THREAD_GROUP_SIZE_Y,
                1);

        _commandBuffer = null;
        _computeShader = null;
        _computeKernel = -1;
    }

    public void DispatchShaderMultiLOD()
    {
        _computeShader.enable();
        gl.glDispatchCompute(
//                _computeShader, _computeKernel,
                OceanRenderer.Instance.LodDataResolution() / THREAD_GROUP_SIZE_X,
                OceanRenderer.Instance.LodDataResolution() / THREAD_GROUP_SIZE_Y,
                OceanRenderer.Instance.CurrentLodCount()
        );

        _commandBuffer = null;
        _computeShader = null;
        _computeKernel = -1;
    }
}
