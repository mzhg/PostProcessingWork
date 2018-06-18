package jet.opengl.demos.intel.fluid.particles;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by Administrator on 2018/4/25 0025.
 */

public class Particle implements Cloneable{
    /** Whether to enable combustion aspects of fluid simulation. */
    public static final int ENABLE_FIRE  = 1;


/** Whether to record a recent history of particle positions in a ring buffer, for rendering pathlines.

 This consumes a lot of memory and CPU time.
 It is meant for diagnosis, not for release.
 */
    public static final int ENABLE_PARTICLE_POSITION_HISTORY = 0;


/** Whether to diagnose particle jerking.
 */
public static final int ENABLE_PARTICLE_JERK_RECORD = 0;


/** Whether to track a brief history of every particle member, for diagnosing determinism.
 */
public static final int ENABLE_PARTICLE_HISTORY = 0;


/// Hacks to reassign density in or at walls.
public static final int POISON_DENSITY_BASED_ON_VORTONS_HITTING_WALLS  = 0,
     POISON_DENSITY_BASED_ON_GRIDPOINTS_INSIDE_WALLS = 0;

/// Hacks to remove component of density gradient parallel to walls.
public static final int POISON_DENSITY_GRADIENT_BASED_ON_VORTONS_HITTING_WALLS = 0,
             POISON_DENSITY_GRADIENT_BASED_ON_GRIDPOINTS_INSIDE_WALLS = 0,
             POISON_DENSITY_GRADIENT_BASED_ON_RIGID_SPHERE_GEOMETRY = 0;

/// Reduce vorton convergence using an ad-hoc SPH-like approach with VPM.
/// Do not enable this when USE_SMOOTHED_PARTICLE_HYDRODYNAMICS is 1.
/// It's mainly useful for using VPM with containers.
public static final int  REDUCE_CONVERGENCE = 0;

/** Whether to compute pressure gradient explicitly.

 Alternative is to assume only vertical pressure gradient from hydrostatic balance.
 */
public static final int  COMPUTE_PRESSURE_GRADIENT = 0;

    public static final float  PiOver6 = Numeric.PI / 6.0f ;

    public static final float Particle_sAmbientTemperature = 300.0f ;

    public final Vector3f mPosition	= new Vector3f()       ;   ///< Position (in world units) of center of particle
    public final Vector3f mVelocity = new Vector3f()	   ;   ///< Velocity (speed and direction) of particle
    public final Vector3f mOrientation = new Vector3f()	   ;   ///< Orientation of particle, in axis-angle form where angle=|orientation|
    public final Vector3f mAngularVelocity = new Vector3f();   ///< Angular velocity of particle.  Same as half the vorticity.
    public float	    mDensity = 1.0f           ;   ///< Either density or mass per particle, depending.  For fire simulations, this is the exhaust (smoke) density.
    public float	    mSize		        ;   ///< Diameter of the region of influence of a particle.
    public int         mBirthTime          ;   ///< Birth time of particle, in "ticks"
    public boolean        mHitBoundary        ;   ///< Whether this particle hit a boundary this frame.
    public final Vector3f mHitNormal = new Vector3f()          ;   ///< Contact normal direction, when hit.
    // Each particle has 3 fractions: fuel, flame and exhaust (smoke).
    // Their sum must equal 1, and each must be non-negative.
    // Only 2 need be explicit; Smoke is computed as smoke=1-fuel-flame.
    // We model all 3 explicitly to simplify rendering code.
    public float	    mFuelFraction       ;   ///< Fraction of mass which is fuel.
    public float	    mFlameFraction      ;   ///< Fraction of mass which is flame.
    public float	    mSmokeFraction = 1.0f     ;   ///< Fraction of mass which is smoke.

    public final Vector3f mPositionSiblings = new Vector3f()   ;   ///< Position (in world units) of center of vortex particle
    public final Vector3f mVorticitySiblings = new Vector3f()  ;   ///< Vorticity of vortex particle
    public int    mNumVortonsIncorporated     ;   ///< Number of actual vortons encountered during influence calculations
    public final Vector3f mTotalCirculation = new Vector3f()          ;   ///< Total circulation encountered during influence calculation
    public int    mNumSibVortonsIncorporated  ;   ///< Number of sibling vortons encountered during influence calculations
    public final int[]    mIndicesOfParent = new int[3]         ;   ///< Indices of parent cell

