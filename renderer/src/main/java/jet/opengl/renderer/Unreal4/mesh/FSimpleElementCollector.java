package jet.opengl.renderer.Unreal4.mesh;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;

import java.util.ArrayList;

import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.renderer.Unreal4.api.FMeshPassProcessorRenderState;
import jet.opengl.renderer.Unreal4.primitive.FPrimitiveDrawInterface;
import jet.opengl.renderer.Unreal4.scenes.ESceneDepthPriorityGroup;
import jet.opengl.renderer.Unreal4.scenes.ESimpleElementBlendMode;
import jet.opengl.renderer.Unreal4.scenes.FSceneView;
import jet.opengl.renderer.Unreal4.utils.FHitProxyId;
import jet.opengl.renderer.Unreal4.hit.HHitProxy;
import jet.opengl.renderer.Unreal4.utils.Unsigned;

public class FSimpleElementCollector extends FPrimitiveDrawInterface {

    final FHitProxyId HitProxyId = new FHitProxyId();
    @Unsigned
    short PrimitiveMeshId;

    boolean bIsMobileHDR;

    /** The dynamic resources which have been registered with this drawer. */
    final ArrayList<FDynamicPrimitiveResource> DynamicResources = new ArrayList<>();

    /** The batched simple elements. */
    public final FBatchedElements BatchedElements = new FBatchedElements();
    public final FBatchedElements TopBatchedElements = new FBatchedElements();

    public FSimpleElementCollector() {
        super(null);

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean IsHitTesting() {
        return false;
    }

    @Override
    public void SetHitProxy(HHitProxy HitProxy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void RegisterDynamicResource(FDynamicPrimitiveResource DynamicResource) {

    }

    @Override
    public void AddReserveLines(byte DepthPriorityGroup, int NumLines, boolean bDepthBiased, boolean bThickLines) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void DrawSprite(ReadableVector3f Position, float SizeX, float SizeY, TextureGL Sprite, ReadableVector4f Color, byte DepthPriorityGroup, float U, float UL, float V, float VL, ESimpleElementBlendMode BlendMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void DrawLine(ReadableVector3f Start, ReadableVector3f End, ReadableVector4f Color, byte DepthPriorityGroup, float Thickness, float DepthBias, boolean bScreenSpace) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void DrawPoint(ReadableVector3f Position, ReadableVector4f Color, float PointSize, byte DepthPriorityGroup) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int DrawMesh(FMeshBatch Mesh) {
        return 0;
    }

    public void DrawBatchedElements(/*FRHICommandList& RHICmdList,*/ FMeshPassProcessorRenderState DrawRenderState, FSceneView InView, int Filter, ESceneDepthPriorityGroup DPG){
        throw new UnsupportedOperationException();
    }

}
