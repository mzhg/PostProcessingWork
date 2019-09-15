package jet.opengl.renderer.Unreal4.views;

import java.util.ArrayList;

import jet.opengl.renderer.Unreal4.mesh.EMeshPass;
import jet.opengl.renderer.Unreal4.mesh.FStaticMeshBatch;
import jet.opengl.renderer.Unreal4.mesh.FVisibleMeshDrawCommand;

public class FViewCommands {
    public final ArrayList<FVisibleMeshDrawCommand>[] MeshCommands = new ArrayList[EMeshPass.values().length];
    public final int[] NumDynamicMeshCommandBuildRequestElements = new int[EMeshPass.values().length];
    public final ArrayList<FStaticMeshBatch>[] DynamicMeshCommandBuildRequests = new ArrayList[EMeshPass.values().length];
}