    static final int NUM_HISTORICAL_POSITIONS = 32 ;
    public final Vector3f[]    mPositionHistory = new Vector3f[ NUM_HISTORICAL_POSITIONS ]    ;   ///< Ring buffer of recent history of positions of this particle.
    public int  mPositionHistoryIndex                           ;   ///< Index into mPositionHistory of next position to record.

    public final Vector3f    mVelocityPrev = new Vector3f()      ;   ///< Particle velocity from previous update.
    public final Vector3f    mAcceleration = new Vector3f()      ;   ///< Particle acceleration for current state.
    public final Vector3f    mAccelerationPrev = new Vector3f()   ;   ///< Particle acceleration from previous update.
    public final Vector3f    mJerk = new Vector3f()              ;   ///< Particle jerk for current state.

    public final Vector3f    mDisplacement = new Vector3f()      ;   ///< Displacement applied from projecting particles.

    public Particle(ReadableVector3f position , ReadableVector3f angularVelocity , float size){
        mPosition.set(position);
        mAngularVelocity.set(angularVelocity);
        mSize = size;
    }

    /** Get radius from size.
     \return the radius of this vorton
     */
    public float getRadius()
    {
        return mSize * 0.5f ;
    }


    /** Set size from radius.
     */
    public void setRadius( float radius )
    {
        mSize = 2.0f * radius ;
    }


    /** Return volume of this particle.

     \return Volume of this particle.

     \note Derivation:
     Volume of a sphere is 4*pi*r^3/3.
     r=size/2 so r^3=size/8.
     V=4*pi*size^3/(3*8)=pi*size^3/6
     */
    public float getVolume()
    {
        return PiOver6 * mSize * mSize * mSize ;
    }


    /** Return density of this particle.
     */
    public float getDensity()
    {
        assert ( mDensity > 0.0f ) ;
        return mDensity ;
    }


    /** Assign density of this particle.

     \see GetDensity.
     */
    public void setDensity( float density )
    {
        mDensity = density ;
        assert( mDensity > 0.0f ) ;
        assert( ! Float.isNaN( mDensity ) && ! Float.isInfinite( mDensity ) ) ;
        assert( getMass() >= 0.0f ) ;
    }


    /** Return mass of this particle.
     */
    public float getMass()
    {
        final float fMass   = getDensity() * getVolume() ;
        assert ( fMass >= 0.0f ) ;
        return fMass ;
    }


    /** Assign mass for this particle.

     \param mass             Mass to assign to this particle.
     */
    public void setMass( float mass )
    {
        assert ( mass >= 0.0f ) ;
        final float density = mass / getVolume() ;
        setDensity( density ) ;
        assert ( getMass() >= 0.0f ) ;
    }


    /** Return temperature of this particle.

     \return Temperature of this particle, in absolute degrees.

     \note   This assumes a Boussinesq approxomation where temperature is
     related to density.

     \note   The formula for temperature comes from the formula for density under
     the Bousinesq approximation:

     density = ambientDensity * ( 1 - thermalExpansionCoefficient * ( temperature - ambientTemperature ) )
     --> density / ambientDensity = 1 - thermalExpansionCoefficient * (temperature-ambientTemperature)
     --> density / ( ambientDensity * (1-thermalExpansionCoefficient) ) = temperature - ambientTemperature
     --> temperature = density / (ambientDensity * (1-thermalExpansionCoefficient) ) + ambientTemperature

     For an ideal gas, thermalExpansionCoefficient=1/temperature so the above formula becomes
     density = ambientDensity * ambientTemperature / temperature
     --> temperature = ambientTemperature * ambientDensity / density

     */
    public float getTemperature( float ambientDensity )
    {
//        #if 0
//            const float thermalExpansionCoefficient = 1.0f ;
//            const float temperature = GetDensity() / ( ambientDensity * ( 1 - thermalExpansionCoefficient ) ) + Particle_sAmbientTemperature ;
//        #else
        final float temperature = Particle_sAmbientTemperature * ambientDensity / getDensity() ;
//        #endif
        assert ( temperature >= 0.0f ) ;
        return temperature ;
    }


