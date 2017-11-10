package jet.opengl.demos.nvidia.shadows;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvCameraXformType;
import com.nvidia.developer.opengl.app.NvInputTransformer;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;
import com.nvidia.developer.opengl.utils.BoundingBox;
import com.nvidia.developer.opengl.utils.ShadowmapGenerateProgram;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/11/10.
 */
public final class MipmapSoftShadowDemo extends NvSampleApp {
    static final int DEPTH_RES = 1024;
    static final double M_LN2  =  0.693147180559945309417;

    private final BoundingBox m_MeshSceneBounds = new BoundingBox();
    private final BoundingBox m_Temp = new BoundingBox();
    private final UniformData m_uniformData = new UniformData();
    private final Matrix4f mClip2Tex = new Matrix4f();
    private final Matrix4f mProj = new Matrix4f();
    private final Matrix4f mLightProj = new Matrix4f();
    private final Matrix4f mLightViewProj = new Matrix4f();
    private BufferGL m_uniformBuffer;
    private GLFuncProvider gl;
    private RenderTargets m_renderTarget;
    private SDKmesh g_MeshScene;
    private SDKmesh g_MeshLight;
    private GLSLProgram m_RenderSceneAcc;
    private GLSLProgram m_RenderSceneFast;
    private GLSLProgram m_RenderSceneNoShadows;
    private float g_fSceneBoxDiag;
    private Runnable g_pRenderState;
    private SSMap m_smap;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_renderTarget = new RenderTargets();
        m_uniformBuffer = new BufferGL();
        m_uniformBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, UniformData.SIZE, null, GLenum.GL_STREAM_DRAW);
        m_uniformBuffer.unbind();

        g_MeshLight = loadModel("arrow.x");
        g_MeshScene = loadModel("bonfire_small.x");

        m_RenderSceneAcc = createRenderProgram("RenderSceneAcc");
        m_RenderSceneFast = createRenderProgram("RenderSceneFast");
        m_RenderSceneNoShadows = createRenderProgram("RenderSceneNoShadows");

        m_transformer.setMotionMode(NvCameraMotionType.DUAL_ORBITAL);

        /*D3DXVECTOR3 vDiag = g_MeshScene.m_vBox[0] - g_MeshScene.m_vBox[1];
        if (bFirstTime || bNeedUI)
        {
            D3DXVECTOR3 vEye = bFirstTime ? D3DXVECTOR3(-57.089104, 33.029869, -58.556995) : (g_MeshScene.m_vBox[0] + vDiag);
            D3DXVECTOR3 vTmp2 = ((g_MeshScene.m_vBox[0] + g_MeshScene.m_vBox[1]) / 2);
            if (bFirstTime)
            {
                g_LCamera.SetModelRot(D3DXMATRIX(0.18668720, 0.60933846, -0.77061951, 0.00000000,
                        -0.44507933, -0.64683354, -0.61928248, 0.00000000,
                        -0.87581515, 0.45859894, 0.15044841, 0.00000000,
                        0.00000000, 0.00000000, 1.0000000, 1.0000008));
            }
            bFirstTime = false;
            g_Camera.SetViewParams(&vEye, &vTmp2);
        }
        g_Camera.SetModelCenter((g_MeshScene.m_vBox[0] + g_MeshScene.m_vBox[1]) / 2);
        g_LCamera.SetModelCenter((g_MeshScene.m_vBox[0] + g_MeshScene.m_vBox[1]) / 2);
        g_fSceneBoxDiag = sqrt(D3DXVec3Dot(&vDiag, &vDiag));*/
        BoundingBox meshSceneBounds = m_MeshSceneBounds;
        BoundingBox tmp = m_Temp;
        for(int i = 0; i < g_MeshScene.getNumMeshes(); i++){
            tmp.setFromExtent(g_MeshScene.getMeshBBoxCenter(i), g_MeshScene.getMeshBBoxExtents(i));
            meshSceneBounds.expandBy(tmp);
        }

        Vector3f vDiag = Vector3f.sub(meshSceneBounds._max, meshSceneBounds._min, null);
        Vector3f vEye = new Vector3f(-57.089104f, 33.029869f, -58.556995f);
        Vector3f vTmp2 = meshSceneBounds.center(null);
        initCamera(NvCameraXformType.MAIN, vEye, vTmp2);

        g_fSceneBoxDiag = vDiag.lengthSquared();
        Vector3f lightPos = new Vector3f(3.57088f * 1.5f, 6.989f * 1.5f, 5.19698f * 1.5f); // position
        lightPos.normalise();
        lightPos.scale(g_fSceneBoxDiag * 4);  // TODO I am not sure whether the 4 of mulitplier is too larger or not.
        initCamera(NvCameraXformType.SECONDARY,
                lightPos,
                Vector3f.ZERO);                 // look at point);


        /*SAFE_RELEASE(g_pRenderState);
        D3D10_RASTERIZER_DESC RasterizerState;
        RasterizerState.FillMode = D3D10_FILL_SOLID;
        RasterizerState.CullMode = D3D10_CULL_FRONT;
        RasterizerState.FrontCounterClockwise = true;
        RasterizerState.DepthBias = false;
        RasterizerState.DepthBiasClamp = 0;
        RasterizerState.SlopeScaledDepthBias = 0;
        RasterizerState.DepthClipEnable = true;
        RasterizerState.ScissorEnable = false;
        RasterizerState.MultisampleEnable = false;
        RasterizerState.AntialiasedLineEnable = false;
        V(pDev10->CreateRasterizerState(&RasterizerState, &g_pRenderState));*/
        g_pRenderState = ()->
        {
            gl.glCullFace(GLenum.GL_FRONT);
            gl.glFrontFace(GLenum.GL_CW);
        };

        m_smap = new SSMap();
    }

    @Override
    public void display() {

        // 1, Render the shadowmap
        {
            // Compute the light transforms
            m_transformer.getModelViewMat(NvCameraXformType.SECONDARY, m_uniformData.mLightView);
            Matrix4f.decompseRigidMatrix(m_uniformData.mLightView, m_uniformData.g_vLightPos, null, null); // TODO The LightView must be a ortho matrix?
            BoundingBox.transform(m_uniformData.mLightView, m_MeshSceneBounds, m_Temp);
            float frustumWidth = Math.max(Math.abs(m_Temp._min.x), Math.abs(m_Temp._max.x)) * 2.0f;
            float frustumHeight = Math.max(Math.abs(m_Temp._min.y), Math.abs(m_Temp._max.y)) * 2.0f;
            float zNear = -m_Temp._max.z;
            float zFar = -m_Temp._min.z;
            Matrix4f.frustum(frustumWidth, frustumHeight, Math.max(0.001f, zNear), Math.max(0.002f, zFar), mLightProj);
            Matrix4f.mul(mLightProj, m_uniformData.mLightView, mLightViewProj);
            m_uniformData.mViewProj.load(mLightViewProj);

            // render shadow map
            m_smap.Render(g_MeshScene);
        }

        // 2, Render scene
        {
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
            gl.glClearDepthf(1.0f);
            gl.glClearColor(0,0,0.5f, 1);
            gl.glClear( GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

            m_transformer.getModelViewMat(NvCameraXformType.MAIN, m_uniformData.mViewProj);
            Matrix4f.mul(mProj, m_uniformData.mViewProj,m_uniformData.mViewProj);

            /*unsigned iTmp = g_SampleUI.GetCheckBox(IDC_BTEXTURED)->GetChecked();  TODO uniform
            V(g_pEffect->GetVariableByName("bTextured")->AsScalar()->SetRawValue(&iTmp, 0, sizeof(iTmp)));
            D3DXVECTOR4 vTmp = D3DXVECTOR4(1, 1, (float)iTmp, 1);
            V(g_pEffect->GetVariableByName("g_vLightFlux")->AsVector()->SetRawValue(&vTmp, 0, sizeof(D3DXVECTOR4)));
            V(g_pEffect->GetVariableByName("g_vMaterialKd")->AsVector()->SetRawValue(&vTmp, 0, sizeof(D3DXVECTOR4)));*/

            g_pRenderState.run();

            // do z-only pass and fill z-buffer  TODO ???
            /*pDev10->IASetInputLayout(ssmap.m_pDepthLayout);
            g_MeshScene.Render(pDev10, ssmap.m_pDRenderTechnique);*/
            updateAndBindBuffer();
            m_smap.m_pDRenderTechnique.enable();
            g_MeshScene.render(-1,-1,-1);

            // render with shader
            /*pDev10->IASetInputLayout(g_pMaxLayout);
            ID3D10EffectShaderResourceVariable *pTexture = g_pEffect->GetVariableByName("DiffuseTex")->AsShaderResource();
            if (ssmap.bAccurateShadow)
            {
                g_MeshScene.Render(pDev10, g_pEffect->GetTechniqueByName("RenderAcc"), pTexture);
            }
            else
            {
                g_MeshScene.Render(pDev10, g_pEffect->GetTechniqueByName("RenderFast"), pTexture);
            }*/
            if(m_smap.bAccurateShadow){
                m_RenderSceneAcc.enable();
            }else{
                m_RenderSceneFast.enable();
            }

            g_MeshScene.render(2,-1,-1);
        }


        // render light
        {
            /*D3DXMATRIX mLightViewInv;
            D3DXMatrixInverse(&mLightViewInv, NULL, &mLightView);
            D3DXMATRIX mLightViewInvWorldViewProj;
            D3DXMatrixMultiply(&mLightViewInvWorldViewProj, &mLightViewInv, &mWorldViewProj);
            V(g_pEffect->GetVariableByName("mViewProj")->AsMatrix()->SetMatrix((float *)&mLightViewInvWorldViewProj));
            g_pEffect->GetTechniqueByName("RenderNoShadows")->GetPassByIndex(0)->Apply(0);
            pDev10->RSSetState(g_pRenderState);
            pDev10->IASetInputLayout(g_pMaxLayout);
            g_MeshLight.Render(pDev10);*/
            m_uniformData.mLightView.invert();
            Matrix4f.mul(m_uniformData.mViewProj, m_uniformData.mLightView, m_uniformData.mViewProj);
            updateAndBindBuffer();

            m_RenderSceneNoShadows.enable();
            g_pRenderState.run();
            g_MeshLight.render(-1,-1,-1);
        }
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0 || height <=0)
            return;

        Matrix4f.perspective(60, (float)width/height, 0.1f, 200.0f, mProj);
    }

    private void initCamera(int index, Vector3f eye, ReadableVector3f at) {
        // Construct the look matrix
//	    	    Matrix4f look;
//	    	    lookAt(look, eye, at, nv.vec3f(0.0f, 1.0f, 0.0f));
        Matrix4f look = Matrix4f.lookAt(eye, at, Vector3f.Y_AXIS, null);

        // Decompose the look matrix to get the yaw and pitch.
        float pitch = (float) Math.atan2(-look.m21, /*_32*/ look.m22/*_33*/);
        float yaw = (float) Math.atan2(look.m20/*_31*/, Vector2f.length(-look.m21/*_32*/, look.m22/*_33*/));

        // Initialize the camera view.
        NvInputTransformer m_camera = getInputTransformer();
        m_camera.setRotationVec(new Vector3f(pitch, yaw, 0.0f), index);
        m_camera.setTranslationVec(new Vector3f(look.m30/*_41*/, look.m31/*_42*/, look.m32/*_43*/), index);
        m_camera.update(0.0f);
    }

    private SDKmesh loadModel(String modelFileName){
        try {
            SDKmesh mesh = new SDKmesh();
            mesh.create("nvidia/ShadowWorks/models/" + modelFileName, false, null);
            mesh.printMeshInformation("");
            return mesh;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static GLSLProgram createRenderProgram(String name){
        String vert_patten = "nvidia/ShadowWorks/Mipmap%sVS.vert";
        String frag_patten = "nvidia/ShadowWorks/Mipmap%sPS.frag";

        try {
            GLSLProgram program = GLSLProgram.createFromFiles(String.format(vert_patten, name), String.format(frag_patten, name));
            program.setName(name);
            return program;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static GLSLProgram createScreenQuadProgram(String name){
        String vert_file = "shader_libs/PostProcessingDefaultScreenSpaceVS.vert";
        String frag_patten = "nvidia/ShadowWorks/Mipmap%sPS.frag";

        try {
            GLSLProgram program = GLSLProgram.createFromFiles(vert_file, String.format(frag_patten, name));
            program.setName(name);
            return program;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void updateAndBindBuffer(){

    }

    private final class SSMap{
        private Texture2D m_pDepthTex, m_pDepthMip2, m_pBigDepth2; ///< textures for rendering
        private Texture2D m_pDepthDSView; ///< depth stencil view
        private Texture2D[] m_pDepthMip2RTViews;
        private Texture2D m_pBigDepth2RTView;
//        ID3D10StateBlock *m_pOldRenderState; ///< we save rendering state here
        private Runnable m_pRasterState; ///< render state we use to render shadow map
        private Runnable m_pDSState; ///< render state we use to render shadow map
        private Texture2D m_pDepthSRView, m_pDepthMip2SRView, m_pBigDepth2SRView;
        private Texture2D[] m_pDepthMip2SRViews;
        int nMips; ///< number of depth mips (depends on the depth map resolution)

//        ID3D10InputLayout *m_pDepthLayout; ///< layout with only POSITION semantic in it
        ShadowmapGenerateProgram m_pDRenderTechnique;
        private GLSLProgram m_pConvertDepth2;
        private GLSLProgram m_pConvertToBig;
        private GLSLProgram m_pCreateMip2;

        SSMap(){
            m_fFilterSize = 0.1f;
            bAccurateShadow = false;
            nMips = (int)(Math.log(DEPTH_RES) / M_LN2);
        }

        void OnDestroy(){
            CommonUtil.safeRelease(m_pDepthTex);
            CommonUtil.safeRelease(m_pDepthMip2);
            CommonUtil.safeRelease(m_pBigDepth2);
            CommonUtil.safeRelease(m_pDepthDSView);
            if(m_pDepthMip2RTViews != null){
                for(Texture2D tex : m_pDepthMip2RTViews)
                    CommonUtil.safeRelease(tex);
            }

            CommonUtil.safeRelease(m_pBigDepth2RTView);
            CommonUtil.safeRelease(m_pDepthSRView);
            CommonUtil.safeRelease(m_pDepthMip2SRView);
            CommonUtil.safeRelease(m_pBigDepth2SRView);

            if(m_pDepthMip2SRViews != null){
                for(Texture2D tex : m_pDepthMip2SRViews)
                    CommonUtil.safeRelease(tex);
            }
        }
        void OnWindowResize(){}

        float m_fFilterSize;
        boolean bAccurateShadow;

        void Render(SDKmesh mesh){
//            m_pDRenderTechnique = pEffect->GetTechniqueByName("RenderDepth");
//            ID3D10EffectTechnique *pDReworkTechnique2 = pEffect->GetTechniqueByName("ReworkDepth2");
            if (m_pDepthTex == null)
            {
                gl = GLFuncProviderFactory.getGLFuncProvider();
                m_pDRenderTechnique = new ShadowmapGenerateProgram();
                m_pConvertDepth2 = createScreenQuadProgram("ConvertDepth2");
                m_pConvertToBig = createScreenQuadProgram("ConvertToBig");
                m_pCreateMip2 = createScreenQuadProgram("CreateMip2");
                m_pDepthMip2SRViews = new Texture2D[nMips];
                m_pDepthMip2RTViews = m_pDepthMip2SRViews;
                // create render targets
                /*D3D10_TEXTURE2D_DESC rtDesc =
                        {
                                DEPTH_RES, //UINT Width;
                                DEPTH_RES, //UINT Height;
                                1,//UINT MipLevels;
                                1,//UINT ArraySize;
                                DXGI_FORMAT_R32_TYPELESS,//DXGI_FORMAT Format;
                                {1, 0}, //DXGI_SAMPLE_DESC SampleDesc;
                                D3D10_USAGE_DEFAULT, //D3D10_USAGE Usage;
                                D3D10_BIND_SHADER_RESOURCE | D3D10_BIND_DEPTH_STENCIL,//UINT BindFlags;
                                0,//UINT CPUAccessFlags;
                                0,//UINT MiscFlags;
                        };
                V(pDev10->CreateTexture2D(&rtDesc, NULL, &m_pDepthTex[0]));*/
                Texture2DDesc rtDesc = new Texture2DDesc(DEPTH_RES,DEPTH_RES, GLenum.GL_DEPTH_COMPONENT32F);
                m_pDepthTex = TextureUtils.createTexture2D(rtDesc, null);

                rtDesc.mipLevels = nMips;
//                rtDesc.BindFlags = D3D10_BIND_SHADER_RESOURCE | D3D10_BIND_RENDER_TARGET;
                rtDesc.format = /*DXGI_FORMAT_R32G32_TYPELESS*/ GLenum.GL_RG32F;
                /*V(pDev10->CreateTexture2D(&rtDesc, NULL, &m_pDepthMip2));*/
                m_pDepthMip2 = TextureUtils.createTexture2D(rtDesc, null);

                rtDesc.width = (rtDesc.width * 3) / 2;
                rtDesc.mipLevels = 1;
                /*V(pDev10->CreateTexture2D(&rtDesc, NULL, &m_pBigDepth2));*/
                m_pBigDepth2 = TextureUtils.createTexture2D(rtDesc, null);

                /*D3D10_DEPTH_STENCIL_VIEW_DESC dsViewDesc;
                D3D10_SHADER_RESOURCE_VIEW_DESC srViewDesc;
                dsViewDesc.Format = DXGI_FORMAT_D32_FLOAT;
                srViewDesc.Format = DXGI_FORMAT_R32_FLOAT;
                dsViewDesc.ViewDimension = D3D10_DSV_DIMENSION_TEXTURE2D;
                srViewDesc.ViewDimension = D3D10_SRV_DIMENSION_TEXTURE2D;
                dsViewDesc.Texture2D.MipSlice = 0;
                srViewDesc.Texture2D.MostDetailedMip = 0;
                srViewDesc.Texture2D.MipLevels = 1;
                V(pDev10->CreateDepthStencilView(m_pDepthTex, &dsViewDesc, &m_pDepthDSView));
                V(pDev10->CreateShaderResourceView(m_pDepthTex, &srViewDesc, &m_pDepthSRView));
                srViewDesc.Texture2D.MipLevels = nMips;
                srViewDesc.Format = DXGI_FORMAT_R32G32_FLOAT;
                V(pDev10->CreateShaderResourceView(m_pDepthMip2, &srViewDesc, &m_pDepthMip2SRView));*/
                m_pDepthDSView = m_pDepthSRView = m_pDepthTex;
                m_pDepthMip2SRView = m_pDepthMip2;

                /*srViewDesc.Texture2D.MipLevels = 1;
                D3D10_RENDER_TARGET_VIEW_DESC rtViewDesc;
                rtViewDesc.ViewDimension = D3D10_RTV_DIMENSION_TEXTURE2D;*/
                for (int im = 0; im < nMips; ++im)
                {
                    /*srViewDesc.Texture2D.MostDetailedMip = im;
                    srViewDesc.Format = DXGI_FORMAT_R32G32_FLOAT;
                    V(pDev10->CreateShaderResourceView(m_pDepthMip2, &srViewDesc, &m_pDepthMip2SRViews[im]));
                    rtViewDesc.Texture2D.MipSlice = im;
                    rtViewDesc.Format = DXGI_FORMAT_R32G32_FLOAT;
                    V(pDev10->CreateRenderTargetView(m_pDepthMip2, &rtViewDesc, &m_pDepthMip2RTViews[im]));*/

                    m_pDepthMip2SRViews[im] = m_pDepthMip2RTViews[im] =
                            TextureUtils.createTextureView(m_pDepthMip2, GLenum.GL_TEXTURE_2D, im, 1, 0, 1);
                }
                /*rtViewDesc.Texture2D.MipSlice = 0;
                V(pDev10->CreateRenderTargetView(m_pBigDepth2, &rtViewDesc, &m_pBigDepth2RTView));
                srViewDesc.Texture2D.MostDetailedMip = 0;
                V(pDev10->CreateShaderResourceView(m_pBigDepth2, &srViewDesc, &m_pBigDepth2SRView));*/
                m_pBigDepth2RTView = m_pBigDepth2SRView = m_pBigDepth2;

                /*static const D3D10_INPUT_ELEMENT_DESC depth_layout[] =
                    {
                            { "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D10_INPUT_PER_VERTEX_DATA, 0 },
                    };
                D3D10_PASS_DESC PassDesc;
                V(m_pDRenderTechnique->GetPassByIndex(0)->GetDesc(&PassDesc));
                V(pDev10->CreateInputLayout(depth_layout, 1, PassDesc.pIAInputSignature, PassDesc.IAInputSignatureSize, &m_pDepthLayout));*/

                /*SAFE_RELEASE(m_pRasterState);
                D3D10_RASTERIZER_DESC RasterState;
                RasterState.FillMode = D3D10_FILL_SOLID;
                RasterState.CullMode = D3D10_CULL_BACK;
                RasterState.FrontCounterClockwise = true;
                RasterState.DepthBias = false;
                RasterState.DepthBiasClamp = 0;
                RasterState.SlopeScaledDepthBias = 0;
                RasterState.DepthClipEnable = true;
                RasterState.ScissorEnable = false;
                RasterState.MultisampleEnable = false;
                RasterState.AntialiasedLineEnable = false;
                V(pDev10->CreateRasterizerState(&RasterState, &m_pRasterState));*/
                m_pRasterState = ()->
                {
                    gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
                    gl.glCullFace(GLenum.GL_BACK);
                    gl.glFrontFace(GLenum.GL_CW);
                    gl.glDisable(GLenum.GL_POLYGON_OFFSET_FILL);
                };

                /*SAFE_RELEASE(m_pDSState);
                D3D10_DEPTH_STENCIL_DESC DSState;
                ZeroMemory(&DSState, sizeof(DSState));
                DSState.DepthEnable = true;
                DSState.DepthWriteMask = D3D10_DEPTH_WRITE_MASK_ALL;
                DSState.DepthFunc = D3D10_COMPARISON_LESS_EQUAL;
                V(pDev10->CreateDepthStencilState(&DSState, &m_pDSState));*/
                m_pDSState = ()->
                {
                    gl.glEnable(GLenum.GL_DEPTH_TEST);
                    gl.glDepthMask(true);
                    gl.glDepthFunc(GLenum.GL_LEQUAL);
                };
            }
            /*if (m_pOldRenderState == NULL)
            {
                D3D10_STATE_BLOCK_MASK SBMask;
                ZeroMemory(&SBMask, sizeof(SBMask));
                SBMask.RSViewports = true;
                SBMask.OMRenderTargets = true;
                SBMask.RSRasterizerState = true;
                V(D3D10CreateStateBlock(pDev10, &SBMask, &m_pOldRenderState));
            }
            V(m_pOldRenderState->Capture());*/

            float fTmp = bAccurateShadow ? (float)(10 + 300 * m_fFilterSize) : (float)(0.5 + 30 * m_fFilterSize);
            /*V(pEffect->GetVariableByName("g_fFilterSize")->AsScalar()->SetFloat(fTmp));
            V(pEffect->GetVariableByName("g_fDoubleFilterSizeRev")->AsScalar()->SetFloat((FLOAT)(1.0 / (2 * fTmp))));*/
            m_uniformData.g_fFilterSize = fTmp;
            m_uniformData.g_fDoubleFilterSizeRev = (1.f / (2.f * fTmp));
            updateAndBindBuffer();

            /*D3D10_VIEWPORT vp;
            vp.Height = DEPTH_RES;
            vp.Width = DEPTH_RES;
            vp.MinDepth = 0;
            vp.MaxDepth = 1;
            vp.TopLeftX = 0;
            vp.TopLeftY = 0;
            pDev10->RSSetViewports(1, &vp);*/
            gl.glViewport(0,0, DEPTH_RES, DEPTH_RES);

            // render depth
            /*pDev10->RSSetState(m_pRasterState);
            pDev10->OMSetDepthStencilState(m_pDSState, 0);*/
            m_pRasterState.run();
            m_pDSState.run();

            /*ID3D10RenderTargetView *pNullRTView = NULL;
            pDev10->OMSetRenderTargets(1, &pNullRTView, m_pDepthDSView[0]);
            pDev10->IASetInputLayout(m_pDepthLayout);
            pDev10->ClearDepthStencilView(m_pDepthDSView[0], D3D10_CLEAR_DEPTH, 1.0, 0);
            pMesh->Render(pDev10, m_pDRenderTechnique);*/
            m_renderTarget.bind();
            m_renderTarget.setRenderTexture(m_pDepthDSView, null);
            gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1.0f));
            m_pDRenderTechnique.enable();
            mesh.render(-1, -1, -1);

            // needed because we will use LightViewProj to compute texture coordinates, but not clip-space coordinates
            if (bAccurateShadow)
            {
                mClip2Tex.set( 0.5f,    0, 0,   0,
                        0, 0.5f, 0,   0,
                        0,    0, 0.5f,   0,
                        0.5f,  0.5f, 0.5f,   1 );
            }
            else
            {
                mClip2Tex.set(0.5f * DEPTH_RES, 0, 0, 0,
                        0, -0.5f * DEPTH_RES, 0, 0,
                        0, 0, 1, 0,  // TODO Notice the z value
                        DEPTH_RES * 0.5f, DEPTH_RES * 0.5f, 0, 1 );
            }
            /*D3DXMATRIX mLightViewProjClip2Tex, mLightProjClip2TexInv, mTmp;
            D3DXMatrixMultiply(&mLightViewProjClip2Tex, &mLightViewProj, &mClip2Tex);
            V(pEffect->GetVariableByName("mLightViewProjClip2Tex")->AsMatrix()->SetMatrix((float *)&mLightViewProjClip2Tex));*/
            Matrix4f.mul(mClip2Tex, mLightViewProj, m_uniformData.mLightViewProjClip2Tex);

            /*D3DXMatrixMultiply(&mTmp, &mLightProj, &mClip2Tex);
            D3DXMatrixInverse(&mLightProjClip2TexInv, NULL, &mTmp);
            V(pEffect->GetVariableByName("mLightProjClip2TexInv")->AsMatrix()->SetMatrix((float *)&mLightProjClip2TexInv));*/
            Matrix4f.invert(m_uniformData.mLightViewProjClip2Tex, m_uniformData.mLightProjClip2TexInv);

            // create mipmap pyramid
            /*V(pEffect->GetVariableByName("DepthTex0")->AsShaderResource()->SetResource(m_pDepthSRView[0]));
            pDev10->OMSetRenderTargets(1, &m_pDepthMip2RTViews[0], NULL);
            ID3D10Buffer *pNullVBuf[] = { NULL };
            unsigned pStrides[] = { 0 };
            unsigned pOffsets[] = { 0 };
            pDev10->IASetVertexBuffers(0, 1, pNullVBuf, pStrides, pOffsets);
            pDev10->IASetInputLayout(NULL);*/
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(m_pDepthSRView.getTarget(), m_pDepthSRView.getTexture());
            m_renderTarget.bind();
            m_renderTarget.setRenderTexture(m_pDepthMip2RTViews[0], null);
            /*V(pDReworkTechnique2->GetPassByName("ConvertDepth")->Apply(0));*/
            m_pConvertDepth2.enable();
            updateAndBindBuffer();
            int vpWidth = DEPTH_RES;
            int vpHeight = DEPTH_RES;
            for (int im = 0; ; )
            {
//                pDev10->Draw(3, 0);
                gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
                if (++im == nMips)
                { break; }
                vpWidth = (vpHeight /= 2);
                /*pDev10->RSSetViewports(1, &vp);
                V(pEffect->GetVariableByName("DepthMip2")->AsShaderResource()->SetResource(m_pDepthMip2SRViews[im - 1]));
                pDev10->OMSetRenderTargets(1, &m_pDepthMip2RTViews[im], NULL);
                V(pDReworkTechnique2->GetPassByName("CreateMip")->Apply(0));*/
                gl.glViewport(0,0, vpWidth, vpHeight);
                gl.glActiveTexture(GLenum.GL_TEXTURE1);
                gl.glBindTexture(m_pDepthMip2SRViews[im - 1].getTarget(), m_pDepthMip2SRViews[im - 1].getTexture());
                m_renderTarget.setRenderTexture(m_pDepthMip2RTViews[im], null);
                m_pCreateMip2.enable();
            }

            /*pDev10->OMSetRenderTargets(1, &pNullRTView, NULL);
            V(pEffect->GetVariableByName("DepthMip2")->AsShaderResource()->SetResource(m_pDepthMip2SRView));
            V(pDReworkTechnique2->GetPassByName("ConvertToBig")->Apply(0));
            vp.Height = DEPTH_RES;
            vp.Width = (DEPTH_RES * 3) / 2;
            vp.MinDepth = 0;
            vp.MaxDepth = 1;
            vp.TopLeftX = 0;
            vp.TopLeftY = 0;
            pDev10->RSSetViewports(1, &vp);
            pDev10->OMSetRenderTargets(1, &m_pBigDepth2RTView, NULL);*/
            gl.glActiveTexture(GLenum.GL_TEXTURE1);
            gl.glBindTexture(m_pDepthMip2SRView.getTarget(), m_pDepthMip2SRView.getTexture());
            m_pConvertToBig.enable();
            gl.glViewport(0,0,DEPTH_RES, (DEPTH_RES * 3) / 2);
            m_renderTarget.setRenderTexture(m_pBigDepth2RTView, null);

            /*pDev10->Draw(3, 0);
            pDev10->OMSetRenderTargets(1, &pNullRTView, NULL);*/
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0,3 );

            /*static bool bSaved = true;
            if (!bSaved)
            {
                bSaved = true;
                ID3D10Texture2D *pTexture = NULL;
                D3D10_TEXTURE2D_DESC textureDesc;
                m_pBigDepth2->GetDesc(&textureDesc);
                textureDesc.Format = DXGI_FORMAT_R32G32_FLOAT;
                textureDesc.CPUAccessFlags = D3D10_CPU_ACCESS_READ;
                textureDesc.Usage = D3D10_USAGE_STAGING;
                textureDesc.BindFlags = 0;
                V(pDev10->CreateTexture2D(&textureDesc, NULL, &pTexture));
                pDev10->CopyResource(pTexture, m_pBigDepth2);
                D3DX10SaveTextureToFile(pTexture, D3DX10_IFF_DDS, L"c:\\fff.dds");
            }
            V(pEffect->GetVariableByName("DepthMip2")->AsShaderResource()->SetResource(m_pBigDepth2SRView));
            V(m_pOldRenderState->Apply());*/
        }
    }

    private static final class UniformData implements Readable{
        static final int SIZE = Vector4f.SIZE * 5 + Matrix4f.SIZE * 4;
        final Vector4f g_vMaterialKd = new Vector4f();
        final Vector3f g_vLightPos = new Vector3f(); ///< light in world CS
        final Vector4f g_vLightFlux = new Vector4f();
        float g_fFilterSize;
        float g_fDoubleFilterSizeRev;
        final Matrix4f mViewProj = new Matrix4f();
        final Matrix4f mLightView = new Matrix4f();
        final Matrix4f mLightViewProjClip2Tex = new Matrix4f();
        final Matrix4f mLightProjClip2TexInv = new Matrix4f();
        boolean bTextured;

        @Override
        public ByteBuffer store(ByteBuffer buf) {
            g_vMaterialKd.store(buf);
            g_vLightPos.store(buf); buf.putInt(0);
            g_vLightFlux.store(buf);
            buf.putFloat(g_fFilterSize);
            buf.putFloat(g_fDoubleFilterSizeRev);
            buf.putLong(0);
            mViewProj.store(buf);
            mLightView.store(buf);
            mLightViewProjClip2Tex.store(buf);
            mLightProjClip2TexInv.store(buf);
            buf.putInt(bTextured ? 1 : 0);
            buf.putLong(0);
            buf.putInt( 0);
            return buf;
        }
    }
}
