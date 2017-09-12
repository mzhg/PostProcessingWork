package nv.visualFX.cloth.libs;

/**
 * abstract cloth constraints and triangle indices<p></p>
 * Created by mazhen'gui on 2017/9/9.
 */

public interface Fabric {

    /** public abstract int Returns the Factory used to create this Fabric.*/
    public abstract Factory getFactory();

    /** public abstract int Returns the number of constraint solve phases stored.
     Phases are groups of constraints that make up the general structure of the fabric.
     Cloth instances can have different configuration settings per phase (see Cloth::setPhaseConfig()).
     Phases are usually split by type (horizontal, vertical, bending, shearing), depending on the cooker used.
     */
    public abstract int getNumPhases();

    /** public abstract int Returns the number of rest lengths stored.
     Each constraint uses the rest value to determine if the two connected particles need to be pulled together or pushed apart.
     */
    public abstract int getNumRestvalues();

    /** public abstract int Returns the number of constraint stiffness values stored.
     It is optional for a Fabric to have per constraint stiffness values provided.
     This function will return 0 if no values are stored.
     Stiffness per constraint values stored here can be used if more fine grain control is required (as opposed to the values stored in the cloth's phase configuration).
     The Cloth 's phase configuration stiffness values will be ignored if stiffness per constraint values are used.
     */
    public abstract int getNumStiffnessValues();


    /** public abstract int Returns the number of sets stored.
     Sets connect a phase to a range of indices.
     */
    public abstract int getNumSets();

    /** public abstract int Returns the number of indices stored.
     Each constraint has a pair of indices that indicate which particles it connects.
     */
    public abstract int getNumIndices();
    /// Returns the number of particles.
    public abstract int getNumParticles();
    /// Returns the number of Tethers stored.
    public abstract int getNumTethers();
    /// Returns the number of triangles that make up the cloth mesh.
    public abstract int getNumTriangles();

    /** Scales all constraint rest lengths.*/
    public abstract void scaleRestvalues(float f);
    /** Scales all tether lengths.*/
    public abstract void scaleTetherLengths(float f);

    /*
    public void incRefCount()
    {
        physx::shdfnd::atomicIncrement(&mRefCount);
        assert(mRefCount > 0);
    }

    /// Returns true if the object is destroyed
    public boolean decRefCount()
    {
        assert (mRefCount > 0);
        int result = physx::shdfnd::atomicDecrement(&mRefCount);
        if (result == 0)
        {
            delete this;
            return true;
        }
        return false;
    }*/
}
