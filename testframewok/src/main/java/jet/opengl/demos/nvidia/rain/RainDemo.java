package jet.opengl.demos.nvidia.rain;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.NvImage;

import static jet.opengl.postprocessing.util.Numeric.random;

/**
 * Created by mazhen'gui on 2017/6/30.
 */

public class RainDemo extends NvSampleApp {
    //presets for the variables
    final Vector3f    g_finalFogVector1 = new Vector3f(0.03f,0.03f,0.03f);
    final float       g_finalDirLightIntensity1 = 0.18f;
    final float       g_finalResponseDirLight1 = 1.0f;
    final float       g_finalPointLightIntensity1 = 0.5f;
    final float       g_finalCosSpotLight1 = 0.3f;
    final float       g_finalResponsePointLight1 = 2.0f;
    final float       g_finalDrawFraction1 = 0.7f;
    final float       g_finalWindAmount1 = 1.0f;
    final Vector3f    g_finalVecEye1 = new Vector3f( 15.5f, 5.0f, 0.0f );
    final Vector3f    g_finalAtVec1  = new Vector3f( 0.0f, 3.0f, 0.0f );
    final Vector3f    g_directionalLightVector1 = new Vector3f(0.551748f, 0.731354f, 0.400869f);

    final Vector3f    g_finalFogVector2 = new Vector3f(0.02f,0.02f,0.02f);
    final float       g_finalDirLightIntensity2 = 0.50f;
    final float       g_finalResponseDirLight2 = 0.68f;
    final float       g_finalPointLightIntensity2 = 0.4f;
    final float       g_finalCosSpotLight2 = 0.54f;
    final float       g_finalResponsePointLight2 = 2.0f;
    final float       g_finalDrawFraction2 = 0.6f;
    final float       g_finalWindAmount2 = 0.48f;
    final Vector3f    g_finalVecEye2 = new Vector3f( -4.0f, 18.0f, -5.0f );
    final Vector3f    g_finalAtVec2  = new Vector3f( 0.0f, 0.0f, 0.0f );
    final Vector3f    g_directionalLightVector2 = new Vector3f(0.470105f, 0.766044f, 0.43838f);

    RenderTechnique   g_pTechniqueRenderSky;
    RenderTechnique   g_pTechniqueRenderScene;
    RenderTechnique   g_pTechniqueAdvanceRain;
    RenderTechnique   g_pTechniqueDrawRainCheap;
    RenderTechnique   g_pTechniqueDrawRain;

    int               g_pParticleStartVAO = 0;
    int               g_pParticleStart    = 0;
    TransformFeedbackObject   g_pParticleStreamTo  = null;
    TransformFeedbackObject   g_pParticleDrawFrom  = null;

    XMesh             g_Mesh;
    XMesh             g_MeshArrow;
    int               g_VertexBufferSky;
    int               g_VertexBufferSkyVAO;

    Texture2D         m_SceneTextureDiffuse;
    Texture2D         m_SceneTextureSpecular;
    Texture2D         m_SceneTextureNormal;
    Texture2D         m_RainTextureArray;

    Texture2D         m_Ftable;
    Texture2D         m_Gtable;

    Texture3D         m_SplashBumpTexture;
    Texture3D         m_SplashDiffuseTexture;

    float g_SpriteSize = 0.8f;
    float g_dirLightRadius = 1000;
    boolean g_bRenderBg = true;
    boolean g_bMoveParticles = true;
    boolean g_bDrawParticles = true;
    int g_numRainVertices = 150000;
    float g_dirLightIntensity = 0.27f;
    float g_PointLightIntensity = 0.58f;
    float g_znear = 1.0f;
    float g_zfar  = 30000.0f;
    float g_fov =  (float)Math.toDegrees(0.3* Numeric.PI);
    float g_cosSpotLight = 0.54f;
    boolean g_bUseSpotLight = true;
    float g_responseDirLight = 0.9f;
    float g_responsePointLight = 2.0f;
    float g_heightMin = 0.0f;
    float g_heightRange = 40.0f;
    float g_radiusMin = 0.0f;
    float g_radiusRange = 45.0f;
    boolean g_bUseCheapShader = false;
    boolean firstFrame;
    int frameCount = 0;
    float g_WindAmount;
    float g_DrawFraction = 1.0f;
    final Vector3f g_fogVector = new Vector3f();
    final Vector3f g_vecEye = new Vector3f();
    final Vector3f g_vecAt = new Vector3f();
    final Vector3f g_lightPos = new Vector3f();
    final Vector3f g_eyeInObjectSpace = new Vector3f();
    final Vector3f g_TotalVel = new Vector3f(-0.05f,-0.5f,0f);
    final Vector3f g_PointLightPos = new Vector3f(  3.7f,5.8f,3.15f);
    final Vector3f g_PointLightPos2 = new Vector3f(-3.7f,5.8f,3.15f);
    final Matrix4f g_WorldMatrix = new Matrix4f();

    final RainParams m_RainParams = new RainParams();

    final List<WindValue> WindAnimation = new ArrayList<>();
    int totalAnimationValues = 0;
    boolean        g_PrintProgramOnce;

    private GLFuncProvider gl;

    @Override
    protected void initRendering() {
        getGLContext().setSwapInterval(0);
        gl = GLFuncProviderFactory.getGLFuncProvider();
        resetVariablesToPreset1();

        m_transformer.setTranslation(-g_finalVecEye1.x, -g_finalVecEye1.y, -g_finalVecEye1.z);
        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);

