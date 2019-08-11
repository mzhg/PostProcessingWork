package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.shader.VisualDepthTextureProgram;
import jet.opengl.postprocessing.texture.FramebufferGL;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.NvImage;

/**
 * Created by mazhen'gui on 2017/8/2.
 */

final class CTerrainIsLand {
    static final float main_buffer_size_multiplier = 1.1f;
    static final float reflection_buffer_size_multiplier = 1.1f;
    static final float refraction_buffer_size_multiplier = 1.1f;
    static final float scene_z_near = 1.0f;
    static final float scene_z_far = 25000.0f;
    static final float camera_fov = 110.0f;

    static final int sky_gridpoints = 10;
    static final float sky_texture_angle = 0.425f;
    static final int water_normalmap_resource_buffer_size_xy = 2048;
    static final int shadowmap_resource_buffer_size_xy = 2048;

    Texture2D rock_bump_texture;
    Texture2D rock_microbump_texture;
    Texture2D rock_diffuse_texture;
    Texture2D sand_bump_texture;
    Texture2D sand_microbump_texture;
    Texture2D sand_diffuse_texture;
    Texture2D grass_diffuse_texture;
    Texture2D slope_diffuse_texture;
    Texture2D sky_texture;
    Texture2D water_bump_texture;

    FramebufferGL reflection_framebuffer;
    FramebufferGL   refraction_framebuffer;
    FramebufferGL   shadownmap_framebuffer;
    FramebufferGL   water_normalmap_framebuffer;
    FramebufferGL   main_color_framebuffer;

    int backbufferWidth, backbufferHeight;

    float[] clearColor = {0.8f, 0.8f, 1.0f, 1.0f};
    float[] refractionClearColor = {0.5f, 0.5f, 0.5f, 1.0f};

    final Vector3f camera_position = new Vector3f();
    final Vector3f camera_direction = new Vector3f();
    final Matrix4f camera_modelView = new Matrix4f();
    final Matrix4f camera_mvp = new Matrix4f();
    final Matrix4f camera_projection = new Matrix4f();

    TextureSampler[] terrain_textures;

    IsRenderHeightfieldProgram renderHeightfieldProgram;
    IsWaterNormalmapCombineProgram waterNormalmapCombineProgram;
    IsMainToBackBufferProgram mainToBackBufferProgram;

    VisualDepthTextureProgram shadowDebugProgram;
    FullscreenProgram textureDebugProgram;

    private CTerrainGenerator m_TerrainVB;
    private SkyRoundGenerator m_SkyVB;
    private SkyRoundRenderer m_SkyRenderer;
    private IsWaterRenderer m_WaterRenderer;

    private GLFuncProvider gl;

