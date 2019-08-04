package jet.opengl.demos.nvidia.volumelight;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureGL;

public class VolumeLightParams {
    public Texture2D sceneColor;
    public Texture2D sceneDepth;
    public TextureGL shadowMap;

    public float cameraNear, cameraFar;
    public float lightNear, lightFar;
    public final Matrix4f cameraView = new Matrix4f();
    public final Matrix4f cameraProj = new Matrix4f();
    public final Matrix4f lightView = new Matrix4f();
    public final Matrix4f lightProj = new Matrix4f();

    public float sampleRate = 1f;

    final Vector3f eyePos = new Vector3f();
    final Vector3f lightForward = new Vector3f();
    final Vector3f lightPosition = new Vector3f();
    final Vector3f lightRight = new Vector3f();
    final Vector3f lightUp = new Vector3f();
    final Matrix4f viewProjInv = new Matrix4f();
    final Matrix4f lightViewProj = new Matrix4f();

    void resolve(){
        Matrix4f.decompseRigidMatrix(cameraView, eyePos, null, null, null);
        Matrix4f.decompseRigidMatrix(lightView, lightPosition, lightRight, lightUp, lightForward);
        lightForward.scale(-1);

        Matrix4f.mul(cameraProj, cameraView, viewProjInv);  viewProjInv.invert();
        Matrix4f.mul(lightProj, lightView, lightViewProj);
    }
}
