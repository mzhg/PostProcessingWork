package jet.opengl.demos.gpupro.lpv;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.obj.NvModelExt;
import com.nvidia.developer.opengl.models.obj.NvModelExtGL;
import com.nvidia.developer.opengl.models.obj.NvModelFileLoader;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector3i;

import java.io.IOException;
import java.nio.FloatBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.AttachType;
import jet.opengl.postprocessing.texture.FramebufferGL;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.DebugTools;
import jet.opengl.postprocessing.util.FileLoader;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.Numeric;

import static jet.opengl.postprocessing.common.GLenum.GL_ARRAY_BUFFER;

public class LightPropagationVolumeDemo extends NvSampleApp implements NvModelFileLoader {
    static final int SHADOWMAPSIZE = 2048;
    static final int RSMSIZE =256;
//            #define TEXTUREDIR "../textures/"
//            #define SRCDIR "../src/"
    static final int MAX_GRID_SIZE = 32;
    static final int VPL_COUNT = RSMSIZE*RSMSIZE;
    static final float DEG2RAD = (Numeric.PI / 180.0f);

//#define VPL_DEBUG
//#define GRIDS_DEBUG
//#define ORTHO_PROJECTION
//#define USESAMPLER3D
//#define PROPAGATION_STEPS 8
    static final int MAX_PROPAGATION_STEPS = 12;
    static final int CASCADES = 3;
    static final float MAX_CELL_SIZE = 2.5f;

    NvModelExtGL m_modelsExt;

    private GLSLProgram basicShader;
    private GLSLProgram rsmShader;
    private GLSLProgram shadowMap;
    private GLSLProgram injectLight;
    private GLSLProgram injectLight_layered;
    private GLSLProgram VPLsDebug;
    private GLSLProgram geometryInject;
    private GLSLProgram geometryInject_layered;
    private GLSLProgram gBufferShader;
    private GLSLProgram propagationShader;
    private GLSLProgram propagationShader_layered;

    private GBuffer gBuffer;
    //glm::vec3 lightPosition(0.0, 4.0, 2.0);
    private CTextureManager texManager;
    private FramebufferGL fboManager = new FramebufferGL();
    private FramebufferGL RSMFboManager = new FramebufferGL();
    private FramebufferGL ShadowMapManager = new FramebufferGL();
    CLightObject light;
//    DebugDrawer * dd, *dd_l1, *dd_l2;
    //GLuint depthPassFBO;
    int texture_units, max_color_attachments;
    int VPLsVAO, VPLsVBO, PropagationVAO, PropagationVBO;
    final Vector3i volumeDimensions = new Vector3i();
    final Vector3f vMin = new Vector3f();
    final Vector3f editedVolumeDimensions = new Vector3f();

    float aspect;
    float movementSpeed = 10.0f;
    float ftime;
    float cellSize;
    float f_tanFovXHalf;
    float f_tanFovYHalf;
    float f_texelAreaModifier = 1.0f; //Arbitrary value
    float f_indirectAttenuation = 1.7f;
    float initialCamHorAngle = 4.41052f;
    float initialCamVerAngle = -0.214501f;

    boolean b_useNormalOffset = false;
    boolean b_firstPropStep = true;
    boolean b_useMultiStepPropagation  = true;
    boolean b_enableGI = true;
    boolean b_canWriteToFile = true;
    boolean b_lightIntesityOnly = false;
    boolean b_compileAndUseAtomicShaders = true;
    boolean b_firstFrame = true;
    boolean b_interpolateBorders = true;

    boolean b_recordingMode = false;
    boolean b_animation = true;

//bool b_recordingMode = true;
//bool b_animation = false;

    boolean b_profileMode = false;
    boolean b_showGrids = false;
    boolean b_useOcclusion = true;

    //1
    boolean b_enableCascades = true;
    boolean b_useLayeredFill = true;
    boolean b_movableLPV = true;

//2
//bool b_enableCascades = true;
//bool b_useLayeredFill = false;
//bool b_movableLPV = true;

//3
//bool b_enableCascades = false;
//bool b_useLayeredFill = true;
//bool b_movableLPV = false;

//4
//bool b_enableCascades = false;
//bool b_useLayeredFill = false;
//bool b_movableLPV = false;

    int volumeDimensionsMult;

    Grid[] levels= new Grid[CASCADES];
//v_allGridMins, v_allCellSizes
    final Vector3f[] v_allGridMins = new Vector3f[CASCADES];
    final Vector3f v_allCellSizes = new Vector3f();
    final Matrix4f lastm0 = new Matrix4f();
    final Matrix4f lastm1 = new Matrix4f();
    final Matrix4f lastm2 = new Matrix4f();

    final Matrix4f biasMatrix = new Matrix4f(
	0.5f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.5f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.5f, 0.0f,
            0.5f, 0.5f, 0.5f, 1.0f
    );

    class  propTextureType{
        TextureGL red, green, blue;
    } ;

    int PROPAGATION_STEPS = 8;

    propTextureType[][] propTextures = new propTextureType[CASCADES][MAX_PROPAGATION_STEPS];
    propTextureType[] injectCascadeTextures = new propTextureType[CASCADES];
    propTextureType[] accumulatorCascadeTextures = new propTextureType[CASCADES];
    TextureGL[] geometryInjectCascadeTextures = new TextureGL[CASCADES];
    FramebufferGL[][] propagationFBOs = new FramebufferGL[CASCADES][MAX_PROPAGATION_STEPS];
    FramebufferGL[] lightInjectCascadeFBOs = new FramebufferGL[CASCADES];
    FramebufferGL[] geometryInjectCascadeFBOs = new FramebufferGL[CASCADES];
    final Vector3f initialCameraPos = new Vector3f(31.4421f, 21.1158f, 3.80755f);

    int level_global = 0;
    int currIndex = 0;

    private GLFuncProvider gl;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        FileLoader old = FileUtils.g_IntenalFileLoader;
        FileUtils.setIntenalFileLoader(FileLoader.g_DefaultFileLoader);
        m_modelsExt = loadModelExt("sponza.obj");
        FileUtils.setIntenalFileLoader(old);

        levels[0] = new Grid(m_modelsExt.GetMaxExt(), m_modelsExt.GetMinExt(),1.0f, 0);
        volumeDimensions.set(levels[0].getDimensions());
        cellSize = levels[0].getCellSize();
        vMin.set(levels[0].getMin());

//        CBoundingBox * bb_l0 = new CBoundingBox(levels[0].getMin(), levels[0].getMax());
//        dd = new DebugDrawer(GL_LINE_STRIP, &(bb_l0->getDebugDrawPoints()), NULL, NULL, glm::vec3(1.0, 0.0, 0.0));
//        delete bb_l0;

        if (CASCADES >= 3) {
            levels[1] = new Grid(levels[0], 0.65f,1);
            levels[2] = new Grid(levels[1], 0.4f,2);

            /*CBoundingBox * bb_l1 = new CBoundingBox(levels[1].getMin(), levels[1].getMax());
            CBoundingBox * bb_l2 = new CBoundingBox(levels[2].getMin(), levels[2].getMax());
            dd_l1 = new DebugDrawer(GL_LINE_STRIP, &(bb_l1->getDebugDrawPoints()), NULL, NULL, glm::vec3(0.0, 1.0, 0.0));
            dd_l2 = new DebugDrawer(GL_LINE_STRIP, &(bb_l2->getDebugDrawPoints()), NULL, NULL, glm::vec3(0.0, 0.0, 1.0));

            delete bb_l1;
            delete bb_l2;*/
        }

        initializeVPLsInvocations();
        initializePropagationVAO(volumeDimensions);
        initInjectFBOs();

        float f_lightFov = light.getFov(); //in degrees, one must convert to radians
        float f_lightAspect = light.getAspectRatio();

        f_tanFovXHalf = (float) Math.tan(0.5 * f_lightFov * DEG2RAD);
        f_tanFovYHalf = (float) (Math.tan(0.5 * f_lightFov * DEG2RAD)*f_lightAspect); //Aspect is always 1, but just for sure

