package jet.opengl.demos.nvidia.volumelight;

import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.Pair;

class RenderVolumeCPUProgram extends RenderVolumeProgram {
    public RenderVolumeCPUProgram(ContextImp_OpenGL context, RenderVolumeDesc desc) {
        super(context, desc);

        if(desc.includeTesslation){
            throw new IllegalArgumentException();
        }
    }

    @Override
    protected Pair<String, Macro[]> getVSShader() {
        if(desc.useQuadVS)
            return new Pair<>("Quad_VS.vert", null);

        String filename = "RenderVolume_Vertex.vert";

        Macro[] vs_macros = {
                new Macro("MESHMODE_FRUSTUM_GRID", 1),
                new Macro("MESHMODE_FRUSTUM_BASE", 2),
                new Macro("MESHMODE_FRUSTUM_CAP", 3),
                new Macro("MESHMODE_OMNI_VOLUME", 4),
                new Macro("MESHMODE_GEOMETRY", 5),
                new Macro("MESHMODE", desc.meshMode),

                new Macro("SHADOWMAPTYPE_ATLAS", RenderVolumeDesc.SHADOWMAPTYPE_ATLAS),
                new Macro("SHADOWMAPTYPE_ARRAY", RenderVolumeDesc.SHADOWMAPTYPE_ARRAY),
                new Macro   ("SHADOWMAPTYPE", desc.shadowMapType),

                new Macro("CASCADECOUNT_1", RenderVolumeDesc.CASCADECOUNT_1),
                new Macro("CASCADECOUNT_2", RenderVolumeDesc.CASCADECOUNT_2),
                new Macro("CASCADECOUNT_3", RenderVolumeDesc.CASCADECOUNT_3),
                new Macro("CASCADECOUNT_4", RenderVolumeDesc.CASCADECOUNT_4),
                new Macro   ("CASCADECOUNT", desc.cascadeCount),

                new Macro("VOLUMETYPE_FRUSTUM", RenderVolumeDesc.VOLUMETYPE_FRUSTUM),
                new Macro("VOLUMETYPE_PARABOLOID", RenderVolumeDesc.VOLUMETYPE_PARABOLOID),
                new Macro   ("VOLUMETYPE", desc.volumeType),
        };

        return new Pair<>(filename, vs_macros);
    }

    @Override
    protected Pair<String, Macro[]> getHSShader() {
        return null;
    }

    @Override
    protected Pair<String, Macro[]> getDSShader() {
        return null;
    }
}
