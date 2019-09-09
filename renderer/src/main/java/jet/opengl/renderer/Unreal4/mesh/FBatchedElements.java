package jet.opengl.renderer.Unreal4.mesh;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;

import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.Recti;
import jet.opengl.postprocessing.util.StackFloat;
import jet.opengl.postprocessing.util.StackInt;
import jet.opengl.renderer.Unreal4.api.FMeshPassProcessorRenderState;
import jet.opengl.renderer.Unreal4.scenes.ESimpleElementBlendMode;
import jet.opengl.renderer.Unreal4.scenes.FDepthFieldGlowInfo;
import jet.opengl.renderer.Unreal4.scenes.FSceneView;
import jet.opengl.renderer.Unreal4.utils.FHitProxyId;

/** Batched elements for later rendering. */
public class FBatchedElements {

    /**Adds a line to the batch. Note only SE_BLEND_Opaque will be used for batched line rendering. */
    public void AddLine(Vector3f  Start, Vector3f  End, Vector4f Color, FHitProxyId HitProxyId, float  Thickness/* = 0.0f*/, float  DepthBias/* = 0.0f*/, boolean  bScreenSpace/* = false*/){
        throw new UnsupportedOperationException();
    }

    /**Adds a translucent line to the batch. */
    public void AddTranslucentLine(Vector3f  Start,Vector3f  End,Vector4f  Color,FHitProxyId  HitProxyId,float  Thickness/* = 0.0f*/,float  DepthBias/* = 0.0f*/,boolean  bScreenSpace/* = false*/){
        throw new UnsupportedOperationException();
    }

    /**Adds a point to the batch. Note only SE_BLEND_Opaque will be used for batched point rendering. */
    public void AddPoint(Vector3f  Position,float  Size,Vector4f  Color,FHitProxyId  HitProxyId){
        throw new UnsupportedOperationException();
    }

    /**Adds a mesh vertex to the batch. */
    public int AddVertex(Vector4f  InPosition, Vector2f InTextureCoordinate, Vector4f  InColor, FHitProxyId  HitProxyId){
        throw new UnsupportedOperationException();
    }

    /**Adds a triangle to the batch. */
    public void AddTriangle(int  V0, int  V1, int  V2, TextureGL Texture, ESimpleElementBlendMode BlendMode){
        throw new UnsupportedOperationException();
    }

    /**Adds a triangle to the batch. */
    public void AddTriangle(int  V0, int V1, int V2, TextureGL Texture, int BlendMode, FDepthFieldGlowInfo GlowInfo/* = FDepthFieldGlowInfo()*/){
        throw new UnsupportedOperationException();
    }

    /**Adds a triangle to the batch. */
    public void AddTriangle(int  V0,int V1,int V2,FBatchedElementParameters  BatchedElementParameters,ESimpleElementBlendMode  BlendMode){
        throw new UnsupportedOperationException();
    }

    /**
     *Reserves space in index array for a mesh element for current number plus expected number.
     *
     *@param NumMeshTriangles - number of triangles to reserve space for
     *@param Texture - used to find the mesh element entry
     *@param BlendMode - used to find the mesh element entry*/
    public void AddReserveTriangles(int  NumMeshTriangles, TextureGL Texture, ESimpleElementBlendMode BlendMode){
        throw new UnsupportedOperationException();
    }

    /**
     *Reserves space in index array for a mesh element
     *
     *@param NumMeshTriangles - number of triangles to reserve space for
     *@param Texture - used to find the mesh element entry
     *@param BlendMode - used to find the mesh element entry*/
    public void ReserveTriangles(int  NumMeshTriangles,TextureGL Texture,ESimpleElementBlendMode  BlendMode){
        throw new UnsupportedOperationException();
    }

    /**
     *Reserves space in mesh vertex array for current number plus expected number.
     *
     *@param NumMeshVerts - number of verts to reserve space for
    public void AddReserveVertices(int  NumMeshVerts){
        throw new UnsupportedOperationException();
    }

    /**
     *Reserves space in mesh vertex array for at least this many total verts.
     *
     *@param NumMeshVerts - number of verts to reserve space for
     */
    public void ReserveVertices(int  NumMeshVerts){
        throw new UnsupportedOperationException();
    }

