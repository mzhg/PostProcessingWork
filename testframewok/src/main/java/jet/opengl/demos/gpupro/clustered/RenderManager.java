package jet.opengl.demos.gpupro.clustered;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.demos.amdfx.common.CFirstPersonCamera;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.texture.FramebufferGL;
import jet.opengl.postprocessing.texture.TextureCube;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;

import static jet.opengl.postprocessing.common.GLenum.GL_SHADER_STORAGE_BUFFER;
import static jet.opengl.postprocessing.common.GLenum.GL_STATIC_COPY;

final class RenderManager {
    //Todo:: shaders should belong to a material not the rendermanager
    private GLSLProgram depthPrePassShader, PBRClusteredShader, skyboxShader,
            highPassFilterShader, gaussianBlurShader, screenSpaceShader,
            dirShadowShader, pointShadowShader, fillCubeMapShader,
            convolveCubeMap, preFilterSpecShader, integrateBRDFShader;

    //TODO::Compute shaders don't have a strong a case as regular shaders to be made a part of
    //other classes, since they feel more like static functions of the renderer than methods that
    //are a part of certain objects.
    private GLSLProgram buildAABBGridCompShader, cullLightsCompShader;

    //Pointers to the scene and its contents which contains all the geometry data
    //that we will be rendering rendering
    private CFirstPersonCamera sceneCamera;
    private Scene  currentScene;
//    DisplayManager *screen;
    private SceneManager   sceneLocator;

    //The canvas is an abstraction for screen space rendering. It helped me build a mental model
    //of drawing at the time but I think it is now unecessary sinceI feel much more comfortable with
    //compute shaders and the inner workings of the GPU.
//    Quad canvas;

    //The variables that determine the size of the cluster grid. They're hand picked for now, but
    //there is some space for optimization and tinkering as seen on the Olsson paper and the ID tech6
    //presentation.
    private static final int gridSizeX = 16;
    private static final int gridSizeY =  9;
    private static final int gridSizeZ = 24;
    private static final int numClusters = gridSizeX * gridSizeY * gridSizeZ;
    private int sizeX, sizeY;

    private int numLights;
    private static final int maxLights = 1000; // pretty overkill for sponza, but ok for testing
    private static final int maxLightsPerTile = 50;

    //Shader buffer objects, currently completely managed by the rendermanager class for creation
    //using and uploading to the gpu, but they should be moved somwehre else to avoid bloat
    private int AABBvolumeGridSSBO, screenToViewSSBO;
    private int lightSSBO, lightIndexListSSBO, lightGridSSBO, lightIndexGlobalCountSSBO;

    //Render pipeline FrameBuffer objects. I absolutely hate that the pointlight shadows have distinct
    //FBO's instead of one big one. I think we will take the approach that is outlined on the Id tech 6 talk
    //and use a giant texture to store all textures. However, since this require a pretty substantial rewrite
    //of the illumination code I have delayed this until after the first official github release of the
    //project.
    private FramebufferGL simpleFBO;
    private FramebufferGL captureFBO;
    private FramebufferGL pingPongFBO;
    private FramebufferGL  dirShadowFBO;
    private FramebufferGL multiSampledFBO;
    private FramebufferGL[]  pointLightShadowFBOs;

    private int m_WindowWidth, m_WindowHeight, m_Samples;
    private GLFuncProvider gl;

    private static final Matrix4f captureProjection = Matrix4f.perspective(90, 1, 1, 10, null);

    static final ScreenToView screen2View = new ScreenToView();