    void onCreate(String shaderPath, String texturePath){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        try {
            loadTextures(texturePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        m_TerrainVB = new CTerrainGenerator();
        m_TerrainVB.initlize();

        m_SkyVB = new SkyRoundGenerator();
        m_SkyVB.initlize(sky_gridpoints, sky_texture_angle, m_TerrainVB.getTerrainParams().terrain_geometry_scale * m_TerrainVB.getTerrainParams().terrain_gridpoints);

        m_SkyRenderer = new SkyRoundRenderer();
        m_SkyRenderer.initlize(m_SkyVB, sky_texture);

        m_WaterRenderer = new IsWaterRenderer();
        m_WaterRenderer.initlize(m_TerrainVB, m_TerrainVB.getDepthmapTexture());

        GLCheck.checkError();
        renderHeightfieldProgram = new IsRenderHeightfieldProgram(/*null,*/ shaderPath);
        waterNormalmapCombineProgram = new IsWaterNormalmapCombineProgram(shaderPath);
        mainToBackBufferProgram = new IsMainToBackBufferProgram(shaderPath);GLCheck.checkError();

        try {
            shadowDebugProgram = new VisualDepthTextureProgram(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        GLCheck.checkError();
        textureDebugProgram = new FullscreenProgram();
        terrain_textures = new TextureSampler[12];
        terrain_textures[0] = new TextureSampler(m_TerrainVB.getHeightmapTexture().getTexture(), IsSamplers.g_SamplerLinearWrap);
        terrain_textures[1] = new TextureSampler(m_TerrainVB.getLayerdefTexture().getTexture(), IsSamplers.g_SamplerLinearWrap);
        terrain_textures[2] = new TextureSampler(sand_bump_texture.getTexture(), IsSamplers.g_SamplerLinearMipmapWrap);
        terrain_textures[3] = new TextureSampler(rock_bump_texture.getTexture(), IsSamplers.g_SamplerLinearMipmapWrap);
        terrain_textures[4] = new TextureSampler(0, IsSamplers.g_SamplerLinearWrap);
        terrain_textures[5] = new TextureSampler(sand_microbump_texture.getTexture(), IsSamplers.g_SamplerAnisotropicWrap);
        terrain_textures[6] = new TextureSampler(rock_microbump_texture.getTexture(), IsSamplers.g_SamplerAnisotropicWrap);
        terrain_textures[7] = new TextureSampler(slope_diffuse_texture.getTexture(), IsSamplers.g_SamplerAnisotropicWrap);
        terrain_textures[8] = new TextureSampler(sand_diffuse_texture.getTexture(), IsSamplers.g_SamplerAnisotropicWrap);
        terrain_textures[9] = new TextureSampler(rock_diffuse_texture.getTexture(), IsSamplers.g_SamplerAnisotropicWrap);
        terrain_textures[10] = new TextureSampler(grass_diffuse_texture.getTexture(), IsSamplers.g_SamplerAnisotropicWrap);
        terrain_textures[11] = new TextureSampler(0, IsSamplers.g_SamplerDepthAnisotropic);
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
        water_normalmap_framebuffer.bind();
        water_normalmap_framebuffer.addTexture2D(new Texture2DDesc(water_normalmap_resource_buffer_size_xy, water_normalmap_resource_buffer_size_xy, GLenum.GL_RGB8), default_desc);

//		builder.setWidth(shadowmap_resource_buffer_size_xy);
//		builder.setHeight(shadowmap_resource_buffer_size_xy);
//		depth_desc.setInternalFormat(FramebufferGL.FBO_DepthBufferType_TEXTURE_DEPTH32F);
//		builder.getColorTextures().clear();
        shadownmap_framebuffer = new FramebufferGL(/*builder*/);
        shadownmap_framebuffer.bind();
        shadownmap_framebuffer.addTexture2D(new Texture2DDesc(shadowmap_resource_buffer_size_xy,shadowmap_resource_buffer_size_xy, GLenum.GL_DEPTH_COMPONENT32F), default_desc);

        terrain_textures[4].textureID = water_normalmap_framebuffer.getAttachedTex(0).getTexture();  // color
        terrain_textures[11].textureID = shadownmap_framebuffer.getAttachedTex(0).getTexture();   // depth

        m_WaterRenderer.setWaterBump(water_bump_texture);
        m_WaterRenderer.setShadowMap((Texture2D) shadownmap_framebuffer.getAttachedTex(0));
        m_WaterRenderer.setRefractionDepth((Texture2D) refraction_framebuffer.getAttachedTex(1));
        m_WaterRenderer.setRefractionColor((Texture2D) refraction_framebuffer.getAttachedTex(0));
        m_WaterRenderer.setReflectionColor((Texture2D) reflection_framebuffer.getAttachedTex(0));
    }

    void onDraw(IsParameters params){
//        if(debug_terrain_rendering){
//            // render the terrain to the main framebuffer directly.
//            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
//            gl.glEnable(GLenum.GL_DEPTH_TEST);
//            renderTerrain(params, true, false);
//            return;
//        }
//
//        if(debug_sky){
//            drawFullscreen(sky_texture);
//            return;
//        }

        // Stage 1. render the shadownmap first.
        if(params.g_Wireframe){
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
        }

        GLCheck.checkError();
        renderShadowMap(params);

//        if(debug_shadowmapping){
//            // render the shadow mapping to the main framebuffer.
//            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
//            gl.glViewport(0, 0, backbufferWidth, backbufferHeight);
//            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
//            gl.glDisable(GLenum.GL_DEPTH_TEST);
//
//            gl.glActiveTexture(GLenum.GL_TEXTURE0);
//            gl.glBindTexture(GLenum.GL_TEXTURE_2D, shadownmap_framebuffer.getAttachedTex(0).getTexture());
//            gl.glBindSampler(0, IsSamplers.g_SamplerLinearClamp);
//
//            shadowDebugProgram.enable();
//            shadowDebugProgram.setUniforms(1, 1000, 0, 1);
////			shadowDebugProgram.applyLightNear(1);
//            gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
//            gl.glBindSampler(0, 0);
//            return;
//        }

        GLCheck.checkError();
        // Stage 2. render the caustics
        renderCaustics(params);
//        if(debug_waternormalmap){
//            drawFullscreen(water_normalmap_framebuffer.getAttachedTex(0).getTexture());
//            return;
//        }

        // Stage 3. render the reflection mapping.
        renderReflection(params);

//        if(debug_reflection){
//            drawFullscreen(reflection_framebuffer.getAttachedTex(0).getTexture());
//            return;
//        }

        // Stage 4. render to the main_color_framebuffer.
        float terrain_minheight=m_TerrainVB.getTerrainParams().terrain_minheight;
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

        // rebind the main_framebuffer.
        main_color_framebuffer.bind();

        // drawing water surface to main buffer
        params.g_TerrainBeingRendered = 0;
        if(params.g_RenderWater)
            renderWater(params);

        //drawing sky to main buffer
        renderSky(params);
    }

    void renderWater(IsParameters params){
        m_WaterRenderer.render(params);
    }

    void drawFullscreen(int textureId){
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, backbufferWidth, backbufferHeight);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
        gl.glDisable(GLenum.GL_DEPTH_TEST);

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, textureId);
        gl.glBindSampler(0, IsSamplers.g_SamplerLinearClamp);

        textureDebugProgram.enable();
        gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
        gl.glBindSampler(0, 0);
    }

    void renderReflection(IsParameters params){
        reflection_framebuffer.bind();
        reflection_framebuffer.setViewPort();

        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(refractionClearColor));
        gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1.0f));

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
        m_SkyRenderer.render(params.g_ModelViewProjectionMatrix, params.g_Wireframe);
    }

    // The method should called in class of IslandWater
    void renderCaustics(IsParameters params){
        if(params.g_RenderCaustics){
            // selecting water_normalmap_resource rendertarget
            water_normalmap_framebuffer.bind();
            water_normalmap_framebuffer.setViewPort();
            gl.glDisable(GLenum.GL_DEPTH_TEST);
//			GL11.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
//			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(clearColor));

            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, water_bump_texture.getTexture());
            gl.glBindSampler(0, IsSamplers.g_SamplerLinearWrap);
            //rendering water normalmap
//			setupNormalView(/*cam*/); // need it just to provide shader with camera position

            waterNormalmapCombineProgram.enable();
            waterNormalmapCombineProgram.applyCameraPosition(params.g_CameraPosition);
            waterNormalmapCombineProgram.applyWaterBumpTexcoordShift(params.g_WaterBumpTexcoordShift);
            gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
            waterNormalmapCombineProgram.disable();
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
            gl.glBindSampler(0, 0);
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            GLCheck.checkError();
        }
    }

    void renderShadowMap(IsParameters params){
        shadownmap_framebuffer.bind();
        shadownmap_framebuffer.setViewPort();

        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glColorMask(false, false, false, false);  // disable color output.

        final float terrain_minheight = m_TerrainVB.getTerrainParams().terrain_minheight;
        params.g_HalfSpaceCullSign = 1.0f;
        params.g_HalfSpaceCullPosition = terrain_minheight*2;
        params.g_TerrainBeingRendered = 1;
        params.g_SkipCausticsCalculation = 1;
        renderTerrain(params, false, true);
        gl.glColorMask(true, true, true, true);
        GLCheck.checkError();
    }

    void renderTerrain(IsParameters params, boolean cullface, boolean shadow_map){

        renderHeightfieldProgram.enable(params, terrain_textures);
        if(params.g_Wireframe){
            renderHeightfieldProgram.setupColorPass();
        }else{
            renderHeightfieldProgram.setupRenderHeightFieldPass();
        }

        renderHeightfieldProgram.setRenderShadowmap(shadow_map);
        m_TerrainVB.draw(0, cullface);
        renderHeightfieldProgram.disable();
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

    void loadTextures(String prefix)throws IOException{
        NvImage.upperLeftOrigin(false);
        rock_bump_texture = wrap(NvImage.uploadTextureFromDDSFile(prefix + "rock_bump6.dds"));
        rock_diffuse_texture = wrap(NvImage.uploadTextureFromDDSFile(prefix + "terrain_rock4.dds"));
        sand_diffuse_texture = wrap(NvImage.uploadTextureFromDDSFile(prefix + "sand_diffuse.dds"));
        sand_bump_texture = wrap(NvImage.uploadTextureFromDDSFile(prefix + "rock_bump4.dds"));
        grass_diffuse_texture = wrap(NvImage.uploadTextureFromDDSFile(prefix + "terrain_grass.dds"));
        slope_diffuse_texture = wrap(NvImage.uploadTextureFromDDSFile(prefix + "terrain_slope.dds"));
        sand_microbump_texture =wrap(NvImage.uploadTextureFromDDSFile(prefix + "lichen1_normal.dds"));
        rock_microbump_texture = wrap(NvImage.uploadTextureFromDDSFile(prefix + "rock_bump4.dds"));
        sky_texture = wrap(NvImage.uploadTextureFromDDSFile(prefix + "sky.dds"));
        water_bump_texture = wrap(NvImage.uploadTextureFromDDSFile("nvidia/WaveWorks/textures/water_bump.dds"));

        GLCheck.checkError();

        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
    }

    static Texture2D wrap(int textureID){
        return TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, textureID);
    }

}
