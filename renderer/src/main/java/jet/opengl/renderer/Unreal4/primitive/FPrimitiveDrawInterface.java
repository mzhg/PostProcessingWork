package jet.opengl.renderer.Unreal4.primitive;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;

import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.renderer.Unreal4.mesh.FDynamicPrimitiveResource;
import jet.opengl.renderer.Unreal4.mesh.FMeshBatch;
import jet.opengl.renderer.Unreal4.scenes.ESimpleElementBlendMode;
import jet.opengl.renderer.Unreal4.scenes.FSceneView;
import jet.opengl.renderer.Unreal4.hit.HHitProxy;

/**
 * The base interface used to query a primitive for its dynamic elements.
 */
public abstract class FPrimitiveDrawInterface {

    public final FSceneView View;

    public FPrimitiveDrawInterface(FSceneView view){
        View = view;
    }

    public abstract boolean IsHitTesting();
    public abstract void SetHitProxy(HHitProxy HitProxy);

    public abstract void RegisterDynamicResource(FDynamicPrimitiveResource DynamicResource);

    public abstract void AddReserveLines(byte DepthPriorityGroup, int NumLines, boolean bDepthBiased, boolean bThickLines);

    public abstract void DrawSprite(
            ReadableVector3f Position,
            float SizeX,
            float SizeY,
            TextureGL Sprite,
            ReadableVector4f Color,
            byte DepthPriorityGroup,
            float U,
            float UL,
            float V,
            float VL,
            ESimpleElementBlendMode BlendMode /*= 1, SE_BLEND_Masked*/
    );

    public abstract void DrawLine(
		ReadableVector3f Start,
        ReadableVector3f End,
        ReadableVector4f Color,
        byte DepthPriorityGroup,
        float Thickness /*= 0.0f*/,
        float DepthBias /*= 0.0f*/,
        boolean bScreenSpace /*= false*/
    );

    public abstract void DrawPoint(
            ReadableVector3f Position,
            ReadableVector4f Color,
        float PointSize,
        byte DepthPriorityGroup
    );

    /**
     * Draw a mesh element.
     * This should only be called through the DrawMesh function.
     *
     * @return Number of passes rendered for the mesh
     */
    public abstract int DrawMesh(FMeshBatch Mesh);
}