    /** Set particle temperature.

     \param temperature  Temperature of particle in absolute degrees.

     Using the Bousinesq approximation and assuming the fluid is an ideal gas,
     temperature = ambientTemperature * ambientDensity / density

     */
    public void setTemperature( float ambientDensity , float temperature )
    {
        assert ( 1.0f == ambientDensity ) ;

        float density = ambientDensity * Particle_sAmbientTemperature / temperature ;
        assert ( ! Float.isNaN( density ) && ! Float.isInfinite( density ) ) ;
        setDensity( density ) ;
    }


    /** Mark this particle for deletion.
     \see Particles::KillParticlesMarkedForDeath, IsAlive.
     */
    public void markDead()
    {
        mPosition.x         = Float.NaN ;
        mPosition.y         = Float.NaN ;
        mPosition.z         = Float.NaN ;
        mVelocity.x         = Float.NaN ;
        mVelocity.y         = Float.NaN ;
        mVelocity.z         = Float.NaN ;
        mOrientation.x      = Float.NaN ;
        mOrientation.y      = Float.NaN ;
        mOrientation.z      = Float.NaN ;
        mAngularVelocity.x  = Float.NaN ;
        mAngularVelocity.y  = Float.NaN ;
        mAngularVelocity.z  = Float.NaN ;
        mDensity            = Float.NaN ;
        mSize               = Float.NaN ;
        mBirthTime          = -1 ;

//        #if POISON_DENSITY_GRADIENT_BASED_ON_VORTONS_HITTING_WALLS
            mHitBoundary        = false ;
        mHitNormal          .set( Float.NaN , Float.NaN , Float.NaN ) ;
//        #endif

//        #if ENABLE_FIRE
            mFuelFraction       = Float.NaN ;
        mFlameFraction      = Float.NaN ;
        mSmokeFraction      = Float.NaN ;
//        #endif
        assert ( ! isAlive() ) ;
    }


    /** Return whether this particle is alive (has not been marked dead).
     \see MarkDead
     */
    public boolean isAlive()
    {
        return mBirthTime >= 0 ;
    }


    public static int nextHistoryIndex( int positionHistoryIndex )
    {
        final int lastHistoricalIndex = NUM_HISTORICAL_POSITIONS - 1 ;
        return positionHistoryIndex >= lastHistoricalIndex ? 0 : positionHistoryIndex + 1 ;
    }

    public int historyNext()
    {
        final int lastHistoricalIndex = NUM_HISTORICAL_POSITIONS - 1 ;
        return nextHistoryIndex( mPositionHistoryIndex ) ;
    }

    /// Return index of first (oldest) element in position history.
    public int historyBegin()
    {
        return mPositionHistoryIndex ;
    }

    /// Return index of last (newest) element in position history.
    public int historyEnd()
    {
        final int lastHistoricalIndex = NUM_HISTORICAL_POSITIONS - 1 ;
        return 0 == mPositionHistoryIndex ? lastHistoricalIndex : mPositionHistoryIndex - 1 ;
    }

    public void recordPositionHistory()
    {
        assert ( mPositionHistoryIndex < NUM_HISTORICAL_POSITIONS ) ;
        // Record current position in history.
        mPositionHistory[ mPositionHistoryIndex ] = mPosition ;
        // Increment ring buffer index.
        mPositionHistoryIndex = historyNext() ;
    }

    @Override
    public Particle clone() {

        try {
            return (Particle) super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new Error(e);
        }
    }

    /** Update diagnostic quantities used to measure jerk.

     This must be called prior to updating particle velocity,
     but before computing jerk.

     \see ComputeJerk.
     */
    public void updateJerkDiagnostics( float timeStep )
    {
//        mJerk               = ( mAcceleration - mAccelerationPrev ) / timeStep ;
        Vector3f.sub(mAcceleration, mAccelerationPrev, mJerk); mJerk.scale(1.0f/timeStep);
        mAccelerationPrev   .set(mAcceleration) ;
//        mAcceleration       = ( mVelocity - mVelocityPrev ) / timeStep ;
        Vector3f.sub(mVelocity, mVelocityPrev, mAcceleration); mAcceleration.scale(1.0f/timeStep);
        mVelocityPrev       .set(mVelocity) ;
    }

