package jet.opengl.renderer.Unreal4.views;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.renderer.Unreal4.UE4Engine;
import jet.opengl.renderer.Unreal4.scenes.FSceneView;
import jet.opengl.renderer.Unreal4.scenes.FSceneViewInitOptions;
import jet.opengl.renderer.Unreal4.utils.FMirrorMatrix;

public class FViewMatrices {
    /** ViewToClip : UE4 projection matrix projects such that clip space Z=1 is the near plane, and Z=0 is the infinite far plane. */
    private final Matrix4f ProjectionMatrix = new Matrix4f();
    /** ViewToClipNoAA : UE4 projection matrix projects such that clip space Z=1 is the near plane, and Z=0 is the infinite far plane. Don't apply any AA jitter */
    private final Matrix4f		ProjectionNoAAMatrix = new Matrix4f();
    /** ClipToView : UE4 projection matrix projects such that clip space Z=1 is the near plane, and Z=0 is the infinite far plane. */
    private final Matrix4f		InvProjectionMatrix= new Matrix4f();
    // WorldToView..
    private final Matrix4f		ViewMatrix= new Matrix4f();
    // ViewToWorld..
    private final Matrix4f		InvViewMatrix = new Matrix4f();
    // WorldToClip : UE4 projection matrix projects such that clip space Z=1 is the near plane, and Z=0 is the infinite far plane. */
    private final Matrix4f		ViewProjectionMatrix = new Matrix4f();
    // ClipToWorld : UE4 projection matrix projects such that clip space Z=1 is the near plane, and Z=0 is the infinite far plane. */
    private final Matrix4f		InvViewProjectionMatrix = new Matrix4f();
    // HMD WorldToView with roll removed
    private final Matrix4f		HMDViewMatrixNoRoll = new Matrix4f();
    /** WorldToView with PreViewTranslation. */
    private final Matrix4f		TranslatedViewMatrix = new Matrix4f();
    /** ViewToWorld with PreViewTranslation. */
    private final Matrix4f		InvTranslatedViewMatrix = new Matrix4f();
    /** WorldToView with PreViewTranslation. */
    private final Matrix4f		OverriddenTranslatedViewMatrix = new Matrix4f();
    /** ViewToWorld with PreViewTranslation. */
    private final Matrix4f		OverriddenInvTranslatedViewMatrix = new Matrix4f();
    /** The view-projection transform, starting from world-space points translated by -ViewOrigin. */
    private final Matrix4f		TranslatedViewProjectionMatrix = new Matrix4f();
    /** The inverse view-projection transform, ending with world-space points translated by -ViewOrigin. */
    private final Matrix4f		InvTranslatedViewProjectionMatrix = new Matrix4f();
    /** The translation to apply to the world before TranslatedViewProjectionMatrix. Usually it is -ViewOrigin but with rereflections this can differ */
    private final Vector3f      PreViewTranslation = new Vector3f();
    /** To support ortho and other modes this is redundant, in world space */
    private final Vector3f		ViewOrigin = new Vector3f();
    /** Scale applied by the projection matrix in X and Y. */
    private final Vector2f      ProjectionScale = new Vector2f();
    /** TemporalAA jitter offset currently stored in the projection matrix */
    private final Vector2f	    TemporalAAProjectionJitter = new Vector2f();

    private final Vector3f      ViewRight = new Vector3f();
    private final Vector3f      ViewUp = new Vector3f();
    private final Vector3f      ViewForward = new Vector3f();

    /**
     * Scale factor to use when computing the size of a sphere in pixels.<p></p>
     *
     * A common calculation is to determine the size of a sphere in pixels when projected on the screen:<br>
     *		ScreenRadius = max(0.5 * ViewSizeX * ProjMatrix[0][0], 0.5 * ViewSizeY * ProjMatrix[1][1]) * SphereRadius / ProjectedSpherePosition.W<p></p>
     * Instead you can now simply use:<br>
     *		ScreenRadius = ScreenScale * SphereRadius / ProjectedSpherePosition.W
     */
    private float ScreenScale = 1;