    //Gets scene and display info. Will be used to build render Queue
    boolean startUp(/*DisplayManager &displayManager,*/ SceneManager sceneManager ){
        LogUtil.i(LogUtil.LogType.DEFAULT, "\nInitializing Renderer.\n");
        gl = GLFuncProviderFactory.getGLFuncProvider();

        //Getting pointers to the data we'll render
        sceneLocator = sceneManager;
        currentScene = sceneLocator.getCurrentScene();
        sceneCamera = currentScene.getCurrentCamera();

        LogUtil.i(LogUtil.LogType.DEFAULT,"Loading FBO's...\n");
        if( !initFBOs() ){
            LogUtil.i(LogUtil.LogType.DEFAULT,"FBO's failed to be initialized correctly.\n");
            return false;
        }

        LogUtil.i(LogUtil.LogType.DEFAULT,"Loading Shaders...\n");
        if (!loadShaders()){
            LogUtil.i(LogUtil.LogType.DEFAULT,"Shaders failed to be initialized correctly.\n");
            return false;
        }

        LogUtil.i(LogUtil.LogType.DEFAULT,"Loading SSBO's...\n");
        if (!initSSBOs()){
            LogUtil.i(LogUtil.LogType.DEFAULT,"SSBO's failed to be initialized correctly.\n");
            return false;
        }

        LogUtil.i(LogUtil.LogType.DEFAULT,"Preprocessing...\n");
        if (!preProcess()){
            LogUtil.i(LogUtil.LogType.DEFAULT,"SSBO's failed to be initialized correctly.\n");
            return false;
        }

        LogUtil.i(LogUtil.LogType.DEFAULT,"Renderer Initialization complete.\n");
        return true;
    }

    void shutDown(){

    }

    //The core rendering loop of the engine. The steps that it follow are indicated on the cpp file
    //but it's just a very vanilla Forward+ clustered renderer with barely any bells and whistled or
    //optimizations. In the future, (the magical land where all projects are complete) I plan on
    //heavily optimizing this part of the program along the lines of the 2014 talk, "beyond porting"
    //But in the mean-time it uses pretty basic an naive openGL.
    void render(const unsigned int start);

    void onResize(int width, int height, int sample){
        m_WindowWidth = width;
        m_WindowHeight = height;
        m_Samples = sample;
    }

    //Internal initialization functions
    private boolean initFBOs();

    private boolean initSSBOs(){
        //Setting up tile size on both X and Y
        sizeX =  ( int)Math.ceil(m_WindowWidth / (float)gridSizeX);

        float zFar    =  sceneCamera.GetFarClip();
        float zNear   =  sceneCamera.GetNearClip();

        //Buffer containing all the clusters
        {
            AABBvolumeGridSSBO = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, AABBvolumeGridSSBO);

            //We generate the buffer but don't populate it yet.
            gl.glBufferData(GLenum.GL_SHADER_STORAGE_BUFFER, numClusters * /*sizeof(struct VolumeTileAABB)*/ Vector4f.SIZE*2, GL_STATIC_COPY);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, AABBvolumeGridSSBO);
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, 0);
        }

        //Setting up screen2View ssbo
        {
            screenToViewSSBO = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, screenToViewSSBO);

            //Setting up contents of buffer
            Matrix4f.invert(sceneCamera.GetProjMatrix(), screen2View.inverseProjectionMat);
            screen2View.tileSizes[0] = gridSizeX;
            screen2View.tileSizes[1] = gridSizeY;
            screen2View.tileSizes[2] = gridSizeZ;
            screen2View.tileSizes[3] = sizeX;
            screen2View.screenWidth  = m_WindowWidth;
            screen2View.screenHeight = m_WindowHeight;
            //Basically reduced a log function into a simple multiplication an addition by pre-calculating these
            screen2View.sliceScalingFactor = (float)(gridSizeZ / (Math.log(zFar / zNear)/ Math.log(2)));
            screen2View.sliceBiasFactor    = -(float)(gridSizeZ * Math.log(zNear) / Math.log(zFar / zNear)) ;

            ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(ScreenToView.SIZE);
            screen2View.store(buffer).flip();
            //Generating and copying data to memory in GPU
            gl.glBufferData(GLenum.GL_SHADER_STORAGE_BUFFER, buffer, GLenum.GL_STATIC_COPY);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, screenToViewSSBO);
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, 0);
        }

        //Setting up lights buffer that contains all the lights in the scene
        {
            lightSSBO = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, lightSSBO);
            gl.glBufferData(GLenum.GL_SHADER_STORAGE_BUFFER, maxLights * GPULight.SIZE, GLenum.GL_DYNAMIC_DRAW);

            int bufMask = GLenum.GL_READ_WRITE;

            /*struct GPULight *lights = (struct GPULight *)glMapBuffer(GL_SHADER_STORAGE_BUFFER, bufMask);
            PointLight *light;*/
            ByteBuffer lights = CacheBuffer.getCachedByteBuffer(numLights * GPULight.SIZE);
            GPULight tempLight = new GPULight();
            for(int i = 0; i < numLights; ++i ){
                //Fetching the light from the current scene
                PointLight light = currentScene.getPointLight(i);
                tempLight.position  .set(light.position, 1.0f);
                tempLight.color     .set(light.color, 1.0f);
                tempLight.enabled   = 1;
                tempLight.intensity = 1.0f;
                tempLight.range     = 65.0f;

                tempLight.store(lights);
            }
