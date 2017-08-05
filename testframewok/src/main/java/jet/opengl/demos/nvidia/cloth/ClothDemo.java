package jet.opengl.demos.nvidia.cloth;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/8/5.
 */

public class ClothDemo extends NvSampleApp {
    // Cloth size
    private static final int g_Width = 31;
    private static final int g_Height = 31;
    private static final int g_NumVertices = g_Width * g_Height;
    private static final float g_TexCoordScale = 4;

    private static final int
            PASS_SIMULATE_FORCE=0,
            PASS_SIMULATE_STRUCTURAL_AND_SHEAR_SPRING_0=1,
            PASS_SIMULATE_STRUCTURAL_AND_SHEAR_SPRING_1=2,
            PASS_SIMULATE_SHEAR_SPRING_0=3,
            PASS_SIMULATE_SHEAR_SPRING_1=4,
            PASS_SIMULATE_COLLISION=5;

    // Position indices
    int g_Old = 0;
    int g_Current = 1;
    int g_New = 2;

// Simulation
    int g_NumSimPasses = 3;
    int g_NumPrimitivesPerBatch = 4;
    int g_LastPrimitiveIndex = g_NumVertices / g_NumPrimitivesPerBatch;
    int g_LastPrimitiveSize = g_NumVertices % g_NumPrimitivesPerBatch;
    int g_NumSimIndices = g_NumPrimitivesPerBatch * (g_LastPrimitiveIndex + (g_LastPrimitiveSize == 0 ? 0 : 1));
    private static final int NUM_SIMULATE_IB =(PASS_SIMULATE_COLLISION + 1);
    final int[] g_NumSimulatedPrimitives = new int[NUM_SIMULATE_IB];
    int g_WindHeading;
    int g_WindStrength;
    int g_NumCutterTriangles;
    int g_FirstCutterTriangle;

    // Rendering
    int g_NumIndices;

// Vertex buffers
    BufferGL g_InitialPositionVB;
    BufferGL g_TexCoordVB;
    BufferGL g_AnchorVB;
    final BufferGL[] g_ParticleVB = new BufferGL[3];
    BufferGL g_NormalTangentVB;
    BufferGL g_CutterEdgeVB;

// Constant buffers
    BufferGL g_CutterEdgeCB;

// Index buffers
    final BufferGL[] g_SimulateIB = new BufferGL[NUM_SIMULATE_IB];
    BufferGL g_NormalTangentIB;
    BufferGL g_RenderIB;

// Vertex layouts
    VertexArrayObject g_ApplyForcesIL;
    VertexArrayObject g_SatisfyConstraintsIL;
    VertexArrayObject g_RenderTangentSpaceIL;
    VertexArrayObject g_AnchorIL;
    VertexArrayObject g_RenderClothIL;
    VertexArrayObject g_RenderMeshIL;

// Render target view
    final Texture2D[] g_ParticleRTV = new Texture2D[3];
    Texture2D g_CutterEdgeRTV;

// Shader resource view
    final Texture2D[] g_ParticleSRV = new Texture2D[3];

// Render state
    Runnable g_WireframeRS;
    Runnable g_SolidRS;

    // Anchor points
    final Vector2f g_AnchorPoint[] = {
            new Vector2f(0, 0),
            new Vector2f(g_Width / 2, 0),
            new Vector2f(g_Width - 1, 0)
    };
    int g_NumAnchorPoints = g_AnchorPoint.length;

    final Vector3f g_Center = new Vector3f();
    final Matrix4f g_TransformAnchorPoints = new Matrix4f();
    final Matrix4f g_Transform = new Matrix4f();

    // Transforms
    final Matrix4f g_Projection = new Matrix4f();
    final Matrix4f g_View = new Matrix4f();
    final Matrix4f g_ViewProjection = new Matrix4f();
    final Vector3f g_Eye = new Vector3f();

    // Scene
    final Vector4f g_Plane[] = {
            new Vector4f(0, 1, 0, 3 ),
            new Vector4f(0, -1, 0, 6),
            new Vector4f(1, 0, 0, 7 ),
            new Vector4f(-1, 0, 0, 7),
            new Vector4f(0, 0, 1, 7 ),
            new Vector4f(0, 0, -1, 7 )
    };
    final int g_NumPlanes = g_Plane.length;