    public FViewMatrices()
    {
        /*ProjectionMatrix.SetIdentity();
        ViewMatrix.SetIdentity();
        HMDViewMatrixNoRoll.SetIdentity();
        TranslatedViewMatrix.SetIdentity();
        TranslatedViewProjectionMatrix.SetIdentity();
        InvTranslatedViewProjectionMatrix.SetIdentity();
        PreViewTranslation = FVector::ZeroVector;
        ViewOrigin = FVector::ZeroVector;
        ProjectionScale = FVector2D::ZeroVector;
        TemporalAAProjectionJitter = FVector2D::ZeroVector;*/
        ScreenScale = 1.f;
    }

    //
    // World = TranslatedWorld - PreViewTranslation
    // TranslatedWorld = World + PreViewTranslation
    //
    public FViewMatrices(FSceneViewInitOptions InitOptions){
        throw new UnsupportedOperationException();
    }

    public void UpdateViewMatrix(ReadableVector3f ViewLocation, ReadableVector3f ViewRotation){
        throw new UnsupportedOperationException();
    }

    public void UpdatePlanarReflectionViewMatrix(FSceneView SourceView, FMirrorMatrix MirrorMatrix){
        throw new UnsupportedOperationException();
    }

    public Matrix4f GetProjectionMatrix()
    {
        return ProjectionMatrix;
    }

    public Matrix4f GetProjectionNoAAMatrix()
    {
        return ProjectionNoAAMatrix;
    }

    public Matrix4f GetInvProjectionMatrix()
    {
        return InvProjectionMatrix;
    }

    public Matrix4f GetViewMatrix()
    {
        return ViewMatrix;
    }

    public Matrix4f GetInvViewMatrix()
    {
        return InvViewMatrix;
    }

    public Matrix4f GetViewProjectionMatrix()
    {
        return ViewProjectionMatrix;
    }

    public Matrix4f GetInvViewProjectionMatrix()
    {
        return InvViewProjectionMatrix;
    }

    public Matrix4f GetHMDViewMatrixNoRoll()
    {
        return HMDViewMatrixNoRoll;
    }

    public Matrix4f GetTranslatedViewMatrix()
    {
        return TranslatedViewMatrix;
    }

    public Matrix4f GetInvTranslatedViewMatrix()
    {
        return InvTranslatedViewMatrix;
    }

    public Matrix4f GetOverriddenTranslatedViewMatrix()
    {
        return OverriddenTranslatedViewMatrix;
    }

    public Matrix4f GetOverriddenInvTranslatedViewMatrix()
    {
        return OverriddenInvTranslatedViewMatrix;
    }

    public Matrix4f GetTranslatedViewProjectionMatrix()
    {
        return TranslatedViewProjectionMatrix;
    }

    public Matrix4f GetInvTranslatedViewProjectionMatrix()
    {
        return InvTranslatedViewProjectionMatrix;
    }

    public ReadableVector3f GetPreViewTranslation()
    {
        return PreViewTranslation;
    }

    public ReadableVector3f GetViewOrigin()
    {
        return ViewOrigin;
    }

    public float GetScreenScale()
    {
        return ScreenScale;
    }

    public ReadableVector2f GetProjectionScale()
    {
        return ProjectionScale;
    }

    /** @return true:perspective, false:orthographic */
    public boolean IsPerspectiveProjection()
    {
        return ProjectionMatrix.m33 < 1.0f;
    }

    public void HackOverrideViewMatrixForShadows(Matrix4f InViewMatrix)
    {
//        OverriddenTranslatedViewMatrix = ViewMatrix = InViewMatrix;
//        OverriddenInvTranslatedViewMatrix = InViewMatrix.Inverse();
        OverriddenTranslatedViewMatrix.load(InViewMatrix);
        ViewMatrix.load(InViewMatrix);

        Matrix4f.decompseRigidMatrix(ViewMatrix, null, ViewRight, ViewUp, ViewForward);
        ViewForward.scale(-1);  // In OpenGL the view look at negative direction.

        Matrix4f.invert(InViewMatrix, OverriddenInvTranslatedViewMatrix);
    }

    public ReadableVector3f GetViewRight() { return ViewRight;}
    public ReadableVector3f GetViewUp() { return ViewUp;}
    public ReadableVector3f GetViewDirection() { return ViewForward;}

