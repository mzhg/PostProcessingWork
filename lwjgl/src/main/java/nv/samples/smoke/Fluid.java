package nv.samples.smoke;

import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by Administrator on 2018/7/8 0008.
 */

final class Fluid implements Disposeable{
    static final int
    /*enum RENDER_TARGET
    {*/
        RENDER_TARGET_VELOCITY0 = 0,
        RENDER_TARGET_VELOCITY1 = 1,
        RENDER_TARGET_PRESSURE = 2,
        RENDER_TARGET_SCALAR0 = 3,
        RENDER_TARGET_SCALAR1 = 4,
        RENDER_TARGET_OBSTACLES = 5,
        RENDER_TARGET_OBSTVELOCITY = 6,
        RENDER_TARGET_TEMPSCALAR = 7,
        RENDER_TARGET_TEMPVECTOR = 8,
        RENDER_TARGET_TEMPVECTOR1 = 9,
        NUM_RENDER_TARGETS = 10,
//    };

    /*enum FLUID_TYPE
    {*/
        FT_SMOKE =0,
        FT_FIRE  =1,
        FT_LIQUID=2;
//    };

    // Internal State
    //===============

    // Grid is used to draw quads for all the slices in the 3D simulation grid
    private Grid                        m_pGrid;


    // D3D10 device
//    ID3D10Device                        *m_pD3DDevice;

    // Textures, rendertarget views and shader resource views
    private final Texture3D[]           m_p3DTextures = new Texture3D[ NUM_RENDER_TARGETS ];
    private final TextureGL[]           m_pShaderResourceViews = m_p3DTextures;
    private final TextureGL[]           m_pRenderTargetViews   = m_p3DTextures;
    private final int[]                 m_RenderTargetFormats  = new int[ NUM_RENDER_TARGETS ];

    // Effect (simulation shaders)
    private String                      m_effectPath;
//    ID3D10Effect                        *m_pEffect;

    // Effect Techniques
    private FluidSimulatorProgram       m_etAdvect;
    private FluidSimulatorProgram       m_etAdvectMACCORMACK;
    private FluidSimulatorProgram       m_etVorticity;
    private FluidSimulatorProgram       m_etConfinement;
    private FluidSimulatorProgram       m_etDiffuse;
    private FluidSimulatorProgram       m_etDivergence;
    private FluidSimulatorProgram       m_etScalarJacobi;
    private FluidSimulatorProgram       m_etProject;

    private FluidSimulatorProgram       m_etInitLevelSetToLiquidHeight;
    private FluidSimulatorProgram       m_etInjectLiquid;
    private FluidSimulatorProgram       m_etAirPressure;
    private FluidSimulatorProgram       m_etRedistance;
    private FluidSimulatorProgram       m_etExtrapolateVelocity;

    private FluidSimulatorProgram       m_etLiquidStream_LevelSet;
    private FluidSimulatorProgram       m_etLiquidStream_Velocity;
    private FluidSimulatorProgram       m_etGravity;

    private FluidSimulatorProgram       m_etGaussian;
    private FluidSimulatorProgram       m_etCopyTextureDensity;
    private FluidSimulatorProgram       m_etAddDensityDerivativeVelocity;

    private FluidSimulatorProgram       m_etStaticObstacleTriangles;
    private FluidSimulatorProgram       m_etStaticObstacleLines;
    private FluidSimulatorProgram       m_etDrawBox;

    private FluidSimulatorProgram       m_etDrawTexture;

    private Texture2D                   m_FireBase;


    // Shader variables
    /*ID3D10EffectScalarVariable          *m_evTextureWidth;
    ID3D10EffectScalarVariable          *m_evTextureHeight;
    ID3D10EffectScalarVariable          *m_evTextureDepth;
    ID3D10EffectScalarVariable          *m_evDrawTexture;
    ID3D10EffectScalarVariable          *m_evDecay;
    ID3D10EffectScalarVariable          *m_evViscosity;
    ID3D10EffectScalarVariable          *m_evRadius;
    ID3D10EffectVectorVariable          *m_evCenter;
    ID3D10EffectVectorVariable          *m_evColor;
    ID3D10EffectVectorVariable          *m_evGravity;
    ID3D10EffectScalarVariable          *m_evVortConfinementScale;
    ID3D10EffectScalarVariable          *m_evTimeStep;
    ID3D10EffectScalarVariable          *m_evAdvectAsTemperature;
    ID3D10EffectScalarVariable          *m_evTreatAsLiquidVelocity;
    ID3D10EffectScalarVariable          *m_evFluidType;
    ID3D10EffectVectorVariable          *m_evObstBoxLBDcorner;
    ID3D10EffectVectorVariable          *m_evObstBoxRTUcorner;
    ID3D10EffectVectorVariable          *m_evObstBoxVelocity;*/

    /*layout(binding = 0) uniform sampler2D Texture_inDensity;

    layout(binding = 1) uniform sampler3D Texture_pressure;
    layout(binding = 2) uniform sampler3D Texture_velocity;
    layout(binding = 3) uniform sampler3D Texture_vorticity;
    layout(binding = 4) uniform sampler3D Texture_divergence;

    layout(binding = 5) uniform sampler3D Texture_phi;
    layout(binding = 6) uniform sampler3D Texture_phi_hat;
    layout(binding = 7) uniform sampler3D Texture_phi_next;
    layout(binding = 8) uniform sampler3D Texture_levelset;

    layout(binding = 9) uniform sampler3D Texture_obstacles;
    layout(binding = 10) uniform sampler3D Texture_obstvelocity;*/

    private static final int  m_evTexture_pressure = 1;
    private static final int  m_evTexture_velocity = 2;
    private static final int  m_evTexture_vorticity = 3;
    private static final int  m_evTexture_divergence = 4;
    private static final int  m_evTexture_phi = 5;
    private static final int  m_evTexture_phi_hat = 6;
    private static final int  m_evTexture_phi_next = 7;
    private static final int  m_evTexture_levelset = 8;
    private static final int  m_evTexture_obstacles = 9;
    private static final int  m_evTexture_obstvelocity = 10;

    // Use this to ping-pong between two render-targets used to store the density or level set
    private int                       m_currentDstScalar = RENDER_TARGET_SCALAR0;
    private int                       m_currentSrcScalar = RENDER_TARGET_SCALAR1;


    // Simulation Parameters
    //======================
    private int  m_eFluidType = FT_SMOKE;
    private boolean m_bMouseDown;
    private boolean        m_bTreatAsLiquidVelocity = true;
    private boolean        m_bGravity;
    private boolean        m_bUseMACCORMACK;
    private int         m_nJacobiIterations = 6;
    private int         m_nDiffusionIterations = 2;
    private int         m_nVelExtrapolationIterations = 6;
    private int         m_nRedistancingIterations = 2;
    private float       m_fDensityDecay = 1.f;        // smoke/fire decay rate (1.0 - dissipation_rate)
    private float       m_fDensityViscosity;    // smoke/fire viscosity
    private float       m_fFluidViscosity;      // viscosity of the fluid (for Diffusion applied to velocity field)
    private float       m_fVorticityConfinementScale;    // vorticity confinement scale
    private float       m_t;                    // local 'time' for procedural variation

    // Emitter parameters
    //=========================
    private float       m_fSaturation;
    private float       m_fImpulseSize;
    private final Vector4f m_vImpulsePos = new Vector4f();
    private final Vector4f m_vImpulseDir = new Vector4f();


    // Liquid stream
    //==============
    private boolean     m_bLiquidStream;
    private float       m_fStreamSize;
    private final Vector4f m_streamCenter = new Vector4f();


    // Obstacles
    //===================================
    // Simulation Domain Boundary
    private boolean        m_bClosedBoundaries;    // can be open or closed
    // ObstacleBox (for testing purposes)
    private boolean        m_bDrawObstacleBox;
    private final Vector4f m_vObstBoxPos = new Vector4f();
    private final Vector4f m_vObstBoxPrevPos = new Vector4f();
    private final Vector4f m_vObstBoxVelocity = new Vector4f();

    private final FluidConstants m_constants = new FluidConstants();
    private GLFuncProvider gl;
    private RenderTargets  fbo;

    Fluid ( /*ID3D10Device* pd3dDevice*/ RenderTargets  fbo){
        this.fbo = fbo;
        final int DXGI_FORMAT_R16G16B16A16_FLOAT = GLenum.GL_RGBA16F;
        final int DXGI_FORMAT_R16_FLOAT = GLenum.GL_R16F;
        final int DXGI_FORMAT_R8_UNORM = GLenum.GL_R8;

        m_RenderTargetFormats [RENDER_TARGET_VELOCITY0]   = DXGI_FORMAT_R16G16B16A16_FLOAT;
        m_RenderTargetFormats [RENDER_TARGET_VELOCITY1]   = DXGI_FORMAT_R16G16B16A16_FLOAT;
        m_RenderTargetFormats [RENDER_TARGET_PRESSURE]    = DXGI_FORMAT_R16_FLOAT;
        m_RenderTargetFormats [RENDER_TARGET_SCALAR0]     = DXGI_FORMAT_R16_FLOAT;
        m_RenderTargetFormats [RENDER_TARGET_SCALAR1]     = DXGI_FORMAT_R16_FLOAT;
        m_RenderTargetFormats [RENDER_TARGET_OBSTACLES]   = DXGI_FORMAT_R8_UNORM;
        m_RenderTargetFormats [RENDER_TARGET_OBSTVELOCITY]= DXGI_FORMAT_R16G16B16A16_FLOAT;
        // RENDER_TARGET_TEMPSCALAR: for AdvectMACCORMACK and for Jacobi (for pressure solver)
        m_RenderTargetFormats [RENDER_TARGET_TEMPSCALAR]  = DXGI_FORMAT_R16_FLOAT;
        // RENDER_TARGET_TEMPVECTOR: for Advect (when using MACCORMACK), Vorticity and Divergence (for Pressure solver)
        m_RenderTargetFormats [RENDER_TARGET_TEMPVECTOR]  = DXGI_FORMAT_R16G16B16A16_FLOAT;
        m_RenderTargetFormats [RENDER_TARGET_TEMPVECTOR1] = DXGI_FORMAT_R16G16B16A16_FLOAT;
    }
//    virtual ~Fluid          ( void );