    /** Kill the particle at the given index, replacing it with the last particle in the given dynamic array.
     */
    public static void kill(List<Particle> particles , int iParticle )
    {
        assert ( iParticle < particles.size() ) ;
//        particles[ iParticle ] = particles[ particles.Size() - 1 ] ;
//        particles.PopBack() ;

        particles.set(iParticle, particles.get(particles.size() - 1));
        particles.remove(particles.size() - 1);
    }

    /** Mark the particle at the given index as one to be killed.
     */
    public static void markForKill( List<Particle> particles , int iParticle )
    {
        assert ( iParticle < particles.size() ) ;
        particles.get(iParticle).markDead() ;
    }

    /** Merge the given two particles.

     @param particles    Dynamic array of particles.

     @param iPcl1        Index of first particle, which will subsume the second particle.

     @param iPcl2        Index of second particle, which will be marked for deletion.
     */
    public static void merge( List<Particle> particles , int iPcl1 , int iPcl2 )
    {
        Particle        pcl1           = particles.get(iPcl1) ;
        final Particle  pcl2           = particles.get(iPcl2) ;
        final float      mass1          = pcl1.getMass() ;
        final float      mass2          = pcl2.getMass() ;
        final float      radius1        = pcl1.mSize * 0.5f ;
        final float      radius2        = pcl2.mSize * 0.5f ;
        assert ( radius1 == radius2 ) ; // TODO: FIXME: code elsewhere assumes all vortons have the same radius.
        final float      radius         = radius1 ; // TODO: Set radius such that total volume is the same.
        final float      momIn1         = 0.4f * mass1 * radius1 * radius1 ;
        final float      momIn2         = 0.4f * mass2 * radius2 * radius2 ;
        final float      mass           = mass1 + mass2 ;
        final float      momIn          = 0.4f * mass * radius * radius ;
        final float      oneOverMass    = 1.0f / mass ;

        assert( mass > 0.0f ) ;
        assert( ! Float.isNaN( oneOverMass           ) && ! Float.isNaN( oneOverMass           ) ) ;
        assert( ! pcl1.mPosition.isNaN() && ! pcl1.mPosition.isNaN( ) ) ;
        assert( ! pcl1.mAngularVelocity.isNaN( ) && ! pcl1.mAngularVelocity.isNaN(  ) ) ;

//        pcl1.mPosition        = ( pcl1.mPosition * mass1 + pcl2.mPosition * mass2 ) * oneOverMass ;
        Vector3f.linear(pcl1.mPosition, mass1,pcl2.mPosition, mass2, pcl1.mPosition); pcl1.mPosition.scale(oneOverMass);
//        pcl1.mVelocity        = ( pcl1.mVelocity * mass1 + pcl2.mVelocity * mass2 ) * oneOverMass ;
        Vector3f.linear(pcl1.mVelocity, mass1, pcl2.mVelocity, mass2, pcl1.mVelocity);  pcl1.mVelocity.scale(oneOverMass);
//        pcl1.mOrientation     = ( pcl1.mOrientation + pcl2.mOrientation ) * 0.5f ;
        Vector3f.add(pcl1.mOrientation, pcl2.mOrientation, pcl1.mOrientation); pcl1.mOrientation.scale(0.5f);
        /*const Vec3 angMom     = ( pcl1.mAngularVelocity * momIn1 + pcl2.mAngularVelocity * momIn2 ) ;
        pcl1.mAngularVelocity = angMom / momIn ;*/
        Vector3f.linear(pcl1.mAngularVelocity, momIn1, pcl2.mAngularVelocity, momIn2, pcl1.mAngularVelocity); pcl1.mAngularVelocity.scale(1.0f/momIn);

        pcl1.setRadius( radius ) ;
        //pcl1.SetMass( mass1 /* Should be "mass" to conserve mass, but without also changing volume, "mass" would change density */ , ambientFluidDensity ) ;
        pcl1.mBirthTime = Math.min( pcl1.mBirthTime , pcl2.mBirthTime ) ;

//#if POISON_DENSITY_GRADIENT_BASED_ON_VORTONS_HITTING_WALLS
        pcl1.mHitBoundary = pcl1.mHitBoundary || pcl2.mHitBoundary ;
        pcl1.mHitNormal.set(pcl1.mHitBoundary ? pcl1.mHitNormal : pcl2.mHitNormal);
//#endif

        assert ( ! pcl1.mPosition.isNaN(         ) && ! pcl1.mPosition.isNaN(         ) ) ;
        assert ( ! pcl1.mAngularVelocity.isNaN(  ) && ! pcl1.mAngularVelocity.isNaN(  ) ) ;

        markForKill( particles , iPcl2 ) ;
    }

