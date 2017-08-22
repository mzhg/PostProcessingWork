package jet.opengl.demos.nvidia.water;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/8/22.
 */

public class MultiWaterWaveSimulator implements WaterWaveSimulator {
    private final List<WaterWaveSimulator> m_WaterSimulators = new ArrayList<>();

    public void addSimulator(WaterWaveSimulator simulator){
        if(!m_WaterSimulators.contains(simulator)){
            m_WaterSimulators.add(simulator);
        }
    }

    public int getSimulatorCount() { return m_WaterSimulators.size();}

    public Texture2D getDisplacementMap(int index) {
        return m_WaterSimulators.get(index).getDisplacementMap();
    }

    public Texture2D getGradMap(int index) {
        return m_WaterSimulators.get(index).getGradMap();
    }

    public Texture2D getNormalMap(int index) {
        return m_WaterSimulators.get(index).getNormalMap();
    }

    @Override
    public Texture2D getDisplacementMap() {
        return getDisplacementMap(0);
    }

    @Override
    public Texture2D getGradMap() {
        return getGradMap(0);
    }

    @Override
    public Texture2D getNormalMap() {
        return getNormalMap(0);
    }

    @Override
    public void updateSimulation(float time) {
        int count = m_WaterSimulators.size();
        for(int i = 0; i < count; i++){
            m_WaterSimulators.get(i).updateSimulation(time);
        }
    }

    @Override
    public void dispose() {
        for(WaterWaveSimulator simulator : m_WaterSimulators){
            simulator.dispose();
        }
    }
}
