package jet.opengl.demos.amdfx.shadows;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Vector4i;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import jet.opengl.demos.amdfx.common.AMD_Mesh;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLProgramPipeline;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

public final class ShadowFX_Sample extends NvSampleApp{
	
	static final int CUBE_FACE_COUNT = 6;
	static final int EXPERIMENTAL_DELAY_END_ALL_ACCESS = 0;
	
	// this is an experimental mode to reduce the amount of code needed to implement the CFX API
	static final int EXPERIMENTAL_EMULATE_GPU_2_STEP_COPY = 0;
	
	final CFirstPersonCamera   g_LightCamera  = new CFirstPersonCamera();
	final CFirstPersonCamera   g_ViewerCamera = new CFirstPersonCamera();
	final CFirstPersonCamera[] g_CubeCamera = new CFirstPersonCamera[CUBE_FACE_COUNT];
	int 					   g_CurrentLightCamera = 0;
	
	CFirstPersonCamera         g_pCurrentCamera = g_ViewerCamera;
	final Matrix4f[] g_LightOrtho = new Matrix4f[CUBE_FACE_COUNT];
	final S_CAMERA_DATA g_ViewerData = new S_CAMERA_DATA();
	final S_CAMERA_DATA[] g_LightData = new S_CAMERA_DATA[CUBE_FACE_COUNT];
	final S_UNIT_CUBE_TRANSFORM m_CubeTransform = new S_UNIT_CUBE_TRANSFORM();
	
	boolean                                          g_bShowLightCamera = false;
	boolean                                          g_bShowShadowMapRegion = false;
	boolean                                          g_bShowShadowMask = false;

	final Vector4f  red    = new Vector4f(1.00f, 0.00f, 0.00f, 1.00f);
	final Vector4f  orange = new Vector4f(1.00f, 0.50f, 0.00f, 1.00f);
	final Vector4f 	yellow = new Vector4f(1.00f, 1.00f, 0.00f, 1.00f);
	final Vector4f  bright_green = new Vector4f(0.50f, 1.00f, 0.00f, 1.00f);
	final Vector4f  green = new Vector4f(0.00f, 1.00f, 0.00f, 1.00f);
	final Vector4f  mint = new Vector4f(0.00f, 1.00f, 0.50f, 1.00f);
	final Vector4f  cyan = new Vector4f(0.00f, 1.00f, 1.00f, 1.00f);
	final Vector4f  deep_blue = new Vector4f(0.00f, 0.50f, 1.00f, 1.00f);
	final Vector4f  blue = new Vector4f(0.00f, 0.00f, 1.00f, 1.00f);
	final Vector4f  purple = new Vector4f(0.50f, 0.00f, 1.00f, 1.00f);
	final Vector4f  magenta = new Vector4f(1.00f, 0.00f, 1.00f, 1.00f);
	final Vector4f  white = new Vector4f(1.00f, 1.00f, 1.00f, 1.00f);
	final Vector4f  grey = new Vector4f(0.50f, 0.50f, 0.50f, 1.00f);
	final Vector4f  black = new Vector4f(0.000f, 0.000f, 0.000f, 0.000f);
	final Vector4f  light_blue = new Vector4f(0.176f, 0.196f, 0.667f, 0.000f);

	final Vector4f  g_Color[] =
	{
	  white, red, green, blue, magenta, cyan, yellow
	};
	
	/*ID3D11InputLayout**/int                        g_pSdkMeshIL = 0;
	/*ID3D11InputLayout**/int                        g_pSceneIL = 0;

	/*ID3D11SamplerState**/int                       g_pLinearWrapSS = 0;

//	ID3D11BlendState*                                g_pOpaqueBS = NULL;
//	ID3D11BlendState*                                g_pShadowMaskChannelBS[4] = { 0,0,0,0 };

	/*ID3D11VertexShader**/ ShaderProgram g_pSceneVS = null;
	/*ID3D11VertexShader**/ ShaderProgram            g_pShadowMapVS = null;

	/*ID3D11PixelShader**/ ShaderProgram             g_pDepthPassScenePS = null;
	/*ID3D11PixelShader**/ ShaderProgram             g_pDepthAndNormalPassScenePS = null;
	/*ID3D11PixelShader**/ ShaderProgram             g_pShadowedScenePS = null;
	/*ID3D11PixelShader**/ ShaderProgram             g_pShadowMapPS = null;

	/*ID3D11VertexShader**/ ShaderProgram            g_pUnitCubeVS = null;
	/*ID3D11VertexShader**/ ShaderProgram            g_pFullscreenVS = null;
	/*ID3D11PixelShader**/  ShaderProgram            g_pUnitCubePS = null;
	/*ID3D11PixelShader**/  ShaderProgram            g_pFullscreenPS = null;
	GLSLProgramPipeline 							 g_pProgramPipeline = null;
	

	// Constant Buffer
	/*ID3D11Buffer**/ int                            g_pModelCB = 0;
	/*ID3D11Buffer**/ int                            g_pViewerCB = 0;
	/*ID3D11Buffer**/ int                            g_pLightCB = 0;
	/*ID3D11Buffer**/ int                            g_pUnitCubeCB = 0;
	int 											 g_pFramebuffer;

//	ID3D11RasterizerState*                           g_pNoCullingSolidRS = NULL;
//	ID3D11RasterizerState*                           g_pBackCullingSolidRS = NULL;
//	ID3D11RasterizerState*                           g_pFrontCullingSolidRS = NULL;
//	ID3D11RasterizerState*                           g_pNoCullingWireframeRS = NULL;
//
//	ID3D11DepthStencilState*                         g_pDepthTestMarkStencilDSS = NULL;
//	ID3D11DepthStencilState*                         g_pStencilTestAndClearDSS = NULL;
//	ID3D11DepthStencilState*                         g_pDepthTestLessDSS = NULL;
//	ID3D11DepthStencilState*                         g_pDepthTestLessEqualDSS = NULL;
	
	Texture2D g_ShadowMask, g_AppDepth, g_AppNormal, g_LightColor, g_LightDepth;
	AMD_Mesh 										 g_Tree, g_BoxPlane;
	AMD_Mesh 	                                     g_MeshArray[] = { g_Tree, /*&g_Tree, &g_Tree, &g_Tree,*/  g_BoxPlane, g_BoxPlane  }; // TODO: rearrange this for a proper instanced rendering
	final Matrix4f[]                                 g_MeshModelMatrix = new Matrix4f[3];
	Texture2D                                   	 g_ShadowMap;
	Texture2D[]                                   	 g_ShadowMapSlices = null;
	float                                            g_ShadowMapSize = 1024;
	int                                              g_ShadowMapAtlasScaleW = CUBE_FACE_COUNT / 2, g_ShadowMapAtlasScaleH = CUBE_FACE_COUNT / g_ShadowMapAtlasScaleW;

	int                                              g_agsGpuCount = 0;
	
	final ShadowFX_Desc                              g_ShadowsDesc = new ShadowFX_Desc();
	int					                             g_ShadowsExecution = ShadowFX_Constants.SHADOWFX_EXECUTION_UNION;

	// App variables
	float                                            g_SunSize = 4.0f;
	float                                            g_DepthTestOffset = 0.00000f;
	float                                            g_NormalOffsetScale = 0.001f;
//	int                                              g_Height = 1080, g_Width = 1920;

	//--------------------------------------------------------------------------------------
	// Timing data
	//--------------------------------------------------------------------------------------
	float                                            g_ShadowRenderingTime = 0.0f;
	float                                            g_ShadowFilteringTime = 0.0f;
	float                                            g_DepthPrepassRenderingTime = 0.0f;
	float                                            g_ShadowMapMasking = 0.0f;
	float                                            g_SceneRendering = 0.0f;

	private GLFuncProvider gl;
	
	@SuppressWarnings("unused")
	@Override
	public void initRendering() {
		gl = GLFuncProviderFactory.getGLFuncProvider();
//		g_ShadowsDesc.m_pDevice = pd3dDevice; // TODO: need to add an option to perform lazy shader compilation
		ShadowFX_Desc.ShadowFX_Initialize(g_ShadowsDesc);

	    try {
			CreateShaders(/*pd3dDevice*/);
		} catch (IOException e) {
			e.printStackTrace();
		}

//	    g_ShadowMap.Release();
//	    g_ShadowMap.CreateSurface(DXUTGetD3D11Device(),
//	        (unsigned int)g_ShadowMapSize * g_ShadowMapAtlasScaleW, (unsigned int)g_ShadowMapSize * g_ShadowMapAtlasScaleH, 1, 1, 1,
//	        DXGI_FORMAT_R32_TYPELESS, DXGI_FORMAT_R32_FLOAT,
//	        DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_D32_FLOAT,
//	        DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_UNKNOWN,
//	        D3D11_USAGE_DEFAULT, false, 0, NULL, NULL, g_ShadowMapCfxTransfer);
	    
	    Texture2DDesc shadowMapDesc = new Texture2DDesc((int)(g_ShadowMapSize * g_ShadowMapAtlasScaleW), (int)(g_ShadowMapSize * g_ShadowMapAtlasScaleH),
				GLenum.GL_DEPTH_COMPONENT32F);
	    g_ShadowMap = TextureUtils.createTexture2D(shadowMapDesc, null);
	    
	    if (EXPERIMENTAL_EMULATE_GPU_2_STEP_COPY==1){
//		    g_ShadowMapCopy.Release();
//		    g_ShadowMapCopy.CreateSurface(DXUTGetD3D11Device(),
//		        (unsigned int)g_ShadowMapSize * g_ShadowMapAtlasScaleW, (unsigned int)g_ShadowMapSize * g_ShadowMapAtlasScaleH, 1, 1, 1,
//		        DXGI_FORMAT_R32_TYPELESS, DXGI_FORMAT_R32_FLOAT,
//		        DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_UNKNOWN,
//		        DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_UNKNOWN,
//		        D3D11_USAGE_DEFAULT, false, 0, NULL, g_agsContext, g_ShadowMapCopyCfxTransfer);
//	    	g_ShadowMapCopy = TextureUtils.createTexture2D(shadowMapDesc, null);
	    }

//	    g_LightDepth.Release();
//	    g_LightDepth.CreateSurface(DXUTGetD3D11Device(),
//	        (unsigned int)512, (unsigned int)512, 1, 1, 1,
//	        DXGI_FORMAT_R24G8_TYPELESS, DXGI_FORMAT_R24_UNORM_X8_TYPELESS,
//	        DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_D24_UNORM_S8_UINT,
//	        DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_D24_UNORM_S8_UINT,
//	        D3D11_USAGE_DEFAULT, false, 0, NULL, g_agsContext, AGS_AFR_TRANSFER_DEFAULT);
	    Texture2DDesc lightDepthDesc = new Texture2DDesc(512, 512, GLenum.GL_DEPTH24_STENCIL8);
	    g_LightDepth = TextureUtils.createTexture2D(lightDepthDesc, null);

//	    g_LightColor.Release();
//	    g_LightColor.CreateSurface(DXUTGetD3D11Device(),
//	        (unsigned int)512, (unsigned int)512, 1, 1, 1,
//	        DXGI_FORMAT_R8G8B8A8_UNORM, DXGI_FORMAT_R8G8B8A8_UNORM,
//	        DXGI_FORMAT_R8G8B8A8_UNORM, DXGI_FORMAT_UNKNOWN,
//	        DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_UNKNOWN,
//	        D3D11_USAGE_DEFAULT, false, 0, NULL, g_agsContext, AGS_AFR_TRANSFER_DEFAULT);
	    Texture2DDesc lightColorDesc = new Texture2DDesc(512, 512, GLenum.GL_RGBA8);
	    g_LightColor = TextureUtils.createTexture2D(lightColorDesc, null);

	    // Setup constant buffer
//	    D3D11_BUFFER_DESC b1dDesc;
//	    b1dDesc.Usage = D3D11_USAGE_DYNAMIC;
//	    b1dDesc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
//	    b1dDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
//	    b1dDesc.MiscFlags = 0;
//	    b1dDesc.ByteWidth = sizeof(S_CAMERA_DATA);
//	    V_RETURN(pd3dDevice->CreateBuffer(&b1dDesc, NULL, &g_pViewerCB));
//	    DXUT_SetDebugName(g_pViewerCB, "g_pViewerCB");
	    g_pViewerCB = gl.glGenBuffer();
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, g_pViewerCB);
		gl.glBufferData(GLenum.GL_ARRAY_BUFFER, S_CAMERA_DATA.SIZE, GLenum.GL_DYNAMIC_DRAW);

//	    b1dDesc.Usage = D3D11_USAGE_DYNAMIC;
//	    b1dDesc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
//	    b1dDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
//	    b1dDesc.MiscFlags = 0;
//	    b1dDesc.ByteWidth = sizeof(S_MODEL_DATA);
//	    V_RETURN(pd3dDevice->CreateBuffer(&b1dDesc, NULL, &g_pModelCB));
//	    DXUT_SetDebugName(g_pModelCB, "g_pModelCB");
	    g_pModelCB = gl.glGenBuffer();
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, g_pModelCB);
		gl.glBufferData(GLenum.GL_ARRAY_BUFFER, S_MODEL_DATA.SIZE, GLenum.GL_DYNAMIC_DRAW);

