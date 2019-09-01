package jet.opengl.renderer.Unreal4;

enum EVelocityPass {
    // Renders a separate velocity pass for opaques.
    Opaque,

    // Renders a separate velocity / depth pass for translucency AFTER the translucent pass.
    Translucency,
}
