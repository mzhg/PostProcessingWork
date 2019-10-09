package jet.opengl.demos.amdfx.tiledrendering;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Random;

import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLProgramPipeline;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

final class LightUtil implements Disposeable, ICONST {
    static final int
            LIGHTING_SHADOWS = 0,
            LIGHTING_RANDOM = 1,
            LIGHTING_NUM_MODES = 2;

    // point lights
    private BufferGL m_pPointLightBufferCenterAndRadius;
    private BufferGL m_pPointLightBufferCenterAndRadiusSRV;
    private BufferGL m_pPointLightBufferColor;
    private BufferGL m_pPointLightBufferColorSRV;

    // shadow casting point lights
    private BufferGL m_pShadowCastingPointLightBufferCenterAndRadius;
    private BufferGL m_pShadowCastingPointLightBufferCenterAndRadiusSRV;
    private BufferGL m_pShadowCastingPointLightBufferColor;
    private BufferGL m_pShadowCastingPointLightBufferColorSRV;

    // spot lights
    private BufferGL m_pSpotLightBufferCenterAndRadius;
    private BufferGL m_pSpotLightBufferCenterAndRadiusSRV;
    private BufferGL m_pSpotLightBufferColor;
    private BufferGL m_pSpotLightBufferColorSRV;
    private BufferGL m_pSpotLightBufferSpotParams;
    private BufferGL m_pSpotLightBufferSpotParamsSRV;

    // these are only used for debug drawing the spot lights
    private BufferGL m_pSpotLightBufferSpotMatrices;
    private BufferGL m_pSpotLightBufferSpotMatricesSRV;

    // spot lights
    private BufferGL m_pShadowCastingSpotLightBufferCenterAndRadius;
    private BufferGL m_pShadowCastingSpotLightBufferCenterAndRadiusSRV;
    private BufferGL m_pShadowCastingSpotLightBufferColor;
    private BufferGL m_pShadowCastingSpotLightBufferColorSRV;
    private BufferGL m_pShadowCastingSpotLightBufferSpotParams;
    private BufferGL m_pShadowCastingSpotLightBufferSpotParamsSRV;

    // these are only used for debug drawing the spot lights
    private BufferGL m_pShadowCastingSpotLightBufferSpotMatrices;
    private BufferGL m_pShadowCastingSpotLightBufferSpotMatricesSRV;

    // sprite quad VB (for debug drawing the point lights)
    private BufferGL m_pQuadForLightsVB;

    // cone VB and IB (for debug drawing the spot lights)
    private BufferGL m_pConeForSpotLightsVB;
    private BufferGL m_pConeForSpotLightsIB;

    // debug draw shaders for the point lights
    private ShaderProgram m_pDebugDrawPointLightsVS;
    private ShaderProgram m_pDebugDrawPointLightsPS;
    private ID3D11InputLayout m_pDebugDrawPointLightsLayout11;

    // debug draw shaders for the spot lights
    private ShaderProgram m_pDebugDrawSpotLightsVS;
    private ShaderProgram m_pDebugDrawSpotLightsPS;
    private ID3D11InputLayout m_pDebugDrawSpotLightsLayout11;

    // state
    private Runnable m_pBlendStateAdditive;
    private GLFuncProvider gl;
    private GLSLProgramPipeline m_ShaderCombine;

    private static final class LightUtilSpriteVertex implements Readable {
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
    private final static LightUtilSpriteVertex[] g_QuadForLightsVertexData = new LightUtilSpriteVertex[6];

    // static arrays for the point light data
    private final static Vector4f[] g_PointLightDataArrayCenterAndRadius = new Vector4f[MAX_NUM_LIGHTS];
    private final static int[]      g_PointLightDataArrayColor = new int[MAX_NUM_LIGHTS];

    private final static Vector4f[] g_ShadowCastingPointLightDataArrayCenterAndRadius = new Vector4f[MAX_NUM_SHADOWCASTING_POINTS];
    private final static int[]      g_ShadowCastingPointLightDataArrayColor = new int[MAX_NUM_SHADOWCASTING_POINTS];

    private final static Matrix4f[][] g_ShadowCastingPointLightViewProjTransposed = new Matrix4f[MAX_NUM_SHADOWCASTING_POINTS][6];
    private final static Matrix4f[][] g_ShadowCastingPointLightViewProjInvTransposed = new Matrix4f[MAX_NUM_SHADOWCASTING_POINTS][6];

    static {
        for(int i = 0; i < g_QuadForLightsVertexData.length; i++) g_QuadForLightsVertexData[i] = new LightUtilSpriteVertex();
    }

    private static final class LightUtilConeVertex implements Readable
    {
        static final int SIZE = Vector3f.SIZE * 2 + Vector2f.SIZE;
        final Vector3f v3Pos = new Vector3f();
        final Vector3f v3Norm = new Vector3f();
        final Vector2f v2TexCoord = new Vector2f();

        @Override
        public ByteBuffer store(ByteBuffer buf) {
            v3Pos.store(buf);
            v3Norm.store(buf);
            v2TexCoord.store(buf);
            return buf;
        }
    };

// static arrays for cone vertex and index data (for visualizing spot lights)
    private static final int            g_nConeNumTris = 90;
    private static final int            g_nConeNumVertices = 2*g_nConeNumTris;
    private static final int            g_nConeNumIndices = 3*g_nConeNumTris;
    private final static LightUtilConeVertex[]  g_ConeForSpotLightsVertexData = new LightUtilConeVertex[g_nConeNumVertices];
    private static final short[]        g_ConeForSpotLightsIndexData = new short[g_nConeNumIndices];

    static {
        for(int i = 0; i<g_ConeForSpotLightsVertexData.length; i++)
            g_ConeForSpotLightsVertexData[i] = new LightUtilConeVertex();
    }

    // these are half-precision (i.e. 16-bit) float values,
// stored as unsigned shorts
    private static final class LightUtilSpotParams implements Readable
    {
        static final int SIZE = 8;
        short fLightDirX;
        short fLightDirY;
        short fCosineOfConeAngleAndLightDirZSign;
        short fFalloffRadius;

        @Override
        public ByteBuffer store(ByteBuffer buf) {
            buf.putShort(fLightDirX);
            buf.putShort(fLightDirY);
            buf.putShort(fCosineOfConeAngleAndLightDirZSign);
            buf.putShort(fFalloffRadius);
            return buf;
        }
    };

    // static arrays for the spot light data
    private final static Vector4f[] g_SpotLightDataArrayCenterAndRadius = new Vector4f[MAX_NUM_LIGHTS];
    private final static int[]      g_SpotLightDataArrayColor = new int[MAX_NUM_LIGHTS];
    private final static LightUtilSpotParams[]  g_SpotLightDataArraySpotParams = new LightUtilSpotParams[MAX_NUM_LIGHTS];

    private final static Vector4f[] g_ShadowCastingSpotLightDataArrayCenterAndRadius = new Vector4f[MAX_NUM_SHADOWCASTING_SPOTS];
    private final static int[]      g_ShadowCastingSpotLightDataArrayColor = new int[MAX_NUM_SHADOWCASTING_SPOTS];
    private final static LightUtilSpotParams[]  g_ShadowCastingSpotLightDataArraySpotParams = new LightUtilSpotParams[MAX_NUM_SHADOWCASTING_SPOTS];

    // rotation matrices used when visualizing the spot lights
    private final static Matrix4f[] g_SpotLightDataArraySpotMatrices = new Matrix4f[MAX_NUM_LIGHTS];
    private final static Matrix4f[] g_ShadowCastingSpotLightDataArraySpotMatrices = new Matrix4f[MAX_NUM_SHADOWCASTING_SPOTS];

    private final static Matrix4f[] g_ShadowCastingSpotLightViewProjTransposed = new Matrix4f[MAX_NUM_SHADOWCASTING_SPOTS];
    private final static Matrix4f[] g_ShadowCastingSpotLightViewProjInvTransposed = new Matrix4f[MAX_NUM_SHADOWCASTING_SPOTS];

// miscellaneous constants
    private final static float TWO_PI = 6.28318530718f;

    // there should only be one LightUtil object
    private final static int LightUtilObjectCounter = 0;
    private static int uCounter = 0;
    private static int GetRandColor()
    {
        uCounter++;

        Vector3f Color = new Vector3f();
        if( uCounter%2 == 0 )
        {
            // since green contributes the most to perceived brightness,
            // cap it's min value to avoid overly dim lights
            Color.set(Numeric.random(0.0f,1.0f),Numeric.random(0.27f,1.0f),Numeric.random(0.0f,1.0f));
        }
        else
        {
            // else ensure the red component has a large value, again
            // to avoid overly dim lights
            Color.set(Numeric.random(0.9f,1.0f),Numeric.random(0.0f,1.0f),Numeric.random(0.0f,1.0f));
        }

        int dwR = (int)(Color.x * 255.0f + 0.5f);
        int dwG = (int)(Color.y * 255.0f + 0.5f);
        int dwB = (int)(Color.z * 255.0f + 0.5f);

        return Numeric.makeRGBA(dwR, dwG, dwB, 255);
    }

    private static Vector3f GetRandLightDirection()
    {
//        static unsigned uCounter = 0;
        uCounter++;

        Vector3f vLightDir = new Vector3f();
        vLightDir.x = Numeric.random(-1.0f,1.0f);
        vLightDir.y = Numeric.random( 0.1f,1.0f);
        vLightDir.z = Numeric.random(-1.0f,1.0f);

        if( uCounter%2 == 0 )
        {
            vLightDir.y = -vLightDir.y;
        }

        /*XMFLOAT3 vResult;
        XMVECTOR NormalizedLightDir = XMVector3Normalize( XMLoadFloat3( &vLightDir) );
        XMStoreFloat3( &vResult, NormalizedLightDir );*/
        vLightDir.normalise();

        return vLightDir;
    }

