package nv.visualFX.cloth.libs;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.ReadableVector3f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by mazhen'gui on 2017/9/9.
 */

public interface Cloth {
    /**	\brief Creates a duplicate of this Cloth instance.
     Same as:
     \code
     getFactory().clone(*this);
     \endcode
     */
    Cloth clone(Factory factory);

    /** \brief Returns the fabric used to create this Cloth.*/
    Fabric getFabric();

    /* particle properties */
    /// Returns the number of particles simulated by this fabric.
    int getNumParticles();
    /** \brief Used internally to synchronize CPU and GPU particle memory.*/
    void lockParticles(); //Might be better if it was called map/unmapParticles
    /** \brief Used internally to synchronize CPU and GPU particle memory.*/
    void unlockParticles();

    /** \brief Returns the simulation particles of the current frame.
     Each PxVec4 element contains the particle position in the XYZ components and the inverse mass in the W component.
     The returned memory may be overwritten (to change attachment point locations for animation for example).
     Setting the inverse mass to 0 locks the particle in place.
     */
    /*MappedFloatBuffer*/FloatBuffer getCurrentParticles();

    /** \brief Returns the simulation particles of the previous frame.
     Similar to getCurrentParticles().
     */
    /*MappedFloatBuffer*/FloatBuffer getPreviousParticles();

    /** \brief Returns platform dependent pointers to the current GPU particle memory.*/
    GpuParticles getGpuParticles();


    /** Set the translation of the local space simulation after next call to simulate().
     This applies a force to make the cloth behave as if it was moved through space.
     This does not move the particles as they are in local space.
     Use the graphics transformation matrices to render the cloth in the proper location.
     The applied force is proportional to the value set with Cloth::setLinearInertia().
     */
    void setTranslation(ReadableVector3f trans);

    /** \brief Set the rotation of the local space simulation after next call to simulate(). 
     Similar to Cloth::setTranslation().
     The applied force is proportional to the value set with Cloth::setAngularInertia() and Cloth::setCentrifugalInertia().
     */
    void setRotation(Quaternion rot);

    /** \brief Returns the current translation value that was set using setTranslation().*/
    ReadableVector3f getTranslation();
    /** \brief Returns the current rotation value that was set using setRotation().*/
    Quaternion getRotation();

    /** \brief Set inertia derived from setTranslation() and setRotation() to zero (once).*/
    void clearInertia();

    /** \brief Adjust the position of the cloth without affecting the dynamics (to call after a world origin shift, for example). */
    void teleport(ReadableVector3f delta);

	/* solver parameters */

    /** \brief Returns the delta time used for previous iteration.*/
    float getPreviousIterationDt();

    /** \brief Sets gravity in global coordinates.*/
    void setGravity(ReadableVector3f gravity);
    /// Returns gravity set with setGravity().
    ReadableVector3f getGravity();

    /** \brief Sets damping of local particle velocity (1/stiffnessFrequency).
     0 (default): velocity is unaffected, 1: velocity is zeroed
     */
    void setDamping(ReadableVector3f damping);
    /// Returns value set with setDamping().
    ReadableVector3f getDamping();

    // portion of local frame velocity applied to particles
    // 0 (default): particles are unaffected
    // same as damping: damp global particle velocity
    void setLinearDrag(ReadableVector3f drag);
    ReadableVector3f getLinearDrag();
    void setAngularDrag(ReadableVector3f drag);
    ReadableVector3f getAngularDrag();

    /** \brief Set the portion of local frame linear acceleration applied to particles.
     0: particles are unaffected, 1 (default): physically correct.
     */
    void setLinearInertia(ReadableVector3f inertia);
    /// Returns value set with getLinearInertia().
    ReadableVector3f getLinearInertia();
    /** \brief Similar to setLinearInertia(), but for angular inertia.*/
    void setAngularInertia(ReadableVector3f inertia);
    /// Returns value set with setAngularInertia().
    ReadableVector3f getAngularInertia();
    /** \brief Similar to setLinearInertia(), but for centrifugal inertia.*/
    void setCentrifugalInertia(ReadableVector3f inertia);
    ///Returns value set with setCentrifugalInertia().
    ReadableVector3f getCentrifugalInertia();

