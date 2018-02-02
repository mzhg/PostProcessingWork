package jet.opengl.demos.intel.cput;

import java.nio.ByteBuffer;
import java.util.Arrays;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgramPipeline;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2018/1/16.
 */

public class CPUTRenderParametersDX extends CPUTRenderParameters implements Disposeable{

    private GLSLProgramPipeline mpShaderProgram;
    private RenderTargets       mpRenderTarget;
    private GLFuncProvider      gl;

    public CPUTRenderParametersDX(){}

    public void InitlizeDX(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        mpShaderProgram = new GLSLProgramPipeline();
        mpRenderTarget = new RenderTargets();
    }

    public void BeginRender(){
        gl.glUseProgram(0);
        mpShaderProgram.enable();
    }

    public void PSSetShader(ShaderProgram ps){ mpShaderProgram.setPS(ps);}
    public void VSSetShader(ShaderProgram vs){ mpShaderProgram.setVS(vs);}

    public void BlitToScreen(int width, int height){
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, mpRenderTarget.getFramebuffer());
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
        gl.glBlitFramebuffer(0, 0, width, height,
                0, 0, width, height,
                GLenum.GL_COLOR_BUFFER_BIT, GLenum.GL_NEAREST);
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, 0);
    }

    public void OMSetRenderTargets(Texture2D[] colorDSV, Texture2D depthStencilDSV){
        mpRenderTarget.bind();

        if(colorDSV == null && depthStencilDSV == null){
//            mpRenderTarget.setRenderTextures(null, null);  nothing need to-do
        }else if(colorDSV == null){
            mpRenderTarget.setRenderTexture(depthStencilDSV, null);
        }else if(depthStencilDSV == null){
            mpRenderTarget.setRenderTextures(colorDSV, null);
        }else{
            Texture2D[] RTVs = Arrays.copyOf(colorDSV, colorDSV.length+1);
            RTVs[colorDSV.length] = depthStencilDSV;
            mpRenderTarget.setRenderTextures(RTVs, null);
        }
    }

    public void OMSetRenderTargets(Texture2D colorDSV, Texture2D depthStencilDSV){
        mpRenderTarget.bind();

        if(colorDSV == null && depthStencilDSV == null){
//            mpRenderTarget.setRenderTextures(null, null);  nothing need to-do
        }else if(colorDSV == null){
            mpRenderTarget.setRenderTexture(depthStencilDSV, null);
        }else if(depthStencilDSV == null){
            mpRenderTarget.setRenderTexture(colorDSV, null);
        }else{
            mpRenderTarget.setRenderTextures(CommonUtil.toArray(colorDSV, depthStencilDSV), null);
        }
    }

    public void ClearRenderTargetView(TextureGL tex, float[] clearColor){
        int internalFormat = tex.getFormat();
        gl.glClearTexImage(tex.getTexture(), 0, TextureUtils.measureFormat(internalFormat),TextureUtils.measureDataType(internalFormat),
                clearColor != null? CacheBuffer.wrap(clearColor): null);
    }

    public void ClearDepthStencilView(TextureGL tex, int mask, float depthClearValue, int stencilClearValue){
        int internalFormat = tex.getFormat();
        int dataType = TextureUtils.measureDataType(internalFormat);
        ByteBuffer clearColor = CacheBuffer.getCachedByteBuffer(8);
        if(dataType == GLenum.GL_UNSIGNED_SHORT){  // depth 16
            int depthValue = (int) (Numeric.clamp(depthClearValue, 0, 1) * Numeric.MAX_USHORT);
            clearColor.putInt(depthValue);
        }else if(dataType == GLenum.GL_UNSIGNED_INT_24_8){  // depth 24 stencil 8
            int depthValue = (int) (Numeric.clamp(depthClearValue, 0, 1) * 0xFFFFFF);
            clearColor.putInt(depthValue).position(3);
            clearColor.put((byte)stencilClearValue);
        }else if(dataType == GLenum.GL_DEPTH_COMPONENT32){  // depth 32
            int depthValue = (int) (Numeric.clamp(depthClearValue, 0, 1) * Numeric.MAX_UINT);
            clearColor.putInt(depthValue);
        }else if(dataType == GLenum.GL_DEPTH_COMPONENT32F){
            clearColor.putFloat(1.0f);
        }else if(dataType == GLenum.GL_FLOAT_32_UNSIGNED_INT_24_8_REV){
            clearColor.putFloat(depthClearValue);
            clearColor.putInt(stencilClearValue);
        }

        clearColor.flip();

        gl.glClearTexImage(tex.getTexture(), 0, TextureUtils.measureFormat(internalFormat), dataType, clearColor);
        GLCheck.checkError();
    }

    public void RSSetViewports(int x, int y, int width, int height){
        gl.glViewport(x, y, width, height);
    }

    @Override
    public void dispose() {
        SAFE_RELEASE(mpRenderTarget); mpRenderTarget = null;
        SAFE_RELEASE(mpShaderProgram); mpShaderProgram = null;
    }
}