    final Matrix4f[] g_PlaneWorld = new Matrix4f[g_NumPlanes];
    final Texture2D[] g_PlaneDiffuseTexture = new Texture2D[g_NumPlanes];
    final Texture2D[] g_PlaneNormalMap = new Texture2D[g_NumPlanes];
    SDKmesh g_PlaneMesh;

    final Vector4f[] g_Sphere = {new Vector4f(0,0,0,0.4f)};
    final int g_NumSpheres =1; // sizeof(g_Sphere) / sizeof(g_Sphere[0]);
    final Matrix4f[] g_SphereWorld = new Matrix4f[g_NumSpheres];
    GLVAO g_SphereMesh;

    final Capsule[] g_Capsule = {new Capsule(new Vector3f(-0.8f, -0.3f, 0), 0.5f, new Vector4f(0, 1, 0, 0),new Vector2f(0.3f, 0.2f) )};
    final int g_NumCapsules = 1; //sizeof(g_Capsule) / sizeof(g_Capsule[0]);
    final Matrix4f[] g_CapsuleWorld = new Matrix4f[g_NumCapsules];
    final SDKmesh[] g_CapsuleMesh = new SDKmesh[g_NumCapsules];

    Ellipsoid[] g_Ellipsoid = {
//            { D3DXVECTOR4(0.8f, 0, 0, 0), D3DXVECTOR4(0.35f, 0.5f, 0.25f, 0) },
            new Ellipsoid()
    };
    final int g_NumEllipsoids = 1; //sizeof(g_Ellipsoid) / sizeof(g_Ellipsoid[0]);
    final Matrix4f[] g_EllipsoidWorld = new Matrix4f[g_NumEllipsoids];

// Textures
    final Texture2D[] g_CellDiffuseTexture = new Texture2D[3];
    final Texture2D[] g_CellNormalMap = new Texture2D[3];
    Texture2D g_ClothDiffuseTexture;
    Texture2D g_ClothNormalMap;

    // Commands
    boolean g_Help = true;
    boolean g_Reset;
    boolean g_Run = true;
    int  g_StepSimulation;
    boolean g_Uncut;
    boolean g_Wireframe;
    boolean g_ShowTangentSpace;
    boolean g_ShiftDown;
    boolean g_CtrlDown;

    private float m_totalTime;
    private float g_CurrentTime;
    private float g_OldStateTime = -1;
    private float g_NewStateTime;
    private float g_OldTimeStep = -1;

    @Override
    protected void initRendering() {
        InitPlanes();
        InitSpheres();
        InitCapsules();
        InitEllipsoids();
    }

    //--------------------------------------------------------------------------------------
// Handle updates to the scene.  This is called regardless of which D3D API is used
//--------------------------------------------------------------------------------------
    void OnFrameMove(/*double time_d,*/ float elapsedTime/*, void* pUserContext*/)
    {
//        ID3D10Device* pd3dDevice = DXUTGetD3D10Device();

        // Update the camera's position based on user input
//        g_Camera.FrameMove(elapsedTime);

        // Simulation control
        if (!g_Run)
            return;

        // Time management
        m_totalTime += elapsedTime;
        float timeStep = 0.01f;
        float time = m_totalTime;

        // State time initialization
        if (g_OldStateTime < 0) {
            g_CurrentTime = time;
            g_OldStateTime = g_CurrentTime - timeStep;
            g_NewStateTime = g_CurrentTime;
            return;
        }

        // Time step clamping
        if (time - g_CurrentTime > 0.1f) {
            elapsedTime = timeStep;
            float currentTime = time - elapsedTime;
            g_OldStateTime += currentTime - g_CurrentTime;
            g_NewStateTime += currentTime - g_CurrentTime;
            g_CurrentTime = currentTime;
        }

        // Simulation loop
        while (time - g_CurrentTime >= timeStep) {
            g_CurrentTime += timeStep;

            // Simulate cloth
            SimulateCloth(/*pd3dDevice,*/ timeStep, g_OldTimeStep < 0 ? timeStep : g_OldTimeStep);

            g_OldTimeStep = timeStep;
            g_OldStateTime = g_NewStateTime;
            g_NewStateTime = g_CurrentTime;
            if (g_StepSimulation > 0) {
                --g_StepSimulation;
                if (g_StepSimulation == 0)
                    g_Run = false;
            }
        }
    }
}