    /** \brief Set target solver iterations per second.
     At least 1 iteration per frame will be solved regardless of the value set.
     */
    void setSolverFrequency(float frequency);
    /// Returns gravity set with getSolverFrequency().*/
    float getSolverFrequency();

    // damp, drag, stiffness exponent per second
    void setStiffnessFrequency(float frequency);
    float getStiffnessFrequency();

    // filter width for averaging dt^2 factor of gravity and
    // external acceleration, in numbers of iterations (default=30).
    void setAcceleationFilterWidth(int filterWidth);
    int getAccelerationFilterWidth();

    // setup edge constraint solver iteration
    void setPhaseConfig(ByteBuffer configs);

	/* collision parameters */

    /** \brief Set spheres for collision detection.
     Elements of spheres contain PxVec4(x,y,z,r) where [x,y,z] is the center and r the radius of the sphere.
     The values currently in range[first, last[ will be replaced with the content of spheres.
     \code
     cloth->setSpheres(Range<const PxVec4>(), 0, cloth->getNumSpheres()); //Removes all spheres
     \endcode
     */
    void setSpheres(FloatBuffer spheres, int first, int last);
    /// Returns the number of spheres currently set.
    int getNumSpheres();


    /** \brief Set indices for capsule collision detection.
     The indices define the spheres that form the end points between the capsule.
     Every two elements in capsules define one capsule.
     The values currently in range[first, last[ will be replaced with the content of capsules.
     Note that first and last are indices to whole capsules consisting of 2 indices each. So if
     you want to update the first two capsules (without changing the total number of capsules)
     you would use the following code:
     \code
     uint32_t capsules[4] = { 0,1,  1,2 }; //Define indices for 2 capsules
     //updates the indices of the first 2 capsules in cloth
     cloth->setCapsules(Range<const uint32_t>(capsules, capsules + 4), 0, 2);
     \endcode
     */
    void setCapsules(IntBuffer capsules, int first, int last);
    /// Returns the number of capsules (which is half the number of capsule indices).
    int getNumCapsules();

    /** \brief Sets plane values to be used with convex collision detection.
     The planes are specified in the form ax + by + cz + d, where elements in planes contain PxVec4(x,y,z,d).
     [x,y,z] is required to be normalized.
     The values currently in range [first, last[ will be replaced with the content of planes.
     Use setConvexes to enable planes for collision detection.
     */
    void setPlanes(FloatBuffer planes, int first, int last);
    /// Returns the number of planes currently set.
    int getNumPlanes();

    /** \brief Enable planes for collision.
     convexMasks must contain masks of the form (1<<planeIndex1)|(1<<planeIndex2)|...|(1<<planeIndexN).
     All planes masked in a single element of convexMasks form a single convex polyhedron.
     The values currently in range [first, last[ will be replaced with the content of convexMasks.
     */
    void setConvexes(IntBuffer convexMasks, int first, int last);
    /// Returns the number of convexMasks currently set.
    int getNumConvexes();

    /** \brief Set triangles for collision.
     Each triangle in the list is defined by of 3 vertices.
     The values currently in range [first, last[ will be replaced with the content of convexMasks.
     */
    void setTriangles(FloatBuffer triangles, int first, int last);
    void setTriangles(FloatBuffer triangles, FloatBuffer value, int first);
    /// Returns the number of triangles currently set.
    int getNumTriangles();

    /// Returns true if we use ccd
    boolean isContinuousCollisionEnabled();
    /// Set if we use ccd or not (disabled by default)
    void enableContinuousCollision(boolean flag);

    // controls how quickly mass is increased during collisions
    float getCollisionMassScale();
    void setCollisionMassScale(float scale);

    /** \brief Set the cloth collision shape friction coefficient.*/
    void setFriction(float friction);
    ///Returns value set with setFriction().
    float getFriction();

    // set particles for collision handling.
    // each indices element consists of 3 particle
    // indices and an index into the lerp weights array.
    void setVirtualParticles(IntBuffer indices, FloatBuffer weights);
    int getNumVirtualParticles();
    int getNumVirtualParticleWeights();

	/* tether constraint parameters */