        texManager = new CTextureManager();
        final int WIDTH = getGLContext().width();
        final int HEIGHT = getGLContext().height();
        gBuffer = new GBuffer(texManager, WIDTH, HEIGHT);

        light = new CLightObject(new Vector3f(-0.197587f, 65.0856f, 10.0773f), new Vector3f(0.000831289f, -0.947236f, -0.320536f));
        light.setHorAngle(3.139f);
        light.setVerAngle(-1.2445f);

        ////////////////////////////////////////////////////
        // TEXTURE INIT
        ////////////////////////////////////////////////////
        texManager.createTexture("render_tex", null, WIDTH, HEIGHT, GLenum.GL_NEAREST, GLenum.GL_RGBA16F, false);
        texManager.createTexture("rsm_normal_tex", null, RSMSIZE, RSMSIZE, GLenum.GL_NEAREST, GLenum.GL_RGBA16F, false);
        texManager.createTexture("rsm_world_space_coords_tex", null, RSMSIZE, RSMSIZE, GLenum.GL_NEAREST, GLenum.GL_RGBA16F, false);
        texManager.createTexture("rsm_flux_tex", null, RSMSIZE, RSMSIZE, GLenum.GL_NEAREST, GLenum.GL_RGBA16F, false);
        texManager.createTexture("rsm_depth_tex", null, SHADOWMAPSIZE, SHADOWMAPSIZE, GLenum.GL_LINEAR, GLenum.GL_DEPTH_COMPONENT32, true);

        initPropStepTextures();
        initPropagationFBOs();

        ////////////////////////////////////////////////////
        // FBO INIT
        ////////////////////////////////////////////////////
        /*fboManager->initFbo();
        fboManager->genRenderDepthBuffer(WIDTH, HEIGHT);
        fboManager->bindRenderDepthBuffer();
        fboManager->bindToFbo(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texManager["render_tex"]);
        //fboManager->bindToFbo(GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, texManager["rsm_normal_tex"]);
        //fboManager->bindToFbo(GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,texManager["rsm_depth_tex"]);
        fboManager->setDrawBuffers();
        if (!fboManager->checkFboStatus()){
            return;
        }*/

        TextureAttachDesc[] attachDescs = {
          new TextureAttachDesc(),
          new TextureAttachDesc(),
          new TextureAttachDesc(),
          new TextureAttachDesc(),
        };

        for(int i = 0; i < attachDescs.length; i++){
            attachDescs[i].level = i;
        }

        Texture2DDesc depthDesc = new Texture2DDesc(WIDTH, HEIGHT, GLenum.GL_DEPTH_COMPONENT32F);
        Texture2D depth = TextureUtils.createTexture2D(depthDesc, null);
        fboManager = new FramebufferGL();
        fboManager.bind();
        fboManager.addTextures(CommonUtil.toArray(texManager.get("render_tex"), depth), CommonUtil.toArray(attachDescs[0], attachDescs[0]));

        /*RSMFboManager->initFbo();
        RSMFboManager->genRenderDepthBuffer(WIDTH, HEIGHT);
        RSMFboManager->bindRenderDepthBuffer();
        RSMFboManager->bindToFbo(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texManager["rsm_world_space_coords_tex"]);
        RSMFboManager->bindToFbo(GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, texManager["rsm_normal_tex"]);
        RSMFboManager->bindToFbo(GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, texManager["rsm_flux_tex"]);
        //RSMFboManager->bindToFbo(GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, texManager["rsm_depth_tex"]);
        RSMFboManager->setDrawBuffers();
        if (!RSMFboManager->checkFboStatus()){
            return;
        }*/
        TextureGL[] attachTexs = {
                texManager.get("rsm_world_space_coords_tex"), texManager.get("rsm_normal_tex"),
                texManager.get("rsm_flux_tex"), depth
        };

        RSMFboManager = new FramebufferGL();
        RSMFboManager.bind();
        RSMFboManager.addTextures(attachTexs, attachDescs);

        /*ShadowMapManager->initFbo();
        ShadowMapManager->bindToFbo(GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, texManager["rsm_depth_tex"]);
        ShadowMapManager->setDrawBuffers();
        if (!ShadowMapManager->checkFboStatus()) {
            return;
        }*/

        ShadowMapManager = new FramebufferGL();
        ShadowMapManager.bind();
        ShadowMapManager.addTexture(texManager.get("rsm_depth_tex"), attachDescs[0]);

        //IN CASE OF PROBLEMS UNCOMMENT LINE BELOW
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

        ////////////////////////////////////////////////////
        // SHADERS INIT
        ////////////////////////////////////////////////////
        /*basicShader.LoadFromFile(GL_VERTEX_SHADER, std::string("../shaders/basicShader.vs").c_str());
        basicShader.LoadFromFile(GL_FRAGMENT_SHADER, std::string("../shaders/basicShader.frag").c_str());
        basicShader.CreateAndLinkProgram();*/

        final String root = "gpupro\\LightPropagationVolume\\shaders\\";
        basicShader = GLSLProgram.createProgram(root+"basicShader.vs", root + "basicShader.frag", null);

        /*rsmShader.LoadFromFile(GL_VERTEX_SHADER, std::string("../shaders/RSMpass.vs").c_str());
        rsmShader.LoadFromFile(GL_FRAGMENT_SHADER, std::string("../shaders/RSMpass.frag").c_str());
        rsmShader.CreateAndLinkProgram();*/
        rsmShader = GLSLProgram.createProgram(root+"RSMpass.vs", root+"RSMpass.frag", null);

        /*shadowMap.LoadFromFile(GL_VERTEX_SHADER, std::string("../shaders/depthOnly.vs").c_str());
        shadowMap.LoadFromFile(GL_FRAGMENT_SHADER, std::string("../shaders/depthOnly.frag").c_str());
        shadowMap.CreateAndLinkProgram();*/

        shadowMap = GLSLProgram.createProgram(root+"depthOnly.vs", root+"depthOnly.frag", null);

        /*gBufferShader.LoadFromFile(GL_VERTEX_SHADER, std::string("../shaders/gbufferFill.vs").c_str());
        gBufferShader.LoadFromFile(GL_FRAGMENT_SHADER, std::string("../shaders/gbufferFill.frag").c_str());
        gBufferShader.CreateAndLinkProgram();*/
        gBufferShader = GLSLProgram.createProgram(root+"gbufferFill.vs", root+"gbufferFill.frag", null);

        if (b_compileAndUseAtomicShaders) {
            /*propagationShader.LoadFromFile(GL_VERTEX_SHADER, std::string("../shaders/propagation.vs").c_str());
            propagationShader.LoadFromFile(GL_FRAGMENT_SHADER, std::string("../shaders/propagation.frag").c_str());
            propagationShader.CreateAndLinkProgram();*/
            propagationShader = GLSLProgram.createProgram(root+"propagation.vs", root+"propagation.frag", null);

            /*injectLight.LoadFromFile(GL_VERTEX_SHADER, std::string("../shaders/lightInject.vs").c_str());
            injectLight.LoadFromFile(GL_FRAGMENT_SHADER, std::string("../shaders/lightInject.frag").c_str());
            injectLight.CreateAndLinkProgram();*/
            injectLight = GLSLProgram.createProgram(root+"lightInject.vs", root+"lightInject.frag", null);

            /*geometryInject.LoadFromFile(GL_VERTEX_SHADER, std::string("../shaders/geometryInject.vs").c_str());
            geometryInject.LoadFromFile(GL_FRAGMENT_SHADER, std::string("../shaders/geometryInject.frag").c_str());
            geometryInject.CreateAndLinkProgram();*/
            geometryInject = GLSLProgram.createProgram(root+"geometryInject.vs", root+"geometryInject.frag", null);
        }


