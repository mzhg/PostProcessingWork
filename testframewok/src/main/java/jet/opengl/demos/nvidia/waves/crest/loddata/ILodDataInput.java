package jet.opengl.demos.nvidia.waves.crest.loddata;

import jet.opengl.demos.nvidia.waves.crest.CommandBuffer;

public interface ILodDataInput {
    void Draw(CommandBuffer buf, float weight, int isTransition);
    float Wavelength();
    boolean Enabled ();
}
