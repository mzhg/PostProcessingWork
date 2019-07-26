package jet.opengl.demos.gpupro.volumetricfog;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.texture.Texture3D;

class MaterialSetupParams {
    final Vector4f ExponentialFogParameters = new Vector4f();
    final Vector4f ExponentialFogParameters2 = new Vector4f();
    final Vector4f ExponentialFogParameters3 = new Vector4f();
    final Vector3f GlobalAlbedo = new Vector3f();
    final Vector3f GlobalEmissive = new Vector3f();
    float GlobalExtinctionScale;
    final Matrix4f UnjitteredClipToTranslatedWorld = new Matrix4f();
    final Matrix4f g_ViewProj = new Matrix4f();
    final Vector3f View_PreViewTranslation = new Vector3f();
    final Vector3f VolumetricFog_GridSize = new Vector3f();
    final Vector3f VolumetricFog_GridZParams = new Vector3f();
}
