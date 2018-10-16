package jet.opengl.demos.nvidia.face.libs;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.omg.CORBA.PRIVATE_MEMBER;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by Administrator on 2018/10/13 0013.
 */

public class SubsurfaceScatteringPass {
    private int width, height;
    private final Matrix4f projection = new Matrix4f();
    private float sssLevel, correction, maxdd;

    private RenderTargets renderTargets;
    private final Texture2D[] textures = new Texture2D[2];
    private GLSLProgram downsamplePS;
    private GLSLProgram blurPS;
    private GLSLProgram blurAccumPS;
    private BufferGL uniformsBuffer;
    private GLFuncProvider gl;

    private static final class Uniforms{
        static final int SIZE = Vector4f.SIZE * 4;
        final Vector2f pixelSize = new Vector2f();
        float sssLevel;
        float correction;

        final Vector3f projection = new Vector3f();
        float maxdd;

        final Vector4f weight = new Vector4f();

        float depth;
        float width;
        float material;

        ByteBuffer store(ByteBuffer buf){
            pixelSize.store(buf);
            buf.putFloat(sssLevel);
            buf.putFloat(correction);

            projection.store(buf);
            buf.putFloat(maxdd);

            weight.store(buf);

            buf.putFloat(depth);
            buf.putFloat(width);
            buf.putFloat(material);
            buf.putFloat(0);

            return buf;
        }
    }

    private final Uniforms uniforms = new Uniforms();

    // width, height, format and samples: they should match the backbuffer.
    //     'samples' is used for downsampling the depth-stencil buffer (see
    //     downsample below).
    // projection: projection matrix used to render the scene.
    // sssLevel: specifies the global level of subsurface scattering (see
    //     article for more info).
    // correction: specifies how subsurface scattering varies with depth
    //     gradient (see article for more info).
    // maxdd: limits the effects of the derivative (see article for more
    //        info).
    public SubsurfaceScatteringPass(//ID3D10Device *device,
                             int width, int height, int format, int samples,Matrix4f projection,
                             float sssLevel, float correction, float maxdd){
        this.width = width;
        this.height = height;
        this.projection.load(projection);
        this.sssLevel = sssLevel;
        this.correction = correction;
        this.maxdd = maxdd;

        renderTargets = new RenderTargets();
        Texture2DDesc desc = new Texture2DDesc(width, height, format);
        textures[0] = TextureUtils.createTexture2D(desc,  null);
        textures[1] = TextureUtils.createTexture2D(desc,  null);

        uniformsBuffer = new BufferGL();
        uniformsBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, Uniforms.SIZE, null, GLenum.GL_STREAM_DRAW);

