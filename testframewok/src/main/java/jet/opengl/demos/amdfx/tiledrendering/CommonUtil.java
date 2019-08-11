package jet.opengl.demos.amdfx.tiledrendering;

import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.ModelGenerator;
import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLProgramPipeline;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.NvImage;

final class CommonUtil implements ICONST, Disposeable {
    /** DebugDrawType */
    static final  int
            DEBUG_DRAW_NONE = 0,
            DEBUG_DRAW_RADAR_COLORS = 1,
            DEBUG_DRAW_GRAYSCALE = 2;

    /** TriangleDensityType */
    static final int
            TRIANGLE_DENSITY_LOW = 0,
            TRIANGLE_DENSITY_MEDIUM = 1,
            TRIANGLE_DENSITY_HIGH = 2,
            TRIANGLE_DENSITY_NUM_TYPES = 3;


    /** DepthStencilStateType */
    static final int
        DEPTH_STENCIL_STATE_DISABLE_DEPTH_WRITE = 0,
        DEPTH_STENCIL_STATE_DISABLE_DEPTH_TEST = 1,
        DEPTH_STENCIL_STATE_DEPTH_GREATER = 2,
        DEPTH_STENCIL_STATE_DEPTH_GREATER_AND_DISABLE_DEPTH_WRITE = 3,
        DEPTH_STENCIL_STATE_DEPTH_EQUAL_AND_DISABLE_DEPTH_WRITE = 4,
        DEPTH_STENCIL_STATE_DEPTH_LESS = 5,
        DEPTH_STENCIL_STATE_NUM_TYPES = 6
    ;

    /** RasterizerStateType */
    static final int
        RASTERIZER_STATE_DISABLE_CULLING = 0,
        RASTERIZER_STATE_WIREFRAME = 1,
        RASTERIZER_STATE_WIREFRAME_DISABLE_CULLING = 2,
        RASTERIZER_STATE_NUM_TYPES = 3;

    /** SamplerStateType */
    static final int
        SAMPLER_STATE_POINT = 0,
        SAMPLER_STATE_LINEAR= 1,
        SAMPLER_STATE_ANISO = 2,
        SAMPLER_STATE_SHADOW = 3,
        SAMPLER_STATE_NUM_TYPES = 4;

    // Light culling constants.
    // These must match their counterparts in CommonHeader.h
    private static final int TILE_RES = 16;
    private static final int MAX_NUM_LIGHTS_PER_TILE = 272;
    private static final int MAX_NUM_VPLS_PER_TILE = 1024;

    private static final class CommonUtilGridVertex implements Readable {
        static final int SIZE = Vector3f.SIZE * 3 + Vector2f.SIZE;

        final Vector3f v3Pos = new Vector3f();
        final Vector3f v3Norm = new Vector3f();
        final Vector2f v2TexCoord = new Vector2f();
        final Vector3f v3Tangent = new Vector3f();

        @Override
        public ByteBuffer store(ByteBuffer buf) {
            v3Pos.store(buf);
            v3Norm.store(buf);
            v2TexCoord.store(buf);
            v3Tangent.store(buf);
            return buf;
        }
    };

    // Grid data (for throwing a lot of tris at the GPU)
    // 30x30 cells, times 2 tris per cell, times 2 for front and back,
    // that's 3600 tris per grid (half front facing and half back facing),
    // times 280 grid objects equals 1,008,000 triangles (half front facing and half back facing)
    private static final int g_nNumGridCells1DHigh = 30;
    private static final int g_nNumGridVerticesHigh = 2 * (g_nNumGridCells1DHigh + 1) * (g_nNumGridCells1DHigh + 1);
    private static final int g_nNumGridIndicesHigh = 2 * 6 * g_nNumGridCells1DHigh * g_nNumGridCells1DHigh;
    static CommonUtilGridVertex[][] g_GridVertexDataHigh = new CommonUtilGridVertex[MAX_NUM_GRID_OBJECTS][g_nNumGridVerticesHigh];
    private static final short[] g_GridIndexDataHigh = new short[g_nNumGridIndicesHigh];

    // Grid data (for throwing a lot of tris at the GPU)
    // 21x21 cells, times 2 tris per cell, times 2 for front and back,
    // that's 1764 tris per grid (half front facing and half back facing),
    // times 280 grid objects equals 493,920 triangles (half front facing and half back facing)
    private static final int g_nNumGridCells1DMed = 21;
    private static final int g_nNumGridVerticesMed = 2 * (g_nNumGridCells1DMed + 1) * (g_nNumGridCells1DMed + 1);
    private static final int g_nNumGridIndicesMed = 2 * 6 * g_nNumGridCells1DMed * g_nNumGridCells1DMed;
    private static CommonUtilGridVertex[][] g_GridVertexDataMed = new CommonUtilGridVertex[MAX_NUM_GRID_OBJECTS][g_nNumGridVerticesMed];
    private static final short[] g_GridIndexDataMed = new short[g_nNumGridIndicesMed];

    // Grid data (for throwing a lot of tris at the GPU)
    // 11x11 cells, times 2 tris per cell, times 2 for front and back,
    // that's 484 tris per grid (half front facing and half back facing),
    // times 280 grid objects equals 135,520 triangles (half front facing and half back facing)
    private static final int g_nNumGridCells1DLow = 11;
    private static final int g_nNumGridVerticesLow = 2 * (g_nNumGridCells1DLow + 1) * (g_nNumGridCells1DLow + 1);
    private static final int g_nNumGridIndicesLow = 2 * 6 * g_nNumGridCells1DLow * g_nNumGridCells1DLow;
    private static CommonUtilGridVertex[][] g_GridVertexDataLow = new CommonUtilGridVertex[MAX_NUM_GRID_OBJECTS][g_nNumGridVerticesLow];
    private static final short[]       g_GridIndexDataLow = new short[g_nNumGridIndicesLow];

    private static final int[] g_nNumGridIndices = { g_nNumGridIndicesLow, g_nNumGridIndicesMed, g_nNumGridIndicesHigh };

    private static final class CommonUtilSpriteVertex implements Readable {
        static final int SIZE = Vector3f.SIZE + Vector2f.SIZE;

        final Vector3f v3Pos = new Vector3f();
        final Vector2f v2TexCoord = new Vector2f();

        @Override
        public ByteBuffer store(ByteBuffer buf) {
            v3Pos.store(buf);
            v2TexCoord.store(buf);
            return buf;
        }
    };

    // static array for sprite quad vertex data
    private static CommonUtilSpriteVertex[]         g_QuadForLegendVertexData = new CommonUtilSpriteVertex[6];

// constants for the legend for the lights-per-tile visualization
    private static final int g_nLegendNumLines = 17;
    private static final int g_nLegendTextureWidth = 32;
    private static final int g_nLegendPaddingLeft = 5;
    private static final int g_nLegendPaddingBottom = 2*/*AMD::HUD::iElementDelta*/25;

    private static final class BlendedObjectIndex
    {
        int   nIndex;
        float fDistance;
    };

    private static final int          g_NumBlendedObjects = 2*20;
    private static final Matrix4f[]   g_BlendedObjectInstanceTransform = new Matrix4f[ g_NumBlendedObjects ];
    private static final BlendedObjectIndex[] g_BlendedObjectIndices = new BlendedObjectIndex[ g_NumBlendedObjects ];

    // there should only be one CommonUtil object
    static int CommonUtilObjectCounter = 0;

    private static int BlendedObjectsSortFunc( BlendedObjectIndex obj1, BlendedObjectIndex obj2 )
    {
        return Float.compare(obj1.fDistance, obj2.fDistance);
    }

    private static void InitBlendedObjectData()
    {
        final Matrix4f mWorld = new Matrix4f();
        for ( int i = 0; i < g_NumBlendedObjects; i++ )
        {
            int nRowLength = 10;
            int col = i % nRowLength;
            int row = (i % (g_NumBlendedObjects/2)) / nRowLength;
            float x = ( col * 200.0f ) - 800.0f;
            float y = i < g_NumBlendedObjects / 2 ? 80.0f : 220.0f;
            float z = ( row * 200.0f ) - 80.0f;

//            g_BlendedObjectInstanceTransform[ i ] = XMLoadFloat4x4( &mWorld );
            if(g_BlendedObjectInstanceTransform[ i ] == null){
                g_BlendedObjectInstanceTransform[ i ] = new Matrix4f();
            }

            g_BlendedObjectInstanceTransform[ i ].m30 = x;
            g_BlendedObjectInstanceTransform[ i ].m31 = y;
            g_BlendedObjectInstanceTransform[ i ].m32 = z;
        }
    }

