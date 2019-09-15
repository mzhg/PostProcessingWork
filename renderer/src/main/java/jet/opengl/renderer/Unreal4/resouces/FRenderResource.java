package jet.opengl.renderer.Unreal4.resouces;

import java.util.LinkedList;
import java.util.List;

import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.renderer.Unreal4.api.ERHIFeatureLevel;

/**
 * A rendering resource which is owned by the rendering thread.
 */
public class FRenderResource {
    /** True if the resource has been initialized. */
    private boolean bInitialized;

//    private final LinkedList<FRenderResource> ResourceLink = new LinkedList<>();

    // This is used during mobile editor preview refactor, this will eventually be replaced with a parameter to InitRHI() etc..
    protected int GetFeatureLevel()  { return FeatureLevel == ERHIFeatureLevel.Num ? ERHIFeatureLevel.SM5 : FeatureLevel; }
    protected boolean HasValidFeatureLevel()  { return FeatureLevel < ERHIFeatureLevel.Num; }

    protected int FeatureLevel = ERHIFeatureLevel.Num;

    public FRenderResource(){}

    public FRenderResource(int InFeatureLevel){
        FeatureLevel = InFeatureLevel;
    }

    private static final LinkedList<FRenderResource> FirstResourceLink = new LinkedList<>();

    public static List<FRenderResource> GetResourceList(){
        return FirstResourceLink;
    }

    public static void ChangeFeatureLevel(int NewFeatureLevel){
        for (FRenderResource Resource : GetResourceList())
        {
            // Only resources configured for a specific feature level need to be updated
            if (Resource.HasValidFeatureLevel())
            {
                Resource.ReleaseRHI();
                Resource.ReleaseDynamicRHI();
                Resource.FeatureLevel = NewFeatureLevel;
                Resource.InitDynamicRHI();
                Resource.InitRHI();
            }
        }
    }

    /**
     * Initializes the dynamic RHI resource and/or RHI render target used by this resource.
     * Called when the resource is initialized, or when reseting all RHI resources.
     * Resources that need to initialize after a D3D device reset must implement this function.
     * This is only called by the rendering thread.
     */
    public void InitDynamicRHI() {}

    /**
     * Releases the dynamic RHI resource and/or RHI render target resources used by this resource.
     * Called when the resource is released, or when reseting all RHI resources.
     * Resources that need to release before a D3D device reset must implement this function.
     * This is only called by the rendering thread.
     */
    public void ReleaseDynamicRHI() {}

    /**
     * Initializes the RHI resources used by this resource.
     * Called when entering the state where both the resource and the RHI have been initialized.
     * This is only called by the rendering thread.
     */
    public void InitRHI() {}

    /**
     * Releases the RHI resources used by this resource.
     * Called when leaving the state where both the resource and the RHI have been initialized.
     * This is only called by the rendering thread.
     */
    public void ReleaseRHI() {}

    /**
     * Initializes the resource.
     * This is only called by the rendering thread.
     */
    public void InitResource(){
        FirstResourceLink.add(this);

        if(GLFuncProviderFactory.isInitlized())
        {
//            CSV_SCOPED_TIMING_STAT_EXCLUSIVE(InitRenderResource);
            InitDynamicRHI();
            InitRHI();
        }

//        FPlatformMisc::MemoryBarrier(); // there are some multithreaded reads of bInitialized
        bInitialized = true;
    }

    /**
     * Prepares the resource for deletion.
     * This is only called by the rendering thread.
     */
    public void ReleaseResource(){
        if(bInitialized)
        {
            if(GLFuncProviderFactory.isInitlized())
            {
                ReleaseRHI();
                ReleaseDynamicRHI();
            }
//#if PLATFORM_NEEDS_RHIRESOURCELIST
//            ResourceLink.Unlink();
//#endif
                FirstResourceLink.remove(this);
                bInitialized = false;
        }
    }

    /**
     * If the resource's RHI resources have been initialized, then release and reinitialize it.  Otherwise, do nothing.
     * This is only called by the rendering thread.
     */
    public void UpdateRHI(){
//        check(IsInRenderingThread());
        if(bInitialized && GLFuncProviderFactory.isInitlized())
        {
            ReleaseRHI();
            ReleaseDynamicRHI();
            InitDynamicRHI();
            InitRHI();
        }
    }

    // Probably temporary code that sends a task back to renderthread_local and blocks waiting for it to call InitResource
    public void InitResourceFromPossiblyParallelRendering(){
        InitResource();
    }

    /** @return The resource's friendly name.  Typically a UObject name. */
    public String GetFriendlyName() { return ("undefined"); }

    // Accessors.
    public boolean IsInitialized() { return bInitialized; }

    /** Initialize all resources initialized before the RHI was initialized */
    public static void InitPreRHIResources(){
        // Notify all initialized FRenderResources that there's a valid RHI device to create their RHI resources for now.
        for (FRenderResource Resource : GetResourceList()){
            Resource.InitRHI();
        }

        // Dynamic resources can have dependencies on static resources (with uniform buffers) and must initialized last!
        for (FRenderResource Resource : GetResourceList()){
            Resource.InitDynamicRHI();
        }
    }
}
