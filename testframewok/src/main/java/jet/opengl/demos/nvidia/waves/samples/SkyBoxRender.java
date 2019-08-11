package jet.opengl.demos.nvidia.waves.samples;

import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.Model;
import com.nvidia.developer.opengl.models.ModelGenerator;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.util.NvImage;

public final class SkyBoxRender implements Disposeable{

	int m_CubeTexture;
	int m_CubeSampler;
	SkyBoxProgram m_SkyBoxProgram;
	GLVAO m_SkyVAO;
	
	final Matrix4f m_MVP = new Matrix4f();
	final Matrix4f m_Projection = new Matrix4f();
	final Matrix4f m_Rotation = new Matrix4f();
	
	boolean m_NeedUpdateMatrix = true;
	boolean m_NeedApplyTextureSampler = true;
	boolean m_SelfLoadTexture = false;
	boolean m_HasMipmap;
	boolean m_HasSetupProperty;
	private GLFuncProvider gl;
	
	public SkyBoxRender() {
		this(100);
	}
	
	public SkyBoxRender(float size) {
		gl = GLFuncProviderFactory.getGLFuncProvider();
		m_SkyBoxProgram = new SkyBoxProgram(0);
		Model sky_model = ModelGenerator.genCube(Math.max(size, 1), false, false, false);
		sky_model.bindAttribIndex(0, 0);
		m_SkyVAO = sky_model.genVAO();
	}
	
	/** Setting the rotation matrix. */
	public void setRotateMatrix(Matrix4f mat) {
		m_Rotation.load(mat);
		m_Rotation.m30 =0;  // make sure this is no translation part
		m_Rotation.m31 =0;
		m_Rotation.m32 =0;
		m_NeedUpdateMatrix = true;
	}
	
	public void setProjectionMatrix(Matrix4f mat) { 
		m_Projection.load(mat);
		m_NeedUpdateMatrix = true;
	}
	
	public void setCubemap(int cubemap){
		releaseTexture();
		
		m_SelfLoadTexture = false;
		m_CubeTexture = cubemap;
	}
	
	public void setCubemapSampler(int sampler){
		m_CubeSampler = sampler;
	}
	
	public void loadCubemapFromDDSFile(String filename) throws IOException{
		releaseTexture();
		
		NvImage image = new NvImage();
		image.loadImageFromFile(filename);
		
		if(!image.isCubeMap()){
			throw new IllegalArgumentException("The " + filename + " doesn't a cube map file.");
		}
		
		m_SelfLoadTexture = true;
		m_CubeTexture = image.updaloadTexture();
		m_HasMipmap = image.getMipLevels() > 1;
		m_HasSetupProperty = false;
		
		// unbind the texture.
		gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, 0);
	}
	
	public void draw() {
		if(m_CubeTexture == 0){
			System.err.println("No cubemap binding. Igore the sky box rendering.");
			return;
		}

		gl.glDisable(GLenum.GL_DEPTH_TEST);
		gl.glDepthMask(false);
		
		// Enable the program and setup matrix.
		m_SkyBoxProgram.enable();
		if(m_NeedUpdateMatrix){
			Matrix4f.mul(m_Projection, m_Rotation, m_MVP);
			m_SkyBoxProgram.applyMVP(m_MVP);
			m_NeedUpdateMatrix = false;
		}
		
		// binding texture.
		gl.glActiveTexture(GLenum.GL_TEXTURE0);
		gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, m_CubeTexture);
		
		// setup the property or sampler.
		/*if(m_CubeSampler == 0 && SamplerUtils.isSamplerSupport()){
			SamplerDesc desc = new SamplerDesc();
			
			if(m_SelfLoadTexture && m_HasMipmap){
				desc.minFilter = GLenum.GL_LINEAR_MIPMAP_LINEAR;
			}
			
			// Use the default value to create sampler object.
			// Note: we din't check the mipmap for the cube map
			// This issue will fixed in the next version.
			m_CubeSampler = SamplerUtils.createSampler(desc);
		}*/
		
		if(m_CubeSampler != 0){
			gl.glBindSampler(0, m_CubeSampler);
		}else{
			gl.glBindSampler(0, 0);
			// The driver doesn't support the sampler object.
			if(m_SelfLoadTexture){
				if(!m_HasSetupProperty){
					SamplerUtils.applyCubemapSampler(m_HasMipmap);
					m_HasSetupProperty = true;
				}
			}else{
				// So the properties depending on the user settings.
			}
		}
		
		// draw the cubemap
		m_SkyVAO.bind();
		m_SkyVAO.draw(GLenum.GL_TRIANGLES);
		
		// reset opengl states.
		m_SkyVAO.unbind();
		gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, 0);
		gl.glDepthMask(true);
		if(m_CubeSampler != 0)
			gl.glBindSampler(0, 0);
	}

	@Override
	public void dispose() {
		m_SkyBoxProgram.dispose();
		m_SkyVAO.dispose();
		
		releaseTexture();
	}
	
	private void releaseTexture(){
		if(m_SelfLoadTexture && m_CubeTexture != 0){
			gl.glDeleteTexture(m_CubeTexture);
			m_CubeTexture = 0;
			m_CubeSampler = 0;
		}
	}

	public int getCubeMap() { return m_CubeTexture;}
}
