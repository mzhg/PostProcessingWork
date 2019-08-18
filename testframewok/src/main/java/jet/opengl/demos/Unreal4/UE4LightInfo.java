package jet.opengl.demos.Unreal4;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.intel.va.VaBoundingSphere;
import jet.opengl.postprocessing.core.volumetricLighting.LightType;
import jet.opengl.postprocessing.texture.TextureGL;

public class UE4LightInfo {
    public final Vector3f position = new Vector3f();
    public float range;
    public final Vector3f direction = new Vector3f();
    public float spotAngle;
    public final Vector3f color = new Vector3f();
    public float intensity;

    public VaBoundingSphere boundingSphere;
    public Matrix4f proj;
    public Matrix4f view;
    public Matrix4f[] cubeViews;  // only for the cube shadow map

    public LightType type;
    public TextureGL shadowmap;
    public float VolumetricScatteringIntensity =1;
    public float SourceLength = 0;
    public float SourceRadius = 0;

}
