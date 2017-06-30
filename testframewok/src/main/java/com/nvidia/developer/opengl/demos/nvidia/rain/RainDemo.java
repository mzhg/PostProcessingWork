package com.nvidia.developer.opengl.demos.nvidia.rain;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.util.Numeric;

import static com.sun.org.apache.xpath.internal.objects.XBoolean.S_FALSE;

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

//global variables

    float g_ScreenWidth = 1264.0f;
    float g_ScreenHeight = 958.0f;
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

    final List<WindValue> WindAnimation = new ArrayList<>();
    int totalAnimationValues = 0;

    @Override
    protected void initRendering() {
        resetVariablesToPreset1();

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
//        g_pTotalVelShaderVariable->SetFloatVector((float*)WindAnimation.at(0).windAmount);

        //set the variables
        D3DXMATRIX ViewMatrix;
        D3DXMATRIX InvViewMatrix;
        ViewMatrix = *g_Camera.GetViewMatrix();
        D3DXMatrixInverse( &InvViewMatrix, NULL, &ViewMatrix );


        D3DXVECTOR3 VecLightEye;
        D3DXVec3Subtract(&VecLightEye,&g_PointLightPos,g_Camera.GetEyePt());
        float lengthVecLightEye = D3DXVec3Length(&VecLightEye);
        D3DXVec3Normalize(&VecLightEye,&VecLightEye);

        D3DXVECTOR3 VecLightEye2;
        D3DXVec3Subtract(&VecLightEye2,&g_PointLightPos2,g_Camera.GetEyePt());
        float lengthVecLightEye2 = D3DXVec3Length(&VecLightEye2);
        D3DXVec3Normalize(&VecLightEye2,&VecLightEye2);


        //initialize the matrices
        g_pInvViewShaderVariable->SetMatrix( (float*)&InvViewMatrix );
        g_pProjectionShaderVariable->SetMatrix( (float*)g_Camera.GetProjMatrix());

        //initialize the vectors
        g_pEyePosShaderVariable->SetFloatVector((float*)&g_vecEye);
        D3DXVECTOR3 LightInViewSpace;
        vectorMatrixMultiply(&LightInViewSpace, ViewMatrix,g_PointLightPos);
        D3DXVec3Normalize(&LightInViewSpace,&LightInViewSpace);
        g_LightPosWithViewTransformationShaderVariable->SetFloatVector((float*)&LightInViewSpace);
        vectorMatrixMultiply(&LightInViewSpace, ViewMatrix,g_PointLightPos2);
        D3DXVec3Normalize(&LightInViewSpace,&LightInViewSpace);
        g_LightPosWithViewTransformation2ShaderVariable->SetFloatVector((float*)&LightInViewSpace);
        g_pFogThicknessShaderVariable->SetFloatVector((float*)&g_fogVector);


        //initialize the scalars
        g_pSpriteSizeShaderVariable->SetFloat(g_SpriteSize);
        g_ScreenWidthShaderVariable->SetFloat(g_ScreenWidth);
        g_ScreenHeightShaderVariable->SetFloat(g_ScreenHeight);
        g_ScreenWidthMultiplierShaderVariable->SetFloat( 2.0f/(g_ScreenWidth-1)  );
        g_ScreenHeightMultiplierShaderVariable->SetFloat( 2.0f/(g_ScreenHeight-1)  );
        g_DSVPointLightShaderVariable->SetFloat( lengthVecLightEye );
        g_DSVPointLight2ShaderVariable->SetFloat( lengthVecLightEye2 );
        g_deShaderVariable->SetFloat( (g_ScreenHeight/2.0f)/(tan(g_fov/2.0f)) );
        setShadingParametersBasedOnRain();



        //----------------------------------------------------------------------------------------------
        //vertex buffers
        //----------------------------------------------------------------------------------------------


        //vertices for the rain particles---------------------------------------------------------------
        //generate vertices in a cylinder above the camera


        firstFrame = true;
        RainVertex* vertices = new RainVertex[g_numRainVertices];
        if(vertices==NULL)
            exit(0);

        for(int i=0;i<g_numRainVertices;i++)
        {
            RainVertex raindrop;
            //use rejection sampling to generate random points inside a circle of radius 1 centered at 0,0
            float SeedX;
            float SeedZ;
            bool pointIsInside = false;
            while(!pointIsInside)
            {
                SeedX = random() - 0.5f;
                SeedZ = random() - 0.5f;
                if( sqrt( SeedX*SeedX + SeedZ*SeedZ ) <= 0.5f )
                    pointIsInside = true;
            }
            //save these random locations for reinitializing rain particles that have fallen out of bounds
            SeedX *= g_radiusRange;
            SeedZ *= g_radiusRange;
            float SeedY = random()*g_heightRange;
            raindrop.seed = D3DXVECTOR3(SeedX,SeedY,SeedZ);

            //add some random speed to the particles, to prevent all the particles from following exactly the same trajectory
            //additionally, random speeds in the vertical direction ensure that temporal aliasing is minimized
            float SpeedX = 40.0f*(random()/20.0f);
            float SpeedZ = 40.0f*(random()/20.0f);
            float SpeedY = 40.0f*(random()/10.0f);
            raindrop.speed = D3DXVECTOR3(SpeedX,SpeedY,SpeedZ);

            //move the rain particles to a random positions in a cylinder above the camera
            float x = SeedX + g_vecEye.x;
            float z = SeedZ + g_vecEye.z;
            float y = SeedY + g_vecEye.y;
            raindrop.pos = D3DXVECTOR3(x,y,z);

            //get an integer between 1 and 8 inclusive to decide which of the 8 types of rain textures the particle will use
            raindrop.Type = int(floor(  random()*8 + 1 ));

            //this number is used to randomly increase the brightness of some rain particles
            raindrop.random = 1;
            float randomIncrease = random();
            if( randomIncrease > 0.8)
                raindrop.random += randomIncrease;

            vertices[i] = raindrop;
        }


        //create vertex buffers for the rain, two will be used to pingpong between during animation
        D3D10_BUFFER_DESC bd;
        bd.ByteWidth = sizeof( RainVertex ) * g_numRainVertices;
        bd.Usage = D3D10_USAGE_DEFAULT;
        bd.BindFlags = D3D10_BIND_VERTEX_BUFFER;
        bd.CPUAccessFlags = 0;
        bd.MiscFlags = 0;
        D3D10_SUBRESOURCE_DATA InitData;
        ZeroMemory( &InitData, sizeof(D3D10_SUBRESOURCE_DATA) );
        InitData.pSysMem = vertices;
        InitData.SysMemPitch = sizeof(RainVertex);
        V_RETURN( pd3dDevice->CreateBuffer( &bd, &InitData, &g_pParticleStart    ) );
        bd.BindFlags |= D3D10_BIND_STREAM_OUTPUT;
        V_RETURN( pd3dDevice->CreateBuffer( &bd, NULL,      &g_pParticleDrawFrom ) );
        V_RETURN( pd3dDevice->CreateBuffer( &bd, NULL,      &g_pParticleStreamTo ) );
        delete[] vertices;

        // Create the vertex input layout for rain
        const D3D10_INPUT_ELEMENT_DESC layout[] =
                {
                        { "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0,  D3D10_INPUT_PER_VERTEX_DATA, 0 },
                        { "SEED",     0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 12,  D3D10_INPUT_PER_VERTEX_DATA, 0 },
                        { "SPEED",    0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 24,  D3D10_INPUT_PER_VERTEX_DATA, 0 },
                        { "RAND",     0, DXGI_FORMAT_R32_FLOAT,       0, 36, D3D10_INPUT_PER_VERTEX_DATA, 0 },
                        { "TYPE",     0, DXGI_FORMAT_R8_UINT,         0, 40, D3D10_INPUT_PER_VERTEX_DATA, 0 },
                };
        UINT numElements = sizeof(layout)/sizeof(layout[0]);
        D3D10_PASS_DESC PassDesc;
        g_pTechniqueDrawRain->GetPassByIndex( 0 )->GetDesc( &PassDesc );
        V_RETURN(pd3dDevice->CreateInputLayout( layout, numElements, PassDesc.pIAInputSignature, PassDesc.IAInputSignatureSize, &g_pVertexLayoutRainVertex ));

        //---------------------------------------------------------------------------------------------

        //scene mesh

        // Create the input layout for the scene
        const D3D10_INPUT_ELEMENT_DESC layoutScene[] =
                {
                        { "POSITION",  0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D10_INPUT_PER_VERTEX_DATA, 0 },
                        { "NORMAL",    0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 12, D3D10_INPUT_PER_VERTEX_DATA, 0 },
                        { "TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT, 0, 24, D3D10_INPUT_PER_VERTEX_DATA, 0 },
                        { "TANGENT",   0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 32, D3D10_INPUT_PER_VERTEX_DATA, 0 },
                };
        UINT numElementsScene = sizeof(layoutScene)/sizeof(layoutScene[0]);
        g_pTechniqueRenderScene->GetPassByIndex( 0 )->GetDesc( &PassDesc );
        V_RETURN( pd3dDevice->CreateInputLayout( layoutScene, numElementsScene, PassDesc.pIAInputSignature, PassDesc.IAInputSignatureSize, &g_pVertexLayoutScene ) );
        // Set the input layout
        pd3dDevice->IASetInputLayout( g_pVertexLayoutScene );
        //load the scene
        V_RETURN(NVUTFindDXSDKMediaFileCch(str, MAX_PATH, L"..\\..\\Media\\Bridge\\Bridge.x"));
        V_RETURN( g_Mesh.Create( pd3dDevice,str, (D3D10_INPUT_ELEMENT_DESC*)layoutScene, numElementsScene ) );

        //---------------------------------------------------------------------------------------------
        //mesh and layout for arrow

        // Create the input layout
        const D3D10_INPUT_ELEMENT_DESC layoutArrow[] =
                {
                        { "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D10_INPUT_PER_VERTEX_DATA, 0 },
                        { "NORMAL", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 12, D3D10_INPUT_PER_VERTEX_DATA, 0 },
                        { "TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT, 0, 24, D3D10_INPUT_PER_VERTEX_DATA, 0 },
                };
        numElements = sizeof(layoutArrow)/sizeof(layoutArrow[0]);
        g_pTechniqueRenderArrow->GetPassByIndex( 0 )->GetDesc( &PassDesc );
        V_RETURN( pd3dDevice->CreateInputLayout( layoutArrow, numElements, PassDesc.pIAInputSignature, PassDesc.IAInputSignatureSize, &g_pVertexLayoutArrow ) );
        //load the arrow mesh
        V_RETURN( NVUTFindDXSDKMediaFileCch( str, MAX_PATH, L"arrow.x" ) );
        V_RETURN( g_MeshArrow.Create( pd3dDevice, str, (D3D10_INPUT_ELEMENT_DESC*)layoutArrow, numElements ) );

        //-------------------------------------------------------------------------------------------------
        //vertex buffer and layout for sky

        // Create the input layout
        D3D10_INPUT_ELEMENT_DESC    layoutSky[] =
                {
                        { "position", 0, DXGI_FORMAT_R32G32_FLOAT, 0, 0, D3D10_INPUT_PER_VERTEX_DATA, 0 },
                };
        numElements = sizeof(layoutSky)/sizeof(layoutSky[0]);
        g_pTechniqueRenderSky->GetPassByIndex( 0 )->GetDesc( &PassDesc );
        V_RETURN( pd3dDevice->CreateInputLayout( layoutSky, numElements, PassDesc.pIAInputSignature, PassDesc.IAInputSignatureSize, &g_pVertexLayoutSky ) );

        //make the vertex buffer for the quad that will be used for the sky
        D3DXVECTOR2 verticesSky[4];
        verticesSky[0] = D3DXVECTOR2( 1.0f,  1.0f);
        verticesSky[1] = D3DXVECTOR2( 1.0f, -1.0f);
        verticesSky[2] = D3DXVECTOR2(-1.0f,  1.0f);
        verticesSky[3] = D3DXVECTOR2(-1.0f, -1.0f);
        D3D10_SUBRESOURCE_DATA InitDataSky;
        InitDataSky.pSysMem  = verticesSky;
        D3D10_BUFFER_DESC      bdSky;
        bdSky.Usage          = D3D10_USAGE_IMMUTABLE;
        bdSky.ByteWidth      = sizeof( D3DXVECTOR2 ) * 4;
        bdSky.BindFlags      = D3D10_BIND_VERTEX_BUFFER;
        bdSky.CPUAccessFlags = 0;
        bdSky.MiscFlags      = 0;
        V_RETURN(pd3dDevice->CreateBuffer( &bdSky, &InitDataSky, &g_VertexBufferSky ));

        //---------------------------------------------------------------------------------------------
        //textures
        //---------------------------------------------------------------------------------------------

        //the textures for the bridge
        V_RETURN(loadTextureFromFile(L"../../Media/Bridge/bridge_color.dds","SceneTextureDiffuse",pd3dDevice));
        V_RETURN(loadTextureFromFile(L"../../Media/Bridge/bridge_spec.dds","SceneTextureSpecular",pd3dDevice));
        V_RETURN(loadTextureFromFile(L"../../Media/Bridge/bridge_normal.dds","SceneTextureNormal",pd3dDevice));

        //the 3D textures for the rain splashes
        V_RETURN(load3DTextureFromFile(L"../../Media/rainTextures/splashes/SBumpVolume.dds","SplashBumpTexture", pd3dDevice));
        V_RETURN(load3DTextureFromFile(L"../../Media/rainTextures/splashes/SDiffuseVolume.dds","SplashDiffuseTexture", pd3dDevice));

        //load the array of rain textures of point lights, see http://www1.cs.columbia.edu/CAVE/databases/rain_streak_db/rain_streak.php
        ID3D10Texture2D* rainTexture = NULL;
        ID3D10ShaderResourceView* textureRV = NULL;
        ID3D10EffectShaderResourceVariable*   textureArray = g_pEffect->GetVariableByName( "rainTextureArray" )->AsShaderResource();
        V_RETURN( LoadTextureArray( pd3dDevice, "../../Media/rainTextures/cv0_vPositive_", 370 , &rainTexture, &textureRV) );
        textureArray->SetResource( textureRV );
        SAFE_RELEASE(rainTexture);
        SAFE_RELEASE(textureRV);

        //load the look up tables for the fog, see http://www1.cs.columbia.edu/~bosun/sig05.htm
        if(loadLUTS("../../Media/F_512_data.csv","Ftable",512,512, pd3dDevice) == S_FALSE)
            loadLUTS("../Media/F_512_data.csv","Ftable",512,512, pd3dDevice);
        if(loadLUTS("../../Media/G0_pi_2_64_data.csv","Gtable",64,64, pd3dDevice) == S_FALSE)
            loadLUTS("../Media/G0_pi_2_64_data.csv","Gtable",64,64, pd3dDevice);
        if(loadLUTS("../../Media/G20_pi_2_64_data.csv","G_20table",64,64, pd3dDevice) == S_FALSE)
            loadLUTS("../Media/G20_pi_2_64_data.csv","G_20table",64,64, pd3dDevice);

        return S_OK;
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