    /** Kill particles marked for death.

     @param particles    (in/out) Reference to particles to kill if they are marked for death.

     Some operations, such as "Merge", can effectively kill particles,
     but those operations can cause the particle array to become reordered.
     That is because Particles::Kill fills the slot previously occupied by
     the killed particle, with a particle at the end of the array, so
     that the array has no "holes".

     In order to make parallelizing this easier, some routines that want to
     kill particles instead simply "mark them for death" so that this routine
     can sweep through and reorder the particles array.

     \see MarkDead, IsAlive.
     */
    public static void killParticlesMarkedForDeath( List<Particle> particles ){
        int numParticles = particles.size() ;
        for( int iPcl = 0 ; iPcl < numParticles ; /* Loop body increments iPcl. */ )
        {
            final Particle pcl = particles.get(iPcl);
            if( ! pcl.isAlive() )
            {   // Particle was marked for death.
                kill( particles , iPcl ) ;
                -- numParticles ;
            }
            else
            {   // Particle remains alive.
                ++ iPcl ;
            }
        }
    }

    /** Compute the geometric center of all given particles.<p></p>

     The "geometric center" is simply the average position of all particles,
     in contrast to the center of mass in which each particle position is weighted
     by its mass.
     */
    public static Vector3f computeGeometricCenter(List<Particle> particles ){
        Vector3f vCenter = new Vector3f( 0.0f , 0.0f , 0.0f ) ;
        final int numTracers = particles.size() ;
        for( int iTracer = 0 ; iTracer < numTracers ; ++ iTracer )
        {
            final Particle pcl = particles.get(iTracer) ;
//            vCenter += pcl.mPosition ;
            Vector3f.add(vCenter, pcl.mPosition, vCenter);
        }
        vCenter.scale(1.0f/numTracers);
        return vCenter ;
    }

    public static class AngularVelocityStats{
        /** Minimum angular velocity of all particles.*/
        public float min;
        /** Maximum angular velocity of all particles. */
        public float max;
        /** Average angular velocity of all particles. */
        public float mean;
        /** Standard deviation of angular velocity of all particles. */
        public float stddev;
        /** Center of angular velocity -- angVel-weighted sum of particle positions */
        public final Vector3f centerOfAngularVelocity = new Vector3f();
    }

    /** Compute vorticity statistics for all vortex particles.

     @param out      The out that stores the results.
     */
    public static void computeAngularVelocityStats( List<Particle> particles , AngularVelocityStats out ){
        out.min = Float.MAX_VALUE ;
        out.max = - out.min ;
        out.centerOfAngularVelocity.set( 0.0f , 0.0f , 0.0f ) ;
        float           sum     = 0.0f ;
        float           sum2    = 0.0f ;
        final  int  numPcls = particles.size() ;
        for( int iPcl = 0 ; iPcl < numPcls ; ++ iPcl )
        {   // For each particle...
            final Particle    pcl         = particles.get(iPcl) ;
            final float         angVelMag   = pcl.mAngularVelocity.length() ;
            sum  += angVelMag ;
            sum2 += angVelMag * angVelMag ;
            out.min = Math.min( out.min , angVelMag ) ;
            out.max = Math.max( out.max , angVelMag ) ;
//            centerOfAngularVelocity += angVelMag * pcl.mPosition ;
            Vector3f.linear(out.centerOfAngularVelocity, pcl.mPosition, angVelMag, out.centerOfAngularVelocity);
        }

        out.mean = sum / numPcls ;
        final float mean2 = sum2 / numPcls ;
        out.stddev = (float) Math.sqrt( mean2 - out.mean * out.mean );
        out.centerOfAngularVelocity.scale(1.0f/sum) ;
    }