    static void UpdateBlendedObjectData(ReadableVector3f vEyePt)
    {
        Vector3f vResult = new Vector3f();
        for ( int i = 0; i < g_NumBlendedObjects; i++ )
        {
            /*XMFLOAT4X4 mWorld;
            XMStoreFloat4x4( &mWorld, g_BlendedObjectInstanceTransform[ i ] );
            XMVECTOR vObj = XMVectorSet( mWorld._41, mWorld._42, mWorld._43, 1.0f );
            XMVECTOR vResult = vEyePt - vObj;*/

            vResult.x = g_BlendedObjectInstanceTransform[ i ].m30 - vEyePt.getX();
            vResult.y = g_BlendedObjectInstanceTransform[ i ].m31 - vEyePt.getY();
            vResult.z = g_BlendedObjectInstanceTransform[ i ].m32 - vEyePt.getZ();

            if(g_BlendedObjectIndices[ i ] == null)
                g_BlendedObjectIndices[ i ] = new BlendedObjectIndex();

            g_BlendedObjectIndices[ i ].nIndex = i;
            g_BlendedObjectIndices[ i ].fDistance = vResult.lengthSquared();  //XMVectorGetX(XMVector3LengthSq( vResult ));
        }

//        qsort( g_BlendedObjectIndices, g_NumBlendedObjects, sizeof( g_BlendedObjectIndices[ 0 ] ), &BlendedObjectsSortFunc );
        Arrays.sort(g_BlendedObjectIndices, CommonUtil::BlendedObjectsSortFunc);
    }

//    template <size_t nNumGridVertices, size_t nNumGridIndices>
    private static void InitGridObjectData(int nNumGridCells1D, CommonUtilGridVertex GridVertexData[/*MAX_NUM_GRID_OBJECTS*/][/*nNumGridVertices*/], short GridIndexData[/*nNumGridIndices*/])
    {
        final float fGridSizeWorldSpace = 100.0f;
        final float fGridSizeWorldSpaceHalf = 0.5f * fGridSizeWorldSpace;

        final float fPosStep = fGridSizeWorldSpace / (float)(nNumGridCells1D);
        final float fTexStep = 1.0f / (float)(nNumGridCells1D);

        final float fPosX = 725.0f;
        final float fPosYStart = 1000.0f;
        final float fStepY = 1.05f * fGridSizeWorldSpace;
        final float fPosZStart = 1467.0f;
        final float fStepZ = 1.05f * fGridSizeWorldSpace;

        for( int nGrid = 0; nGrid < MAX_NUM_GRID_OBJECTS; nGrid++ )
        {
            final float fCurrentPosYOffset = fPosYStart - (float)((nGrid/28)%10)*fStepY;
            final float fCurrentPosZOffset = fPosZStart - (float)(nGrid%28)*fStepZ;

            // front side verts
            for( int i = 0; i < nNumGridCells1D+1; i++ )
            {
                final float fPosY = fCurrentPosYOffset + fGridSizeWorldSpaceHalf - (float)i*fPosStep;
                final float fV = (float)i*fTexStep;
                for( int j = 0; j < nNumGridCells1D+1; j++ )
                {
                    final float fPosZ = fCurrentPosZOffset - fGridSizeWorldSpaceHalf + (float)j*fPosStep;
                    final float fU = (float)j*fTexStep;
                    final int idx = (nNumGridCells1D+1) * i + j;
                    if(GridVertexData[nGrid][idx] == null)
                        GridVertexData[nGrid][idx] = new CommonUtilGridVertex();

                    GridVertexData[nGrid][idx].v3Pos.set(fPosX, fPosY, fPosZ);
                    GridVertexData[nGrid][idx].v3Norm.set(1,0,0);
                    GridVertexData[nGrid][idx].v2TexCoord.set(fU,fV);
                    GridVertexData[nGrid][idx].v3Tangent.set(0,0,-1);
                }
            }

            // back side verts
            for( int i = 0; i < nNumGridCells1D+1; i++ )
            {
                final float fPosY = fCurrentPosYOffset + fGridSizeWorldSpaceHalf - (float)i*fPosStep;
                final float fV = (float)i*fTexStep;
                for( int j = 0; j < nNumGridCells1D+1; j++ )
                {
                    final float fPosZ = fCurrentPosZOffset + fGridSizeWorldSpaceHalf - (float)j*fPosStep;
                    final float fU = (float)j*fTexStep;
                    final int idx = (nNumGridCells1D+1) * (nNumGridCells1D+1) + (nNumGridCells1D+1) * i + j;
                    if(GridVertexData[nGrid][idx] == null)
                        GridVertexData[nGrid][idx] = new CommonUtilGridVertex();

                    GridVertexData[nGrid][idx].v3Pos.set(fPosX, fPosY, fPosZ);
                    GridVertexData[nGrid][idx].v3Norm.set(-1,0,0);
                    GridVertexData[nGrid][idx].v2TexCoord.set(fU,fV);
                    GridVertexData[nGrid][idx].v3Tangent.set(0,0,1);
                }
            }
        }

        // front side tris
        for( int i = 0; i < nNumGridCells1D; i++ )
        {
            for( int j = 0; j < nNumGridCells1D; j++ )
            {
                final int vertexStartIndexThisRow = (nNumGridCells1D+1) * i + j;
                final int vertexStartIndexNextRow = (nNumGridCells1D+1) * (i+1) + j;
                final int idx = (6 * nNumGridCells1D * i) + (6*j);
                GridIndexData[idx+0] = (short)(vertexStartIndexThisRow);
                GridIndexData[idx+1] = (short)(vertexStartIndexThisRow+1);
                GridIndexData[idx+2] = (short)(vertexStartIndexNextRow);
                GridIndexData[idx+3] = (short)(vertexStartIndexThisRow+1);
                GridIndexData[idx+4] = (short)(vertexStartIndexNextRow+1);
                GridIndexData[idx+5] = (short)(vertexStartIndexNextRow);
            }
        }

        // back side tris
        for( int i = 0; i < nNumGridCells1D; i++ )
        {
            for( int j = 0; j < nNumGridCells1D; j++ )
            {
                final int vertexStartIndexThisRow = (nNumGridCells1D+1) * (nNumGridCells1D+1) + (nNumGridCells1D+1) * i + j;
                final int vertexStartIndexNextRow = (nNumGridCells1D+1) * (nNumGridCells1D+1) + (nNumGridCells1D+1) * (i+1) + j;
                final int idx = (6 * nNumGridCells1D * nNumGridCells1D) + (6 * nNumGridCells1D * i) + (6*j);
                GridIndexData[idx+0] = (short)(vertexStartIndexThisRow);
                GridIndexData[idx+1] = (short)(vertexStartIndexThisRow+1);
                GridIndexData[idx+2] = (short)(vertexStartIndexNextRow);
                GridIndexData[idx+3] = (short)(vertexStartIndexThisRow+1);
                GridIndexData[idx+4] = (short)(vertexStartIndexNextRow+1);
                GridIndexData[idx+5] = (short)(vertexStartIndexNextRow);
            }
        }
    }


    // forward rendering render target width and height
    private int                    m_uWidth;
    private int                    m_uHeight;

    // buffers for light culling
    private BufferGL               m_pLightIndexBuffer;
    private BufferGL               m_pLightIndexBufferSRV;
    private BufferGL               m_pLightIndexBufferUAV;

    // buffers for light culling with alpha-blended geometry
    private BufferGL               m_pLightIndexBufferForBlendedObjects;
    private BufferGL               m_pLightIndexBufferForBlendedObjectsSRV;
    private BufferGL               m_pLightIndexBufferForBlendedObjectsUAV;

    // buffers for spot light culling
    private BufferGL               m_pSpotIndexBuffer;
    private BufferGL               m_pSpotIndexBufferSRV;
    private BufferGL               m_pSpotIndexBufferUAV;

    // buffers for spot light culling with alpha-blended geometry
    private BufferGL               m_pSpotIndexBufferForBlendedObjects;
    private BufferGL               m_pSpotIndexBufferForBlendedObjectsSRV;
    private BufferGL               m_pSpotIndexBufferForBlendedObjectsUAV;

    // buffers for VPL culling
    private BufferGL               m_pVPLIndexBuffer;
    private BufferGL               m_pVPLIndexBufferSRV;
    private BufferGL               m_pVPLIndexBufferUAV;

    // cube VB and IB (for blended objects)
    private BufferGL               m_pBlendedVB;
    private BufferGL               m_pBlendedIB;
    private GLVAO                  m_pBlendedVAO;

    // instance data for the blended objects
    private BufferGL               m_pBlendedTransform;
    private BufferGL               m_pBlendedTransformSRV;

    // grid VB and IB (for different triangle densities)
    private BufferGL[][]           m_pGridVB = new BufferGL[TRIANGLE_DENSITY_NUM_TYPES][MAX_NUM_GRID_OBJECTS];
    private BufferGL[]             m_pGridIB = new BufferGL[TRIANGLE_DENSITY_NUM_TYPES];

    // grid diffuse and normal map textures
    private Texture2D              m_pGridDiffuseTextureSRV;
    private Texture2D              m_pGridNormalMapSRV;

    // sprite quad VB (for debug drawing the lights-per-tile legend texture)
    private BufferGL               m_pQuadForLegendVB;

    // shaders for transparency (these use forward rendering, but we put them
    // here in CommonUtil instead of in ForwardPlusUtil because they are used
    // by both the Forward+ path and the Tiled Deferred path)
    private ShaderProgram       m_pSceneBlendedVS;
    private ShaderProgram       m_pSceneBlendedDepthVS;
    private ShaderProgram       m_pSceneBlendedPS;
    private ShaderProgram       m_pSceneBlendedPSShadows;
    private ID3D11InputLayout   m_pSceneBlendedLayout;
    private ID3D11InputLayout   m_pSceneBlendedDepthLayout;
    private GLSLProgramPipeline m_pProgramPipeline;

    // compute shaders for tiled culling
    private static final int NUM_LIGHT_CULLING_COMPUTE_SHADERS_FOR_BLENDED_OBJECTS = NUM_MSAA_SETTINGS;  // one for each MSAA setting
    private GLSLProgram[]       m_pLightCullCSForBlendedObjects = new GLSLProgram[NUM_LIGHT_CULLING_COMPUTE_SHADERS_FOR_BLENDED_OBJECTS];

    // debug draw shaders for the lights-per-tile visualization modes
    private ShaderProgram       m_pDebugDrawNumLightsPerTileRadarColorsPS;
    private ShaderProgram       m_pDebugDrawNumLightsPerTileGrayscalePS;
    private ShaderProgram       m_pDebugDrawNumLightsAndVPLsPerTileRadarColorsPS;
    private ShaderProgram       m_pDebugDrawNumLightsAndVPLsPerTileGrayscalePS;
    private ShaderProgram       m_pDebugDrawNumLightsPerTileForTransparencyWithVPLsEnabledRadarColorsPS;
    private ShaderProgram       m_pDebugDrawNumLightsPerTileForTransparencyWithVPLsEnabledGrayscalePS;

    // debug draw shaders for the lights-per-tile legend
    private ShaderProgram       m_pDebugDrawLegendForNumLightsPerTileVS;
    private ShaderProgram       m_pDebugDrawLegendForNumLightsPerTileRadarColorsPS;
    private ShaderProgram       m_pDebugDrawLegendForNumLightsPerTileGrayscalePS;
    private ID3D11InputLayout   m_pDebugDrawLegendForNumLightsLayout11;

    // shaders for full-screen blit/downsample
    private static final int    NUM_FULL_SCREEN_PIXEL_SHADERS = NUM_MSAA_SETTINGS;  // one for each MSAA setting
    private ShaderProgram       m_pFullScreenVS;
    private ShaderProgram[]     m_pFullScreenPS = new ShaderProgram[NUM_FULL_SCREEN_PIXEL_SHADERS];

    // state
    private Runnable[]          m_pDepthStencilState = new Runnable[DEPTH_STENCIL_STATE_NUM_TYPES];
    private Runnable[]          m_pRasterizerState = new Runnable[RASTERIZER_STATE_NUM_TYPES];
    private int[]               m_pSamplerState = new int[SAMPLER_STATE_NUM_TYPES];

    private GLFuncProvider      gl;

    static void InitStaticData(){
        // make sure our indices will actually fit in a R16_UINT
        assert( g_nNumGridVerticesHigh <= 65536 );
        assert( g_nNumGridVerticesMed  <= 65536 );
        assert( g_nNumGridVerticesLow  <= 65536 );

        InitGridObjectData( g_nNumGridCells1DHigh, g_GridVertexDataHigh, g_GridIndexDataHigh );
        InitGridObjectData( g_nNumGridCells1DMed,  g_GridVertexDataMed,  g_GridIndexDataMed  );
        InitGridObjectData( g_nNumGridCells1DLow,  g_GridVertexDataLow,  g_GridIndexDataLow  );

        InitBlendedObjectData();
    }