        //populate the wind animation
        WindAnimation.clear();
        int time = 0; //time in seconds between each key
        WindAnimation.add(new WindValue( new Vector3f(-0.1f,-0.5f,0f),   time ) );
        WindAnimation.add(new WindValue( new Vector3f(-0.4f,-0.5f,0.04f), time += 10 ) );
        WindAnimation.add(new WindValue( new Vector3f(-0.2f,-0.5f,-0.4f),   time += 5 ) );
        WindAnimation.add(new WindValue( new Vector3f(0.0f,-0.5f,-0.02f), time += 10 ) );
        WindAnimation.add(new WindValue( new Vector3f(0.0f,-0.5f,-0.02f), time += 10 ) );
        WindAnimation.add(new WindValue( new Vector3f(0.1f,-0.5f,0.4f),  time += 6) );
        WindAnimation.add(new WindValue( new Vector3f(-0.1f,-0.5f,0f),   time += 5 ) );

        setShadingParametersBasedOnRain();

        // shader programs
        g_pTechniqueRenderSky = new RenderTechnique("SkyVS.vert", null, "SkyPS.frag");
        g_pTechniqueRenderSky.printPrograminfo();

        g_pTechniqueRenderScene = new RenderTechnique("SceneVS.vert", "SceneGS.gemo", "ScenePS.frag");
        g_pTechniqueAdvanceRain = new RenderTechnique("AdvanceRainVS.vert", "StreamOutGS.gemo", null, this::bindFeedback);
        g_pTechniqueDrawRainCheap = new RenderTechnique("ParticleVS.vert", "RenderRainCheapGS.gemo", "RenderRainCheapPS.frag");
        g_pTechniqueDrawRain = new RenderTechnique("ParticleVS.vert", "ParticleGS.gemo", "ParticlePS.frag");
        GLCheck.checkError();

        //----------------------------------------------------------------------------------------------
        //vertex buffers
        //----------------------------------------------------------------------------------------------
//        g_MeshArrow = new XMesh("arrow_mesh", 2, GLenum.GL_UNSIGNED_SHORT);
        g_Mesh      = new XMesh("Bridge", 1, GLenum.GL_UNSIGNED_INT);

        //vertices for the rain particles---------------------------------------------------------------
        //generate vertices in a cylinder above the camera
        firstFrame = true;
        RainVertex[] vertices = new RainVertex[g_numRainVertices];

        for(int i=0;i<g_numRainVertices;i++)
        {
            RainVertex raindrop = new RainVertex();
            //use rejection sampling to generate random points inside a circle of radius 1 centered at 0,0
            float SeedX = 0;
            float SeedZ = 0;
            boolean pointIsInside = false;
            while(!pointIsInside)
            {
                SeedX = random() - 0.5f;
                SeedZ = random() - 0.5f;
                if( Math.sqrt( SeedX*SeedX + SeedZ*SeedZ ) <= 0.5 )
                    pointIsInside = true;
            }
            //save these random locations for reinitializing rain particles that have fallen out of bounds
            SeedX *= g_radiusRange;
            SeedZ *= g_radiusRange;
            float SeedY = random()*g_heightRange;
            raindrop.seed.set(SeedX,SeedY,SeedZ);

            //add some random speed to the particles, to prevent all the particles from following exactly the same trajectory
            //additionally, random speeds in the vertical direction ensure that temporal aliasing is minimized
            float SpeedX = 40.0f*(random()/20.0f);
            float SpeedZ = 40.0f*(random()/20.0f);
            float SpeedY = 40.0f*(random()/10.0f);
            raindrop.speed.set(SpeedX,SpeedY,SpeedZ);

            //move the rain particles to a random positions in a cylinder above the camera
            float x = SeedX + g_vecEye.x;
            float z = SeedZ + g_vecEye.z;
            float y = SeedY + g_vecEye.y;
            raindrop.pos.set(x,y,z);

            //get an integer between 1 and 8 inclusive to decide which of the 8 types of rain textures the particle will use
            raindrop.Type = (int)(Math.floor(  random()*8 + 1 ));

            //this number is used to randomly increase the brightness of some rain particles
            raindrop.random = 1;
            float randomIncrease = random();
            if( randomIncrease > 0.8)
                raindrop.random += randomIncrease;

            vertices[i] = raindrop;
        }


        //create vertex buffers for the rain, two will be used to pingpong between during animation
        final int ByteWidth = RainVertex.SIZE * g_numRainVertices;
        ByteBuffer rainVertexData = CacheBuffer.getCachedByteBuffer(ByteWidth);
        for(RainVertex vertex : vertices){
            vertex.store(rainVertexData);
        }
        rainVertexData.flip();

        Runnable g_pVertexLayoutRainVertex = ()->
        {
            // POSITION
            gl.glEnableVertexAttribArray(0);
            gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, RainVertex.SIZE, 0);
            // SEED
            gl.glEnableVertexAttribArray(1);
            gl.glVertexAttribPointer(1, 3, GLenum.GL_FLOAT, false, RainVertex.SIZE, Vector3f.SIZE);
            // SPEED
            gl.glEnableVertexAttribArray(2);
            gl.glVertexAttribPointer(2, 3, GLenum.GL_FLOAT, false, RainVertex.SIZE, Vector3f.SIZE * 2);
            // RAND
            gl.glEnableVertexAttribArray(3);
            gl.glVertexAttribPointer(3, 1, GLenum.GL_FLOAT, false, RainVertex.SIZE, Vector3f.SIZE * 3);
            // TYPE
            gl.glEnableVertexAttribArray(4);
            gl.glVertexAttribIPointer(4, 1, GLenum.GL_UNSIGNED_INT, RainVertex.SIZE, Vector3f.SIZE * 3 + 4);
        };

        {
            g_pParticleStartVAO = gl.glGenVertexArray();
            gl.glBindVertexArray(g_pParticleStartVAO);
            g_pParticleStart =gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, g_pParticleStart);
            gl.glBufferData(GLenum.GL_ARRAY_BUFFER, rainVertexData, GLenum.GL_STREAM_DRAW);
            g_pVertexLayoutRainVertex.run();
            gl.glBindVertexArray(0);
        }

        {
            g_pParticleDrawFrom = new TransformFeedbackObject(new int[]{ByteWidth}, new Runnable[]{g_pVertexLayoutRainVertex});
            g_pParticleStreamTo = new TransformFeedbackObject(new int[]{ByteWidth}, new Runnable[]{g_pVertexLayoutRainVertex});
        }

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        vertices = null;
        GLCheck.checkError();

        // Create the vertex input layout for rain
