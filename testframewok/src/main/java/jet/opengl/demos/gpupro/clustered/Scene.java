package jet.opengl.demos.gpupro.clustered;

import com.nvidia.developer.opengl.app.WindowEventListener;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.amdfx.common.CFirstPersonCamera;
import jet.opengl.demos.gpupro.rvi.SponzaMesh;
import jet.opengl.demos.nvidia.shadows.ShadowMapGenerator;
import jet.opengl.demos.nvidia.waves.samples.SkyBoxRender;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureCube;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.StackInt;

final class Scene {
    private String sceneID;
    private boolean slices = false;

    //TODO:: unify light model so that we can have a single array pointing to a base class (or single class)
    //so that we can iterate through it
    private DirectionalLight dirLight;
    private PointLight[] pointLights;

    //Tired of making things private, making them public as I go and we'll fix the rest later
    TextureCube irradianceMap, specFilteredMap;
    Texture2D brdfLUTTexture;
    SkyBox mainSkyBox;
    int pointLightCount;
    private boolean loadingError;
    private GLFuncProvider gl;

    //TODO:: No real reason this should be a pointer, it could be treated like the other objects
    //which just initialize their data separately from their constructor
    private CFirstPersonCamera mainCamera;

    //Contains the models that remain after frustrum culling which is TB done
    /*std::vector<Model*> visibleModels;
    std::vector<Model*> modelsInScene;*/

    SponzaMesh model;
    private final Matrix4f m_modelTransform = new Matrix4f();

    private static final String JSON_FOLDER = "E:\\SDK\\HybridRenderingEngine\\assets\\scenes\\";
    private static final String MODEL_FOLDER = "E:\\SDK\\HybridRenderingEngine\\assets\\models\\";

    private final Matrix4f m_tmp = new Matrix4f();

