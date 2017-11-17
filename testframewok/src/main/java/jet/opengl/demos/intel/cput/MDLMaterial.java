package jet.opengl.demos.intel.cput;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mazhen'gui on 2017/11/11.
 */

final class MDLMaterial {
    String texture_DM;
    String texture_SM;
    String texture_NM;
    boolean texture_NMSrgb;

    String vertexShaderFile;
    String vertexShaderMain;
    String vertexShaderProfile;

    String pixelShaderFile;
    String pixelShaderMain;
    String pixelShaderProfile;

    final List<String> buffers = new ArrayList<>();
}