    /**
     *Reserves space in line vertex array
     *
     *@param NumLines - number of lines to reserve space for
     *@param bDepthBiased - whether reserving depth-biased lines or non-biased lines
     *@param bThickLines - whether reserving regular lines or thick lines*/
    public void AddReserveLines(int  NumLines,boolean  bDepthBiased/* = false*/,boolean  bThickLines/* = false*/){
        throw new UnsupportedOperationException();
    }

    /**Adds a sprite to the batch. */
    public void AddSprite(Vector3f Position, float  SizeX, float  SizeY, TextureGL  Texture, Vector4f  Color, FHitProxyId  HitProxyId, float  U, float  UL, float  V, float  VL, ESimpleElementBlendMode  BlendMode/* = SE_BLEND_Masked*/){
        throw new UnsupportedOperationException();
    }

    /***
     * Draws the batch
     *
     * @param View			FSceneView for shaders that need access to view constants. Non-optional to also reference its ViewProjectionMatrix and size of the ViewRect
     * @param bHitTesting	Whether or not we are hit testing
     * @param Gamma			Optional gamma override*/
    public boolean Draw(FMeshPassProcessorRenderState DrawRenderState, int FeatureLevel, boolean bNeedToSwitchVerticalAxis, FSceneView View, boolean bHitTesting, float Gamma/* = 1.0f*/, int Filter/* = EBlendModeFilter::All*/){
        throw new UnsupportedOperationException();
    }

    /***
     * Creates a proxy FSceneView for operations that are not tied directly to a scene but still require batched elements to be drawn.*/
    public static FSceneView CreateProxySceneView(Matrix4f ProjectionMatrix, Recti ViewRect){
        throw new UnsupportedOperationException();
    }

    public boolean HasPrimsToDraw(){

        return( LineVertices.size() > 0 || Points.size() > 0 || Sprites.size() > 0 || MeshElements.size() > 0 || ThickLines.size() > 0 || WireTris.size() > 0 );
    }

    /**	* Adds a triangle to the batch. Extensive version where all parameters can be passed in.*/
    public void AddTriangleExtensive(int V0, int V1, int V2, FBatchedElementParameters BatchedElementParameters, TextureGL Texture, ESimpleElementBlendMode BlendMode, FDepthFieldGlowInfo GlowInfo/* = FDepthFieldGlowInfo()*/){
        throw new UnsupportedOperationException();
    }

    /**	* Clears any batched elements **/
    public void Clear(){
        throw new UnsupportedOperationException();
    }

    /**	* Helper function to return the amount of memory allocated by this class
     *
     * @return number of bytes allocated by this container*/
    public int GetAllocatedSize(){

        /*return sizeof(*this) + Points.GetAllocatedSize() + WireTris.GetAllocatedSize() + WireTriVerts.GetAllocatedSize() + ThickLines.GetAllocatedSize()
                + Sprites.GetAllocatedSize() + MeshElements.GetAllocatedSize() + MeshVertices.GetAllocatedSize();*/

        throw new UnsupportedOperationException();
    }

    public void EnableMobileHDREncoding(boolean bInEnableHDREncoding){

        bEnableHDREncoding = bInEnableHDREncoding;
    }

    /***
     * Draws points
     *
     * @param	Transform	Transformation matrix to use
     * @param	ViewportSizeX	Horizontal viewport size in pixels
     * @param	ViewportSizeY	Vertical viewport size in pixels
     * @param	CameraX		Local space normalized view direction X vector
     * @param	CameraY		Local space normalized view direction Y vector*/
    private void DrawPointElements(Matrix4f Transform, int ViewportSizeX, int ViewportSizeY, Vector3f CameraX, Vector3f CameraY){
        throw new UnsupportedOperationException();
    }