    //Builds scene using a string to a JSON scene description file
    Scene(String sceneFolder){
        //Load all cameras, models and lights and return false if it fails
        loadingError = !loadContent();

        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    //Updates all models, lights and cameras
    void update(int deltaT){
        /*visibleModels.clear();
        mainCamera.update(deltaT);
        //Light update could go here too
        for(Model *model : modelsInScene){
            model->update(deltaT);
        }*/
        frustrumCulling();
    }

    //Forward rendering drawing functions.
    //The reason these are called here and not inside the renderer class is that most of the data
    //is located within this class. There might be an opportunity to restructure this in a more data oriented
    //way, but I don't see how just yet. Possibly just keeping a struct of arrays of all the items in the scene?
    void drawPointLightShadow(GLSLProgram pointLightShader, int index, int cubeMapTarget){
        //Current light
        PointLight light = pointLights[index];
        light.depthMapTextureID = cubeMapTarget;
        //Shader setup
        pointLightShader.enable();
        /*pointLightShader.setVec3("lightPos", light->position);
        pointLightShader.setFloat("far_plane", light->zFar);*/

        int loc = pointLightShader.getUniformLocation("lightPos");
        gl.glUniform3f(loc, light.position.x,light.position.y, light.position.z);
        loc = pointLightShader.getUniformLocation("far_plane");
        gl.glUniform1f(loc, light.zFar);

        //Matrix setup
//        glm::mat4 lightMatrix, M;
        Matrix4f shadowProj = light.shadowProjectionMat;
        for (int face = 0; face < 6; ++face){
            /*std::string number = std::to_string(face);
            lightMatrix = shadowProj * light->lookAtPerFace[face];
            pointLightShader.setMat4(("shadowMatrices[" + number + "]").c_str(), lightMatrix);*/

            Matrix4f lightMatrix = Matrix4f.mul(shadowProj, light.lookAtPerFace[face], m_tmp);
            loc = pointLightShader.getUniformLocation("shadowMatrices[" + face + "]");
            gl.glUniform4fv(loc, CacheBuffer.wrap(lightMatrix));
        }

        /*for(int i = 0; i < modelsInScene.size(); ++i){
            Model * currentModel = modelsInScene[i];

            M = currentModel->modelMatrix;
            //Shader setup stuff that changes every frame
            pointLightShader.setMat4("M", M);

            //Draw object
            currentModel->draw(pointLightShader, false);
        }*/

        loc = pointLightShader.getUniformLocation("M");
        gl.glUniform4fv(loc, CacheBuffer.wrap(m_modelTransform));

//        model.draw() ; todo
    }

    //Currently assumes there's only one directional light, also uses the simplest shadow map algorithm
    //that leaves a lot to be desired in terms of resolution, thinking about moving to cascaded shadow maps
    //or maybe variance idk yet.
    void drawDirLightShadows(GLSLProgram dirLightShader, int targetTextureID){
//        glm::mat4 ModelLS = glm::mat4(1.0);
        dirLight.depthMapTextureID = targetTextureID;

        float left = dirLight.orthoBoxSize;
        float right = -left;
        float top = left;
        float bottom = -top;
        Matrix4f.ortho(left, right, bottom, top, dirLight.zNear, dirLight.zFar, dirLight.shadowProjectionMat);
        Matrix4f.lookAt(-dirLight.direction.x, -dirLight.direction.y, -dirLight.direction.z,
                0,0,0,0,1,0, dirLight.lightView);

//        dirLight.lightSpaceMatrix = dirLight.shadowProjectionMat * dirLight.lightView;
        Matrix4f.mul(dirLight.shadowProjectionMat, dirLight.lightView, dirLight.lightSpaceMatrix);

        //Drawing every object into the shadow buffer
        /*for(unsigned int i = 0; i < modelsInScene.size(); ++i){
            Model * currentModel = modelsInScene[i];

            //Matrix setup
            ModelLS = dirLight.lightSpaceMatrix * currentModel->modelMatrix;

            //Shader setup stuff that changes every frame
            dirLightShader.use();
            dirLightShader.setMat4("lightSpaceMatrix", ModelLS);

            //Draw object
            currentModel->draw(dirLightShader, false);
        }*/

        Matrix4f ModelLS = Matrix4f.mul(dirLight.lightSpaceMatrix, m_modelTransform, m_tmp);
        int loc = dirLightShader.getUniformLocation("lightSpaceMatrix");
        gl.glUniform4fv(loc, CacheBuffer.wrap(ModelLS));
    }

    //Sets up the common uniforms for each model and loaded all texture units. A lot of driver calls here
    //Re-watch the beyond porting talk to try to reduce api calls. Specifically texture related calls.
    void drawFullScene(GLSLProgram mainSceneShader, GLSLProgram skyboxShader){
        //Matrix Setup
        /*glm::mat4 MVP = glm::mat4(1.0);  todo  setup matrices
        glm::mat4 M   = glm::mat4(1.0);
        glm::mat4 VP  = mainCamera->projectionMatrix * mainCamera->viewMatrix;
        glm::mat4 VPCubeMap = mainCamera->projectionMatrix *glm::mat4(glm::mat3(mainCamera->viewMatrix));*/

        //Just to avoid magic constants
        final int numTextures =  5;

        //Setting colors in the gui
        /*if(ImGui::CollapsingHeader("Directional Light Settings")){
            ImGui::TextColored(ImVec4(1,1,1,1), "Directional light Settings");
            ImGui::ColorEdit3("Color", (float *)&dirLight.color);
            ImGui::SliderFloat("Strength", &dirLight.strength, 0.1f, 200.0f);
            ImGui::SliderFloat("BoxSize", &dirLight.orthoBoxSize, 0.1f, 500.0f);
            ImGui::SliderFloat3("Direction", (float*)&dirLight.direction, -5.0f, 5.0f);
        }*/

        mainSceneShader.enable();
        /*if(ImGui::CollapsingHeader("Cluster Debugging Light Settings")){
            ImGui::Checkbox("Display depth Slices", &slices);
        }*/


        GLSLUtil.setFloat3(mainSceneShader, "dirLight.direction", dirLight.direction);
        GLSLUtil.setBool(mainSceneShader, "slices", slices);
        GLSLUtil.setFloat3(mainSceneShader,"dirLight.color",   dirLight.strength * dirLight.color.x, dirLight.strength * dirLight.color.y, dirLight.strength * dirLight.color.z);
        GLSLUtil.setMat4(mainSceneShader,"lightSpaceMatrix", dirLight.lightSpaceMatrix);
        GLSLUtil.setFloat3(mainSceneShader, "cameraPos_wS", mainCamera.GetEyePt());
        GLSLUtil.setFloat(mainSceneShader,"zFar", mainCamera.GetFarClip());
        GLSLUtil.setFloat(mainSceneShader,"zNear", mainCamera.GetNearClip());

        for (int i = 0; i < pointLightCount; ++i)
        {
            PointLight light = pointLights[i];
//            std::string number = std::to_string(i);

            gl.glActiveTexture(GLenum.GL_TEXTURE0 + numTextures + i);
            GLSLUtil.setInt(mainSceneShader, ("depthMaps[" + i + "]"), numTextures + i);
            gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, light.depthMapTextureID);
            GLSLUtil.setFloat(mainSceneShader,"far_plane", light.zFar);
        }

        //Setting directional shadow depth map textures
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + numTextures + pointLightCount);
        GLSLUtil.setInt(mainSceneShader,"shadowMap", numTextures + pointLightCount);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, dirLight.depthMapTextureID);

        //TODO:: Formalize htis a bit more
        //Setting environment map texture
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + numTextures + pointLightCount + 1);
        GLSLUtil.setInt(mainSceneShader,"irradianceMap", numTextures + pointLightCount + 1);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, irradianceMap.getTexture());

        //Setting environment map texture for specular
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + numTextures + pointLightCount + 2);
        GLSLUtil.setInt(mainSceneShader,"prefilterMap", numTextures + pointLightCount + 2);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, specFilteredMap.getTexture());

        //Setting lookup table
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + numTextures + pointLightCount + 3);
        GLSLUtil.setInt(mainSceneShader,"brdfLUT", numTextures + pointLightCount + 3);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, brdfLUTTexture.getTexture());

        /*for(int i = 0; i < visibleModels.size(); ++i){ todo render model
            Model * currentModel = visibleModels[i];

            //Matrix setup
            M  = currentModel->modelMatrix;
            MVP = VP * M;

            //Shader setup stuff that changes every frame
            mainSceneShader.setMat4("MVP", MVP);
            mainSceneShader.setMat4("M", M);

            //Draw object
            currentModel->draw(mainSceneShader, true);
        }*/

        //Drawing skybox
        /*skyboxShader.enable(); todo render sky box.
        skyboxShader.setMat4("VP", VPCubeMap);*/
        mainSkyBox.draw();
    }

    //Very simple setup that iterates through all objects and draws their depth value to a buffer
    //Optimization is very possible here, specifically because we draw all items.
    void drawDepthPass(GLSLProgram depthPassShader){
        //Matrix Setup
        /*glm::mat4 MVP = glm::mat4(1.0);  todo
        glm::mat4 VP  = mainCamera->projectionMatrix * mainCamera->viewMatrix;

        //Drawing every object into the depth buffer
        for(int i = 0; i < modelsInScene.size(); ++i){
            Model * currentModel = modelsInScene[i];

            //Matrix setup
            MVP = VP * currentModel->modelMatrix;

            //Shader setup stuff that changes every frame
            depthPassShader.use();
            depthPassShader.setMat4("MVP", MVP);

            //Draw object
            currentModel->draw(depthPassShader, false);
        }*/

        throw new UnsupportedOperationException();
    }

    //Getters used in the setup of the render queue