//	    b1dDesc.Usage = D3D11_USAGE_DYNAMIC;
//	    b1dDesc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
//	    b1dDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
//	    b1dDesc.MiscFlags = 0;
//	    b1dDesc.ByteWidth = sizeof(g_LightData);
//	    V_RETURN(pd3dDevice->CreateBuffer(&b1dDesc, NULL, &g_pLightCB));
//	    DXUT_SetDebugName(g_pLightCB, "g_pLightCB");
	    g_pLightCB = gl.glGenBuffer();
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, g_pLightCB);
		gl.glBufferData(GLenum.GL_ARRAY_BUFFER, S_CAMERA_DATA.SIZE * g_LightData.length, GLenum.GL_DYNAMIC_DRAW);

//	    b1dDesc.Usage = D3D11_USAGE_DYNAMIC;
//	    b1dDesc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
//	    b1dDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
//	    b1dDesc.MiscFlags = 0;
//	    b1dDesc.ByteWidth = sizeof(S_UNIT_CUBE_TRANSFORM);
//	    V_RETURN(pd3dDevice->CreateBuffer(&b1dDesc, NULL, &g_pUnitCubeCB));
//	    DXUT_SetDebugName(g_pUnitCubeCB, "g_pUnitCubeCB");
	    g_pUnitCubeCB = gl.glGenBuffer();
	    gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, g_pUnitCubeCB);
	    gl.glBufferData(GLenum.GL_ARRAY_BUFFER, S_UNIT_CUBE_TRANSFORM.SIZE, GLenum.GL_DYNAMIC_DRAW);
	    
	    // TODO Load the meshes
//	    V_RETURN(g_Tree.Create(pd3dDevice, "..\\media\\coconuttree\\", "coconut.sdkmesh", true));
//	    V_RETURN(g_BoxPlane.Create(pd3dDevice, "..\\media\\plane\\", "plane.sdkmesh", true));

//	    g_MeshModelMatrix[0] = XMMatrixScaling(0.01f, 0.01f, 0.01f) * XMMatrixTranslation(5, 0, 0);
//	    g_MeshModelMatrix[1] = XMMatrixIdentity();
//	    g_MeshModelMatrix[2] = XMMatrixScaling(1.0f, 10.0f, 0.001f) * XMMatrixTranslation(0, 10, -2.5);

		for(int i = 0; i < g_MeshModelMatrix.length; i++){
			g_MeshModelMatrix[i] = new Matrix4f();
		}
	    g_MeshModelMatrix[0].m00 = g_MeshModelMatrix[0].m11 = g_MeshModelMatrix[0].m22 = 0.01f;
	    g_MeshModelMatrix[0].m30 = 5.0f;
	    
	    g_MeshModelMatrix[2].m00 = 1.0f;
	    g_MeshModelMatrix[2].m11 = 10.0f;
	    g_MeshModelMatrix[2].m22 = 0.001f;
	    g_MeshModelMatrix[2].m30 = 0;
	    g_MeshModelMatrix[2].m30 = 10;
	    g_MeshModelMatrix[2].m30 = -2.5f;

	    g_ViewerData.m_Color.set(1.0f, 1.0f, 1.0f, 1.0f);

	    for (int light = 0; light < CUBE_FACE_COUNT; light++)
	    {
	    	g_LightData[light] = new S_CAMERA_DATA();
	        g_LightData[light].m_Color.set(g_Color[light]);
	        g_LightData[light].m_BackBufferDim.set(g_ShadowMapSize, g_ShadowMapSize);
	        g_LightData[light].m_BackBufferDimRcp.set(1.0f / g_ShadowMapSize, 1.0f / g_ShadowMapSize);
	    }

	    // Setup the camera's view parameters
	    Vector3f vecEye = new Vector3f(19.193f, 3.425f, 1.794f);
	    Vector3f vecLightEye = new Vector3f(0.471f, 2.855f, 2.096f);
	    Vector3f vecAt = new Vector3f(18.193f, 3.425f, 1.794f);
	    Vector3f vecUp = new Vector3f(0.0f, 1.0f, 0.0f);

//	    g_ViewerCamera.SetViewParams(vecEye, vecAt, vecUp);
//	    g_ViewerCamera.FrameMove(0.00001f);
	    m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
		m_transformer.setTranslation(-19.193f, -3.425f, -1.794f);

//	    g_LightCamera.SetViewParams(vecLightEye, vecAt, vecUp);
//	    g_LightCamera.FrameMove(0.00001f);
//	    g_LightCamera.SetProjParams(AMD_PI / 2.0f, 1.0f, 0.01f, 25.0f);

	    if (g_ShadowsExecution != ShadowFX_Constants.SHADOWFX_EXECUTION_CASCADE)
	        InitializeCubeCamera(/*&g_LightCamera,*/ g_CubeCamera/*, g_LightData*/);
	    else
	        InitializeCascadeCamera(/*&g_ViewerCamera,*/ g_CubeCamera, /*g_LightData,*/ g_LightOrtho);

	    // Create sampler states for point, linear and point_cmp
//	    CD3D11_DEFAULT defaultDesc;
	    SamplerDesc defaultDesc = new SamplerDesc();
	    // Point
//	    CD3D11_SAMPLER_DESC ssDesc(defaultDesc);
//	    ssDesc.Filter = D3D11_FILTER_MIN_MAG_MIP_LINEAR;
//	    ssDesc.AddressU = D3D11_TEXTURE_ADDRESS_WRAP;
//	    ssDesc.AddressV = D3D11_TEXTURE_ADDRESS_WRAP;
//	    ssDesc.AddressW = D3D11_TEXTURE_ADDRESS_WRAP;
//	    V_RETURN(pd3dDevice->CreateSamplerState(&ssDesc, &g_pLinearWrapSS));
//	    DXUT_SetDebugName(g_pLinearWrapSS, "g_pLinearWrapSS");
	    g_pLinearWrapSS = SamplerUtils.createSampler(defaultDesc);
	    GLCheck.checkError();

//	    CD3D11_BLEND_DESC bsDesc(defaultDesc);
//	    pd3dDevice->CreateBlendState((const D3D11_BLEND_DESC*)&bsDesc, &g_pOpaqueBS);

//	    bsDesc.RenderTarget[0].BlendEnable = true;
//	    bsDesc.RenderTarget[0].BlendOp = D3D11_BLEND_OP_MIN;
//	    bsDesc.RenderTarget[0].BlendOpAlpha = D3D11_BLEND_OP_MIN;
//	    bsDesc.RenderTarget[0].RenderTargetWriteMask = 0x1;
//	    pd3dDevice->CreateBlendState((const D3D11_BLEND_DESC*)&bsDesc, &g_pShadowMaskChannelBS[0]);
//	    bsDesc.RenderTarget[0].RenderTargetWriteMask = 0x2;
//	    pd3dDevice->CreateBlendState((const D3D11_BLEND_DESC*)&bsDesc, &g_pShadowMaskChannelBS[1]);
//	    bsDesc.RenderTarget[0].RenderTargetWriteMask = 0x4;
//	    pd3dDevice->CreateBlendState((const D3D11_BLEND_DESC*)&bsDesc, &g_pShadowMaskChannelBS[2]);
//	    bsDesc.RenderTarget[0].RenderTargetWriteMask = 0x8;
//	    pd3dDevice->CreateBlendState((const D3D11_BLEND_DESC*)&bsDesc, &g_pShadowMaskChannelBS[3]);
	    
	    // TODO Blend state.

//	    CD3D11_RASTERIZER_DESC rsDesc(defaultDesc);
//	    rsDesc.CullMode = D3D11_CULL_NONE;
//	    rsDesc.FillMode = D3D11_FILL_SOLID;
//	    pd3dDevice->CreateRasterizerState(&rsDesc, &g_pNoCullingSolidRS);
//	    rsDesc.CullMode = D3D11_CULL_NONE;
//	    rsDesc.FillMode = D3D11_FILL_SOLID;
//	    pd3dDevice->CreateRasterizerState(&rsDesc, &g_pBackCullingSolidRS);
//	    rsDesc.CullMode = D3D11_CULL_FRONT;
//	    rsDesc.FillMode = D3D11_FILL_SOLID;
//	    pd3dDevice->CreateRasterizerState(&rsDesc, &g_pFrontCullingSolidRS);
//	    rsDesc.CullMode = D3D11_CULL_NONE;
//	    rsDesc.FillMode = D3D11_FILL_WIREFRAME;
//	    pd3dDevice->CreateRasterizerState(&rsDesc, &g_pNoCullingWireframeRS);
	    
	    // TODO Depth stencil state

	    // regular DSS
//	    CD3D11_DEPTH_STENCIL_DESC dssDesc(defaultDesc);
//	    pd3dDevice->CreateDepthStencilState(&dssDesc, &g_pDepthTestLessDSS);

//	    dssDesc.DepthFunc = D3D11_COMPARISON_LESS_EQUAL;
//	    pd3dDevice->CreateDepthStencilState(&dssDesc, &g_pDepthTestLessEqualDSS);

	    // DSS - no depth test and no depth write ; stencil test and clear stencil afterwards
//	    dssDesc = CD3D11_DEPTH_STENCIL_DESC(defaultDesc);
//	    dssDesc.DepthEnable = false;
//	    dssDesc.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ZERO;
//	    dssDesc.StencilWriteMask = 0xff;
//	    dssDesc.StencilEnable = true;
//	    dssDesc.FrontFace.StencilFunc = D3D11_COMPARISON_EQUAL;
//	    dssDesc.BackFace.StencilFunc = D3D11_COMPARISON_EQUAL;
//	    dssDesc.FrontFace.StencilFailOp = D3D11_STENCIL_OP_ZERO;
//	    dssDesc.FrontFace.StencilPassOp = D3D11_STENCIL_OP_ZERO;
//	    dssDesc.FrontFace.StencilDepthFailOp = D3D11_STENCIL_OP_ZERO;
//	    dssDesc.BackFace.StencilFailOp = D3D11_STENCIL_OP_ZERO;
//	    dssDesc.BackFace.StencilPassOp = D3D11_STENCIL_OP_ZERO;
//	    dssDesc.BackFace.StencilDepthFailOp = D3D11_STENCIL_OP_ZERO;
//	    pd3dDevice->CreateDepthStencilState(&dssDesc, &g_pStencilTestAndClearDSS);

	    // DSS - depth test enabled but no depth write ; stencil write but no test