//        const D3D10_INPUT_ELEMENT_DESC layout[] =
//                {
//                        { "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0,  D3D10_INPUT_PER_VERTEX_DATA, 0 },
//                        { "SEED",     0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 12,  D3D10_INPUT_PER_VERTEX_DATA, 0 },
//                        { "SPEED",    0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 24,  D3D10_INPUT_PER_VERTEX_DATA, 0 },
//                        { "RAND",     0, DXGI_FORMAT_R32_FLOAT,       0, 36, D3D10_INPUT_PER_VERTEX_DATA, 0 },
//                        { "TYPE",     0, DXGI_FORMAT_R8_UINT,         0, 40, D3D10_INPUT_PER_VERTEX_DATA, 0 },
//                };
//        UINT numElements = sizeof(layout)/sizeof(layout[0]);
//        D3D10_PASS_DESC PassDesc;
//        g_pTechniqueDrawRain->GetPassByIndex( 0 )->GetDesc( &PassDesc );
//        V_RETURN(pd3dDevice->CreateInputLayout( layout, numElements, PassDesc.pIAInputSignature, PassDesc.IAInputSignatureSize, &g_pVertexLayoutRainVertex ));

        //---------------------------------------------------------------------------------------------

        //scene mesh

        // Create the input layout for the scene
//        const D3D10_INPUT_ELEMENT_DESC layoutScene[] =
//                {
//                        { "POSITION",  0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D10_INPUT_PER_VERTEX_DATA, 0 },
//                        { "NORMAL",    0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 12, D3D10_INPUT_PER_VERTEX_DATA, 0 },
//                        { "TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT, 0, 24, D3D10_INPUT_PER_VERTEX_DATA, 0 },
//                        { "TANGENT",   0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 32, D3D10_INPUT_PER_VERTEX_DATA, 0 },
//                };
//        UINT numElementsScene = sizeof(layoutScene)/sizeof(layoutScene[0]);
//        g_pTechniqueRenderScene->GetPassByIndex( 0 )->GetDesc( &PassDesc );
//        V_RETURN( pd3dDevice->CreateInputLayout( layoutScene, numElementsScene, PassDesc.pIAInputSignature, PassDesc.IAInputSignatureSize, &g_pVertexLayoutScene ) );
//        // Set the input layout
//        pd3dDevice->IASetInputLayout( g_pVertexLayoutScene );
//        //load the scene  TODO
//        V_RETURN(NVUTFindDXSDKMediaFileCch(str, MAX_PATH, L"..\\..\\Media\\Bridge\\Bridge.x"));
//        V_RETURN( g_Mesh.Create( pd3dDevice,str, (D3D10_INPUT_ELEMENT_DESC*)layoutScene, numElementsScene ) );


        //---------------------------------------------------------------------------------------------
        //mesh and layout for arrow

        // Create the input layout
//        const D3D10_INPUT_ELEMENT_DESC layoutArrow[] =
//                {
//                        { "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D10_INPUT_PER_VERTEX_DATA, 0 },
//                        { "NORMAL", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 12, D3D10_INPUT_PER_VERTEX_DATA, 0 },
//                        { "TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT, 0, 24, D3D10_INPUT_PER_VERTEX_DATA, 0 },
//                };
//        numElements = sizeof(layoutArrow)/sizeof(layoutArrow[0]);
//        g_pTechniqueRenderArrow->GetPassByIndex( 0 )->GetDesc( &PassDesc );
//        V_RETURN( pd3dDevice->CreateInputLayout( layoutArrow, numElements, PassDesc.pIAInputSignature, PassDesc.IAInputSignatureSize, &g_pVertexLayoutArrow ) );
        //load the arrow mesh  TODO
//        V_RETURN( NVUTFindDXSDKMediaFileCch( str, MAX_PATH, L"arrow.x" ) );
//        V_RETURN( g_MeshArrow.Create( pd3dDevice, str, (D3D10_INPUT_ELEMENT_DESC*)layoutArrow, numElements ) );

        //-------------------------------------------------------------------------------------------------
        //vertex buffer and layout for sky

        // Create the input layout
//        D3D10_INPUT_ELEMENT_DESC    layoutSky[] =
//                {
//                        { "position", 0, DXGI_FORMAT_R32G32_FLOAT, 0, 0, D3D10_INPUT_PER_VERTEX_DATA, 0 },
//                };
//        numElements = sizeof(layoutSky)/sizeof(layoutSky[0]);
//        g_pTechniqueRenderSky->GetPassByIndex( 0 )->GetDesc( &PassDesc );
//        V_RETURN( pd3dDevice->CreateInputLayout( layoutSky, numElements, PassDesc.pIAInputSignature, PassDesc.IAInputSignatureSize, &g_pVertexLayoutSky ) );

        //make the vertex buffer for the quad that will be used for the sky