        /*injectLight_layered.LoadFromFile(GL_VERTEX_SHADER, std::string("../shaders/lightInject_layered.vs").c_str());
        injectLight_layered.LoadFromFile(GL_GEOMETRY_SHADER, std::string("../shaders/lightInject_layered.gs").c_str());
        injectLight_layered.LoadFromFile(GL_FRAGMENT_SHADER, std::string("../shaders/lightInject_layered.frag").c_str());
        injectLight_layered.CreateAndLinkProgram();*/
        injectLight_layered = GLSLProgram.createProgram(root+"lightInject_layered.vs", root + "lightInject_layered.gs",root+"lightInject_layered.frag", null);

        /*geometryInject_layered.LoadFromFile(GL_VERTEX_SHADER, std::string("../shaders/geometryInject_layered.vs").c_str());
        geometryInject_layered.LoadFromFile(GL_GEOMETRY_SHADER, std::string("../shaders/geometryInject_layered.gs").c_str());
        geometryInject_layered.LoadFromFile(GL_FRAGMENT_SHADER, std::string("../shaders/geometryInject_layered.frag").c_str());
        geometryInject_layered.CreateAndLinkProgram();*/
        geometryInject_layered = GLSLProgram.createProgram(root+"geometryInject_layered.vs", root + "geometryInject_layered.gs",root+"geometryInject_layered.frag", null);

