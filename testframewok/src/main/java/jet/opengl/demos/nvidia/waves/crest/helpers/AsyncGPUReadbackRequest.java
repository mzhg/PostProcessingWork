package jet.opengl.demos.nvidia.waves.crest.helpers;

import java.nio.ByteBuffer;

public class AsyncGPUReadbackRequest {

    public boolean hasError() {
        return false;
    }

    public boolean done() { return false;}

    public boolean GetData(ByteBuffer dest){  return true;}
}