//    std::vector<Model*>* getVisiblemodels();
    CFirstPersonCamera getCurrentCamera() { return mainCamera;}
    PointLight getPointLight(int index) { return pointLights[index];}
    int getShadowRes() { return dirLight.shadowRes;}

    //Scene loading functions and environment map generation (but not actually filling the memory just yet)
    private boolean loadContent(){
        //Parsing into Json file readable format
        /*std::string folderPath = "../assets/scenes/";
        std::string fileExtension = ".json";
        std::string sceneConfigFilePath = folderPath + sceneID + fileExtension;
        std::ifstream file(sceneConfigFilePath.c_str());
        json configJson;
        file >> configJson;*/

        //Checking that config file belongs to current scene and is properly formatted
        /*if (configJson["sceneID"] != sceneID & ((unsigned int)configJson["models"].size() != 0)){
            printf("Error! Config file: %s does not belong to current scene, check configuration.\n", sceneConfigFilePath.c_str());
            return false;
        }*/

        sceneID = "Sponza";

        //now we parse the rest of the file, but don't do any other checks. It would be worth it to
        //have a preliminary check that looks at the content of the scene description file and only then
        //decides what to load and what to generate incase it can't find the data, because right now
        //if you can't find the data it will just crash. So a check for correct formatting might not only
        //make sense in a correctness based
        LogUtil.i(LogUtil.LogType.DEFAULT,"Loading camera...\n");
        loadCamera(/*configJson*/);

        LogUtil.i(LogUtil.LogType.DEFAULT,"Loading models...\n");
        loadSceneModels(/*configJson*/);

        LogUtil.i(LogUtil.LogType.DEFAULT,"Loading skybox...\n");
//        CubeMap::cubeMapCube.setup();
        loadSkyBox(/*configJson*/);

        LogUtil.i(LogUtil.LogType.DEFAULT,"Loading lights...\n");
        loadLights(/*configJson*/);

        LogUtil.i(LogUtil.LogType.DEFAULT,"Generating environment maps...\n");
        generateEnvironmentMaps();

        LogUtil.i(LogUtil.LogType.DEFAULT,"Reticulating splines...\n");

        //lastly we check if the scene is empty and return
        LogUtil.i(LogUtil.LogType.DEFAULT,"Loading Complete!...\n");
        return true;
    }

    static final int
            SHADOW_MAP = 0,
            HDR_MAP = 1,
            PREFILTER_MAP = 2;

    static TextureCube createCubeMap(int width, int height, int cubeType){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        int ID = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, ID);
        final int numSidesInCube = 6;

        switch(cubeType){
            case SHADOW_MAP:
                for (int i = 0; i < numSidesInCube; ++i){
                    gl.glTexImage2D(GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
                        0, GLenum.GL_DEPTH_COMPONENT24, width, height, 0,
                            GLenum.GL_DEPTH_COMPONENT, GLenum.GL_FLOAT, null);
            }
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_NEAREST);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_NEAREST);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_COMPARE_MODE, GLenum.GL_COMPARE_REF_TO_TEXTURE);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
            break;

            case HDR_MAP:
                for (int i = 0; i < numSidesInCube; ++i){
                    gl.glTexImage2D(GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
                        0, GLenum.GL_RGB32F,
                        width, height, 0,
                            GLenum.GL_RGB, GLenum.GL_FLOAT, null);
            }
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_EDGE);
            break;

            case PREFILTER_MAP:
                for ( int i = 0; i < numSidesInCube; ++i){
                    gl.glTexImage2D(GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
                        0, GLenum.GL_RGB16F,
                        width, height, 0,
                            GLenum.GL_RGB, GLenum.GL_FLOAT, null);
            }
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);

            //For the specular IBL component we use the mipmap levels to store increasingly
            //rougher representations of the environment. And then interpolater between those
            gl.glGenerateMipmap(GLenum.GL_TEXTURE_CUBE_MAP);