    //--------------------------------------------------------------------------------------
    // Calculate AABB around all meshes in the scene
    //--------------------------------------------------------------------------------------
    static void CalculateSceneMinMax(SDKmesh Mesh, Vector3f pBBoxMinOut, Vector3f pBBoxMaxOut ){
        /**pBBoxMaxOut = Mesh.GetMeshBBoxCenter( 0 ) + Mesh.GetMeshBBoxExtents( 0 );
        *pBBoxMinOut = Mesh.GetMeshBBoxCenter( 0 ) - Mesh.GetMeshBBoxExtents( 0 );*/

        Vector3f center = Mesh.getMeshBBoxCenter(0);
        Vector3f extents = Mesh.getMeshBBoxExtents(0);

        Vector3f.add(center, extents, pBBoxMinOut);
        Vector3f.sub(center, extents, pBBoxMinOut);

        Vector3f vNewMax = new Vector3f();
        Vector3f vNewMin = new Vector3f();

        for( int i = 1; i < Mesh.getNumMeshes(); i++ )
        {
            /*XMVECTOR vNewMax = Mesh.GetMeshBBoxCenter( i ) + Mesh.GetMeshBBoxExtents( i );
            XMVECTOR vNewMin = Mesh.GetMeshBBoxCenter( i ) - Mesh.GetMeshBBoxExtents( i );*/

            center = Mesh.getMeshBBoxCenter(i);
            extents = Mesh.getMeshBBoxExtents(i);

            Vector3f.add(center, extents, vNewMax);
            Vector3f.sub(center, extents, vNewMin);

            pBBoxMaxOut = Vector3f.max(pBBoxMaxOut, vNewMax, pBBoxMaxOut);
            pBBoxMinOut = Vector3f.max(pBBoxMinOut, vNewMin, pBBoxMinOut);
        }
    }

