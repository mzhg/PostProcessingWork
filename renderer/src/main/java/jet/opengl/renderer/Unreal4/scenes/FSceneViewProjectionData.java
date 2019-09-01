package jet.opengl.renderer.Unreal4.scenes;

// SceneView.h

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.Recti;

/** Projection data for a FSceneView */
public class FSceneViewProjectionData {
    /** The view origin. */
    public final Vector3f ViewOrigin = new Vector3f();

    /** Rotation matrix transforming from world space to view space. */
    public final Matrix4f ViewRotationMatrix = new Matrix4f();

    /** UE4 projection matrix projects such that clip space Z=1 is the near plane, and Z=0 is the infinite far plane. */
    public final Matrix4f ProjectionMatrix = new Matrix4f();

    //The unconstrained (no aspect ratio bars applied) view rectangle (also unscaled)
    protected final Recti ViewRect = new Recti();

    // The constrained view rectangle (identical to UnconstrainedUnscaledViewRect if aspect ratio is not constrained)
    protected final Recti ConstrainedViewRect = new Recti();

    public void SetViewRectangle(Recti InViewRect)
    {
        ViewRect.set(InViewRect);
        ConstrainedViewRect.set(InViewRect);
    }

    public void SetConstrainedViewRectangle(Recti InViewRect)
    {
        ConstrainedViewRect.set(InViewRect);
    }

    public boolean IsValidViewRectangle()
    {
        return (ConstrainedViewRect.x >= 0) &&
                (ConstrainedViewRect.y >= 0) &&
                (ConstrainedViewRect.width > 0) &&
                (ConstrainedViewRect.height > 0);
    }

    public boolean IsPerspectiveProjection()
    {
        return ProjectionMatrix.m33 < 1.0f;
    }

    public Recti  GetViewRect() { return ViewRect; }
    public Recti  GetConstrainedViewRect() { return ConstrainedViewRect; }

    public void ComputeViewProjectionMatrix(Matrix4f out)
    {
//        return FTranslationMatrix(-ViewOrigin) * ViewRotationMatrix * ProjectionMatrix;
        out.setTranslate(-ViewOrigin.x, -ViewOrigin.y, -ViewOrigin.z);

        Matrix4f.mul(ViewRotationMatrix, out, out);
        Matrix4f.mul(ProjectionMatrix, out, out);
    }
}