    /** \brief Set Tether constraint scale.
     1.0 is the original scale of the Fabric.
     0.0 disables tether constraints in the Solver.
     */
    void setTetherConstraintScale(float scale);
    ///Returns value set with setTetherConstraintScale().
    float getTetherConstraintScale();
    /** \brief Set Tether constraint stiffness..
     1.0 is the default.
     <1.0 makes the constraints behave springy.
     */
    void setTetherConstraintStiffness(float stiffness);
    ///Returns value set with setTetherConstraintStiffness().
    float getTetherConstraintStiffness();

	/* motion constraint parameters */

    /** \brief Returns reference to motion constraints (position, radius)
     The entire range must be written after calling this function.
     */
    FloatBuffer getMotionConstraints();
    /** \brief Removes all motion constraints.
     */
    void clearMotionConstraints();
    int getNumMotionConstraints();
    void setMotionConstraintScaleBias(float scale, float bias);
    float getMotionConstraintScale();
    float getMotionConstraintBias();
    void setMotionConstraintStiffness(float stiffness);
    float getMotionConstraintStiffness();

	/* separation constraint parameters */

    // return reference to separation constraints (position, radius)
    // The entire range must be written after calling this function.
    FloatBuffer getSeparationConstraints();
    void clearSeparationConstraints();
    int getNumSeparationConstraints();

	/* clear interpolation */

    // assign current to previous positions for
    // collision spheres, motion, and separation constraints
    void clearInterpolation();

	/* particle acceleration parameters */

    // return reference to particle accelerations (in local coordinates)
    // The entire range must be written after calling this function.
    FloatBuffer getParticleAccelerations();
    void clearParticleAccelerations();
    int getNumParticleAccelerations();

	/* wind */

    /** /brief Set wind in global coordinates. Acts on the fabric's triangles. */
    void setWindVelocity(ReadableVector3f velocity);
    ///Returns value set with setWindVelocity().
    ReadableVector3f getWindVelocity();
    /** /brief Sets the air drag coefficient. */
    void setDragCoefficient(float coefficient);
    ///Returns value set with setDragCoefficient().
    float getDragCoefficient();
    /** /brief Sets the air lift coefficient. */
    void setLiftCoefficient(float coefficient);
    ///Returns value set with setLiftCoefficient().
    float getLiftCoefficient();
    /** /brief Sets the fluid density used for air drag/lift calculations. */
    void setFluidDensity(float density);
    ///Returns value set with setFluidDensity().
    float getFluidDensity();

	/* self collision */

    /** /brief Set the distance particles need to be separated from each other withing the cloth. */
    void setSelfCollisionDistance(float distance);
    ///Returns value set with setSelfCollisionDistance().
    float getSelfCollisionDistance();
    /** /brief Set the constraint stiffness for the self collision constraints. */
    void setSelfCollisionStiffness(float stiffness);
    ///Returns value set with setSelfCollisionStiffness().
    float getSelfCollisionStiffness();

    /** \brief Set self collision indices.
     Each index in the range indicates that the particle at that index should be used for self collision.
     If set to an empty range (default) all particles will be used.
     */
    void setSelfCollisionIndices(IntBuffer indices);
    ///Returns the number of self collision indices set.
    int getNumSelfCollisionIndices();

	/* rest positions */

    // set rest particle positions used during self-collision
    void setRestPositions(FloatBuffer positions);
    int getNumRestPositions();

	/* bounding box */

    /** \brief Returns current particle position bounds center in local space */
    ReadableVector3f getBoundingBoxCenter();
    /** \brief Returns current particle position bounds size in local space */
    ReadableVector3f getBoundingBoxScale();

	/* sleeping (disabled by default) */

    // max particle velocity (per axis) to pass sleep test
    void setSleepThreshold(float threshold);
    float getSleepThreshold();
    // test sleep condition every nth millisecond
    void setSleepTestInterval(int interval);
    int getSleepTestInterval();
    // put cloth to sleep when n consecutive sleep tests pass
    void setSleepAfterCount(int count);
    int getSleepAfterCount();
    int getSleepPassCount();
    boolean isAsleep();
    void putToSleep();
    void wakeUp();

    /**  \brief Set user data. Not used internally.	*/
    void setUserData(Object userData);
    // Returns value set by setUserData().
    Object getUserData();
}