        /*propagationShader_layered.LoadFromFile(GL_VERTEX_SHADER, std::string("../shaders/propagation_layered.vs").c_str());
        propagationShader_layered.LoadFromFile(GL_GEOMETRY_SHADER, std::string("../shaders/propagation_layered.gs").c_str());
        propagationShader_layered.LoadFromFile(GL_FRAGMENT_SHADER, std::string("../shaders/propagation_layered.frag").c_str());
        propagationShader_layered.CreateAndLinkProgram();*/
        propagationShader_layered = GLSLProgram.createProgram(root+"propagation_layered.vs", root + "propagation_layered.gs",root+"propagation_layered.frag", null);
    }

    @Override
    public void display() {
        //Clear the screen
        gl.glClear( GLenum. GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        //Clear color
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        //Enable depth testing
        gl.glEnable(GLenum.GL_DEPTH_TEST);

        //Camera update
        /*glm::mat4 m = glm::mat4(1.0f);  TODO
        //m = glm::scale(m, glm::vec3(5.0f));
        //glm::mat4 m = glm::mat4(1.0f);
        glm::mat4 v, mvp, mv, vp, p;
        glm::mat3 mn;
        if (b_animation) {
            //std::cout << currIndex << "/" << splinePath.getSplineCameraPath().size() - 1 << std::endl;
            if (currIndex >= splinePath.getSplineCameraPath().size() - 1) {
                kill();
                return;
            }
            tmp = new animationCamera();
            tmp = splinePath.getSplineCameraPathOnIndex(currIndex);
            v = tmp->getAnimationCameraViewMatrix();
            p = tmp->getAnimationCameraProjectionMatrix();
            mn = glm::transpose(glm::inverse(glm::mat3(v*m)));
            mvp = p * v * m;
            mv = v * m;
            vp = p * v;
            //cout << inc << endl;
            currIndex += inc;
            //currIndex *= 2;


            //check end
        }
        else {
            controlCamera->computeMatricesFromInputs();
            v = controlCamera->getViewMatrix();
            p = controlCamera->getProjectionMatrix();
            mn = glm::transpose(glm::inverse(glm::mat3(v*m)));
            mvp = p * v * m;
            mv = v * m;
            vp = p * v;
        }


        glm::mat4 v_light = light->getViewMatrix();
        glm::mat4 p_light = light->getProjMatrix();
        glm::mat4 mvp_light = p_light * v_light * m;
        glm::mat4 inverse_vLight = glm::inverse(v_light);
        glm::mat3 mn_light = glm::transpose(glm::inverse(glm::mat3(v_light*m)));

        glm::vec3 lightPosition = light->getPosition();*/

        //Update grid
        updateGrid();

        ////////////////////////////////////////////////////
        // SHADOW MAP
        ////////////////////////////////////////////////////
        //glEnable(GL_CULL_FACE);
        //glCullFace(GL_FRONT);
        gl.glEnable(GLenum.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(1, 1);

//        glBindFramebuffer(GLenum.GL_FRAMEBUFFER, ShadowMapManager->getFboId());
        ShadowMapManager.bind();
        shadowMap.enable();
        gl.glViewport(0, 0, SHADOWMAPSIZE, SHADOWMAPSIZE);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        light.computeMatrixes();
//        glUniformMatrix4fv(shadowMap("mvp"), 1, GL_FALSE, glm::value_ptr(mvp_light));
//        mesh->render();  TODO render model
        shadowMap.disable();
        gl.glBindFramebuffer(GLenum. GL_FRAMEBUFFER, 0);

        gl.glDisable(GLenum.GL_POLYGON_OFFSET_FILL);
        //glDisable(GL_CULL_FACE);
        if (b_enableGI) {
            ////////////////////////////////////////////////////
            // RSM
            ////////////////////////////////////////////////////
//            RSM.start();  time query
            RSMFboManager.bind();
            rsmShader.enable();
            gl.glViewport(0, 0, RSMSIZE, RSMSIZE);
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
            light.computeMatrixes();
            /*gl.glUniformMatrix4fv(rsmShader("mvp"), 1, GL_FALSE, glm::value_ptr(mvp_light)); TODO
            gl.glUniformMatrix4fv(rsmShader("m"), 1, GL_FALSE, glm::value_ptr(m));
            gl.glUniform3f(rsmShader("v_lightPos"), lightPosition.x, lightPosition.y, lightPosition.z);*/
            //glUniformMatrix3fv(rsmShader("mn"), 1, GL_FALSE, glm::value_ptr(mn_light));
//            mesh->render();  todo
            rsmShader.disable();
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
//            RSM.stop();
            //std::cout << testQuery.getElapsedTime() << std::endl;
            ////////////////////////////////////////////////////
            // LIGHT INJECT
            ////////////////////////////////////////////////////
            //texManager.clear3Dtexture(texManager["LPVGridR"]);
            //texManager.clear3Dtexture(texManager["LPVGridG"]);
            //texManager.clear3Dtexture(texManager["LPVGridB"]);

            int end = 1;

            if (b_enableCascades)
                end = CASCADES;
//            inject.start();
            gl.glViewport(0, 0, volumeDimensions.x, volumeDimensions.y); //!! Set vieport to width and height of 3D texture!!
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);

            for (int i = 0; i < end; i++) {
                texManager.clear3Dtexture(injectCascadeTextures[i].red);
                texManager.clear3Dtexture(injectCascadeTextures[i].green);
                texManager.clear3Dtexture(injectCascadeTextures[i].blue);

                vMin.set(levels[i].getMin());
                cellSize = levels[i].getCellSize();

                if (b_useLayeredFill) {

//                    gl.glBindFramebuffer(GL_FRAMEBUFFER, lightInjectCascadeFBOs[i].getFboId());
                    lightInjectCascadeFBOs[i].bind();
                    gl.glEnable(GLenum.GL_BLEND);
                    gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE);
                    //Additive
                    gl.glBlendEquation(GLenum.GL_FUNC_ADD);
                    injectLight_layered.enable();

                    /*glUniform1i(injectLight_layered("rsm_world_space_coords_tex"), 0);  todo
                    glUniform1i(injectLight_layered("rsm_normal_tex"), 1);
                    glUniform1i(injectLight_layered("rsm_flux_tex"), 2);
                    glUniform1i(injectLight_layered("i_RSMsize"), RSMSIZE);
                    glUniform1f(injectLight_layered("f_cellSize"), cellSize);
                    glUniform3f(injectLight_layered("v_gridDim"), volumeDimensions.x, volumeDimensions.y, volumeDimensions.z);
                    glUniform3f(injectLight_layered("v_min"), vMin.x, vMin.y, vMin.z);*/
                    gl.glActiveTexture(GLenum.GL_TEXTURE0);
                    gl.glBindTexture(GLenum.GL_TEXTURE_2D, texManager.get("rsm_world_space_coords_tex").getTexture());
                    gl.glActiveTexture(GLenum.GL_TEXTURE1);
                    gl.glBindTexture(GLenum.GL_TEXTURE_2D, texManager.get("rsm_normal_tex").getTexture());
                    gl.glActiveTexture(GLenum.GL_TEXTURE2);
                    gl.glBindTexture(GLenum.GL_TEXTURE_2D, texManager.get("rsm_flux_tex").getTexture());

                    gl.glBindVertexArray(VPLsVAO);//aktivujeme VAO
                    gl.glDrawArrays(GLenum.GL_POINTS, 0, VPL_COUNT);
                    gl.glBindVertexArray(0);//deaktivujeme VAO
                    injectLight_layered.disable();
                    gl.glDisable(GLenum.GL_BLEND);
                    gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
                }
                else {
                    if (b_compileAndUseAtomicShaders) {
                        injectLight.enable();
                        //texManager.clear3Dtexture(texManager["LPVGridR"]);
                        //texManager.clear3Dtexture(texManager["LPVGridG"]);
                        //texManager.clear3Dtexture(texManager["LPVGridB"]);

                        /*glUniform1i(injectLight("LPVGridR"), 0);
                        glUniform1i(injectLight("LPVGridG"), 1);
                        glUniform1i(injectLight("LPVGridB"), 2);
                        glUniform1i(injectLight("rsm_world_space_coords_tex"), 0);
                        glUniform1i(injectLight("rsm_normal_tex"), 1);
                        glUniform1i(injectLight("rsm_flux_tex"), 2);
                        glUniform1i(injectLight("i_RSMsize"), RSMSIZE);
                        glUniform1f(injectLight("f_cellSize"), cellSize);
                        glUniform3f(injectLight("v_gridDim"), volumeDimensions.x, volumeDimensions.y, volumeDimensions.z);
                        glUniform3f(injectLight("v_min"), vMin.x, vMin.y, vMin.z);*/
                        gl.glActiveTexture(GLenum.GL_TEXTURE0);
                        gl.glBindTexture(GLenum.GL_TEXTURE_2D, texManager.get("rsm_world_space_coords_tex").getTexture());
                        gl.glActiveTexture(GLenum.GL_TEXTURE1);
                        gl.glBindTexture(GLenum.GL_TEXTURE_2D, texManager.get("rsm_normal_tex").getTexture());
                        gl.glActiveTexture(GLenum.GL_TEXTURE2);
                        gl.glBindTexture(GLenum.GL_TEXTURE_2D, texManager.get("rsm_flux_tex").getTexture());
                        gl.glBindImageTexture(0, injectCascadeTextures[i].red.getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);
                        gl.glBindImageTexture(1, injectCascadeTextures[i].green.getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);
                        gl.glBindImageTexture(2, injectCascadeTextures[i].blue.getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);
                        gl.glBindVertexArray(VPLsVAO);
                        gl.glDrawArrays(GLenum.GL_POINTS, 0, VPL_COUNT);
                        gl.glBindVertexArray(0);
                        injectLight.disable();
                    }
                }

                ////////////////////////////////////////////////////
                // GEOMETRY INJECT
                ////////////////////////////////////////////////////
                gl.glViewport(0, 0, volumeDimensions.x, volumeDimensions.y); //!! Set vieport to width and height of 3D texture!!
                gl.glDisable(GLenum.GL_DEPTH_TEST);
                gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
                texManager.clear3Dtexture(geometryInjectCascadeTextures[i]);
                if (b_useLayeredFill) {
//                    gl.glBindFramebuffer(GL_FRAMEBUFFER, geometryInjectCascadeFBOs[i].getFboId());
                    geometryInjectCascadeFBOs[i].bind();
                    gl.glEnable(GLenum.GL_BLEND);
                    gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE);
                    //Additive
                    gl.glBlendEquation(GLenum.GL_FUNC_ADD);
                    geometryInject_layered.enable();
                    /*glUniform1i(geometryInject_layered("rsm_world_space_coords_tex"), 0); todo
                    glUniform1i(geometryInject_layered("rsm_normal_tex"), 1);
                    glUniform1i(geometryInject_layered("i_RSMsize"), RSMSIZE);
                    glUniform1f(geometryInject_layered("f_cellSize"), cellSize);
                    glUniform1f(geometryInject_layered("f_tanFovXHalf"), f_tanFovXHalf);
                    glUniform1f(geometryInject_layered("f_tanFovYHalf"), f_tanFovYHalf);
                    glUniform1f(geometryInject_layered("f_texelAreaModifier"), f_texelAreaModifier);
                    glUniform3f(geometryInject_layered("v_gridDim"), volumeDimensions.x, volumeDimensions.y, volumeDimensions.z);
                    glUniform3f(geometryInject_layered("v_min"), vMin.x, vMin.y, vMin.z);
                    glUniform3f(geometryInject_layered("v_lightPos"), lightPosition.x, lightPosition.y, lightPosition.z);
                    glUniformMatrix4fv(geometryInject_layered("m_lightView"), 1, GL_FALSE, glm::value_ptr(v_light));*/
                    gl.glActiveTexture(GLenum.GL_TEXTURE0);
                    gl.glBindTexture(GLenum.GL_TEXTURE_2D, texManager.get("rsm_world_space_coords_tex").getTexture());
                    gl.glActiveTexture(GLenum.GL_TEXTURE1);
                    gl.glBindTexture(GLenum.GL_TEXTURE_2D, texManager.get("rsm_normal_tex").getTexture());
                    gl.glBindVertexArray(VPLsVAO);
                    gl.glDrawArrays(GLenum.GL_POINTS, 0, VPL_COUNT);
                    gl.glBindVertexArray(0);
                    geometryInject_layered.disable();
                    gl.glDisable(GLenum.GL_BLEND);
                    gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
                }
                else {
                    if (b_compileAndUseAtomicShaders) {
                        geometryInject.enable();
                        /*glUniform1i(geometryInject("GeometryVolume"), 0);
                        glUniform1i(geometryInject("rsm_world_space_coords_tex"), 0);
                        glUniform1i(geometryInject("rsm_normal_tex"), 1);
                        glUniform1i(geometryInject("i_RSMsize"), RSMSIZE);
                        glUniform1f(geometryInject("f_cellSize"), cellSize);
                        glUniform1f(geometryInject("f_tanFovXHalf"), f_tanFovXHalf);
                        glUniform1f(geometryInject("f_tanFovYHalf"), f_tanFovYHalf);
                        glUniform1f(geometryInject("f_texelAreaModifier"), f_texelAreaModifier);
                        glUniform3f(geometryInject("v_gridDim"), volumeDimensions.x, volumeDimensions.y, volumeDimensions.z);
                        glUniform3f(geometryInject("v_min"), vMin.x, vMin.y, vMin.z);
                        glUniform3f(geometryInject("v_lightPos"), lightPosition.x, lightPosition.y, lightPosition.z);
                        glUniformMatrix4fv(geometryInject("m_lightView"), 1, GL_FALSE, glm::value_ptr(v_light));*/
                        //glUniformMatrix4fv(geometryInject("m_lightView"), 1, GL_FALSE, glm::value_ptr(v));
                        gl.glActiveTexture(GLenum.GL_TEXTURE0);
                        gl.glBindTexture(GLenum.GL_TEXTURE_2D, texManager.get("rsm_world_space_coords_tex").getTexture());
                        gl.glActiveTexture(GLenum.GL_TEXTURE1);
                        gl.glBindTexture(GLenum.GL_TEXTURE_2D, texManager.get("rsm_normal_tex").getTexture());
                        gl.glBindImageTexture(0, geometryInjectCascadeTextures[i].getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);
                        gl.glBindVertexArray(VPLsVAO);
                        gl.glDrawArrays(GLenum.GL_POINTS, 0, VPL_COUNT);
                        gl.glBindVertexArray(0);
                        geometryInject.disable();
                    }
                }
            }
//            inject.stop();

            ////////////////////////////////////////////////////
            // LIGHT PROPAGATION
            ////////////////////////////////////////////////////
//            propagation.start();
            if (b_useLayeredFill) {
                for (int l = 0; l < end; l++) {
                    propagate_layered(l);
                }
            }
            else {
                if (b_compileAndUseAtomicShaders) {
                    for (int l = 0; l < end; l++) {
                        propagate(l);
                    }
                }
            }
//            propagation.stop();
        }

        if (b_profileMode && !b_firstFrame) {
//            RSMTimes << RSM.getElapsedTime() << std::endl;
//            injectTimes << inject.getElapsedTime() << std::endl;
//            PropagationTimes << propagation.getElapsedTime() << std::endl;
        }

        final int WIDTH = getGLContext().width();
        final int HEIGHT = getGLContext().height();
