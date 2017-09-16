package nv.visualFX.cloth.libs.dx;

import java.nio.FloatBuffer;

/**
 * Created by mazhen'gui on 2017/9/16.
 */

final class DxUtil {
    static float maxElement(FloatBuffer buffer){
        float maxValue = -Float.MAX_VALUE;
        for(int i = buffer.position(); i < buffer.limit(); i++){
            maxValue = Math.max(maxValue, buffer.get(i));
        }

        return maxValue;
    }
}
