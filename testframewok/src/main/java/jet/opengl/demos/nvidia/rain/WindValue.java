package jet.opengl.demos.nvidia.rain;

import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/6/30.
 */

final class WindValue {
    Vector3f windAmount;
    int time;

    WindValue(Vector3f windAmount, int time){
        this.windAmount = windAmount;
        this.time       = time;
    }
}