    private static void PackSpotParams(LightUtilSpotParams PackedParams, Vector3f vLightDir, float fCosineOfConeAngle, float fFalloffRadius)
    {
        assert( fCosineOfConeAngle > 0.0f );
        assert( fFalloffRadius > 0.0f );

        PackedParams.fLightDirX = Numeric.convertFloatToHFloat( vLightDir.x );
        PackedParams.fLightDirY = Numeric.convertFloatToHFloat( vLightDir.y );
        PackedParams.fCosineOfConeAngleAndLightDirZSign = Numeric.convertFloatToHFloat( fCosineOfConeAngle );
        PackedParams.fFalloffRadius = Numeric.convertFloatToHFloat( fFalloffRadius );

        // put the sign bit for light dir z in the sign bit for the cone angle
        // (we can do this because we know the cone angle is always positive)
        if( vLightDir.z < 0.0f )
        {
            PackedParams.fCosineOfConeAngleAndLightDirZSign |= 0x8000;
        }
        else
        {
            PackedParams.fCosineOfConeAngleAndLightDirZSign &= 0x7FFF;
        }
    }

    private static void CalcPointLightView(ReadableVector3f PositionAndRadius, int nFace, Matrix4f View )
    {
        final Vector3f dir[  ] =
        {
            new Vector3f(  1.0f,  0.0f,  0.0f),
            new Vector3f( -1.0f,  0.0f,  0.0f),
            new Vector3f(  0.0f,  1.0f,  0.0f),
            new Vector3f(  0.0f, -1.0f,  0.0f),
            new Vector3f(  0.0f,  0.0f, -1.0f),
            new Vector3f(  0.0f,  0.0f,  1.0f),
        };

        final Vector3f up[  ] =
        {
                new Vector3f( 0.0f, 1.0f,  0.0f),
                new Vector3f( 0.0f, 1.0f,  0.0f),
                new Vector3f( 0.0f, 0.0f,  1.0f),
                new Vector3f( 0.0f, 0.0f, -1.0f ),
                new Vector3f( 0.0f, 1.0f,  0.0f ),
                new Vector3f( 0.0f, 1.0f,  0.0f),
        };

//        XMVECTOR eye = XMVectorSet( PositionAndRadius.x, PositionAndRadius.y, PositionAndRadius.z, 0.0f );

//        XMVECTOR at = eye + dir[ nFace ];
        Vector3f at = Vector3f.add(PositionAndRadius, dir[nFace], null);
//        View = XMMatrixLookAtLH( eye, at, up[ nFace ] );
        Matrix4f.lookAt(PositionAndRadius, at, up[nFace], View);
    }

    private static void CalcPointLightProj(ReadableVector4f PositionAndRadius, Matrix4f Proj )
    {
//        Proj = XMMatrixPerspectiveFovLH( XMConvertToRadians( 90.0f ), 1.0f, 2.0f, PositionAndRadius.w );
        Matrix4f.perspective(90, 1.0f, 2.0f, PositionAndRadius.getW(), Proj);
    }

    static void CalcPointLightViewProj( ReadableVector4f positionAndRadius, Matrix4f[] viewProjTransposed, Matrix4f[] viewProjInvTransposed )
    {
        Matrix4f proj = new Matrix4f();
        Matrix4f view = new Matrix4f();
        CalcPointLightProj( positionAndRadius, proj );
        for ( int i = 0; i < 6; i++ )
        {
            CalcPointLightView( positionAndRadius, i, view );
            /*XMMATRIX viewProj = view * proj;
            viewProjTransposed[i] = XMMatrixTranspose( viewProj );
            XMMATRIX viewProjInv = XMMatrixInverse( NULL, viewProj );
            viewProjInvTransposed[i] = XMMatrixTranspose( viewProjInv );*/
            viewProjTransposed[i] = Matrix4f.mul(proj, view, viewProjTransposed[i]);
            viewProjInvTransposed[i] = Matrix4f.invert(viewProjTransposed[i], viewProjInvTransposed[i]);
        }
    }

    private static int uShadowCastingPointLightCounter = 0;

    static void AddShadowCastingPointLight( ReadableVector4f positionAndRadius, int color )
    {

        assert( uShadowCastingPointLightCounter < MAX_NUM_SHADOWCASTING_POINTS );

        if(g_ShadowCastingPointLightDataArrayCenterAndRadius[ uShadowCastingPointLightCounter ] == null)
            g_ShadowCastingPointLightDataArrayCenterAndRadius[ uShadowCastingPointLightCounter ] = new Vector4f();

        g_ShadowCastingPointLightDataArrayCenterAndRadius[ uShadowCastingPointLightCounter ].set(positionAndRadius);
        g_ShadowCastingPointLightDataArrayColor[ uShadowCastingPointLightCounter ] = color;

        CalcPointLightViewProj( positionAndRadius, g_ShadowCastingPointLightViewProjTransposed[uShadowCastingPointLightCounter], g_ShadowCastingPointLightViewProjInvTransposed[uShadowCastingPointLightCounter] );

        uShadowCastingPointLightCounter++;
    }

    static void CalcSpotLightView( ReadableVector3f Eye, ReadableVector3f LookAt,Matrix4f View )
    {
        /*XMFLOAT3 Up( 0.0f, 1.0f, 0.0f );
        View = XMMatrixLookAtLH( XMLoadFloat3(&Eye), XMLoadFloat3(&LookAt), XMLoadFloat3(&Up) );*/
        Objects.requireNonNull(View);
        Matrix4f.lookAt(Eye, LookAt, Vector3f.Y_AXIS, View);
    }

    static void CalcSpotLightProj( float radius, Matrix4f Proj )
    {
//        Proj = XMMatrixPerspectiveFovLH( XMConvertToRadians( 70.52877936f ), 1.0f, 2.0f, radius );

        Objects.requireNonNull(Proj);
        Matrix4f.perspective(70.52877936f, 1.0f, 2.0f, radius, Proj);
    }

    static void CalcSpotLightViewProj( ReadableVector4f positionAndRadius, ReadableVector3f lookAt,
                                       Matrix4f viewProjTransposed, Matrix4f viewProjInvTransposed )
    {
        Matrix4f proj = viewProjTransposed;
        Matrix4f view = viewProjInvTransposed;
        CalcSpotLightProj( positionAndRadius.getW(), proj );
        CalcSpotLightView( positionAndRadius, lookAt, view );

        Matrix4f.mul(proj, view, viewProjTransposed);
        Matrix4f.invert(viewProjTransposed, viewProjInvTransposed);

        /*XMMATRIX viewProj = view * proj;
    *viewProjTransposed = XMMatrixTranspose( viewProj );
        XMMATRIX viewProjInv = XMMatrixInverse( NULL, viewProj );
    *viewProjInvTransposed = XMMatrixTranspose( viewProjInv );*/
    }

    private static int uShadowCastingSpotLightCounter = 0;
    static void AddShadowCastingSpotLight( ReadableVector4f positionAndRadius, ReadableVector3f lookAt, int color )
    {
        assert( uShadowCastingSpotLightCounter < MAX_NUM_SHADOWCASTING_SPOTS );

        /*XMVECTOR eye = XMLoadFloat3( (XMFLOAT3*)(&positionAndRadius) );
        XMVECTOR dir = XMLoadFloat3(&lookAt) - eye;
        dir = XMVector3Normalize( dir );
        XMFLOAT3 f3Dir;
        XMStoreFloat3(&f3Dir, dir);*/

        ReadableVector3f eye = positionAndRadius;
        Vector3f f3Dir = Vector3f.sub(lookAt, eye, null);
        f3Dir.normalise();

//        XMVECTOR boundingSpherePos = eye + (dir * positionAndRadius.w);
        Vector3f boundingSpherePos = Vector3f.linear(eye, f3Dir, positionAndRadius.getW(), null);

        if(g_ShadowCastingSpotLightDataArrayCenterAndRadius[ uShadowCastingSpotLightCounter ] == null)
            g_ShadowCastingSpotLightDataArrayCenterAndRadius[ uShadowCastingSpotLightCounter ] = new Vector4f();
        g_ShadowCastingSpotLightDataArrayCenterAndRadius[ uShadowCastingSpotLightCounter ].set( boundingSpherePos, positionAndRadius.getW() );
        g_ShadowCastingSpotLightDataArrayColor[ uShadowCastingSpotLightCounter ] = color;

        // cosine of cone angle is cosine(35.26438968 degrees) = 0.816496580927726
        if(g_ShadowCastingSpotLightDataArraySpotParams[ uShadowCastingSpotLightCounter ] == null)
            g_ShadowCastingSpotLightDataArraySpotParams[ uShadowCastingSpotLightCounter ] = new LightUtilSpotParams();
        PackSpotParams(g_ShadowCastingSpotLightDataArraySpotParams[ uShadowCastingSpotLightCounter ], f3Dir, 0.816496580927726f, positionAndRadius.getW() * 1.33333333f );

        if(g_ShadowCastingSpotLightViewProjTransposed[uShadowCastingSpotLightCounter] == null)
            g_ShadowCastingSpotLightViewProjTransposed[uShadowCastingSpotLightCounter] = new Matrix4f();
        if(g_ShadowCastingSpotLightViewProjInvTransposed[uShadowCastingSpotLightCounter] == null)
            g_ShadowCastingSpotLightViewProjInvTransposed[uShadowCastingSpotLightCounter] = new Matrix4f();
        CalcSpotLightViewProj( positionAndRadius, lookAt, g_ShadowCastingSpotLightViewProjTransposed[uShadowCastingSpotLightCounter], g_ShadowCastingSpotLightViewProjInvTransposed[uShadowCastingSpotLightCounter] );

        // build a "rotate from one vector to another" matrix, to point the spot light
        // cone along its light direction
        /*XMVECTOR s = XMVectorSet(0.0f,-1.0f,0.0f,0.0f);
        XMVECTOR t = dir;
        XMFLOAT3 v;
        XMStoreFloat3( &v, XMVector3Cross(s,t) );*/

        float e = Vector3f.dot(Vector3f.Y_AXIS_NEG,f3Dir);
        Vector3f v= Vector3f.cross(Vector3f.Y_AXIS_NEG, f3Dir, f3Dir);

        float h = 1.0f / (1.0f + e);

        Matrix4f f4x4Rotation = g_ShadowCastingSpotLightDataArraySpotMatrices[ uShadowCastingSpotLightCounter ];
        if(f4x4Rotation == null)
            f4x4Rotation = new Matrix4f();

        f4x4Rotation.m00 = e + h*v.x*v.x;
        f4x4Rotation.m01 = h*v.x*v.y - v.z;
        f4x4Rotation.m02 = h*v.x*v.z + v.y;
        f4x4Rotation.m10 = h*v.x*v.y + v.z;
        f4x4Rotation.m11 = e + h*v.y*v.y;
        f4x4Rotation.m12 = h*v.y*v.z - v.x;
        f4x4Rotation.m20 = h*v.x*v.z - v.y;
        f4x4Rotation.m21 = h*v.y*v.z + v.x;
        f4x4Rotation.m22 = e + h*v.z*v.z;
        f4x4Rotation.m30  = 0;
        f4x4Rotation.m31  = 0;
        f4x4Rotation.m32  = 0;
        f4x4Rotation.m33  = 1;
//        XMMATRIX mRotation = XMLoadFloat4x4( &f4x4Rotation );

        g_ShadowCastingSpotLightDataArraySpotMatrices[ uShadowCastingSpotLightCounter ] = f4x4Rotation;
        uShadowCastingSpotLightCounter++;
    }

