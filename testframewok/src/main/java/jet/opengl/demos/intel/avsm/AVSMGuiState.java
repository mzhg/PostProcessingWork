package jet.opengl.demos.intel.avsm;

import jet.opengl.demos.intel.cput.AVSMMethod;

/**
 * Created by mazhen'gui on 2017/11/16.
 */

final class AVSMGuiState {
    AVSMMethod Method;
    int    ShadowTextureDimensions;
    int    NumberOfNodes;
    float           ParticleSize;
    float           ParticleOpacity;
    boolean            ParticlesPaused;
    boolean			EnableStats;
    boolean			WireFrame;
    boolean			tessellate;
    boolean			vertexShaderShadowLookup;
    float			TessellationDensity;
    float           LightLatitude;
    float           LightLongitude;


}
