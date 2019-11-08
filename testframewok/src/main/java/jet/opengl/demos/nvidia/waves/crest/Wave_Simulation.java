package jet.opengl.demos.nvidia.waves.crest;

public class Wave_Simulation {

    final Wave_Simulation_Params m_Params = new Wave_Simulation_Params();

    Wave_Simulation_Animation_Pass _lodDataAnimWaves;
    Wave_Simulation_Flow_Pass _lodDataFlow;
    Wave_Simulation_SeaFloorDepth_Pass _lodDataSeaDepths;
    Wave_Simulation_Dynamic_Pass _lodDataDynWaves;
    Wave_Simulation_Foam_Pass _lodDataFoam;
    Wave_Simulation_Shadow_Pass _lodDataShadow;

    Wave_Collision_Provider collision_provider;

    public void init(Wave_CDClipmap clipmap, Wave_Simulation_Params params, Wave_Demo_Animation animation){
        m_Params.set(params);

        // Create the LOD data managers
        _lodDataAnimWaves = new Wave_Simulation_Animation_Pass(animation);
        _lodDataAnimWaves.init(clipmap, this);
        if (m_Params.create_dynamic_wave) {
            _lodDataDynWaves = new Wave_Simulation_Dynamic_Pass();
            _lodDataDynWaves.init(clipmap, this);
        }
        if (m_Params.create_flow) {
            _lodDataFlow = new Wave_Simulation_Flow_Pass();
            _lodDataFlow.init(clipmap, this);
        }
        if (m_Params.create_foam) {
            _lodDataFoam = new Wave_Simulation_Foam_Pass();
            _lodDataFoam.init(clipmap, this);
        }
        if (m_Params.CreateShadowData) {
            _lodDataShadow = new Wave_Simulation_Shadow_Pass();
            _lodDataShadow.init(clipmap, this);
        }
        if (m_Params.create_seafloordepth) {
            _lodDataSeaDepths = new Wave_Simulation_SeaFloorDepth_Pass();
            _lodDataSeaDepths.init(clipmap, this);
        }
    }

    public void update(float deltaTime){
        lateUpdateLods();
        Build(deltaTime);
    }

    private void lateUpdateLods()
    {
        if (_lodDataAnimWaves != null) _lodDataAnimWaves.UpdateLodData();
        if (_lodDataDynWaves != null) _lodDataDynWaves.UpdateLodData();
        if (_lodDataFlow != null) _lodDataFlow.UpdateLodData();
        if (_lodDataFoam != null) _lodDataFoam.UpdateLodData();
        if (_lodDataSeaDepths != null) _lodDataSeaDepths.UpdateLodData();
        if (_lodDataShadow != null) _lodDataShadow.UpdateLodData();
    }

    private void Build(float deltaTime)
    {
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // --- Ocean depths
        if (_lodDataSeaDepths !=null)
        {
            _lodDataSeaDepths.BuildCommandBuffer(deltaTime);
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // --- Flow data
        if (_lodDataFlow != null)
        {
            _lodDataFlow.BuildCommandBuffer(deltaTime);
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // --- Dynamic wave simulations
        if (_lodDataDynWaves != null)
        {
            _lodDataDynWaves.BuildCommandBuffer(deltaTime);
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // --- Animated waves next
        if (_lodDataAnimWaves != null)
        {
            _lodDataAnimWaves.BuildCommandBuffer(deltaTime);
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // --- Foam simulation
        if (_lodDataFoam != null)
        {
            _lodDataFoam.BuildCommandBuffer(deltaTime);
        }
    }
}