//            maxMipLevels = 5;
            break;
        }

        return TextureUtils.createTextureCube(GLenum.GL_TEXTURE_CUBE_MAP, ID);
    }

    private void generateEnvironmentMaps(){
        //Diffuse map
        /*irradianceMap.width = 32;
        irradianceMap.height = 32;
        irradianceMap.generateCubeMap(irradianceMap.width, irradianceMap.height, HDR_MAP);*/

        irradianceMap = createCubeMap(32, 32, HDR_MAP);

        //Specular map
        /*specFilteredMap.width = 128;
        specFilteredMap.height = 128;
        specFilteredMap.generateCubeMap(specFilteredMap.width, specFilteredMap.height, PREFILTER_MAP);*/

        specFilteredMap = createCubeMap(128, 128, PREFILTER_MAP);

        //Setting up texture ahead of time
        int res = 512;
        /*brdfLUTTexture.height = res;
        brdfLUTTexture.width  = res;
        glGenTextures(1, &brdfLUTTexture.textureID);
        glBindTexture(GL_TEXTURE_2D, brdfLUTTexture.textureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RG16F, res, res, 0, GL_RG, GL_FLOAT, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);*/

        Texture2DDesc desc = new Texture2DDesc(res, res, GLenum.GL_RG16F);
        brdfLUTTexture = TextureUtils.createTexture2D(desc, null);
    }

    private void loadSkyBox(/*const json &sceneConfigJson*/){
        String skyBoxName = "barcelona";
        boolean isHDR = true;
        int resolution = 512;
    }

    private void loadLights(/*const json &sceneConfigJson*/){
        dirLight.direction.set(0, -5.f, 1.333f);
        dirLight.distance = 100;
        dirLight.color.set(1, 1, 1);
        dirLight.strength = 1;
        dirLight.zNear = 1;
        dirLight.zFar = 700;
        dirLight.orthoBoxSize = 100;
        dirLight.shadowRes = 2048;

        //Matrix values
        float left   = dirLight.orthoBoxSize;
        float right  = -left;
        float top    = left;
        float bottom = -top;
        //I'm not sure yet why we have to multiply by the distance here, I understand that if I don't much of the
        //screen won't be shown, but I am confused as this goes against my understanding of how an orthographic
        //projection works. This will have to be reviewed at a later point.
        Matrix4f.ortho(left, right, bottom, top, dirLight.zNear, dirLight.zFar, dirLight.shadowProjectionMat);
        Matrix4f.lookAt(-dirLight.direction.x, -dirLight.direction.y, -dirLight.direction.z,
                0.0f, 0.0f, 0.0f,0.0f, 1.0f, 0.0f, dirLight.lightView);

        Matrix4f.mul(dirLight.shadowProjectionMat, dirLight.lightView, dirLight.lightSpaceMatrix);

        // Point lights
        pointLightCount = 4;
        pointLights = new PointLight[4];
        pointLights[0] = new PointLight();
        pointLights[0].position.set(-110.0f, 14.0f, -41.0f);
        pointLights[0].color.set(1.f, 0.f, 0.f);
        pointLights[0].strength = 1;
        pointLights[0].zNear = 0.1f;
        pointLights[0].zFar = 100.f;
        pointLights[0].shadowRes = 2048;

        pointLights[1] = new PointLight();
        pointLights[1].position.set(110.0f, 14.0f, -41.0f);
        pointLights[1].color.set(0, 1, 0);
        pointLights[1].strength = 1;
        pointLights[1].zNear = 0.1f;
        pointLights[1].zFar = 100.f;
        pointLights[1].shadowRes = 2048;

        pointLights[2] = new PointLight();
        pointLights[2].position.set(110, 16, 41);
        pointLights[2].color.set(0, 1, 0);
        pointLights[2].strength = 1;
        pointLights[2].zNear = 0.1f;
        pointLights[2].zFar = 100.f;
        pointLights[2].shadowRes = 2048;

        pointLights[3] = new PointLight();
        pointLights[3].position.set(-110, 16, 41);
        pointLights[3].color.set(1, 0, 1);
        pointLights[3].strength = 1;
        pointLights[3].zNear = 0.1f;
        pointLights[3].zFar = 100.f;
        pointLights[3].shadowRes = 2048;

        for(int i = 0; i < pointLights.length; i++){
            ShadowMapGenerator.buildCubeShadowMatrices(pointLights[i].position, pointLights[i].zNear, pointLights[i].zFar, pointLights[i].shadowProjectionMat, pointLights[i].lookAtPerFace);
        }
    }

    private void loadCamera(/*const json &sceneConfigJson*/){
//        mainCamera
    }

    private void loadSceneModels(/*const json &sceneConfigJson*/){
        m_modelTransform.scale(0.1f);
        model = new SponzaMesh();
    }

    //Builds the list of meshes that are visible
    //Currently disabled while waiting for rework of model mesh loader and material system
    //Since we want frustum culling to work per mesh now, instead of per model.
    private void frustrumCulling(){

    }
}
