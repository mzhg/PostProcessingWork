package jet.opengl.renderer.Unreal4;

import jet.opengl.postprocessing.util.StackLong;
import jet.opengl.renderer.Unreal4.utils.TBitArray;

/** Mobile only. Information used to determine whether static meshes will be rendered with CSM shaders or not. */
public class FMobileCSMVisibilityInfo {
    /** true if there are any primitives affected by CSM subjects */
    public boolean bMobileDynamicCSMInUse = false;

    // true if all draws should be forced to use CSM shaders.
    public boolean bAlwaysUseCSM = false;

    /** Visibility lists for static meshes that will use expensive CSM shaders. */
    public final TBitArray MobilePrimitiveCSMReceiverVisibilityMap = new TBitArray();
    public final TBitArray MobileCSMStaticMeshVisibilityMap = new TBitArray();
    public final StackLong MobileCSMStaticBatchVisibility = new StackLong();

    /** Visibility lists for static meshes that will use the non CSM shaders. */
    public final TBitArray MobileNonCSMStaticMeshVisibilityMap = new TBitArray();
    public final StackLong MobileNonCSMStaticBatchVisibility = new StackLong();


}