    void AddShadersToCache( /*AMD::ShaderCache *pShaderCache*/ ){
        // Ensure all shaders (and input layouts) are released
        SAFE_RELEASE( m_pSceneBlendedVS );
        SAFE_RELEASE( m_pSceneBlendedDepthVS );
        SAFE_RELEASE( m_pSceneBlendedPS );
        SAFE_RELEASE( m_pSceneBlendedPSShadows );
//        SAFE_RELEASE( m_pSceneBlendedLayout );
//        SAFE_RELEASE( m_pSceneBlendedDepthLayout );

        for( int i = 0; i < NUM_LIGHT_CULLING_COMPUTE_SHADERS_FOR_BLENDED_OBJECTS; i++ )
        {
            SAFE_RELEASE(m_pLightCullCSForBlendedObjects[i]);
        }

        SAFE_RELEASE( m_pDebugDrawNumLightsPerTileRadarColorsPS );
        SAFE_RELEASE( m_pDebugDrawNumLightsPerTileGrayscalePS );
        SAFE_RELEASE( m_pDebugDrawNumLightsAndVPLsPerTileRadarColorsPS );
        SAFE_RELEASE( m_pDebugDrawNumLightsAndVPLsPerTileGrayscalePS );
        SAFE_RELEASE( m_pDebugDrawNumLightsPerTileForTransparencyWithVPLsEnabledRadarColorsPS );
        SAFE_RELEASE( m_pDebugDrawNumLightsPerTileForTransparencyWithVPLsEnabledGrayscalePS );
        SAFE_RELEASE( m_pDebugDrawLegendForNumLightsPerTileVS );
        SAFE_RELEASE( m_pDebugDrawLegendForNumLightsPerTileRadarColorsPS );
        SAFE_RELEASE( m_pDebugDrawLegendForNumLightsPerTileGrayscalePS );
//        SAFE_RELEASE( m_pDebugDrawLegendForNumLightsLayout11 );

        SAFE_RELEASE(m_pFullScreenVS);

        for( int i = 0; i < NUM_FULL_SCREEN_PIXEL_SHADERS; i++ )
        {
            SAFE_RELEASE(m_pFullScreenPS[i]);
        }

        Macro ShaderMacroFullScreenPS = new Macro("NUM_MSAA_SAMPLES", 0);
//        wcscpy_s( ShaderMacroFullScreenPS.m_wsName, AMD::ShaderCache::m_uMACRO_MAX_LENGTH, L"NUM_MSAA_SAMPLES" );

        Macro ShaderMacroBlendedPS = new Macro("SHADOWS_ENABLED", 0);
//        wcscpy_s( ShaderMacroBlendedPS.m_wsName, AMD::ShaderCache::m_uMACRO_MAX_LENGTH, L"SHADOWS_ENABLED" );

        Macro ShaderMacroDebugDrawNumLightsPerTilePS[] = {
                new Macro("VPLS_ENABLED", 1),
                new Macro("BLENDED_PASS", 1),
        };
//        wcscpy_s( ShaderMacroDebugDrawNumLightsPerTilePS[0].m_wsName, AMD::ShaderCache::m_uMACRO_MAX_LENGTH, L"VPLS_ENABLED" );
//        wcscpy_s( ShaderMacroDebugDrawNumLightsPerTilePS[1].m_wsName, AMD::ShaderCache::m_uMACRO_MAX_LENGTH, L"BLENDED_PASS" );

        final int DXGI_FORMAT_R32G32B32_FLOAT = GLenum.GL_RGBA32F;
        final int DXGI_FORMAT_R16G16_FLOAT = GLenum.GL_RG16F;
        final int DXGI_FORMAT_R32G32_FLOAT = GLenum.GL_RG32F;
        final int D3D11_INPUT_PER_VERTEX_DATA = 0;

        final D3D11_INPUT_ELEMENT_DESC LayoutForBlendedObjects[] =
        {
                new D3D11_INPUT_ELEMENT_DESC( "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0,  0, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
                new D3D11_INPUT_ELEMENT_DESC( "NORMAL",   0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 12, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
                new D3D11_INPUT_ELEMENT_DESC( "TEXCOORD", 0, DXGI_FORMAT_R16G16_FLOAT,    0, 24, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
        };

        final D3D11_INPUT_ELEMENT_DESC LayoutForSprites[] =
        {
                new D3D11_INPUT_ELEMENT_DESC( "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0,  0, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
                new D3D11_INPUT_ELEMENT_DESC( "TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT,    0, 12, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
        };

        m_pSceneBlendedLayout = ID3D11InputLayout.createInputLayoutFrom(LayoutForBlendedObjects);
        m_pSceneBlendedDepthLayout = ID3D11InputLayout.createInputLayoutFrom(LayoutForBlendedObjects);

        throw new UnsupportedOperationException("Don't forget to create shaders.");
        /*pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pSceneBlendedVS, AMD::ShaderCache::SHADER_TYPE_VERTEX, L"vs_5_0", L"RenderBlendedVS",
                L"Transparency.hlsl", 0, NULL, &m_pSceneBlendedLayout, LayoutForBlendedObjects, ARRAYSIZE( LayoutForBlendedObjects ) );

        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pSceneBlendedDepthVS, AMD::ShaderCache::SHADER_TYPE_VERTEX, L"vs_5_0", L"RenderBlendedDepthVS",
                L"Transparency.hlsl", 0, NULL, &m_pSceneBlendedDepthLayout, LayoutForBlendedObjects, ARRAYSIZE( LayoutForBlendedObjects ) );

        // SHADOWS_ENABLED = 0 (false)
        ShaderMacroBlendedPS.m_iValue = 0;
        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pSceneBlendedPS, AMD::ShaderCache::SHADER_TYPE_PIXEL, L"ps_5_0", L"RenderBlendedPS",
                L"Transparency.hlsl", 1, &ShaderMacroBlendedPS, NULL, NULL, 0 );

        // SHADOWS_ENABLED = 1 (true)
        ShaderMacroBlendedPS.m_iValue = 1;
        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pSceneBlendedPSShadows, AMD::ShaderCache::SHADER_TYPE_PIXEL, L"ps_5_0", L"RenderBlendedPS",
                L"Transparency.hlsl", 1, &ShaderMacroBlendedPS, NULL, NULL, 0 );

        // BLENDED_PASS = 0 (false)
        ShaderMacroDebugDrawNumLightsPerTilePS[1].m_iValue = 0;

        // VPLS_ENABLED = 0 (false)
        ShaderMacroDebugDrawNumLightsPerTilePS[0].m_iValue = 0;
        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pDebugDrawNumLightsPerTileRadarColorsPS, AMD::ShaderCache::SHADER_TYPE_PIXEL, L"ps_5_0", L"DebugDrawNumLightsPerTileRadarColorsPS",
                L"DebugDraw.hlsl", 2, ShaderMacroDebugDrawNumLightsPerTilePS, NULL, NULL, 0 );

        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pDebugDrawNumLightsPerTileGrayscalePS, AMD::ShaderCache::SHADER_TYPE_PIXEL, L"ps_5_0", L"DebugDrawNumLightsPerTileGrayscalePS",
                L"DebugDraw.hlsl", 2, ShaderMacroDebugDrawNumLightsPerTilePS, NULL, NULL, 0 );

        // VPLS_ENABLED = 1 (true)
        ShaderMacroDebugDrawNumLightsPerTilePS[0].m_iValue = 1;
        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pDebugDrawNumLightsAndVPLsPerTileRadarColorsPS, AMD::ShaderCache::SHADER_TYPE_PIXEL, L"ps_5_0", L"DebugDrawNumLightsPerTileRadarColorsPS",
                L"DebugDraw.hlsl", 2, ShaderMacroDebugDrawNumLightsPerTilePS, NULL, NULL, 0 );

        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pDebugDrawNumLightsAndVPLsPerTileGrayscalePS, AMD::ShaderCache::SHADER_TYPE_PIXEL, L"ps_5_0", L"DebugDrawNumLightsPerTileGrayscalePS",
                L"DebugDraw.hlsl", 2, ShaderMacroDebugDrawNumLightsPerTilePS, NULL, NULL, 0 );

        // BLENDED_PASS = 1 (true), still with VPLS_ENABLED = 1 (true)
        ShaderMacroDebugDrawNumLightsPerTilePS[1].m_iValue = 1;
        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pDebugDrawNumLightsPerTileForTransparencyWithVPLsEnabledRadarColorsPS, AMD::ShaderCache::SHADER_TYPE_PIXEL, L"ps_5_0", L"DebugDrawNumLightsPerTileRadarColorsPS",
                L"DebugDraw.hlsl", 2, ShaderMacroDebugDrawNumLightsPerTilePS, NULL, NULL, 0 );

        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pDebugDrawNumLightsPerTileForTransparencyWithVPLsEnabledGrayscalePS, AMD::ShaderCache::SHADER_TYPE_PIXEL, L"ps_5_0", L"DebugDrawNumLightsPerTileGrayscalePS",
                L"DebugDraw.hlsl", 2, ShaderMacroDebugDrawNumLightsPerTilePS, NULL, NULL, 0 );

        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pDebugDrawLegendForNumLightsPerTileVS, AMD::ShaderCache::SHADER_TYPE_VERTEX, L"vs_5_0", L"DebugDrawLegendForNumLightsPerTileVS",
                L"DebugDraw.hlsl", 0, NULL, &m_pDebugDrawLegendForNumLightsLayout11, LayoutForSprites, ARRAYSIZE( LayoutForSprites ) );

        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pDebugDrawLegendForNumLightsPerTileRadarColorsPS, AMD::ShaderCache::SHADER_TYPE_PIXEL, L"ps_5_0", L"DebugDrawLegendForNumLightsPerTileRadarColorsPS",
                L"DebugDraw.hlsl", 0, NULL, NULL, NULL, 0 );

        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pDebugDrawLegendForNumLightsPerTileGrayscalePS, AMD::ShaderCache::SHADER_TYPE_PIXEL, L"ps_5_0", L"DebugDrawLegendForNumLightsPerTileGrayscalePS",
                L"DebugDraw.hlsl", 0, NULL, NULL, NULL, 0 );

        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pFullScreenVS, AMD::ShaderCache::SHADER_TYPE_VERTEX, L"vs_5_0", L"FullScreenQuadVS",
                L"Common.hlsl", 0, NULL, NULL, NULL, 0 );

        // sanity check
        assert(NUM_FULL_SCREEN_PIXEL_SHADERS == NUM_MSAA_SETTINGS);

        for( int i = 0; i < NUM_FULL_SCREEN_PIXEL_SHADERS; i++ )
        {
            // set NUM_MSAA_SAMPLES
            ShaderMacroFullScreenPS.m_iValue = g_nMSAASampleCount[i];
            pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pFullScreenPS[i], AMD::ShaderCache::SHADER_TYPE_PIXEL, L"ps_5_0", L"FullScreenBlitPS",
                L"Common.hlsl", 1, &ShaderMacroFullScreenPS, NULL, NULL, 0 );
        }

        AMD::ShaderCache::Macro ShaderMacroLightCullCS[2];
        wcscpy_s( ShaderMacroLightCullCS[0].m_wsName, AMD::ShaderCache::m_uMACRO_MAX_LENGTH, L"TILED_CULLING_COMPUTE_SHADER_MODE" );
        wcscpy_s( ShaderMacroLightCullCS[1].m_wsName, AMD::ShaderCache::m_uMACRO_MAX_LENGTH, L"NUM_MSAA_SAMPLES" );

        // Set TILED_CULLING_COMPUTE_SHADER_MODE to 4 (blended geometry mode)
        ShaderMacroLightCullCS[0].m_iValue = 4;

        // sanity check
        assert(NUM_LIGHT_CULLING_COMPUTE_SHADERS_FOR_BLENDED_OBJECTS == NUM_MSAA_SETTINGS);

        for( int i = 0; i < NUM_MSAA_SETTINGS; i++ )
        {
            // set NUM_MSAA_SAMPLES
            ShaderMacroLightCullCS[1].m_iValue = g_nMSAASampleCount[i];
            pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pLightCullCSForBlendedObjects[i], AMD::ShaderCache::SHADER_TYPE_COMPUTE, L"cs_5_0", L"CullLightsCS",
                L"TilingForward.hlsl", 2, ShaderMacroLightCullCS, NULL, NULL, 0 );
        }*/
    }

    void SortTransparentObjects(ReadableVector3f vEyePt){
        UpdateBlendedObjectData( vEyePt );
    }

    void RenderTransparentObjects(int nDebugDrawType, boolean bShadowsEnabled, boolean bVPLsEnabled, boolean bDepthOnlyRendering){
       /* D3D11_MAPPED_SUBRESOURCE MappedResource;
        ID3D11DeviceContext* pd3dImmediateContext = DXUTGetD3D11DeviceContext();

        // update the buffer containing the per-instance transforms
        V( pd3dImmediateContext->Map( m_pBlendedTransform, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource ) );
        XMMATRIX* BlendedTransformArray = (XMMATRIX*)MappedResource.pData;
        for ( int i = 0; i < g_NumBlendedObjects; i++ )
        {
            BlendedTransformArray[i] = XMMatrixTranspose(g_BlendedObjectInstanceTransform[ g_BlendedObjectIndices[ i ].nIndex ]);
        }
        pd3dImmediateContext->Unmap( m_pBlendedTransform, 0 );*/

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(m_pBlendedTransform.getBufferSize());
        for ( int i = 0; i < g_NumBlendedObjects; i++ )
        {
            g_BlendedObjectInstanceTransform[ g_BlendedObjectIndices[ i ].nIndex ].store(buffer);
        }
        buffer.flip();
        m_pBlendedTransform.update(0, buffer);

        if( bDepthOnlyRendering )
        {
//            pd3dImmediateContext->IASetInputLayout( m_pSceneBlendedDepthLayout );
//            m_pSceneBlendedDepthLayout.bind();  todo
        }
        else
        {
//            pd3dImmediateContext->IASetInputLayout( m_pSceneBlendedLayout );
//            m_pSceneBlendedLayout.bind();  todo
        }

        /*UINT strides[] = { 28 };
        UINT offsets[] = { 0 };
        pd3dImmediateContext->IASetIndexBuffer( m_pBlendedIB, DXGI_FORMAT_R16_UINT, 0 );
        pd3dImmediateContext->IASetVertexBuffers( 0, 1, &m_pBlendedVB, strides, offsets );
        pd3dImmediateContext->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST );*/

        gl.glUseProgram(0);
        m_pProgramPipeline.enable();
        m_pBlendedVAO.bind();

        /*ID3D11ShaderResourceView* pNULLSRV = NULL;
        ID3D11SamplerState* pNULLSampler = NULL;*/

        if( bDepthOnlyRendering )
        {
            /*pd3dImmediateContext->VSSetShader( m_pSceneBlendedDepthVS, NULL, 0 );
            pd3dImmediateContext->PSSetShader( NULL, NULL, 0 );  // null pixel shader*/
            m_pProgramPipeline.setVS(m_pSceneBlendedDepthVS);
            m_pProgramPipeline.setPS(null);
        }
        else
        {
//            pd3dImmediateContext->VSSetShader( m_pSceneBlendedVS, NULL, 0 );
            m_pProgramPipeline.setVS(m_pSceneBlendedVS);

            // See if we need to use one of the debug drawing shaders instead of the default
            boolean bDebugDrawingEnabled = ( nDebugDrawType == DEBUG_DRAW_RADAR_COLORS ) || ( nDebugDrawType == DEBUG_DRAW_GRAYSCALE );
            if( bDebugDrawingEnabled )
            {
                // although transparency does not use VPLs, we still need to use bVPLsEnabled to get the correct lights-per-tile
                // visualization shader, because bVPLsEnabled = true increases the max num lights per tile
//                pd3dImmediateContext->PSSetShader( GetDebugDrawNumLightsPerTilePS(nDebugDrawType, bVPLsEnabled, true), NULL, 0 );
                m_pProgramPipeline.setPS(GetDebugDrawNumLightsPerTilePS(nDebugDrawType, bVPLsEnabled, true));
            }
            else
            {
                ShaderProgram pSceneBlendedPS = bShadowsEnabled ? m_pSceneBlendedPSShadows : m_pSceneBlendedPS;
//                pd3dImmediateContext->PSSetShader( pSceneBlendedPS, NULL, 0 );
                m_pProgramPipeline.setPS(pSceneBlendedPS);
            }
        }

        /*pd3dImmediateContext->VSSetShaderResources( 0, 1, &m_pBlendedTransformSRV );
        pd3dImmediateContext->PSSetShaderResources( 0, 1, &pNULLSRV );
        pd3dImmediateContext->PSSetShaderResources( 1, 1, &pNULLSRV );
        pd3dImmediateContext->PSSetSamplers( 0, 1, &pNULLSampler );*/
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_pBlendedTransformSRV.getBuffer());

        /*pd3dImmediateContext->DrawIndexedInstanced( 36, g_NumBlendedObjects, 0, 0, 0 );
        pd3dImmediateContext->VSSetShaderResources( 0, 1, &pNULLSRV );*/
        m_pBlendedVAO.draw(GLenum.GL_TRIANGLES, g_NumBlendedObjects);
        m_pBlendedVAO.unbind();

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, 0);
    }

    void RenderLegend(/*CDXUTTextHelper *pTxtHelper,*/ int nLineHeight, ReadableVector4f Color, int nDebugDrawType, boolean bVPLsEnabled ){
        // draw the legend texture for the lights-per-tile visualization
        {
            /*ID3D11ShaderResourceView* pNULLSRV = NULL;
            ID3D11SamplerState* pNULLSampler = NULL;*/

            // choose pixel shader based on radar vs. grayscale
            ShaderProgram pPixelShader = ( nDebugDrawType == DEBUG_DRAW_GRAYSCALE ) ? m_pDebugDrawLegendForNumLightsPerTileGrayscalePS : m_pDebugDrawLegendForNumLightsPerTileRadarColorsPS;

//            ID3D11DeviceContext* pd3dImmediateContext = DXUTGetD3D11DeviceContext();

            // save depth state (for later restore)
            /*ID3D11DepthStencilState* pDepthStencilStateStored11 = NULL;
            UINT uStencilRefStored11;
            pd3dImmediateContext->OMGetDepthStencilState( &pDepthStencilStateStored11, &uStencilRefStored11 );*/

            // disable depth test
//            pd3dImmediateContext->OMSetDepthStencilState( GetDepthStencilState(DEPTH_STENCIL_STATE_DISABLE_DEPTH_TEST), 0x00 );
            GetDepthStencilState(DEPTH_STENCIL_STATE_DISABLE_DEPTH_TEST).run();

            // Set vertex buffer
            /*UINT uStride = sizeof( CommonUtilSpriteVertex );
            UINT uOffset = 0;
            pd3dImmediateContext->IASetVertexBuffers( 0, 1, &m_pQuadForLegendVB, &uStride, &uOffset );*/
            m_pQuadForLegendVB.bind();
            // Set the input layout
            m_pDebugDrawLegendForNumLightsLayout11.bind();

            // Set primitive topology
            /*pd3dImmediateContext->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST );

            pd3dImmediateContext->VSSetShader( m_pDebugDrawLegendForNumLightsPerTileVS, NULL, 0 );
            pd3dImmediateContext->PSSetShader( pPixelShader, NULL, 0 );
            pd3dImmediateContext->PSSetShaderResources( 0, 1, &pNULLSRV );
            pd3dImmediateContext->PSSetShaderResources( 1, 1, &pNULLSRV );
            pd3dImmediateContext->PSSetSamplers( 0, 1, &pNULLSampler );*/
            m_pProgramPipeline.enable();
            m_pProgramPipeline.setVS(m_pDebugDrawLegendForNumLightsPerTileVS);
            m_pProgramPipeline.setPS(pPixelShader);

//            pd3dImmediateContext->Draw(6,0);
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 6);

            // restore to previous
            /*pd3dImmediateContext->OMSetDepthStencilState( pDepthStencilStateStored11, uStencilRefStored11 );
            SAFE_RELEASE( pDepthStencilStateStored11 );*/
        }
    }

    // Various hook functions
    void OnCreateDevice( /*ID3D11Device* pd3dDevice*/ ){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_pProgramPipeline = new GLSLProgramPipeline();

        // Create the alpha blended cube geometry
//        AMD::CreateCube( 40.0f, &m_pBlendedVB, &m_pBlendedIB );
        m_pBlendedVAO = ModelGenerator.genCube(40, true, false, false).genVAO();

        // Create the buffer for the instance data
        /*D3D11_BUFFER_DESC BlendedObjectTransformBufferDesc;
        ZeroMemory( &BlendedObjectTransformBufferDesc, sizeof(BlendedObjectTransformBufferDesc) );
        BlendedObjectTransformBufferDesc.Usage = D3D11_USAGE_DYNAMIC;
        BlendedObjectTransformBufferDesc.ByteWidth = sizeof( g_BlendedObjectInstanceTransform );
        BlendedObjectTransformBufferDesc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
        BlendedObjectTransformBufferDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        BlendedObjectTransformBufferDesc.MiscFlags = D3D11_RESOURCE_MISC_BUFFER_STRUCTURED;
        BlendedObjectTransformBufferDesc.StructureByteStride = sizeof( XMMATRIX );
        V_RETURN( pd3dDevice->CreateBuffer( &BlendedObjectTransformBufferDesc, 0, &m_pBlendedTransform ) );
        DXUT_SetDebugName( m_pBlendedTransform, "BlendedTransform" );*/
        m_pBlendedTransform = new BufferGL();
        m_pBlendedTransform.initlize(GLenum.GL_ARRAY_BUFFER, g_BlendedObjectInstanceTransform.length * Matrix4f.SIZE, null, GLenum.GL_STATIC_DRAW);

        /*D3D11_SHADER_RESOURCE_VIEW_DESC BlendedObjectTransformBufferSRVDesc;
        ZeroMemory( &BlendedObjectTransformBufferSRVDesc, sizeof( D3D11_SHADER_RESOURCE_VIEW_DESC ) );
        BlendedObjectTransformBufferSRVDesc.Format = DXGI_FORMAT_UNKNOWN;
        BlendedObjectTransformBufferSRVDesc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
        BlendedObjectTransformBufferSRVDesc.Buffer.ElementOffset = 0;
        BlendedObjectTransformBufferSRVDesc.Buffer.ElementWidth = g_NumBlendedObjects;
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pBlendedTransform, &BlendedObjectTransformBufferSRVDesc, &m_pBlendedTransformSRV ) );*/
        m_pBlendedTransformSRV = m_pBlendedTransform;

        // Create the vertex buffer for the grid objects
        /*D3D11_BUFFER_DESC VBDesc;
        ZeroMemory( &VBDesc, sizeof(VBDesc) );
        VBDesc.Usage = D3D11_USAGE_IMMUTABLE;
        VBDesc.ByteWidth = sizeof( g_GridVertexDataHigh[0] );
        VBDesc.BindFlags = D3D11_BIND_VERTEX_BUFFER;*/
        for( int i = 0; i < MAX_NUM_GRID_OBJECTS; i++ )
        {
            /*InitData.pSysMem = g_GridVertexDataHigh[i];
            V_RETURN( pd3dDevice->CreateBuffer( &VBDesc, &InitData, &m_pGridVB[TRIANGLE_DENSITY_HIGH][i] ) );*/
            ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(g_GridVertexDataHigh[0].length * CommonUtilGridVertex.SIZE);
            CacheBuffer.put( buffer, g_GridVertexDataHigh[i]);
            buffer.flip();

            m_pGridVB[TRIANGLE_DENSITY_HIGH][i] = new BufferGL();
            m_pGridVB[TRIANGLE_DENSITY_HIGH][i].initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        }

//        VBDesc.ByteWidth = sizeof( g_GridVertexDataMed[0] );
        for( int i = 0; i < MAX_NUM_GRID_OBJECTS; i++ )
        {
            /*InitData.pSysMem = g_GridVertexDataMed[i];
            V_RETURN( pd3dDevice->CreateBuffer( &VBDesc, &InitData, &m_pGridVB[TRIANGLE_DENSITY_MEDIUM][i] ) );*/
            ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(g_GridVertexDataHigh[0].length * CommonUtilGridVertex.SIZE);
            CacheBuffer.put( buffer, g_GridVertexDataMed[i]);
            buffer.flip();

            m_pGridVB[TRIANGLE_DENSITY_MEDIUM][i] = new BufferGL();
            m_pGridVB[TRIANGLE_DENSITY_MEDIUM][i].initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        }

//        VBDesc.ByteWidth = sizeof( g_GridVertexDataLow[0] );
        for( int i = 0; i < MAX_NUM_GRID_OBJECTS; i++ )
        {
            /*InitData.pSysMem = g_GridVertexDataLow[i];
            V_RETURN( pd3dDevice->CreateBuffer( &VBDesc, &InitData, &m_pGridVB[TRIANGLE_DENSITY_LOW][i] ) );*/
            ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(g_GridVertexDataLow[0].length * CommonUtilGridVertex.SIZE);
            CacheBuffer.put( buffer, g_GridVertexDataLow[i]);
            buffer.flip();

            m_pGridVB[TRIANGLE_DENSITY_LOW][i] = new BufferGL();
            m_pGridVB[TRIANGLE_DENSITY_LOW][i].initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        }

        // Create the index buffer for the grid objects
        /*D3D11_BUFFER_DESC IBDesc;
        ZeroMemory( &IBDesc, sizeof(IBDesc) );
        IBDesc.Usage = D3D11_USAGE_IMMUTABLE;
        IBDesc.ByteWidth = sizeof( g_GridIndexDataHigh );
        IBDesc.BindFlags = D3D11_BIND_INDEX_BUFFER;
        InitData.pSysMem = g_GridIndexDataHigh;
        V_RETURN( pd3dDevice->CreateBuffer( &IBDesc, &InitData, &m_pGridIB[TRIANGLE_DENSITY_HIGH] ) );*/

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(g_GridIndexDataHigh.length * 2);
        CacheBuffer.put( buffer, g_GridIndexDataHigh);
        buffer.flip();

        m_pGridIB[TRIANGLE_DENSITY_HIGH] = new BufferGL();
        m_pGridIB[TRIANGLE_DENSITY_HIGH].initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);

        /*IBDesc.ByteWidth = sizeof( g_GridIndexDataMed );
        InitData.pSysMem = g_GridIndexDataMed;
        V_RETURN( pd3dDevice->CreateBuffer( &IBDesc, &InitData, &m_pGridIB[TRIANGLE_DENSITY_MEDIUM] ) );*/

        buffer = CacheBuffer.getCachedByteBuffer(g_GridIndexDataMed.length * 2);
        CacheBuffer.put( buffer, g_GridIndexDataMed);
        buffer.flip();

        m_pGridIB[TRIANGLE_DENSITY_MEDIUM] = new BufferGL();
        m_pGridIB[TRIANGLE_DENSITY_MEDIUM].initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);

        /*IBDesc.ByteWidth = sizeof( g_GridIndexDataLow );
        InitData.pSysMem = g_GridIndexDataLow;
        V_RETURN( pd3dDevice->CreateBuffer( &IBDesc, &InitData, &m_pGridIB[TRIANGLE_DENSITY_LOW] ) );*/

        buffer = CacheBuffer.getCachedByteBuffer(g_GridIndexDataLow.length * 2);
        CacheBuffer.put( buffer, g_GridIndexDataLow);
        buffer.flip();

        m_pGridIB[TRIANGLE_DENSITY_LOW] = new BufferGL();
        m_pGridIB[TRIANGLE_DENSITY_LOW].initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);

