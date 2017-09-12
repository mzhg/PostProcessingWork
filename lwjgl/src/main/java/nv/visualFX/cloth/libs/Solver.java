package nv.visualFX.cloth.libs;

import java.util.List;

/**
 * Created by mazhen'gui on 2017/9/9.
 */

public interface Solver {
    /// Adds cloth object.
    void addCloth(Cloth cloth);

    /// Removes cloth object.
    void removeCloth(Cloth cloth);

    /// Returns the numer of cloths added to the solver.
    int getNumCloths() ;

    /// Returns the pointer to the first cloth added to the solver
    List<Cloth> getClothList() ;

    // functions executing the simulation work.
    /** Begins a simulation frame.
     Returns false if there is nothing to simulate.
     Use simulateChunk() after calling this function to do the computation.
     @param dt The delta time for this frame.
     */
    boolean beginSimulation(float dt);

    /** Do the computationally heavy part of the simulation.
     Call this function getSimulationChunkCount() times to do the entire simulation.
     This function can be called from multiple threads in parallel.
     All Chunks need to be simulated before ending the frame.
     */
    void simulateChunk(int idx);

    /** \brief Finishes up the simulation.
     This function can be expensive if inter-collision is enabled.
     */
    void endSimulation();

    /** \brief Returns the number of chunks that need to be simulated this frame.
     */
    int getSimulationChunkCount() ;

    // inter-collision parameters
    void setInterCollisionDistance(float distance);
    float getInterCollisionDistance() ;
    void setInterCollisionStiffness(float stiffness);
    float getInterCollisionStiffness() ;
    void setInterCollisionNbIterations(int nbIterations);
    int getInterCollisionNbIterations() ;
    void setInterCollisionFilter(InterCollisionFilter filter);

    /// Returns true if an unrecoverable error has occurred.
    boolean hasError() ;
}