//            glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
            lights.flip();
            gl.glBufferSubData(GLenum.GL_SHADER_STORAGE_BUFFER, 0, lights);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 3, lightSSBO);
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, 0);
        }

        //A list of indices to the lights that are active and intersect with a cluster
        {
            int totalNumLights =  numClusters * maxLightsPerTile; //50 lights per tile max
            lightIndexListSSBO = gl.glGenBuffer();
            gl.glBindBuffer(GL_SHADER_STORAGE_BUFFER, lightIndexListSSBO);

            //We generate the buffer but don't populate it yet.
            gl.glBufferData(GL_SHADER_STORAGE_BUFFER,  totalNumLights * /*sizeof(unsigned int)*/4, GL_STATIC_COPY);
            gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, lightIndexListSSBO);
            gl.glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        }

        //Every tile takes two unsigned ints one to represent the number of lights in that grid
        //Another to represent the offset to the light index list from where to begin reading light indexes from
        //This implementation is straight up from Olsson paper
        {
            gl.glGenBuffers(1, &lightGridSSBO);
            gl.glBindBuffer(GL_SHADER_STORAGE_BUFFER, lightGridSSBO);

            gl.glBufferData(GL_SHADER_STORAGE_BUFFER, numClusters * 2 * /*sizeof(unsigned int)*/4, GL_STATIC_COPY);
            gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, lightGridSSBO);
            gl.glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        }

        //Setting up simplest ssbo in the world
        {
            gl.glGenBuffers(1, &lightIndexGlobalCountSSBO);
            gl.glBindBuffer(GL_SHADER_STORAGE_BUFFER, lightIndexGlobalCountSSBO);

            //Every tile takes two unsigned ints one to represent the number of lights in that grid
            //Another to represent the offset
            gl.glBufferData(GL_SHADER_STORAGE_BUFFER, /*sizeof(unsigned int)*/4,  GL_STATIC_COPY);
            gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 6, lightIndexGlobalCountSSBO);
            gl.glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        }

        return true;
    }

    private boolean loadShaders();
    //TODO:: rewrite shadow mapping to be dynamic and fast and make use of cluster shading
    //and some new low driver overhead stuff I've been reading about
    private boolean preProcess(){
        //Initializing the surface that we use to draw screen-space effects
//        canvas.setup();

        //Building the grid of AABB enclosing the view frustum clusters
        buildAABBGridCompShader.enable();
        GLSLUtil.setFloat(buildAABBGridCompShader,"zNear", sceneCamera.GetNearClip());
        GLSLUtil.setFloat(buildAABBGridCompShader,"zFar", sceneCamera.GetFarClip());
        gl.glDispatchCompute(gridSizeX, gridSizeY, gridSizeZ);
        gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT);

        //Environment Mapping
        //Passing equirectangular map to cubemap
        captureFBO.bind();
