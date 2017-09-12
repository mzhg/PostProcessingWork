package nv.visualFX.cloth.libs.dx;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.ReadableVector3f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import nv.visualFX.cloth.libs.Cloth;
import nv.visualFX.cloth.libs.Fabric;
import nv.visualFX.cloth.libs.Factory;
import nv.visualFX.cloth.libs.GpuParticles;

/**
 * Created by mazhen'gui on 2017/9/12.
 */

final class DxCloth implements Cloth{

    DxCloth(DxFactory factory, DxFabric fabric, FloatBuffer particles){

    }

    DxCloth(DxFactory factory, DxCloth cloth){

    }

    @Override
    public Cloth clone(Factory factory) {
        return null;
    }

    @Override
    public Fabric getFabric() {
        return null;
    }

    @Override
    public int getNumParticles() {
        return 0;
    }

    @Override
    public void lockParticles() {

    }

    @Override
    public void unlockParticles() {

    }

    @Override
    public FloatBuffer getCurrentParticles() {
        return null;
    }

    @Override
    public FloatBuffer getPreviousParticles() {
        return null;
    }

    @Override
    public GpuParticles getGpuParticles() {
        return null;
    }

    @Override
    public void setTranslation(ReadableVector3f trans) {

    }

    @Override
    public void setRotation(Quaternion rot) {

    }

    @Override
    public ReadableVector3f getTranslation() {
        return null;
    }

    @Override
    public Quaternion getRotation() {
        return null;
    }

    @Override
    public void clearInertia() {

    }

    @Override
    public void teleport(ReadableVector3f delta) {

    }

    @Override
    public float getPreviousIterationDt() {
        return 0;
    }

    @Override
    public void setGravity(ReadableVector3f gravity) {

    }

    @Override
    public ReadableVector3f getGravity() {
        return null;
    }

    @Override
    public void setDamping(ReadableVector3f damping) {

    }

    @Override
    public ReadableVector3f getDamping() {
        return null;
    }

    @Override
    public void setLinearDrag(ReadableVector3f drag) {

    }

    @Override
    public ReadableVector3f getLinearDrag() {
        return null;
    }

    @Override
    public void setAngularDrag(ReadableVector3f drag) {

    }

    @Override
    public ReadableVector3f getAngularDrag() {
        return null;
    }

    @Override
    public void setLinearInertia(ReadableVector3f inertia) {

    }

    @Override
    public ReadableVector3f getLinearInertia() {
        return null;
    }

    @Override
    public void setAngularInertia(ReadableVector3f inertia) {

    }

    @Override
    public ReadableVector3f getAngularInertia() {
        return null;
    }

    @Override
    public void setCentrifugalInertia(ReadableVector3f inertia) {

    }

    @Override
    public ReadableVector3f getCentrifugalInertia() {
        return null;
    }

    @Override
    public void setSolverFrequency(float frequency) {

    }

    @Override
    public float getSolverFrequency() {
        return 0;
    }

    @Override
    public void setStiffnessFrequency(float frequency) {

    }

    @Override
    public float getStiffnessFrequency() {
        return 0;
    }

    @Override
    public void setAcceleationFilterWidth(int filterWidth) {

    }

    @Override
    public int getAccelerationFilterWidth() {
        return 0;
    }

    @Override
    public void setPhaseConfig(ByteBuffer configs) {

    }

    @Override
    public void setSpheres(FloatBuffer spheres, int first, int last) {

    }

    @Override
    public int getNumSpheres() {
        return 0;
    }

    @Override
    public void setCapsules(IntBuffer capsules, int first, int last) {

    }

    @Override
    public int getNumCapsules() {
        return 0;
    }

    @Override
    public void setPlanes(FloatBuffer planes, int first, int last) {

    }

    @Override
    public int getNumPlanes() {
        return 0;
    }

    @Override
    public void setConvexes(IntBuffer convexMasks, int first, int last) {

    }

    @Override
    public int getNumConvexes() {
        return 0;
    }

    @Override
    public void setTriangles(FloatBuffer triangles, int first, int last) {

    }

    @Override
    public void setTriangles(FloatBuffer triangles, FloatBuffer value, int first) {

    }

    @Override
    public int getNumTriangles() {
        return 0;
    }

