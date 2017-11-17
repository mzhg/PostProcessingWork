package jet.opengl.demos.intel.va;

import java.util.UUID;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public class VaRenderMaterialConstructorParams implements VaConstructorParamsBase {
    VaRenderMaterialManager    RenderMaterialManager;
    UUID UID;
    public VaRenderMaterialConstructorParams(VaRenderMaterialManager renderMaterialManager,  UUID  uid ) /*: RenderMaterialManager(renderMaterialManager), UID( uid )*/ {
        RenderMaterialManager = renderMaterialManager;
        UID = uid;
    }
}