//        D3DXVECTOR2 verticesSky[4];
//        verticesSky[0] = D3DXVECTOR2( 1.0f,  1.0f);
//        verticesSky[1] = D3DXVECTOR2( 1.0f, -1.0f);
//        verticesSky[2] = D3DXVECTOR2(-1.0f,  1.0f);
//        verticesSky[3] = D3DXVECTOR2(-1.0f, -1.0f);
//        D3D10_SUBRESOURCE_DATA InitDataSky;
//        InitDataSky.pSysMem  = verticesSky;
//        D3D10_BUFFER_DESC      bdSky;
//        bdSky.Usage          = D3D10_USAGE_IMMUTABLE;
//        bdSky.ByteWidth      = sizeof( D3DXVECTOR2 ) * 4;
//        bdSky.BindFlags      = D3D10_BIND_VERTEX_BUFFER;
//        bdSky.CPUAccessFlags = 0;
//        bdSky.MiscFlags      = 0;
//        V_RETURN(pd3dDevice->CreateBuffer( &bdSky, &InitDataSky, &g_VertexBufferSky ));

        g_VertexBufferSkyVAO = gl.glGenVertexArray();
        gl.glBindVertexArray(g_VertexBufferSkyVAO);
        {
            g_VertexBufferSky = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, g_VertexBufferSky);
            FloatBuffer verticesSky = CacheBuffer.getCachedFloatBuffer(8);
            verticesSky.put(1.0f).put(1.0f);
            verticesSky.put(1.0f).put(-1.0f);
            verticesSky.put(-1.0f).put(1.0f);
            verticesSky.put(-1.0f).put(-1.0f);
            verticesSky.flip();
            gl.glBufferData(GLenum.GL_ARRAY_BUFFER, verticesSky, GLenum.GL_STATIC_DRAW);
            gl.glEnableVertexAttribArray(0);
            gl.glVertexAttribPointer(0, 2, GLenum.GL_FLOAT, false, 0, 0);
        }
        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        GLCheck.checkError();

        //---------------------------------------------------------------------------------------------
        //textures
        //---------------------------------------------------------------------------------------------

        //the textures for the bridge
//        V_RETURN(loadTextureFromFile(L"../../Media/Bridge/bridge_color.dds","SceneTextureDiffuse",pd3dDevice));
//        V_RETURN(loadTextureFromFile(L"../../Media/Bridge/bridge_spec.dds","SceneTextureSpecular",pd3dDevice));
//        V_RETURN(loadTextureFromFile(L"../../Media/Bridge/bridge_normal.dds","SceneTextureNormal",pd3dDevice));

        try {
            int SceneTextureDiffuse = NvImage.uploadTextureFromDDSFile("nvidia/Rain/textures/bridge_color.dds");
            int SceneTextureSpecular = NvImage.uploadTextureFromDDSFile("nvidia/Rain/textures/bridge_spec.dds");
            int SceneTextureNormal = NvImage.uploadTextureFromDDSFile("nvidia/Rain/textures/bridge_normal.dds");

            m_SceneTextureDiffuse = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, SceneTextureDiffuse);
            m_SceneTextureSpecular = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, SceneTextureSpecular);
            m_SceneTextureNormal = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, SceneTextureNormal);

            //the 3D textures for the rain splashes
            int SplashBumpTexture = NvImage.uploadTextureFromDDSFile("nvidia/Rain/textures/SBumpVolume.dds");
            int SplashDiffuseTexture = NvImage.uploadTextureFromDDSFile("nvidia/Rain/textures/SDiffuseVolume.dds");
            GLCheck.checkError();
            int largest = gl.glGetInteger(GLenum.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
            gl.glBindTexture(GLenum.GL_TEXTURE_3D, SplashBumpTexture);
            gl.glTexParameteri(GLenum.GL_TEXTURE_3D, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT,largest);
            gl.glTexParameteri(GLenum.GL_TEXTURE_3D, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_REPEAT);
            gl.glTexParameteri(GLenum.GL_TEXTURE_3D, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);

            gl.glBindTexture(GLenum.GL_TEXTURE_3D, SplashDiffuseTexture);
            gl.glTexParameteri(GLenum.GL_TEXTURE_3D, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT,largest);
            gl.glTexParameteri(GLenum.GL_TEXTURE_3D, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_REPEAT);
            gl.glTexParameteri(GLenum.GL_TEXTURE_3D, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);

            m_SplashBumpTexture = TextureUtils.createTexture3D(GLenum.GL_TEXTURE_3D, SplashBumpTexture);
            m_SplashDiffuseTexture = TextureUtils.createTexture3D(GLenum.GL_TEXTURE_3D, SplashDiffuseTexture);

            //load the array of rain textures of point lights, see http://www1.cs.columbia.edu/CAVE/databases/rain_streak_db/rain_streak.php
            int rainTextureArray = gl.glGenTexture();
            gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, rainTextureArray);
            gl.glTexStorage3D(GLenum.GL_TEXTURE_2D_ARRAY, 10, GLenum.GL_R8, 16,526, 370);
            final String pattern = "nvidia/Rain/textures/cv0_vPositive_0";
            final String[] tokens = { "", "0", "00", "000"};
            for(int i = 0; i < 370; i++){
                int idx;
                /*if(i == 0)
                    idx = 3;
                else*/ if( i < 10)
                    idx = 2;
                else if( i < 100)
                    idx = 1;
                else
                    idx = 0;

                String filename = pattern + tokens[idx] + i + ".dds";
                NvImage image = new NvImage();
                image.loadImageFromFile(filename);

                int w = image.getWidth();
                int h = image.getHeight();
                for (int l = 0; l < image.getMipLevels(); l++) {
                    gl.glTexSubImage3D(GLenum.GL_TEXTURE_2D_ARRAY, l, 0, 0, i, w, h, 1, GLenum.GL_RED, GLenum.GL_UNSIGNED_BYTE, CacheBuffer.wrap(image.getLevel(l)));
                    w >>= 1;
                    h >>= 1;
                    w = (w != 0) ? w : 1;
                    h = (h != 0) ? h : 1;
                }
            }

            gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);

            m_RainTextureArray = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D_ARRAY, rainTextureArray);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, 0);

            GLCheck.checkError();

            TextureDataDesc initData = new TextureDataDesc();
            initData.data = FileUtils.loadBytes("nvidia/Rain/textures/F_512_data.dat");
            initData.type = GLenum.GL_FLOAT;
            initData.format = GLenum.GL_RED;
            m_Ftable = TextureUtils.createTexture2D(new Texture2DDesc(512,512, GLenum.GL_R32F), initData);

            initData.data = FileUtils.loadBytes("nvidia/Rain/textures/G0_pi_2_64_data.dat");
            m_Gtable = TextureUtils.createTexture2D(new Texture2DDesc(64,64, GLenum.GL_R32F), initData);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }


//        V_RETURN(load3DTextureFromFile(L"../../Media/rainTextures/splashes/SBumpVolume.dds","SplashBumpTexture", pd3dDevice));
//        V_RETURN(load3DTextureFromFile(L"../../Media/rainTextures/splashes/SDiffuseVolume.dds","SplashDiffuseTexture", pd3dDevice));



//        ID3D10Texture2D* rainTexture = NULL;
//        ID3D10ShaderResourceView* textureRV = NULL;
//        ID3D10EffectShaderResourceVariable*   textureArray = g_pEffect->GetVariableByName( "rainTextureArray" )->AsShaderResource();
//        V_RETURN( LoadTextureArray( pd3dDevice, "../../Media/rainTextures/cv0_vPositive_", 370 , &rainTexture, &textureRV) );
//        textureArray->SetResource( textureRV );
//        SAFE_RELEASE(rainTexture);
//        SAFE_RELEASE(textureRV);

        //load the look up tables for the fog, see http://www1.cs.columbia.edu/~bosun/sig05.htm
//        if(loadLUTS("../../Media/F_512_data.csv","Ftable",512,512, pd3dDevice) == S_FALSE)
//            loadLUTS("../Media/F_512_data.csv","Ftable",512,512, pd3dDevice);
//        if(loadLUTS("../../Media/G0_pi_2_64_data.csv","Gtable",64,64, pd3dDevice) == S_FALSE)
//            loadLUTS("../Media/G0_pi_2_64_data.csv","Gtable",64,64, pd3dDevice);
//        if(loadLUTS("../../Media/G20_pi_2_64_data.csv","G_20table",64,64, pd3dDevice) == S_FALSE)
//            loadLUTS("../Media/G20_pi_2_64_data.csv","G_20table",64,64, pd3dDevice);