    void Initialize      ( int width, int height, int depth, int fluidType ){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_eFluidType = fluidType;

        try {
            LoadShaders();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*D3D10_TEXTURE3D_DESC desc;
        desc.BindFlags = D3D10_BIND_SHADER_RESOURCE | D3D10_BIND_RENDER_TARGET;
        desc.CPUAccessFlags = 0;
        desc.MipLevels = 1;
        desc.MiscFlags = 0;
        desc.Usage = D3D10_USAGE_DEFAULT;
        desc.Width =  width;
        desc.Height = height;
        desc.Depth =  depth;*/

        Texture3DDesc desc = new Texture3DDesc(width, height, depth, 1, 0);

        for(int rtIndex=0; rtIndex<NUM_RENDER_TARGETS; rtIndex++)
        {
            desc.format = m_RenderTargetFormats[rtIndex];
            CreateTextureAndViews( rtIndex, desc );
        }

        /*m_evTextureWidth->SetFloat( float(width));
        m_evTextureHeight->SetFloat(float(height));
        m_evTextureDepth->SetFloat(float(depth)));*/

        m_constants.textureWidth = width;
        m_constants.textureHeight = height;
        m_constants.textureDepth = depth;


        m_pGrid = new Grid( );


        m_pGrid.Initialize( width, height, depth, m_etAdvect );

        Reset();

        // Simulation variables
        m_bMouseDown = false;
        m_bGravity = true;
        m_bTreatAsLiquidVelocity = true;
        m_bUseMACCORMACK = true;
        m_nJacobiIterations = 6;
        m_nDiffusionIterations = 2;
        m_nVelExtrapolationIterations = 10;
        m_nRedistancingIterations = 11;
        m_fDensityDecay = 1.0f;
        m_fDensityViscosity = 0.0f;
        m_fFluidViscosity = 0.0f;
        m_fVorticityConfinementScale = 0.0f;
        m_t = 0;

        m_fSaturation =  0.78f;
        m_fImpulseSize = 0.0f;
        m_vImpulsePos.x = (float)(m_pGrid.GetDimX() / 2);
        m_vImpulsePos.y = 0;
        m_vImpulsePos.z = (float)(m_pGrid.GetDimZ() / 2);
        m_vImpulsePos.w = 1;
        m_vImpulseDir.x = 0;
        m_vImpulseDir.y = 1;
        m_vImpulseDir.z = 0;
        m_vImpulseDir.w = 1;

        m_bLiquidStream = true;

        m_bClosedBoundaries = false;

        m_bDrawObstacleBox = false;
        m_vObstBoxPos.set(0, 0, 0, 0);
        m_vObstBoxPrevPos.set(0, 0, 0, 0);
        m_vObstBoxVelocity.set(0, 0, 0, 0);


        if( m_eFluidType == FT_LIQUID )
        {
            m_fImpulseSize = 0.20f;

            m_fStreamSize = 3.5f;
            m_streamCenter .set((m_pGrid.GetDimX()/2 - 3)  , (m_pGrid.GetDimY()/2 - 20), (m_pGrid.GetDimZ()/2 + 12), 1.0f);

            // Used closed boundaries for water
            m_bClosedBoundaries = true;
            // FOR TESTING PURPOSES
            // disable velocity advection only within the levelset (advect everywhere)
//            m_bTreatAsLiquidVelocity = false;
            // disable velicity extrapolation out of the level set
//            m_nVelExtrapolationIterations = 2;
        }
        else
        {
            m_fImpulseSize = 0.15f;

            m_bGravity = false;

            m_bLiquidStream = false;
        }
    }

    static int currentSrcVelocity = RENDER_TARGET_VELOCITY0;
    static int currentDstVelocity = RENDER_TARGET_VELOCITY1;

    // TODO: this is not really working very well, some flickering occurrs for timesteps less than 2.0
    // Addition of external density/liquid and external forces
    static float elapsedTimeSinceLastUpdate = 2.0f;

    void Update ( float timestep, boolean bUseMACCORMACKVelocity/*=true*/, boolean bUseSuppliedParameters /*= false*/,
                  float fInputVorticityConfinementScale/*=0*/, float fInputDensityDecay/*=0.9999f*/, float fInputDensityViscosity/*=0*/,
                  float fInputFluidViscosity/*=0*/,float fInputImpulseSize/*=0*/, float randCenterScale /*= 1.0*/){
        // All drawing will take place to a viewport with the dimensions of a 3D texture slice
        /*D3D10_VIEWPORT rtViewport;
        rtViewport.TopLeftX = 0;
        rtViewport.TopLeftY = 0;
        rtViewport.MinDepth = 0;
        rtViewport.MaxDepth = 1;
        rtViewport.Width =  GetTextureWidth();
        rtViewport.Height = GetTextureHeight();
        m_pD3DDevice->RSSetViewports(1,&rtViewport);*/
        gl.glViewport(0,0, GetTextureWidth(), GetTextureHeight());

        // If the mouse is not being used, set fixed emitter position and direction
        if( !m_bMouseDown )
        {
            // If there is no user interaction with the mouse
            //  emit with some initial velocity from a fixed location in the volume
            // the emitter location
            m_vImpulsePos.x = m_pGrid.GetDimX() * 0.1f;
            m_vImpulsePos.y = m_pGrid.GetDimY() * 0.9f;
            m_vImpulsePos.z = m_pGrid.GetDimZ() * 0.25f;
            // the emitter direction and initial velocity
            float impulseStrength = 0.8f;
            m_vImpulseDir.x = 0.5f * impulseStrength;
            m_vImpulseDir.y = -0.5f * impulseStrength;
            m_vImpulseDir.z = 0.8f * impulseStrength;
        }


        // Hard-coded obstacle box used for testing purposes:
        {
            // Update the obstacle box velocity based on its movement
            {
//                m_vObstBoxVelocity = (m_vObstBoxPos - m_vObstBoxPrevPos) / timestep;
                Vector4f.sub(m_vObstBoxPos, m_vObstBoxPrevPos, m_vObstBoxVelocity).scale(1.0f/timestep);

                // Exagerate the velocity a bit to give more momentum to the fluid
//                m_vObstBoxVelocity *= 1.5f;
                m_vObstBoxVelocity.scale(1.5f);
                // Scale m_vObstBoxVelocity to voxel space
                m_vObstBoxVelocity.x *= m_pGrid.GetDimX(); m_vObstBoxVelocity.y *= m_pGrid.GetDimY(); m_vObstBoxVelocity.z *= m_pGrid.GetDimZ();
//                m_evObstBoxVelocity.SetFloatVector(m_vObstBoxVelocity);
                m_constants.obstBoxVelocity.set(m_vObstBoxVelocity);
                m_vObstBoxPrevPos.set(m_vObstBoxPos);
            }

            if( m_bDrawObstacleBox )
            {
                DrawObstacleTestBox( RENDER_TARGET_OBSTACLES, RENDER_TARGET_OBSTVELOCITY );
            }
        }

        if( m_bClosedBoundaries )
        {
            DrawObstacleBoundaryBox( RENDER_TARGET_OBSTACLES, RENDER_TARGET_OBSTVELOCITY );
        }



        // Set vorticity confinment and decay parameters
        // (m_bUserMACCORMACK may change dynamically based on user input, so we set these variables at every frame)
        if( bUseSuppliedParameters)
        {
            m_fVorticityConfinementScale = fInputVorticityConfinementScale;
            m_fDensityDecay = fInputDensityDecay;
            m_fDensityViscosity = fInputDensityViscosity;
            m_fFluidViscosity =  fInputFluidViscosity;
        }
        else if( m_bUseMACCORMACK )
        {
            if(m_eFluidType == FT_FIRE)
            {
                m_fVorticityConfinementScale = 0.03f;
                m_fDensityDecay = 0.9995f;
            }
            else
            {
                //m_fVorticityConfinementScale = 0.02f;
                //m_fDensityDecay = 0.994f;
                m_fVorticityConfinementScale = 0.0085f;
                m_fDensityDecay = 1.0f;
            }

            // use Diffusion
            m_fDensityViscosity = 0.0003f;
            m_fFluidViscosity =  0.000025f;
        }
        else
        {
            m_fVorticityConfinementScale = 0.12f;
            m_fDensityDecay = 0.9999f;
            // no Diffusion
            m_fDensityViscosity = 0.0f;
            m_fFluidViscosity =  0.0f;
        }

        if( m_eFluidType == FT_LIQUID )
        {
            // Use no decay for Level Set advection (liquids)
            m_fDensityDecay = 1.0f;
            // no diffusion applied to level set advection
            m_fDensityViscosity = 0.0f;
        }

        // we want the rate of decay to be the same with any timestep (since we tweaked with timestep = 2.0 we use 2.0 here)
        double dDensityDecay = Math.pow((double)m_fDensityDecay, (double)(timestep/2.0));
        float fDensityDecay = (float) dDensityDecay;


//        m_evFluidType->SetInt( m_eFluidType );
        m_constants.fluidType = m_eFluidType;

        // Advection of Density or Level set
        boolean bAdvectAsTemperature = (m_eFluidType == FT_FIRE);
        if( m_bUseMACCORMACK )
        {
            // Advect forward to get \phi^(n+1)
            Advect(  timestep, 1.0f, false, bAdvectAsTemperature, RENDER_TARGET_TEMPVECTOR, currentSrcVelocity, m_currentSrcScalar, RENDER_TARGET_OBSTACLES, m_currentSrcScalar );
            // Advect back to get \bar{\phi}
            Advect( -timestep, 1.0f, false, bAdvectAsTemperature, RENDER_TARGET_TEMPSCALAR, currentSrcVelocity, RENDER_TARGET_TEMPVECTOR, RENDER_TARGET_OBSTACLES, m_currentSrcScalar );
            // Advect forward but use the MACCORMACK advection shader which uses both \phi and \bar{\phi}
            //  as source quantity  (specifically, (3/2)\phi^n - (1/2)\bar{\phi})
            AdvectMACCORMACK( timestep, fDensityDecay, false, bAdvectAsTemperature, m_currentDstScalar, currentSrcVelocity, m_currentSrcScalar, RENDER_TARGET_TEMPSCALAR, RENDER_TARGET_OBSTACLES, m_currentSrcScalar );
        }
        else
        {
            Advect( timestep, fDensityDecay, false, bAdvectAsTemperature, m_currentDstScalar, currentSrcVelocity, m_currentSrcScalar, RENDER_TARGET_OBSTACLES, m_currentSrcScalar );
        }

        // Advection of Velocity
        boolean bAdvectAsLiquidVelocity = (m_eFluidType == FT_LIQUID) && m_bTreatAsLiquidVelocity;


        if( m_bUseMACCORMACK && bUseMACCORMACKVelocity)
        {
            // Advect forward to get \phi^(n+1)
            Advect(  timestep, 1.0f, bAdvectAsLiquidVelocity, false, RENDER_TARGET_TEMPVECTOR, currentSrcVelocity, currentSrcVelocity, RENDER_TARGET_OBSTACLES, m_currentDstScalar );

            // Advect back to get \bar{\phi}
            Advect( -timestep, 1.0f, bAdvectAsLiquidVelocity, false, RENDER_TARGET_TEMPVECTOR1, currentSrcVelocity, RENDER_TARGET_TEMPVECTOR, RENDER_TARGET_OBSTACLES, m_currentDstScalar );

            // Advect forward but use the MACCORMACK advection shader which uses both \phi and \bar{\phi}
            //  as source quantity  (specifically, (3/2)\phi^n - (1/2)\bar{\phi})
            AdvectMACCORMACK( timestep, 1.0f, bAdvectAsLiquidVelocity, false, currentDstVelocity, currentSrcVelocity, currentSrcVelocity, RENDER_TARGET_TEMPVECTOR1, RENDER_TARGET_OBSTACLES, m_currentDstScalar );
        }
        else
        {
            Advect( timestep, 1.0f, bAdvectAsLiquidVelocity, false, currentDstVelocity, currentSrcVelocity, currentSrcVelocity, RENDER_TARGET_OBSTACLES, m_currentDstScalar );
        }

        // Diffusion of Density for smoke
        if( (m_fDensityViscosity > 0.0f) && (m_eFluidType == FT_SMOKE) )
        {
//            swap(m_currentDstScalar, m_currentSrcScalar);
            {
                int a = m_currentDstScalar;
                m_currentDstScalar = m_currentSrcScalar;
                m_currentSrcScalar = a;
            }

            Diffuse( timestep, m_fDensityViscosity, m_currentDstScalar, m_currentSrcScalar, RENDER_TARGET_TEMPSCALAR, RENDER_TARGET_OBSTACLES );
        }
        // Diffusion of Velocity field
        if( m_fFluidViscosity > 0.0f )
        {
//            swap(currentDstVelocity, currentSrcVelocity);
            {
                int a = currentDstVelocity;
                currentDstVelocity = currentSrcVelocity;
                currentSrcVelocity = a;
            }
            Diffuse( timestep, m_fFluidViscosity, currentDstVelocity, currentSrcVelocity, RENDER_TARGET_TEMPVECTOR, RENDER_TARGET_OBSTACLES );
        }


        // Vorticity confinement
        if( m_fVorticityConfinementScale > 0.0f )
        {
            ComputeVorticity( RENDER_TARGET_TEMPVECTOR, currentDstVelocity);
            ApplyVorticityConfinement( timestep, currentDstVelocity, RENDER_TARGET_TEMPVECTOR, m_currentDstScalar, RENDER_TARGET_OBSTACLES);
        }

        if( elapsedTimeSinceLastUpdate >= 2.0f )
        {
            AddNewMatter( timestep, m_currentDstScalar, RENDER_TARGET_OBSTACLES );
            if(bUseSuppliedParameters)
                AddExternalForces( timestep, currentDstVelocity, RENDER_TARGET_OBSTACLES, m_currentDstScalar,fInputImpulseSize, randCenterScale );
            else
                AddExternalForces( timestep, currentDstVelocity, RENDER_TARGET_OBSTACLES, m_currentDstScalar,m_fImpulseSize, 1 );

            elapsedTimeSinceLastUpdate = 0.0f;
        }
        elapsedTimeSinceLastUpdate += timestep;

        // Pressure projection
        ComputeVelocityDivergence( RENDER_TARGET_TEMPVECTOR, currentDstVelocity, RENDER_TARGET_OBSTACLES, RENDER_TARGET_OBSTVELOCITY);
        ComputePressure( RENDER_TARGET_PRESSURE, RENDER_TARGET_TEMPSCALAR, RENDER_TARGET_TEMPVECTOR, RENDER_TARGET_OBSTACLES, m_currentDstScalar);
        ProjectVelocity( currentSrcVelocity, RENDER_TARGET_PRESSURE, currentDstVelocity, RENDER_TARGET_OBSTACLES, RENDER_TARGET_OBSTVELOCITY, m_currentDstScalar);

//#if 1
        if( m_eFluidType == FT_LIQUID )
        {
            // Redistancing of level set (only for liquids)
            RedistanceLevelSet(m_currentSrcScalar, m_currentDstScalar, RENDER_TARGET_TEMPSCALAR);
            // Velocity extrapolation: from the surface along the level set gradient
            ExtrapolateVelocity(currentSrcVelocity, RENDER_TARGET_TEMPVECTOR, RENDER_TARGET_OBSTACLES, m_currentSrcScalar);
        }
        else
//#endif
        {
            // Swap scalar textures (we ping-pong between them for advection purposes)
//            swap(m_currentDstScalar, m_currentSrcScalar);
            int a = m_currentDstScalar;
            m_currentDstScalar = m_currentSrcScalar;
            m_currentSrcScalar = a;
        }
    }

//    void Render3D           ( void );

    void Draw            ( int field )
    {
        /*HRESULT hr(S_OK);
        D3D10_VIEWPORT rtViewport;
        rtViewport.TopLeftX = 0;
        rtViewport.TopLeftY = 0;
        rtViewport.MinDepth = 0;
        rtViewport.MaxDepth = 1;
        rtViewport.Width = g_Width;
        rtViewport.Height = g_Height;
        m_pD3DDevice->RSSetViewports(1,&rtViewport);*/
        gl.glViewport(0,0,SmokeDemo.g_Width, SmokeDemo.g_Height);

        /*ID3D10RenderTargetView* pRTV = DXUTGetD3D10RenderTargetView();
        m_pD3DDevice->OMSetRenderTargets( 1, &pRTV , NULL );*/
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

//        m_evDrawTexture->SetInt( field );
        m_constants.drawTextureNumber = field;

        // Set resources and apply technique
        /*m_evTexture_phi->SetResource(m_pShaderResourceViews[m_currentDstScalar]);
        m_evTexture_velocity->SetResource(m_pShaderResourceViews[RENDER_TARGET_VELOCITY0]);
        m_evTexture_obstacles->SetResource(m_pShaderResourceViews[RENDER_TARGET_OBSTACLES]);
        m_evTexture_obstvelocity->SetResource(m_pShaderResourceViews[RENDER_TARGET_OBSTVELOCITY]);
        m_evTexture_pressure->SetResource(m_pShaderResourceViews[RENDER_TARGET_PRESSURE]);*/
        gl.glBindTextureUnit(m_evTexture_phi, m_pShaderResourceViews[m_currentDstScalar].getTexture());
        gl.glBindTextureUnit(m_evTexture_velocity, m_pShaderResourceViews[RENDER_TARGET_VELOCITY0].getTexture());
        gl.glBindTextureUnit(m_evTexture_obstacles, m_pShaderResourceViews[RENDER_TARGET_OBSTACLES].getTexture());
        gl.glBindTextureUnit(m_evTexture_obstvelocity, m_pShaderResourceViews[RENDER_TARGET_OBSTVELOCITY].getTexture());
        gl.glBindTextureUnit(m_evTexture_pressure, m_pShaderResourceViews[RENDER_TARGET_PRESSURE].getTexture());

//        m_etDrawTexture->GetPassByIndex(0)->Apply(0));
        m_etDrawTexture.enable();

        m_pGrid.DrawSlicesToScreen();

        // Unset resources and apply technique (so that the resource is actually unbound)
        /*m_evTexture_phi->SetResource(NULL);
        m_evTexture_velocity->SetResource(NULL);
        m_evTexture_obstacles->SetResource(NULL);
        m_evTexture_obstvelocity->SetResource(NULL);
        m_evTexture_pressure->SetResource(NULL);*/
        gl.glBindTextureUnit(m_evTexture_phi, 0);
        gl.glBindTextureUnit(m_evTexture_velocity, 0);
        gl.glBindTextureUnit(m_evTexture_obstacles, 0);
        gl.glBindTextureUnit(m_evTexture_obstvelocity, 0);
        gl.glBindTextureUnit(m_evTexture_pressure, 0);

//        V_RETURN(m_etDrawTexture->GetPassByIndex(0)->Apply(0));
//        m_etDrawTexture.enable();
    }

    void Impulse            ( int x, int y, int z, float dX, float dY, float dZ ){
        m_vImpulsePos.x  = (float)x;
        m_vImpulsePos.y  = (float)y;
        m_vImpulsePos.z  = (float)z;
        m_vImpulseDir.x = dX;
        m_vImpulseDir.y = dY;
        m_vImpulseDir.z = dZ;
    }

    private final void ClearRenderTargetView(TextureGL texture, float[] values){
        if(values == null){
            gl.glClearTexImage(texture.getTexture(), 0, TextureUtils.measureFormat(texture.getFormat()), TextureUtils.measureDataType(texture.getFormat()), null);
        }else {
            gl.glClearTexImage(texture.getTexture(), 0, TextureUtils.measureFormat(texture.getFormat()), TextureUtils.measureDataType(texture.getFormat()), CacheBuffer.wrap(values));
        }
    }

    void Reset              ( ){
        float zero[/*4*/] = /*{0, 0, 0, 0 }*/ null;
        final float OBSTACLE_EXTERIOR = 1;
        float obstacle_exterior[/*4*/] = { OBSTACLE_EXTERIOR, OBSTACLE_EXTERIOR, OBSTACLE_EXTERIOR, OBSTACLE_EXTERIOR };
        m_currentDstScalar = RENDER_TARGET_SCALAR0;
        m_currentSrcScalar = RENDER_TARGET_SCALAR1;
        m_t=0;
        ClearRenderTargetView( m_pRenderTargetViews[RENDER_TARGET_VELOCITY0], zero );
        ClearRenderTargetView( m_pRenderTargetViews[RENDER_TARGET_VELOCITY1], zero );
        ClearRenderTargetView( m_pRenderTargetViews[RENDER_TARGET_PRESSURE], zero );
        ClearRenderTargetView( m_pRenderTargetViews[RENDER_TARGET_SCALAR0], zero );
        ClearRenderTargetView( m_pRenderTargetViews[RENDER_TARGET_SCALAR1], zero );
        ClearRenderTargetView( m_pRenderTargetViews[RENDER_TARGET_OBSTACLES], obstacle_exterior );
        ClearRenderTargetView( m_pRenderTargetViews[RENDER_TARGET_OBSTVELOCITY], zero );
        ClearRenderTargetView( m_pRenderTargetViews[RENDER_TARGET_TEMPSCALAR], zero );
        ClearRenderTargetView( m_pRenderTargetViews[RENDER_TARGET_TEMPVECTOR], zero );


        if( m_eFluidType == FT_LIQUID )
        {
            // All drawing will take place to a viewport with the dimensions of a 3D texture slice
            /*D3D10_VIEWPORT rtViewport;
            rtViewport.TopLeftX = 0;
            rtViewport.TopLeftY = 0;
            rtViewport.MinDepth = 0;
            rtViewport.MaxDepth = 1;
            rtViewport.Width =  GetTextureWidth();
            rtViewport.Height = GetTextureHeight();
            m_pD3DDevice->RSSetViewports(1,&rtViewport);*/
            gl.glViewport(0,0,GetTextureWidth(), GetTextureHeight());

            SetRenderTarget( RENDER_TARGET_SCALAR0 );
            m_etInitLevelSetToLiquidHeight/*->GetPassByIndex(0)->Apply(0)*/.enable();
            m_pGrid.DrawSlices();
//            m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);  TODO

            SetRenderTarget( RENDER_TARGET_SCALAR1 );
            m_etInitLevelSetToLiquidHeight.enable();
            m_pGrid.DrawSlices();
//            m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);  TODO
        }
    }

    int GetTextureWidth     (  ) { return m_pGrid.GetDimX();}
    int GetTextureHeight    (  ) { return m_pGrid.GetDimY();}
    int GetGridCols         (  ) { return m_pGrid.GetCols();}
    int GetGridRows         (  ) { return m_pGrid.GetRows();}
    Texture3D Get3DTexture(int rt) {return m_p3DTextures[rt];};

    void SetObstaclePositionInNormalizedGrid( float x, float y, float z ){
        m_vObstBoxPos.set(x, y, z, 1);
        Vector3f pos = new Vector3f(x, y, z);

        Vector3f boxHDims = new Vector3f(0.2f * 0.5f, 0.3f * 0.5f, 0.3f * 0.5f);
        Vector3f boxLBDcorner = Vector3f.sub(pos, boxHDims, null);
        boxLBDcorner.x *= m_pGrid.GetDimX(); boxLBDcorner.y *= m_pGrid.GetDimY(); boxLBDcorner.z *= m_pGrid.GetDimZ();
        Vector3f boxRTUcorner = Vector3f.add(pos, boxHDims, null);
        boxRTUcorner.x *= m_pGrid.GetDimX(); boxRTUcorner.y *= m_pGrid.GetDimY(); boxRTUcorner.z *= m_pGrid.GetDimZ();

//        m_evObstBoxLBDcorner.SetFloatVector(boxLBDcorner);
//        m_evObstBoxRTUcorner.SetFloatVector(boxRTUcorner);

        m_constants.obstBoxLBDcorner.set(boxLBDcorner);
        m_constants.obstBoxRTUcorner.set(boxRTUcorner);
        m_bDrawObstacleBox = true;
    }

    void SetUseMACCORMACK       (boolean b)    { m_bUseMACCORMACK = b; }
    void SetNumJacobiIterations (int i)     { m_nJacobiIterations = i;    }
    void SetMouseDown           (boolean b)    { m_bMouseDown = b;     }
    void SetEnableGravity       (boolean b)    { m_bGravity = b;       }
    void SetEnableLiquidStream  (boolean b)    { m_bLiquidStream = b;   }

    boolean GetUseMACCORMACK       (  )    { return m_bUseMACCORMACK;  }
    int  GetNumJacobiIterations (  )    { return m_nJacobiIterations;     }
    boolean GetMouseDown           (  )    { return m_bMouseDown;      }
    boolean GetEnableGravity       (  )    { return m_bGravity;        }
    boolean GetEnableLiquidStream  (  )    { return m_bLiquidStream;    }
    int GetDimX                 (  )    { if(m_pGrid == null) return 0; return m_pGrid.GetDimX(); };
    int GetDimY                 (  )    { if(m_pGrid == null) return 0; return m_pGrid.GetDimY(); };
    int GetDimZ                 (  )    { if(m_pGrid == null) return 0; return m_pGrid.GetDimZ(); };

    int GetFormat       ( int rt ) {return m_RenderTargetFormats[rt];};
    TextureGL getCurrentShaderResourceView() {return m_pShaderResourceViews[m_currentSrcScalar]; }
    void drawGridSlices         ( )    {m_pGrid.DrawSlices(); };

    private void LoadShaders(  ) throws IOException {
        Runnable NoBlending = ()->
        {
            gl.glDisable(GLenum.GL_BLEND);
        };

        Runnable AdditiveBlending = ()->
        {
            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE);
        };

        Runnable AlphaBlending = ()->
        {
            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendFuncSeparate(GLenum.GL_SRC_ALPHA, GLenum.GL_ONE_MINUS_SRC_ALPHA, GLenum.GL_ONE, GLenum.GL_ONE);
        };

        Runnable DisableDepth = ()->
        {
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glDisable(GLenum.GL_STENCIL_TEST);
        };

        Runnable CullBack = ()->
        {
            gl.glEnable(GLenum.GL_CULL_FACE);
            gl.glCullFace(GLenum.GL_BACK);
            gl.glFrontFace(GLenum.GL_CCW);
        };

        Runnable CullNone = ()->
        {
            gl.glDisable(GLenum.GL_CULL_FACE);
        };

        m_etAdvect = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_ADVECT.frag");
        m_etAdvect.setBlendState(NoBlending);
        m_etAdvect.setDepthStencilState(DisableDepth);
        m_etAdvect.setRasterizerState(CullNone);

        m_etAdvectMACCORMACK = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_ADVECT_MACCORMACK.frag");
        m_etAdvectMACCORMACK.setBlendState(NoBlending);
        m_etAdvectMACCORMACK.setDepthStencilState(DisableDepth);
        m_etAdvectMACCORMACK.setRasterizerState(CullNone);

        m_etVorticity = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_VORTICITY.frag");
        m_etVorticity.setBlendState(NoBlending);
        m_etVorticity.setDepthStencilState(DisableDepth);
        m_etVorticity.setRasterizerState(CullNone);

        m_etConfinement = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_CONFINEMENT.frag");
        m_etConfinement.setBlendState(AdditiveBlending);
        m_etConfinement.setDepthStencilState(DisableDepth);
        m_etConfinement.setRasterizerState(CullNone);

        m_etDiffuse = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_DIFFUSE.frag");
        m_etDiffuse.setBlendState(NoBlending);
        m_etDiffuse.setDepthStencilState(DisableDepth);
        m_etDiffuse.setRasterizerState(CullNone);

        m_etDivergence = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_DIVERGENCE.frag");
        m_etDivergence.setBlendState(NoBlending);
        m_etDivergence.setDepthStencilState(DisableDepth);
        m_etDivergence.setRasterizerState(CullNone);

        m_etScalarJacobi = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_SCALAR_JACOBI.frag");
        m_etScalarJacobi.setBlendState(NoBlending);
        m_etScalarJacobi.setDepthStencilState(DisableDepth);
        m_etScalarJacobi.setRasterizerState(CullNone);

        m_etProject = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_PROJECT.frag");
        m_etProject.setBlendState(NoBlending);
        m_etProject.setDepthStencilState(DisableDepth);
        m_etProject.setRasterizerState(CullNone);

        m_etInitLevelSetToLiquidHeight = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_SIGNED_DISTANCE_TO_LIQUIDHEIGHT.frag");
        m_etInitLevelSetToLiquidHeight.setBlendState(NoBlending);
        m_etInitLevelSetToLiquidHeight.setDepthStencilState(DisableDepth);
        m_etInitLevelSetToLiquidHeight.setRasterizerState(CullNone);

        m_etInjectLiquid = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_INJECT_LIQUID.frag");
        m_etInjectLiquid.setBlendState(AdditiveBlending);
        m_etInjectLiquid.setDepthStencilState(DisableDepth);
        m_etInjectLiquid.setRasterizerState(CullNone);

        m_etAirPressure = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_APPLY_AIR_PRESSURE.frag");
        m_etAirPressure.setBlendState(AlphaBlending);
        m_etAirPressure.setDepthStencilState(DisableDepth);
        m_etAirPressure.setRasterizerState(CullNone);

        m_etRedistance = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_REDISTANCING.frag");
        m_etRedistance.setBlendState(NoBlending);
        m_etRedistance.setDepthStencilState(DisableDepth);
        m_etRedistance.setRasterizerState(CullNone);

        m_etExtrapolateVelocity = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_EXTRAPOLATE_VELOCITY.frag");
        m_etExtrapolateVelocity.setBlendState(NoBlending);
        m_etExtrapolateVelocity.setDepthStencilState(DisableDepth);
        m_etExtrapolateVelocity.setRasterizerState(CullNone);

        m_etLiquidStream_LevelSet = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_EXTRAPOLATE_VELOCITY.frag", new Macro("outputColor", 0));
        m_etLiquidStream_LevelSet.setBlendState(AlphaBlending);
        m_etLiquidStream_LevelSet.setDepthStencilState(DisableDepth);
        m_etLiquidStream_LevelSet.setRasterizerState(CullNone);

        m_etLiquidStream_Velocity = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_EXTRAPOLATE_VELOCITY.frag", new Macro("outputColor", 1));
        m_etLiquidStream_Velocity.setBlendState(AlphaBlending);
        m_etLiquidStream_Velocity.setDepthStencilState(DisableDepth);
        m_etLiquidStream_Velocity.setRasterizerState(CullNone);

        m_etGravity = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_APPLY_GRAVITY.frag");
        m_etGravity.setBlendState(AdditiveBlending);
        m_etGravity.setDepthStencilState(DisableDepth);
        m_etGravity.setRasterizerState(CullNone);

        m_etGaussian = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_GAUSSIAN.frag");
        m_etGaussian.setBlendState(AlphaBlending);
        m_etGaussian.setDepthStencilState(DisableDepth);
        m_etGaussian.setRasterizerState(CullNone);

        m_etCopyTextureDensity = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_COPY_TEXURE.frag");
        m_etCopyTextureDensity.setBlendState(NoBlending);
        m_etCopyTextureDensity.setDepthStencilState(DisableDepth);
        m_etCopyTextureDensity.setRasterizerState(CullNone);

        m_etAddDensityDerivativeVelocity = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_ADD_DERIVATIVE_VEL.frag");
        m_etAddDensityDerivativeVelocity.setBlendState(AdditiveBlending);
        m_etAddDensityDerivativeVelocity.setDepthStencilState(DisableDepth);
        m_etAddDensityDerivativeVelocity.setRasterizerState(CullNone);

        m_etStaticObstacleTriangles = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_STATIC_OBSTACLE.frag");
        m_etStaticObstacleTriangles.setBlendState(NoBlending);
        m_etStaticObstacleTriangles.setDepthStencilState(DisableDepth);
        m_etStaticObstacleTriangles.setRasterizerState(CullBack);

        m_etStaticObstacleLines = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY_LINE.gemo", "PS_STATIC_OBSTACLE.frag");
        m_etStaticObstacleLines.setBlendState(NoBlending);
        m_etStaticObstacleLines.setDepthStencilState(DisableDepth);
        m_etStaticObstacleLines.setRasterizerState(CullBack);

        m_etDrawBox = new FluidSimulatorProgram("VS_GRID.vert", "GS_ARRAY.gemo", "PS_DYNAMIC_OBSTACLE_BOX.frag");
        m_etDrawBox.setBlendState(NoBlending);
        m_etDrawBox.setDepthStencilState(DisableDepth);
        m_etDrawBox.setRasterizerState(CullBack);

        m_etDrawTexture = new FluidSimulatorProgram("VS_GRID.vert", "PS_DRAW_TEXTURE.frag");
        m_etDrawTexture.setBlendState(NoBlending);
        m_etDrawTexture.setDepthStencilState(DisableDepth);
        m_etDrawTexture.setRasterizerState(CullNone);

        int textureID = NvImage.uploadTextureFromDDSFile("nvidia/Smoke/textures/fireBase.dds");
        m_FireBase = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, textureID);
    }

    private void DrawObstacleTestBox      ( int dstObst, int dstObstVel ){
        // Draw a box into the obstacles 3D textures.
        // In RENDER_TARGET_OBSTACLES, cells inside the obstacle will be grey
        //   (0.5 at the boundary and 1.0 if within boundary inside), and cells outside will be black
        // In RENDER_TARGET_OBSTVELOCITY, cell at the boundary will have a defined velocity,
        //  while cells inside or outside will have undefined velocity (likely to be set to 0)

        TextureGL pObstRenderTargets[] = {
                m_pRenderTargetViews[dstObst],
                m_pRenderTargetViews[dstObstVel]
        };
//        m_pD3DDevice->OMSetRenderTargets( 2, pObstRenderTargets, NULL );
        fbo.bind();
        fbo.setRenderTextures(pObstRenderTargets, null);

        m_etDrawBox/*->GetPassByIndex(0)->Apply(0)*/.enable();
        m_pGrid.DrawSlices();

//        m_pD3DDevice->OMSetRenderTargets( 0, NULL, NULL );
        fbo.unbind();
    }

    private void DrawObstacleBoundaryBox( int dstObst, int dstObstVel ){
        // Draw the boundary walls of the box into the obstacles 3D textures.
        // In RENDER_TARGET_OBSTACLES, cells inside the obstacle will be grey
        //   (0.5 at the boundary and 1.0 if within boundary inside), and cells outside will be black
        // In RENDER_TARGET_OBSTVELOCITY, cell at the boundary will have a defined velocity,
        //  while cells inside or outside will have undefined velocity (likely to be set to 0)

        TextureGL pObstRenderTargets[] = {
                m_pRenderTargetViews[dstObst],
                m_pRenderTargetViews[dstObstVel]
        };
//        m_pD3DDevice->OMSetRenderTargets( 2, pObstRenderTargets, NULL );
        fbo.bind();
        fbo.setRenderTextures(pObstRenderTargets, null);

        m_etStaticObstacleTriangles.enable();
        m_pGrid.DrawBoundaryQuads();

        m_etStaticObstacleLines.enable();
        m_pGrid.DrawBoundaryLines();

//        m_pD3DDevice->OMSetRenderTargets( 0, NULL, NULL );
        fbo.unbind();
    }

    private void AdvectMACCORMACK         ( float timestep, float decay, boolean bAsLiquidVelocity, boolean bAsTemperature, int dstPhi, int srcVel, int srcPhi, int srcPhi_hat, int srcObst, int srcLevelSet ){
        // Advect forward but use the MACCORMACK advection shader which uses both \phi and \bar{\phi}
        //  as source quantity  (specifically, (3/2)\phi^n - (1/2)\bar{\phi})
        /*m_evAdvectAsTemperature->SetBool(bAsTemperature);
        m_evTimeStep->SetFloat(timestep);
        m_evDecay->SetFloat(decay);
        m_evTexture_velocity->SetResource( m_pShaderResourceViews[srcVel] );
        m_evTexture_phi->SetResource( m_pShaderResourceViews[srcPhi] );
        m_evTexture_phi_hat->SetResource( m_pShaderResourceViews[srcPhi_hat] );
        m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );
        m_evTreatAsLiquidVelocity->SetBool( bAsLiquidVelocity );
        m_evTexture_levelset->SetResource( m_pShaderResourceViews[srcLevelSet] );*/
        m_constants.advectAsTemperature = bAsTemperature;
        m_constants.timestep = timestep;
        m_constants.decay = decay;
        m_constants.treatAsLiquidVelocity = bAsLiquidVelocity;

        gl.glBindTextureUnit(m_evTexture_velocity, m_pShaderResourceViews[srcVel].getTexture());
        gl.glBindTextureUnit(m_evTexture_phi, m_pShaderResourceViews[srcPhi].getTexture());
        gl.glBindTextureUnit(m_evTexture_phi_hat, m_pShaderResourceViews[srcPhi_hat].getTexture());
        gl.glBindTextureUnit(m_evTexture_obstacles, m_pShaderResourceViews[srcObst].getTexture());
        gl.glBindTextureUnit(m_evTexture_levelset, m_pShaderResourceViews[srcLevelSet].getTexture());

        m_etAdvectMACCORMACK.enable();
        SetRenderTarget( dstPhi );

        m_pGrid.DrawSlices();

        /*m_evTexture_velocity->SetResource( NULL );
        m_evTexture_phi->SetResource( NULL );
        m_evTexture_phi_hat->SetResource( NULL );
        m_evTexture_obstacles->SetResource( NULL );
        m_evTreatAsLiquidVelocity->SetBool( false );
        m_evTexture_levelset->SetResource( NULL );
        m_etAdvectMACCORMACK->GetPassByIndex(0)->Apply(0);
        m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);*/
        gl.glBindTextureUnit(m_evTexture_velocity, 0);
        gl.glBindTextureUnit(m_evTexture_phi, 0);
        gl.glBindTextureUnit(m_evTexture_obstacles, 0);
        gl.glBindTextureUnit(m_evTexture_levelset, 0);
        gl.glBindTextureUnit(m_evTexture_phi_hat, 0);
        m_constants.treatAsLiquidVelocity = false;
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
    }

    private void Advect                   ( float timestep, float decay, boolean bAsLiquidVelocity, boolean bAsTemperature, int dstPhi, int srcVel, int srcPhi, int srcObst, int srcLevelSet ){
        /*m_evAdvectAsTemperature->SetBool(bAsTemperature);
        m_evTimeStep->SetFloat(timestep);
        m_evDecay->SetFloat(decay);
        m_evTexture_velocity->SetResource( m_pShaderResourceViews[srcVel] );
        m_evTexture_phi->SetResource( m_pShaderResourceViews[srcPhi] );
        m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );
        m_evTreatAsLiquidVelocity->SetBool( bAsLiquidVelocity );
        m_evTexture_levelset->SetResource( m_pShaderResourceViews[srcLevelSet] );*/
        m_constants.advectAsTemperature = bAsTemperature;
        m_constants.timestep = timestep;
        m_constants.decay = decay;
        m_constants.treatAsLiquidVelocity = bAsLiquidVelocity;

        gl.glBindTextureUnit(m_evTexture_velocity, m_pShaderResourceViews[srcVel].getTexture());
        gl.glBindTextureUnit(m_evTexture_phi, m_pShaderResourceViews[srcPhi].getTexture());
        gl.glBindTextureUnit(m_evTexture_obstacles, m_pShaderResourceViews[srcObst].getTexture());
        gl.glBindTextureUnit(m_evTexture_levelset, m_pShaderResourceViews[srcLevelSet].getTexture());

        m_etAdvect.enable();
        SetRenderTarget( dstPhi );

        m_pGrid.DrawSlices();

        /*m_evTexture_velocity->SetResource( NULL );
        m_evTexture_phi->SetResource( NULL );
        m_evTexture_obstacles->SetResource( NULL );
        m_evTreatAsLiquidVelocity->SetBool( false );
        m_evTexture_levelset->SetResource( NULL );
        m_etAdvect->GetPassByIndex(0)->Apply(0);
        m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);*/

        gl.glBindTextureUnit(m_evTexture_velocity, 0);
        gl.glBindTextureUnit(m_evTexture_phi, 0);
        gl.glBindTextureUnit(m_evTexture_obstacles, 0);
        gl.glBindTextureUnit(m_evTexture_levelset, 0);
        m_constants.treatAsLiquidVelocity = false;
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
    }

    private void Diffuse                  ( float timestep, float viscosity, int dstPhi, int srcPhi, int tmpPhi, int srcObst ){
        // These are bound first, as they stay the same through all iterations
        /*m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );
        m_evTexture_phi->SetResource( m_pShaderResourceViews[srcPhi] );
        m_evViscosity->SetFloat(viscosity);
        m_evTimeStep->SetFloat(timestep);*/
        gl.glBindTextureUnit(m_evTexture_obstacles, m_pShaderResourceViews[srcObst].getTexture());
        gl.glBindTextureUnit(m_evTexture_phi,       m_pShaderResourceViews[srcPhi].getTexture());

        m_constants.viscosity = viscosity;
        m_constants.timestep = timestep;


        // If # of iterations is odd we write first to the desired destination, otherwise to the tmp buffer. The source is the opposite
        int dst = (m_nDiffusionIterations & 1) != 0 ? dstPhi : tmpPhi;
        int src = (m_nDiffusionIterations & 1) != 0 ? tmpPhi : dstPhi;

        // First pass is a bit different, as we need the srcPhi also in phi_next
//        m_evTexture_phi_next->SetResource( m_pShaderResourceViews[srcPhi] );
        gl.glBindTextureUnit(m_evTexture_phi_next, m_pShaderResourceViews[srcPhi].getTexture());
        m_etDiffuse.enable();
        SetRenderTarget( dst );
        m_pGrid.DrawSlices();
//        m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);

        // Do more passes if needed
        for( int iteration = 1; iteration < m_nDiffusionIterations; iteration++ )
        {
//            swap(dst, src);
            int a = dst;
            dst = src;
            src = a;

//            m_evTexture_phi_next->SetResource( m_pShaderResourceViews[src] );
            gl.glBindTextureUnit(m_evTexture_phi_next, m_pShaderResourceViews[src].getTexture());
            m_etDiffuse.enable();
            SetRenderTarget( dst );

            m_pGrid.DrawSlices();

            /*m_evTexture_phi_next->SetResource(NULL);
            m_etDiffuse->GetPassByIndex(0)->Apply(0);
            m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);*/
            gl.glBindTextureUnit(m_evTexture_phi_next, 0);
        }

        /*m_evTexture_obstacles->SetResource(NULL);
        m_evTexture_phi->SetResource(NULL);
        m_evTexture_phi_next->SetResource( NULL );
        m_etDiffuse->GetPassByIndex(0)->Apply(0);*/

        gl.glBindTextureUnit(m_evTexture_obstacles, 0);
        gl.glBindTextureUnit(m_evTexture_phi, 0);
        gl.glBindTextureUnit(m_evTexture_phi_next, 0);
    }

    private void ComputeVorticity         ( int dstVorticity, int srcVel ){
//        m_evTexture_velocity.SetResource( m_pShaderResourceViews[srcVel] );
        gl.glBindTextureUnit(m_evTexture_velocity, m_pShaderResourceViews[srcVel].getTexture());

        m_etVorticity.enable();
        SetRenderTarget( dstVorticity );

        m_pGrid.DrawSlices();

        /*m_evTexture_velocity->SetResource( NULL );
        m_etVorticity->GetPassByIndex(0)->Apply(0);
        m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);*/

        gl.glBindTextureUnit(m_evTexture_velocity, 0);
    }

    private void ApplyVorticityConfinement( float timestep, int dstVel, int srcVorticity, int srcObst, int srcLevelSet ){
        /*m_evVortConfinementScale->SetFloat(m_fVorticityConfinementScale);
        m_evTimeStep->SetFloat(timestep);
        m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );
        m_evTexture_vorticity->SetResource( m_pShaderResourceViews[srcVorticity] );
        m_evTexture_levelset->SetResource( m_pShaderResourceViews[srcLevelSet] );
        if( (m_eFluidType == FT_LIQUID) && m_bTreatAsLiquidVelocity)
            m_evTreatAsLiquidVelocity->SetBool(true);*/

        m_constants.vortConfinementScale = m_fVorticityConfinementScale;
        m_constants.timestep = timestep;
        gl.glBindTextureUnit(m_evTexture_obstacles, m_pShaderResourceViews[srcObst].getTexture());
        gl.glBindTextureUnit(m_evTexture_vorticity, m_pShaderResourceViews[srcVorticity].getTexture());
        gl.glBindTextureUnit(m_evTexture_levelset, m_pShaderResourceViews[srcLevelSet].getTexture());

        if( (m_eFluidType == FT_LIQUID) && m_bTreatAsLiquidVelocity)
            m_constants.treatAsLiquidVelocity = true;

        m_etConfinement.enable();
        SetRenderTarget( dstVel );

        m_pGrid.DrawSlices();

        /*m_evTexture_obstacles->SetResource( NULL);
        m_evTexture_vorticity->SetResource( NULL );
        m_evTexture_levelset->SetResource( NULL );
        m_evTreatAsLiquidVelocity->SetBool( false );
        m_etConfinement->GetPassByIndex(0)->Apply(0);
        m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);*/
    }

    private static float lilrand()
    {
        return (/*rand()/float(RAND_MAX)*/Numeric.random() - 0.5f)*5.0f;
    }

    private void AddNewMatter             ( float timestep, int dstPhi, int srcObst ){
        SetRenderTarget( dstPhi );

        //if( !m_bMouseDown )


        switch(m_eFluidType)
        {
            // Add Fire Density
            case FT_FIRE:
            {
                // density of the fire is computed using a sinusoidal function of 'm_t' to make it more interesting.
                float fireDensity = 3.0f/**timestep*/ * (float)(((( Math.sin(m_t)*0.5f + 0.5f)) * Numeric.random()*2 )*(1.0f-m_fSaturation) + m_fSaturation);
//                Vector4f vDensity = new Vector4f(fireDensity, fireDensity, fireDensity, 1.0f);

//                m_evColor->SetFloatVector((float*)&vDensity );
                m_constants.color.set(fireDensity, fireDensity, fireDensity, 1.0f);
//                m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );
                gl.glBindTextureUnit(m_evTexture_obstacles,  m_pShaderResourceViews[srcObst].getTexture());
                m_etCopyTextureDensity.enable();

                m_pGrid.DrawSlice(3);

                /*m_evTexture_obstacles->SetResource( NULL );
                m_etCopyTextureDensity->GetPassByIndex(0)->Apply(0);*/
                gl.glBindTextureUnit(m_evTexture_obstacles,0);
            }
            break;

            // Add Smoke Density (rendering guassian balls of smoke)
            case FT_SMOKE:
            {
                // the density of the smoke is computed using a sinusoidal function of 'm_t' to make it more interesting.
                float density = 1.5f/*timestep*/ * (float)(((Math.sin( m_t + 2.0f*Numeric.PI/3.0f )*0.5f + 0.5f))*m_fSaturation + (1.0f-m_fSaturation));
//                D3DXVECTOR4 smokeDensity(density, density, density, 1.0f);

                /*m_evRadius->SetFloat(m_fImpulseSize);
                m_evCenter->SetFloatVector((float*)&m_vImpulsePos );
                m_evColor->SetFloatVector((float*)&smokeDensity );
                m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );*/
                m_constants.center.set(m_vImpulsePos);
                m_constants.color.set(density, density, density, 1.0f);
                m_constants.radius = m_fImpulseSize;
                gl.glBindTextureUnit(m_evTexture_obstacles, m_pShaderResourceViews[srcObst].getTexture());

                m_etGaussian.enable();

                m_pGrid.DrawSlices();

//                m_evTexture_obstacles->SetResource( NULL );
                gl.glBindTextureUnit(m_evTexture_obstacles, 0);
            }
            break;

            case FT_LIQUID:
            {
                // Hack to counter the lack of convergence in the pressure projection step with Jacobi iterations:
                //  blend between a "known" initial balanced state and the current state
/*#if 0
                m_etInjectLiquid->GetPassByIndex(0)->Apply(0);
                m_pGrid->DrawSlices();
#endif*/

                // Add a ball of liquid (should appear as a stream)
                if(m_bLiquidStream) {
                    /*m_evRadius->SetFloat(m_fStreamSize);
                    m_evCenter->SetFloatVector((float*)&m_streamCenter );
                    m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );*/
                    m_constants.radius = m_fStreamSize;
                    m_constants.center.set(m_streamCenter);
                    gl.glBindTextureUnit(m_evTexture_obstacles, m_pShaderResourceViews[srcObst].getTexture());
                    m_etLiquidStream_LevelSet.enable();

                    m_pGrid.DrawSlices();

                    /*m_evTexture_obstacles->SetResource( NULL );
                    m_etLiquidStream_LevelSet->GetPassByIndex(0)->Apply(0);*/
                    gl.glBindTextureUnit(m_evTexture_obstacles, 0);
                }
            }
            break;

            default:
                assert(false);
                break;
        }

//        m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);
        fbo.unbind();
    }


    private void AddExternalForces        ( float timestep, int dstVel, int srcObst, int srcLevelSet, float impulseSize, float randCenterScale ){
        SetRenderTarget( dstVel );
        float g_zVelocityScale = 4.0f;
        float g_xyVelocityScale = 4.0f;
        switch(m_eFluidType)
        {
            case FT_FIRE:
            {
                // Add Fire Velocity
                m_t += 0.015f * timestep;

                float var = 0.25f;
                float fireDensity = 1.5f * (float)((((Math.sin(m_t)*0.5f + 0.5f)) * Numeric.random()*2 )*(1.0f-m_fSaturation) + m_fSaturation);
                /*Vector4f fireVelocity(g_xyVelocityScale*(0.5f + var) + fireDensity,
                    g_xyVelocityScale*(0.5f + var) + fireDensity,
                    g_zVelocityScale *(0.5f + var) + fireDensity, 1.0f);

                m_evColor->SetFloatVector((float*)&fireVelocity);
                m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );*/
                m_constants.color.set(g_xyVelocityScale*(0.5f + var) + fireDensity,
                        g_xyVelocityScale*(0.5f + var) + fireDensity,
                        g_zVelocityScale *(0.5f + var) + fireDensity, 1.0f);
                gl.glBindTextureUnit(m_evTexture_obstacles, m_pShaderResourceViews[srcObst].getTexture());

                m_etAddDensityDerivativeVelocity.enable();

                m_pGrid.DrawSlice(3);

                /*m_evTexture_obstacles->SetResource( NULL );
                m_etAddDensityDerivativeVelocity->GetPassByIndex(0)->Apply(0);*/
                gl.glBindTextureUnit(m_evTexture_obstacles, 0);
            }
            break;

            case FT_SMOKE:
            {
                // Add gaussian ball of Velocity
                m_t += 0.025f * timestep;

//                D3DXVECTOR4 center(m_vImpulsePos.x+lilrand()*randCenterScale, m_vImpulsePos.y+lilrand()*randCenterScale, m_vImpulsePos.z+lilrand()*randCenterScale, 1);

                // m_evColor in this case is the initial velocity given to the emitted smoke
                /*m_evRadius->SetFloat(impulseSize);
                m_evCenter->SetFloatVector((float*)&center);
                m_evColor->SetFloatVector((float*)&m_vImpulseDir);
                m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );*/
                m_constants.radius = impulseSize;
                m_constants.center.set(m_vImpulsePos.x+lilrand()*randCenterScale, m_vImpulsePos.y+lilrand()*randCenterScale, m_vImpulsePos.z+lilrand()*randCenterScale);
                m_constants.color.set(m_vImpulseDir);
                gl.glBindTextureUnit(m_evTexture_obstacles, m_pShaderResourceViews[srcObst].getTexture());

                m_etGaussian.enable();

                m_pGrid.DrawSlices();

//                m_evTexture_obstacles->SetResource( NULL );
                gl.glBindTextureUnit(m_evTexture_obstacles,0);
                m_etGaussian.enable();
            }
            break;

            case FT_LIQUID:
            {
/*#if 0
                // for boat
                D3DXVECTOR4 posBoat(- 10.0f, m_pGrid->GetDimY()/2.0f + 4.0f, 38.0f, 1.0f );
                D3DXMATRIX rotBoat;

                D3DXMatrixRotationY(&rotBoat, g_fModelRotation);
                D3DXVec3Transform((D3DXVECTOR4*)&posBoat, (D3DXVECTOR3*)&posBoat, &rotBoat);

                D3DXVECTOR4 dirBoat(-0.4f * g_fRotSpeed /10.0f - 0.3f, -0.3f * g_fRotSpeed/10.0f, 0.0f, 1.0f);
                D3DXVec3Transform((D3DXVECTOR4*)&dirBoat, (D3DXVECTOR3*)&dirBoat, &rotBoat);

                // for water ski
                D3DXVECTOR4 posWaterSki(-8.0f, m_pGrid->GetDimY()/2.0f + 4.0f, -41.0f, 1.0f);
                D3DXMATRIX rotWaterSki;

                D3DXMatrixRotationY(&rotWaterSki, g_fModelRotation);
                D3DXVec3Transform((D3DXVECTOR4*)&posWaterSki, (D3DXVECTOR3*)&posWaterSki, &rotWaterSki);

                D3DXVECTOR4 dirWaterSki(-0.4f * g_fRotSpeed /10.0f, -0.2f * g_fRotSpeed/10.0f, -0.2f * g_fRotSpeed/10.0f, 1.0f);
                D3DXVec3Transform((D3DXVECTOR4*)&dirWaterSki, (D3DXVECTOR3*)&dirWaterSki, &rotWaterSki);


                // velocity impulse for boat
                D3DXVECTOR3 centerBoat(  posBoat.x+lilrand() + m_pGrid->GetDimX()/2, posBoat.y+lilrand(), posBoat.z+lilrand() + m_pGrid->GetDimZ()/2);

                m_evRadius->SetFloat(m_fImpulseSize);
                m_evCenter->SetFloatVector((float*)&centerBoat);
                m_evColor->SetFloatVector((float*)&dirBoat);
                m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );
                m_etGaussian->GetPassByIndex(0)->Apply(0);

                m_pGrid->DrawSlices();


                // velocity impulse for water ski
                D3DXVECTOR3 centerWaterSki(  posWaterSki.x+lilrand() + m_pGrid->GetDimX()/2, posWaterSki.y+lilrand(), posWaterSki.z+lilrand() + m_pGrid->GetDimZ()/2);

                m_evRadius->SetFloat(m_fImpulseSize*1.2f);
                m_evCenter->SetFloatVector((float*)&centerWaterSki);
                m_evColor->SetFloatVector((float*)&dirWaterSki);
                m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );
                m_etGaussian->GetPassByIndex(0)->Apply(0);

                m_pGrid->DrawSlices();

                m_evTexture_obstacles->SetResource( NULL );
                m_etGaussian->GetPassByIndex(0)->Apply(0);
#endif*/

                // liquid stream
//#if 1
                if(m_bLiquidStream) {
//                    D3DXVECTOR4 streamVelocity(-0.7f, 1.0f, -2.0f, 1.0f);

                    /*m_evRadius->SetFloat(m_fStreamSize);
                    m_evCenter->SetFloatVector((float*)&m_streamCenter);
                    m_evColor->SetFloatVector((float*)&streamVelocity);
                    m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );*/
                    m_constants.radius = m_fStreamSize;
                    m_constants.center.set(m_streamCenter);
                    m_constants.color.set(-0.7f, 1.0f, -2.0f, 1.0f);
                    gl.glBindTextureUnit(m_evTexture_obstacles, m_pShaderResourceViews[srcObst].getTexture());

                    m_etLiquidStream_Velocity.enable();

                    m_pGrid.DrawSlices();

                    /*m_evTexture_obstacles->SetResource( NULL );
                    m_etLiquidStream_Velocity->GetPassByIndex(0)->Apply(0);*/
                    gl.glBindTextureUnit(m_evTexture_obstacles, 0);
                }
//#endif

            }
            break;
            default:
                assert(false);
                break;
        }

        // Apply gravity
        if( m_bGravity )
        {
//            D3DXVECTOR3 gravity(0.0f, 0.0f, -0.04f);

            /*m_evGravity->SetFloatVector((float*)&gravity);
            if( (m_eFluidType == FT_LIQUID) && m_bTreatAsLiquidVelocity)
                m_evTreatAsLiquidVelocity->SetBool( true );
            m_evTexture_levelset->SetResource( m_pShaderResourceViews[srcLevelSet] );
            m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );*/
            m_constants.gravity.set(0.0f, 0.0f, -0.04f);
            if( (m_eFluidType == FT_LIQUID) && m_bTreatAsLiquidVelocity)
                m_constants.treatAsLiquidVelocity = true;
            gl.glBindTextureUnit(m_evTexture_levelset, m_pShaderResourceViews[srcLevelSet].getTexture());
            gl.glBindTextureUnit(m_evTexture_obstacles, m_pShaderResourceViews[srcObst].getTexture());

            m_etGravity.enable();

            m_pGrid.DrawSlices();

            /*m_evTreatAsLiquidVelocity->SetBool( false );
            m_evTexture_levelset->SetResource( NULL );
            m_evTexture_obstacles->SetResource( NULL );
            m_etGravity->GetPassByIndex(0)->Apply(0);*/
            m_constants.treatAsLiquidVelocity = false;
            gl.glBindTextureUnit(m_evTexture_levelset, 0);
            gl.glBindTextureUnit(m_evTexture_obstacles, 0);
        }

//        m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);
        fbo.unbind();
    }

    private void ComputeVelocityDivergence( int dstDivergence, int srcVel, int srcObst, int srcObstVel ){
        /*m_evTexture_velocity->SetResource( m_pShaderResourceViews[srcVel] );
        m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );
        m_evTexture_obstvelocity->SetResource( m_pShaderResourceViews[srcObstVel] );*/
        gl.glBindTextureUnit(m_evTexture_velocity, m_pShaderResourceViews[srcVel].getTexture());
        gl.glBindTextureUnit(m_evTexture_obstacles, m_pShaderResourceViews[srcObst].getTexture());
        gl.glBindTextureUnit(m_evTexture_obstvelocity, m_pShaderResourceViews[srcObstVel].getTexture());

        m_etDivergence.enable();
        SetRenderTarget( dstDivergence );

        m_pGrid.DrawSlices();

        /*m_evTexture_velocity->SetResource( NULL );
        m_evTexture_obstacles->SetResource( NULL );
        m_evTexture_obstvelocity->SetResource( NULL );*/
        gl.glBindTextureUnit(m_evTexture_velocity, 0);
        gl.glBindTextureUnit(m_evTexture_obstacles, 0);
        gl.glBindTextureUnit(m_evTexture_obstvelocity, 0);

        /*m_etDivergence->GetPassByIndex(0)->Apply(0);
        m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL); */
    }

    private void ComputePressure          ( int dstAndSrcPressure, int tmpPressure, int srcDivergence, int srcObst, int srcLevelSet ){
        /*m_evTexture_divergence->SetResource( m_pShaderResourceViews[srcDivergence] );
        m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );*/
        gl.glBindTextureUnit(m_evTexture_divergence, m_pShaderResourceViews[srcDivergence].getTexture());
        gl.glBindTextureUnit(m_evTexture_obstacles, m_pShaderResourceViews[srcObst].getTexture());

        for( int iteration = 0; iteration < m_nJacobiIterations/2.0; iteration++ )
        {
//            m_evTexture_pressure->SetResource( m_pShaderResourceViews[dstAndSrcPressure] );
            gl.glBindTextureUnit(m_evTexture_pressure, m_pShaderResourceViews[dstAndSrcPressure].getTexture());
            m_etScalarJacobi.enable();
            SetRenderTarget( tmpPressure );

            m_pGrid.DrawSlices();

//            m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);


//            m_evTexture_pressure->SetResource( m_pShaderResourceViews[tmpPressure] );
            gl.glBindTextureUnit(m_evTexture_pressure, m_pShaderResourceViews[tmpPressure].getTexture());
            m_etScalarJacobi.enable();
            SetRenderTarget( dstAndSrcPressure );

            m_pGrid.DrawSlices();
//#if 1
            if( (m_eFluidType == FT_LIQUID) && m_bTreatAsLiquidVelocity )
            {
                /*m_evTreatAsLiquidVelocity->SetBool( true );
                m_evTexture_levelset->SetResource( m_pShaderResourceViews[srcLevelSet] );*/
                m_constants.treatAsLiquidVelocity = true;
                gl.glBindTextureUnit(m_evTexture_levelset, m_pShaderResourceViews[srcLevelSet].getTexture());

                m_etAirPressure.enable();

                m_pGrid.DrawSlices();

                /*m_evTreatAsLiquidVelocity->SetBool( false );
                m_evTexture_levelset->SetResource( NULL );
                m_evTexture_phi->SetResource( NULL );*/
                m_etAirPressure.enable();

                gl.glBindTextureUnit(m_evTexture_levelset, 0);
            }
//#endif
//            m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);

        }

       /* m_evTexture_pressure->SetResource(NULL);
        m_evTexture_divergence->SetResource(NULL);
        m_evTexture_obstacles->SetResource( NULL );
        m_etScalarJacobi->GetPassByIndex(0)->Apply(0);
        m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);*/

        gl.glBindTextureUnit(m_evTexture_pressure, 0);
        gl.glBindTextureUnit(m_evTexture_divergence, 0);
        gl.glBindTextureUnit(m_evTexture_obstacles, 0);
    }

    private void ProjectVelocity          ( int dstVel, int srcPressure, int srcVel, int srcObst, int srcObstVel, int srcLevelSet ){
        /*m_evTexture_pressure->SetResource( m_pShaderResourceViews[srcPressure] );
        m_evTexture_velocity->SetResource( m_pShaderResourceViews[srcVel] );
        m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );
        m_evTexture_obstvelocity->SetResource( m_pShaderResourceViews[srcObstVel] );
        if( (m_eFluidType == FT_LIQUID) && m_bTreatAsLiquidVelocity )
            m_evTreatAsLiquidVelocity->SetBool( true );
        m_evTexture_levelset->SetResource( m_pShaderResourceViews[srcLevelSet] );*/

        gl.glBindTextureUnit(m_evTexture_pressure, m_pShaderResourceViews[srcPressure].getTexture());
        gl.glBindTextureUnit(m_evTexture_velocity, m_pShaderResourceViews[srcVel].getTexture());
        gl.glBindTextureUnit(m_evTexture_obstacles, m_pShaderResourceViews[srcObst].getTexture());
        gl.glBindTextureUnit(m_evTexture_obstvelocity, m_pShaderResourceViews[srcObstVel].getTexture());
        if( (m_eFluidType == FT_LIQUID) && m_bTreatAsLiquidVelocity )
            m_constants.treatAsLiquidVelocity = true;
        gl.glBindTextureUnit(m_evTexture_levelset, m_pShaderResourceViews[srcLevelSet].getTexture());

        m_etProject.enable();
        SetRenderTarget( dstVel );

        m_pGrid.DrawSlices();

        /*m_evTexture_pressure->SetResource( NULL );
        m_evTexture_velocity->SetResource( NULL );
        m_evTexture_obstacles->SetResource( NULL );
        m_evTexture_obstvelocity->SetResource( NULL );
        m_evTreatAsLiquidVelocity->SetBool( false );
        m_evTexture_levelset->SetResource( NULL );
        m_etProject->GetPassByIndex(0)->Apply(0);
        m_pD3DDevice->OMSetRenderTargets( 0, NULL, NULL );*/
        gl.glBindTextureUnit(m_evTexture_pressure, 0);
        gl.glBindTextureUnit(m_evTexture_velocity, 0);
        gl.glBindTextureUnit(m_evTexture_obstacles, 0);
        gl.glBindTextureUnit(m_evTexture_obstvelocity, 0);
        gl.glBindTextureUnit(m_evTexture_levelset, 0);
        m_constants.treatAsLiquidVelocity = false;
    }

    private void RedistanceLevelSet       ( int dstLevelSet, int srcLevelSet, int tmpLevelSet ){
//        m_evTexture_phi->SetResource( m_pShaderResourceViews[srcLevelSet] );
        gl.glBindTextureUnit(m_evTexture_phi, m_pShaderResourceViews[srcLevelSet].getTexture());
        int src = tmpLevelSet;
        int dst = dstLevelSet;

        // To ensure the end result is in dstLevelSet we use an odd number of iterations (override it if it's not odd)
        m_nRedistancingIterations |= 0x1;

        for( int iteration = 0; iteration < m_nRedistancingIterations; iteration++ )
        {
            if( iteration == 0 )
//                m_evTexture_phi_next->SetResource( m_pShaderResourceViews[srcLevelSet] );
                gl.glBindTextureUnit(m_evTexture_phi_next, m_pShaderResourceViews[srcLevelSet].getTexture());
            else
//                m_evTexture_phi_next->SetResource( m_pShaderResourceViews[src] );
                gl.glBindTextureUnit(m_evTexture_phi_next, m_pShaderResourceViews[src].getTexture());

            m_etRedistance.enable();
            SetRenderTarget( dst );

            m_pGrid.DrawSlices();

//            m_evTexture_phi_next->SetResource(NULL);
            gl.glBindTextureUnit(m_evTexture_phi_next, 0);
            m_etRedistance.enable();
//            m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);
            fbo.unbind();

//            swap(src, dst);
            int a = dst;
            dst = src;
            src = a;
        }

        /*m_evTexture_phi->SetResource(NULL);
        m_evTexture_phi_next->SetResource(NULL);
        m_etRedistance->GetPassByIndex(0)->Apply(0);
        m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);*/

        gl.glBindTextureUnit(m_evTexture_phi, 0);
        gl.glBindTextureUnit(m_evTexture_phi_next, 0);
    }

    private void ExtrapolateVelocity( int dstAndSrcVel, int tmpVel, int srcObst, int srcLeveSet ){
        /*m_evTexture_obstacles->SetResource( m_pShaderResourceViews[srcObst] );
        m_evTexture_levelset->SetResource( m_pShaderResourceViews[srcLeveSet] );
        m_evTreatAsLiquidVelocity->SetBool(m_bTreatAsLiquidVelocity);*/
        gl.glBindTextureUnit(m_evTexture_obstacles, m_pShaderResourceViews[srcObst].getTexture());
        gl.glBindTextureUnit(m_evTexture_levelset, m_pShaderResourceViews[srcLeveSet].getTexture());
        m_constants.treatAsLiquidVelocity = m_bTreatAsLiquidVelocity;

        int src = dstAndSrcVel;
        int dst = tmpVel;

        // To ensure the end result is in the original texture
        //  override the number of iterations if it's not even (add one more in that case)
        if(( m_nVelExtrapolationIterations & 0x1 )!=0)
            m_nVelExtrapolationIterations++;

        for( int iteration = 0; iteration < m_nVelExtrapolationIterations; iteration++ )
        {
//            m_evTexture_velocity->SetResource( m_pShaderResourceViews[src] );
            gl.glBindTextureUnit(m_evTexture_velocity, m_pShaderResourceViews[src].getTexture());
            m_etExtrapolateVelocity.enable();
            SetRenderTarget( dst );

            m_pGrid.DrawSlices();

//            m_evTexture_velocity->SetResource( NULL );
            gl.glBindTextureUnit(m_evTexture_velocity, 0);
            m_etExtrapolateVelocity.enable();
//            m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL)

//            swap(src, dst);
            int a = dst;
            dst = src;
            src = a;
        }

        /*m_evTexture_obstacles->SetResource(NULL);
        m_evTexture_levelset->SetResource(NULL);
        m_evTexture_velocity->SetResource(NULL);
        m_evTreatAsLiquidVelocity->SetBool(false);
        m_etScalarJacobi->GetPassByIndex(0)->Apply(0);
        m_pD3DDevice->OMSetRenderTargets(0, NULL, NULL);*/

        gl.glBindTextureUnit(m_evTexture_obstacles, 0);
        gl.glBindTextureUnit(m_evTexture_levelset, 0);
        gl.glBindTextureUnit(m_evTexture_velocity, 0);

        m_constants.treatAsLiquidVelocity = false;
    }

    //D3D10 helper functions
    void CreateTextureAndViews(int rtIndex, Texture3DDesc TexDesc){
        m_p3DTextures[rtIndex] = TextureUtils.createTexture3D(TexDesc, null);
    }

    void SetRenderTarget(int rtIndex/*, ID3D10DepthStencilView * optionalDSV = NULL */){
        fbo.bind();
        fbo.setRenderTexture(m_pRenderTargetViews[rtIndex], null);
    }

    @Override
    public void dispose() {
        SAFE_RELEASE(m_pGrid);
//        SAFE_RELEASE(m_pD3DDevice);
//        SAFE_RELEASE(m_pEffect);

        for(int i=0;i<NUM_RENDER_TARGETS;i++)
        {
            SAFE_RELEASE(m_p3DTextures[i]);
            SAFE_RELEASE(m_pShaderResourceViews[i]);
            SAFE_RELEASE(m_pRenderTargetViews[i]);
        }
    }
}
