package nv.visualFX.cloth.libs;

/**
 * Created by mazhen'gui on 2017/9/12.
 */

class SwFabric implements Fabric{
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