//        currentScene.mainSkyBox.fillCubeMapWithTexture(fillCubeMapShader);

        //Cubemap convolution TODO:: This could probably be moved to a function of the scene or environment maps
        //themselves as a class / static function
        int res = currentScene.irradianceMap.getWidth();
        captureFBO.resizeFrameBuffer(res);
        int environmentID = currentScene.mainSkyBox.skyBoxCubeMap.getTexture();
        convolveCubeMap(environmentID, convolveCubeMap, currentScene.irradianceMap);

        //Cubemap prefiltering TODO:: Same as above
        int captureRBO = captureFBO.depthBuffer;
        preFilterCubeMap(environmentID, captureRBO, preFilterSpecShader, currentScene.specFilteredMap);

        //BRDF lookup texture
        integrateBRDFShader.enable();
        res = currentScene.brdfLUTTexture.getHeight();
        captureFBO.resizeFrameBuffer(res);
        int id = currentScene.brdfLUTTexture.getTexture();
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, GLenum.GL_TEXTURE_2D, id, 0);
        gl.glViewport(0, 0, res, res);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        canvas.draw();

        //Making sure that the viewport is the correct size after rendering
        gl.glViewport(0, 0, m_WindowWidth, m_WindowHeight);

        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthMask(true);

        //Populating depth cube maps for the point light shadows
        for (int i = 0; i < currentScene.pointLightCount; ++i){
            pointLightShadowFBOs[i].bind();
//            pointLightShadowFBOs[i].clear(GLenum.GL_DEPTH_BUFFER_BIT, new Vector3f(1.0f));  todo
//            currentScene.drawPointLightShadow(pointShadowShader,i, pointLightShadowFBOs[i].depthBuffer);todo
        }

        // Directional shadows
        dirShadowFBO.bind();
//        dirShadowFBO.clear(GL_DEPTH_BUFFER_BIT, glm::vec3(1.0f));  todo
//        currentScene.drawDirLightShadows(dirShadowShader, dirShadowFBO.depthBuffer);  todo

        //As we add more error checking this will change from a dummy variable to an actual thing
        return true;
    }

    //For use in the diffuse IBL setup for now
    void convolveCubeMap(int environmentMap, GLSLProgram convolveShader, TextureCube result){
        convolveShader.enable();
        GLSLUtil.setInt(convolveShader,"environmentMap", 0);
        GLSLUtil.setMat4(convolveShader,"projection", captureProjection);

//        gl.glViewport(0, 0, width, height);  todo
        gl.glActiveTexture(GLenum. GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, environmentMap);

        /*for(int i = 0; i < numSidesInCube; ++i){  todo
            convolveShader.setMat4("view", captureViews[i]);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                    GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, textureID, 0);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            drawCube();
        }*/

        throw new UnsupportedOperationException();
    }

    //Specular IBL cubemap component of hte integral
    void preFilterCubeMap( int environmentMap,int captureRBO,GLSLProgram filterShader, TextureCube result){

        filterShader.enable();
        GLSLUtil.setInt(filterShader,"environmentMap", 0);
        GLSLUtil.setMat4(filterShader,"projection", captureProjection);

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, environmentMap);

        //For each Mip level we have to pre-filter the cubemap at each cube face
        for( int mip = 0; mip < result.getMipLevels(); ++mip){
            //Mip levels are decreasing powers of two of the original resolution of the cubemap
            int mipWidth  = (int)( result.getWidth()  * Math.pow(0.5, mip));
            int mipHeight = (int)( result.getHeight() * Math.pow(0.5, mip));

            //The depth component needs to be resized for each mip level too
            gl.glBindRenderbuffer(GLenum.GL_RENDERBUFFER, captureRBO);
            gl.glRenderbufferStorage(GLenum.GL_RENDERBUFFER, GLenum.GL_DEPTH_COMPONENT24, mipWidth, mipHeight);
            gl.glViewport(0, 0, mipWidth, mipHeight);

            for(int i = 0; i < 6; ++i){
                GLSLUtil.setMat4(filterShader,"view", captureViews[i]);

                gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0,
                        GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
                        result.getTexture(), mip);

                gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
                drawCube();
            }
        }
    }

    //Functions used to break up the main render function into more manageable parts.
    void postProcess(int start);
}