    // Various hook functions
    void OnCreateDevice( /*ID3D11Device* pd3dDevice*/ ){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_ShaderCombine = new GLSLProgramPipeline();
        // Create the point light buffer (center and radius)
        /*D3D11_BUFFER_DESC LightBufferDesc;
        ZeroMemory( &LightBufferDesc, sizeof(LightBufferDesc) );
        LightBufferDesc.Usage = D3D11_USAGE_IMMUTABLE;
        LightBufferDesc.ByteWidth = sizeof( g_PointLightDataArrayCenterAndRadius );
        LightBufferDesc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
        InitData.pSysMem = g_PointLightDataArrayCenterAndRadius;
        V_RETURN( pd3dDevice->CreateBuffer( &LightBufferDesc, &InitData, &m_pPointLightBufferCenterAndRadius ) );
        DXUT_SetDebugName( m_pPointLightBufferCenterAndRadius, "PointLightBufferCenterAndRadius" );*/

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(g_PointLightDataArrayCenterAndRadius.length * Vector4f.SIZE);
        CacheBuffer.put(buffer, g_PointLightDataArrayCenterAndRadius);
        buffer.flip();

        m_pPointLightBufferCenterAndRadius = new BufferGL();
        m_pPointLightBufferCenterAndRadius.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pPointLightBufferCenterAndRadius.setName("PointLightBufferCenterAndRadius");

        /*D3D11_SHADER_RESOURCE_VIEW_DESC SRVDesc;
        ZeroMemory( &SRVDesc, sizeof( D3D11_SHADER_RESOURCE_VIEW_DESC ) );
        SRVDesc.Format = DXGI_FORMAT_R32G32B32A32_FLOAT;
        SRVDesc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
        SRVDesc.Buffer.ElementOffset = 0;
        SRVDesc.Buffer.ElementWidth = MAX_NUM_LIGHTS;
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pPointLightBufferCenterAndRadius, &SRVDesc, &m_pPointLightBufferCenterAndRadiusSRV ) );*/

        m_pPointLightBufferCenterAndRadiusSRV = m_pPointLightBufferCenterAndRadius;

        // Create the point light buffer (color)
//        LightBufferDesc.ByteWidth = sizeof( g_PointLightDataArrayColor );
//        InitData.pSysMem = g_PointLightDataArrayColor;
//        V_RETURN( pd3dDevice->CreateBuffer( &LightBufferDesc, &InitData, &m_pPointLightBufferColor ) );
//        DXUT_SetDebugName( m_pPointLightBufferColor, "PointLightBufferColor" );

        buffer = CacheBuffer.getCachedByteBuffer(g_PointLightDataArrayColor.length * 4);
        CacheBuffer.put(buffer, g_PointLightDataArrayColor);
        buffer.flip();

        m_pPointLightBufferColor = new BufferGL();
        m_pPointLightBufferColor.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pPointLightBufferColor.setName("PointLightBufferColor");

        /*SRVDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pPointLightBufferColor, &SRVDesc, &m_pPointLightBufferColorSRV ) );*/
        m_pPointLightBufferColorSRV = m_pPointLightBufferColor;

        // Create the shadow-casting point light buffer (center and radius)
        /*LightBufferDesc.ByteWidth = sizeof( g_ShadowCastingPointLightDataArrayCenterAndRadius );
        InitData.pSysMem = g_ShadowCastingPointLightDataArrayCenterAndRadius;
        V_RETURN( pd3dDevice->CreateBuffer( &LightBufferDesc, &InitData, &m_pShadowCastingPointLightBufferCenterAndRadius ) );
        DXUT_SetDebugName( m_pShadowCastingPointLightBufferCenterAndRadius, "ShadowCastingPointLightBufferCenterAndRadius" );*/

        buffer = CacheBuffer.getCachedByteBuffer(g_ShadowCastingPointLightDataArrayCenterAndRadius.length * Vector4f.SIZE);
        CacheBuffer.put(buffer, g_ShadowCastingPointLightDataArrayCenterAndRadius);
        buffer.flip();

        m_pShadowCastingPointLightBufferCenterAndRadius = new BufferGL();
        m_pShadowCastingPointLightBufferCenterAndRadius.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pShadowCastingPointLightBufferCenterAndRadius.setName("ShadowCastingPointLightBufferCenterAndRadius");

//        SRVDesc.Format = DXGI_FORMAT_R32G32B32A32_FLOAT;
//        SRVDesc.Buffer.ElementWidth = MAX_NUM_SHADOWCASTING_POINTS;
//        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pShadowCastingPointLightBufferCenterAndRadius, &SRVDesc, &m_pShadowCastingPointLightBufferCenterAndRadiusSRV ) );
        m_pShadowCastingPointLightBufferCenterAndRadiusSRV = m_pShadowCastingPointLightBufferCenterAndRadius;

        // Create the shadow-casting point light buffer (color)
        /*LightBufferDesc.ByteWidth = sizeof( g_ShadowCastingPointLightDataArrayColor );
        InitData.pSysMem = g_ShadowCastingPointLightDataArrayColor;
        V_RETURN( pd3dDevice->CreateBuffer( &LightBufferDesc, &InitData, &m_pShadowCastingPointLightBufferColor ) );
        DXUT_SetDebugName( m_pShadowCastingPointLightBufferColor, "ShadowCastingPointLightBufferColor" );*/

        buffer = CacheBuffer.getCachedByteBuffer(g_ShadowCastingPointLightDataArrayColor.length * 4);
        CacheBuffer.put(buffer, g_ShadowCastingPointLightDataArrayColor);
        buffer.flip();

        m_pShadowCastingPointLightBufferColor = new BufferGL();
        m_pShadowCastingPointLightBufferColor.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pShadowCastingPointLightBufferColor.setName("ShadowCastingPointLightBufferColor");

        /*SRVDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pShadowCastingPointLightBufferColor, &SRVDesc, &m_pShadowCastingPointLightBufferColorSRV ) );*/
        m_pShadowCastingPointLightBufferColorSRV = m_pShadowCastingPointLightBufferColor;

        // Create the spot light buffer (center and radius)
        /*ZeroMemory( &LightBufferDesc, sizeof(LightBufferDesc) );
        LightBufferDesc.Usage = D3D11_USAGE_IMMUTABLE;
        LightBufferDesc.ByteWidth = sizeof( g_SpotLightDataArrayCenterAndRadius );
        LightBufferDesc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
        InitData.pSysMem = g_SpotLightDataArrayCenterAndRadius;
        V_RETURN( pd3dDevice->CreateBuffer( &LightBufferDesc, &InitData, &m_pSpotLightBufferCenterAndRadius ) );
        DXUT_SetDebugName( m_pSpotLightBufferCenterAndRadius, "SpotLightBufferCenterAndRadius" );*/

        buffer = CacheBuffer.getCachedByteBuffer(g_SpotLightDataArrayCenterAndRadius.length * Vector4f.SIZE);
        CacheBuffer.put(buffer, g_SpotLightDataArrayCenterAndRadius);
        buffer.flip();

        m_pSpotLightBufferCenterAndRadius = new BufferGL();
        m_pSpotLightBufferCenterAndRadius.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pSpotLightBufferCenterAndRadius.setName("SpotLightBufferCenterAndRadius");

        /*ZeroMemory( &SRVDesc, sizeof( D3D11_SHADER_RESOURCE_VIEW_DESC ) );
        SRVDesc.Format = DXGI_FORMAT_R32G32B32A32_FLOAT;
        SRVDesc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
        SRVDesc.Buffer.ElementOffset = 0;
        SRVDesc.Buffer.ElementWidth = MAX_NUM_LIGHTS;
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pSpotLightBufferCenterAndRadius, &SRVDesc, &m_pSpotLightBufferCenterAndRadiusSRV ) );*/

        m_pSpotLightBufferCenterAndRadiusSRV = m_pSpotLightBufferCenterAndRadius;

        // Create the spot light buffer (color)
        /*LightBufferDesc.ByteWidth = sizeof( g_SpotLightDataArrayColor );
        InitData.pSysMem = g_SpotLightDataArrayColor;
        V_RETURN( pd3dDevice->CreateBuffer( &LightBufferDesc, &InitData, &m_pSpotLightBufferColor ) );
        DXUT_SetDebugName( m_pSpotLightBufferColor, "SpotLightBufferColor" );*/

        buffer = CacheBuffer.getCachedByteBuffer(g_SpotLightDataArrayColor.length * 4);
        CacheBuffer.put(buffer, g_SpotLightDataArrayColor);
        buffer.flip();

        m_pSpotLightBufferColor = new BufferGL();
        m_pSpotLightBufferColor.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pSpotLightBufferColor.setName("SpotLightBufferColor");

        /*SRVDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pSpotLightBufferColor, &SRVDesc, &m_pSpotLightBufferColorSRV ) );*/

        m_pSpotLightBufferColorSRV = m_pSpotLightBufferColor;

        // Create the spot light buffer (spot light parameters)
        /*LightBufferDesc.ByteWidth = sizeof( g_SpotLightDataArraySpotParams );
        InitData.pSysMem = g_SpotLightDataArraySpotParams;
        V_RETURN( pd3dDevice->CreateBuffer( &LightBufferDesc, &InitData, &m_pSpotLightBufferSpotParams ) );
        DXUT_SetDebugName( m_pSpotLightBufferSpotParams, "SpotLightBufferSpotParams" );*/

        buffer = CacheBuffer.getCachedByteBuffer(g_SpotLightDataArraySpotParams.length * LightUtilSpotParams.SIZE);
        CacheBuffer.put(buffer, g_SpotLightDataArraySpotParams);
        buffer.flip();

        m_pSpotLightBufferSpotParams = new BufferGL();
        m_pSpotLightBufferSpotParams.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pSpotLightBufferSpotParams.setName("SpotLightBufferSpotParams");

        /*SRVDesc.Format = DXGI_FORMAT_R16G16B16A16_FLOAT;
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pSpotLightBufferSpotParams, &SRVDesc, &m_pSpotLightBufferSpotParamsSRV ) );*/
        m_pSpotLightBufferSpotParamsSRV = m_pSpotLightBufferSpotParams;

        // Create the light buffer (spot light matrices, only used for debug drawing the spot lights)
        /*LightBufferDesc.ByteWidth = sizeof( g_SpotLightDataArraySpotMatrices );
        InitData.pSysMem = g_SpotLightDataArraySpotMatrices;
        V_RETURN( pd3dDevice->CreateBuffer( &LightBufferDesc, &InitData, &m_pSpotLightBufferSpotMatrices ) );
        DXUT_SetDebugName( m_pSpotLightBufferSpotMatrices, "SpotLightBufferSpotMatrices" );*/

        buffer = CacheBuffer.getCachedByteBuffer(g_SpotLightDataArraySpotMatrices.length * Matrix4f.SIZE);
        CacheBuffer.put(buffer, g_SpotLightDataArraySpotMatrices);
        buffer.flip();

        m_pSpotLightBufferSpotMatrices = new BufferGL();
        m_pSpotLightBufferSpotMatrices.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pSpotLightBufferSpotMatrices.setName("SpotLightBufferSpotMatrices");

        /*SRVDesc.Format = DXGI_FORMAT_R32G32B32A32_FLOAT;
        SRVDesc.Buffer.ElementWidth = 4*MAX_NUM_LIGHTS;
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pSpotLightBufferSpotMatrices, &SRVDesc, &m_pSpotLightBufferSpotMatricesSRV ) );*/

        m_pSpotLightBufferSpotMatricesSRV = m_pSpotLightBufferSpotMatrices;

        // Create the shadow-casting spot light buffer (center and radius)
        /*LightBufferDesc.ByteWidth = sizeof( g_ShadowCastingSpotLightDataArrayCenterAndRadius );
        InitData.pSysMem = g_ShadowCastingSpotLightDataArrayCenterAndRadius;
        V_RETURN( pd3dDevice->CreateBuffer( &LightBufferDesc, &InitData, &m_pShadowCastingSpotLightBufferCenterAndRadius ) );
        DXUT_SetDebugName( m_pShadowCastingSpotLightBufferCenterAndRadius, "ShadowCastingSpotLightBufferCenterAndRadius" );*/

        buffer = CacheBuffer.getCachedByteBuffer(g_ShadowCastingSpotLightDataArrayCenterAndRadius.length * Vector4f.SIZE);
        CacheBuffer.put(buffer, g_ShadowCastingSpotLightDataArrayCenterAndRadius);
        buffer.flip();

        m_pShadowCastingSpotLightBufferCenterAndRadius = new BufferGL();
        m_pShadowCastingSpotLightBufferCenterAndRadius.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pShadowCastingSpotLightBufferCenterAndRadius.setName("ShadowCastingSpotLightBufferCenterAndRadius");

        /*SRVDesc.Format = DXGI_FORMAT_R32G32B32A32_FLOAT;
        SRVDesc.Buffer.ElementWidth = MAX_NUM_SHADOWCASTING_SPOTS;
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pShadowCastingSpotLightBufferCenterAndRadius, &SRVDesc, &m_pShadowCastingSpotLightBufferCenterAndRadiusSRV ) );*/
        m_pShadowCastingSpotLightBufferCenterAndRadiusSRV = m_pShadowCastingSpotLightBufferCenterAndRadius;

        // Create the shadow-casting spot light buffer (color)
        /*LightBufferDesc.ByteWidth = sizeof( g_ShadowCastingSpotLightDataArrayColor );
        InitData.pSysMem = g_ShadowCastingSpotLightDataArrayColor;
        V_RETURN( pd3dDevice->CreateBuffer( &LightBufferDesc, &InitData, &m_pShadowCastingSpotLightBufferColor ) );
        DXUT_SetDebugName( m_pShadowCastingSpotLightBufferColor, "ShadowCastingSpotLightBufferColor" );*/

        buffer = CacheBuffer.getCachedByteBuffer(g_ShadowCastingSpotLightDataArrayColor.length * 4);
        CacheBuffer.put(buffer, g_ShadowCastingSpotLightDataArrayColor);
        buffer.flip();

        m_pShadowCastingSpotLightBufferColor = new BufferGL();
        m_pShadowCastingSpotLightBufferColor.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pShadowCastingSpotLightBufferColor.setName("ShadowCastingSpotLightBufferColor");

        /*SRVDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pShadowCastingSpotLightBufferColor, &SRVDesc, &m_pShadowCastingSpotLightBufferColorSRV ) );*/
        m_pShadowCastingSpotLightBufferColorSRV = m_pShadowCastingSpotLightBufferColor;

        // Create the shadow-casting spot light buffer (spot light parameters)
        /*LightBufferDesc.ByteWidth = sizeof( g_ShadowCastingSpotLightDataArraySpotParams );
        InitData.pSysMem = g_ShadowCastingSpotLightDataArraySpotParams;
        V_RETURN( pd3dDevice->CreateBuffer( &LightBufferDesc, &InitData, &m_pShadowCastingSpotLightBufferSpotParams ) );
        DXUT_SetDebugName( m_pShadowCastingSpotLightBufferSpotParams, "ShadowCastingSpotLightBufferSpotParams" );*/

        buffer = CacheBuffer.getCachedByteBuffer(g_ShadowCastingSpotLightDataArraySpotParams.length * LightUtilSpotParams.SIZE);
        CacheBuffer.put(buffer, g_ShadowCastingSpotLightDataArraySpotParams);
        buffer.flip();

        m_pShadowCastingSpotLightBufferSpotParams = new BufferGL();
        m_pShadowCastingSpotLightBufferSpotParams.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pShadowCastingSpotLightBufferSpotParams.setName("ShadowCastingSpotLightBufferSpotParams");

        /*SRVDesc.Format = DXGI_FORMAT_R16G16B16A16_FLOAT;
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pShadowCastingSpotLightBufferSpotParams, &SRVDesc, &m_pShadowCastingSpotLightBufferSpotParamsSRV ) );*/
        m_pShadowCastingSpotLightBufferSpotParamsSRV = m_pShadowCastingSpotLightBufferSpotParams;

        // Create the shadow-casting spot light buffer (spot light matrices, only used for debug drawing the spot lights)
        /*LightBufferDesc.ByteWidth = sizeof( g_ShadowCastingSpotLightDataArraySpotMatrices );
        InitData.pSysMem = g_ShadowCastingSpotLightDataArraySpotMatrices;
        V_RETURN( pd3dDevice->CreateBuffer( &LightBufferDesc, &InitData, &m_pShadowCastingSpotLightBufferSpotMatrices ) );
        DXUT_SetDebugName( m_pShadowCastingSpotLightBufferSpotMatrices, "ShadowCastingSpotLightBufferSpotMatrices" );*/

        buffer = CacheBuffer.getCachedByteBuffer(g_ShadowCastingSpotLightDataArraySpotMatrices.length * Matrix4f.SIZE);
        CacheBuffer.put(buffer, g_ShadowCastingSpotLightDataArraySpotMatrices);
        buffer.flip();

        m_pShadowCastingSpotLightBufferSpotMatrices = new BufferGL();
        m_pShadowCastingSpotLightBufferSpotMatrices.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pShadowCastingSpotLightBufferSpotMatrices.setName("ShadowCastingSpotLightBufferSpotMatrices");

        /*SRVDesc.Format = DXGI_FORMAT_R32G32B32A32_FLOAT;
        SRVDesc.Buffer.ElementWidth = 4*MAX_NUM_SHADOWCASTING_SPOTS;
        V_RETURN( pd3dDevice->CreateShaderResourceView( m_pShadowCastingSpotLightBufferSpotMatrices, &SRVDesc, &m_pShadowCastingSpotLightBufferSpotMatricesSRV ) );*/
        m_pShadowCastingSpotLightBufferSpotMatricesSRV = m_pShadowCastingSpotLightBufferSpotMatrices;

        // Create the vertex buffer for the sprites (a single quad)
        /*D3D11_BUFFER_DESC VBDesc;
        ZeroMemory( &VBDesc, sizeof(VBDesc) );
        VBDesc.Usage = D3D11_USAGE_IMMUTABLE;
        VBDesc.ByteWidth = sizeof( g_QuadForLightsVertexData );
        VBDesc.BindFlags = D3D11_BIND_VERTEX_BUFFER;
        InitData.pSysMem = g_QuadForLightsVertexData;
        V_RETURN( pd3dDevice->CreateBuffer( &VBDesc, &InitData, &m_pQuadForLightsVB ) );
        DXUT_SetDebugName( m_pQuadForLightsVB, "QuadForLightsVB" );*/

        buffer = CacheBuffer.getCachedByteBuffer(g_QuadForLightsVertexData.length * LightUtilSpriteVertex.SIZE);
        CacheBuffer.put(buffer, g_QuadForLightsVertexData);
        buffer.flip();

        m_pQuadForLightsVB = new BufferGL();
        m_pQuadForLightsVB.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pQuadForLightsVB.setName("QuadForLightsVB");

        // Create the vertex buffer for the cone
        /*VBDesc.ByteWidth = sizeof( g_ConeForSpotLightsVertexData );
        InitData.pSysMem = g_ConeForSpotLightsVertexData;
        V_RETURN( pd3dDevice->CreateBuffer( &VBDesc, &InitData, &m_pConeForSpotLightsVB ) );
        DXUT_SetDebugName( m_pConeForSpotLightsVB, "ConeForSpotLightsVB" );*/

        buffer = CacheBuffer.getCachedByteBuffer(g_ConeForSpotLightsVertexData.length * LightUtilConeVertex.SIZE);
        CacheBuffer.put(buffer, g_ConeForSpotLightsVertexData);
        buffer.flip();

        m_pConeForSpotLightsVB = new BufferGL();
        m_pConeForSpotLightsVB.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pConeForSpotLightsVB.setName("ConeForSpotLightsVB");

        // Create the index buffer for the cone
        /*D3D11_BUFFER_DESC IBDesc;
        ZeroMemory( &IBDesc, sizeof(IBDesc) );
        IBDesc.Usage = D3D11_USAGE_IMMUTABLE;
        IBDesc.ByteWidth = sizeof( g_ConeForSpotLightsIndexData );
        IBDesc.BindFlags = D3D11_BIND_INDEX_BUFFER;
        InitData.pSysMem = g_ConeForSpotLightsIndexData;
        V_RETURN( pd3dDevice->CreateBuffer( &IBDesc, &InitData, &m_pConeForSpotLightsIB ) );
        DXUT_SetDebugName( m_pConeForSpotLightsIB, "ConeForSpotLightsIB" );*/

        buffer = CacheBuffer.getCachedByteBuffer(g_ConeForSpotLightsIndexData.length * 2);
        CacheBuffer.put(buffer, g_ConeForSpotLightsIndexData);
        buffer.flip();

        m_pConeForSpotLightsIB = new BufferGL();
        m_pConeForSpotLightsIB.initlize(GLenum.GL_ARRAY_BUFFER, buffer.remaining(), buffer, GLenum.GL_STATIC_DRAW);
        m_pConeForSpotLightsIB.setName("ConeForSpotLightsIB");

        // Create blend states
       /* D3D11_BLEND_DESC BlendStateDesc;
        ZeroMemory( &BlendStateDesc, sizeof( D3D11_BLEND_DESC ) );
        BlendStateDesc.AlphaToCoverageEnable = FALSE;
        BlendStateDesc.IndependentBlendEnable = FALSE;
        BlendStateDesc.RenderTarget[0].BlendEnable = TRUE;
        BlendStateDesc.RenderTarget[0].SrcBlend = D3D11_BLEND_ONE;
        BlendStateDesc.RenderTarget[0].DestBlend = D3D11_BLEND_ONE;
        BlendStateDesc.RenderTarget[0].BlendOp = D3D11_BLEND_OP_ADD;
        BlendStateDesc.RenderTarget[0].SrcBlendAlpha = D3D11_BLEND_ZERO;
        BlendStateDesc.RenderTarget[0].DestBlendAlpha = D3D11_BLEND_ONE;
        BlendStateDesc.RenderTarget[0].BlendOpAlpha = D3D11_BLEND_OP_ADD;
        BlendStateDesc.RenderTarget[0].RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;
        V_RETURN( pd3dDevice->CreateBlendState( &BlendStateDesc, &m_pBlendStateAdditive ) );*/

        m_pBlendStateAdditive = ()->
        {
            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendFuncSeparate(GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ZERO, GLenum.GL_ONE);
            gl.glColorMask(true, true, true, true);
        };
    }

