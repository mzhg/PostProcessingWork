package nv.visualFX.cloth.libs.dx;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import nv.visualFX.cloth.libs.Fabric;
import nv.visualFX.cloth.libs.Factory;

/**
 * Created by mazhen'gui on 2017/9/12.
 */

final class DxFabric implements Fabric{

    DxFabric(DxFactory factory, int numParticles, IntBuffer phaseIndices, IntBuffer sets,
             FloatBuffer restvalues, FloatBuffer stiffnessValues, IntBuffer indices, IntBuffer anchors,
             FloatBuffer tetherLengths, IntBuffer triangles, int id){

    }

    @Override
    public Factory getFactory() {
        return null;
    }

    @Override
    public int getNumPhases() {
        return 0;
    }

    @Override
    public int getNumRestvalues() {
        return 0;
    }

    @Override
    public int getNumStiffnessValues() {
        return 0;
    }

    @Override
    public int getNumSets() {
        return 0;
    }

    @Override
    public int getNumIndices() {
        return 0;
    }

    @Override
    public int getNumParticles() {
        return 0;
    }

    @Override
    public int getNumTethers() {
        return 0;
    }

    @Override
    public int getNumTriangles() {
        return 0;
    }

    @Override
    public void scaleRestvalues(float f) {

    }

    @Override
    public void scaleTetherLengths(float f) {

    }
}
