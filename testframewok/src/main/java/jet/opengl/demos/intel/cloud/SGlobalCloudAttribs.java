package jet.opengl.demos.intel.cloud;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by mazhen'gui on 2017/7/7.
 */

final class SGlobalCloudAttribs implements Readable, Writable{

    int uiInnerRingDim = 128;
    int uiRingExtension = 4;
    int uiRingDimension = uiRingExtension + uiInnerRingDim + uiRingExtension;
    int uiNumRings = 5;

    int uiMaxLayers = 4;
    int uiNumCells = 0;
    int uiMaxParticles = 0;
    int uiDownscaleFactor = 2;

    float fCloudDensityThreshold = 0.35f;
    float fCloudThickness = 700.f;
    float fCloudAltitude = 3000.f;
    float fParticleCutOffDist = 2e+5f;

    float fTime;
    float fCloudVolumeDensity = 5e-3f;
    final Vector2f f2LiSpCloudDensityDim = new Vector2f(512,512);

    int uiBackBufferWidth = 1024;
    int uiBackBufferHeight = 768;
    int uiDownscaledBackBufferWidth = uiBackBufferWidth / uiDownscaleFactor;
    int uiDownscaledBackBufferHeight = uiBackBufferHeight/uiDownscaleFactor;

    float fTileTexWidth=32;
    float fTileTexHeight=32;
    int uiLiSpFirstListIndTexDim = 128;
    int uiNumCascades;

    final Vector4f f4Parameter = new Vector4f();

    float fScatteringCoeff = 0.07f;
    float fAttenuationCoeff = fScatteringCoeff;
    int uiNumParticleLayers = 1;
    int uiDensityGenerationMethod = 0;

    boolean bVolumetricBlending = true;
    int uiParameter;
    int uiDensityBufferScale = 2;
    float fReferenceParticleRadius = 200.f;

    final Vector4f[] f4TilingFrustumPlanes = new Vector4f[6];
    // Transform from view space to light projection space
    final Matrix4f mParticleTiling = new Matrix4f();