        try {
            String path = "nvidia/FaceWorks/shaders/";
            downsamplePS = GLSLProgram.createFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", path+"DownsamplePS.frag");
            blurPS = GLSLProgram.createFromFiles(path + "PassZVS.vert", path+"BlurPS.frag");
            blurAccumPS = GLSLProgram.createFromFiles(path + "PassZVS.vert", path+"BlurAccumPS.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }

        uniforms.pixelSize.set(1.0f / width, 1.0f / height);
        setInputVars();
    }

    // Both 'depthResource' and 'stencilResource' can be output in the main
    // render pass using multiple render targets. They are used to produce
    // a downsampled depth-stencil and depth map.
    //
    // depthRenderTarget: output render target where the downsampled linear
    //     depth will be stored.
    // depthResource: input multisampled resource of the *linear* depth
    //     map.
    // stencilResource: input multisampled resource of the stencil buffer.
    // depthStencil: output downsampled depth-stencil.
    public void downsample(Texture2D depthRenderTarget,
                           Texture2D depthResource,
                           Texture2D stencilResource,
                           Texture2D depthStencil){
        renderTargets.bind();
        renderTargets.setRenderTextures(CommonUtil.toArray(depthRenderTarget, depthStencil), null);
        gl.glViewport(0, 0, depthStencil.getWidth(), depthStencil.getHeight());
        gl.glClearBufferfi(GLenum.GL_DEPTH_STENCIL, 0, 1.f, 0);

        downsamplePS.enable();

        gl.glBindTextureUnit(0, depthResource.getTexture());
        gl.glBindTextureUnit(1, stencilResource.getTexture());

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

    }

    // IMPORTANT NOTICE: all render targets below must not be multisampled.
    // This implies that you have to resolve the main render target and
    // downsample the depth-stencil buffer. For this task you can use the
    // above 'downsample' function.
    //
    // mainRenderTarget: render target of the rendered final image.
    // mainResource: shader resource of the rendered final image.
    // depthResource: shader resource of the *linear* depth map. We cannot
    //     use the original depth-stencil because we need to use it as shader
    //     resource at the same time we use it as depth-stencil
    // depthStencil: depth-stencil used to render the scene (in the
    //     conventional non-linear form).
    // gaussians: sum of gaussians of the profile we want to render.
    // stencilId: stencil value used to render the objects we must apply
    //     subsurface scattering.
    public void render(Texture2D mainRenderTarget,
                       Texture2D mainResource,
                       Texture2D depthResource,
                       Texture2D depthStencil,
                     List<Gaussian> gaussians,
                int stencilId){
        uniforms.material = stencilId;

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(Uniforms.SIZE);
        uniforms.store(buffer);
        buffer.flip();

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, uniformsBuffer.getBuffer());
        uniformsBuffer.update(0, buffer);

        gl.glClearTexImage(textures[0].getTexture(), 0, TextureUtils.measureFormat(textures[0].getFormat()), TextureUtils.measureDataType(textures[0].getFormat()), null);
        gl.glClearTexImage(textures[1].getTexture(), 0, TextureUtils.measureFormat(textures[1].getFormat()), TextureUtils.measureDataType(textures[1].getFormat()), null);

        blurPass(mainResource, textures[0], mainRenderTarget, depthStencil, gaussians.get(0), true);
        for (int i = 1; i <gaussians.size(); i++) {
            blurPass(textures[0], textures[0], mainRenderTarget, depthStencil, gaussians.get(i), false);
        }
    }

    public void setSssLevel(float sssLevel) { this.sssLevel = sssLevel; setInputVars(); }
    public float getSssLevel()  { return sssLevel; }

    public void setCorrection(float correction) { this.correction = correction; setInputVars(); }
    public float getCorrection()  { return correction; }

    public void setMaxdd(float maxdd) { this.maxdd = maxdd; setInputVars(); }
    public float getMaxdd()  { return maxdd; }

    public void setProjectionMatrix(Matrix4f projection) { this.projection.load(projection); setInputVars(); }
    public  Matrix4f getProjectionMatrix()  { return projection; }

    private void setInputVars(){
        uniforms.correction = correction;
        uniforms.maxdd = maxdd;

        // This changes the SSS level depending on the viewport size and the camera
        // FOV. For a camera FOV of 20.0 and a viewport height of 720 pixels, we
        // want to use the sssLevel as is. For other cases, we will scale it
        // accordingly.
        // In D3DXMatrixPerspectiveFovLH we have _22 = cot(fovY / 2), thus:
        float scaleViewport = height / 720.0f;
        float scaleFov = (float) (projection.m11 / (1.0 / Math.tan(Math.toRadians(20.0f) / 2.0f)));
        float t = scaleViewport * scaleFov * sssLevel;

        uniforms.sssLevel = t;
//        uniforms.projection.set(projection._33, projection._43, projection._34);  TODO
    }
    private void blurPass(Texture2D src,
                          Texture2D dst,
                          Texture2D finalOut,
                          Texture2D depthStencil,
                       Gaussian gaussian,
                  boolean firstGaussian){
        float depth = firstGaussian? 1.0f : Math.min(Math.max(linearToDepth(0.5f * gaussian.getWidth() * sssLevel), 0.0f), 1.0f);
        
    }

    private float linearToDepth(float z){
        return 0; // todo
    }
}