    public final ArrayList<FSimpleElementVertex> LineVertices = new ArrayList<>();

    private static final class FBatchedPoint
    {
        final Vector3f Position = new Vector3f();
        float Size;
        final Vector4f Color = new Vector4f();
        FHitProxyId HitProxyId;
    };

    private final ArrayList<FBatchedPoint> Points = new ArrayList<>();

    /*struct FBatchedWireTris
    {
        float DepthBias;
    };*/
    private final StackFloat WireTris = new StackFloat();
//    TArray<FSimpleElementVertex> WireTriVerts;

    private static final class  FBatchedThickLines
    {
        final Vector3f Start = new Vector3f();
        final Vector3f End = new Vector3f();
        float Thickness;
        final Vector4f Color = new Vector4f();
        FHitProxyId HitProxyId;
        float DepthBias;
        int bScreenSpace;
    };
    private final ArrayList<FBatchedThickLines> ThickLines = new ArrayList<>();

    private static final class FBatchedSprite
    {
        final Vector3f Position = new Vector3f();
        float SizeX;
        float SizeY;
		TextureGL Texture;
        final Vector4f Color = new Vector4f();
        FHitProxyId HitProxyId;
        float U;
        float UL;
        float V;
        float VL;
        int BlendMode;
    };
    /** This array is sorted during draw-calls */
    public final ArrayList<FBatchedSprite> Sprites = new ArrayList<>();

    private static final class FBatchedMeshElement
    {
        /** starting index in vertex buffer for this batch */
        int MinVertex;
        /** largest vertex index used by this batch */
        int MaxVertex;
        /** index buffer for triangles */
        StackInt Indices;
        /** all triangles in this batch draw with the same texture */
		TextureGL Texture;
        /** Parameters for this batched element */
        FBatchedElementParameters BatchedElementParameters;
        /** all triangles in this batch draw with the same blend mode */
        ESimpleElementBlendMode BlendMode;
        /** all triangles in this batch draw with the same depth field glow (depth field blend modes only) */
        FDepthFieldGlowInfo GlowInfo;
    };

    /** Max number of mesh index entries that will fit in a DrawPriUP call */
    private int MaxMeshIndicesAllowed;
    /** Max number of mesh vertices that will fit in a DrawPriUP call */
    private int MaxMeshVerticesAllowed;

    private final ArrayList<FBatchedMeshElement> MeshElements = new ArrayList<FBatchedMeshElement>();
    private final ArrayList<FSimpleElementVertex > MeshVertices = new ArrayList<>();

    /* bound shader state for the fast path
    private static class FSimpleElementBSSContainer
    {
        static int NumBSS = ESimpleElementBlendMode.SE_BLEND_RGBA_MASK_START.ordinal();
        FGlobalBoundShaderState UnencodedBSS;
        FGlobalBoundShaderState EncodedBSS[NumBSS];
        public:
        FGlobalBoundShaderState& GetBSS(bool bEncoded, ESimpleElementBlendMode BlendMode)
        {
            if (bEncoded)
            {
                check((uint32)BlendMode < NumBSS);
                return EncodedBSS[BlendMode];
            }
            else
            {
                return UnencodedBSS;
            }
        }
    };*/

    /**
     * Sets the appropriate vertex and pixel shader.
     */
    private void PrepareShaders(
//            FRHICommandList& RHICmdList,
//            FGraphicsPipelineStateInitializer& GraphicsPSOInit,
            int FeatureLevel,
            ESimpleElementBlendMode BlendMode,
		    Matrix4f Transform,
            boolean bSwitchVerticalAxis,
            FBatchedElementParameters BatchedElementParameters,
		    TextureGL Texture,
            boolean bHitTesting,
            float Gamma,
		    FDepthFieldGlowInfo GlowInfo, //= NULL,
		    FSceneView View //= NULL
    ) {
        throw new UnsupportedOperationException();
    }

    /** if false then prevent the use of HDR encoded shaders. */
    private boolean bEnableHDREncoding;
}
