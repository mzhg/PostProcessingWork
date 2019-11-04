package jet.opengl.demos.nvidia.waves.crest;

import jet.opengl.postprocessing.shader.Macro;

final class WaveRenderDesc implements Cloneable{
    boolean applyNormalMapping;
    boolean computeDirectionalLight;
    boolean subSurfaceScattering;
    boolean subSurfaceAllowColor;
    boolean transparency;
    boolean caustics;
    boolean foam;
    boolean foam3dlighting;
    boolean planarreflections;
    boolean overrideReflectionCubemap;
    boolean proceduralSky;
    boolean underwater;
    boolean flow;
    boolean shadows;
    boolean debugDisableShapeTextures;
    boolean debugVisualiseShapeSample;
    boolean debugVisualiseFlow;
    boolean debugDisableSmoothlod;
    boolean debugWireframe;

    public Macro[] getMacros(){
        return new Macro[]{
          new Macro("_APPLYNORMALMAPPING_ON", applyNormalMapping),
          new Macro("_COMPUTEDIRECTIONALLIGHT_ON", computeDirectionalLight),
          new Macro("_SUBSURFACESCATTERING_ON", subSurfaceScattering),
          new Macro("_SUBSURFACESHALLOWCOLOUR_ON", subSurfaceAllowColor),
          new Macro("_TRANSPARENCY_ON", transparency),
          new Macro("_CAUSTICS_ON", caustics),
          new Macro("_FOAM_ON", foam),
          new Macro("_FOAM3DLIGHTING_ON", foam3dlighting),
          new Macro("_PLANARREFLECTIONS_ON", planarreflections),
          new Macro("_OVERRIDEREFLECTIONCUBEMAP_ON", overrideReflectionCubemap),
          new Macro("_PROCEDURALSKY_ON", proceduralSky),
          new Macro("_UNDERWATER_ON", underwater),
          new Macro("_DEBUGDISABLESHAPETEXTURES_ON", debugDisableShapeTextures),
          new Macro("_SHADOWS_ON", shadows),
          new Macro("_DEBUGVISUALISESHAPESAMPLE_ON", debugVisualiseShapeSample),
          new Macro("_DEBUGVISUALISEFLOW_ON", debugVisualiseFlow),
          new Macro("_DEBUGDISABLESMOOTHLOD_ON", debugDisableSmoothlod),
          new Macro("_WIREFRAME_ONE", debugWireframe),
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WaveRenderDesc that = (WaveRenderDesc) o;

        if (applyNormalMapping != that.applyNormalMapping) return false;
        if (computeDirectionalLight != that.computeDirectionalLight) return false;
        if (subSurfaceScattering != that.subSurfaceScattering) return false;
        if (subSurfaceAllowColor != that.subSurfaceAllowColor) return false;
        if (transparency != that.transparency) return false;
        if (caustics != that.caustics) return false;
        if (foam != that.foam) return false;
        if (foam3dlighting != that.foam3dlighting) return false;
        if (planarreflections != that.planarreflections) return false;
        if (overrideReflectionCubemap != that.overrideReflectionCubemap) return false;
        if (proceduralSky != that.proceduralSky) return false;
        if (underwater != that.underwater) return false;
        if (flow != that.flow) return false;
        if (shadows != that.shadows) return false;
        if (debugDisableShapeTextures != that.debugDisableShapeTextures) return false;
        if (debugVisualiseShapeSample != that.debugVisualiseShapeSample) return false;
        if (debugVisualiseFlow != that.debugVisualiseFlow) return false;
        if (debugWireframe != that.debugWireframe) return false;
        return debugDisableSmoothlod == that.debugDisableSmoothlod;
    }

    @Override
    public int hashCode() {
        int result = (applyNormalMapping ? 1 : 0);
        result = 31 * result + (computeDirectionalLight ? 1 : 0);
        result = 31 * result + (subSurfaceScattering ? 1 : 0);
        result = 31 * result + (subSurfaceAllowColor ? 1 : 0);
        result = 31 * result + (transparency ? 1 : 0);
        result = 31 * result + (caustics ? 1 : 0);
        result = 31 * result + (foam ? 1 : 0);
        result = 31 * result + (foam3dlighting ? 1 : 0);
        result = 31 * result + (planarreflections ? 1 : 0);
        result = 31 * result + (overrideReflectionCubemap ? 1 : 0);
        result = 31 * result + (proceduralSky ? 1 : 0);
        result = 31 * result + (underwater ? 1 : 0);
        result = 31 * result + (flow ? 1 : 0);
        result = 31 * result + (shadows ? 1 : 0);
        result = 31 * result + (debugDisableShapeTextures ? 1 : 0);
        result = 31 * result + (debugVisualiseShapeSample ? 1 : 0);
        result = 31 * result + (debugVisualiseFlow ? 1 : 0);
        result = 31 * result + (debugDisableSmoothlod ? 1 : 0);
        result = 31 * result + (debugWireframe ? 1 : 0);
        return result;
    }

    @Override
    protected Object clone()  {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return null;
    }
}
