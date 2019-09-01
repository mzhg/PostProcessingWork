package jet.opengl.renderer.Unreal4.api;

public enum ESimpleRenderTargetMode {
    // These will all store out color and depth
    EExistingColorAndDepth,							// Color = Existing, Depth = Existing
    EUninitializedColorAndDepth,					// Color = ????, Depth = ????
    EUninitializedColorExistingDepth,				// Color = ????, Depth = Existing
    EUninitializedColorClearDepth,					// Color = ????, Depth = Default
    EClearColorExistingDepth,						// Clear Color = whatever was bound to the rendertarget at creation time. Depth = Existing
    EClearColorAndDepth,							// Clear color and depth to bound clear values.
    EExistingContents_NoDepthStore,					// Load existing contents, but don't store depth out.  depth can be written.
    EExistingColorAndClearDepth,					// Color = Existing, Depth = clear value
    EExistingColorAndDepthAndClearStencil,			// Color = Existing, Depth = Existing, Stencil = clear

    // If you add an item here, make sure to add it to DecodeRenderTargetMode() as well!
}
