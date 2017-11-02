package jet.opengl.demos.intel.avsm;

/**
 * Created by mazhen'gui on 2017/10/9.
 */

final class FrameStats {
    float VolumetricShadowTime;
    float SolidGeometryTime;
    float ParticlesTime;
    float TotalTime;

    void reset(){
        VolumetricShadowTime = 0;
        SolidGeometryTime = 0;
        ParticlesTime = 0;
        TotalTime = 0;
    }

    void set(FrameStats ohs){
        VolumetricShadowTime = ohs.VolumetricShadowTime;
        SolidGeometryTime = ohs.SolidGeometryTime;
        ParticlesTime = ohs.ParticlesTime;
        TotalTime = ohs.TotalTime;
    }
}
