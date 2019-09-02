package jet.opengl.renderer.Unreal4.scenes;

import jet.opengl.renderer.Unreal4.ESequencerState;
import jet.opengl.renderer.Unreal4.FReferenceCollector;
import jet.opengl.renderer.Unreal4.UE4Engine;

public abstract class FSceneViewStateInterface {
    /** This scene state's view parent; NULL if no parent present. */
    private FSceneViewStateInterface	ViewParent;
    /** Reference counts the number of children parented to this state. */
    private int							NumChildren;

    /** Called in the game thread to destroy the view state. */
    public abstract void Destroy();

    /** Sets the view state's scene parent. */
    public void SetViewParent(FSceneViewStateInterface InViewParent)
    {
        if ( ViewParent != null)
        {
            // Assert that the existing parent does not have a parent.
            UE4Engine.check( !ViewParent.HasViewParent() );
            // Decrement ref ctr of existing parent.
            --ViewParent.NumChildren;
        }

        if ( InViewParent != null && InViewParent != this )
        {
            // Assert that the incoming parent does not have a parent.
            UE4Engine.check( !InViewParent.HasViewParent() );
            ViewParent = InViewParent;
            // Increment ref ctr of new parent.
            InViewParent.NumChildren++;
        }
        else
        {
            ViewParent = null;
        }
    }
    /** @return			The view state's scene parent, or NULL if none present. */
    public FSceneViewStateInterface GetViewParent()
    {
        return ViewParent;
    }

    /** @return			true if the scene state has a parent, false otherwise. */
    public boolean HasViewParent()
    {
        return GetViewParent() != null;
    }
    /** @return			true if this scene state is a parent, false otherwise. */
    public boolean IsViewParent()
    {
        return NumChildren > 0;
    }

    /** @return	the derived view state object */
    public abstract FSceneViewState GetConcreteViewState ();

    public abstract void AddReferencedObjects(FReferenceCollector Collector);

    public int GetSizeBytes() { return 0; }

    /** Resets pool for GetReusableMID() */
    public abstract void OnStartPostProcessing(FSceneView CurrentView);

    /**
     * Allows MIDs being created and released during view rendering without the overhead of creating and releasing objects
     * As MID are not allowed to be parent of MID this gets fixed up by parenting it to the next Material or MIC
     * @param InSource can be Material, MIC or MID, must not be 0
     */
    public abstract UMaterialInstanceDynamic GetReusableMID(UMaterialInterface InSource);

//#if !(UE_BUILD_SHIPPING || UE_BUILD_TEST)
    /** If frozen view matrices are available, set those as active on the SceneView */
    public abstract void ActivateFrozenViewMatrices(FSceneView SceneView);

    /** If frozen view matrices were set, restore the previous view matrices */
    public abstract void RestoreUnfrozenViewMatrices(FSceneView SceneView);
//#endif
    // rest some state (e.g. FrameIndexMod8, TemporalAASampleIndex) to make the rendering [more] deterministic
    public abstract void ResetViewState();

    /** Returns the temporal LOD struct from the viewstate */
    public abstract FTemporalLODState GetTemporalLODState();

    /**
     * Returns the blend factor between the last two LOD samples
     */
    public abstract float GetTemporalLODTransition();

    /**
     * returns a unique key for the view state, non-zero
     */
    public abstract int GetViewKey();

    //
    public abstract int GetCurrentTemporalAASampleIndex() ;

    public abstract void SetSequencerState(ESequencerState InSequencerState);

    public abstract ESequencerState GetSequencerState();

    /** Returns the current PreExposure value. PreExposure is a custom scale applied to the scene color to prevent buffer overflow. */
    public abstract float GetPreExposure();

    /**
     * returns the occlusion frame counter
     */
    public abstract int GetOcclusionFrameCounter();
}