    void OnDestroyDevice(){
        dispose();
    }

    void OnResizedSwapChain( int width, int height ){}
    void OnReleasingSwapChain(){}

    private static int COLOR(int r, int g, int b){
        return Numeric.makeRGBA(r,g,b,255);
    }

    /**
     * Fill in the data for the lights (center, radius, and color). Also fill in the vertex data for the sprite quad.
     * @param BBoxMin
     * @param BBoxMax
     */
    static void InitLights( ReadableVector3f BBoxMin, ReadableVector3f BBoxMax ){
        // init the random seed to 1, so that results are deterministic
        // across different runs of the sample
        Random random = new Random(1);

        class GetRandFloat{
            float of(float lower, float upper){
               return random.nextFloat() * (upper - lower) + lower;
            }
        }

        final GetRandFloat randFloat = new GetRandFloat();
        // scale the size of the lights based on the size of the scene
        /*XMVECTOR BBoxExtents = 0.5f * (BBoxMax - BBoxMin);
        float fRadius = 0.075f * XMVectorGetX(XMVector3Length(BBoxExtents));*/
        float fRadius = Vector3f.distance(BBoxMax, BBoxMin) * 0.5f * 0.075f;

        // For point lights, the radius of the bounding sphere for the light (used for culling)
        // and the falloff distance of the light (used for lighting) are the same. Not so for
        // spot lights. A spot light is a right circular cone. The height of the cone is the
        // falloff distance. We want to fit the cone of the spot light inside the bounding sphere.
        // From calculus, we know the cone with maximum volume that can fit inside a sphere has height:
        // h_cone = (4/3)*r_sphere
        float fSpotLightFalloffRadius = 1.333333333333f * fRadius;

        /*XMFLOAT3 vBBoxMin, vBBoxMax;
        XMStoreFloat3( &vBBoxMin, BBoxMin );
        XMStoreFloat3( &vBBoxMax, BBoxMax );*/

        // initialize the point light data
        for (int i = 0; i < MAX_NUM_LIGHTS; i++)
        {
            if(g_PointLightDataArrayCenterAndRadius[i] == null)
                g_PointLightDataArrayCenterAndRadius[i] = new Vector4f();
            g_PointLightDataArrayCenterAndRadius[i].set(randFloat.of(BBoxMin.getX(),BBoxMax.getX()),
                    randFloat.of(BBoxMin.getY(),BBoxMax.getY()), randFloat.of(BBoxMin.getZ(),BBoxMax.getZ()), fRadius);
            g_PointLightDataArrayColor[i] = GetRandColor();
        }

        // initialize the spot light data
        for (int i = 0; i < MAX_NUM_LIGHTS; i++)
        {
            if(g_SpotLightDataArrayCenterAndRadius[i] == null)
                g_SpotLightDataArrayCenterAndRadius[i] = new Vector4f();
            g_SpotLightDataArrayCenterAndRadius[i].set(randFloat.of(BBoxMin.getX(),BBoxMax.getX()), randFloat.of(BBoxMin.getY(),BBoxMax.getY()), randFloat.of(BBoxMin.getZ(),BBoxMax.getZ()), fRadius);
            g_SpotLightDataArrayColor[i] = GetRandColor();

            Vector3f vLightDir = GetRandLightDirection();

            // Okay, so we fit a max-volume cone inside our bounding sphere for the spot light. We need to find
            // the cone angle for that cone. Google on "cone inside sphere" (without the quotes) to find info
            // on how to derive these formulas for the height and radius of the max-volume cone inside a sphere.
            // h_cone = (4/3)*r_sphere
            // r_cone = sqrt(8/9)*r_sphere
            // tan(theta) = r_cone/h_cone = sqrt(2)/2 = 0.7071067811865475244
            // theta = 35.26438968 degrees
            // store the cosine of this angle: cosine(35.26438968 degrees) = 0.816496580927726

            // random direction, cosine of cone angle, falloff radius calcuated above
            if(g_SpotLightDataArraySpotParams[i] == null) g_SpotLightDataArraySpotParams[i] = new LightUtilSpotParams();
            PackSpotParams(g_SpotLightDataArraySpotParams[i],vLightDir, 0.816496580927726f, fSpotLightFalloffRadius);

            // build a "rotate from one vector to another" matrix, to point the spot light
            // cone along its light direction
            /*XMVECTOR s = XMVectorSet(0.0f,-1.0f,0.0f,0.0f);
            XMVECTOR t = XMLoadFloat3( &vLightDir );
            XMFLOAT3 v;
            XMStoreFloat3( &v, XMVector3Cross(s,t) );
            float e = XMVectorGetX(XMVector3Dot(s,t));
            float h = 1.0f / (1.0f + e);

            XMFLOAT4X4 f4x4Rotation;
            XMStoreFloat4x4( &f4x4Rotation, XMMatrixIdentity() );
            f4x4Rotation._11 = e + h*v.x*v.x;
            f4x4Rotation._12 = h*v.x*v.y - v.z;
            f4x4Rotation._13 = h*v.x*v.z + v.y;
            f4x4Rotation._21 = h*v.x*v.y + v.z;
            f4x4Rotation._22 = e + h*v.y*v.y;
            f4x4Rotation._23 = h*v.y*v.z - v.x;
            f4x4Rotation._31 = h*v.x*v.z - v.y;
            f4x4Rotation._32 = h*v.y*v.z + v.x;
            f4x4Rotation._33 = e + h*v.z*v.z;
            XMMATRIX mRotation = XMLoadFloat4x4( &f4x4Rotation );*/

            float e = Vector3f.dot(Vector3f.Y_AXIS_NEG,vLightDir);
            Vector3f v= Vector3f.cross(Vector3f.Y_AXIS_NEG, vLightDir, vLightDir);
            float h = 1.0f / (1.0f + e);
            Matrix4f f4x4Rotation = g_SpotLightDataArraySpotMatrices[ uShadowCastingSpotLightCounter ];
            if(f4x4Rotation == null)
                f4x4Rotation = new Matrix4f();

            f4x4Rotation.m00 = e + h*v.x*v.x;
            f4x4Rotation.m01 = h*v.x*v.y - v.z;
            f4x4Rotation.m02 = h*v.x*v.z + v.y;
            f4x4Rotation.m10 = h*v.x*v.y + v.z;
            f4x4Rotation.m11 = e + h*v.y*v.y;
            f4x4Rotation.m12 = h*v.y*v.z - v.x;
            f4x4Rotation.m20 = h*v.x*v.z - v.y;
            f4x4Rotation.m21 = h*v.y*v.z + v.x;
            f4x4Rotation.m22 = e + h*v.z*v.z;
            f4x4Rotation.m30  = 0;
            f4x4Rotation.m31  = 0;
            f4x4Rotation.m32  = 0;
            f4x4Rotation.m33  = 1;

            g_SpotLightDataArraySpotMatrices[i] = f4x4Rotation/*XMMatrixTranspose(mRotation)*/;
        }

        // initialize the shadow-casting point light data
        {
            // Hanging lamps
            AddShadowCastingPointLight( new Vector4f( -620.0f, 136.0f, 218.0f, 450.0f ), COLOR( 200, 100, 0 ) );
            AddShadowCastingPointLight( new Vector4f( -620.0f, 136.0f, -140.0f, 450.0f ), COLOR( 200, 100, 0 ) );
            AddShadowCastingPointLight( new Vector4f(  490.0f, 136.0f, 218.0f, 450.0f ), COLOR( 200, 100, 0 ) );
            AddShadowCastingPointLight( new Vector4f(  490.0f, 136.0f, -140.0f, 450.0f ), COLOR( 200, 100, 0 ) );

            // Corner lights
            AddShadowCastingPointLight( new Vector4f( -1280.0f, 120.0f, -300.0f, 500.0f ), COLOR( 120, 60, 60 ) );
            AddShadowCastingPointLight( new Vector4f( -1280.0f, 200.0f,  430.0f, 600.0f ), COLOR( 50, 50, 128 ) );
            AddShadowCastingPointLight( new Vector4f( 1030.0f, 200.0f, 545.0f, 500.0f ), COLOR( 255, 128, 0 ) );
            AddShadowCastingPointLight( new Vector4f( 1180.0f, 220.0f, -390.0f, 500.0f ), COLOR( 100, 100, 255 ) );

            // Midpoint lights
            AddShadowCastingPointLight( new Vector4f( -65.0f, 100.0f, 220.0f, 500.0f ), COLOR( 200, 200, 200 ) );
            AddShadowCastingPointLight( new Vector4f( -65.0f, 100.0f,-140.0f, 500.0f ), COLOR( 200, 200, 200 ) );

            // High gallery lights
            AddShadowCastingPointLight( new Vector4f( 600.0f, 660.0f, -30.0f, 800.0f ), COLOR( 100, 100, 100 ) );
            AddShadowCastingPointLight( new Vector4f( -700.0f, 660.0f, 80.0f, 800.0f ), COLOR( 100, 100, 100 ) );
        }

        {
            // Curtain spot
            AddShadowCastingSpotLight( new Vector4f(  -772.0f, 254.0f, -503.0f, 800.0f ), new Vector3f( -814.0f, 180.0f, -250.0f ), COLOR( 255, 255, 255 ) );

            // Lion spots
            AddShadowCastingSpotLight( new Vector4f(  1130.0f, 378.0f, 40.0f, 500.0f ), new Vector3f( 1150.0f, 290.0f, 40.0f ), COLOR( 200, 200, 100 ) );
            AddShadowCastingSpotLight( new Vector4f( -1260.0f, 378.0f, 40.0f, 500.0f ), new Vector3f( -1280.0f, 290.0f, 40.0f ), COLOR( 200, 200, 100 ) );

            // Gallery spots
            AddShadowCastingSpotLight( new Vector4f( -115.0f, 660.0f, -100.0f, 800.0f ), new Vector3f( -115.0f, 630.0f, 0.0f ), COLOR( 200, 200, 200 ) );
            AddShadowCastingSpotLight( new Vector4f( -115.0f, 660.0f,  100.0f, 800.0f ), new Vector3f( -115.0f, 630.0f, -100.0f ), COLOR( 200, 200, 200 ) );

            AddShadowCastingSpotLight( new Vector4f( -770.0f, 660.0f, -100.0f, 800.0f ), new Vector3f( -770.0f, 630.0f, 0.0f ), COLOR( 200, 200, 200 ) );
            AddShadowCastingSpotLight( new Vector4f( -770.0f, 660.0f,  100.0f, 800.0f ), new Vector3f( -770.0f, 630.0f, -100.0f ), COLOR( 200, 200, 200 ) );

            AddShadowCastingSpotLight( new Vector4f( 500.0f, 660.0f, -100.0f, 800.0f ), new Vector3f( 500.0f, 630.0f, 0.0f ), COLOR( 200, 200, 200 ) );
            AddShadowCastingSpotLight( new Vector4f( 500.0f, 660.0f,  100.0f, 800.0f ), new Vector3f( 500.0f, 630.0f, -100.0f ), COLOR( 200, 200, 200 ) );

            // Red corner spots
            AddShadowCastingSpotLight( new Vector4f( -1240.0f, 90.0f, -70.0f, 700.0f ), new Vector3f( -1240.0f, 140.0f, -405.0f ), COLOR( 200, 0, 0 ) );
            AddShadowCastingSpotLight( new Vector4f( -1000.0f, 90.0f, -260.0f, 700.0f ), new Vector3f( -1240.0f, 140.0f, -405.0f ), COLOR( 200, 0, 0 ) );

            // Green corner spot
            AddShadowCastingSpotLight( new Vector4f( -900.0f, 60.0f, 340.0f, 700.0f ), new Vector3f( -1360.0f, 255.0f, 555.0f ), COLOR( 100, 200, 100 ) );
        }

        // initialize the vertex buffer data for a quad (for drawing the lights)
        float fQuadHalfSize = 0.083f * fRadius;
        g_QuadForLightsVertexData[0].v3Pos.set(-fQuadHalfSize, -fQuadHalfSize, 0.0f );
        g_QuadForLightsVertexData[0].v2TexCoord.set( 0.0f, 0.0f );
        g_QuadForLightsVertexData[1].v3Pos.set(-fQuadHalfSize,  fQuadHalfSize, 0.0f );
        g_QuadForLightsVertexData[1].v2TexCoord.set( 0.0f, 1.0f );
        g_QuadForLightsVertexData[2].v3Pos.set( fQuadHalfSize, -fQuadHalfSize, 0.0f );
        g_QuadForLightsVertexData[2].v2TexCoord.set( 1.0f, 0.0f );
        g_QuadForLightsVertexData[3].v3Pos.set(-fQuadHalfSize,  fQuadHalfSize, 0.0f );
        g_QuadForLightsVertexData[3].v2TexCoord.set( 0.0f, 1.0f );
        g_QuadForLightsVertexData[4].v3Pos.set( fQuadHalfSize,  fQuadHalfSize, 0.0f );
        g_QuadForLightsVertexData[4].v2TexCoord.set( 1.0f, 1.0f );
        g_QuadForLightsVertexData[5].v3Pos.set( fQuadHalfSize, -fQuadHalfSize, 0.0f );
        g_QuadForLightsVertexData[5].v2TexCoord.set( 1.0f, 0.0f );

        // initialize the vertex and index buffer data for a cone (for drawing the spot lights)
        {
            // h_cone = (4/3)*r_sphere
            // r_cone = sqrt(8/9)*r_sphere
            float fConeSphereRadius = 0.033f * fRadius;
            float fConeHeight = 1.333333333333f * fConeSphereRadius;
            float fConeRadius = 0.942809041582f * fConeSphereRadius;

            for (int i = 0; i < g_nConeNumTris; i++)
            {
                // We want to calculate points along the circle at the end of the cone.
                // The parametric equations for this circle are:
                // x=r_cone*cosine(t)
                // z=r_cone*sine(t)
                float t = ((float)i / (float)g_nConeNumTris) * TWO_PI;
                g_ConeForSpotLightsVertexData[2*i+1].v3Pos.set( fConeRadius*(float)Math.cos(t), -fConeHeight, fConeRadius*(float)Math.sin(t) );
                g_ConeForSpotLightsVertexData[2*i+1].v2TexCoord.set( 0.0f, 1.0f );

                // normal = (h_cone*cosine(t), r_cone, h_cone*sine(t))
//                Vector3f vNormal = new Vector3f( fConeHeight*(float)Math.cos(t), fConeRadius, fConeHeight*(float)Math.sin(t) );
//                XMStoreFloat3( &vNormal, XMVector3Normalize( XMLoadFloat3( &vNormal ) ) );
                g_ConeForSpotLightsVertexData[2*i+1].v3Norm.set(fConeHeight*(float)Math.cos(t), fConeRadius, fConeHeight*(float)Math.sin(t));
                g_ConeForSpotLightsVertexData[2*i+1].v3Norm.normalise();
//#ifdef _DEBUG
                // check that the normal is actually perpendicular
                float dot = Vector3f.dot( g_ConeForSpotLightsVertexData[2*i+1].v3Pos, g_ConeForSpotLightsVertexData[2*i+1].v3Norm );
                if(Math.abs(dot) >= 0.001f) {
                    throw new IllegalStateException();
                }
//#endif
            }

            // create duplicate points for the top of the cone, each with its own normal
            for (int i = 0; i < g_nConeNumTris-1; i++)
            {
                g_ConeForSpotLightsVertexData[2*i].v3Pos.set( 0.0f, 0.0f, 0.0f );
                g_ConeForSpotLightsVertexData[2*i].v2TexCoord.set( 0.0f, 0.0f );

                /*XMFLOAT3 vNormal;
                XMVECTOR Normal = XMLoadFloat3(&g_ConeForSpotLightsVertexData[2*i+1].v3Norm) + XMLoadFloat3(&g_ConeForSpotLightsVertexData[2*i+3].v3Norm);
                XMStoreFloat3( &vNormal, XMVector3Normalize( Normal ) );
                g_ConeForSpotLightsVertexData[2*i].v3Norm = vNormal;*/
                Vector3f.add(g_ConeForSpotLightsVertexData[2*i+1].v3Norm, g_ConeForSpotLightsVertexData[2*i+3].v3Norm, g_ConeForSpotLightsVertexData[2*i].v3Norm);
                g_ConeForSpotLightsVertexData[2*i].v3Norm.normalise();
            }

            // fill in the index buffer for the cone
            for (int i = 0; i < g_nConeNumTris; i++)
            {
                g_ConeForSpotLightsIndexData[3*i+0] = ( short)(2*i);
                g_ConeForSpotLightsIndexData[3*i+1] = ( short)(2*i+3);
                g_ConeForSpotLightsIndexData[3*i+2] = ( short)(2*i+1);
            }

            // fix up the last triangle
            g_ConeForSpotLightsIndexData[3*g_nConeNumTris-2] = 1;
        }
    }

