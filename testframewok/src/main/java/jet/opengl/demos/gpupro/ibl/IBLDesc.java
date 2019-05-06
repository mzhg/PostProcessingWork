package jet.opengl.demos.gpupro.ibl;

import jet.opengl.postprocessing.texture.TextureCube;

public class IBLDesc {
    public boolean sourceFromFile = false;
    public String sourceFilename;
    public TextureCube sourceEnvMap;

    public boolean outputToInternalCubeMap = true;
    public int outputSize = 128;
    public TextureCube outputEnvMap;
}