//        return S_OK;
    }

    @Override
    public void display() {
        updateParticles();

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.0f,0.0f,0.0f,1.0f));
        gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1.0f));

        updateConstantsForSky();
        renderSky();
        renderArrow();
        updateConstantsForScene();
        renderScene();
        advanceRain();
        renderRain();

        g_PrintProgramOnce = true;
    }

    private float m_time;
    private final Vector3f interpolatedWind = new Vector3f();

    private void updateConstantsForSky(){
        m_transformer.getModelViewMat(m_RainParams.g_mWorldView);
        m_RainParams.g_mWorld.setIdentity();
        Matrix4f.invert(m_RainParams.g_mWorldView, m_RainParams.g_mInvView);
        Matrix4f.mul(m_RainParams.g_mProjection, m_RainParams.g_mWorldView, m_RainParams.g_mWorldViewProj);
        Matrix4f.invert(m_RainParams.g_mWorldViewProj, m_RainParams.g_mViewProjectionInverse);
        Matrix4f.decompseRigidMatrix(m_RainParams.g_mWorldView, m_RainParams.g_eyePos, null, null);

//        D3DXVECTOR3 VecLightEye;
//        D3DXVec3Subtract(&VecLightEye,&g_PointLightPos,g_Camera.GetEyePt());
//        float lengthVecLightEye = D3DXVec3Length(&VecLightEye);
//        D3DXVec3Normalize(&VecLightEye,&VecLightEye);
//        g_VecPointLightEyeShaderVariable->SetFloatVector((float*)&VecLightEye);
//        g_DSVPointLightShaderVariable->SetFloat( lengthVecLightEye );

//        D3DXVECTOR3 VecLightEye2;
//        D3DXVec3Subtract(&VecLightEye2,&g_PointLightPos2,g_Camera.GetEyePt());
//        float lengthVecLightEye2 = D3DXVec3Length(&VecLightEye2);
//        D3DXVec3Normalize(&VecLightEye2,&VecLightEye2);
//        g_VecPointLightEye2ShaderVariable->SetFloatVector((float*)&VecLightEye2);
//        g_DSVPointLight2ShaderVariable->SetFloat( lengthVecLightEye2 );

        // update the first spot light attribute
        Vector3f vecLightEye = m_RainParams.g_VecPointLightEye;
        Vector3f.sub(g_PointLightPos, m_RainParams.g_eyePos, vecLightEye);
        float lengthVecLightEye = vecLightEye.length();
        vecLightEye.scale(1.0f/lengthVecLightEye);
        m_RainParams.g_DSVPointLight = lengthVecLightEye;

        // update the second spot light attribute
        Vector3f vecLightEye2 = m_RainParams.g_VecPointLightEye2;
        Vector3f.sub(g_PointLightPos2, m_RainParams.g_eyePos, vecLightEye2);
        float lengthVecLightEye2 = vecLightEye2.length();
        vecLightEye2.scale(1.0f/lengthVecLightEye2);
        m_RainParams.g_DSVPointLight2 = lengthVecLightEye2;

        m_RainParams.g_PointLightIntensity = g_PointLightIntensity;


//        ViewMatrix = *g_Camera.GetViewMatrix();
//        D3DXVECTOR3 LightInViewSpace;
//        vectorMatrixMultiply(&LightInViewSpace, ViewMatrix,g_PointLightPos);
//        D3DXVec3Normalize(&LightInViewSpace,&LightInViewSpace);
//        g_LightPosWithViewTransformationShaderVariable->SetFloatVector((float*)&LightInViewSpace);
//        vectorMatrixMultiply(&LightInViewSpace, ViewMatrix,g_PointLightPos2);
//        D3DXVec3Normalize(&LightInViewSpace,&LightInViewSpace);
//        g_LightPosWithViewTransformation2ShaderVariable->SetFloatVector((float*)&LightInViewSpace);
        Vector3f lightInViewSpace = m_RainParams.g_ViewSpaceLightVec;
//        lightInViewSpace.set(g_PointLightPos);
        Matrix4f.transformVector(m_RainParams.g_mWorldView, g_PointLightPos, lightInViewSpace);
        lightInViewSpace.normalise();

        Vector3f lightInViewSpace2 = m_RainParams.g_ViewSpaceLightVec2;
//        lightInViewSpace2.set(g_PointLightPos2);
        Matrix4f.transformVector(m_RainParams.g_mWorldView, g_PointLightPos2, lightInViewSpace2);
        lightInViewSpace2.normalise();

    }

    private void updateParticles(){
        //------------------------------------------------------------------------------------
        // Set the wind vector
        //piece-wise linear interpolation for the animation curve
        //------------------------------------------------------------------------------------

        //let the frame rate stabilize before using it to determine animation speed
        if(frameCount < 50)
        {
            Vector3f wind = WindAnimation.get(0).windAmount;
//            g_FPSShaderVariable->SetFloat(40.0f);
//            g_pTotalVelShaderVariable->SetFloatVector((float*)wind);
            m_RainParams.g_TotalVel.set(wind.x, wind.y, wind.z);
            frameCount++;
            m_RainParams.g_FrameRate = 60;
        }
        else

        {
            if(g_bMoveParticles)
            {
                //estimate a value of wind at the given time frame
                //we use piecewise linear interpolation between an array of key frames of wind values

                int lastTime = WindAnimation.get(WindAnimation.size() - 1).time;
                int upperFrame = 1;
                float eplsedTime = getFrameDeltaTime();
                float framesPerSecond = 1.0f/eplsedTime; //  DXUTGetFPS();
//                g_FPSShaderVariable->SetFloat(framesPerSecond);
                m_RainParams.g_FrameRate = framesPerSecond;

                while( m_time > WindAnimation.get(upperFrame).time )
                    upperFrame++;

                int lowerFrame = upperFrame - 1;

                final WindValue lowerWind = WindAnimation.get(lowerFrame);
                final WindValue upperWind = WindAnimation.get(upperFrame);
                float amount = (m_time - lowerWind.time)/(upperWind.time - lowerWind.time);
//                D3DXVECTOR3 interpolatedWind = WindAnimation.at(lowerFrame).windAmount + amount*( WindAnimation.at(upperFrame).windAmount - WindAnimation.at(lowerFrame).windAmount);

                interpolatedWind.x = lowerWind.windAmount.x + amount * (upperWind.windAmount.x-lowerWind.windAmount.x);
                interpolatedWind.y = lowerWind.windAmount.y + amount * (upperWind.windAmount.y-lowerWind.windAmount.y);
                interpolatedWind.z = lowerWind.windAmount.z + amount * (upperWind.windAmount.z-lowerWind.windAmount.z);

                //adjust the wind based on the current frame rate; the values were estimated for 40FPS
                interpolatedWind.scale(40.0f/framesPerSecond);
                //lerp between the wind vector and just the vector pointing down depending on the amount of user chosen wind
//                interpolatedWind = g_WindAmount*interpolatedWind + (1-g_WindAmount)*(D3DXVECTOR3(0,-0.5,0));
                interpolatedWind.x = g_WindAmount * interpolatedWind.x;
                interpolatedWind.y = g_WindAmount * interpolatedWind.y - (1-g_WindAmount) * 0.5f;
                interpolatedWind.z = g_WindAmount * interpolatedWind.z;

//                g_pTotalVelShaderVariable->SetFloatVector((float*)interpolatedWind);
                m_RainParams.g_TotalVel.set(interpolatedWind.x, interpolatedWind.y, interpolatedWind.z);
                m_time += eplsedTime;
                if(m_time>lastTime)
                    m_time = 0;
            }
        }
    }

    private void renderSky(){
        //---------------------------------------------------------------------------------------
        // Draw the sky quad
        //------------------------------------------------------------------------------------

        if(g_bRenderBg){
            gl.glBindVertexArray(g_VertexBufferSkyVAO);
            g_pTechniqueRenderSky.enable();
            g_pTechniqueRenderSky.setUniform(m_RainParams);

            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glDisable(GLenum.GL_CULL_FACE);
            gl.glDisable(GLenum.GL_BLEND);

            gl.glActiveTexture(GLenum.GL_TEXTURE6);
            gl.glBindTexture(m_Ftable.getTarget(), m_Ftable.getTexture());

            gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
            gl.glBindVertexArray(0);
            gl.glBindTexture(m_Ftable.getTarget(), 0);

            if(!g_PrintProgramOnce){
                g_pTechniqueRenderSky.setName("TechniqueRenderSky");
                g_pTechniqueRenderSky.printPrograminfo();
            }
        }
    }

    //------------------------------------------------------------------------------------
    //render the light directional control, and update the value of the light vector
    //------------------------------------------------------------------------------------
    private void renderArrow(){}

    private float m_timeCycle;
    private void updateConstantsForScene(){
        m_transformer.getModelViewMat(m_RainParams.g_mWorldView);
        m_RainParams.g_mWorld.setIdentity();
        m_RainParams.g_mWorld.m00 = 3;
        m_RainParams.g_mWorld.m11 = 3;
        m_RainParams.g_mWorld.m22 = 3;
        Matrix4f.mul(m_RainParams.g_mWorldView, m_RainParams.g_mWorld, m_RainParams.g_mWorldView);
        Matrix4f.mul(m_RainParams.g_mProjection, m_RainParams.g_mWorldView, m_RainParams.g_mWorldViewProj);

        if(g_bMoveParticles)
            m_timeCycle += 0.085;
//        g_timeCycleShaderVariable->SetFloat(timeCycle);
        m_RainParams.g_timeCycle = m_timeCycle;
        if(m_timeCycle>=1)
        {
            m_timeCycle = 0;
//            g_rainSplashesXDisplaceShaderVariable->SetFloat( random()*2 );
//            g_rainSplashesYDisplaceShaderVariable->SetFloat( random()*2 );
            m_RainParams.g_splashXDisplace = random() * 2;
            m_RainParams.g_splashYDisplace = random() * 2;
        }
    }

    private void renderScene(){
        if(g_bRenderBg){
            g_pTechniqueRenderScene.enable();
            g_pTechniqueRenderScene.setUniform(m_RainParams);

            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDisable(GLenum.GL_CULL_FACE);
            gl.glDisable(GLenum.GL_BLEND);

            bind(6, m_Ftable);
            bind(7, m_Gtable);
            bind(8, null);
            bind(3, m_SceneTextureDiffuse);
            bind(4, m_SceneTextureSpecular);
            bind(5, m_SceneTextureNormal);
            bind(9, m_SplashBumpTexture);
            bind(10, m_SplashDiffuseTexture);

            g_Mesh.render();

            bind(6, null);
            bind(7, null);
            bind(8, null);
            bind(3, null);
            bind(4, null);
            bind(5, null);
            bind(9, null);
            bind(10, null);

            if(!g_PrintProgramOnce){
                g_pTechniqueRenderScene.setName("TechniqueRenderScene");
                g_pTechniqueRenderScene.printPrograminfo();
            }
        }
    }

    private void advanceRain(){
        if(g_bMoveParticles){
            m_RainParams.moveParticles = true;

            g_pTechniqueAdvanceRain.enable();
            g_pTechniqueAdvanceRain.setUniform(m_RainParams);

            g_pParticleStreamTo.beginRecord(GLenum.GL_POINTS);

            if(firstFrame){
                gl.glBindVertexArray(g_pParticleStartVAO);
                gl.glDrawArrays(GLenum.GL_POINTS, 0, g_numRainVertices);
                gl.glBindVertexArray(0);
            }else{
                g_pParticleDrawFrom.drawStream(0);
            }
            g_pParticleStreamTo.endRecord();

            if(!g_PrintProgramOnce){
                g_pTechniqueAdvanceRain.setName("TechniqueAdvanceRain");
                g_pTechniqueAdvanceRain.printPrograminfo();
            }

            TransformFeedbackObject pTemp = g_pParticleDrawFrom;
            g_pParticleDrawFrom = g_pParticleStreamTo;
            g_pParticleStreamTo = pTemp;

            firstFrame = false;
        }
    }

    private void renderRain(){
        if(g_bDrawParticles){
            m_transformer.getModelViewMat(m_RainParams.g_mWorldView);
            m_RainParams.g_mWorld.setIdentity();
            Matrix4f.mul(m_RainParams.g_mProjection, m_RainParams.g_mWorldView, m_RainParams.g_mWorldViewProj);

            RenderTechnique program;
            if(g_bUseCheapShader) {
                program = g_pTechniqueDrawRainCheap;
                g_pTechniqueDrawRainCheap.setName("TechniqueDrawRainCheap");
            }else{
                program = g_pTechniqueDrawRain;
                g_pTechniqueDrawRain.setName("TechniqueDrawRain");
            }

            program.enable();
            program.setUniform(m_RainParams);

            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendFuncSeparate(GLenum.GL_SRC_ALPHA, GLenum.GL_ONE_MINUS_SRC_ALPHA, GLenum.GL_ONE, GLenum.GL_ONE);
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthMask(false);

            bind(0, m_RainTextureArray);
            g_pParticleDrawFrom.drawStream(0);

            gl.glDisable(GLenum.GL_BLEND);
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glDepthMask(true);
            bind(0, null);

            if(!g_PrintProgramOnce){
                program.printPrograminfo();
            }
        }
    }

    private void bind(int unit, TextureGL texture){
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + unit);
        if(texture != null){
            gl.glBindTexture(texture.getTarget(), texture.getTexture());
        }else{
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        }
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0 || height <= 0)
            return;

        float fAspectRatio = (float)width/height;
        m_RainParams.g_Near = g_znear;
        m_RainParams.g_Far = g_zfar;
        m_RainParams.g_ScreenWidth = width;
        m_RainParams.g_ScreenHeight = height;

        Matrix4f.perspective(g_fov, fAspectRatio, g_znear, g_zfar, m_RainParams.g_mProjection);
        Matrix4f.invert(m_RainParams.g_mProjection, m_RainParams.g_mInverseProjection);
        gl.glViewport(0,0, width, height);

        // TODO uniform variables
        m_RainParams.g_de = (float) ((height/2.0f)/(Math.tan(Math.toRadians(g_fov/2.0f))));
    }

    private void bindFeedback(int programid){
//        layout (stream = 0) out vec3 Out_Pos;
//        layout (stream = 0) out vec3 Out_Seed;
//        layout (stream = 0) out vec3 Out_Speed;
//        layout (stream = 0) out float Out_Random;
//        layout (stream = 0) out uint Out_Type;
        final String[] varyings =
        {
            "Out_Pos", "Out_Seed","Out_Speed", "Out_Random", "Out_Type",
        };

        gl.glTransformFeedbackVaryings(programid, varyings, GLenum.GL_INTERLEAVED_ATTRIBS);
    }

    //----------------------------------------------------------------------------------------