//	    dssDesc = CD3D11_DEPTH_STENCIL_DESC(defaultDesc);
//	    dssDesc.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ZERO;
//	    dssDesc.StencilEnable = true;
//	    dssDesc.FrontFace.StencilFailOp = D3D11_STENCIL_OP_KEEP;
//	    dssDesc.FrontFace.StencilPassOp = D3D11_STENCIL_OP_KEEP;
//	    dssDesc.FrontFace.StencilDepthFailOp = D3D11_STENCIL_OP_DECR;
//	    dssDesc.BackFace.StencilFailOp = D3D11_STENCIL_OP_KEEP;
//	    dssDesc.BackFace.StencilPassOp = D3D11_STENCIL_OP_KEEP;
//	    dssDesc.BackFace.StencilDepthFailOp = D3D11_STENCIL_OP_INCR;
//	    dssDesc.StencilWriteMask = 0xff;
//	    pd3dDevice->CreateDepthStencilState(&dssDesc, &g_pDepthTestMarkStencilDSS);
	}
	
	static int                 nCount = 0;
    static float               fTimeShadowMap = 0.0f;
    static float               fTimeShadowMapFiltering = 0.0f;
    static float               fTimeDepthPrepass = 0.0f;
    static float               fSceneRendering = 0.0f;
    static float               fShadowMapMasking = 0.0f;
    static boolean                bCapture = false;

    static int                 shadowMapFrameDelay = 0;

    
    private static final int AMD_ARRAY_SIZE(int[] a) { return a != null ? a.length : 0;} 
    private static<T>    int AMD_ARRAY_SIZE(T[] a) 	 { return a != null ? a.length : 0;} 
	
	@Override
	public void display() {
//		D3D11_MAPPED_SUBRESOURCE MappedResource;
		
		onFrameMove(getFrameDeltaTime());

	    Vector4i[]              pNullSR = null;
	    ShaderProgram           pNullHS = null;
	    ShaderProgram        	pNullDS = null;
	    ShaderProgram      		pNullGS = null;
	    ShaderProgram         	pNullPS = null;
	    Texture2D[]  			pNullSRV = null;
	    Texture2D[]			    pNullRTV = null;
	    Texture2D			    pNullDSV = null;
	    int[]			        pNullSS = null;
	    CFirstPersonCamera      pNullCamera = null;
//
//	    ID3D11RenderTargetView*    pOriginalRTV = NULL;
//	    ID3D11DepthStencilView*    pOriginalDSV = NULL;

	    final  int                 showLightArea = 512;
	    // if running on an MGPU PC, update shadow map once in two frames 
	    int                 	   maxShadowMapFrameDelay = g_agsGpuCount > 1 ? 2 : 1; 
	    

//	    int shadowTextureType = g_HUD.m_GUI.GetRadioButton(IDC_RADIO_SHADOW_MAP_T2D)->GetChecked() ? AMD::SHADOWFX_TEXTURE_2D : AMD::SHADOWFX_TEXTURE_2D_ARRAY;
	    int shadowTextureType = ShadowFX_Constants.SHADOWFX_TEXTURE_2D;

//	    TIMER_Reset();
//
//	    if (g_SettingsDlg.IsActive()) // If the settings dialog is being shown, then render it instead of rendering the app's scene
//	    {
//	        g_SettingsDlg.OnRender(fElapsedTime);
//	        return;
//	    }

//	    pd3dContext->OMGetRenderTargets(1, &pOriginalRTV, &pOriginalDSV); // Store the original render target and depth buffer so we can reset it at the end of the frame

//	    pd3dContext->ClearRenderTargetView(g_ShadowMask._rtv, black.f);
//	    pd3dContext->ClearRenderTargetView(pOriginalRTV, light_blue.f);
//	    pd3dContext->ClearRenderTargetView(g_LightColor._rtv, deep_blue.f);
//	    pd3dContext->ClearRenderTargetView(g_AppNormal._rtv, grey.f);
//	    pd3dContext->ClearDepthStencilView(pOriginalDSV, D3D11_CLEAR_DEPTH, 1.0f, 0);
//	    pd3dContext->ClearDepthStencilView(g_AppDepth._dsv, D3D11_CLEAR_DEPTH | D3D11_CLEAR_STENCIL, 1.0f, 0);
//	    pd3dContext->ClearDepthStencilView(g_LightDepth._dsv, D3D11_CLEAR_DEPTH | D3D11_CLEAR_STENCIL, 1.0f, 0);
	    gl.glClearTexImage(g_ShadowMask.getTexture(), 0, TextureUtils.measureFormat(g_ShadowMask.getFormat()),
				TextureUtils.measureDataType(g_ShadowMask.getFormat()), (ByteBuffer)null);
	    GLCheck.checkError();
	    if(g_pFramebuffer == 0)
	    	g_pFramebuffer = gl.glGenFramebuffer();

		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, g_pFramebuffer);
		gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, g_LightColor.getTarget(), g_LightColor.getTexture(), 0);
		gl.glDrawBuffers(GLenum.GL_COLOR_ATTACHMENT0);
		gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(deep_blue));

		gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, g_AppNormal.getTarget(), g_AppNormal.getTexture(), 0);
		gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(grey));

		gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, g_AppNormal.getTarget(), 0, 0);
		gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_STENCIL_ATTACHMENT, g_AppDepth.getTarget(), g_AppDepth.getTexture(), 0);
		gl.glDrawBuffers(GLenum.GL_NONE);
		gl.glClearBufferfi(GLenum.GL_DEPTH_STENCIL, 0, 1, 0);

		gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_STENCIL_ATTACHMENT, g_LightDepth.getTarget(), g_LightDepth.getTexture(), 0);
		gl.glClearBufferfi(GLenum.GL_DEPTH_STENCIL, 0, 1, 0);
	    GLCheck.checkError();
	    
	    {
	        Texture2D[] pSRV = { g_ShadowMask };
	        int[]       pCB = { g_pModelCB, g_pViewerCB, g_pLightCB };
	        int[]       pSS = { g_pLinearWrapSS };

	        SetCameraConstantBufferData(/*pd3dContext,*/ g_pViewerCB, CommonUtil.toArray(g_ViewerData), CommonUtil.toArray(g_ViewerCamera), null, 1);

//	        TIMER_Begin(0, L"Depth Prepass Rendering");
	        {
	            Texture2D pRTV[] = { g_AppNormal };

	            RenderScene(//pd3dContext,
	                g_MeshArray, g_MeshModelMatrix, g_MeshArray.length,
	                CommonUtil.toArray(new Vector4f(0.0f, 0.0f, getGLContext().width(), getGLContext().height())), 1,
	                null, 0,
	                /*g_pBackCullingSolidRS*/ null, /*g_pOpaqueBS*/ null, /*white.f*/ null,
	                /*g_pDepthTestLessDSS*/ null, 0, g_pSceneIL,
	                g_pSceneVS, pNullHS, pNullDS, pNullGS, g_pDepthAndNormalPassScenePS,
	                g_pModelCB, 0, pCB, 0, AMD_ARRAY_SIZE(pCB),
	                pSS, 0, AMD_ARRAY_SIZE(pSS), pNullSRV, 1, 0,
	                pRTV, AMD_ARRAY_SIZE(pRTV), g_AppDepth,
	                g_ViewerData, pNullCamera);

	        }
//	        TIMER_End();

	        if (g_ShadowsExecution == ShadowFX_Constants.SHADOWFX_EXECUTION_CASCADE)
	            SetCameraConstantBufferData(/*pd3dContext,*/ g_pLightCB, g_LightData, g_CubeCamera, g_LightOrtho, CUBE_FACE_COUNT);
	        else
	            SetCameraConstantBufferData(/*pd3dContext,*/ g_pLightCB, g_LightData, g_CubeCamera, null, CUBE_FACE_COUNT);
/*
	#if (EXPERIMENTAL_EMULATE_GPU_2_STEP_COPY == 0) // if experimental code is diables, begin access the original shadow map
	        agsDriverExtensions_NotifyResourceBeginAllAccess(g_agsContext, g_ShadowMap._t2d);
	#endif
*/
	        if (shadowMapFrameDelay % maxShadowMapFrameDelay == 0)
	        {
	            if (shadowTextureType == ShadowFX_Constants.SHADOWFX_TEXTURE_2D)
	            {
//	                pd3dContext->ClearDepthStencilView(g_ShadowMap._dsv, D3D11_CLEAR_DEPTH, 1.0, 0); // there is only 1 shadow map, so clear it every frame
	            	
	            	
//	                TIMER_Begin(0, L"Shadow Map Rendering"); // Render shadow map into separate texture array slices
	                {
	                    for (int light = 0; light < CUBE_FACE_COUNT; light++)
	                    {
	                        int lightIndexX = light % g_ShadowMapAtlasScaleW;
	                        int lightIndexY = light / g_ShadowMapAtlasScaleW;

	                        RenderScene(//pd3dContext,
	                            g_MeshArray, g_MeshModelMatrix, AMD_ARRAY_SIZE(g_MeshArray),
									CommonUtil.toArray(new Vector4f(g_ShadowMapSize*lightIndexX, g_ShadowMapSize*lightIndexY, g_ShadowMapSize, g_ShadowMapSize)), 1,
	                            pNullSR, 0,
	                            /*g_pFrontCullingSolidRS*/null, /*g_pOpaqueBS*/null, /*white.f*/null,
	                            /*g_pDepthTestLessDSS*/null, 0, g_pSceneIL,
	                            g_pSceneVS, pNullHS, pNullDS, pNullGS, g_pDepthPassScenePS,
	                            g_pModelCB, 0, pCB, 0, AMD_ARRAY_SIZE(pCB),
	                            pSS, 0, AMD_ARRAY_SIZE(pSS), pNullSRV, 1, 0,
	                            pNullRTV, 0, g_ShadowMap,
	                            g_LightData[light], pNullCamera);

	                    }
	                }
//	                TIMER_End();
	            }

	            if (shadowTextureType == ShadowFX_Constants.SHADOWFX_TEXTURE_2D_ARRAY)
	            {
//	                TIMER_Begin(0, L"Shadow Map Rendering"); // Render shadow map into separate texture array slices
	                {
	                    for (int light = 0; light < CUBE_FACE_COUNT; light++)
	                    {
//	                        pd3dContext->ClearDepthStencilView(g_ShadowMap._dsv_cube[light], D3D11_CLEAR_DEPTH, 1.0, 0); // there is only 1 shadow map, so clear it every frame

	                        RenderScene(//pd3dContext,
	                            g_MeshArray, g_MeshModelMatrix, AMD_ARRAY_SIZE(g_MeshArray),
									CommonUtil.toArray(new Vector4f(0.0f, 0.0f, g_ShadowMapSize, g_ShadowMapSize)), 1,
	                            pNullSR, 0,
	                            /*g_pFrontCullingSolidRS*/ null, /*g_pOpaqueBS*/null, /*white.f*/ null,
	                            /*g_pDepthTestLessDSS*/null, 0, g_pSceneIL,
	                            g_pSceneVS, pNullHS, pNullDS, pNullGS, g_pDepthPassScenePS,
	                            g_pModelCB, 0, pCB, 0, AMD_ARRAY_SIZE(pCB),
	                            pSS, 0, AMD_ARRAY_SIZE(pSS), pNullSRV, 1, 0,
	                            pNullRTV, 0, g_ShadowMapSlices[light],
	                            g_LightData[light], pNullCamera);

	                    }
	                }
//	                TIMER_End();
	            }

//	#if (EXPERIMENTAL_EMULATE_GPU_2_STEP_COPY == 1) // if experimental is enabled, then only shadow map Copy needs to notify access 
//	            agsDriverExtensions_NotifyResourceBeginAllAccess(g_agsContext, g_ShadowMapCopy._t2d);
//	            pd3dContext->CopyResource(g_ShadowMapCopy._t2d, g_ShadowMap._t2d); // update the shadow map copy from current frame 
//	            agsDriverExtensions_NotifyResourceEndWrites(g_agsContext, g_ShadowMapCopy._t2d, NULL, 0, 0); // intiate transfer
//	# if (EXPERIMENTAL_DELAY_END_ALL_ACCESS == 0)
//	            agsDriverExtensions_NotifyResourceEndAllAccess(g_agsContext, g_ShadowMapCopy._t2d); // we won't be accessing this resource again in the frame!
//	# endif
//
//	#elif (EXPERIMENTAL_EMULATE_GPU_2_STEP_COPY == 0)
//	            agsDriverExtensions_NotifyResourceEndWrites(g_agsContext, g_ShadowMap._t2d, NULL, 0, 0); // if experimental is disabled - we can only tell the driver that shadow map is done updating
//	#endif
//	        }
//	        else
//	        {
//	#if (EXPERIMENTAL_EMULATE_GPU_2_STEP_COPY == 1) // if experimental is enabled, then only shadow map Copy needs to notify access 
//	            agsDriverExtensions_NotifyResourceBeginAllAccess(g_agsContext, g_ShadowMapCopy._t2d); // 
//	            pd3dContext->CopyResource(g_ShadowMap._t2d, g_ShadowMapCopy._t2d);
//	# if (EXPERIMENTAL_DELAY_END_ALL_ACCESS == 0)
//	            agsDriverExtensions_NotifyResourceEndAllAccess(g_agsContext, g_ShadowMapCopy._t2d); // we won't be accessing this resource again in the frame!
//	# endif
//	#endif
	        }

	        shadowMapFrameDelay++;

//	        TIMER_Begin(0, L"Shadow Map Masking");
	        {
	            for (int light = 0; light < CUBE_FACE_COUNT; light++)
	            {
//	                pd3dContext->Map(g_pUnitCubeCB, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource);
//	                S_UNIT_CUBE_TRANSFORM* pUnitCubeCB = (S_UNIT_CUBE_TRANSFORM*)MappedResource.pData;
	            	S_UNIT_CUBE_TRANSFORM pUnitCubeCB = m_CubeTransform;
	                {
//	                    pUnitCubeCB->m_Transform = g_ViewerData.m_ViewProjection  * g_LightData[light].m_ViewProjectionInv;
	                	Matrix4f.mul(g_LightData[light].m_ViewProjectionInv, g_ViewerData.m_ViewProjection, pUnitCubeCB.m_Transform);
	                    pUnitCubeCB.m_Inverse.load(g_LightData[light].m_ViewProjectionInv);
	                    pUnitCubeCB.m_Forward.load(g_ViewerData.m_ViewProjection);
	                    pUnitCubeCB.m_Color.set(white);
	                }
//	                pd3dContext->Unmap(g_pUnitCubeCB, 0);
	                updateBuffer(pUnitCubeCB, S_UNIT_CUBE_TRANSFORM.SIZE, g_pUnitCubeCB);

	                RenderUnitCube( // pd3dContext,
	                    new Vector4f(0.0f, 0.0f, getGLContext().width(), getGLContext().height()),
	                    pNullSR, 0,
	                    /*g_pNoCullingSolidRS*/ null,
	                    /*g_pOpaqueBS*/null, /*white.f*/ null,
	                    /*g_pDepthTestMarkStencilDSS*/ null, 1,
	                    g_pUnitCubeVS, pNullHS, pNullDS, pNullGS, pNullPS,
	                    new int[]{g_pUnitCubeCB}, 0, 1,
	                    pNullSS, 0, 0,
	                    pNullSRV, 0, 0,
	                    pNullRTV, 0, g_AppDepth);
	            }
	        }
//	        TIMER_End();

	        g_ShadowsDesc.m_Execution = g_ShadowsExecution;

//	        TIMER_Begin(0, L"Shadow Map Filtering");
	        {
	            g_ShadowsDesc.m_NormalOption = ShadowFX_Constants.SHADOWFX_NORMAL_OPTION_NONE;
//	            if (g_HUD.m_GUI.GetRadioButton(IDC_RADIO_SHADOWFX_NORMAL_OPTION_CALC)->GetChecked())
//	                g_ShadowsDesc.m_NormalOption = AMD::SHADOWFX_NORMAL_OPTION_CALC_FROM_DEPTH;
//	            if (g_HUD.m_GUI.GetRadioButton(IDC_RADIO_SHADOWFX_NORMAL_OPTION_READ)->GetChecked())
//	                g_ShadowsDesc.m_NormalOption = AMD::SHADOWFX_NORMAL_OPTION_READ_FROM_SRV;

//	            if (g_HUD.m_GUI.GetCheckBox(IDC_CHECKBOX_FILTERING)->GetChecked())
	            {
//	                g_ShadowsDesc.m_TapType = g_HUD.m_GUI.GetRadioButton(IDC_RADIO_SHADOWFX_TAP_TYPE_FIXED)->GetChecked() ? AMD::SHADOWFX_TAP_TYPE_FIXED : AMD::SHADOWFX_TAP_TYPE_POISSON;
//	                g_ShadowsDesc.m_Filtering = g_HUD.m_GUI.GetRadioButton(IDC_RADIO_SHADOWFX_FILTERING_UNIFORM)->GetChecked() ? AMD::SHADOWFX_FILTERING_UNIFORM : AMD::SHADOWFX_FILTERING_CONTACT;
//	                g_ShadowsDesc.m_TextureFetch = g_HUD.m_GUI.GetRadioButton(IDC_RADIO_SHADOWFX_TEXTURE_FETCH_GATHER4)->GetChecked() ? AMD::SHADOWFX_TEXTURE_FETCH_GATHER4 : AMD::SHADOWFX_TEXTURE_FETCH_PCF;

	                g_ShadowsDesc.m_TapType = ShadowFX_Constants.SHADOWFX_TAP_TYPE_POISSON;
	                g_ShadowsDesc.m_Filtering = ShadowFX_Constants.SHADOWFX_FILTERING_CONTACT;
	                g_ShadowsDesc.m_TextureFetch = ShadowFX_Constants.SHADOWFX_TEXTURE_FETCH_GATHER4;
	                
	                g_ShadowsDesc.m_TextureType = shadowTextureType;

//	                switch (g_HUD.m_GUI.GetComboBox(IDC_COMBOBOX_SHADOWFX_FILTER_SIZE)->GetSelectedIndex())
//	                {
//	                case 0: g_ShadowsDesc.m_FilterSize = AMD::SHADOWFX_FILTER_SIZE_7;  break;
//	                case 1: g_ShadowsDesc.m_FilterSize = AMD::SHADOWFX_FILTER_SIZE_9;  break;
//	                case 2: g_ShadowsDesc.m_FilterSize = AMD::SHADOWFX_FILTER_SIZE_11; break;
//	                case 3: g_ShadowsDesc.m_FilterSize = AMD::SHADOWFX_FILTER_SIZE_13; break;
//	                case 4: g_ShadowsDesc.m_FilterSize = AMD::SHADOWFX_FILTER_SIZE_15; break;
//	                }
	                
	                g_ShadowsDesc.m_FilterSize = ShadowFX_Constants.SHADOWFX_FILTER_SIZE_9;
	            }
//	            else
//	            {
//	                g_ShadowsDesc.m_Filtering = AMD::SHADOWFX_FILTERING_DEBUG_POINT;
//	            }

//	            Vector2f backbufferDim = new Vector2f(nvApp.width(), nvApp.height());
	            Vector2f shadowAtlasRegionDim = new Vector2f(g_ShadowMapSize, g_ShadowMapSize);

	            g_ShadowsDesc.m_ActiveLightCount = CUBE_FACE_COUNT;

//	            memcpy(&g_ShadowsDesc.m_Viewer.m_View, &g_ViewerData.m_View, sizeof(g_ShadowsDesc.m_Viewer.m_View));
//	            memcpy(&g_ShadowsDesc.m_Viewer.m_Projection, &g_ViewerData.m_Projection, sizeof(g_ShadowsDesc.m_Viewer.m_Projection));
//	            memcpy(&g_ShadowsDesc.m_Viewer.m_ViewProjection, &g_ViewerData.m_ViewProjection, sizeof(g_ShadowsDesc.m_Viewer.m_ViewProjection));
//	            memcpy(&g_ShadowsDesc.m_Viewer.m_View_Inv, &g_ViewerData.m_ViewInv, sizeof(g_ShadowsDesc.m_Viewer.m_View_Inv));
//	            memcpy(&g_ShadowsDesc.m_Viewer.m_Projection_Inv, &g_ViewerData.m_ProjectionInv, sizeof(g_ShadowsDesc.m_Viewer.m_Projection_Inv));
//	            memcpy(&g_ShadowsDesc.m_Viewer.m_ViewProjection_Inv, &g_ViewerData.m_ViewProjectionInv, sizeof(g_ShadowsDesc.m_Viewer.m_ViewProjection_Inv));
//	            memcpy(&g_ShadowsDesc.m_Viewer.m_Position, &g_ViewerData.m_Position, sizeof(g_ShadowsDesc.m_Viewer.m_Position));
//	            memcpy(&g_ShadowsDesc.m_Viewer.m_Direction, &g_ViewerData.m_Direction, sizeof(g_ShadowsDesc.m_Viewer.m_Direction));
//	            memcpy(&g_ShadowsDesc.m_Viewer.m_Up, &g_ViewerData.m_Up, sizeof(g_ShadowsDesc.m_Viewer.m_Up));
//	            memcpy(&g_ShadowsDesc.m_Viewer.m_Color, &g_ViewerData.m_Color, sizeof(g_ShadowsDesc.m_Viewer.m_Color));
//	            memcpy(&g_ShadowsDesc.m_DepthSize, &backbufferDim, sizeof(g_ShadowsDesc.m_DepthSize));
	            g_ShadowsDesc.m_Viewer.m_View.load(g_ViewerData.m_View);
	            g_ShadowsDesc.m_Viewer.m_Projection.load(g_ViewerData.m_Projection);
	            g_ShadowsDesc.m_Viewer.m_ViewProjection.load(g_ViewerData.m_ViewProjection);
	            g_ShadowsDesc.m_Viewer.m_View_Inv.load(g_ViewerData.m_ViewInv);
	            g_ShadowsDesc.m_Viewer.m_Projection_Inv.load(g_ViewerData.m_ProjectionInv);
	            g_ShadowsDesc.m_Viewer.m_ViewProjection_Inv.load(g_ViewerData.m_ViewProjectionInv);
	            g_ShadowsDesc.m_Viewer.m_Position.set(g_ViewerData.m_Position);
	            g_ShadowsDesc.m_Viewer.m_Direction.set(g_ViewerData.m_Direction);
	            g_ShadowsDesc.m_Viewer.m_Up.set(g_ViewerData.m_Up);
	            g_ShadowsDesc.m_Viewer.m_Color.set(g_ViewerData.m_Color);
//	            g_ShadowsDesc.m_Viewer.m_DepthSize.set(backbufferDim);
	            

	            final Vector4f shadowRegion = new Vector4f();
	            for (int i = 0; i < CUBE_FACE_COUNT; i++)
	            {
	                int lightIndexX = i % g_ShadowMapAtlasScaleW;
	                int lightIndexY = i / g_ShadowMapAtlasScaleW;

//	                float4 shadowRegion(0.0f, 0.0f, 0.0f, 0.0f);

	                if (shadowTextureType == ShadowFX_Constants.SHADOWFX_TEXTURE_2D)
	                {
	                    shadowRegion.x = 1.0f * lightIndexX / g_ShadowMapAtlasScaleW;
	                    shadowRegion.z = 1.0f * (lightIndexX + 1.0f) / g_ShadowMapAtlasScaleW;
	                    shadowRegion.y = 1.0f * lightIndexY / g_ShadowMapAtlasScaleH;
	                    shadowRegion.w = 1.0f * (lightIndexY + 1.0f) / g_ShadowMapAtlasScaleH;

	                    g_ShadowsDesc.m_ArraySlice[i] = 0;
	                }

	                if (shadowTextureType == ShadowFX_Constants.SHADOWFX_TEXTURE_2D_ARRAY)
	                {
	                    shadowRegion.x = 0.0f;
	                    shadowRegion.z = 1.0f;
	                    shadowRegion.y = 0.0f;
	                    shadowRegion.w = 1.0f;

	                    g_ShadowsDesc.m_ArraySlice[i] = i;
	                }

//	                memcpy(&g_ShadowsDesc.m_ShadowSize[i], &shadowAtlasRegionDim, sizeof(g_ShadowsDesc.m_ShadowSize[i]));
//	                memcpy(&g_ShadowsDesc.m_ShadowRegion[i], &shadowRegion, sizeof(g_ShadowsDesc.m_ShadowRegion[i]));
//	                memcpy(&g_ShadowsDesc.m_Light[i].m_View, &g_LightData[i].m_View, sizeof(g_ShadowsDesc.m_Light[i].m_View));
//	                memcpy(&g_ShadowsDesc.m_Light[i].m_Projection, &g_LightData[i].m_Projection, sizeof(g_ShadowsDesc.m_Light[i].m_Projection));
//	                memcpy(&g_ShadowsDesc.m_Light[i].m_ViewProjection, &g_LightData[i].m_ViewProjection, sizeof(g_ShadowsDesc.m_Light[i].m_ViewProjection));
//	                memcpy(&g_ShadowsDesc.m_Light[i].m_View_Inv, &g_LightData[i].m_ViewInv, sizeof(g_ShadowsDesc.m_Light[i].m_View_Inv));
//	                memcpy(&g_ShadowsDesc.m_Light[i].m_Projection_Inv, &g_LightData[i].m_ProjectionInv, sizeof(g_ShadowsDesc.m_Light[i].m_Projection_Inv));
//	                memcpy(&g_ShadowsDesc.m_Light[i].m_ViewProjection_Inv, &g_LightData[i].m_ViewProjectionInv, sizeof(g_ShadowsDesc.m_Light[i].m_ViewProjection_Inv));

	                g_ShadowsDesc.m_ShadowSize[i].set(shadowAtlasRegionDim);
	                g_ShadowsDesc.m_ShadowRegion[i].set(shadowRegion);
	                g_ShadowsDesc.m_Light[i].m_View.load(g_LightData[i].m_View);
	                g_ShadowsDesc.m_Light[i].m_Projection.load(g_LightData[i].m_Projection);
	                g_ShadowsDesc.m_Light[i].m_ViewProjection.load(g_LightData[i].m_ViewProjection);
	                g_ShadowsDesc.m_Light[i].m_View_Inv.load(g_LightData[i].m_ViewInv);
	                g_ShadowsDesc.m_Light[i].m_Projection_Inv.load(g_LightData[i].m_ProjectionInv);
	                g_ShadowsDesc.m_Light[i].m_ViewProjection_Inv.load(g_LightData[i].m_ViewProjectionInv);
	                
//	                memcpy(&g_ShadowsDesc.m_Light[i].m_Position, &g_LightData[i].m_Position, sizeof(g_ShadowsDesc.m_Light[i].m_Position));
//	                memcpy(&g_ShadowsDesc.m_Light[i].m_Up, &g_LightData[i].m_Up, sizeof(g_ShadowsDesc.m_Light[i].m_Up));
//	                memcpy(&g_ShadowsDesc.m_Light[i].m_Direction, &g_LightData[i].m_Direction, sizeof(g_ShadowsDesc.m_Light[i].m_Direction));
	                
	                g_ShadowsDesc.m_Light[i].m_Position.set(g_LightData[i].m_Position);
	                g_ShadowsDesc.m_Light[i].m_Up.set(g_LightData[i].m_Up);
	                g_ShadowsDesc.m_Light[i].m_Direction.set(g_LightData[i].m_Direction);

	                g_ShadowsDesc.m_Light[i].m_Aspect = g_LightCamera.GetAspect();
	                g_ShadowsDesc.m_Light[i].m_Fov = g_LightCamera.GetFOV();
	                g_ShadowsDesc.m_Light[i].m_FarPlane = g_LightCamera.GetFarClip();
	                g_ShadowsDesc.m_Light[i].m_NearPlane = g_LightCamera.GetNearClip();

	                g_ShadowsDesc.m_SunArea[i] = g_SunSize * g_SunSize; // for the filtering we actually need squared size 
	                g_ShadowsDesc.m_DepthTestOffset[i] = g_DepthTestOffset;
	                g_ShadowsDesc.m_NormalOffsetScale[i] = g_NormalOffsetScale;
	            }

//	            g_ShadowsDesc.m_pContext = pd3dContext;
//	            g_ShadowsDesc.m_pDevice = pd3dDevice;
	            g_ShadowsDesc.m_pDepthSRV = g_AppDepth;
	            g_ShadowsDesc.m_pNormalSRV = g_AppNormal;
	            g_ShadowsDesc.m_pOutputRTV = g_ShadowMask;
	            g_ShadowsDesc.m_OutputChannels = 1;
	            g_ShadowsDesc.m_ReferenceDSS = 0;
	            g_ShadowsDesc.m_pOutputDSS = null;
	            g_ShadowsDesc.m_pOutputDSV = null;
	            g_ShadowsDesc.m_EnableCapture = false;

	            if (shadowTextureType == ShadowFX_Constants.SHADOWFX_TEXTURE_2D || shadowTextureType == ShadowFX_Constants.SHADOWFX_TEXTURE_2D_ARRAY)
	            {
	                g_ShadowsDesc.m_pShadowSRV = g_ShadowMap;
	            }

	            g_ShadowsDesc.m_TextureType = shadowTextureType;

	            ShadowFX_Desc.ShadowFX_Render(g_ShadowsDesc);

//	#if (EXPERIMENTAL_EMULATE_GPU_2_STEP_COPY == 0 && EXPERIMENTAL_DELAY_END_ALL_ACCESS == 0)
//	            agsDriverExtensions_NotifyResourceEndAllAccess(g_agsContext, g_ShadowMap._t2d); // this is really the last point where we need the shadow map
//	#endif
	        }
//	        TIMER_End();

	        bCapture = false;

//	        TIMER_Begin(0, L"Scene Rendering");
	        RenderScene(//pd3dContext,
	            g_MeshArray, g_MeshModelMatrix, AMD_ARRAY_SIZE(g_MeshArray),
					CommonUtil.toArray(new Vector4f(0.0f, 0.0f, getGLContext().width(), getGLContext().height())), 1,
	            pNullSR, 0,
	            /*g_pBackCullingSolidRS*/ null, /*g_pOpaqueBS*/ null, /*white.f*/ null,
	            /*g_pDepthTestLessEqualDSS*/null, 0, g_pSceneIL,
	            g_pSceneVS, pNullHS, pNullDS, pNullGS, g_pShadowedScenePS,
	            g_pModelCB, 0, pCB, 0, AMD_ARRAY_SIZE(pCB),
	            pSS, 0, AMD_ARRAY_SIZE(pSS), pSRV, 0, AMD_ARRAY_SIZE(pSRV),
	            /*pOriginalRTV*/null, 1, g_AppDepth,
	            g_ViewerData, pNullCamera);

	        for (int light = 0; light < CUBE_FACE_COUNT; light++)
	        {
//	            pd3dContext->Map(g_pUnitCubeCB, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource);
//	            S_UNIT_CUBE_TRANSFORM* pUnitCubeCB = (S_UNIT_CUBE_TRANSFORM*)MappedResource.pData;
	        	S_UNIT_CUBE_TRANSFORM pUnitCubeCB = m_CubeTransform;
	            {
//	                pUnitCubeCB->m_Transform = g_ViewerData.m_ViewProjection * g_LightData[light].m_ViewProjectionInv;
	            	Matrix4f.mul(g_LightData[light].m_ViewProjectionInv, g_ViewerData.m_ViewProjection, pUnitCubeCB.m_Transform);
	                pUnitCubeCB.m_Inverse.load(g_LightData[light].m_ViewProjectionInv);
	                pUnitCubeCB.m_Forward.load(g_ViewerData.m_ViewProjection);
	                pUnitCubeCB.m_Color.set(g_LightData[light].m_Color);
	            }
//	            pd3dContext->Unmap(g_pUnitCubeCB, 0);
	            updateBuffer(pUnitCubeCB, S_UNIT_CUBE_TRANSFORM.SIZE, g_pUnitCubeCB);

	            RenderUnitCube(//pd3dContext,
	                new Vector4f(0.0f, 0.0f, getGLContext().width(), getGLContext().height()),
	                pNullSR, 0,
	                /*g_pNoCullingWireframeRS*/ null,
	                /*g_pOpaqueBS*/null, /*white.f*/null,
	                /*g_pDepthTestLessDSS*/ null, 1,
	                g_pUnitCubeVS, pNullHS, pNullDS, pNullGS, g_pUnitCubePS,
	                new int[]{g_pUnitCubeCB}, 0, 1,
	                pNullSS, 0, 0,
	                pNullSRV, 0, 0,
	                /*pOriginalRTV*/null, /*1*/0, g_AppDepth);
	        }

	        if (g_bShowLightCamera)
	        {
	            RenderScene(//pd3dContext,
	                g_MeshArray, g_MeshModelMatrix, AMD_ARRAY_SIZE(g_MeshArray),
						CommonUtil.toArray(new Vector4f(0.0f, 0.0f, showLightArea, showLightArea)), 1,
	                pNullSR, 0,
	                /*g_pBackCullingSolidRS*/null, /*g_pOpaqueBS*/null, /*white.f*/null,
	                /*g_pDepthTestLessDSS*/null, 0, g_pSceneIL,
	                g_pSceneVS, pNullHS, pNullDS, pNullGS, g_pShadowMapPS,
	                g_pModelCB, 0, pCB, 0, AMD_ARRAY_SIZE(pCB),
	                pSS, 0, AMD_ARRAY_SIZE(pSS), pSRV, 1, AMD_ARRAY_SIZE(pSRV),
						CommonUtil.toArray(g_LightColor), 1, g_LightDepth,
	                null,
	                g_pCurrentCamera);
	        }

	        if (g_bShowShadowMapRegion)
	        {
	            RenderScene(//pd3dContext,
	                g_MeshArray, g_MeshModelMatrix, AMD_ARRAY_SIZE(g_MeshArray),
						CommonUtil.toArray(new Vector4f(0.0f, 0.0f, showLightArea, showLightArea)), 1,
	                pNullSR, 0,
	                /*g_pBackCullingSolidRS*/null, /*g_pOpaqueBS*/null, /*white.f*/null,
	                /*g_pDepthTestLessDSS*/null, 0, g_pSceneIL,
	                g_pSceneVS, pNullHS, pNullDS, pNullGS, g_pShadowMapPS,
	                g_pModelCB, 0, pCB, 0, AMD_ARRAY_SIZE(pCB),
	                pSS, 0, AMD_ARRAY_SIZE(pSS), pSRV, 1, AMD_ARRAY_SIZE(pSRV),
						CommonUtil.toArray(g_LightColor), 1, g_LightDepth,
	                g_LightData[g_CurrentLightCamera], pNullCamera);
	        }

	        if (g_bShowLightCamera || g_bShowShadowMapRegion)
	            RenderFullscreenPass(//pd3dContext,
	                new Vector4f(0.0f, 64.0f, 512.0f, 512.0f),
	                g_pFullscreenVS, g_pFullscreenPS,
	                pNullSR, 0, null, 0, CommonUtil.toInts(g_pLinearWrapSS), 1, CommonUtil.toArray(g_LightColor), 1,
	                /*pOriginalRTV*/null, /*1*/ 0, null, 0, 0, pNullDSV, null, 0, null, null);

	        if (g_bShowShadowMask)
	            RenderFullscreenPass(//pd3dContext,
	            	new Vector4f(0.0f, 0.0f, getGLContext().width(), getGLContext().height()),
	                g_pFullscreenVS, g_pFullscreenPS,
	                pNullSR, 0, null, 0, CommonUtil.toInts(g_pLinearWrapSS), 1, CommonUtil.toArray(g_ShadowMask), 1,
	                /*&pOriginalRTV*/null, /*1*/0, null, 0, 0, pNullDSV, null, 0, null, null);
//	        TIMER_End();
	    }

	    // for experimental delay of end all access - this is roughly the end of the frame, let the driver know that we are done with corresponding resource
