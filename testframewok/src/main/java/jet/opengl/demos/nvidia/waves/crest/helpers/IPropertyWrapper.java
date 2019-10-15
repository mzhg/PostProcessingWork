package jet.opengl.demos.nvidia.waves.crest.helpers;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.texture.TextureGL;

/** Unified interface for setting properties on both materials and material property blocks*/
public interface IPropertyWrapper {

    void SetFloat(int param, float value);
    void SetFloatArray(int param, float[] value);
    void SetVector(int param, Vector4f value);
    void SetVectorArray(int param, Vector4f[] value);
    void SetTexture(int param, TextureGL value);
    void SetMatrix(int param, Matrix4f matrix);
    void SetInt(int param, int value);
}