//function to reset all the controls to preset values
//----------------------------------------------------------------------------------------
    void resetVariablesToPreset1()
    {
        //set all variables to preset values
        g_bRenderBg = true;
        g_bMoveParticles = true;
        g_bDrawParticles = true;
        g_bUseCheapShader = false;
        g_bUseSpotLight = true;
        g_fogVector.set(g_finalFogVector1);
        g_dirLightIntensity = g_finalDirLightIntensity1;
        g_responseDirLight = g_finalResponseDirLight1;
        g_PointLightIntensity = g_finalPointLightIntensity1;
        g_cosSpotLight = g_finalCosSpotLight1;
        g_responsePointLight = g_finalResponsePointLight1;
        g_DrawFraction = g_finalDrawFraction1;
        g_WindAmount = g_finalWindAmount1;

        //update variables in the shader
//        g_pRenderBGShaderVariable->SetInt(g_bRenderBg);
//        g_pMoveParticlesShaderVariable->SetInt(g_bMoveParticles);
//        g_UseSpotLightShaderVariable->SetBool(g_bUseSpotLight);
//        g_pFogThicknessShaderVariable->SetFloatVector((float*)&g_fogVector);
//        g_pDirLightIntensityShaderVariable->SetFloat(g_dirLightIntensity);
//        g_pResponseDirLightShaderVariable->SetFloat(g_responseDirLight);
//        g_pPointLightIntensityShaderVariable->SetFloat(g_PointLightIntensity);
//        g_CosSpotLightShaderVariable->SetFloat(g_cosSpotLight);
//        g_pResponsePointLightShaderVariable->SetFloat(g_responsePointLight);
        // TODO setup uniform variables
        setShadingParametersBasedOnRain();

//        SAFE_DELETE(g_lightDirectionWidget);
//        g_lightDirectionWidget = new CDXUTDirectionWidget();
//        g_lightDirectionWidget->SetButtonMask(MOUSE_LEFT_BUTTON);
//        g_lightDirectionWidget->SetLightDirection( g_directionalLightVector1 );
//        g_lightDirectionWidget->SetRadius( 10.0f );

        //set the camera back to where it was
        g_vecEye.set(g_finalVecEye1);
        g_vecAt.set(g_finalAtVec1);
//        g_Camera.SetViewParams( &g_vecEye, &g_vecAt );
    }


    void resetVariablesToPreset2()
    {
        //set all variables to preset values
        g_bRenderBg = true;
        g_bMoveParticles = true;
        g_bDrawParticles = true;
        g_bUseCheapShader = false;
        g_bUseSpotLight = true;
        g_fogVector.set(g_finalFogVector2);
        g_dirLightIntensity = g_finalDirLightIntensity2;
        g_responseDirLight = g_finalResponseDirLight2;
        g_PointLightIntensity = g_finalPointLightIntensity2;
        g_cosSpotLight = g_finalCosSpotLight2;
        g_responsePointLight = g_finalResponsePointLight2;
        g_DrawFraction = g_finalDrawFraction2;
        g_WindAmount = g_finalWindAmount2;

        //TODO update variables in the shader
//        g_pRenderBGShaderVariable->SetInt(g_bRenderBg);
//        g_pMoveParticlesShaderVariable->SetInt(g_bMoveParticles);
//        g_UseSpotLightShaderVariable->SetBool(g_bUseSpotLight);
//        g_pFogThicknessShaderVariable->SetFloatVector((float*)&g_fogVector);
//        g_pDirLightIntensityShaderVariable->SetFloat(g_dirLightIntensity);
//        g_pResponseDirLightShaderVariable->SetFloat(g_responseDirLight);
//        g_pPointLightIntensityShaderVariable->SetFloat(g_PointLightIntensity);
//        g_CosSpotLightShaderVariable->SetFloat(g_cosSpotLight);
//        g_pResponsePointLightShaderVariable->SetFloat(g_responsePointLight);
        setShadingParametersBasedOnRain();

//        SAFE_DELETE(g_lightDirectionWidget);
//        g_lightDirectionWidget = new CDXUTDirectionWidget();
//        g_lightDirectionWidget->SetButtonMask(MOUSE_LEFT_BUTTON);
//        g_lightDirectionWidget->SetLightDirection( g_directionalLightVector1 );
//        g_lightDirectionWidget->SetRadius( 10.0f );

        //set the camera back to where it was
        g_vecEye.set(g_finalVecEye2);
        g_vecAt.set(g_finalAtVec2);
//        g_Camera.SetViewParams( &g_vecEye, &g_vecAt );
    }


    void setShadingParametersBasedOnRain()
    {
        //dry
        float DryKd = 1;
        float DryKsPoint = 0;
        float DryKsDir = 0;
        float DryspecPower = 5;
        //wet
        float WetKd = 0.4f;
        float WetKsPoint = 2;
        float WetKsDir = 2;
        float WetspecPower = 100;

        // TODO update uniform variables.
//        g_KdShaderVariable->SetFloat( DryKd + g_DrawFraction*(WetKd-DryKd) );
//        g_KsPointShaderVariable->SetFloat( DryKsPoint + g_DrawFraction*(WetKsPoint-DryKsPoint) );
//        g_KsDirShaderVariable->SetFloat( DryKsDir + g_DrawFraction*(WetKsDir-DryKsDir) );
//        g_SpecPowerShaderVariable->SetFloat( DryspecPower + g_DrawFraction*(WetspecPower-DryspecPower) );
    }

}