    // returning a 2D array as XMMATRIX*[6], please forgive this ugly syntax
    static Matrix4f[][] GetShadowCastingPointLightViewProjTransposedArray(){
        return g_ShadowCastingPointLightViewProjTransposed;
    }

    static Matrix4f[][] GetShadowCastingPointLightViewProjInvTransposedArray(){
        return g_ShadowCastingPointLightViewProjInvTransposed;
    }

    static Matrix4f[] GetShadowCastingSpotLightViewProjTransposedArray(){
        return g_ShadowCastingSpotLightViewProjTransposed;
    }

    static Matrix4f[] GetShadowCastingSpotLightViewProjInvTransposedArray(){
        return g_ShadowCastingSpotLightViewProjInvTransposed;
    }

    void AddShadersToCache( /*AMD::ShaderCache *pShaderCache*/ ) throws IOException {
        // Ensure all shaders (and input layouts) are released
        SAFE_RELEASE( m_pDebugDrawPointLightsVS );
        SAFE_RELEASE( m_pDebugDrawPointLightsPS );
//        SAFE_RELEASE( m_pDebugDrawPointLightsLayout11 );
        SAFE_RELEASE( m_pDebugDrawSpotLightsVS );
        SAFE_RELEASE( m_pDebugDrawSpotLightsPS );
//        SAFE_RELEASE( m_pDebugDrawSpotLightsLayout11 );

        final int DXGI_FORMAT_R32G32B32_FLOAT = GLenum.GL_RGBA32F;
        final int DXGI_FORMAT_R32G32_FLOAT = GLenum.GL_RG32F;
        final int D3D11_INPUT_PER_VERTEX_DATA = 0;
        final D3D11_INPUT_ELEMENT_DESC LayoutForSprites[] =
        {
            new D3D11_INPUT_ELEMENT_DESC( "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0,  0, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
            new D3D11_INPUT_ELEMENT_DESC( "TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT,    0, 12, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
        };

        final D3D11_INPUT_ELEMENT_DESC LayoutForCone[] =
        {
            new D3D11_INPUT_ELEMENT_DESC( "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0,  0, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
            new D3D11_INPUT_ELEMENT_DESC( "NORMAL",   0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 12, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
            new D3D11_INPUT_ELEMENT_DESC( "TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT,    0, 24, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
        };

        m_pDebugDrawPointLightsVS = GLSLProgram.createShaderProgramFromFile(SHADER_PATH+"DebugDrawPointLightsVS.vert", ShaderType.VERTEX);
        m_pDebugDrawPointLightsPS = GLSLProgram.createShaderProgramFromFile(SHADER_PATH+"DebugDrawPointLightsPS.frag", ShaderType.FRAGMENT);
        m_pDebugDrawSpotLightsVS = GLSLProgram.createShaderProgramFromFile(SHADER_PATH+"DebugDrawSpotLightsVS.vert", ShaderType.VERTEX);
        m_pDebugDrawSpotLightsPS = GLSLProgram.createShaderProgramFromFile(SHADER_PATH+"DebugDrawSpotLightsPS.frag", ShaderType.FRAGMENT);

        /*pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pDebugDrawPointLightsVS, AMD::ShaderCache::SHADER_TYPE_VERTEX, L"vs_5_0", L"DebugDrawPointLightsVS",
                L"DebugDraw.hlsl", 0, NULL, &m_pDebugDrawPointLightsLayout11, LayoutForSprites, ARRAYSIZE( LayoutForSprites ) );

        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pDebugDrawPointLightsPS, AMD::ShaderCache::SHADER_TYPE_PIXEL, L"ps_5_0", L"DebugDrawPointLightsPS",
                L"DebugDraw.hlsl", 0, NULL, NULL, NULL, 0 );

        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pDebugDrawSpotLightsVS, AMD::ShaderCache::SHADER_TYPE_VERTEX, L"vs_5_0", L"DebugDrawSpotLightsVS",
                L"DebugDraw.hlsl", 0, NULL, &m_pDebugDrawSpotLightsLayout11, LayoutForCone, ARRAYSIZE( LayoutForCone ) );

        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pDebugDrawSpotLightsPS, AMD::ShaderCache::SHADER_TYPE_PIXEL, L"ps_5_0", L"DebugDrawSpotLightsPS",
                L"DebugDraw.hlsl", 0, NULL, NULL, NULL, 0 );*/
    }

    void RenderLights( float fElapsedTime, int uNumPointLights, int uNumSpotLights, int nLightingMode, CommonUtil CommonUtil ){
        /*ID3D11ShaderResourceView* pNULLSRV = NULL;
        ID3D11SamplerState* pNULLSampler = NULL;

        ID3D11DeviceContext* pd3dImmediateContext = DXUTGetD3D11DeviceContext();*/

        // save blend state (for later restore)
        /*ID3D11BlendState* pBlendStateStored11 = NULL;
        FLOAT afBlendFactorStored11[4];
        UINT uSampleMaskStored11;
        pd3dImmediateContext->OMGetBlendState( &pBlendStateStored11, afBlendFactorStored11, &uSampleMaskStored11 );
        FLOAT BlendFactor[4] = { 0,0,0,0 };*/

        // save depth state (for later restore)
       /* ID3D11DepthStencilState* pDepthStencilStateStored11 = NULL;
        UINT uStencilRefStored11;
        pd3dImmediateContext->OMGetDepthStencilState( &pDepthStencilStateStored11, &uStencilRefStored11 );*/

        // point lights
        if( uNumPointLights > 0 )
        {
            // additive blending, enable depth test but don't write depth, disable culling
            /*pd3dImmediateContext->OMSetBlendState( m_pBlendStateAdditive, BlendFactor, 0xFFFFFFFF );
            pd3dImmediateContext->OMSetDepthStencilState( CommonUtil.GetDepthStencilState(DEPTH_STENCIL_STATE_DISABLE_DEPTH_WRITE), 0x00 );
            pd3dImmediateContext->RSSetState( CommonUtil.GetRasterizerState(RASTERIZER_STATE_DISABLE_CULLING) );*/

            m_pBlendStateAdditive.run();
            CommonUtil.GetDepthStencilState(CommonUtil.DEPTH_STENCIL_STATE_DISABLE_DEPTH_WRITE).run();
            CommonUtil.GetRasterizerState(CommonUtil.RASTERIZER_STATE_DISABLE_CULLING).run();

            // Set the input layout
            /*pd3dImmediateContext->IASetInputLayout( m_pDebugDrawPointLightsLayout11 );
            // Set vertex buffer
            UINT uStride = sizeof( LightUtilSpriteVertex );
            UINT uOffset = 0;
            pd3dImmediateContext->IASetVertexBuffers( 0, 1, &m_pQuadForLightsVB, &uStride, &uOffset );
            // Set primitive topology
            pd3dImmediateContext->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST );

            pd3dImmediateContext->VSSetShader( m_pDebugDrawPointLightsVS, NULL, 0 );
            pd3dImmediateContext->VSSetShaderResources( 2, 1, GetPointLightBufferCenterAndRadiusSRVParam(nLightingMode) );
            pd3dImmediateContext->VSSetShaderResources( 3, 1, GetPointLightBufferColorSRVParam(nLightingMode) );
            pd3dImmediateContext->PSSetShader( m_pDebugDrawPointLightsPS, NULL, 0 );
            pd3dImmediateContext->PSSetShaderResources( 0, 1, &pNULLSRV );
            pd3dImmediateContext->PSSetShaderResources( 1, 1, &pNULLSRV );
            pd3dImmediateContext->PSSetSamplers( 0, 1, &pNULLSampler );*/
            gl.glUseProgram(0);
            m_ShaderCombine.enable();
            m_ShaderCombine.setVS(m_pDebugDrawPointLightsVS);
            m_ShaderCombine.setPS(m_pDebugDrawPointLightsPS);

            // todo shader resources
            m_pQuadForLightsVB.bind();
            m_pDebugDrawPointLightsLayout11.bind();

//            pd3dImmediateContext->DrawInstanced(6,uNumPointLights,0,0);
            gl.glDrawArraysInstanced(GLenum.GL_TRIANGLES, 0, 6, uNumPointLights);

            m_pDebugDrawPointLightsLayout11.unbind();
            m_pQuadForLightsVB.unbind();

            // restore to default
            /*pd3dImmediateContext->VSSetShaderResources( 2, 1, &pNULLSRV );
            pd3dImmediateContext->VSSetShaderResources( 3, 1, &pNULLSRV );*/
        }

        // spot lights
        if( uNumSpotLights > 0 )
        {
            // render spot lights as ordinary opaque geometry
            /*pd3dImmediateContext->OMSetBlendState( m_pBlendStateAdditive, BlendFactor, 0xFFFFFFFF );
            pd3dImmediateContext->OMSetDepthStencilState( CommonUtil.GetDepthStencilState(DEPTH_STENCIL_STATE_DEPTH_GREATER), 0x00 );
            pd3dImmediateContext->RSSetState( CommonUtil.GetRasterizerState(RASTERIZER_STATE_DISABLE_CULLING) );*/

            m_pBlendStateAdditive.run();
            CommonUtil.GetDepthStencilState(CommonUtil.DEPTH_STENCIL_STATE_DEPTH_GREATER).run();
            CommonUtil.GetRasterizerState(CommonUtil.RASTERIZER_STATE_DISABLE_CULLING).run();

            // Set the input layout
            /*pd3dImmediateContext->IASetInputLayout( m_pDebugDrawSpotLightsLayout11 );
            // Set vertex buffer
            UINT uStride = sizeof( LightUtilConeVertex );
            UINT uOffset = 0;
            pd3dImmediateContext->IASetVertexBuffers( 0, 1, &m_pConeForSpotLightsVB, &uStride, &uOffset );
            pd3dImmediateContext->IASetIndexBuffer(m_pConeForSpotLightsIB, DXGI_FORMAT_R16_UINT, 0 );
            // Set primitive topology
            pd3dImmediateContext->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST );
            pd3dImmediateContext->VSSetShader( m_pDebugDrawSpotLightsVS, NULL, 0 );
            pd3dImmediateContext->VSSetShaderResources( 5, 1, GetSpotLightBufferCenterAndRadiusSRVParam(nLightingMode) );
            pd3dImmediateContext->VSSetShaderResources( 6, 1, GetSpotLightBufferColorSRVParam(nLightingMode) );
            pd3dImmediateContext->VSSetShaderResources( 7, 1, GetSpotLightBufferSpotParamsSRVParam(nLightingMode) );
            pd3dImmediateContext->VSSetShaderResources( 12, 1, GetSpotLightBufferSpotMatricesSRVParam(nLightingMode) );
            pd3dImmediateContext->PSSetShader( m_pDebugDrawSpotLightsPS, NULL, 0 );
            pd3dImmediateContext->PSSetShaderResources( 0, 1, &pNULLSRV );
            pd3dImmediateContext->PSSetShaderResources( 1, 1, &pNULLSRV );
            pd3dImmediateContext->PSSetSamplers( 0, 1, &pNULLSampler );*/
            gl.glUseProgram(0);
            m_ShaderCombine.enable();
            m_ShaderCombine.setVS(m_pDebugDrawSpotLightsVS);
            m_ShaderCombine.setPS(m_pDebugDrawSpotLightsPS);

            // todo shader resources
            m_pConeForSpotLightsVB.bind();
            m_pDebugDrawSpotLightsLayout11.bind();
            m_pConeForSpotLightsIB.bind();

//            pd3dImmediateContext->DrawInstanced(6,uNumPointLights,0,0);
            gl.glDrawElementsInstanced(GLenum.GL_TRIANGLES, g_nConeNumIndices, GLenum.GL_UNSIGNED_SHORT, 0, uNumPointLights);

            m_pDebugDrawSpotLightsLayout11.unbind();
            m_pConeForSpotLightsVB.unbind();

            /*pd3dImmediateContext->DrawIndexedInstanced(g_nConeNumIndices,uNumSpotLights,0,0,0);
            // restore to default
            pd3dImmediateContext->VSSetShaderResources( 5, 1, &pNULLSRV );
            pd3dImmediateContext->VSSetShaderResources( 6, 1, &pNULLSRV );
            pd3dImmediateContext->VSSetShaderResources( 7, 1, &pNULLSRV );
            pd3dImmediateContext->VSSetShaderResources( 12, 1, &pNULLSRV );*/
        }

        // restore to default
//        pd3dImmediateContext->RSSetState( NULL );
        gl.glDisable(GLenum.GL_BLEND);
        gl.glColorMask(true, true, true, true);

        // restore to previous
        /*pd3dImmediateContext->OMSetDepthStencilState( pDepthStencilStateStored11, uStencilRefStored11 );
        pd3dImmediateContext->OMSetBlendState( pBlendStateStored11, afBlendFactorStored11, uSampleMaskStored11 );
        SAFE_RELEASE( pDepthStencilStateStored11 );
        SAFE_RELEASE( pBlendStateStored11 );*/
    }

    BufferGL GetPointLightBufferCenterAndRadiusSRVParam( int nLightingMode )  { return (nLightingMode == LIGHTING_SHADOWS) ? m_pShadowCastingPointLightBufferCenterAndRadiusSRV : m_pPointLightBufferCenterAndRadiusSRV; }
    BufferGL GetPointLightBufferColorSRVParam( int nLightingMode )  { return (nLightingMode == LIGHTING_SHADOWS) ? m_pShadowCastingPointLightBufferColorSRV : m_pPointLightBufferColorSRV; }

    BufferGL GetSpotLightBufferCenterAndRadiusSRVParam( int nLightingMode )  { return (nLightingMode == LIGHTING_SHADOWS) ? m_pShadowCastingSpotLightBufferCenterAndRadiusSRV : m_pSpotLightBufferCenterAndRadiusSRV; }
    BufferGL GetSpotLightBufferColorSRVParam( int nLightingMode )  { return (nLightingMode == LIGHTING_SHADOWS) ? m_pShadowCastingSpotLightBufferColorSRV : m_pSpotLightBufferColorSRV; }
    BufferGL GetSpotLightBufferSpotParamsSRVParam( int nLightingMode )  { return (nLightingMode == LIGHTING_SHADOWS) ? m_pShadowCastingSpotLightBufferSpotParamsSRV : m_pSpotLightBufferSpotParamsSRV; }
    BufferGL GetSpotLightBufferSpotMatricesSRVParam( int nLightingMode )  { return (nLightingMode == LIGHTING_SHADOWS) ? m_pShadowCastingSpotLightBufferSpotMatricesSRV : m_pSpotLightBufferSpotMatricesSRV; }

    @Override
    public void dispose() {
        SAFE_RELEASE(m_pPointLightBufferCenterAndRadius);
        SAFE_RELEASE(m_pPointLightBufferCenterAndRadiusSRV);
        SAFE_RELEASE(m_pPointLightBufferColor);
        SAFE_RELEASE(m_pPointLightBufferColorSRV);
        SAFE_RELEASE(m_pShadowCastingPointLightBufferCenterAndRadius);
        SAFE_RELEASE(m_pShadowCastingPointLightBufferCenterAndRadiusSRV);
        SAFE_RELEASE(m_pShadowCastingPointLightBufferColor);
        SAFE_RELEASE(m_pShadowCastingPointLightBufferColorSRV);
        SAFE_RELEASE(m_pSpotLightBufferCenterAndRadius);
        SAFE_RELEASE(m_pSpotLightBufferCenterAndRadiusSRV);
        SAFE_RELEASE(m_pSpotLightBufferColor);
        SAFE_RELEASE(m_pSpotLightBufferColorSRV);
        SAFE_RELEASE(m_pSpotLightBufferSpotParams);
        SAFE_RELEASE(m_pSpotLightBufferSpotParamsSRV);
        SAFE_RELEASE(m_pSpotLightBufferSpotMatrices);
        SAFE_RELEASE(m_pSpotLightBufferSpotMatricesSRV);
        SAFE_RELEASE(m_pShadowCastingSpotLightBufferCenterAndRadius);
        SAFE_RELEASE(m_pShadowCastingSpotLightBufferCenterAndRadiusSRV);
        SAFE_RELEASE(m_pShadowCastingSpotLightBufferColor);
        SAFE_RELEASE(m_pShadowCastingSpotLightBufferColorSRV);
        SAFE_RELEASE(m_pShadowCastingSpotLightBufferSpotParams);
        SAFE_RELEASE(m_pShadowCastingSpotLightBufferSpotParamsSRV);
        SAFE_RELEASE(m_pShadowCastingSpotLightBufferSpotMatrices);
        SAFE_RELEASE(m_pShadowCastingSpotLightBufferSpotMatricesSRV);
        SAFE_RELEASE(m_pQuadForLightsVB);
        SAFE_RELEASE(m_pConeForSpotLightsVB);
        SAFE_RELEASE(m_pConeForSpotLightsIB);
        SAFE_RELEASE(m_pDebugDrawPointLightsVS);
        SAFE_RELEASE(m_pDebugDrawPointLightsPS);
//        SAFE_RELEASE(m_pDebugDrawPointLightsLayout11);
        SAFE_RELEASE(m_pDebugDrawSpotLightsVS);
        SAFE_RELEASE(m_pDebugDrawSpotLightsPS);
//        SAFE_RELEASE(m_pDebugDrawSpotLightsLayout11);
//        SAFE_RELEASE(m_pBlendStateAdditive);
    }
}
