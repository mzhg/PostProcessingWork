package jet.opengl.renderer.Unreal4.scenes;

import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.HashSet;

import jet.opengl.renderer.Unreal4.FPrimitiveComponentId;
import jet.opengl.renderer.Unreal4.UE4Engine;

// Construction parameters for a FSceneView
public class FSceneViewInitOptions extends FSceneViewProjectionData {
    public FSceneViewFamily ViewFamily;
    public FSceneViewStateInterface SceneViewStateInterface;
//	const AActor* ViewActor;
    public int PlayerIndex = UE4Engine.INDEX_NONE;
//    FViewElementDrawer* ViewElementDrawer;

    public final Vector4f BackgroundColor= new Vector4f();
    public final Vector4f OverlayColor = new Vector4f();
    public final Vector4f ColorScale = new Vector4f(1,1,1,1);

    /** For stereoscopic rendering, whether or not this is a full pass, or a left / right eye pass */
//    EStereoscopicPass StereoPass;

    /** For stereoscopic scene capture rendering. Half of the view's stereo IPD (- for lhs, + for rhs) */
    public float StereoIPD;

    /** Conversion from world units (uu) to meters, so we can scale motion to the world appropriately */
    public float WorldToMetersScale = 100;

    public HashSet<FPrimitiveComponentId> HiddenPrimitives;

    /** The primitives which are visible for this view. If the array is not empty, all other primitives will be hidden. */
    public HashSet<FPrimitiveComponentId> ShowOnlyPrimitives;

    // -1,-1 if not setup
    public final Vector2i CursorPos = new Vector2i(-1,-1);

    public float LODDistanceFactor = 1;

    /** If > 0, overrides the view's far clipping plane with a plane at the specified distance. */
    public float OverrideFarClippingPlaneDistance = -1;

    /** World origin offset value. Non-zero only for a single frame when origin is rebased */
    public final Vector3f OriginOffsetThisFrame = new Vector3f();

    /** Was there a camera cut this frame? */
    public boolean bInCameraCut;

    /** Whether to use FOV when computing mesh LOD. */
    public boolean bUseFieldOfViewForLOD = true;

    /** Actual field of view and that desired by the camera originally */
    public float FOV = 90;
    public float DesiredFOV = 90;

//#if WITH_EDITOR
    /** default to 0'th view index, which is a bitfield of 1 */
    public long EditorViewBitflag = 1;

    /** this can be specified for ortho views so that it's min draw distance/LOD parenting etc, is controlled by a perspective viewport */
    public final Vector3f OverrideLODViewOrigin = new Vector3f();

    /** In case of ortho, generate a fake view position that has a non-zero W component. The view position will be derived based on the view matrix. */
    public boolean bUseFauxOrthoViewPos;

    /** Whether game screen percentage should be disabled. */
    public boolean bDisableGameScreenPercentage;
}