//	#if (EXPERIMENTAL_EMULATE_GPU_2_STEP_COPY == 1 && EXPERIMENTAL_DELAY_END_ALL_ACCESS == 1)
//	    agsDriverExtensions_NotifyResourceEndAllAccess(g_agsContext, g_ShadowMapCopy._t2d);
//	#elif (EXPERIMENTAL_EMULATE_GPU_2_STEP_COPY == 0 && EXPERIMENTAL_DELAY_END_ALL_ACCESS == 1)
//	    agsDriverExtensions_NotifyResourceEndAllAccess(g_agsContext, g_ShadowMap._t2d);
//	#endif

//	    pd3dContext->RSSetViewports(1, &CD3D11_VIEWPORT(0.0f, 0.0f, (float)g_Width, (float)g_Height));
	    gl.glViewport(0, 0, getGLContext().width(), getGLContext().height());

//	    pd3dContext->OMSetRenderTargets(1, &pOriginalRTV, pOriginalDSV);
//	    SAFE_RELEASE(pOriginalRTV);
//	    SAFE_RELEASE(pOriginalDSV);
	    gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

	    // Render GUI
//	    if (g_bRenderHUD)
//	    {
//	        DXUT_BeginPerfEvent(DXUT_PERFEVENTCOLOR, L"HUD / Stats");
//
//	        {
//	            // Render the HUD
//	            if (g_bRenderHUD)
//	            {
//	                g_MagnifyTool.Render();
//	                g_HUD.OnRender(fElapsedTime);
//	            }
//	            RenderText();
//	        }
//
//	        DXUT_EndPerfEvent();
//	    }