//        finalLighting.start();
        ////////////////////////////////////////////////////
        // RENDER SCENE TO TEXTURE
        ////////////////////////////////////////////////////
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        //glCullFace(GL_BACK);
        gl.glViewport(0, 0, WIDTH, HEIGHT);
//        gl.glBindFramebuffer(GL_FRAMEBUFFER, fboManager->getFboId());
        fboManager.bind();
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        basicShader.enable();
//        glUniform1i(basicShader("RAccumulatorLPV_l0"), 3);
        gl.glActiveTexture(GLenum.GL_TEXTURE3);
        gl.glBindTexture(GLenum.GL_TEXTURE_3D, accumulatorCascadeTextures[0].red.getTexture());
//        glUniform1i(basicShader("GAccumulatorLPV_l0"), 4);
        gl.glActiveTexture(GLenum.GL_TEXTURE4);
        gl.glBindTexture(GLenum.GL_TEXTURE_3D, accumulatorCascadeTextures[0].green.getTexture());
//        glUniform1i(basicShader("BAccumulatorLPV_l0"), 5);
        gl.glActiveTexture(GLenum.GL_TEXTURE5);
        gl.glBindTexture(GLenum.GL_TEXTURE_3D, accumulatorCascadeTextures[0].blue.getTexture());

//        glUniform1i(basicShader("RAccumulatorLPV_l1"), 6);
        gl.glActiveTexture(GLenum.GL_TEXTURE6);
        gl.glBindTexture(GLenum.GL_TEXTURE_3D, accumulatorCascadeTextures[1].red.getTexture());
//        glUniform1i(basicShader("GAccumulatorLPV_l1"), 7);
        gl.glActiveTexture(GLenum.GL_TEXTURE7);
        gl.glBindTexture(GLenum.GL_TEXTURE_3D, accumulatorCascadeTextures[1].green.getTexture());
//        glUniform1i(basicShader("BAccumulatorLPV_l1"), 8);
        gl.glActiveTexture(GLenum.GL_TEXTURE8);
        gl.glBindTexture(GLenum.GL_TEXTURE_3D, accumulatorCascadeTextures[1].blue.getTexture());

//        glUniform1i(basicShader("RAccumulatorLPV_l2"), 9);
        gl.glActiveTexture(GLenum.GL_TEXTURE9);
        gl.glBindTexture(GLenum.GL_TEXTURE_3D, accumulatorCascadeTextures[2].red.getTexture());
//        glUniform1i(basicShader("GAccumulatorLPV_l2"), 10);
        gl.glActiveTexture(GLenum.GL_TEXTURE10);
        gl.glBindTexture(GLenum.GL_TEXTURE_3D, accumulatorCascadeTextures[2].green.getTexture());
//        glUniform1i(basicShader("BAccumulatorLPV_l2"), 11);
        gl.glActiveTexture(GLenum.GL_TEXTURE11);
        gl.glBindTexture(GLenum.GL_TEXTURE_3D, accumulatorCascadeTextures[2].blue.getTexture());

        /*glUniform1f(basicShader("f_indirectAttenuation"), f_indirectAttenuation);// f_indirectAttenuation  todo
        glUniform1i(basicShader("b_enableGI"), b_enableGI);
        glUniform1i(basicShader("b_enableCascades"), b_enableCascades);
        glUniform1i(basicShader("b_lightIntesityOnly"), b_lightIntesityOnly);
        glUniform1i(basicShader("b_interpolateBorders"), b_interpolateBorders);
        glUniform3f(basicShader("v_gridDim"), volumeDimensions.x, volumeDimensions.y, volumeDimensions.z);*/

        v_allGridMins[0] = levels[0].getMin();
        v_allGridMins[1] = levels[1].getMin();
        v_allGridMins[2] = levels[2].getMin();

        v_allCellSizes.set(levels[0].getCellSize(), levels[1].getCellSize(), levels[2].getCellSize());

        /*glUniform3fv(basicShader("v_allGridMins"), 3, glm::value_ptr(v_allGridMins[0]));  todo
        glUniform3fv(basicShader("v_allCellSizes"), 1, glm::value_ptr(v_allCellSizes));
        glUniform1i(basicShader("depthTexture"), 1); //Texture unit 1 is for shadow maps.
        glUniformMatrix4fv(basicShader("mvp"), 1, GL_FALSE, glm::value_ptr(mvp));
        glUniformMatrix4fv(basicShader("mv"), 1, GL_FALSE, glm::value_ptr(mv));
        glUniformMatrix4fv(basicShader("v"), 1, GL_FALSE, glm::value_ptr(v));
        glUniformMatrix4fv(basicShader("shadowMatrix"), 1, GL_FALSE, glm::value_ptr(biasMatrix*mvp_light));
        glUniformMatrix3fv(basicShader("mn"), 1, GL_FALSE, glm::value_ptr(mn));
        glUniform3f(basicShader("vLightPos"), lightPosition.x, lightPosition.y, lightPosition.z);*/
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, texManager.get("rsm_depth_tex").getTexture());
//        mesh->render();  todo
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        basicShader.disable();
        /*if (b_showGrids) {  todo debug
            dd.setVPMatrix(mvp);
            dd->updateVBO(&(CBoundingBox::calculatePointDimensions(levels[0].getMin(), levels[0].getMax())));
            dd->draw();
            if (CASCADES >= 3) {
                dd_l1->setVPMatrix(mvp);
                dd_l1->updateVBO(&(CBoundingBox::calculatePointDimensions(levels[1].getMin(), levels[1].getMax())));
                dd_l1->draw();
                dd_l2->setVPMatrix(mvp);
                dd_l2->updateVBO(&(CBoundingBox::calculatePointDimensions(levels[2].getMin(), levels[2].getMax())));
                dd_l2->draw();
            }
        }*/
        ////////////////////////////////////////////////////
        // VPL DEBUG DRAW
        ////////////////////////////////////////////////////
/*#ifdef VPL_DEBUG
        glEnable(GL_PROGRAM_POINT_SIZE);
        glPointSize(2.5f);
        VPLsDebug.Use();
        glUniformMatrix4fv(VPLsDebug("mvp"), 1, GL_FALSE, glm::value_ptr(mvp));
        glUniform1i(VPLsDebug("i_RSMsize"), RSMSIZE);
        glUniform1i(VPLsDebug("rsm_world_space_coords_tex"), 0);
        glUniform1i(VPLsDebug("rsm_normal_tex"), 1);
        glUniform1i(VPLsDebug("b_useNormalOffset"), b_useNormalOffset);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texManager["rsm_world_space_coords_tex"]);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, texManager["rsm_normal_tex"]);
        glBindVertexArray(VPLsVAO);
        glDrawArrays(GL_POINTS, 0, VPL_COUNT);
        glBindVertexArray(0);
        VPLsDebug.UnUse();
        glPointSize(1.0f);
        glDisable(GL_PROGRAM_POINT_SIZE);
#endif*/

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
//        finalLighting.stop();
        //std::cout << finalLighting.getElapsedTime() << endl;

        ////////////////////////////////////////////////////
        // FINAL COMPOSITION
        ////////////////////////////////////////////////////
        //Draw quad on screen
        gl.glViewport(0, 0, WIDTH, HEIGHT);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