    public void SaveProjectionNoAAMatrix()
    {
        ProjectionNoAAMatrix.load(ProjectionMatrix);
    }

    public void HackAddTemporalAAProjectionJitter(ReadableVector2f InTemporalAAProjectionJitter)
    {
        UE4Engine.ensure(TemporalAAProjectionJitter.getX() == 0.0f && TemporalAAProjectionJitter.getY() == 0.0f);

        TemporalAAProjectionJitter.set(InTemporalAAProjectionJitter);

        ProjectionMatrix.m20 += TemporalAAProjectionJitter.getX();  // TODO
        ProjectionMatrix.m21 += TemporalAAProjectionJitter.getY();
//        InvProjectionMatrix = InvertProjectionMatrix(ProjectionMatrix);
        Matrix4f.invert(ProjectionMatrix, InvProjectionMatrix);

        RecomputeDerivedMatrices();
    }

    public void HackRemoveTemporalAAProjectionJitter()
    {
        ProjectionMatrix.m20 -= TemporalAAProjectionJitter.getX();  // TODO
        ProjectionMatrix.m21 -= TemporalAAProjectionJitter.getY();
//        InvProjectionMatrix = InvertProjectionMatrix(ProjectionMatrix);
        Matrix4f.invert(ProjectionMatrix, InvProjectionMatrix);

        TemporalAAProjectionJitter.set(0,0);
        RecomputeDerivedMatrices();
    }

    public void ComputeProjectionNoAAMatrix(Matrix4f OutProjNoAAMatrix)
    {
        OutProjNoAAMatrix.load(ProjectionMatrix);

        OutProjNoAAMatrix.m20 -= TemporalAAProjectionJitter.x;
        OutProjNoAAMatrix.m21 -= TemporalAAProjectionJitter.y;
    }

    public ReadableVector2f GetTemporalAAJitter()
    {
        return TemporalAAProjectionJitter;
    }

    public void ComputeViewRotationProjectionMatrix(Matrix4f Out)
    {
//        return ViewMatrix.RemoveTranslation() * ProjectionMatrix;
        Out.load(ViewMatrix);

        // Remove any translation from this matrix
        Out.m30 = 0;
        Out.m31 = 0;
        Out.m32 = 0;

        Matrix4f.mul(ProjectionMatrix, Out, Out);
    }

	public void ComputeInvProjectionNoAAMatrix(Matrix4f OutInvProjectionNoAAMatrix)
    {
//        return InvertProjectionMatrix( ComputeProjectionNoAAMatrix() );
        ComputeProjectionNoAAMatrix(OutInvProjectionNoAAMatrix);
        OutInvProjectionNoAAMatrix.invert();
    }

    // @return in radians (horizontal,vertical)
	public void ComputeHalfFieldOfViewPerAxis(Vector2f Out)
    {
//		const FMatrix ClipToView = ComputeInvProjectionNoAAMatrix();
        final Matrix4f ClipToView = CacheBuffer.getCachedMatrix();
        ComputeInvProjectionNoAAMatrix(ClipToView);

        /*FVector VCenter = FVector(ClipToView.TransformPosition(FVector(0.0, 0.0, 0.0)));
        FVector VUp = FVector(ClipToView.TransformPosition(FVector(0.0, 1.0, 0.0)));
        FVector VRight = FVector(ClipToView.TransformPosition(FVector(1.0, 0.0, 0.0)));

        VCenter.Normalize();
        VUp.Normalize();
        VRight.Normalize();*/

        final Vector3f VCenter = CacheBuffer.getCachedVec3();
        final Vector3f VUp = CacheBuffer.getCachedVec3();
        final Vector3f VRight = CacheBuffer.getCachedVec3();

        Matrix4f.transformVector(ClipToView, Vector3f.ZERO, VCenter);  // TODO
        Matrix4f.transformVector(ClipToView, Vector3f.Y_AXIS, VUp);
        Matrix4f.transformVector(ClipToView, Vector3f.X_AXIS, VRight);

//        return FVector2D(FMath::Acos(VCenter | VRight), FMath::Acos(VCenter | VUp));
        Out.set((float)Math.acos(Vector3f.dot(VCenter, VRight)), (float)Math.acos(Vector3f.dot(VCenter, VUp)));

        CacheBuffer.free(ClipToView);
        CacheBuffer.free(VCenter);
        CacheBuffer.free(VUp);
        CacheBuffer.free(VRight);
    }