    SGlobalCloudAttribs(){
        for(int i = 0; i < f4TilingFrustumPlanes.length; i++){
            f4TilingFrustumPlanes[i] = new Vector4f();
        }
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SGlobalCloudAttribs{");
        sb.append("uiInnerRingDim=").append(uiInnerRingDim);
        sb.append("\n uiRingExtension=").append(uiRingExtension);
        sb.append("\n uiRingDimension=").append(uiRingDimension);
        sb.append("\n uiNumRings=").append(uiNumRings);
        sb.append("\n uiMaxLayers=").append(uiMaxLayers);
        sb.append("\n uiNumCells=").append(uiNumCells);
        sb.append("\n uiMaxParticles=").append(uiMaxParticles);
        sb.append("\n uiDownscaleFactor=").append(uiDownscaleFactor);
        sb.append("\n fCloudDensityThreshold=").append(fCloudDensityThreshold);
        sb.append("\n fCloudThickness=").append(fCloudThickness);
        sb.append("\n fCloudAltitude=").append(fCloudAltitude);
        sb.append("\n fParticleCutOffDist=").append(fParticleCutOffDist);
        sb.append("\n fTime=").append(fTime);
        sb.append("\n fCloudVolumeDensity=").append(fCloudVolumeDensity);
        sb.append("\n f2LiSpCloudDensityDim=").append(f2LiSpCloudDensityDim);
        sb.append("\n uiBackBufferWidth=").append(uiBackBufferWidth);
        sb.append("\n uiBackBufferHeight=").append(uiBackBufferHeight);
        sb.append("\n uiDownscaledBackBufferWidth=").append(uiDownscaledBackBufferWidth);
        sb.append("\n uiDownscaledBackBufferHeight=").append(uiDownscaledBackBufferHeight);
        sb.append("\n fTileTexWidth=").append(fTileTexWidth);
        sb.append("\n fTileTexHeight=").append(fTileTexHeight);
        sb.append("\n uiLiSpFirstListIndTexDim=").append(uiLiSpFirstListIndTexDim);
        sb.append("\n uiNumCascades=").append(uiNumCascades);
        sb.append("\n f4Parameter=").append(f4Parameter);
        sb.append("\n fScatteringCoeff=").append(fScatteringCoeff);
        sb.append("\n fAttenuationCoeff=").append(fAttenuationCoeff);
        sb.append("\n uiNumParticleLayers=").append(uiNumParticleLayers);
        sb.append("\n uiDensityGenerationMethod=").append(uiDensityGenerationMethod);
        sb.append("\n bVolumetricBlending=").append(bVolumetricBlending);
        sb.append("\n uiParameter=").append(uiParameter);
        sb.append("\n uiDensityBufferScale=").append(uiDensityBufferScale);
        sb.append("\n fReferenceParticleRadius=").append(fReferenceParticleRadius);
        sb.append("\n f4TilingFrustumPlanes=").append(f4TilingFrustumPlanes == null ? "null" : Arrays.asList(f4TilingFrustumPlanes).toString());
        sb.append("\n mParticleTiling=").append(mParticleTiling);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putInt(uiInnerRingDim);
        buf.putInt(uiRingExtension);
        buf.putInt(uiRingDimension);
        buf.putInt(uiNumRings);
        buf.putInt(uiMaxLayers);
        buf.putInt(uiNumCells);
        buf.putInt(uiMaxParticles);
        buf.putInt(uiDownscaleFactor);
        buf.putFloat(fCloudDensityThreshold);
        buf.putFloat(fCloudThickness);
        buf.putFloat(fCloudAltitude);
        buf.putFloat(fParticleCutOffDist);
        buf.putFloat(fTime);
        buf.putFloat(fCloudVolumeDensity);
        f2LiSpCloudDensityDim.store(buf);
        buf.putInt(uiBackBufferWidth);
        buf.putInt(uiBackBufferHeight);
        buf.putInt(uiDownscaledBackBufferWidth);
        buf.putInt(uiDownscaledBackBufferHeight);
        buf.putFloat(fTileTexWidth);
        buf.putFloat(fTileTexHeight);
        buf.putInt(uiLiSpFirstListIndTexDim);
        buf.putInt(uiNumCascades);
        f4Parameter.store(buf);
        buf.putFloat(fScatteringCoeff);
        buf.putFloat(fAttenuationCoeff);
        buf.putInt(uiNumParticleLayers);
        buf.putInt(uiDensityGenerationMethod);
        buf.putInt(bVolumetricBlending ? 1:0);
        buf.putInt(uiParameter);
        buf.putInt(uiDensityBufferScale);
        buf.putFloat(fReferenceParticleRadius);
        for(int i = 0; i < f4TilingFrustumPlanes.length; i++)
            f4TilingFrustumPlanes[i].store(buf);
        mParticleTiling.store(buf);
        return buf;
    }

    @Override
    public Writable load(ByteBuffer buf) {
        uiInnerRingDim = buf.getInt();
        uiRingExtension = buf.getInt();
        uiRingDimension = buf.getInt();
        uiNumRings = buf.getInt();
        uiMaxLayers = buf.getInt();
        uiNumCells = buf.getInt();
        uiMaxParticles = buf.getInt();
        uiDownscaleFactor = buf.getInt();
        fCloudDensityThreshold = buf.getFloat();
        fCloudThickness = buf.getFloat();
        fCloudAltitude = buf.getFloat();
        fParticleCutOffDist = buf.getFloat();
        fTime = buf.getFloat();
        fCloudVolumeDensity = buf.getFloat();
        f2LiSpCloudDensityDim.load(buf);
        uiBackBufferWidth = buf.getInt();
        uiBackBufferHeight = buf.getInt();
        uiDownscaledBackBufferWidth = buf.getInt();
        uiDownscaledBackBufferHeight = buf.getInt();
        fTileTexWidth = buf.getFloat();
        fTileTexHeight = buf.getFloat();
        uiLiSpFirstListIndTexDim = buf.getInt();
        uiNumCascades = buf.getInt();
        f4Parameter.load(buf);
        fScatteringCoeff = buf.getFloat();
        fAttenuationCoeff = buf.getFloat();
        uiNumParticleLayers = buf.getInt();
        uiDensityGenerationMethod = buf.getInt();
        bVolumetricBlending = buf.getInt()!=0;
        uiParameter = buf.getInt();
        uiDensityBufferScale = buf.getInt();
        fReferenceParticleRadius = buf.getFloat();
        for(int i = 0; i < f4TilingFrustumPlanes.length; i++)
            f4TilingFrustumPlanes[i].load(buf);
        mParticleTiling.load(buf);

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SGlobalCloudAttribs that = (SGlobalCloudAttribs) o;

        if (uiInnerRingDim != that.uiInnerRingDim) return false;
        if (uiRingExtension != that.uiRingExtension) return false;
        if (uiRingDimension != that.uiRingDimension) return false;
        if (uiNumRings != that.uiNumRings) return false;
        if (uiMaxLayers != that.uiMaxLayers) return false;
        if (uiNumCells != that.uiNumCells) return false;
        if (uiMaxParticles != that.uiMaxParticles) return false;
        if (uiDownscaleFactor != that.uiDownscaleFactor) return false;
        if (Float.compare(that.fCloudDensityThreshold, fCloudDensityThreshold) != 0) return false;
        if (Float.compare(that.fCloudThickness, fCloudThickness) != 0) return false;
        if (Float.compare(that.fCloudAltitude, fCloudAltitude) != 0) return false;
        if (Float.compare(that.fParticleCutOffDist, fParticleCutOffDist) != 0) return false;
        if (Float.compare(that.fTime, fTime) != 0) return false;
        if (Float.compare(that.fCloudVolumeDensity, fCloudVolumeDensity) != 0) return false;
        if (uiBackBufferWidth != that.uiBackBufferWidth) return false;
        if (uiBackBufferHeight != that.uiBackBufferHeight) return false;
        if (uiDownscaledBackBufferWidth != that.uiDownscaledBackBufferWidth) return false;
        if (uiDownscaledBackBufferHeight != that.uiDownscaledBackBufferHeight) return false;
        if (Float.compare(that.fTileTexWidth, fTileTexWidth) != 0) return false;
        if (Float.compare(that.fTileTexHeight, fTileTexHeight) != 0) return false;
        if (uiLiSpFirstListIndTexDim != that.uiLiSpFirstListIndTexDim) return false;
        if (uiNumCascades != that.uiNumCascades) return false;
        if (Float.compare(that.fScatteringCoeff, fScatteringCoeff) != 0) return false;
        if (Float.compare(that.fAttenuationCoeff, fAttenuationCoeff) != 0) return false;
        if (uiNumParticleLayers != that.uiNumParticleLayers) return false;
        if (uiDensityGenerationMethod != that.uiDensityGenerationMethod) return false;
        if (bVolumetricBlending != that.bVolumetricBlending) return false;
        if (uiParameter != that.uiParameter) return false;
        if (uiDensityBufferScale != that.uiDensityBufferScale) return false;
        if (Float.compare(that.fReferenceParticleRadius, fReferenceParticleRadius) != 0)
            return false;
        if (!f2LiSpCloudDensityDim.equals(that.f2LiSpCloudDensityDim)) return false;
        if (!f4Parameter.equals(that.f4Parameter)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(f4TilingFrustumPlanes, that.f4TilingFrustumPlanes)) return false;
        return mParticleTiling.equals(that.mParticleTiling);
    }

    @Override
    public int hashCode() {
        int result = uiInnerRingDim;
        result = 31 * result + uiRingExtension;
        result = 31 * result + uiRingDimension;
        result = 31 * result + uiNumRings;
        result = 31 * result + uiMaxLayers;
        result = 31 * result + uiNumCells;
        result = 31 * result + uiMaxParticles;
        result = 31 * result + uiDownscaleFactor;
        result = 31 * result + (fCloudDensityThreshold != +0.0f ? Float.floatToIntBits(fCloudDensityThreshold) : 0);
        result = 31 * result + (fCloudThickness != +0.0f ? Float.floatToIntBits(fCloudThickness) : 0);
        result = 31 * result + (fCloudAltitude != +0.0f ? Float.floatToIntBits(fCloudAltitude) : 0);
        result = 31 * result + (fParticleCutOffDist != +0.0f ? Float.floatToIntBits(fParticleCutOffDist) : 0);
        result = 31 * result + (fTime != +0.0f ? Float.floatToIntBits(fTime) : 0);
        result = 31 * result + (fCloudVolumeDensity != +0.0f ? Float.floatToIntBits(fCloudVolumeDensity) : 0);
        result = 31 * result + f2LiSpCloudDensityDim.hashCode();
        result = 31 * result + uiBackBufferWidth;
        result = 31 * result + uiBackBufferHeight;
        result = 31 * result + uiDownscaledBackBufferWidth;
        result = 31 * result + uiDownscaledBackBufferHeight;
        result = 31 * result + (fTileTexWidth != +0.0f ? Float.floatToIntBits(fTileTexWidth) : 0);
        result = 31 * result + (fTileTexHeight != +0.0f ? Float.floatToIntBits(fTileTexHeight) : 0);
        result = 31 * result + uiLiSpFirstListIndTexDim;
        result = 31 * result + uiNumCascades;
        result = 31 * result + f4Parameter.hashCode();
        result = 31 * result + (fScatteringCoeff != +0.0f ? Float.floatToIntBits(fScatteringCoeff) : 0);
        result = 31 * result + (fAttenuationCoeff != +0.0f ? Float.floatToIntBits(fAttenuationCoeff) : 0);
        result = 31 * result + uiNumParticleLayers;
        result = 31 * result + uiDensityGenerationMethod;
        result = 31 * result + (bVolumetricBlending ? 1 : 0);
        result = 31 * result + uiParameter;
        result = 31 * result + uiDensityBufferScale;
        result = 31 * result + (fReferenceParticleRadius != +0.0f ? Float.floatToIntBits(fReferenceParticleRadius) : 0);
        result = 31 * result + Arrays.hashCode(f4TilingFrustumPlanes);
        result = 31 * result + mParticleTiling.hashCode();
        return result;
    }
}