//        ctv2->setTexture(texManager["render_tex"]);  todo render the final image to screen.
//        ctv2->draw();

        b_firstFrame = false;
    }

    NvModelExtGL loadModelExt(String model_filename)
    {
        NvModelExt.SetFileLoader(this);
        NvModelExt pModel = null;
        try {
            pModel = NvModelExt.CreateFromObj(model_filename, 40.0f, true, true, 0.01f, 0.001f, 3000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        NvModelExtGL pGLModelExt = NvModelExtGL.Create(pModel);

        return pGLModelExt;
    }

    @Override
    public byte[] loadDataFromFile(String fileName) throws IOException {
        final String file = "E:\\SDK\\VCTRenderer\\engine\\assets\\models\\crytek-sponza\\";

        NvModelExtGL.setTexturePath(file);
        return DebugTools.loadBytes(file + fileName);
    }

    void initializeVPLsInvocations() {
        ////////////////////////////////////////////////////
        // VPL INIT STUFF
        ////////////////////////////////////////////////////
//        injectLight.enable();
        //Generate VAO
        VPLsVAO = gl.glGenVertexArray();

        //Bind VAO
        gl.glBindVertexArray(VPLsVAO);

        //Generate VBO
        VPLsVBO = gl.glGenBuffer();
        //Bind VBO
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, VPLsVBO);

        float[]testPoints = new float[2 * VPL_COUNT];
        float step = 1.0f / VPL_COUNT;
        for (int i = 0; i < VPL_COUNT; ++i) {
            testPoints[i * 2] = 0.0f;
            testPoints[i * 2 + 1] = 0.0f;
        }

        //Alocate buffer
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(testPoints), GLenum.GL_STATIC_DRAW);
        //Fill VBO
        //glBufferSubData(GL_ARRAY_BUFFER, 0, sizeof(testPoints), testPoints);

        //Fill attributes and uniforms
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 2, GLenum.GL_FLOAT, false, /*(sizeof(float)* 2)*/8, 0);
        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

//        injectLight.disable();
    }

    /*
    Initializes VAO for propagation
    */
    void initializePropagationVAO(Vector3i volumeDimensions) {
//        propagationShader.Use();

        //Generate VAO
        PropagationVAO = gl.glGenVertexArray();

        //Bind VAO
        gl.glBindVertexArray(PropagationVAO);

        //Generate VBO
        PropagationVBO = gl.glGenBuffer();
        //Bind VBO
        gl.glBindBuffer(GL_ARRAY_BUFFER, PropagationVBO);

        volumeDimensionsMult = (int) (volumeDimensions.x * volumeDimensions.y * volumeDimensions.z);
        int x = (int) volumeDimensions.x, y = (int) volumeDimensions.y, z = (int) volumeDimensions.z;
	/*float *testPoints = new float[3 * count];
	for (int i = 0; i < count; ++i) {
		testPoints[i * 3] = 0.0f;
		testPoints[i * 3 + 1] = 0.0f;
		testPoints[i * 3 + 2] = 0.0f;
	}*/

//        std::vector<glm::vec3> coords;
        FloatBuffer coords = CacheBuffer.getCachedFloatBuffer(z * y * x * 3);
        for (int d = 0; d < z; d++) {
            for (int c = 0; c < y; c++) {
                for (int r = 0; r < x; r++) {
//                    coords.push_back(glm::vec3((float)r, (float)c, (float)d));
                    coords.put(r);
                    coords.put(c);
                    coords.put(d);
                }
            }
        }
        coords.flip();

        //std::cout << coords.size() * 3 * sizeof(float) << std::endl;
        //std::cout << coords.size() * sizeof(glm::vec3) << std::endl;

        //Alocate buffer
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, coords, GLenum.GL_STATIC_DRAW);
        //Fill VBO
        //glBufferSubData(GL_ARRAY_BUFFER, 0, sizeof(testPoints), testPoints);

        //Fill attributes and uniforms
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, /*(sizeof(float)* 3)*/12, 0);
        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

//        propagationShader.disable();
        //delete testPoints;

	/*for (int i = 0; i < coords.size(); i++) {
		std::cout << coords[i].x << " " << coords[i].y << " " << coords[i].z << std::endl;
	}*/
        //delete tmp;
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0 || height <=0)
            return;

