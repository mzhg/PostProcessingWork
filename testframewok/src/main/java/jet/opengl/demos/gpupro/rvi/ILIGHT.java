package jet.opengl.demos.gpupro.rvi;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.core.volumetricLighting.LightType;

abstract class ILIGHT {

    int index;
    boolean active = true;
    boolean hasShadow;
    boolean performUpdate = true;
    GLOBAL_ILLUM globalIllumPP;

    abstract LightType GetLightType();

    abstract void Update();

    abstract void SetupShadowMapSurface(SURFACE surface);

    // adds surface for direct illumination
    abstract void AddLitSurface();

    // adds surfaces for indirect illumination
    abstract void AddGridSurfaces();

    abstract BufferGL GetUniformBuffer();

    int GetIndex()
    {
        return index;
    }

    void SetActive(boolean active)
    {
        this.active = active;
    }

    boolean IsActive()
    {
        return active;
    }

    boolean HasShadow()
    {
        return hasShadow;
    }

    abstract void CalculateMatrices();

    abstract void UpdateUniformBuffer();
}