    @Override
    public boolean isContinuousCollisionEnabled() {
        return false;
    }

    @Override
    public void enableContinuousCollision(boolean flag) {

    }

    @Override
    public float getCollisionMassScale() {
        return 0;
    }

    @Override
    public void setCollisionMassScale(float scale) {

    }

    @Override
    public void setFriction(float friction) {

    }

    @Override
    public float getFriction() {
        return 0;
    }

    @Override
    public void setVirtualParticles(IntBuffer indices, FloatBuffer weights) {

    }

    @Override
    public int getNumVirtualParticles() {
        return 0;
    }

    @Override
    public int getNumVirtualParticleWeights() {
        return 0;
    }

    @Override
    public void setTetherConstraintScale(float scale) {

    }

    @Override
    public float getTetherConstraintScale() {
        return 0;
    }

    @Override
    public void setTetherConstraintStiffness(float stiffness) {

    }

    @Override
    public float getTetherConstraintStiffness() {
        return 0;
    }

    @Override
    public FloatBuffer getMotionConstraints() {
        return null;
    }

    @Override
    public void clearMotionConstraints() {

    }

    @Override
    public int getNumMotionConstraints() {
        return 0;
    }

    @Override
    public void setMotionConstraintScaleBias(float scale, float bias) {

    }

    @Override
    public float getMotionConstraintScale() {
        return 0;
    }

    @Override
    public float getMotionConstraintBias() {
        return 0;
    }

    @Override
    public void setMotionConstraintStiffness(float stiffness) {

    }

    @Override
    public float getMotionConstraintStiffness() {
        return 0;
    }

    @Override
    public FloatBuffer getSeparationConstraints() {
        return null;
    }

    @Override
    public void clearSeparationConstraints() {

    }

    @Override
    public int getNumSeparationConstraints() {
        return 0;
    }

    @Override
    public void clearInterpolation() {

    }

    @Override
    public FloatBuffer getParticleAccelerations() {
        return null;
    }

    @Override
    public void clearParticleAccelerations() {

    }

    @Override
    public int getNumParticleAccelerations() {
        return 0;
    }

    @Override
    public void setWindVelocity(ReadableVector3f velocity) {

    }

    @Override
    public ReadableVector3f getWindVelocity() {
        return null;
    }

    @Override
    public void setDragCoefficient(float coefficient) {

    }

    @Override
    public float getDragCoefficient() {
        return 0;
    }

    @Override
    public void setLiftCoefficient(float coefficient) {

    }

    @Override
    public float getLiftCoefficient() {
        return 0;
    }

    @Override
    public void setFluidDensity(float density) {

    }

    @Override
    public float getFluidDensity() {
        return 0;
    }

    @Override
    public void setSelfCollisionDistance(float distance) {

    }

    @Override
    public float getSelfCollisionDistance() {
        return 0;
    }

    @Override
    public void setSelfCollisionStiffness(float stiffness) {

    }

    @Override
    public float getSelfCollisionStiffness() {
        return 0;
    }

    @Override
    public void setSelfCollisionIndices(IntBuffer indices) {

    }

    @Override
    public int getNumSelfCollisionIndices() {
        return 0;
    }

    @Override
    public void setRestPositions(FloatBuffer positions) {

    }

    @Override
    public int getNumRestPositions() {
        return 0;
    }

    @Override
    public ReadableVector3f getBoundingBoxCenter() {
        return null;
    }

    @Override
    public ReadableVector3f getBoundingBoxScale() {
        return null;
    }

    @Override
    public void setSleepThreshold(float threshold) {

    }

    @Override
    public float getSleepThreshold() {
        return 0;
    }

    @Override
    public void setSleepTestInterval(int interval) {

    }

    @Override
    public int getSleepTestInterval() {
        return 0;
    }

    @Override
    public void setSleepAfterCount(int count) {

    }

    @Override
    public int getSleepAfterCount() {
        return 0;
    }

    @Override
    public int getSleepPassCount() {
        return 0;
    }

    @Override
    public boolean isAsleep() {
        return false;
    }

    @Override
    public void putToSleep() {

    }

    @Override
    public void wakeUp() {

    }

    @Override
    public void setUserData(Object userData) {

    }

    @Override
    public Object getUserData() {
        return null;
    }
}