//	    fTimeShadowMap += (float)TIMER_GetTime(Gpu, L"Shadow Map Rendering") * 1000.0f;
//	    fTimeShadowMapFiltering += (float)TIMER_GetTime(Gpu, L"Shadow Map Filtering") * 1000.0f;
//	    fTimeDepthPrepass += (float)TIMER_GetTime(Gpu, L"Depth Prepass Rendering") * 1000.0f;
//	    fShadowMapMasking += (float)TIMER_GetTime(Gpu, L"Shadow Map Masking") * 1000.0f;
//	    fSceneRendering += (float)TIMER_GetTime(Gpu, L"Scene Rendering") * 1000.0f;
//
//	    if (nCount++ == 100)
//	    {
//	        g_ShadowRenderingTime = fTimeShadowMap / (float)nCount;
//	        g_ShadowFilteringTime = fTimeShadowMapFiltering / (float)nCount;
//	        g_DepthPrepassRenderingTime = fTimeDepthPrepass / (float)nCount;
//
//	        g_ShadowMapMasking = fShadowMapMasking / (float)nCount;
//	        g_SceneRendering = fSceneRendering / (float)nCount;
//
//	        fShadowMapMasking = fSceneRendering = fTimeShadowMap = fTimeShadowMapFiltering = fTimeDepthPrepass = 0.0f;
//	        nCount = 0;
//	    }
	}
	
	@Override
	public void reshape(int width, int height) {
//		g_Height = pBackBufferSurfaceDesc->Height;
//	    g_Width = pBackBufferSurfaceDesc->Width;
		
		if(width <= 0 || height <= 0)
			return;
		
		if(g_ShadowMask == null || g_ShadowMask.getWidth() != width || g_ShadowMask.getHeight() != height){
			
//		    g_ShadowMask.CreateSurface(DXUTGetD3D11Device(),
//		        (unsigned int)pBackBufferSurfaceDesc->Width,
//		        (unsigned int)pBackBufferSurfaceDesc->Height, 1, 1, 1,
//		        DXGI_FORMAT_R16G16B16A16_UNORM, DXGI_FORMAT_R16G16B16A16_UNORM,
//		        DXGI_FORMAT_R16G16B16A16_UNORM, DXGI_FORMAT_UNKNOWN,
//		        DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_UNKNOWN,
//		        D3D11_USAGE_DEFAULT, false, 0, NULL, g_agsContext, AGS_AFR_TRANSFER_DEFAULT);
			
			if(g_ShadowMask != null)
				g_ShadowMask.dispose();
			Texture2DDesc shadowMaskDesc = new Texture2DDesc(width, height, GLenum.GL_RGBA16F);
			g_ShadowMask = TextureUtils.createTexture2D(shadowMaskDesc, null);

//		    g_AppDepth.Release();
//		    g_AppDepth.CreateSurface(DXUTGetD3D11Device(),
//		        (unsigned int)pBackBufferSurfaceDesc->Width,
//		        (unsigned int)pBackBufferSurfaceDesc->Height, 1, 1, 1,
//		        DXGI_FORMAT_R24G8_TYPELESS, DXGI_FORMAT_R24_UNORM_X8_TYPELESS,
//		        DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_D24_UNORM_S8_UINT,
//		        DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_D24_UNORM_S8_UINT,
//		        D3D11_USAGE_DEFAULT, false, 0, NULL, g_agsContext, AGS_AFR_TRANSFER_DEFAULT);
			
			if(g_AppDepth != null)
				g_AppDepth.dispose();
			Texture2DDesc appDepthDesc = new Texture2DDesc(width, height, GLenum.GL_DEPTH24_STENCIL8);
			g_AppDepth = TextureUtils.createTexture2D(appDepthDesc, null);

//		    g_AppNormal.Release();
//		    g_AppNormal.CreateSurface(DXUTGetD3D11Device(),
//		        (unsigned int)pBackBufferSurfaceDesc->Width,
//		        (unsigned int)pBackBufferSurfaceDesc->Height, 1, 1, 1,
//		        DXGI_FORMAT_R8G8B8A8_UNORM, DXGI_FORMAT_R8G8B8A8_UNORM,
//		        DXGI_FORMAT_R8G8B8A8_UNORM, DXGI_FORMAT_UNKNOWN,
//		        DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_UNKNOWN,
//		        D3D11_USAGE_DEFAULT, false, 0, NULL, g_agsContext, AGS_AFR_TRANSFER_DEFAULT);
			if(g_AppNormal != null)
				g_AppNormal.dispose();
			Texture2DDesc appNormalDesc = new Texture2DDesc(width, height, GLenum.GL_RGBA8);
			g_AppNormal = TextureUtils.createTexture2D(appNormalDesc, null);

//		    V_RETURN(g_DialogResourceManager.OnD3D11ResizedSwapChain(pd3dDevice, pBackBufferSurfaceDesc));
//		    V_RETURN(g_SettingsDlg.OnD3D11ResizedSwapChain(pd3dDevice, pBackBufferSurfaceDesc));

		    // Setup the camera's projection parameters
		    float fAspectRatio = (float)width / (float)height;
//		    g_ViewerCamera.SetProjParams(XM_PI / 4, fAspectRatio, 1.0f, 200.0f);
//		    g_ViewerData.m_BackBufferDim = float2((float)g_Width, (float)g_Height);
//		    g_ViewerData.m_BackBufferDimRcp = float2(1.0f / (float)g_Width, 1.0f / (float)g_Height);
			Matrix4f.perspective((float)Math.toDegrees(Math.PI/4), fAspectRatio, 1.0f, 200.0f, g_ViewerData.m_Projection);
			Matrix4f.invert(g_ViewerData.m_Projection, g_ViewerData.m_ProjectionInv);

		    // Set the location and size of the AMD standard HUD
//		    g_HUD.m_GUI.SetLocation(pBackBufferSurfaceDesc->Width - AMD::HUD::iDialogWidth, 0);
//		    g_HUD.m_GUI.SetSize(AMD::HUD::iDialogWidth, pBackBufferSurfaceDesc->Height);
		    // Magnify tool will capture from the color buffer
//		    g_MagnifyTool.OnResizedSwapChain(pd3dDevice, pSwapChain, pBackBufferSurfaceDesc, pUserContext, pBackBufferSurfaceDesc->Width - AMD::HUD::iDialogWidth, 0);
//		    D3D11_RENDER_TARGET_VIEW_DESC rtvDesc;
//		    ID3D11Resource* pTempRTResource;
//		    DXUTGetD3D11RenderTargetView()->GetResource(&pTempRTResource);
//		    DXUTGetD3D11RenderTargetView()->GetDesc(&rtvDesc);
//		    g_MagnifyTool.SetSourceResources(pTempRTResource, rtvDesc.Format, g_Width, g_Height, DXUTGetDXGIBackBufferSurfaceDesc()->SampleDesc.Count);
//		    g_MagnifyTool.SetPixelRegion(128);
//		    g_MagnifyTool.SetScale(5);
//		    SAFE_RELEASE(pTempRTResource);
		}
	}
	
	void InitializeCubeCamera(CFirstPersonCamera[] pCubeCamera)
	{
//	    float4 eye = pViewer->GetEyePt();
	    ReadableVector3f eye = m_transformer.getTranslationVec();
	    ReadableVector3f dir_x = Vector3f.X_AXIS;
	    ReadableVector3f dir_y = Vector3f.Y_AXIS;
	    ReadableVector3f dir_z = Vector3f.Z_AXIS;

	    float aspect = 1.0f;
	    float znear = 0.01f;
	    float zfar = 25.0f;
	    float fov = (float)Math.toDegrees(Math.PI / 2.0);

	    Vector3f[] cube_look_at = new Vector3f[6];
	    Vector3f[] cube_up      = new Vector3f[6];
	    for(int i = 0; i < 6;i++){
			cube_look_at[i] = new Vector3f();
			cube_up[i] = new Vector3f();
		}
	    
//	    cube_look_at[0] = eye + dir_x*(1.0f);
	    Vector3f.linear(eye, dir_x, 1.0f, cube_look_at[0]);
	    
//	    cube_up[0] = dir_y*(1.0f);
	    cube_up[0].set(dir_y);
	    
//	    cube_look_at[1] = eye + dir_x*(-1.0f);
	    Vector3f.linear(eye, dir_x, -1.0f, cube_look_at[1]);
	    
//	    cube_up[1] = dir_y*(1.0f);
	    cube_up[1].set(dir_y);
	    
//	    cube_look_at[2] = eye + dir_y*(1.0f);
	    Vector3f.linear(eye, dir_y, 1.0f, cube_look_at[2]);
	    
//	    cube_up[2] = dir_z*(-1.0f);
	    Vector3f.scale(dir_z, -1.0f, cube_up[2]);
	    
//	    cube_look_at[3] = eye + dir_y*(-1.0f);
	    Vector3f.linear(eye, dir_y, -1.0f, cube_look_at[3]);
	    
//	    cube_up[3] = dir_z*(1.0f);
	    cube_up[3].set(dir_z);
	    
//	    cube_look_at[4] = eye + dir_z*(1.0f);
	    Vector3f.linear(eye, dir_z, 1.0f, cube_look_at[4]);
	    
//	    cube_up[4] = dir_y*(1.0f);
	    cube_up[4].set(dir_y);
	    
//	    cube_look_at[5] = eye + dir_z*(-1.0f);
	    Vector3f.linear(eye, dir_z, -1.0f, cube_look_at[5]);
	    
//	    cube_up[5] = dir_y*(1.0f);
	    cube_up[5].set(dir_y);

	    for (int i = 0; i < 6; i++)
	    {
	    	if(pCubeCamera[i] == null)
	    		pCubeCamera[i] = new CFirstPersonCamera();
	    	
	        pCubeCamera[i].SetProjParams(fov, aspect, znear, zfar);

	        pCubeCamera[i].SetViewParams(eye, cube_look_at[i], cube_up[i]);
//
//	        pCubeCamera[i].FrameMove(0.000001f);
	    	// TODO
	    }
	}
	
	void InitializeCascadeCamera(CFirstPersonCamera[] pCubeCamera,Matrix4f[] ortho)
	{
	    ReadableVector3f eye = m_transformer.getTranslationVec();
//	    Vector3f direction = pViewer->GetWorldAhead();
//	    Vector3f right = pViewer->GetWorldRight();
	    Vector3f up = new Vector3f();
	    Matrix4f.decompseRigidMatrix(g_ViewerData.m_View, null, null, up);
	    
	    Vector3f light = new Vector3f(1.0f, 1.0f, 1.0f);

	    float aspect = 1.0f;
	    float znear = 0.01f;
	    float zfar = 25.0f;
	    float fov = (float)Math.toDegrees(Math.PI / 2.0f);

	    for (int i = 0; i < 6; i++)
	    {
	        pCubeCamera[i].SetProjParams(fov, aspect, znear, zfar);
	        pCubeCamera[i].SetViewParams(Vector3f.linear(eye,light, 2, light), eye, up);

//	        pCubeCamera[i].FrameMove(0.000001f);

	        ortho[i] = Matrix4f.ortho(-5.0f*(i + 0.5f), 5.0f*(i + 0.5f), -5.0f*(i + 0.5f), 5.0f*(i + 0.5f), -5.0f*(i + 1.5f), 5.0f*(i + 1.5f), ortho[i]); // pLightCamera->GetProjMatrix(); // XMMatrixOrthographicOffCenterLH( -10.0, 10.0, -10.0, 10.0, -30.0, 30.0 );
	    }
	}
	
	void SetCameraConstantBufferData(//ID3D11DeviceContext* pd3dContext,
		    int                                       pd3dCB,
		    S_CAMERA_DATA[]                                      pCameraData,
		    CFirstPersonCamera[]                                 pCamera,
		    Matrix4f[]                                           pProjection,
		              int                                        nCount)
		{
//		    D3D11_MAPPED_SUBRESOURCE MappedResource;

//		    if (pd3dContext == NULL) { OutputDebugString(AMD_FUNCTION_WIDE_NAME L" received a NULL D3D11 Context pointer \n");         return; }
//		    if (pd3dCB == NULL) { OutputDebugString(AMD_FUNCTION_WIDE_NAME L" received a NULL D3D11 Constant Buffer pointer \n"); return; }

		    for (int i = 0; i < nCount; i++)
		    {
		        CFirstPersonCamera camera = pCamera[i];
		        S_CAMERA_DATA cameraData = pCameraData[i];

		        Matrix4f  view = camera.GetViewMatrix();
		        Matrix4f  proj = pProjection != null ? pProjection[i] : camera.GetProjMatrix();
		        Matrix4f  viewproj = cameraData.m_ViewProjection;
		        Matrix4f.mul(proj, view, viewproj);
//		        Matrix4f  view_inv = XMMatrixInverse(&XMMatrixDeterminant(view), view);
		        Matrix4f  view_inv = cameraData.m_ViewInv;
		        Matrix4f.invert(view, view_inv);
//		        Matrix4f  proj_inv = XMMatrixInverse(&XMMatrixDeterminant(proj), proj);
		        Matrix4f  proj_inv = cameraData.m_ProjectionInv;
		        Matrix4f.invert(proj, proj_inv);
		        
//		        Matrix4f  viewproj_inv = XMMatrixInverse(&XMMatrixDeterminant(viewproj), viewproj);
		        Matrix4f  viewproj_inv = cameraData.m_ViewProjectionInv;
		        Matrix4f.invert(viewproj, viewproj_inv);

		        cameraData.m_View.load(view);
		        cameraData.m_Projection.load(proj);
//		        cameraData.m_ViewInv = XMMatrixTranspose(view_inv);
//		        cameraData.m_ProjectionInv = XMMatrixTranspose(proj_inv);
//		        cameraData.m_ViewProjection = XMMatrixTranspose(viewproj);
//		        cameraData.m_ViewProjectionInv = XMMatrixTranspose(viewproj_inv);

		        cameraData.m_Position.set(camera.GetEyePt());
//		        cameraData.m_Direction = XMVector3Normalize(camera.GetLookAtPt() - camera.GetEyePt());
		        Vector3f.sub(camera.GetLookAtPt(), camera.GetEyePt(), cameraData.m_Direction);
		        cameraData.m_Direction.w = 0;
		        cameraData.m_Direction.normalise();
		        
		        cameraData.m_Up.set(camera.GetWorldUp());
		        cameraData.m_Fov = camera.GetFOV();
		        cameraData.m_Aspect = camera.GetAspect();
		        cameraData.m_zNear = camera.GetNearClip();
		        cameraData.m_zFar = camera.GetFarClip();
		    }

//		    pd3dContext->Map(pd3dCB, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource);
//		    if (MappedResource.pData)
//		    {
//		        memcpy(MappedResource.pData, pCameraData, sizeof(S_CAMERA_DATA) * nCount);
//		    }
//		    pd3dContext->Unmap(pd3dCB, 0);
		    
		    updateBuffer(pCameraData, S_CAMERA_DATA.SIZE * nCount, pd3dCB);
		}
	
	private final int[] pNullSRV = new int[12];
	//--------------------------------------------------------------------------------------
	// Render the scene (either for the main scene or the shadow map scene)
	//--------------------------------------------------------------------------------------
	void RenderScene(//ID3D11DeviceContext*  pd3dContext,
		 AMD_Mesh[]                           pMesh,
	    Matrix4f[]                           pModelMatrix,
	    int			                         nMeshCount,

	    Vector4f[]                     		 pVP,           // ViewPort array
	    int    			                     nVPCount,      // Viewport count

	    Vector4i[]                           pSR,           // Scissor Rects array
	    int 		                         nSRCount,      // Scissor rect count

	    Runnable              				 pRS,           // Raster State

	    Runnable                   			 pBS,           // Blend State
	    float[] 	                         pFactorBS,     // Blend state factor

	    Runnable 				             pDSS,          // Depth Stencil State
	    int 		                         dssRef,        // Depth stencil state reference value

	    int 			                     pIL,           // Input Layout
	    ShaderProgram	                     pVS,           // Vertex Shader
	    ShaderProgram                    	 pHS,           // Hull Shader
	    ShaderProgram                 		 pDS,           // Domain Shader
	    ShaderProgram               		 pGS,           // Geometry SHader
	    ShaderProgram                  		 pPS,           // Pixel Shader

	    int         		                 pModelCB,
	    int 		                         nModelCBSlot,

	    int[]		                         ppCB,          // Constant Buffer array
	    int 		                         nCBStart,      // First slot to attach constant buffer array
	    int 		                         nCBCount,      // Number of constant buffers in the array

	    int[] 				                 ppSS,          // Sampler State array
	    int                        			 nSSStart,      // First slot to attach sampler state array
	    int                        			 nSSCount,      // Number of sampler states in the array

	    Texture2D[]				             ppSRV,         // Shader Resource View array
	    int                                  nSRVStart,     // First slot to attach sr views array
	    int  		                         nSRVCount,     // Number of sr views in the array

	    Texture2D[]			                 ppRTV,         // Render Target View array
	    int                         		 nRTVCount,     // Number of rt views in the array
	    Texture2D 			                 pDSV,          // Depth Stencil View

	    S_CAMERA_DATA                        pViewerData,
	    CFirstPersonCamera                   pCamera)
	{
//	    ID3D11RenderTargetView *   const pNullRTV[8] = { 0 };
//	    ID3D11ShaderResourceView * const pNullSRV[128] = { 0 };

	    // Unbind anything that could be still bound on input or output
	    // If this doesn't happen, DX Runtime will spam with warnings
//	    pd3dContext->OMSetRenderTargets(AMD_ARRAY_SIZE(pNullRTV), pNullRTV, NULL);
//	    pd3dContext->CSSetShaderResources(0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV);
//	    pd3dContext->VSSetShaderResources(0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV);
//	    pd3dContext->HSSetShaderResources(0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV);
//	    pd3dContext->DSSetShaderResources(0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV);
//	    pd3dContext->GSSetShaderResources(0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV);
//	    pd3dContext->PSSetShaderResources(0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV);
		gl.glBindTextures(0, CacheBuffer.wrap(pNullSRV));  // unbind the texture resources.

//	    pd3dContext->IASetInputLayout(pIL);
		gl.glBindVertexArray(pIL);

//	    pd3dContext->VSSetShader(pVS, NULL, 0);
//	    pd3dContext->HSSetShader(pHS, NULL, 0);
//	    pd3dContext->DSSetShader(pDS, NULL, 0);
//	    pd3dContext->GSSetShader(pGS, NULL, 0);
//	    pd3dContext->PSSetShader(pPS, NULL, 0);
		if(g_pProgramPipeline == null){
			g_pProgramPipeline = new GLSLProgramPipeline();
		}
		
//		g_pProgramPipeline.setRenderShader(null, null, null, null, null);
		g_pProgramPipeline.enable();
		g_pProgramPipeline.setVS(null);
		g_pProgramPipeline.setTC(null);
		g_pProgramPipeline.setTE(null);
		g_pProgramPipeline.setGS(null);
		g_pProgramPipeline.setPS(null);

	    if (nSSCount > 0)
	    {
//	        pd3dContext->VSSetSamplers(nSSStart, nSSCount, ppSS);
//	        pd3dContext->HSSetSamplers(nSSStart, nSSCount, ppSS);
//	        pd3dContext->DSSetSamplers(nSSStart, nSSCount, ppSS);
//	        pd3dContext->GSSetSamplers(nSSStart, nSSCount, ppSS);
//	        pd3dContext->PSSetSamplers(nSSStart, nSSCount, ppSS);
	    	
	    	gl.glBindSamplers(nSSStart, CacheBuffer.wrap(ppSS));
	    }

	    if (nSRVCount > 0)
	    {
//	        pd3dContext->VSSetShaderResources(nSRVStart, nSRVCount, ppSRV);
//	        pd3dContext->HSSetShaderResources(nSRVStart, nSRVCount, ppSRV);
//	        pd3dContext->DSSetShaderResources(nSRVStart, nSRVCount, ppSRV);
//	        pd3dContext->GSSetShaderResources(nSRVStart, nSRVCount, ppSRV);
//	        pd3dContext->PSSetShaderResources(nSRVStart, nSRVCount, ppSRV);
	    	for(int i = 0; i < nSRVCount; i++){
				gl.glBindTextureUnit(nSRVStart + i, ppSRV[i].getTexture());
	    	}
	    }

	    if (nCBCount > 0)
	    {
//	        pd3dContext->VSSetConstantBuffers(nCBStart, nCBCount, ppCB);
//	        pd3dContext->HSSetConstantBuffers(nCBStart, nCBCount, ppCB);
//	        pd3dContext->DSSetConstantBuffers(nCBStart, nCBCount, ppCB);
//	        pd3dContext->GSSetConstantBuffers(nCBStart, nCBCount, ppCB);
//	        pd3dContext->PSSetConstantBuffers(nCBStart, nCBCount, ppCB);
	    	
	    	// TODO Uniform buffers
	    	for(int i = 0; i < nCBCount; i++){
				gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, nCBStart + i, ppCB[i]);
	    	}
	    }
	    
	    if(g_pFramebuffer == 0)
	    	g_pFramebuffer = gl.glGenFramebuffer();