        // Load the diffuse and normal map for the grid
        {
            /*WCHAR path[MAX_PATH];
            DXUTFindDXSDKMediaFileCch( path, MAX_PATH, L"misc\\default_diff.dds" );
            // Create the shader resource view.
            CreateDDSTextureFromFile( pd3dDevice, path, NULL, &m_pGridDiffuseTextureSRV );
            DXUTFindDXSDKMediaFileCch( path, MAX_PATH, L"misc\\default_norm.dds" );
            // Create the shader resource view.
            CreateDDSTextureFromFile( pd3dDevice, path, NULL, &m_pGridNormalMapSRV );*/

            try {
                int diffTex = NvImage.uploadTextureFromDDSFile("amdfx/TiledLighting11/textures/default_diff.dds");
                m_pGridDiffuseTextureSRV = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, diffTex);

                int normalTex = NvImage.uploadTextureFromDDSFile("amdfx/TiledLighting11/textures/default_norm.dds");
                m_pGridNormalMapSRV = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, normalTex);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        // Default depth-stencil state, except with inverted DepthFunc
        // (because we are using inverted 32-bit float depth for better precision)
        /*D3D11_DEPTH_STENCIL_DESC DepthStencilDesc;
        ZeroMemory( &DepthStencilDesc, sizeof( D3D11_DEPTH_STENCIL_DESC ) );
        DepthStencilDesc.DepthEnable = TRUE;
        DepthStencilDesc.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ALL;
        DepthStencilDesc.DepthFunc = D3D11_COMPARISON_GREATER;  // we are using inverted 32-bit float depth for better precision
        DepthStencilDesc.StencilEnable = FALSE;
        DepthStencilDesc.StencilReadMask = D3D11_DEFAULT_STENCIL_READ_MASK;
        DepthStencilDesc.StencilWriteMask = D3D11_DEFAULT_STENCIL_WRITE_MASK;
        V_RETURN( pd3dDevice->CreateDepthStencilState( &DepthStencilDesc, &m_pDepthStencilState[DEPTH_STENCIL_STATE_DEPTH_GREATER] ) );*/

        m_pDepthStencilState[DEPTH_STENCIL_STATE_DEPTH_GREATER]= ()->
        {
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthMask(true);
            gl.glDepthFunc(GLenum.GL_GREATER);

            gl.glDisable(GLenum.GL_STENCIL_TEST);
        };

        // Disable depth test write
        /*DepthStencilDesc.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ZERO;
        V_RETURN( pd3dDevice->CreateDepthStencilState( &DepthStencilDesc, &m_pDepthStencilState[DEPTH_STENCIL_STATE_DISABLE_DEPTH_WRITE] ) );*/

        m_pDepthStencilState[DEPTH_STENCIL_STATE_DISABLE_DEPTH_WRITE] = ()->
        {
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthMask(false);
            gl.glDepthFunc(GLenum.GL_GREATER);

            gl.glDisable(GLenum.GL_STENCIL_TEST);
        };

        // Disable depth test
        /*DepthStencilDesc.DepthEnable = FALSE;
        V_RETURN( pd3dDevice->CreateDepthStencilState( &DepthStencilDesc, &m_pDepthStencilState[DEPTH_STENCIL_STATE_DISABLE_DEPTH_TEST] ) );*/

        m_pDepthStencilState[DEPTH_STENCIL_STATE_DISABLE_DEPTH_TEST]= ()->
        {
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glDepthMask(false);
            gl.glDepthFunc(GLenum.GL_GREATER);

            gl.glDisable(GLenum.GL_STENCIL_TEST);
        };

        // Comparison greater with depth writes disabled
        /*DepthStencilDesc.DepthEnable = TRUE;
        DepthStencilDesc.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ZERO;
        DepthStencilDesc.DepthFunc = D3D11_COMPARISON_GREATER;  // we are using inverted 32-bit float depth for better precision
        V_RETURN( pd3dDevice->CreateDepthStencilState( &DepthStencilDesc, &m_pDepthStencilState[DEPTH_STENCIL_STATE_DEPTH_GREATER_AND_DISABLE_DEPTH_WRITE] ) );*/

        m_pDepthStencilState[DEPTH_STENCIL_STATE_DEPTH_GREATER_AND_DISABLE_DEPTH_WRITE] = ()->
        {
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthMask(false);
            gl.glDepthFunc(GLenum.GL_GREATER);

            gl.glDisable(GLenum.GL_STENCIL_TEST);
        };

