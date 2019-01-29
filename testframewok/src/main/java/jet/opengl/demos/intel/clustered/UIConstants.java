package jet.opengl.demos.intel.clustered;


import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

final class UIConstants implements Readable {
     static final int SIZE = Vector4f.SIZE;

     int lightingOnly;
     int faceNormals;
     int visualizeLightCount;
     int visualizePerSampleShading;

     int lightCullTechnique;
     int clusteredGridScale;
     int Dummy0;
     int Dummy1;

     @Override
     public ByteBuffer store(ByteBuffer buf) {
          buf.putInt(lightingOnly);
          buf.putInt(faceNormals);
          buf.putInt(visualizeLightCount);
          buf.putInt(visualizePerSampleShading);

          buf.putInt(lightCullTechnique);
          buf.putInt(clusteredGridScale);
          buf.putInt(Dummy0);
          buf.putInt(Dummy1);
          return buf;
     }
}
