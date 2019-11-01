package jet.opengl.demos.nvidia.waves.crest;

import jet.opengl.demos.nvidia.waves.ocean.Technique;

/**
 * Registers a custom input to the wave shape. Attach this GameObjects that you want to render into the displacmeent textures to affect ocean shape.
 */
final class Wave_Simulation_Animation_Input extends Wave_Simulation_Common_Input{

    private Wave_Simulation m_Simulation;
    private Wave_CDClipmap m_Clipmap;

    Wave_Simulation_Animation_Input(Technique material, Wave_Mesh mesh, Wave_Simulation simulation, Wave_CDClipmap clipmap) {
        super(material, mesh);

        m_Simulation = simulation;
        m_Clipmap = clipmap;
    }

    @Override
    protected void update() {
        float maxDispVert = 0f;
        final float _maxDisplacementHorizontal = m_Simulation.m_Params.max_displacement_horizontal;
        final float _maxDisplacementVertical = m_Simulation.m_Params.max_displacement_Vertical;

        // let ocean system know how far from the sea level this shape may displace the surface
        if (m_Simulation.m_Params.report_bounds_to_system)
        {
            /*var minY = _rend.bounds.min.y;
            var maxY = _rend.bounds.max.y;  todo
            var seaLevel = OceanRenderer.Instance.SeaLevel;
            maxDispVert = Mathf.Max(Mathf.Abs(seaLevel - minY), Mathf.Abs(seaLevel - maxY));*/
        }

        maxDispVert = Math.max(maxDispVert, _maxDisplacementVertical);

        if (_maxDisplacementHorizontal > 0f || _maxDisplacementVertical > 0f)
        {
            m_Clipmap.ReportMaxDisplacementFromShape(_maxDisplacementHorizontal, maxDispVert, 0f);
        }
    }
}
