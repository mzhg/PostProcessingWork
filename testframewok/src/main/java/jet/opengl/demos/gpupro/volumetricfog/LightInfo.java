package jet.opengl.demos.gpupro.volumetricfog;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.intel.va.VaBoundingSphere;
import jet.opengl.postprocessing.core.volumetricLighting.LightType;
import jet.opengl.postprocessing.texture.TextureGL;

class LightInfo {
    final Vector3f position = new Vector3f();
    float range;
    final Vector3f direction = new Vector3f();
    float spotAngle;
    final Vector3f color = new Vector3f();
    float intensity;

    VaBoundingSphere boundingSphere;
    Matrix4f proj;
    Matrix4f view;
    Matrix4f[] cubeViews;  // only for the cube shadow map

    LightType type;
    TextureGL shadowmap;
}