        // Comparison equal with depth writes disabled
        /*DepthStencilDesc.DepthFunc = D3D11_COMPARISON_EQUAL;
        V_RETURN( pd3dDevice->CreateDepthStencilState( &DepthStencilDesc, &m_pDepthStencilState[DEPTH_STENCIL_STATE_DEPTH_EQUAL_AND_DISABLE_DEPTH_WRITE] ) );*/

        m_pDepthStencilState[DEPTH_STENCIL_STATE_DEPTH_EQUAL_AND_DISABLE_DEPTH_WRITE] = ()->
        {
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthMask(false);
            gl.glDepthFunc(GLenum.GL_EQUAL);

            gl.glDisable(GLenum.GL_STENCIL_TEST);
        };

        // Comparison less, for shadow maps
        /*DepthStencilDesc.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ALL;
        DepthStencilDesc.DepthFunc = D3D11_COMPARISON_LESS;
        V_RETURN( pd3dDevice->CreateDepthStencilState( &DepthStencilDesc, &m_pDepthStencilState[DEPTH_STENCIL_STATE_DEPTH_LESS] ) );*/

        m_pDepthStencilState[DEPTH_STENCIL_STATE_DEPTH_LESS] = ()->
        {
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthMask(true);
            gl.glDepthFunc(GLenum.GL_LESS);

            gl.glDisable(GLenum.GL_STENCIL_TEST);
        };

        // Disable culling
        /*D3D11_RASTERIZER_DESC RasterizerDesc;
        RasterizerDesc.FillMode = D3D11_FILL_SOLID;
        RasterizerDesc.CullMode = D3D11_CULL_NONE;       // disable culling
        RasterizerDesc.FrontCounterClockwise = FALSE;
        RasterizerDesc.DepthBias = 0;
        RasterizerDesc.DepthBiasClamp = 0.0f;
        RasterizerDesc.SlopeScaledDepthBias = 0.0f;
        RasterizerDesc.DepthClipEnable = TRUE;
        RasterizerDesc.ScissorEnable = FALSE;
        RasterizerDesc.MultisampleEnable = FALSE;
        RasterizerDesc.AntialiasedLineEnable = FALSE;
        V_RETURN( pd3dDevice->CreateRasterizerState( &RasterizerDesc, &m_pRasterizerState[RASTERIZER_STATE_DISABLE_CULLING] ) );*/

        m_pRasterizerState[RASTERIZER_STATE_DISABLE_CULLING] = ()->
        {
            gl.glDisable(GLenum.GL_CULL_FACE);
            gl.glFrontFace(GLenum.GL_CCW);
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
        };

        /*RasterizerDesc.FillMode = D3D11_FILL_WIREFRAME;  // wireframe
        RasterizerDesc.CullMode = D3D11_CULL_BACK;
        V_RETURN( pd3dDevice->CreateRasterizerState( &RasterizerDesc, &m_pRasterizerState[RASTERIZER_STATE_WIREFRAME] ) );*/

        m_pRasterizerState[RASTERIZER_STATE_WIREFRAME] = ()->
        {
            gl.glEnable(GLenum.GL_CULL_FACE);
            gl.glFrontFace(GLenum.GL_CCW);
            gl.glCullFace(GLenum.GL_BACK);
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
        };

        /*RasterizerDesc.FillMode = D3D11_FILL_WIREFRAME;  // wireframe and ...
        RasterizerDesc.CullMode = D3D11_CULL_NONE;       // disable culling
        V_RETURN( pd3dDevice->CreateRasterizerState( &RasterizerDesc, &m_pRasterizerState[RASTERIZER_STATE_WIREFRAME_DISABLE_CULLING] ) );*/

        m_pRasterizerState[RASTERIZER_STATE_WIREFRAME_DISABLE_CULLING] = ()->
        {
            gl.glDisable(GLenum.GL_CULL_FACE);
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
        };

        // Create state objects
        /*D3D11_SAMPLER_DESC SamplerDesc;
        ZeroMemory( &SamplerDesc, sizeof(SamplerDesc) );
        SamplerDesc.Filter = D3D11_FILTER_MIN_MAG_MIP_POINT;
        SamplerDesc.AddressU = SamplerDesc.AddressV = SamplerDesc.AddressW = D3D11_TEXTURE_ADDRESS_WRAP;
        SamplerDesc.MaxAnisotropy = 16;
        SamplerDesc.ComparisonFunc = D3D11_COMPARISON_NEVER;
        SamplerDesc.MinLOD = -D3D11_FLOAT32_MAX;
        SamplerDesc.MaxLOD =  D3D11_FLOAT32_MAX;
        V_RETURN( pd3dDevice->CreateSamplerState( &SamplerDesc, &m_pSamplerState[SAMPLER_STATE_POINT] ) );
        SamplerDesc.Filter = D3D11_FILTER_MIN_MAG_MIP_LINEAR;
        V_RETURN( pd3dDevice->CreateSamplerState( &SamplerDesc, &m_pSamplerState[SAMPLER_STATE_LINEAR] ) );
        SamplerDesc.Filter = D3D11_FILTER_ANISOTROPIC;
        V_RETURN( pd3dDevice->CreateSamplerState( &SamplerDesc, &m_pSamplerState[SAMPLER_STATE_ANISO] ) );*/

        SamplerDesc samplerDesc = new SamplerDesc();
        m_pSamplerState[SAMPLER_STATE_LINEAR] = SamplerUtils.createSampler(samplerDesc);
        samplerDesc.magFilter = GLenum.GL_NEAREST;
        samplerDesc.minFilter = GLenum.GL_NEAREST;
        m_pSamplerState[SAMPLER_STATE_POINT] = SamplerUtils.createSampler(samplerDesc);
        samplerDesc.anisotropic = 16;
        m_pSamplerState[SAMPLER_STATE_ANISO] = SamplerUtils.createSampler(samplerDesc);

        // One more for shadows
       /* SamplerDesc.Filter = D3D11_FILTER_COMPARISON_MIN_MAG_MIP_LINEAR;
        SamplerDesc.ComparisonFunc = D3D11_COMPARISON_LESS_EQUAL;
        SamplerDesc.AddressU = SamplerDesc.AddressV = SamplerDesc.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP;
        SamplerDesc.MaxAnisotropy = 1;
        V_RETURN( pd3dDevice->CreateSamplerState( &SamplerDesc, &m_pSamplerState[SAMPLER_STATE_SHADOW] ) );*/