//        aspect = float(height) / float(width);
    }

    /*
Update movable grid
*/
    void updateGrid() {
        if (b_movableLPV) {
            Vector3f pos = new Vector3f();
            Vector3f dir = new Vector3f();
            /*if (b_animation) {
                pos = tmp->getAnimationCameraPosition();
                dir = tmp->getAnimationCameraDirection();
            }
            else {
                pos = controlCamera->getPosition();
                dir = controlCamera->getDirection();
            }*/

            // todo
            levels[0].translateGrid(pos, dir);
            levels[1].translateGrid(pos, dir);
            levels[2].translateGrid(pos, dir);
        }
        //vMin = levels[level].getMin();
        //printVector(vMin);
    }

    //This function *MUST* be called after creation of injectCascadeTextures
    void initPropStepTextures() {
        for (int l = 0; l < CASCADES; l++){
            propTextures[l][0].red = injectCascadeTextures[l].red;
            propTextures[l][0].green = injectCascadeTextures[l].green;
            propTextures[l][0].blue = injectCascadeTextures[l].blue;
            for (int i = 1; i < MAX_PROPAGATION_STEPS; i++) {
                String texNameR = "RLPVStep" + i + "_cascade_" + l;
                String texNameG = "GLPVStep" + i + "_cascade_" + l;
                String texNameB = "BLPVStep" + i + "_cascade_" + l;
                //std::cout << texName << std::endl;
                texManager.createRGBA16F3DTexture(texNameR, volumeDimensions, GLenum.GL_NEAREST, GLenum.GL_CLAMP_TO_BORDER);
                texManager.createRGBA16F3DTexture(texNameG, volumeDimensions, GLenum.GL_NEAREST, GLenum.GL_CLAMP_TO_BORDER);
                texManager.createRGBA16F3DTexture(texNameB, volumeDimensions, GLenum.GL_NEAREST, GLenum.GL_CLAMP_TO_BORDER);
                propTextures[l][i].red = texManager.get(texNameR);
                propTextures[l][i].green = texManager.get(texNameG);
                propTextures[l][i].blue = texManager.get(texNameB);
            }
        }
    }

    //This function *MUST* be called after creation of accumulatorCascadeTextures
    void initPropagationFBOs() {
        //for (int i = 1; i < PROPAGATION_STEPS; i++) {
        TextureGL[] attachTexs = new TextureGL[6];
        TextureAttachDesc[] attachDescs = new TextureAttachDesc[6];

        for(int i = 0; i < 6 ; i++){
            attachDescs[i].index = i;
            attachDescs[i].type = AttachType.TEXTURE;
        }

        for (int l = 0; l < CASCADES; l++) {
            for (int i = 1; i < MAX_PROPAGATION_STEPS; i++) {
                attachTexs[0] = accumulatorCascadeTextures[l].red;
                attachTexs[1] = accumulatorCascadeTextures[l].green;
                attachTexs[2] = accumulatorCascadeTextures[l].blue;
                attachTexs[3] = propTextures[l][i].red;
                attachTexs[4] = propTextures[l][i].green;
                attachTexs[5] = propTextures[l][i].blue;

                propagationFBOs[l][i].bind();
                propagationFBOs[l][i].addTextures(attachTexs, attachDescs);

                /*propagationFBOs[l][i].initFbo();
                propagationFBOs[l][i].bind3DTextureToFbo(GL_COLOR_ATTACHMENT0, accumulatorCascadeTextures[l].red);
                propagationFBOs[l][i].bind3DTextureToFbo(GL_COLOR_ATTACHMENT1, accumulatorCascadeTextures[l].green);
                propagationFBOs[l][i].bind3DTextureToFbo(GL_COLOR_ATTACHMENT2, accumulatorCascadeTextures[l].blue);

                propagationFBOs[l][i].bind3DTextureToFbo(GL_COLOR_ATTACHMENT3, propTextures[l][i].red);
                propagationFBOs[l][i].bind3DTextureToFbo(GL_COLOR_ATTACHMENT4, propTextures[l][i].green);
                propagationFBOs[l][i].bind3DTextureToFbo(GL_COLOR_ATTACHMENT5, propTextures[l][i].blue);
                propagationFBOs[l][i].setDrawBuffers();
                if (!propagationFBOs[l][i].checkFboStatus()) {
                    return;
                }*/
            }
        }
    }

    //Create textures & FBOs
    void initInjectFBOs() {
        TextureGL[] attachTexs = new TextureGL[3];
        TextureAttachDesc[] attachDescs = new TextureAttachDesc[3];

        for(int i = 0; i < attachDescs.length ; i++){
            attachDescs[i].index = i;
            attachDescs[i].type = AttachType.TEXTURE;
        }

        for (int i = 0; i < CASCADES; i++)
        {
            String texNameR = "LPVGridR_cascade_" + i;
            String texNameG = "LPVGridG_cascade_" + i;
            String texNameB = "LPVGridB_cascade_" + i;

            String texNameOcclusion = "GeometryVolume_cascade_" + i;

            String texNameRaccum = "RAccumulatorLPV_cascade_" + i;
            String texNameGaccum = "GAccumulatorLPV_cascade_" + i;
            String texNameBaccum = "BAccumulatorLPV_cascade_" + i;

            texManager.createRGBA16F3DTexture(texNameR, volumeDimensions, GLenum.GL_NEAREST, GLenum.GL_CLAMP_TO_BORDER);
            texManager.createRGBA16F3DTexture(texNameG, volumeDimensions, GLenum.GL_NEAREST, GLenum.GL_CLAMP_TO_BORDER);
            texManager.createRGBA16F3DTexture(texNameB, volumeDimensions, GLenum.GL_NEAREST, GLenum.GL_CLAMP_TO_BORDER);

            texManager.createRGBA16F3DTexture(texNameOcclusion, volumeDimensions, GLenum.GL_LINEAR, GLenum.GL_CLAMP_TO_BORDER);

            texManager.createRGBA16F3DTexture(texNameRaccum, volumeDimensions, GLenum.GL_LINEAR, GLenum.GL_CLAMP_TO_BORDER);
            texManager.createRGBA16F3DTexture(texNameGaccum, volumeDimensions, GLenum.GL_LINEAR, GLenum.GL_CLAMP_TO_BORDER);
            texManager.createRGBA16F3DTexture(texNameBaccum, volumeDimensions, GLenum.GL_LINEAR, GLenum.GL_CLAMP_TO_BORDER);

            injectCascadeTextures[i].red = texManager.get(texNameR);
            injectCascadeTextures[i].green = texManager.get(texNameG);
            injectCascadeTextures[i].blue = texManager.get(texNameB);

            geometryInjectCascadeTextures[i] = texManager.get(texNameOcclusion);

            accumulatorCascadeTextures[i].red = texManager.get(texNameRaccum);
            accumulatorCascadeTextures[i].green = texManager.get(texNameGaccum);
            accumulatorCascadeTextures[i].blue = texManager.get(texNameBaccum);

            attachTexs[0] = injectCascadeTextures[i].red;
            attachTexs[1] = injectCascadeTextures[i].green;
            attachTexs[2] = injectCascadeTextures[i].blue;

            lightInjectCascadeFBOs[i] = new FramebufferGL();
            lightInjectCascadeFBOs[i].bind();
            lightInjectCascadeFBOs[i].addTextures(attachTexs, attachDescs);

            /*lightInjectCascadeFBOs[i].bind3DTextureToFbo(GL_COLOR_ATTACHMENT0, injectCascadeTextures[i].red);
            lightInjectCascadeFBOs[i].bind3DTextureToFbo(GL_COLOR_ATTACHMENT1, injectCascadeTextures[i].green);
            lightInjectCascadeFBOs[i].bind3DTextureToFbo(GL_COLOR_ATTACHMENT2, injectCascadeTextures[i].blue);

            lightInjectCascadeFBOs[i].setDrawBuffers();
            if (!lightInjectCascadeFBOs[i].checkFboStatus()) {
                return;
            }*/

            geometryInjectCascadeFBOs[i] = new FramebufferGL();
            geometryInjectCascadeFBOs[i].bind();
            geometryInjectCascadeFBOs[i].addTexture(geometryInjectCascadeTextures[i], attachDescs[0]);
            /*geometryInjectCascadeFBOs[i].initFbo();
            geometryInjectCascadeFBOs[i].bind3DTextureToFbo(GL_COLOR_ATTACHMENT0, geometryInjectCascadeTextures[i]);

            geometryInjectCascadeFBOs[i].setDrawBuffers();
            if (!geometryInjectCascadeFBOs[i].checkFboStatus()) {
                return;
            }*/
        }
    }

    /*
Propagation using atomic operations
*/
    void propagate(int level) {
        gl.glViewport(0, 0, volumeDimensions.x, volumeDimensions.y); //!! Set vieport to width and height of 3D texture!!
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        propagationShader.enable();
        b_firstPropStep = true;

        vMin.set(levels[level].getMin());
        cellSize = levels[level].getCellSize();

        //GLfloat data[4] = { 0.0f, 0.0f, 0.0f, 0.0f };
        //glClearTexImage(texManager["AccumulatorLPV"], 0, GL_RGBA, GL_FLOAT, &data[0]);
        //texManager.clear3Dtexture(texManager["AccumulatorLPV"]);
        texManager.clear3Dtexture(accumulatorCascadeTextures[level].red);
        texManager.clear3Dtexture(accumulatorCascadeTextures[level].green);
        texManager.clear3Dtexture(accumulatorCascadeTextures[level].blue);
        //texManager.clear3Dtexture(propTextures[1]);

        /*gl.glUniform1i(propagationShader("RAccumulatorLPV"), 0);  TODO
        gl.glUniform1i(propagationShader("GAccumulatorLPV"), 1);
        gl.glUniform1i(propagationShader("BAccumulatorLPV"), 2);

        gl.glUniform1i(propagationShader("RLightGridForNextStep"), 3);
        gl.glUniform1i(propagationShader("GLightGridForNextStep"), 4);
        gl.glUniform1i(propagationShader("BLightGridForNextStep"), 5);

        gl.glUniform1i(propagationShader("GeometryVolume"), 0);
        gl.glUniform1i(propagationShader("LPVGridR"), 1);
        gl.glUniform1i(propagationShader("LPVGridG"), 2);
        gl.glUniform1i(propagationShader("LPVGridB"), 3);
        //glUniform1i(propagationShader("b_firstPropStep"), b_firstPropStep);
        gl.glUniform3f(propagationShader("v_gridDim"), volumeDimensions.x, volumeDimensions.y, volumeDimensions.z);*/

        //glBindImageTexture(0, texManager["AccumulatorLPV"], 0, GL_TRUE, 0, GL_READ_WRITE, GL_RGBA16F);
        gl.glBindImageTexture(0, accumulatorCascadeTextures[level].red.getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);
        gl.glBindImageTexture(1, accumulatorCascadeTextures[level].green.getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);
        gl.glBindImageTexture(2, accumulatorCascadeTextures[level].blue.getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);
        //glBindImageTexture(5, texManager["GeometryVolume"], 0, GL_TRUE, 0, GL_READ_WRITE, GL_RGBA16F);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_3D, geometryInjectCascadeTextures[level].getTexture());

        if (b_useMultiStepPropagation ) {
            for (int i = 1; i < PROPAGATION_STEPS; i++) {
                //glUniform1i(propagationShader("AccumulatorLPV"), 0);
                if (i > 0)
                    b_firstPropStep = false;
//                glUniform1i(propagationShader("b_firstPropStep"), b_firstPropStep);  TODO
//                glUniform1i(propagationShader("b_useOcclusion"), b_useOcclusion);
                gl.glActiveTexture(GLenum.GL_TEXTURE1);
                gl.glBindTexture(GLenum.GL_TEXTURE_3D, propTextures[level][i - 1].red.getTexture());
                gl.glActiveTexture(GLenum.GL_TEXTURE2);
                gl.glBindTexture(GLenum.GL_TEXTURE_3D, propTextures[level][i - 1].green.getTexture());
                gl.glActiveTexture(GLenum.GL_TEXTURE3);
                gl.glBindTexture(GLenum.GL_TEXTURE_3D, propTextures[level][i - 1].blue.getTexture());

                gl.glBindImageTexture(3, propTextures[level][i].red.getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);
                gl.glBindImageTexture(4, propTextures[level][i].green.getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);
                gl.glBindImageTexture(5, propTextures[level][i].blue.getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);

                gl.glBindVertexArray(PropagationVAO);
                gl.glDrawArrays(GLenum.GL_POINTS, 0, volumeDimensionsMult);
                gl.glBindVertexArray(0);
            }

            for (int j = 1; j < PROPAGATION_STEPS; j++) {
                texManager.clear3Dtexture(propTextures[level][j].red);
                texManager.clear3Dtexture(propTextures[level][j].green);
                texManager.clear3Dtexture(propTextures[level][j].blue);
            }
        }
        else {
            //glBindImageTexture(3, propTextures[0], 0, GL_TRUE, 0, GL_READ_WRITE, GL_RGBA16F);
            //glBindImageTexture(4, propTextures[1], 0, GL_TRUE, 0, GL_READ_WRITE, GL_RGBA16F);
            gl.glActiveTexture(GLenum.GL_TEXTURE1);
            gl.glBindTexture(GLenum.GL_TEXTURE_3D, propTextures[level][0].red.getTexture());
            gl.glActiveTexture(GLenum.GL_TEXTURE2);
            gl.glBindTexture(GLenum.GL_TEXTURE_3D, propTextures[level][0].green.getTexture());
            gl.glActiveTexture(GLenum.GL_TEXTURE3);
            gl.glBindTexture(GLenum.GL_TEXTURE_3D, propTextures[level][0].blue.getTexture());

            gl.glBindImageTexture(3, propTextures[level][1].red.getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);
            gl.glBindImageTexture(4, propTextures[level][1].green.getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);
            gl.glBindImageTexture(5, propTextures[level][1].blue.getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);

//            gl.glUniform1i(propagationShader("b_firstPropStep"), b_firstPropStep);  TODO

            gl.glBindVertexArray(PropagationVAO);
            gl.glDrawArrays(GLenum.GL_POINTS, 0, volumeDimensionsMult);
            gl.glBindVertexArray(0);

            texManager.clear3Dtexture(propTextures[level][1].red);
            texManager.clear3Dtexture(propTextures[level][1].green);
            texManager.clear3Dtexture(propTextures[level][1].blue);
        }
        propagationShader.disable();
    }

    /* Propagation using geometry shader*/
    void propagate_layered(int level) {
        gl.glViewport(0, 0, volumeDimensions.x, volumeDimensions.y); //!! Set vieport to width and height of 3D texture!!
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        propagationShader_layered.enable();
        b_firstPropStep = true;

        vMin.set(levels[level].getMin());
        cellSize = levels[level].getCellSize();

        texManager.clear3Dtexture(accumulatorCascadeTextures[level].red);
        texManager.clear3Dtexture(accumulatorCascadeTextures[level].green);
        texManager.clear3Dtexture(accumulatorCascadeTextures[level].blue);

        /*glUniform1i(propagationShader_layered("GeometryVolume"), 0); todo
        glUniform1i(propagationShader_layered("LPVGridR"), 1);
        glUniform1i(propagationShader_layered("LPVGridG"), 2);
        glUniform1i(propagationShader_layered("LPVGridB"), 3);
        glUniform3f(propagationShader_layered("v_gridDim"), volumeDimensions.x, volumeDimensions.y, volumeDimensions.z);*/

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_3D, geometryInjectCascadeTextures[level].getTexture());

        if (b_useMultiStepPropagation ) {
            for (int i = 1; i < PROPAGATION_STEPS; i++) {
                if (i > 0)
                    b_firstPropStep = false;
                /*glUniform1i(propagationShader_layered("b_firstPropStep"), b_firstPropStep);todo
                glUniform1i(propagationShader_layered("b_useOcclusion"), b_useOcclusion);*/
                gl.glActiveTexture(GLenum.GL_TEXTURE1);
                gl.glBindTexture(GLenum.GL_TEXTURE_3D, propTextures[level][i - 1].red.getTexture());
                gl.glActiveTexture(GLenum.GL_TEXTURE2);
                gl.glBindTexture(GLenum.GL_TEXTURE_3D, propTextures[level][i - 1].green.getTexture());
                gl.glActiveTexture(GLenum.GL_TEXTURE3);
                gl.glBindTexture(GLenum.GL_TEXTURE_3D, propTextures[level][i - 1].blue.getTexture());

//                gl.glBindFramebuffer(GL_FRAMEBUFFER, propagationFBOs[level][i].getFboId());
                propagationFBOs[level][i].bind();
                gl.glEnable(GLenum.GL_BLEND);
                gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE);
                //Additive
                gl.glBlendEquation(GLenum.GL_FUNC_ADD);

                gl.glBindImageTexture(3, propTextures[level][i].red.getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);
                gl.glBindImageTexture(4, propTextures[level][i].green.getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);
                gl.glBindImageTexture(5, propTextures[level][i].blue.getTexture(), 0, true, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA16F);

                gl.glBindVertexArray(PropagationVAO);
                gl.glDrawArrays(GLenum.GL_POINTS, 0, volumeDimensionsMult);
                gl.glBindVertexArray(0);

                gl.glDisable(GLenum.GL_BLEND);
                gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            }

            for (int j = 1; j < PROPAGATION_STEPS; j++) {
                texManager.clear3Dtexture(propTextures[level][j].red);
                texManager.clear3Dtexture(propTextures[level][j].green);
                texManager.clear3Dtexture(propTextures[level][j].blue);
            }
        }
        else {
            gl.glActiveTexture(GLenum.GL_TEXTURE1);
            gl.glBindTexture(GLenum.GL_TEXTURE_3D, propTextures[level][0].red.getTexture());
            gl.glActiveTexture(GLenum.GL_TEXTURE2);
            gl.glBindTexture(GLenum.GL_TEXTURE_3D, propTextures[level][0].green.getTexture());
            gl.glActiveTexture(GLenum.GL_TEXTURE3);
            gl.glBindTexture(GLenum.GL_TEXTURE_3D, propTextures[level][0].blue.getTexture());

//            gl.glBindFramebuffer(GL_FRAMEBUFFER, propagationFBOs[level][1].getFboId());
            propagationFBOs[level][1].bind();
            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE);
            //Additive
            gl.glBlendEquation(GLenum.GL_FUNC_ADD);

//            gl.glUniform1i(propagationShader_layered("b_firstPropStep"), b_firstPropStep); todo

            gl.glBindVertexArray(PropagationVAO);
            gl.glDrawArrays(GLenum.GL_POINTS, 0, volumeDimensionsMult);
            gl.glBindVertexArray(0);

            gl.glDisable(GLenum.GL_BLEND);
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

            texManager.clear3Dtexture(propTextures[level][1].red);
            texManager.clear3Dtexture(propTextures[level][1].green);
            texManager.clear3Dtexture(propTextures[level][1].blue);
        }
        propagationShader_layered.disable();
    }
}
