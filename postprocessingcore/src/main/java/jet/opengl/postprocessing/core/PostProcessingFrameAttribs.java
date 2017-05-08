package jet.opengl.postprocessing.core;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.Recti;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public class PostProcessingFrameAttribs {
    public final Recti viewport = new Recti();
    public final Recti clipRect = new Recti();
    public Texture2D sceneColorTexture;
    public Texture2D sceneDepthTexture;
    public Texture2D outputTexture;
    public boolean colorDepthCombined;

    public final Matrix4f viewMat = new Matrix4f();
    public final Matrix4f projMat = new Matrix4f();

    public float cameraNear, cameraFar;
}
