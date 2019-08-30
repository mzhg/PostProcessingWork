package jet.opengl.renderer.Unreal4;

import java.util.ArrayList;
import java.util.List;

final class FSortedShadowMapAtlas {
    final FShadowMapRenderTargetsRefCounted RenderTargets = new FShadowMapRenderTargetsRefCounted();
    final List<FProjectedShadowInfo> Shadows = new ArrayList<>();
}