//	    pd3dContext->OMSetRenderTargets(nRTVCount, ppRTV, pDSV);
		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, g_pFramebuffer);
	    IntBuffer drawbuffers = CacheBuffer.getCachedIntBuffer(nRTVCount);
	    for(int i = 0;i < nRTVCount; i++){
	    	int attachment = GLenum.GL_COLOR_ATTACHMENT0 + i;
			gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, attachment, ppRTV[i].getTexture(), 0);
	    	drawbuffers.put(attachment);
	    }
	    if(pDSV != null){
//	    	int attachment = TextureUtils.measureFormat(internalFormat) TODO
			gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_STENCIL_ATTACHMENT, pDSV.getTexture(), 0);
			gl.glClearBufferfi(GLenum.GL_DEPTH_STENCIL, 0, 1, 0);
	    }
	    
	    drawbuffers.flip();
		gl.glDrawBuffers(drawbuffers);
	    
//	    pd3dContext->OMSetBlendState(pBS, pFactorBS, 0xf);
	    if(pBS != null){
	    	pBS.run();
	    }else{
			gl.glDisable(GLenum.GL_BLEND);
	    }
	    
//	    pd3dContext->OMSetDepthStencilState(pDSS, dssRef);
	    if(pDSS != null){
	    	pDSS.run();
	    }else{
			gl.glEnable(GLenum.GL_DEPTH_TEST);
			gl.glDepthFunc(GLenum.GL_LESS);
	    }
	    