        m_pSamplerState[SAMPLER_STATE_SHADOW] = SamplerUtils.getDepthComparisonSampler();

    }
    void OnDestroyDevice() {dispose();}

    void OnResizedSwapChain( /*ID3D11Device* pd3dDevice, const DXGI_SURFACE_DESC* pBackBufferSurfaceDesc*/int widh, int height, int nLineHeight ){
        m_uWidth = widh;
        m_uHeight = height;

        // depends on m_uWidth and m_uHeight, so don't do this
        // until you have updated them (see above)
        int uNumTiles = GetNumTilesX()*GetNumTilesY();
        int uMaxNumElementsPerTile = GetMaxNumElementsPerTile();
        int uMaxNumVPLElementsPerTile = GetMaxNumVPLElementsPerTile();

        /*D3D11_BUFFER_DESC BufferDesc;
        ZeroMemory( &BufferDesc, sizeof(BufferDesc) );
        BufferDesc.Usage = D3D11_USAGE_DEFAULT;
        BufferDesc.ByteWidth = 2 * uMaxNumElementsPerTile * uNumTiles;
        BufferDesc.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS;
        V_RETURN( pd3dDevice->CreateBuffer( &BufferDesc, NULL, &m_pLightIndexBuffer ) );
        DXUT_SetDebugName( m_pLightIndexBuffer, "LightIndexBuffer" );*/

        m_pLightIndexBuffer = new BufferGL();
        m_pLightIndexBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, 2 * uMaxNumElementsPerTile * uNumTiles, null, GLenum.GL_STREAM_READ);
        m_pLightIndexBuffer.setName("LightIndexBuffer");

        /*V_RETURN( pd3dDevice->CreateBuffer( &BufferDesc, NULL, &m_pLightIndexBufferForBlendedObjects ) );
        DXUT_SetDebugName( m_pLightIndexBufferForBlendedObjects, "LightIndexBufferForBlendedObjects" );*/

        m_pLightIndexBufferForBlendedObjects = new BufferGL();
        m_pLightIndexBufferForBlendedObjects.initlize(GLenum.GL_UNIFORM_BUFFER, 2 * uMaxNumElementsPerTile * uNumTiles, null, GLenum.GL_STREAM_READ);
        m_pLightIndexBufferForBlendedObjects.setName("LightIndexBufferForBlendedObjects");

        /*V_RETURN( pd3dDevice->CreateBuffer( &BufferDesc, NULL, &m_pSpotIndexBuffer ) );
        DXUT_SetDebugName( m_pSpotIndexBuffer, "SpotIndexBuffer" );*/

        m_pSpotIndexBuffer = new BufferGL();
        m_pSpotIndexBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, 2 * uMaxNumElementsPerTile * uNumTiles, null, GLenum.GL_STREAM_READ);
        m_pSpotIndexBuffer.setName("SpotIndexBuffer");

        /*V_RETURN( pd3dDevice->CreateBuffer( &BufferDesc, NULL, &m_pSpotIndexBufferForBlendedObjects ) );
        DXUT_SetDebugName( m_pSpotIndexBufferForBlendedObjects, "SpotIndexBufferForBlendedObjects" );*/

        m_pSpotIndexBufferForBlendedObjects = new BufferGL();
        m_pSpotIndexBufferForBlendedObjects.initlize(GLenum.GL_UNIFORM_BUFFER, 2 * uMaxNumElementsPerTile * uNumTiles, null, GLenum.GL_STREAM_READ);
        m_pSpotIndexBufferForBlendedObjects.setName("SpotIndexBufferForBlendedObjects");

        /*BufferDesc.ByteWidth = 2 * uMaxNumVPLElementsPerTile * uNumTiles;
        V_RETURN( pd3dDevice->CreateBuffer( &BufferDesc, NULL, &m_pVPLIndexBuffer ) );
        DXUT_SetDebugName( m_pVPLIndexBuffer, "VPLIndexBuffer" );*/

        m_pVPLIndexBuffer = new BufferGL();
        m_pVPLIndexBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, 2 * uMaxNumVPLElementsPerTile * uNumTiles, null, GLenum.GL_STREAM_READ);
        m_pVPLIndexBuffer.setName("VPLIndexBuffer");

        /*D3D11_SHADER_RESOURCE_VIEW_DESC SRVDesc;
        ZeroMemory( &SRVDesc, sizeof( D3D11_SHADER_RESOURCE_VIEW_DESC ) );
        SRVDesc.Format = DXGI_FORMAT_R16_UINT;
        SRVDesc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
        SRVDesc.Buffer.ElementOffset = 0;
        SRVDesc.Buffer.ElementWidth = uMaxNumElementsPerTile * uNumTiles;
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pLightIndexBuffer, &SRVDesc, &m_pLightIndexBufferSRV ) );
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pLightIndexBufferForBlendedObjects, &SRVDesc, &m_pLightIndexBufferForBlendedObjectsSRV ) );*/

        m_pLightIndexBufferSRV = m_pLightIndexBuffer;
        m_pLightIndexBufferForBlendedObjectsSRV = m_pLightIndexBufferForBlendedObjects;

        /*V_RETURN( pd3dDevice->CreateShaderResourceView( m_pSpotIndexBuffer, &SRVDesc, &m_pSpotIndexBufferSRV ) );
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pSpotIndexBufferForBlendedObjects, &SRVDesc, &m_pSpotIndexBufferForBlendedObjectsSRV ) );*/

        m_pSpotIndexBufferSRV = m_pSpotIndexBuffer;
        m_pSpotIndexBufferForBlendedObjectsSRV = m_pSpotIndexBufferForBlendedObjects;

        /*SRVDesc.Buffer.ElementWidth = uMaxNumVPLElementsPerTile * uNumTiles;
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pVPLIndexBuffer, &SRVDesc, &m_pVPLIndexBufferSRV ) );*/
        m_pVPLIndexBufferSRV = m_pVPLIndexBuffer;

        /*D3D11_UNORDERED_ACCESS_VIEW_DESC UAVDesc;
        ZeroMemory( &UAVDesc, sizeof( D3D11_UNORDERED_ACCESS_VIEW_DESC ) );
        UAVDesc.Format = DXGI_FORMAT_R16_UINT;
        UAVDesc.ViewDimension = D3D11_UAV_DIMENSION_BUFFER;
        UAVDesc.Buffer.FirstElement = 0;
        UAVDesc.Buffer.NumElements = uMaxNumElementsPerTile * uNumTiles;
        V_RETURN( pd3dDevice->CreateUnorderedAccessView( m_pLightIndexBuffer, &UAVDesc, &m_pLightIndexBufferUAV ) );
        V_RETURN( pd3dDevice->CreateUnorderedAccessView( m_pLightIndexBufferForBlendedObjects, &UAVDesc, &m_pLightIndexBufferForBlendedObjectsUAV ) );*/

        m_pLightIndexBufferUAV = m_pLightIndexBuffer;
        m_pLightIndexBufferForBlendedObjectsUAV = m_pLightIndexBufferForBlendedObjects;

        /*V_RETURN( pd3dDevice->CreateUnorderedAccessView( m_pSpotIndexBuffer, &UAVDesc, &m_pSpotIndexBufferUAV ) );
        V_RETURN( pd3dDevice->CreateUnorderedAccessView( m_pSpotIndexBufferForBlendedObjects, &UAVDesc, &m_pSpotIndexBufferForBlendedObjectsUAV ) );*/

        m_pSpotIndexBufferUAV = m_pSpotIndexBuffer;
        m_pSpotIndexBufferForBlendedObjectsUAV = m_pSpotIndexBufferForBlendedObjects;

        /*UAVDesc.Buffer.NumElements = uMaxNumVPLElementsPerTile * uNumTiles;
        V_RETURN( pd3dDevice->CreateUnorderedAccessView( m_pVPLIndexBuffer, &UAVDesc, &m_pVPLIndexBufferUAV ) );*/

        m_pVPLIndexBufferUAV = m_pVPLIndexBuffer;

        // initialize the vertex buffer data for a quad (for drawing the lights-per-tile legend)
        final float kTextureHeight = (float)g_nLegendNumLines * (float)nLineHeight;
        final float kTextureWidth = (float)g_nLegendTextureWidth;
        final float kPaddingLeft = (float)g_nLegendPaddingLeft;
        final float kPaddingBottom = (float)g_nLegendPaddingBottom;
        float fLeft = kPaddingLeft;
        float fRight = kPaddingLeft + kTextureWidth;
        float fTop = (float)m_uHeight - kPaddingBottom - kTextureHeight;
        float fBottom =(float)m_uHeight - kPaddingBottom;
        g_QuadForLegendVertexData[0].v3Pos.set( fLeft,  fBottom, 0.0f );
        g_QuadForLegendVertexData[0].v2TexCoord.set( 0.0f, 0.0f );
        g_QuadForLegendVertexData[1].v3Pos.set( fLeft,  fTop, 0.0f );
        g_QuadForLegendVertexData[1].v2TexCoord.set( 0.0f, 1.0f );
        g_QuadForLegendVertexData[2].v3Pos.set( fRight, fBottom, 0.0f );
        g_QuadForLegendVertexData[2].v2TexCoord.set( 1.0f, 0.0f );
        g_QuadForLegendVertexData[3].v3Pos.set( fLeft,  fTop, 0.0f );
        g_QuadForLegendVertexData[3].v2TexCoord.set( 0.0f, 1.0f );
        g_QuadForLegendVertexData[4].v3Pos.set( fRight,  fTop, 0.0f );
        g_QuadForLegendVertexData[4].v2TexCoord.set( 1.0f, 1.0f );
        g_QuadForLegendVertexData[5].v3Pos.set( fRight, fBottom, 0.0f );
        g_QuadForLegendVertexData[5].v2TexCoord.set( 1.0f, 0.0f );

        // Create the vertex buffer for the sprite (a single quad)
        /*D3D11_SUBRESOURCE_DATA InitData;
        D3D11_BUFFER_DESC VBDesc;
        ZeroMemory( &VBDesc, sizeof(VBDesc) );
        VBDesc.Usage = D3D11_USAGE_IMMUTABLE;
        VBDesc.ByteWidth = sizeof( g_QuadForLegendVertexData );
        VBDesc.BindFlags = D3D11_BIND_VERTEX_BUFFER;
        InitData.pSysMem = g_QuadForLegendVertexData;
        V_RETURN( pd3dDevice->CreateBuffer( &VBDesc, &InitData, &m_pQuadForLegendVB ) );
        DXUT_SetDebugName( m_pQuadForLegendVB, "QuadForLegendVB" );*/

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(g_QuadForLegendVertexData.length * CommonUtilSpriteVertex.SIZE );
        CacheBuffer.put(buffer, g_QuadForLegendVertexData);
        buffer.flip();
        m_pQuadForLegendVB = new BufferGL();
        m_pQuadForLegendVB.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pQuadForLegendVB.setName("QuadForLegendVB");
    }
    void OnReleasingSwapChain(){
        SAFE_RELEASE(m_pLightIndexBuffer);
        SAFE_RELEASE(m_pLightIndexBufferSRV);
        SAFE_RELEASE(m_pLightIndexBufferUAV);
        SAFE_RELEASE(m_pLightIndexBufferForBlendedObjects);
        SAFE_RELEASE(m_pLightIndexBufferForBlendedObjectsSRV);
        SAFE_RELEASE(m_pLightIndexBufferForBlendedObjectsUAV);
        SAFE_RELEASE(m_pSpotIndexBuffer);
        SAFE_RELEASE(m_pSpotIndexBufferSRV);
        SAFE_RELEASE(m_pSpotIndexBufferUAV);
        SAFE_RELEASE(m_pSpotIndexBufferForBlendedObjects);
        SAFE_RELEASE(m_pSpotIndexBufferForBlendedObjectsSRV);
        SAFE_RELEASE(m_pSpotIndexBufferForBlendedObjectsUAV);
        SAFE_RELEASE(m_pVPLIndexBuffer);
        SAFE_RELEASE(m_pVPLIndexBufferSRV);
        SAFE_RELEASE(m_pVPLIndexBufferUAV);
        SAFE_RELEASE(m_pQuadForLegendVB);
    }

    /** Calculate the number of tiles in the horizontal direction */
    int GetNumTilesX() { return (int)( ( m_uWidth + TILE_RES - 1 ) / (float)TILE_RES );}
    /** Calculate the number of tiles in the vertical direction */
    int GetNumTilesY() { return (int)( ( m_uHeight + TILE_RES - 1 ) / (float)TILE_RES );}

    /**
     * Adjust max number of lights per tile based on screen height.
     * This assumes that the demo has a constant vertical field of view (fovy).<p></p>
     *
     * Note that the light culling tile size stays fixed as screen size changes.
     * With a constant fovy, reducing the screen height shrinks the projected
     * view of the scene, and so more lights can fall into our fixed tile size.<p></p>
     *
     * This function reduces the max lights per tile as screen height increases,
     * to save memory. It was tuned for this particular demo and is not intended
     * as a general solution for all scenes.
     * @return
     */
    int GetMaxNumLightsPerTile(){
        final int kAdjustmentMultipier = 16;

        // I haven't tested at greater than 1080p, so cap it
        int uHeight = (m_uHeight > 1080) ? 1080 : m_uHeight;

        // adjust max lights per tile down as height increases
        return ( MAX_NUM_LIGHTS_PER_TILE - ( kAdjustmentMultipier * ( uHeight / 120 ) ) );
    }

    int GetMaxNumElementsPerTile(){
        // max num lights times 2 (because the halfZ method has two lists per tile, list A and B),
        // plus two more to store the 32-bit halfZ, plus one more for the light count of list A,
        // plus one more for the light count of list B
        return (2*GetMaxNumLightsPerTile() + 4);
    }

    /**
     * Adjust max number of lights per tile based on screen height.
     * This assumes that the demo has a constant vertical field of view (fovy).<p></p>
     *
     * Note that the light culling tile size stays fixed as screen size changes.
     * With a constant fovy, reducing the screen height shrinks the projected
     * view of the scene, and so more lights can fall into our fixed tile size.<p></p>
     *
     * This function reduces the max lights per tile as screen height increases,
     * to save memory. It was tuned for this particular demo and is not intended
     * as a general solution for all scenes.
     * @return
     */
    int GetMaxNumVPLsPerTile() {
        final int kAdjustmentMultipier = 8;

        // I haven't tested at greater than 1080p, so cap it
        int uHeight = (m_uHeight > 1080) ? 1080 : m_uHeight;

        // adjust max lights per tile down as height increases
        return ( MAX_NUM_VPLS_PER_TILE - ( kAdjustmentMultipier * ( uHeight / 120 ) ) );
    }

    int GetMaxNumVPLElementsPerTile(){
        // max num lights times 2 (because the halfZ method has two lists per tile, list A and B),
        // plus two more to store the 32-bit halfZ, plus one more for the light count of list A,
        // plus one more for the light count of list B
        return (2*GetMaxNumVPLsPerTile() + 4);
    }

    BufferGL GetLightIndexBufferSRVParam() { return m_pLightIndexBufferSRV; }
    BufferGL GetLightIndexBufferUAVParam()  { return m_pLightIndexBufferUAV; }

    BufferGL GetLightIndexBufferForBlendedObjectsSRVParam() { return m_pLightIndexBufferForBlendedObjectsSRV; }
    BufferGL GetLightIndexBufferForBlendedObjectsUAVParam() { return m_pLightIndexBufferForBlendedObjectsUAV; }

    BufferGL GetSpotIndexBufferSRVParam()  { return m_pSpotIndexBufferSRV; }
    BufferGL GetSpotIndexBufferUAVParam()  { return m_pSpotIndexBufferUAV; }

    BufferGL GetSpotIndexBufferForBlendedObjectsSRVParam()  { return m_pSpotIndexBufferForBlendedObjectsSRV; }
    BufferGL GetSpotIndexBufferForBlendedObjectsUAVParam()  { return m_pSpotIndexBufferForBlendedObjectsUAV; }

    BufferGL GetVPLIndexBufferSRVParam()  { return m_pVPLIndexBufferSRV; }
    BufferGL GetVPLIndexBufferUAVParam()  { return m_pVPLIndexBufferUAV; }

    void DrawGrid(int nGridNumber, int nTriangleDensity, boolean bWithTextures /*= true*/) {
        // clamp nGridNumber
        nGridNumber = (nGridNumber < 0) ? 0 : nGridNumber;
        nGridNumber = (nGridNumber > MAX_NUM_GRID_OBJECTS-1) ? MAX_NUM_GRID_OBJECTS-1 : nGridNumber;

        // clamp nTriangleDensity
        nTriangleDensity = (nTriangleDensity < 0) ? 0 : nTriangleDensity;
        nTriangleDensity = (nTriangleDensity > TRIANGLE_DENSITY_NUM_TYPES-1) ? TRIANGLE_DENSITY_NUM_TYPES-1 : nTriangleDensity;

        BufferGL[] pGridVB = m_pGridVB[nTriangleDensity];
        BufferGL pGridIB = m_pGridIB[nTriangleDensity];


        // Set vertex buffer
        /*UINT uStride = sizeof( CommonUtilGridVertex );
        UINT uOffset = 0;
        pd3dImmediateContext->IASetVertexBuffers( 0, 1, &pGridVB[nGridNumber], &uStride, &uOffset );
        pd3dImmediateContext->IASetIndexBuffer( pGridIB, DXGI_FORMAT_R16_UINT, 0 );*/
        pGridVB[nGridNumber].bind();
        // todo InputLayout
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, pGridIB.getBuffer());

        // Set primitive topology
