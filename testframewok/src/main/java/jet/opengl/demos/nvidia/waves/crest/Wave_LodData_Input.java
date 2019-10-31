package jet.opengl.demos.nvidia.waves.crest;

interface Wave_LodData_Input {
    void draw(float weight, boolean isTransition);
    float wavelength();
    boolean enabled ();
}
