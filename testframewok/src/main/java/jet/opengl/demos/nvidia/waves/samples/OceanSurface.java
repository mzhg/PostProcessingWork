package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector2f;

import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Quadtree_Stats;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Savestate;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Simulation;

/**
 * Created by mazhen'gui on 2017/7/27.
 */

final class OceanSurface {

    private DistanceField pDistanceFieldModule; // Not owned!
    void AttachDistanceFieldModule( DistanceField pDistanceField ) { pDistanceFieldModule = pDistanceField; }

    // --------------------------------- Rendering routines -----------------------------------
    // Rendering
    void renderShaded(//		ID3D11DeviceContext* pDC,
                              Matrix4f matView,
                              Matrix4f matProj,
                              GFSDK_WaveWorks_Simulation hSim,
                              GFSDK_WaveWorks_Savestate hSavestate,
                              final ReadableVector2f windDir,
                              float steepness,
                               float amplitude,
                               float wavelength,
                               float speed,
                               float parallelness,
                               float totalTime){

    }

    void getQuadTreeStats(GFSDK_WaveWorks_Quadtree_Stats stats){

    }
}
