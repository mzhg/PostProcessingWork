package jet.opengl.renderer.Unreal4.scenes;

/**
 * A priority for sorting scene elements by depth.<br>
 * Elements with higher priority occlude elements with lower priority, disregarding distance.
 */
public enum ESceneDepthPriorityGroup {
    /** World scene DPG. */
    SDPG_World,
    /** Foreground scene DPG. */
    SDPG_Foreground,
}