    /** Compute the total angular momentum of all given particles.<p></p>

     note   Used to diagnose the simulation.  Angular momentum should be
     conserved, that is, constant over time.
     */
    public static Vector3f computeAngularMomentum( List<Particle> particles ){
        Vector3f vAngMom = new Vector3f( 0.0f , 0.0f , 0.0f ) ;
        final int numParticles = particles.size() ;
        for( int iPcl = 0 ; iPcl < numParticles ; ++ iPcl )
        {
            Particle pcl = particles.get(iPcl) ;
            final float mass    = pcl.getMass() ;
            final float radius  = pcl.getRadius() ;
            final float momentOfInertia = 0.4f * mass * radius * radius ;
//            vAngMom += pcl.mAngularVelocity * momentOfInertia ;
            Vector3f.linear(vAngMom, pcl.mAngularVelocity, momentOfInertia, vAngMom);
        }
        return vAngMom ;
    }

    /** Compute velocity statistics for particles.

     @param out      The out that stores the results.
     */

    public static void computeVelocityStats( List<Particle> particles , AngularVelocityStats out ){
        out.min = Float.MAX_VALUE ;
        out.max = - out.min ;
        out.centerOfAngularVelocity.set( 0.0f , 0.0f , 0.0f ) ;
        float           sum     = 0.0f ;
        float           sum2    = 0.0f ;
        final int       numPcls = particles.size() ;
        for( int iPcl = 0 ; iPcl < numPcls ; ++ iPcl )
        {   // For each particle...
            final Particle     pcl     = particles.get(iPcl) ;
            final float         velMag  = pcl.mVelocity.length() ;
            sum  += velMag ;
            sum2 += velMag * velMag ;
            out.min = Math.min( out.min , velMag ) ;
            out.max = Math.max( out.max , velMag ) ;
//            centerOfVelocity += velMag * pcl.mPosition ;
            Vector3f.linear(out.centerOfAngularVelocity, pcl.mPosition, velMag, out.centerOfAngularVelocity);
        }
        out.mean = sum / numPcls ;
        final float mean2 = sum2 / numPcls ;
        out.stddev = (float)Math.sqrt( mean2 - out.mean * out.mean ) ;
        out.centerOfAngularVelocity.scale(1.0f/sum) ;
    }

    /** Compute temperature statistics for all particles.
     */
    public static void computeTemperatureStats( List<Particle> particles , AngularVelocityStats out ){
        out.min = Float.MAX_VALUE ;
        out.max = - out.min ;
        float           sum     = 0.0f ;
        float           sum2    = 0.0f ;
        final int    numPcls = particles.size() ;
        for( int iPcl = 0 ; iPcl < numPcls ; ++ iPcl )
        {   // For each particle...
            final Particle     pcl         = particles.get(iPcl) ;
            final float        temperature = pcl.getTemperature( 1.0f ) ;
            sum += temperature ;
            sum2 += temperature * temperature ;
            out.min = Math.min( out.min , temperature ) ;
            out.max = Math.max( out.max , temperature ) ;
        }
        out.mean = sum / numPcls;
        final float mean2 = sum2 / numPcls;
        out.stddev = (float)Math.sqrt( mean2 - out.mean * out.mean ) ;
    }

    public static void computeDensityStats(List<Particle> particles , AngularVelocityStats out ){
        out.min = Float.MAX_VALUE ;
        out.max = - out.min ;
        float           sum     = 0.0f ;
        float           sum2    = 0.0f ;
        final int       numPcls = particles.size() ;
        for( int iPcl = 0 ; iPcl < numPcls ; ++ iPcl )
        {   // For each particle...
            final Particle    pcl     = particles.get(iPcl) ;
            final float       density = pcl.getDensity() ;
            sum += density ;
            sum2 += density * density ;
            out.min = Math.min( out.min , density ) ;
            out.max = Math.max( out.max , density ) ;
        }
        out.mean = sum / ( numPcls ) ;
        final float mean2 = sum2 / ( numPcls ) ;
        out.stddev = (float)Math.sqrt( mean2 - out.mean * out.mean ) ;
    }

    public static void partitionParticles(List<Particle> particles){
        throw new UnsupportedOperationException();
    }
}