//        pd3dImmediateContext->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST );

        if( bWithTextures )
        {
            /*pd3dImmediateContext->PSSetShaderResources( 0, 1, &m_pGridDiffuseTextureSRV );
            pd3dImmediateContext->PSSetShaderResources( 1, 1, &m_pGridNormalMapSRV );*/
            // todo binding textures.
        }

//        pd3dImmediateContext->DrawIndexed( g_nNumGridIndices[nTriangleDensity], 0, 0 );
        gl.glDrawElements(GLenum.GL_TRIANGLES, g_nNumGridIndices[nTriangleDensity], GLenum.GL_UNSIGNED_SHORT, 0);
    }

    /** Return one of the lights-per-tile visualization shaders, based on nDebugDrawType */
    ShaderProgram  GetDebugDrawNumLightsPerTilePS( int nDebugDrawType, boolean bVPLsEnabled, boolean bForTransparentObjects ){
        if ( ( nDebugDrawType != DEBUG_DRAW_RADAR_COLORS ) && ( nDebugDrawType != DEBUG_DRAW_GRAYSCALE ) )
        {
            return null;
        }

        if( ( nDebugDrawType == DEBUG_DRAW_RADAR_COLORS ) && bVPLsEnabled && !bForTransparentObjects)
        {
            return m_pDebugDrawNumLightsAndVPLsPerTileRadarColorsPS;
        }
        else if( ( nDebugDrawType == DEBUG_DRAW_RADAR_COLORS ) && !bVPLsEnabled )
        {
            return m_pDebugDrawNumLightsPerTileRadarColorsPS;
        }
        else if( ( nDebugDrawType == DEBUG_DRAW_GRAYSCALE ) && bVPLsEnabled && !bForTransparentObjects )
        {
            return m_pDebugDrawNumLightsAndVPLsPerTileGrayscalePS;
        }
        else if( ( nDebugDrawType == DEBUG_DRAW_GRAYSCALE ) && !bVPLsEnabled )
        {
            return m_pDebugDrawNumLightsPerTileGrayscalePS;
        }
        else if( ( nDebugDrawType == DEBUG_DRAW_RADAR_COLORS ) && bVPLsEnabled && bForTransparentObjects )
        {
            return m_pDebugDrawNumLightsPerTileForTransparencyWithVPLsEnabledRadarColorsPS;
        }
        else if( ( nDebugDrawType == DEBUG_DRAW_GRAYSCALE ) && bVPLsEnabled && bForTransparentObjects )
        {
            return m_pDebugDrawNumLightsPerTileForTransparencyWithVPLsEnabledGrayscalePS;
        }
        else
        {
            // default
            return m_pDebugDrawNumLightsPerTileGrayscalePS;
        }
    }

    ShaderProgram  GetFullScreenVS()  { return m_pFullScreenVS; }

    /** Return one of the full-screen pixel shaders, based on MSAA settings */
    ShaderProgram  GetFullScreenPS( int uMSAASampleCount ) {
        // sanity check
        assert(NUM_FULL_SCREEN_PIXEL_SHADERS == NUM_MSAA_SETTINGS);

        switch( uMSAASampleCount )
        {
            case 1: return m_pFullScreenPS[MSAA_SETTING_NO_MSAA];
            case 2: return m_pFullScreenPS[MSAA_SETTING_2X_MSAA];
            case 4: return m_pFullScreenPS[MSAA_SETTING_4X_MSAA];
            default: assert(false); break;
        }

        return null;
    }

    /** Return one of the light culling compute shaders, based on MSAA settings */
    GLSLProgram GetLightCullCSForBlendedObjects( int uMSAASampleCount){
        // sanity check
        assert(NUM_LIGHT_CULLING_COMPUTE_SHADERS_FOR_BLENDED_OBJECTS == NUM_MSAA_SETTINGS);

        switch( uMSAASampleCount )
        {
            case 1: return m_pLightCullCSForBlendedObjects[MSAA_SETTING_NO_MSAA];
            case 2: return m_pLightCullCSForBlendedObjects[MSAA_SETTING_2X_MSAA];
            case 4: return m_pLightCullCSForBlendedObjects[MSAA_SETTING_4X_MSAA];
            default: assert(false); break;
        }

        return null;
    }

    Runnable GetDepthStencilState( int nDepthStencilStateType )  { return m_pDepthStencilState[nDepthStencilStateType]; }
    Runnable GetRasterizerState( int nRasterizerStateType )  { return m_pRasterizerState[nRasterizerStateType]; }

    int GetSamplerStateParam( int nSamplerStateType )  { return m_pSamplerState[nSamplerStateType]; }

    @Override
    public void dispose() {
        SAFE_RELEASE(m_pLightIndexBuffer);
        SAFE_RELEASE(m_pLightIndexBufferSRV);
        SAFE_RELEASE(m_pLightIndexBufferUAV);
        SAFE_RELEASE(m_pLightIndexBufferForBlendedObjects);
        SAFE_RELEASE(m_pLightIndexBufferForBlendedObjectsSRV);
        SAFE_RELEASE(m_pLightIndexBufferForBlendedObjectsUAV);
        SAFE_RELEASE(m_pSpotIndexBuffer);
        SAFE_RELEASE(m_pSpotIndexBufferSRV);
        SAFE_RELEASE(m_pSpotIndexBufferUAV);
        SAFE_RELEASE(m_pSpotIndexBufferForBlendedObjects);
        SAFE_RELEASE(m_pSpotIndexBufferForBlendedObjectsSRV);
        SAFE_RELEASE(m_pSpotIndexBufferForBlendedObjectsUAV);
        SAFE_RELEASE(m_pVPLIndexBuffer);
        SAFE_RELEASE(m_pVPLIndexBufferSRV);
        SAFE_RELEASE(m_pVPLIndexBufferUAV);
        SAFE_RELEASE(m_pBlendedVB);
        SAFE_RELEASE(m_pBlendedIB);
        SAFE_RELEASE(m_pBlendedTransform);
        SAFE_RELEASE(m_pBlendedTransformSRV);
        SAFE_RELEASE(m_pGridDiffuseTextureSRV);
        SAFE_RELEASE(m_pGridNormalMapSRV);
        SAFE_RELEASE(m_pQuadForLegendVB);
        SAFE_RELEASE(m_pSceneBlendedVS);
        SAFE_RELEASE(m_pSceneBlendedDepthVS);
        SAFE_RELEASE(m_pSceneBlendedPS);
        SAFE_RELEASE(m_pSceneBlendedPSShadows);
//        SAFE_RELEASE(m_pSceneBlendedLayout);
//        SAFE_RELEASE(m_pSceneBlendedDepthLayout);
        SAFE_RELEASE(m_pDebugDrawNumLightsPerTileRadarColorsPS);
        SAFE_RELEASE(m_pDebugDrawNumLightsPerTileGrayscalePS);
        SAFE_RELEASE(m_pDebugDrawNumLightsAndVPLsPerTileRadarColorsPS);
        SAFE_RELEASE(m_pDebugDrawNumLightsAndVPLsPerTileGrayscalePS);
        SAFE_RELEASE(m_pDebugDrawNumLightsPerTileForTransparencyWithVPLsEnabledRadarColorsPS);
        SAFE_RELEASE(m_pDebugDrawNumLightsPerTileForTransparencyWithVPLsEnabledGrayscalePS);
        SAFE_RELEASE(m_pDebugDrawLegendForNumLightsPerTileVS);
        SAFE_RELEASE(m_pDebugDrawLegendForNumLightsPerTileRadarColorsPS);
        SAFE_RELEASE(m_pDebugDrawLegendForNumLightsPerTileGrayscalePS);
//        SAFE_RELEASE(m_pDebugDrawLegendForNumLightsLayout11);
        SAFE_RELEASE(m_pFullScreenVS);

        for( int i = 0; i < TRIANGLE_DENSITY_NUM_TYPES; i++ )
        {
            for( int j = 0; j < MAX_NUM_GRID_OBJECTS; j++ )
            {
                SAFE_RELEASE(m_pGridVB[i][j]);
            }

            SAFE_RELEASE(m_pGridIB[i]);
        }

        for( int i = 0; i < NUM_LIGHT_CULLING_COMPUTE_SHADERS_FOR_BLENDED_OBJECTS; i++ )
        {
            SAFE_RELEASE(m_pLightCullCSForBlendedObjects[i]);
        }

        for( int i = 0; i < NUM_FULL_SCREEN_PIXEL_SHADERS; i++ )
        {
            SAFE_RELEASE(m_pFullScreenPS[i]);
        }

        for( int i = 0; i < DEPTH_STENCIL_STATE_NUM_TYPES; i++ )
        {
//            SAFE_RELEASE(m_pDepthStencilState[i]);
        }

        for( int i = 0; i < RASTERIZER_STATE_NUM_TYPES; i++ )
        {
//            SAFE_RELEASE(m_pRasterizerState[i]);
        }

        for( int i = 0; i < SAMPLER_STATE_NUM_TYPES; i++ )
        {
//            SAFE_RELEASE(m_pSamplerState[i]);
        }
    }
}
