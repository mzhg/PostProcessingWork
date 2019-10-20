package jet.opengl.demos.nvidia.waves.crest.helpers;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class AsyncGPUReadbackRequest {

    public boolean hasError() {
        return false;
    }

    public boolean done() { return false;}

    public boolean GetData(ByteBuffer dest){  return true;}
    public boolean GetData(ShortBuffer dest){  return true;}
}
