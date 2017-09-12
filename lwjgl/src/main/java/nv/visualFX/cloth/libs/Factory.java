package nv.visualFX.cloth.libs;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * abstract factory to create context-specific simulation components
 * such as cloth, solver, collision, etc.
 * Created by mazhen'gui on 2017/9/9.
 */

public interface Factory {
    Platform getPlatform();

    /*
     Create fabric data used to setup cloth object.
     Look at the cooking extension for helper functions to create fabrics from meshes.
     The returned fabric will have a refcount of 1.
     @param numParticles number of particles, must be larger than any particle index
     @param phaseIndices map from phase to set index
     @param sets inclusive prefix sum of restvalue count per set
     @param restvalues array of constraint rest values
     @param indices array of particle index pair per constraint
    */
    Fabric createFabric(int numParticles, IntBuffer phaseIndices, IntBuffer sets,
                        FloatBuffer restvalues, FloatBuffer stiffnessValues, IntBuffer indices,
                        IntBuffer anchors, FloatBuffer tetherLengths,
                        IntBuffer triangles);

    /**
     Create cloth object.
     @param particles initial particle positions.
     @param fabric edge distance constraint structure
     */
    Cloth createCloth(FloatBuffer particles, Fabric fabric);

    /**
     \Create cloth solver object.
     */
    Solver createSolver();

    /**
     \brief Create a copy of a cloth instance
     @param cloth the instance to be cloned, need not match the factory type
     */
    Cloth clone(Cloth cloth);

    /**
     \brief Extract original data from a fabric object.
     Use the getNum* methods on Cloth to get the memory requirements before calling this function.
     @param fabric to extract from, must match factory type
     @param phaseIndices pre-allocated memory range to write phase => set indices
     @param sets pre-allocated memory range to write sets
     @param restvalues pre-allocated memory range to write restvalues
     @param indices pre-allocated memory range to write indices
     */
    void extractFabricData(Fabric fabric, IntBuffer phaseIndices, IntBuffer sets,
                                   FloatBuffer restvalues, FloatBuffer stiffnessValues, IntBuffer indices, IntBuffer anchors,
                                   FloatBuffer tetherLengths, IntBuffer triangles);

    /**
     \brief Extract current collision spheres and capsules from a cloth object.
     Use the getNum* methods on Cloth to get the memory requirements before calling this function.
     @param cloth the instance to extract from, must match factory type
     @param spheres pre-allocated memory range to write spheres
     @param capsules pre-allocated memory range to write capsules
     @param planes pre-allocated memory range to write planes
     @param convexes pre-allocated memory range to write convexes
     @param triangles pre-allocated memory range to write triangles
     */
    void extractCollisionData(Cloth cloth, FloatBuffer spheres, IntBuffer capsules,
                                      FloatBuffer planes, IntBuffer convexes, FloatBuffer triangles);

    /**
     Extract current motion constraints from a cloth object
     Use the getNum* methods on Cloth to get the memory requirements before calling this function.
     @param cloth the instance to extract from, must match factory type
     @param destConstraints pre-allocated memory range to write constraints
     */
    void extractMotionConstraints( Cloth cloth, FloatBuffer destConstraints);

    /**
     Extract current separation constraints from a cloth object
     @param cloth the instance to extract from, must match factory type
     @param destConstraints pre-allocated memory range to write constraints
     */
    void extractSeparationConstraints( Cloth cloth, FloatBuffer destConstraints);

    /**
     Extract current particle accelerations from a cloth object
     @param cloth the instance to extract from, must match factory type
     @param destAccelerations pre-allocated memory range to write accelerations
     */
    void extractParticleAccelerations( Cloth cloth, FloatBuffer destAccelerations);

    /**
     Extract particles from a cloth object
     @param cloth the instance to extract from, must match factory type
     @param destIndices pre-allocated memory range to write indices
     @param destWeights pre-allocated memory range to write weights
     */
    void extractVirtualParticles( Cloth cloth, IntBuffer destIndices,
                                         FloatBuffer destWeights);

    /**
     Extract self collision indices from cloth object.
     @param cloth the instance to extract from, must match factory type
     @param destIndices pre-allocated memory range to write indices
     */
    void extractSelfCollisionIndices(Cloth cloth, IntBuffer destIndices);

    /**
     Extract particle rest positions from cloth object.
     @param cloth the instance to extract from, must match factory type
     @param destRestPositions pre-allocated memory range to write rest positions
     */
    void extractRestPositions(Cloth cloth, FloatBuffer destRestPositions);
}