//	    pd3dContext->RSSetState(pRS);
	    if(pRS != null){
	    	pRS.run();
	    }else{
			gl.glDisable(GLenum.GL_CULL_FACE);
	    }
	    
//	    pd3dContext->RSSetScissorRects(nSRCount, pSR);
	    if(nSRCount > 0){
			gl.glEnable(GLenum.GL_SCISSOR_TEST);
	    	for(int i = 0; i < nSRCount; i++){
				gl.glScissorIndexed(i, pSR[i].x, pSR[i].y, pSR[i].z, pSR[i].w);
	    	}
	    }else{
			gl.glDisable(GLenum.GL_SCISSOR_TEST);
	    }
	    
//	    pd3dContext->RSSetViewports(nVPCount, pVP);
	    if(nVPCount > 0){
	    	for(int i = 0; i < nVPCount; i++){
				gl.glViewportIndexedf(i, pVP[i].x, pVP[i].y, pVP[i].z, pVP[i].w);
	    	}
	    }else{
	    	// TODO At least 1.
	    }
	    
	    GLCheck.checkError();

	    // Setup the view matrices
//	    XMMATRIX view = pCamera != NULL ? pCamera->GetViewMatrix() : XMMatrixTranspose(pViewerData->m_View);
//	    XMMATRIX proj = pCamera != NULL ? pCamera->GetProjMatrix() : XMMatrixTranspose(pViewerData->m_Projection);
//	    XMMATRIX viewproj = view * proj; TODO
	    Matrix4f viewproj = pViewerData.m_ViewProjection;

	    for (int mesh = 0; mesh < nMeshCount; mesh++)
	    {
	        SetModelMatrices(/*pd3dContext,*/ pModelCB,
	            pModelMatrix[mesh], viewproj);

	        pMesh[mesh].Render();
	    }
	    
	    GLCheck.checkError();
		gl.glBindVertexArray(0);
	}
	
	void RenderUnitCube( //ID3D11DeviceContext*       pd3dContext,
	        Vector4f                                  		VP,
	        Vector4i[]                                      pSR,   int nSRCount,
	        /*ID3D11RasterizerState **/ Runnable            pRS,
	        /*ID3D11BlendState **/ Runnable                 pBS,   final float bsFactor[],
	        /*ID3D11DepthStencilState**/ Runnable           pDSS,  int stencilRef,
	        ShaderProgram	                                pVS,
	        ShaderProgram  	                                pHS,
	        ShaderProgram      		                        pDS,
	        ShaderProgram            		                pGS,
	        ShaderProgram                    	            pPS,
	        int[]	    	                                ppCB,  int nCBStart,  int nCBCount,
	        int[]			                                ppSS,  int nSSStart,  int nSSCount,
	        Texture2D[] 			                        ppSRV, int nSRVStart, int nSRVCount,
	        Texture2D[]									    ppRTV, int nRTVCount,
	        Texture2D			                            pDSV)
	    {
	        // Useful common locals
//	        ID3D11RenderTargetView *   const pNullRTV[8]    = { 0 };
//	        ID3D11ShaderResourceView * const pNullSRV[128]  = { 0 };
//	        ID3D11Buffer *             const pNullBuffer[8] = { 0 };

	        // Unbind anything that could be still bound on input or output
	        // If this doesn't happen, DX Runtime will spam with warnings
//	        pd3dContext->OMSetRenderTargets( AMD_ARRAY_SIZE(pNullRTV), pNullRTV, NULL );
//	        pd3dContext->CSSetShaderResources( 0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV );
//	        pd3dContext->VSSetShaderResources( 0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV );
//	        pd3dContext->HSSetShaderResources( 0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV );
//	        pd3dContext->DSSetShaderResources( 0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV );
//	        pd3dContext->GSSetShaderResources( 0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV );
//	        pd3dContext->PSSetShaderResources( 0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV );

//	        UINT NullStride[8] = { 0 };
//	        UINT NullOffset[8] = { 0 };

	        if ( /*pd3dContext == NULL || */pVS == null || (ppRTV == null && pDSV == null) )
	        {
//	            AMD_OUTPUT_DEBUG_STRING("Invalid interface pointers in function %s\n", AMD_FUNCTION_NAME);
//	            return E_POINTER;
	        	throw new NullPointerException();
	        }

//	        pd3dContext->VSSetShader( pVS, NULL, 0 );
//	        pd3dContext->HSSetShader( pHS, NULL, 0 );
//	        pd3dContext->DSSetShader( pDS, NULL, 0 );
//	        pd3dContext->GSSetShader( pGS, NULL, 0 );
//	        pd3dContext->PSSetShader( pPS, NULL, 0 );

	        if (nSSCount > 0)
	        {
//	            pd3dContext->VSSetSamplers( nSSStart, nSSCount, ppSS );
//	            pd3dContext->HSSetSamplers( nSSStart, nSSCount, ppSS );
//	            pd3dContext->DSSetSamplers( nSSStart, nSSCount, ppSS );
//	            pd3dContext->GSSetSamplers( nSSStart, nSSCount, ppSS );
//	            pd3dContext->PSSetSamplers( nSSStart, nSSCount, ppSS );
	        	gl.glBindSamplers(nSSStart, CacheBuffer.wrap(ppSS));
	        }

	        if (nSRVCount > 0)
	        {
//	            pd3dContext->VSSetShaderResources( nSRVStart, nSRVCount, ppSRV );
//	            pd3dContext->HSSetShaderResources( nSRVStart, nSRVCount, ppSRV );
//	            pd3dContext->DSSetShaderResources( nSRVStart, nSRVCount, ppSRV );
//	            pd3dContext->GSSetShaderResources( nSRVStart, nSRVCount, ppSRV );
//	            pd3dContext->PSSetShaderResources( nSRVStart, nSRVCount, ppSRV );
	        	
	        	for(int i = 0; i < nSRVCount; i++){
	        		Texture2D tex = ppSRV[i];
	        		if(tex != null){
	        			gl.glBindTextureUnit(nSRVStart + i, tex.getTexture());
	        		}else{
	        			gl.glBindTextureUnit(nSRVStart + i, 0);
	        		}
	        	}
	        }

	        if (nCBCount > 0)
	        {
//	            pd3dContext->VSSetConstantBuffers( nCBStart, nCBCount, ppCB );
//	            pd3dContext->HSSetConstantBuffers( nCBStart, nCBCount, ppCB );
//	            pd3dContext->DSSetConstantBuffers( nCBStart, nCBCount, ppCB );
//	            pd3dContext->GSSetConstantBuffers( nCBStart, nCBCount, ppCB );
//	            pd3dContext->PSSetConstantBuffers( nCBStart, nCBCount, ppCB );
	        	
	        	for(int i = 0; i < nCBCount; i++){
					gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, nCBStart + i, ppCB[i]);
	        	}
	        }

//	        pd3dContext->OMSetDepthStencilState( pDSS, stencilRef );
	        if(pDSS != null){
	        	pDSS.run();
	        }else{
				gl.glEnable(GLenum.GL_DEPTH_TEST);
				gl.glDepthFunc(GLenum.GL_LESS);
				gl.glDepthMask(true);
				gl.glDisable(GLenum.GL_STENCIL_TEST);
	        }
	        
//	        pd3dContext->OMSetRenderTargets( nRTVCount, (ID3D11RenderTargetView*const*)ppRTV, pDSV );
	        if(g_pFramebuffer == 0){
	        	g_pFramebuffer = gl.glGenFramebuffer();
	        }
	        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, g_pFramebuffer);
	        IntBuffer drawbuffers = CacheBuffer.getCachedIntBuffer(nRTVCount);
		    for(int i = 0;i < nRTVCount; i++){
		    	int attachment = GLenum.GL_COLOR_ATTACHMENT0 + i;
		    	gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, attachment, ppRTV[i].getTexture(), 0);
		    	drawbuffers.put(attachment);
		    }
		    if(pDSV != null){
//		    	int attachment = TextureUtils.measureFormat(internalFormat) TODO
		    	gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_STENCIL_ATTACHMENT, pDSV.getTexture(), 0);
				gl.glClearBufferfi(GLenum.GL_DEPTH_STENCIL, 0, 1, 0);
		    }
		    
		    drawbuffers.flip();
			gl.glDrawBuffers(drawbuffers);
	        
//	        pd3dContext->OMSetBlendState(pBS, bsFactor, 0xFFFFFFFF);
		    if(pBS != null){
		    	pBS.run();
		    }else{
				gl.glDisable(GLenum.GL_BLEND);
		    }

//	        pd3dContext->RSSetViewports( 1, &VP );
			gl.glViewportArrayv(0, CacheBuffer.wrap(VP));
//	        pd3dContext->RSSetScissorRects( nSRCount, pSR );
//	        pd3dContext->RSSetState( pRS );
		    if(nSRCount > 0){
				gl.glEnable(GLenum.GL_SCISSOR_TEST);
		    	for(int i = 0; i < nSRCount; i++){
					gl.glScissorIndexed(i, pSR[i].x, pSR[i].y, pSR[i].z, pSR[i].w);
		    	}
		    }else{
				gl.glDisable(GLenum.GL_SCISSOR_TEST);
		    }

//	        pd3dContext->IASetInputLayout( NULL );
//	        pd3dContext->IASetVertexBuffers( 0, AMD_ARRAY_SIZE(pNullBuffer), pNullBuffer, NullStride, NullOffset );
//	        pd3dContext->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST );
//
//	        pd3dContext->Draw( 36, 0 );
			gl.glBindVertexArray(0);
			gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 36);

	        // Unbind RTVs and SRVs back to NULL (otherwise D3D will throw warnings)
