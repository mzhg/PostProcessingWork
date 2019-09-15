package jet.opengl.renderer.Unreal4.mesh;

/**
 * FMeshDrawCommand fully describes a mesh pass draw call, captured just above the RHI. <br>
 FMeshDrawCommand should contain only data needed to draw.  For InitViews payloads, use FVisibleMeshDrawCommand.<br>
 * FMeshDrawCommands are cached at Primitive AddToScene time for vertex factories that support it (no per-frame or per-view shader binding changes).
 * Dynamic Instancing operates at the FMeshDrawCommand level for robustness.  <br>
 Adding per-command shader bindings will reduce the efficiency of Dynamic Instancing, but rendering will always be correct.<br>
 * Any resources referenced by a command must be kept alive for the lifetime of the command.  FMeshDrawCommand is not responsible for lifetime management of resources.
 For uniform buffers referenced by cached FMeshDrawCommand's, RHIUpdateUniformBuffer makes it possible to access per-frame data in the shader without changing bindings.
 */
public class FMeshDrawCommand {
}
