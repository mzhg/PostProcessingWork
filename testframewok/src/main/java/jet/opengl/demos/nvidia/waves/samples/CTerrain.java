package jet.opengl.demos.nvidia.waves.samples;

import java.io.IOException;
import java.util.Random;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL40;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.shader.VisualDepthTextureProgram;
import jet.opengl.postprocessing.texture.FramebufferGL;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.util.buffer.GLUtil;
import jet.util.check.GLError;
import jet.util.opengl.pixel.NvImage;
import jet.util.opengl.pixel.TextureData;
import jet.util.opengl.pixel.TextureUtils;
import jet.util.opengl.shader.libs.FullscreenProgram;
import jet.util.opengl.shader.libs.FullscreenShadowProgram;
import jet.util.opengl.shader.libs.postprocessing.FrameBufferBuilder;
import jet.util.opengl.shader.libs.postprocessing.FramebufferGL;
import jet.util.opengl.shader.libs.postprocessing.TextureInfo;

/*public*/ class CTerrain {
	
	static boolean debug_terrain_rendering = false;
	static boolean debug_shadowmapping = false;
	static boolean debug_reflection = false;
	static boolean debug_waternormalmap = false;
	static boolean debug_main_color = false;
	static boolean debug_sky = false;
	
	static final int terrain_gridpoints = 512;
	static final int terrain_numpatches_1d = 64;
	static final float terrain_geometry_scale = 1.0f;
	static final float terrain_maxheight = 30.0f;
	static final float terrain_minheight = -30.0f;
	static final float terrain_fractalfactor = 0.68f;;
	static final float terrain_fractalinitialvalue = 100.0f;
	static final float terrain_smoothfactor1 = 0.99f;
	static final float terrain_smoothfactor2 = 0.10f;
	static final float terrain_rockfactor = 0.95f;
	static final int terrain_smoothsteps = 40;
	static final float terrain_height_underwater_start = -100.0f;
	static final float terrain_height_underwater_end = -8.0f;
	static final float terrain_height_sand_start = -30.0f;
	static final float terrain_height_sand_end = 1.7f;
	static final float terrain_height_grass_start = 1.7f;
	static final float terrain_height_grass_end = 30.0f;
	static final float terrain_height_rocks_start = -2.0f;
	static final float terrain_height_trees_start = 4.0f;
	static final float terrain_height_trees_end = 30.0f;
	static final float terrain_slope_grass_start = 0.96f;
	static final float terrain_slope_rocks_start = 0.85f;
	static final float terrain_far_range = terrain_gridpoints*terrain_geometry_scale;
	static final int shadowmap_resource_buffer_size_xy = 2048;
	static final int water_normalmap_resource_buffer_size_xy = 2048;
	static final int terrain_layerdef_map_texture_size = 1024;
	static final int terrain_depth_shadow_map_texture_size = 512;
	static final int sky_gridpoints = 10;
	static final float sky_texture_angle = 0.425f;
	static final float main_buffer_size_multiplier = 1.1f;
	static final float reflection_buffer_size_multiplier = 1.1f;
	static final float refraction_buffer_size_multiplier = 1.1f;
	static final float scene_z_near = 1.0f;
	static final float scene_z_far = 25000.0f;
	static final float camera_fov = 110.0f;
	
//	ID3D11Texture2D		*rock_bump_texture;
//	ID3D11ShaderResourceView *rock_bump_textureSRV;
	int 				rock_bump_texture;

//	ID3D11Texture2D		*rock_microbump_texture;
//	ID3D11ShaderResourceView *rock_microbump_textureSRV;
	int 				rock_microbump_texture;

//	ID3D11Texture2D		*rock_diffuse_texture;
//	ID3D11ShaderResourceView *rock_diffuse_textureSRV;	
	int 				rock_diffuse_texture;

//	ID3D11Texture2D		*sand_bump_texture;
//	ID3D11ShaderResourceView *sand_bump_textureSRV;
	int					sand_bump_texture;

//	ID3D11Texture2D		*sand_microbump_texture;
//	ID3D11ShaderResourceView *sand_microbump_textureSRV;
	int					sand_microbump_texture;

//	ID3D11Texture2D		*sand_diffuse_texture;
//	ID3D11ShaderResourceView *sand_diffuse_textureSRV;
	int 				sand_diffuse_texture;

//	ID3D11Texture2D		*grass_diffuse_texture;
//	ID3D11ShaderResourceView *grass_diffuse_textureSRV;	
	int					grass_diffuse_texture;

//	ID3D11Texture2D		*slope_diffuse_texture;
//	ID3D11ShaderResourceView *slope_diffuse_textureSRV;
	int 				slope_diffuse_texture;

//	ID3D11Texture2D		*water_bump_texture;
//	ID3D11ShaderResourceView *water_bump_textureSRV;
	int 				water_bump_texture;

//	ID3D11Texture2D		*sky_texture;
//	ID3D11ShaderResourceView *sky_textureSRV;
	int 				sky_texture;

//	ID3D11Texture2D			 *reflection_color_resource;
//	ID3D11ShaderResourceView *reflection_color_resourceSRV;
//	ID3D11RenderTargetView   *reflection_color_resourceRTV;
//	ID3D11Texture2D			 *reflection_depth_resource;
//	ID3D11DepthStencilView   *reflection_depth_resourceDSV;
	FramebufferGL reflection_framebuffer;
	
//	ID3D11Texture2D			 *refraction_color_resource;
//	ID3D11ShaderResourceView *refraction_color_resourceSRV;
//	ID3D11RenderTargetView   *refraction_color_resourceRTV;
//	ID3D11Texture2D			 *refraction_depth_resource;
//	ID3D11RenderTargetView   *refraction_depth_resourceRTV;
//	ID3D11ShaderResourceView *refraction_depth_resourceSRV;
	FramebufferGL   refraction_framebuffer;

//	ID3D11Texture2D			 *shadowmap_resource;
//	ID3D11ShaderResourceView *shadowmap_resourceSRV;
//	ID3D11DepthStencilView   *shadowmap_resourceDSV;
	FramebufferGL   shadownmap_framebuffer;
	
//	ID3D11Texture2D			 *water_normalmap_resource;
//	ID3D11ShaderResourceView *water_normalmap_resourceSRV;
//	ID3D11RenderTargetView   *water_normalmap_resourceRTV;
	FramebufferGL   water_normalmap_framebuffer;

//	ID3D11Texture2D			 *main_color_resource;
//	ID3D11ShaderResourceView *main_color_resourceSRV;
//	ID3D11RenderTargetView   *main_color_resourceRTV;
//	ID3D11Texture2D			 *main_depth_resource;
//	ID3D11DepthStencilView   *main_depth_resourceDSV;
//	ID3D11ShaderResourceView *main_depth_resourceSRV;
	FramebufferGL   main_color_framebuffer;
	
//	ID3D11Texture2D		*heightmap_texture;
//	ID3D11ShaderResourceView *heightmap_textureSRV;
	int 				heightmap_texture;

//	ID3D11Texture2D		*layerdef_texture;
//	ID3D11ShaderResourceView *layerdef_textureSRV;
	int 				layerdef_texture;

//	ID3D11Texture2D		*depthmap_texture;
//	ID3D11ShaderResourceView *depthmap_textureSRV;
	int					depthmap_texture;

//	ID3D11Buffer		*heightfield_vertexbuffer;
//	ID3D11Buffer		*sky_vertexbuffer;
	int 				heightfield_vertexbuffer;
	int 				sky_vertexbuffer;
	
	int backbufferWidth, backbufferHeight;
	Random random = new Random(123);
	
	float[] clearColor = {0.8f, 0.8f, 1.0f, 1.0f};
	float[] refractionClearColor = {0.5f, 0.5f, 0.5f, 1.0f};
//	final Matrix4f reflect_mvp = new Matrix4f();
//	final Matrix4f reflect_proj = new Matrix4f();
	
	final Vector3f camera_position = new Vector3f();
	final Vector3f camera_direction = new Vector3f();
	final Matrix4f camera_modelView = new Matrix4f();
	final Matrix4f camera_mvp = new Matrix4f();
	final Matrix4f camera_projection = new Matrix4f();
	
	TextureSampler[] terrain_textures;
	TextureSampler[] water_textures;
	
	IsRenderHeightfieldProgram renderHeightfieldProgram;
	IsWaterRenderProgram waterRenderProgram;
	IsWaterNormalmapCombineProgram waterNormalmapCombineProgram;
	IsSkyProgram skyProgram;
	IsMainToBackBufferProgram mainToBackBufferProgram;
	
	VisualDepthTextureProgram shadowDebugProgram;
	FullscreenProgram textureDebugProgram;

	private GLFuncProvider gl;
	
	void onCreate(String shaderPath, String texturePath){
		gl = GLFuncProviderFactory.getGLFuncProvider();
		try {
			loadTextures(texturePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		createTerrain();
		
		renderHeightfieldProgram = new IsRenderHeightfieldProgram(shaderPath);
		waterNormalmapCombineProgram = new IsWaterNormalmapCombineProgram(shaderPath);
		waterRenderProgram = new IsWaterRenderProgram(shaderPath);
		skyProgram = new IsSkyProgram(shaderPath);
		mainToBackBufferProgram = new IsMainToBackBufferProgram(shaderPath);

		try {
			shadowDebugProgram = new VisualDepthTextureProgram(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		textureDebugProgram = new FullscreenProgram();
//		g_HeightfieldTexture,  0
//		g_LayerdefTexture,  1
//		g_SandBumpTexture,  2
//		g_RockBumpTexture,  3
//		g_WaterNormalMapTexture,  4
//		g_SandMicroBumpTexture,  5
//		g_RockMicroBumpTexture,  6
//		g_SlopeDiffuseTexture,  7
//		g_SandDiffuseTexture,  8
//		g_RockDiffuseTexture,  9
//		g_GrassDiffuseTexture,  10
//		g_DepthTexture,  11
		terrain_textures = new TextureSampler[12];
		terrain_textures[0] = new TextureSampler(heightmap_texture, IsSamplers.g_SamplerLinearWrap);
		terrain_textures[1] = new TextureSampler(layerdef_texture, IsSamplers.g_SamplerLinearWrap);
		terrain_textures[2] = new TextureSampler(sand_bump_texture, IsSamplers.g_SamplerLinearMipmapWrap);
		terrain_textures[3] = new TextureSampler(rock_bump_texture, IsSamplers.g_SamplerLinearMipmapWrap);
		terrain_textures[4] = new TextureSampler(0, IsSamplers.g_SamplerLinearWrap);
		terrain_textures[5] = new TextureSampler(sand_microbump_texture, IsSamplers.g_SamplerAnisotropicWrap);
		terrain_textures[6] = new TextureSampler(rock_microbump_texture, IsSamplers.g_SamplerAnisotropicWrap);
		terrain_textures[7] = new TextureSampler(slope_diffuse_texture, IsSamplers.g_SamplerAnisotropicWrap);
		terrain_textures[8] = new TextureSampler(sand_diffuse_texture, IsSamplers.g_SamplerAnisotropicWrap);
		terrain_textures[9] = new TextureSampler(rock_diffuse_texture, IsSamplers.g_SamplerAnisotropicWrap);
		terrain_textures[10] = new TextureSampler(grass_diffuse_texture, IsSamplers.g_SamplerAnisotropicWrap);
		terrain_textures[11] = new TextureSampler(0, IsSamplers.g_SamplerDepthAnisotropic);
		
		water_textures = new TextureSampler[7];
		water_textures[0] = new TextureSampler(0, 0);  // g_HeightfieldTexture;
		water_textures[1] = new TextureSampler(0, IsSamplers.g_SamplerDepthAnisotropic);  // g_DepthTexture;
		water_textures[2] = new TextureSampler(water_bump_texture, IsSamplers.g_SamplerLinearWrap);  // g_WaterBumpTexture;
		water_textures[3] = new TextureSampler(0, IsSamplers.g_SamplerLinearClamp);  // g_RefractionDepthTextureResolved;
		water_textures[4] = new TextureSampler(sky_texture, IsSamplers.g_SamplerLinearClamp);  // g_ReflectionTexture;
		water_textures[5] = new TextureSampler(0, IsSamplers.g_SamplerLinearClamp);  // g_RefractionTexture;
		water_textures[6] = new TextureSampler(depthmap_texture, IsSamplers.g_SamplerLinearWrap);  // g_DepthMapTexture;
		
	}

	private void buildFramebuffer(FramebufferGL fbo, Texture2DDesc[] tex_descs, TextureAttachDesc[] attach_descs){
		fbo.bind();

		for(int i = 0; i < tex_descs.length;i++){
			fbo.addTexture2D(tex_descs[i], attach_descs[i]);
		}

		gl.glDrawBuffers(GLenum.GL_COLOR_ATTACHMENT0);
	}
	
	void onReshape(int width, int height){
		if(width <= 0 || height <= 0)
			return;

		if(backbufferWidth == width && backbufferHeight == height)
			return;
		// release previous framebuffers.
		releaseFrameBuffer();
		backbufferWidth = width;
		backbufferHeight = height;
		
//		FrameBufferBuilder builder = new FrameBufferBuilder();
//		TextureInfo tex_desc = builder.createColorTexture();
//		builder.setWidth((int) (backbufferWidth * main_buffer_size_multiplier));
//		builder.setHeight((int) (backbufferHeight * main_buffer_size_multiplier));
//		tex_desc.setInternalFormat(GL11.GL_RGBA8);
//		TextureInfo depth_desc = builder.getOrCreateDepthTexture();
//		depth_desc.setInternalFormat(FramebufferGL.FBO_DepthBufferType_TEXTURE_DEPTH32F);

		Texture2DDesc[] tex_descs = {
				new Texture2DDesc((int) (backbufferWidth * main_buffer_size_multiplier), (int) (backbufferHeight * main_buffer_size_multiplier), GLenum.GL_RGBA8),
				new Texture2DDesc((int) (backbufferWidth * main_buffer_size_multiplier), (int) (backbufferHeight * main_buffer_size_multiplier), GLenum.GL_DEPTH_COMPONENT32F),
		};

		final TextureAttachDesc default_desc = new TextureAttachDesc();

		TextureAttachDesc[] attach_descs = {
				default_desc,
				default_desc
		};
		
		main_color_framebuffer = new FramebufferGL();
		reflection_framebuffer = new FramebufferGL();
		refraction_framebuffer = new FramebufferGL();
		buildFramebuffer(main_color_framebuffer, tex_descs, attach_descs);
		buildFramebuffer(reflection_framebuffer, tex_descs, attach_descs);
		buildFramebuffer(refraction_framebuffer, tex_descs, attach_descs);

//		builder.setWidth(water_normalmap_resource_buffer_size_xy);
//		builder.setHeight(water_normalmap_resource_buffer_size_xy);
//		depth_desc.setInternalFormat(FramebufferGL.FBO_DepthBufferType_NONE);
		water_normalmap_framebuffer = new FramebufferGL();
		water_normalmap_framebuffer.addTexture2D(new Texture2DDesc(water_normalmap_resource_buffer_size_xy, water_normalmap_resource_buffer_size_xy, GLenum.GL_RGB8), default_desc);
		
//		builder.setWidth(shadowmap_resource_buffer_size_xy);
//		builder.setHeight(shadowmap_resource_buffer_size_xy);
//		depth_desc.setInternalFormat(FramebufferGL.FBO_DepthBufferType_TEXTURE_DEPTH32F);
//		builder.getColorTextures().clear();
		shadownmap_framebuffer = new FramebufferGL(/*builder*/);
		shadownmap_framebuffer.addTexture2D(new Texture2DDesc(shadowmap_resource_buffer_size_xy,shadowmap_resource_buffer_size_xy, GLenum.GL_DEPTH_COMPONENT32F), default_desc);
		
		terrain_textures[4].textureID = water_normalmap_framebuffer.getAttachedTex(0).getTexture();  // color
		terrain_textures[11].textureID = shadownmap_framebuffer.getAttachedTex(0).getTexture();   // depth
		
		water_textures[1].textureID = shadownmap_framebuffer.getAttachedTex(0).getTexture();  // depth
		water_textures[3].textureID = refraction_framebuffer.getAttachedTex(1).getTexture();  // depth
		water_textures[4].textureID = reflection_framebuffer.getAttachedTex(0).getTexture();  // color
		water_textures[5].textureID = refraction_framebuffer.getAttachedTex(0).getTexture();  // color
	}
	
	void onDraw(IsParameters params){
		if(debug_terrain_rendering){
			// render the terrain to the main framebuffer directly.
			gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
			gl.glEnable(GLenum.GL_DEPTH_TEST);
			renderTerrain(params, true, false);
			return;
		}
		
		if(debug_sky){
			drawFullscreen(sky_texture);
			return;
		}
		
		// Stage 1. render the shadownmap first.
		if(params.g_Wireframe){
			gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
		}
		renderShadowMap(params);
		
		if(debug_shadowmapping){
			// render the shadow mapping to the main framebuffer.
			gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
			gl.glViewport(0, 0, backbufferWidth, backbufferHeight);
			gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
			gl.glDisable(GLenum.GL_DEPTH_TEST);

			gl.glActiveTexture(GLenum.GL_TEXTURE0);
			gl.glBindTexture(GLenum.GL_TEXTURE_2D, shadownmap_framebuffer.getAttachedTex(0).getTexture());
			gl.glBindSampler(0, IsSamplers.g_SamplerLinearClamp);
			
			shadowDebugProgram.enable();
			shadowDebugProgram.setUniforms(1, 1000, 0, 1);
//			shadowDebugProgram.applyLightNear(1);
			gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
			gl.glBindSampler(0, 0);
			return;
		}
		
		GLCheck.checkError();
		// Stage 2. render the caustics
		renderCaustics(params);
		if(debug_waternormalmap){
			drawFullscreen(water_normalmap_framebuffer.getAttachedTex(0).getTexture());
			return;
		}
		
		// Stage 3. render the reflection mapping.
		renderReflection(params);
		
		if(debug_reflection){
			drawFullscreen(reflection_framebuffer.getAttachedTex(0).getTexture());
			return;
		}
		
		// Stage 4. render to the main_color_framebuffer.
		params.g_HalfSpaceCullSign = 1.0f;
		params.g_HalfSpaceCullPosition=terrain_minheight*2;
		renderMainColor(params);
		GLCheck.checkError();
		
		if(params.g_Wireframe){
			gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
		}
		
		// Stage 5. getting back to rendering to default buffer
		int texture = params.g_showReflection ? refraction_framebuffer.getAttachedTex(0).getTexture() : main_color_framebuffer.getAttachedTex(0).getTexture();
		drawFullscreen(texture);
	}
	
	void renderMainColor(IsParameters params){
		main_color_framebuffer.bind();
		main_color_framebuffer.setViewPort();
		
		gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(clearColor));
		gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1.0f));
		
		// drawing terrain to main buffer
		params.g_TerrainBeingRendered = 1.0f;
		params.g_SkipCausticsCalculation = 0;
		renderTerrain(params, true, false);
		
		// resolving main buffer color to refraction color resource
		gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, refraction_framebuffer.getFramebuffer());
		gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, main_color_framebuffer.getFramebuffer());
		gl.glReadBuffer(GLenum.GL_COLOR_ATTACHMENT0);
		gl.glDrawBuffers(GLenum.GL_COLOR_ATTACHMENT0);
		gl.glBlitFramebuffer(0, 0, main_color_framebuffer.getWidth(), main_color_framebuffer.getHeight(),
				0, 0, refraction_framebuffer.getWidth(), refraction_framebuffer.getHeight(),
				GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT, GLenum.GL_NEAREST);

		gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, 0);
		
		main_color_framebuffer.bind();
		
		// drawing water surface to main buffer
		params.g_TerrainBeingRendered = 0;
		if(params.g_RenderWater)
			renderWater(params);
		
		//drawing sky to main buffer
		renderSky(params);
	}
	
	void renderWater(IsParameters params){
		waterRenderProgram.enable(params, water_textures);
		if(params.g_Wireframe)
			waterRenderProgram.setupColorPass();
		else
			waterRenderProgram.setupWaterPatchPass();
		
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glCullFace(GL11.GL_BACK);
		GLError.checkError();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, heightfield_vertexbuffer);
		GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, 0, 0);
		GL20.glEnableVertexAttribArray(0);
		
		GL40.glPatchParameteri(GL40.GL_PATCH_VERTICES, 1);
		GL11.glDrawArrays(GL40.GL_PATCHES, 0, terrain_numpatches_1d*terrain_numpatches_1d);
		
		// reset the state.
		waterRenderProgram.disable();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL20.glDisableVertexAttribArray(0);
		GL11.glDisable(GL11.GL_CULL_FACE);
	}
	
	void drawFullscreen(int textureId){
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		GL11.glViewport(0, 0, backbufferWidth, backbufferHeight);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
		GL33.glBindSampler(0, IsSamplers.g_SamplerLinearClamp);
		
		textureDebugProgram.enable();
		GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
		GL33.glBindSampler(0, 0);
	}
	
	void renderReflection(IsParameters params){
		reflection_framebuffer.enableRenderToColorAndDepth(0);
		reflection_framebuffer.setViewPort();
		
		GL30.glClearBufferfv(GL11.GL_COLOR, 0, GLUtil.wrap(refractionClearColor));
		GL30.glClearBufferfv(GL11.GL_DEPTH, 0, GLUtil.wrap(1.0f));
		
		setupReflectionView(params);
		
		// drawing sky to reflection RT
		renderSky(params);
		
		// drawing terrain to reflection RT
		params.g_SkipCausticsCalculation = 1;
		renderTerrain(params, false, false);
		
		endReflection(params);
	}
	
	void setupReflectionView(IsParameters params){
		// Save the old value
		camera_position.set(params.g_CameraPosition);
		camera_direction.set(params.g_CameraDirection);
		camera_modelView.load(params.g_ModelViewMatrix);
		camera_mvp.load(params.g_ModelViewProjectionMatrix);
		camera_projection.load(params.g_Projection);
		
		Vector3f eyePoint = params.g_CameraPosition;
	    Vector3f direction = params.g_CameraDirection;
	    
	    // make the camera below the water.
		eyePoint.y=-1.0f*eyePoint.y+1.0f;
//		lookAtPoint.y=-1.0f*lookAtPoint.y+1.0f;
		direction.y *= -1;
		
		Matrix4f modelView = params.g_ModelViewMatrix;
		Matrix4f viewProj = params.g_ModelViewProjectionMatrix;
		modelView.m01 *= -1;
		modelView.m21 *= -1;
		modelView.m12 *= -1;
		modelView.m31 = -modelView.m31 - 1;
		
		Vector4f plane = new Vector4f(0, 1, 0, 0.5f);
		Matrix4f mat = Matrix4f.invertRigid(modelView, null).transpose();
		// transform the plane to camera coordinates.
		Matrix4f.transform(mat, plane, plane);
		// construct the oblique projection with new near plane.
		Matrix4f.obliqueClipping(params.g_Projection, plane, params.g_Projection);
		Matrix4f.mul(params.g_Projection, modelView, viewProj);
		
		params.g_HalfSpaceCullSign = 1.0f;
		params.g_HalfSpaceCullPosition = -0.6f;
	}
	
	void endReflection(IsParameters params){
		params.g_CameraPosition.set(camera_position);
		params.g_CameraDirection.set(camera_direction);
		params.g_ModelViewMatrix.load(camera_modelView);
		params.g_ModelViewProjectionMatrix.load(camera_mvp);
		params.g_Projection.load(camera_projection);
	}
	
	void renderSky(IsParameters params){
		GLError.checkError();
		skyProgram.enable();
		skyProgram.setModelViewProjectionMatrix(params.g_ModelViewProjectionMatrix);
		GLError.checkError();
		if(params.g_Wireframe)
			skyProgram.setupColorPass();
		else
			skyProgram.setupSkyPass();
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, sky_texture);
		GL33.glBindSampler(0, IsSamplers.g_SamplerLinearWrap);
		int stride = 4 * 6;
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, sky_vertexbuffer);
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, stride, 0);
		GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 4 * 4);
		
		GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, sky_gridpoints*(sky_gridpoints+2)*2);
		
		skyProgram.disable();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GL33.glBindSampler(0, 0);
		
		GLError.checkError();
	}
	
	// 这个方法应该在IslandWater中调用
	void renderCaustics(IsParameters params){
		if(params.g_RenderCaustics){
			// selecting water_normalmap_resource rendertarget
			water_normalmap_framebuffer.enableRenderToColorAndDepth(0);
			water_normalmap_framebuffer.setViewPort();
			GL11.glDisable(GL11.GL_DEPTH_TEST);
//			GL11.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
//			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
			GL30.glClearBufferfv(GL11.GL_COLOR, 0, GLUtil.wrap(clearColor));
			GLError.checkError();
			
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, water_bump_texture);
			GL33.glBindSampler(0, IsSamplers.g_SamplerLinearWrap);
			//rendering water normalmap
//			setupNormalView(/*cam*/); // need it just to provide shader with camera position
			
			waterNormalmapCombineProgram.enable();
			GLError.checkError();
			waterNormalmapCombineProgram.applyCameraPosition(params.g_CameraPosition);
			waterNormalmapCombineProgram.applyWaterBumpTexcoordShift(params.g_WaterBumpTexcoordShift);
			GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
			GLError.checkError();
			waterNormalmapCombineProgram.disable();
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
			GL33.glBindSampler(0, 0);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GLError.checkError();
		}
	}
	
	void renderShadowMap(IsParameters params){
		shadownmap_framebuffer.enableRenderToColorAndDepth(0);
		shadownmap_framebuffer.setViewPort();
		
		GLError.checkError();
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glColorMask(false, false, false, false);  // disable color output.
		
		params.g_HalfSpaceCullSign = 1.0f;
		params.g_HalfSpaceCullPosition = terrain_minheight*2;
		params.g_TerrainBeingRendered = 1;
		params.g_SkipCausticsCalculation = 1;
		GLError.checkError();
		renderTerrain(params, false, true);
		GL11.glColorMask(true, true, true, true);
		GLError.checkError();
	}
	
	void renderTerrain(IsParameters params, boolean cullface, boolean shadow_map){
		
		renderHeightfieldProgram.enable(params, terrain_textures);
		GLError.checkError();
		if(params.g_Wireframe){
			renderHeightfieldProgram.setupColorPass();
		}else{
			renderHeightfieldProgram.setupRenderHeightFieldPass();
		}
		
		renderHeightfieldProgram.setRenderShadowmap(shadow_map);
		
		GLError.checkError();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, heightfield_vertexbuffer);
		GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, 0, 0);
		GL20.glEnableVertexAttribArray(0);
		if(cullface){
			GL11.glFrontFace(GL11.GL_CCW);
			GL11.glCullFace(GL11.GL_BACK);
		}else{
			GL11.glFrontFace(GL11.GL_CW);
			GL11.glCullFace(GL11.GL_FRONT);
		}
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL40.glPatchParameteri(GL40.GL_PATCH_VERTICES, 1);
		GL11.glDrawArrays(GL40.GL_PATCHES, 0, terrain_numpatches_1d*terrain_numpatches_1d);
		
		// reset the state.
		renderHeightfieldProgram.disable();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL20.glDisableVertexAttribArray(0);
		
			GL11.glDisable(GL11.GL_CULL_FACE);
			if(cullface)
				GL11.glFrontFace(GL11.GL_CCW);
	}
	
	void releaseFrameBuffer(){
		if(main_color_framebuffer != null){
			main_color_framebuffer.dispose();
			main_color_framebuffer = null;
		}
		
		if(reflection_framebuffer != null){
			reflection_framebuffer.dispose();
			reflection_framebuffer = null;
		}
		
		if(refraction_framebuffer != null){
			refraction_framebuffer.dispose();
			refraction_framebuffer = null;
		}
		
		if(shadownmap_framebuffer != null){
			shadownmap_framebuffer.dispose();
			shadownmap_framebuffer = null;
		}
		
		if(water_normalmap_framebuffer != null){
			water_normalmap_framebuffer.dispose();
			water_normalmap_framebuffer = null;
		}
	}
	
	void onDestroy(){
		releaseFrameBuffer();
	}
	
	public static void main(String[] args) {
		Vector3f u = new Vector3f(2,1,3);
		Vector3f v = new Vector3f(2, 0,0);
		
		System.out.println("u x v = " + Vector3f.cross(u, v, null));
		System.out.println("v x u = " + Vector3f.cross(v, u, null));
		
		System.out.println(0x83f3);
	}
	
	void loadTextures(String prefix)throws IOException{
		NvImage.upperLeftOrigin(false);
		rock_bump_texture = NvImage.uploadTextureFromDDSFile(prefix + "rock_bump6.dds");
		printFormat("rock_bump6.dds", rock_bump_texture);
		GLError.checkError();
		
		rock_diffuse_texture = NvImage.uploadTextureFromDDSFile(prefix + "terrain_rock4.dds");
		printFormat("terrain_rock4.dds", rock_diffuse_texture);
		GLError.checkError();
		
		sand_diffuse_texture = NvImage.uploadTextureFromDDSFile(prefix + "sand_diffuse.dds");
		printFormat("sand_diffuse.dds", sand_diffuse_texture);
		GLError.checkError();
		
		sand_bump_texture = NvImage.uploadTextureFromDDSFile(prefix + "rock_bump4.dds");
		printFormat("rock_bump4.dds", sand_bump_texture);
		GLError.checkError();
		
		grass_diffuse_texture = NvImage.uploadTextureFromDDSFile(prefix + "terrain_grass.dds");
		printFormat("terrain_grass.dds", grass_diffuse_texture);
		GLError.checkError();
		
		slope_diffuse_texture = NvImage.uploadTextureFromDDSFile(prefix + "terrain_slope.dds");
		printFormat("terrain_slope.dds", slope_diffuse_texture);
		GLError.checkError();
		
		sand_microbump_texture =NvImage.uploadTextureFromDDSFile(prefix + "lichen1_normal.dds");
		printFormat("lichen1_normal.dds", sand_microbump_texture);
		GLError.checkError();
		
		rock_microbump_texture = NvImage.uploadTextureFromDDSFile(prefix + "rock_bump4.dds");
		printFormat("rock_bump4.dds", rock_microbump_texture);
		GLError.checkError();   //TODO rock_microbump_texture is equal to the sand_bump_texture
		
		water_bump_texture = NvImage.uploadTextureFromDDSFile(prefix + "water_bump.dds");
		printFormat("water_bump.dds", water_bump_texture);
		GLError.checkError();
		
		sky_texture = NvImage.uploadTextureFromDDSFile(prefix + "sky.dds");
		GLError.checkError();
		
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}
	
	void printFormat(String name, int textureID){
		if(textureID == 0){
			System.err.println(name + "'s textureID is 0.");
			return;
		}
		
		int internalFormat = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
		System.out.println(name + " format = " + TextureUtils.getFormatName(internalFormat));
	}
	
	private void createTerrain(){
		int i,j,k,l;
		float x,z;
		int ix,iz;
		float[] backterrain;
//	    D3DXVECTOR3 vec1,vec2,vec3;
		final Vector3f vec1 = new Vector3f();
		final Vector3f vec2 = new Vector3f();
		final Vector3f vec3 = new Vector3f();
		int currentstep=terrain_gridpoints;
		float mv,rm;
		float yscale=0,maxheight=0,minheight=0;

		float []height_linear_array;
		float []patches_rawdata;
		
		float[][]			height = new float[terrain_gridpoints+1][terrain_gridpoints+1];
		float[][]           normal = new float[terrain_gridpoints+1][(terrain_gridpoints+1) * 3];
		
//		HRESULT result;
//		D3D11_SUBRESOURCE_DATA subresource_data;
//		D3D11_TEXTURE2D_DESC tex_desc;
//		D3D11_SHADER_RESOURCE_VIEW_DESC textureSRV_desc; 

//		backterrain = (float *) malloc((terrain_gridpoints+1)*(terrain_gridpoints+1)*sizeof(float));
		backterrain = new float[(terrain_gridpoints+1)*(terrain_gridpoints+1)];
		rm=terrain_fractalinitialvalue;
		backterrain[0]=0;
		backterrain[0+terrain_gridpoints*terrain_gridpoints]=0;
		backterrain[terrain_gridpoints]=0;
		backterrain[terrain_gridpoints+terrain_gridpoints*terrain_gridpoints]=0;
	    currentstep=terrain_gridpoints;
//		srand(12);
		
		// generating fractal terrain using square-diamond method
		while (currentstep>1)
		{
			//square step;
			i=0;
			j=0;


			while (i<terrain_gridpoints)
			{
				j=0;
				while (j<terrain_gridpoints)
				{
					
					mv=backterrain[i+terrain_gridpoints*j];
					mv+=backterrain[(i+currentstep)+terrain_gridpoints*j];
					mv+=backterrain[(i+currentstep)+terrain_gridpoints*(j+currentstep)];
					mv+=backterrain[i+terrain_gridpoints*(j+currentstep)];
					mv/=4.0;
					backterrain[i+currentstep/2+terrain_gridpoints*(j+currentstep/2)]=(float)(mv+rm*(/*(rand()%1000)/1000.0f*/ random.nextFloat() -0.5f));
					j+=currentstep;
				}
			i+=currentstep;
			}

			//diamond step;
			i=0;
			j=0;

			while (i<terrain_gridpoints)
			{
				j=0;
				while (j<terrain_gridpoints)
				{

					mv=0;
					mv=backterrain[i+terrain_gridpoints*j];
					mv+=backterrain[(i+currentstep)+terrain_gridpoints*j];
					mv+=backterrain[(i+currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
					mv+=backterrain[i+currentstep/2+terrain_gridpoints*gp_wrap(j-currentstep/2)];
					mv/=4;
					backterrain[i+currentstep/2+terrain_gridpoints*j]=(float)(mv+rm*(/*(rand()%1000)/1000.0f*/random.nextFloat()-0.5f));

					mv=0;
					mv=backterrain[i+terrain_gridpoints*j];
					mv+=backterrain[i+terrain_gridpoints*(j+currentstep)];
					mv+=backterrain[(i+currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
					mv+=backterrain[gp_wrap(i-currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
					mv/=4;
					backterrain[i+terrain_gridpoints*(j+currentstep/2)]=(float)(mv+rm*(/*(rand()%1000)/1000.0f*/random.nextFloat()-0.5f));

					mv=0;
					mv=backterrain[i+currentstep+terrain_gridpoints*j];
					mv+=backterrain[i+currentstep+terrain_gridpoints*(j+currentstep)];
					mv+=backterrain[(i+currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
					mv+=backterrain[gp_wrap(i+currentstep/2+currentstep)+terrain_gridpoints*(j+currentstep/2)];
					mv/=4;
					backterrain[i+currentstep+terrain_gridpoints*(j+currentstep/2)]=(float)(mv+rm*(/*(rand()%1000)/1000.0f*/random.nextFloat()-0.5f));

					mv=0;
					mv=backterrain[i+currentstep+terrain_gridpoints*(j+currentstep)];
					mv+=backterrain[i+terrain_gridpoints*(j+currentstep)];
					mv+=backterrain[(i+currentstep/2)+terrain_gridpoints*(j+currentstep/2)];
					mv+=backterrain[i+currentstep/2+terrain_gridpoints*gp_wrap(j+currentstep/2+currentstep)];
					mv/=4;
					backterrain[i+currentstep/2+terrain_gridpoints*(j+currentstep)]=(float)(mv+rm*(/*(rand()%1000)/1000.0f*/random.nextFloat()-0.5f));
					j+=currentstep;
				}
				i+=currentstep;
			}
			//changing current step;
			currentstep/=2;
	        rm*=terrain_fractalfactor;
		}	

		// scaling to minheight..maxheight range
		for (i=0;i<terrain_gridpoints+1;i++)
			for (j=0;j<terrain_gridpoints+1;j++)
			{
				height[i][j]=backterrain[i+terrain_gridpoints*j];
			}
		maxheight=height[0][0];
		minheight=height[0][0];
		for(i=0;i<terrain_gridpoints+1;i++)
			for(j=0;j<terrain_gridpoints+1;j++)
			{
				if(height[i][j]>maxheight) maxheight=height[i][j];
				if(height[i][j]<minheight) minheight=height[i][j];
			}
//		offset=minheight-terrain_minheight;
		yscale=(terrain_maxheight-terrain_minheight)/(maxheight-minheight);

		for(i=0;i<terrain_gridpoints+1;i++)
			for(j=0;j<terrain_gridpoints+1;j++)
			{
				height[i][j]-=minheight;
				height[i][j]*=yscale;
				height[i][j]+=terrain_minheight;
			}

		// moving down edges of heightmap	
		for (i=0;i<terrain_gridpoints+1;i++)
			for (j=0;j<terrain_gridpoints+1;j++)
			{
				mv=(float)((i-terrain_gridpoints/2.0f)*(i-terrain_gridpoints/2.0f)+(j-terrain_gridpoints/2.0f)*(j-terrain_gridpoints/2.0f));
				rm=(float)((terrain_gridpoints*0.8f)*(terrain_gridpoints*0.8f)/4.0f);
				if(mv>rm)
				{
					height[i][j]-=((mv-rm)/1000.0f)*terrain_geometry_scale;
				}
				if(height[i][j]<terrain_minheight)
				{
					height[i][j]=terrain_minheight;
				}
			}	


		// terrain banks
		for(k=0;k<10;k++)
		{	
			for(i=0;i<terrain_gridpoints+1;i++)
				for(j=0;j<terrain_gridpoints+1;j++)
				{
					mv=height[i][j];
					if((mv)>0.02f) 
					{
						mv-=0.02f;
					}
					if(mv<-0.02f) 
					{
						mv+=0.02f;
					}
					height[i][j]=mv;
				}
		}

		// smoothing 
		for(k=0;k<terrain_smoothsteps;k++)
		{	
			for(i=0;i<terrain_gridpoints+1;i++)
				for(j=0;j<terrain_gridpoints+1;j++)
				{

					vec1.x=2*terrain_geometry_scale;
					vec1.y=terrain_geometry_scale*(height[gp_wrap(i+1)][j]-height[gp_wrap(i-1)][j]);
					vec1.z=0;
					vec2.x=0;
					vec2.y=-terrain_geometry_scale*(height[i][gp_wrap(j+1)]-height[i][gp_wrap(j-1)]);
					vec2.z=-2*terrain_geometry_scale;

//					D3DXVec3Cross(&vec3,&vec1,&vec2);
//					D3DXVec3Normalize(&vec3,&vec3);
					
					Vector3f.cross(vec1, vec2, vec3);
					vec3.normalise();
					
//					vec3.x = terrain_geometry_scale*(height[gp_wrap(i+1)][j]-height[gp_wrap(i-1)][j]);
//					vec3.y = 2;
//					vec3.z=terrain_geometry_scale*(height[i][gp_wrap(j+1)]-height[i][gp_wrap(j-1)]);
//					vec3.normalise();
					
					if(((vec3.y>terrain_rockfactor)||(height[i][j]<1.2f))) 
					{
						rm=terrain_smoothfactor1;
						mv=height[i][j]*(1.0f-rm) +rm*0.25f*(height[gp_wrap(i-1)][j]+height[i][gp_wrap(j-1)]+height[gp_wrap(i+1)][j]+height[i][gp_wrap(j+1)]);
						backterrain[i+terrain_gridpoints*j]=mv;
					}
					else
					{
						rm=terrain_smoothfactor2;
						mv=height[i][j]*(1.0f-rm) +rm*0.25f*(height[gp_wrap(i-1)][j]+height[i][gp_wrap(j-1)]+height[gp_wrap(i+1)][j]+height[i][gp_wrap(j+1)]);
						backterrain[i+terrain_gridpoints*j]=mv;
					}

				}
				for (i=0;i<terrain_gridpoints+1;i++)
					for (j=0;j<terrain_gridpoints+1;j++)
					{
						height[i][j]=(backterrain[i+terrain_gridpoints*j]);
					}
		}
		for(i=0;i<terrain_gridpoints+1;i++)
			for(j=0;j<terrain_gridpoints+1;j++)
			{
				rm=0.5f;
				mv=height[i][j]*(1.0f-rm) +rm*0.25f*(height[gp_wrap(i-1)][j]+height[i][gp_wrap(j-1)]+height[gp_wrap(i+1)][j]+height[i][gp_wrap(j+1)]);
				backterrain[i+terrain_gridpoints*j]=mv;
			}
		for (i=0;i<terrain_gridpoints+1;i++)
			for (j=0;j<terrain_gridpoints+1;j++)
			{
				height[i][j]=(backterrain[i+terrain_gridpoints*j]);
			}


//		free(backterrain);
		backterrain = null;

		//calculating normals
		for (i=0;i<terrain_gridpoints+1;i++)
			for (j=0;j<terrain_gridpoints+1;j++)
			{
				vec1.x=2*terrain_geometry_scale;
				vec1.y=terrain_geometry_scale*(height[gp_wrap(i+1)][j]-height[gp_wrap(i-1)][j]);
				vec1.z=0;
				vec2.x=0;
				vec2.y=-terrain_geometry_scale*(height[i][gp_wrap(j+1)]-height[i][gp_wrap(j-1)]);
				vec2.z=-2*terrain_geometry_scale;
//				D3DXVec3Cross(&normal[i][j],&vec1,&vec2);
//				D3DXVec3Normalize(&normal[i][j],&normal[i][j]);
				
				Vector3f.cross(vec1, vec2, vec3);
				
//				vec3.x = terrain_geometry_scale*(height[gp_wrap(i+1)][j]-height[gp_wrap(i-1)][j]);
//				vec3.y = 2 * terrain_geometry_scale;
//				vec3.z=terrain_geometry_scale*(height[i][gp_wrap(j+1)]-height[i][gp_wrap(j-1)]);
				vec3.normalise();
				vec3.store(normal[i], j * 3);
			}


		// buiding layerdef 
//		byte* temp_layerdef_map_texture_pixels=(byte *)malloc(terrain_layerdef_map_texture_size*terrain_layerdef_map_texture_size*4);
//		byte* layerdef_map_texture_pixels=(byte *)malloc(terrain_layerdef_map_texture_size*terrain_layerdef_map_texture_size*4);
		byte[] temp_layerdef_map_texture_pixels = new byte[terrain_layerdef_map_texture_size*terrain_layerdef_map_texture_size*4];
		byte[] layerdef_map_texture_pixels = new byte[terrain_layerdef_map_texture_size*terrain_layerdef_map_texture_size*4];
		
		for(i=0;i<terrain_layerdef_map_texture_size;i++)
			for(j=0;j<terrain_layerdef_map_texture_size;j++)
			{
				x=(float)(terrain_gridpoints)*((float)i/(float)terrain_layerdef_map_texture_size);
				z=(float)(terrain_gridpoints)*((float)j/(float)terrain_layerdef_map_texture_size);
				ix=(int)Math.floor(x);
				iz=(int)Math.floor(z);
				rm=bilinear_interpolation(x-ix,z-iz,height[ix][iz],height[ix+1][iz],height[ix+1][iz+1],height[ix][iz+1])*terrain_geometry_scale;
				
				temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
				temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
				temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
				temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;

				if((rm>terrain_height_underwater_start)&&(rm<=terrain_height_underwater_end))
				{
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=-1;
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;
				}

				if((rm>terrain_height_sand_start)&&(rm<=terrain_height_sand_end))
				{
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=-1;
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;
				}

				if((rm>terrain_height_grass_start)&&(rm<=terrain_height_grass_end))
				{
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=-1;
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;
				}

//				mv=bilinear_interpolation(x-ix,z-iz,normal[ix][iz][1],normal[ix+1][iz][1],normal[ix+1][iz+1][1],normal[ix][iz+1][1]);
				mv=bilinear_interpolation(x-ix,z-iz,normal[ix][iz*3+1],normal[ix+1][iz*3+1],normal[ix+1][(iz+1)*3+1],normal[ix][(iz+1)*3+1]);

				if((mv<terrain_slope_grass_start)&&(rm>terrain_height_sand_end))
				{
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=0;
				}

				if((mv<terrain_slope_rocks_start)&&(rm>terrain_height_rocks_start))
				{
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=0;
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=0;
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=0;
					temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=-1;
				}

			}
		for(i=0;i<terrain_layerdef_map_texture_size;i++)
			for(j=0;j<terrain_layerdef_map_texture_size;j++)
			{
				layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0];
				layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1];
				layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2];
				layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=temp_layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3];
			}


		for(i=2;i<terrain_layerdef_map_texture_size-2;i++)
			for(j=2;j<terrain_layerdef_map_texture_size-2;j++)
			{
				int n1=0;
				int n2=0;
				int n3=0;
				int n4=0;
				for(k=-2;k<3;k++)
					for(l=-2;l<3;l++)
						{
								n1+=temp_layerdef_map_texture_pixels[((j+k)*terrain_layerdef_map_texture_size+i+l)*4+0];
								n2+=temp_layerdef_map_texture_pixels[((j+k)*terrain_layerdef_map_texture_size+i+l)*4+1];
								n3+=temp_layerdef_map_texture_pixels[((j+k)*terrain_layerdef_map_texture_size+i+l)*4+2];
								n4+=temp_layerdef_map_texture_pixels[((j+k)*terrain_layerdef_map_texture_size+i+l)*4+3];
						}
				layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+0]=(byte)(n1/25);
				layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+1]=(byte)(n2/25);
				layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+2]=(byte)(n3/25);
				layerdef_map_texture_pixels[(j*terrain_layerdef_map_texture_size+i)*4+3]=(byte)(n4/25);
			}
		
		TextureData tex_desc = new TextureData();
		tex_desc.width = terrain_layerdef_map_texture_size;
		tex_desc.height = terrain_layerdef_map_texture_size;
		tex_desc.genMipmap = false;
		tex_desc.target = GL11.GL_TEXTURE_2D;
		tex_desc.internalFormat = GL11.GL_RGBA8;
		tex_desc.pixels = GLUtil.wrap(layerdef_map_texture_pixels);
		layerdef_texture = TextureUtils.createTexture(tex_desc);
		
		temp_layerdef_map_texture_pixels = null;
		layerdef_map_texture_pixels = null;
		
		height_linear_array = new float [terrain_gridpoints*terrain_gridpoints*4];
		patches_rawdata = new float [terrain_numpatches_1d*terrain_numpatches_1d*4];

		for(i=0;i<terrain_gridpoints;i++)
			for(j=0; j<terrain_gridpoints;j++)
			{
				height_linear_array[(i+j*terrain_gridpoints)*4+0]=normal[i][j*3+0];
				height_linear_array[(i+j*terrain_gridpoints)*4+1]=normal[i][j*3+1];
				height_linear_array[(i+j*terrain_gridpoints)*4+2]=normal[i][j*3+2];
				height_linear_array[(i+j*terrain_gridpoints)*4+3]=height[i][j];
			}
		
		tex_desc.width = terrain_gridpoints;
		tex_desc.height = terrain_gridpoints;
		tex_desc.internalFormat = GL30.GL_RGBA32F;
		tex_desc.pixels = GLUtil.wrapToBytes(height_linear_array);
		
		heightmap_texture = TextureUtils.createTexture(tex_desc);
		height_linear_array = null;
		
		//building depthmap
//		byte * depth_shadow_map_texture_pixels=(byte *)malloc(terrain_depth_shadow_map_texture_size*terrain_depth_shadow_map_texture_size*4);
		byte[] depth_shadow_map_texture_pixels = new byte[terrain_depth_shadow_map_texture_size*terrain_depth_shadow_map_texture_size*4];
		for(i=0;i<terrain_depth_shadow_map_texture_size;i++)
			for(j=0;j<terrain_depth_shadow_map_texture_size;j++)
			{
				x=(float)(terrain_gridpoints)*((float)i/(float)terrain_depth_shadow_map_texture_size);
				z=(float)(terrain_gridpoints)*((float)j/(float)terrain_depth_shadow_map_texture_size);
				ix=(int)Math.floor(x);
				iz=(int)Math.floor(z);
				rm=bilinear_interpolation(x-ix,z-iz,height[ix][iz],height[ix+1][iz],height[ix+1][iz+1],height[ix][iz+1])*terrain_geometry_scale;
				
				if(rm>0)
				{
					depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+0]=0;
					depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+1]=0;
					depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+2]=0;
				}
				else
				{
					float no=(1.0f*255.0f*(rm/(terrain_minheight*terrain_geometry_scale)))-1.0f;
					if(no>255) no=255;
					if(no<0) no=0;
					depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+0]=(byte)no;
					
					no=(10.0f*255.0f*(rm/(terrain_minheight*terrain_geometry_scale)))-40.0f;
					if(no>255) no=255;
					if(no<0) no=0;
					depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+1]=(byte)no;

					no=(100.0f*255.0f*(rm/(terrain_minheight*terrain_geometry_scale)))-300.0f;
					if(no>255) no=255;
					if(no<0) no=0;
					depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+2]=(byte)no;
				}
				depth_shadow_map_texture_pixels[(j*terrain_depth_shadow_map_texture_size+i)*4+3]=0;
			}
		
		tex_desc.width = terrain_depth_shadow_map_texture_size;
		tex_desc.height = terrain_depth_shadow_map_texture_size;
		tex_desc.internalFormat = GL11.GL_RGBA8;
		tex_desc.pixels = GLUtil.wrap(depth_shadow_map_texture_pixels);
		
		depthmap_texture = TextureUtils.createTexture(tex_desc);
		depth_shadow_map_texture_pixels = null;
		
		// creating terrain vertex buffer
		for(i=0;i<terrain_numpatches_1d;i++)
			for(j=0;j<terrain_numpatches_1d;j++)
			{
				patches_rawdata[(i+j*terrain_numpatches_1d)*4+0]=i*terrain_geometry_scale*terrain_gridpoints/terrain_numpatches_1d;
				patches_rawdata[(i+j*terrain_numpatches_1d)*4+1]=j*terrain_geometry_scale*terrain_gridpoints/terrain_numpatches_1d;
				patches_rawdata[(i+j*terrain_numpatches_1d)*4+2]=terrain_geometry_scale*terrain_gridpoints/terrain_numpatches_1d;
				patches_rawdata[(i+j*terrain_numpatches_1d)*4+3]=terrain_geometry_scale*terrain_gridpoints/terrain_numpatches_1d;
			}
		
		heightfield_vertexbuffer = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, heightfield_vertexbuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, GLUtil.wrap(patches_rawdata), GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		
		patches_rawdata = null;
		
		// creating sky vertex buffer
		float[] sky_vertexdata;
		int floatnum;
		sky_vertexdata = new float [sky_gridpoints*(sky_gridpoints+2)*2*6];
		float PI = (float) Math.PI;
		for(j=0;j<sky_gridpoints;j++)
		{
			
			i=0;
			floatnum=(j*(sky_gridpoints+2)*2)*6;
			sky_vertexdata[floatnum+0]=terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*cos(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)j/(float)sky_gridpoints);
			sky_vertexdata[floatnum+1]=4000.0f*sin(-0.5f*PI+PI*(float)(j)/(float)sky_gridpoints);
			sky_vertexdata[floatnum+2]=terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*sin(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)j/(float)sky_gridpoints);
			sky_vertexdata[floatnum+3]=1;
			sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
			sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)j/(float)sky_gridpoints;
			floatnum+=6;
			for(i=0;i<sky_gridpoints+1;i++)
			{
				sky_vertexdata[floatnum+0]=terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*cos(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)j/(float)sky_gridpoints);
				sky_vertexdata[floatnum+1]=4000.0f*sin(-0.5f*PI+PI*(float)(j)/(float)sky_gridpoints);
				sky_vertexdata[floatnum+2]=terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*sin(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)j/(float)sky_gridpoints);
				sky_vertexdata[floatnum+3]=1;
				sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
				sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)j/(float)sky_gridpoints;
				floatnum+=6;
				sky_vertexdata[floatnum+0]=terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*cos(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)(j+1)/(float)sky_gridpoints);
				sky_vertexdata[floatnum+1]=4000.0f*sin(-0.5f*PI+PI*(float)(j+1)/(float)sky_gridpoints);
				sky_vertexdata[floatnum+2]=terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*sin(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)(j+1)/(float)sky_gridpoints);
				sky_vertexdata[floatnum+3]=1;
				sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
				sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)(j+1)/(float)sky_gridpoints;
				floatnum+=6;
			}
			i=sky_gridpoints;
			sky_vertexdata[floatnum+0]=terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*cos(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)(j+1)/(float)sky_gridpoints);
			sky_vertexdata[floatnum+1]=4000.0f*sin(-0.5f*PI+PI*(float)(j+1)/(float)sky_gridpoints);
			sky_vertexdata[floatnum+2]=terrain_gridpoints*terrain_geometry_scale*0.5f+4000.0f*sin(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)(j+1)/(float)sky_gridpoints);
			sky_vertexdata[floatnum+3]=1;
			sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
			sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)(j+1)/(float)sky_gridpoints;
			floatnum+=6;
		}
		
		sky_vertexbuffer = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, sky_vertexbuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, GLUtil.wrap(sky_vertexdata), GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
	}
	
	static float sin(float angle) { return (float)Math.sin(angle);}
	static float cos(float angle) { return (float)Math.cos(angle);}
	
	int gp_wrap( int a)
	{
		if(a<0) return (a+terrain_gridpoints);
		if(a>=terrain_gridpoints) return (a-terrain_gridpoints);
		return a;
	}
	
	float bilinear_interpolation(float fx, float fy, float a, float b, float c, float d)
	{
		float s1,s2,s3,s4;
		s1=fx*fy;
		s2=(1-fx)*fy;
		s3=(1-fx)*(1-fy);
		s4=fx*(1-fy);
		return((a*s3+b*s4+c*s1+d*s2));
	}
}