//	        pd3dContext->OMSetRenderTargets( AMD_ARRAY_SIZE(pNullRTV), pNullRTV, NULL );
//	        pd3dContext->PSSetShaderResources( 0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV );

			gl.glBindTextures(0, CacheBuffer.wrap(pNullSRV));
			gl.glDisable(GLenum.GL_SCISSOR_TEST);
	    }
	
	void RenderFullscreenPass(
//	        ID3D11DeviceContext*        pDeviceContext,
	        Vector4f              		Viewport,
	        ShaderProgram         		pVS,
	        ShaderProgram          		pPS,
	        Vector4i[]                  pScissor,   int uNumSR,
	        int[]		                ppCB,       int uNumCBs,
	        int[]				        ppSamplers, int uNumSamplers,
	        Texture2D[]				    ppSRVs,     int uNumSRVs,
	        Texture2D[]				    ppRTVs,     int uNumRTVs,
	        Texture2D[]				    ppUAVs,     int uStartUAV, int uNumUAVs,
	        Texture2D			        pDSV,
	        Runnable    				pOutputDSS, int uStencilRef,
	        Runnable		            pOutputBS,
	        Runnable			        pOutputRS)
	    {
	        RenderFullscreenInstancedPass(//pDeviceContext,
	                                             Viewport,
	                                             pVS, null, pPS,
	                                             pScissor, uNumSR,
	                                             ppCB, uNumCBs,
	                                             ppSamplers, uNumSamplers,
	                                             ppSRVs, uNumSRVs,
	                                             ppRTVs, uNumRTVs,
	                                             ppUAVs, uStartUAV, uNumUAVs,
	                                             pDSV, pOutputDSS, uStencilRef,
	                                             pOutputBS, pOutputRS, 1);
	    }

    void RenderFullscreenInstancedPass(
//	        ID3D11DeviceContext*        pDeviceContext,
    	Vector4f	                Viewport,
        ShaderProgram	            pVS,
        ShaderProgram		        pGS,
        ShaderProgram	            pPS,
        Vector4i[]                  pScissor,   int uNumSR,
        int[]		                ppCB,       int uNumCBs,
        int[]				        ppSamplers, int uNumSamplers,
        Texture2D[]				    ppSRVs,     int uNumSRVs,
        Texture2D[]				    ppRTVs,     int uNumRTVs,
        Texture2D[]				    ppUAVs,     int uStartUAV, int uNumUAVs,
        Texture2D     				pDSV,
        Runnable				    pOutputDSS, int uStencilRef,
        Runnable         		 	pOutputBS,
        Runnable				    pOutputRS,
        int                			instanceCount)
    {
        float white[] = {1.0f, 1.0f, 1.0f, 1.0f};
//	        ID3D11ShaderResourceView*  pNullSRV[8]    = { NULL };
//	        ID3D11RenderTargetView*    pNullRTV[8]    = { NULL };
//	        ID3D11UnorderedAccessView* pNullUAV[8]    = { NULL };
//	        ID3D11Buffer*              pNullBuffer[8] = { NULL };
//	        uint NullStride[8] = { 0 };
//	        uint NullOffset[8] = { 0 };

        if (((pVS == null && pPS == null) || (ppRTVs == null && pDSV == null && ppUAVs == null)))
        {
//	            AMD_OUTPUT_DEBUG_STRING("Invalid pointer argument in function %s\n", AMD_FUNCTION_NAME);
//	            return E_POINTER;
        	throw new NullPointerException();
        }

        {// binding the render targets
//        	pDeviceContext->OMSetDepthStencilState( pOutputDSS, uStencilRef );
//            if (ppUAVs == NULL)
//            {
//                pDeviceContext->OMSetRenderTargets( uNumRTVs, (ID3D11RenderTargetView*const*)ppRTVs, pDSV );
//            }
//            else
//            {
//                pDeviceContext->OMSetRenderTargetsAndUnorderedAccessViews( uNumRTVs, (ID3D11RenderTargetView*const*)ppRTVs, pDSV, uStartUAV, uNumUAVs, ppUAVs, NULL);
//            }
        	if(pOutputDSS != null){
        		pOutputDSS.run();
        	}
        	
        	gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, g_pFramebuffer);
        	
        	if(pDSV != null){
				gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_STENCIL_ATTACHMENT, pDSV.getTarget(), pDSV.getTexture(), 0);
        	}
        	
        	int nRTVCount = uNumRTVs;
        	if(nRTVCount > 0){
	        	IntBuffer drawbuffers = CacheBuffer.getCachedIntBuffer(nRTVCount);
			    for(int i = 0;i < nRTVCount; i++){
			    	int attachment = GLenum.GL_COLOR_ATTACHMENT0 + i;
					gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, attachment, ppRTVs[i].getTexture(), 0);
			    	drawbuffers.put(attachment);
			    }
			    drawbuffers.flip();
				gl.glDrawBuffers(drawbuffers);
        	}
        	
        	int nUAVCount = AMD_ARRAY_SIZE(ppUAVs);
        	for(int i = 0; i < nUAVCount; i++){
        		Texture2D texture = ppUAVs[i];
				gl.glBindImageTexture(uStartUAV + i, texture != null ? texture.getTexture() : 0, 0, false, 0,
						GLenum.GL_READ_WRITE, texture != null ? texture.getFormat() : GLenum.GL_RGBA8);
        	}
        }
//        pDeviceContext->OMSetBlendState(pOutputBS, white, 0xFFFFFFFF);
        if(pOutputBS != null){
        	pOutputBS.run();
        }else{
			gl.glDisable(GLenum.GL_BLEND);
        }

//        pDeviceContext->RSSetViewports( 1, &Viewport );
//        pDeviceContext->RSSetScissorRects(uNumSR, pScissor);

		gl.glViewportArrayv(0, CacheBuffer.wrap(Viewport));
        if(uNumSR > 0){
			gl.glEnable(GLenum.GL_SCISSOR_TEST);
	    	for(int i = 0; i < uNumSR; i++){
				gl.glScissorIndexed(i, pScissor[i].x, pScissor[i].y, pScissor[i].z, pScissor[i].w);
	    	}
	    }else{
			gl.glDisable(GLenum.GL_SCISSOR_TEST);
	    }
        
//        pDeviceContext->RSSetState( pOutputRS );
        if(pOutputRS != null)
        	pOutputRS.run();

//        pDeviceContext->PSSetConstantBuffers( 0, uNumCBs, ppCB);
        if(uNumCBs > 0){
        	for(int i = 0; i < uNumCBs; i++){
				gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, i, ppCB[i]);
        	}
        }
        
//        pDeviceContext->PSSetShaderResources( 0, uNumSRVs, ppSRVs );
        if(uNumSRVs > 0){
        	for(int i = 0; i < uNumSRVs; i++){
				gl.glBindTextureUnit(i, (ppSRVs != null && ppSRVs[i] !=null)? ppSRVs[i].getTexture():0);
        	}
        }
        
//        pDeviceContext->PSSetSamplers( 0, uNumSamplers, ppSamplers );
        if(uNumSamplers>0){
			gl.glBindSamplers(0, CacheBuffer.wrap(ppSamplers, 0, uNumSamplers));
        }

//        pDeviceContext->IASetInputLayout( NULL );
//        pDeviceContext->IASetVertexBuffers( 0, AMD_ARRAY_SIZE(pNullBuffer), pNullBuffer, NullStride, NullOffset );
//        pDeviceContext->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST );
//
//        pDeviceContext->VSSetShader( pVS, NULL, 0 );
//        pDeviceContext->GSSetShader( pGS, NULL, 0 );
//        pDeviceContext->PSSetShader(pPS, NULL, 0);
		gl.glBindVertexArray(0);
        g_pProgramPipeline.enable();
//        g_pProgramPipeline.setRenderShader(pVS, pPS, null, null, pGS);
		g_pProgramPipeline.setVS(pVS);
		g_pProgramPipeline.setGS(pGS);
		g_pProgramPipeline.setPS(pPS);

//        pDeviceContext->Draw( 3 * instanceCount, 0 );
		gl.glDrawArraysInstanced(GLenum.GL_TRIANGLES, 0, 3, instanceCount);

        // Unbind RTVs and SRVs back to NULL (otherwise D3D will throw warnings)
//        if (ppUAVs == NULL)
//        {
//            pDeviceContext->OMSetRenderTargets( AMD_ARRAY_SIZE(pNullRTV), pNullRTV, NULL );
//        }
//        else
//        {
//            pDeviceContext->OMSetRenderTargetsAndUnorderedAccessViews( uNumRTVs, pNullRTV, NULL, uStartUAV, uNumUAVs, pNullUAV, NULL);
//        }
//
//        pDeviceContext->PSSetShaderResources( 0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV );
        gl.glBindTextures(0, CacheBuffer.wrap(pNullSRV));
        gl.glBindImageTextures(0, CacheBuffer.wrap(pNullSRV));
        gl.glBindSamplers(0, CacheBuffer.wrap(pNullSRV));
    }
	
	private void updateBuffer(Readable[] data, int size, int buffer){
		gl.glBindVertexArray(0);
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, buffer);
		ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(size);
		for(Readable r : data){
			r.store(bytes);
		}
		bytes.flip();

		gl.glBufferSubData(GLenum.GL_ARRAY_BUFFER, 0, bytes);
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
	}
	
	private void updateBuffer(Readable data, int size, int buffer){
		gl.glBindVertexArray(0);
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, buffer);
		ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(size);
		data.store(bytes).flip();
		gl.glBufferSubData(GLenum.GL_ARRAY_BUFFER, 0, bytes);
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
	}

	private final S_MODEL_DATA modelData = new S_MODEL_DATA();
		void SetModelMatrices(//ID3D11DeviceContext*  pd3dContext,
		    int			                              pd3dCB,
		    final Matrix4f                            world,
		    final Matrix4f                            viewProj)
		{
//		    D3D11_MAPPED_SUBRESOURCE MappedResource;

//		    S_MODEL_DATA modelData;
		    modelData.m_World.load(world);
//		    modelData.m_WorldViewProjection = XMMatrixTranspose(world * viewProj);
		    Matrix4f.mul(viewProj, world, modelData.m_WorldViewProjection);
		    modelData.m_Ambient.set(0.1f, 0.1f, 0.1f, 1.0f);
		    modelData.m_Diffuse.set(1.0f, 1.0f, 1.0f, 1.0f);

//		    pd3dContext->Map(pd3dCB, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource);
//		    if (MappedResource.pData)
//		        memcpy(MappedResource.pData, &modelData, sizeof(modelData));
//		    pd3dContext->Unmap(pd3dCB, 0);
		    
		    updateBuffer(modelData, S_MODEL_DATA.SIZE, pd3dCB);
		}
	
	//--------------------------------------------------------------------------------------
	// This callback function will be called once at the beginning of every frame. This is the
	// best location for your application to handle updates to the scene, but is not
	// intended to contain actual rendering calls, which should instead be placed in the
	// OnFrameRender callback.
	//--------------------------------------------------------------------------------------
	private void onFrameMove(float fTime){
		if ((g_ShadowsExecution == ShadowFX_Constants.SHADOWFX_EXECUTION_CUBE ||
	         g_ShadowsExecution == ShadowFX_Constants.SHADOWFX_EXECUTION_UNION) 
	         && g_bShowLightCamera) // if using light camera - need to update constant buffer values
	        InitializeCubeCamera(g_CubeCamera);

	    if (g_ShadowsExecution == ShadowFX_Constants.SHADOWFX_EXECUTION_CASCADE)
	        InitializeCascadeCamera(g_CubeCamera, g_LightOrtho);
	}
	
	void CreateShaders() throws IOException{
		final String shader_path = "amdfx/ShadowFX_Sample/shaders/";
		g_pShadowMapVS = g_pSceneVS = GLSLProgram.createShaderProgramFromFile(shader_path + "RenderSceneVS.vert", ShaderType.VERTEX);

		g_pShadowMapPS = GLSLProgram.createShaderProgramFromFile(shader_path + "RenderShadowMapPS.frag", ShaderType.FRAGMENT);

		g_pShadowedScenePS = GLSLProgram.createShaderProgramFromFile(shader_path + "RenderShadowedScenePS.frag", ShaderType.FRAGMENT);

		g_pDepthPassScenePS = GLSLProgram.createShaderProgramFromFile(shader_path + "DepthPassScenePS.frag", ShaderType.FRAGMENT);

		g_pDepthAndNormalPassScenePS = GLSLProgram.createShaderProgramFromFile(shader_path + "DepthAndNormalPassScenePS.frag", ShaderType.FRAGMENT);
		
		g_pUnitCubeVS = GLSLProgram.createShaderProgramFromFile(shader_path + "AMD_UnitCube.vert", ShaderType.VERTEX);

		g_pUnitCubePS = GLSLProgram.createShaderProgramFromFile(shader_path + "AMD_UnitCube.frag", ShaderType.FRAGMENT);

		g_pFullscreenVS = GLSLProgram.createShaderProgramFromFile("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", ShaderType.VERTEX);

		g_pFullscreenPS = GLSLProgram.createShaderProgramFromFile("shader_libs/PostProcessingDefaultScreenSpacePS.frag", ShaderType.VERTEX);
	}
}
