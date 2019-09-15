package jet.opengl.renderer.Unreal4.hit;

/**
 * The priority a hit proxy has when choosing between several hit proxies near the point the user clicked.
 * HPP_World - this is the default priority
 * HPP_Wireframe - the priority of items that are drawn in wireframe, such as volumes
 * HPP_UI - the priority of the UI components such as the translation widget
 */
public enum EHitProxyPriority {
    HPP_World ,
    HPP_Wireframe ,
    HPP_Foreground ,
    HPP_UI
}