    public void ApplyWorldOffset(ReadableVector3f InOffset)
    {
//        ViewOrigin+= InOffset;
//        PreViewTranslation-= InOffset;

        Vector3f.add(ViewOrigin, InOffset, ViewOrigin);
        Vector3f.sub(PreViewTranslation, InOffset, PreViewTranslation);

//        ViewMatrix.SetOrigin(ViewMatrix.GetOrigin() + ViewMatrix.TransformVector(-InOffset));
        final Vector3f temp0 = CacheBuffer.getCachedVec3();
        final Vector3f temp1 = CacheBuffer.getCachedVec3();

        Vector3f.scale(InOffset, -1, temp0);
        Matrix4f.transformNormal(ViewMatrix, temp0, temp1);

        ViewMatrix.m30 += temp1.x;
        ViewMatrix.m31 += temp1.y;
        ViewMatrix.m32 += temp1.z;

//        InvViewMatrix.SetOrigin(ViewOrigin);
        InvViewMatrix.m30 = ViewOrigin.x;
        InvViewMatrix.m31 = ViewOrigin.y;
        InvViewMatrix.m32 = ViewOrigin.z;

        CacheBuffer.free(temp0);
        CacheBuffer.free(temp1);

        RecomputeDerivedMatrices();
    }

    private void RecomputeDerivedMatrices()
    {
        // Compute the view projection matrix and its inverse.
//        ViewProjectionMatrix = GetViewMatrix() * GetProjectionMatrix();
//        InvViewProjectionMatrix = GetInvProjectionMatrix() * GetInvViewMatrix();

        Matrix4f.mul(GetProjectionMatrix(), GetViewMatrix(), ViewProjectionMatrix);
        Matrix4f.mul(GetInvViewMatrix(), GetInvProjectionMatrix(), InvViewProjectionMatrix);

        // Compute a transform from view origin centered world-space to clip space.
//        TranslatedViewProjectionMatrix = GetTranslatedViewMatrix() * GetProjectionMatrix();
//        InvTranslatedViewProjectionMatrix = GetInvProjectionMatrix() * GetInvTranslatedViewMatrix();
        Matrix4f.mul(GetProjectionMatrix(), GetTranslatedViewMatrix(), TranslatedViewProjectionMatrix);
        Matrix4f.mul(GetInvTranslatedViewMatrix(), GetInvProjectionMatrix(), InvTranslatedViewProjectionMatrix);
    }

//    static const FMatrix InvertProjectionMatrix( const FMatrix& M )
//    {
//        if( M.M[1][0] == 0.0f &&
//                M.M[3][0] == 0.0f &&
//                M.M[0][1] == 0.0f &&
//                M.M[3][1] == 0.0f &&
//                M.M[0][2] == 0.0f &&
//                M.M[1][2] == 0.0f &&
//                M.M[0][3] == 0.0f &&
//                M.M[1][3] == 0.0f &&
//                M.M[2][3] == 1.0f &&
//                M.M[3][3] == 0.0f )
//        {
//            // Solve the common case directly with very high precision.
//			/*
//			M =
//			| a | 0 | 0 | 0 |
//			| 0 | b | 0 | 0 |
//			| s | t | c | 1 |
//			| 0 | 0 | d | 0 |
//			*/
//
//            double a = M.M[0][0];
//            double b = M.M[1][1];
//            double c = M.M[2][2];
//            double d = M.M[3][2];
//            double s = M.M[2][0];
//            double t = M.M[2][1];
//
//            return FMatrix(
//                    FPlane( 1.0 / a, 0.0f, 0.0f, 0.0f ),
//                    FPlane( 0.0f, 1.0 / b, 0.0f, 0.0f ),
//                    FPlane( 0.0f, 0.0f, 0.0f, 1.0 / d ),
//                    FPlane( -s/a, -t/b, 1.0f, -c/d )
//            );
//        }
//        else
//        {
//            return M.Inverse();
//        }
//    }
}
