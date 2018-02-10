package jet.opengl.demos.flight404;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2018/2/10.
 */

final class Particle {
    final Vector3f loc = new Vector3f();  // position vector
    final Vector3f vel = new Vector3f();  // velocity vector
    float radius; // particle's size
    float age;    // current age of particle
    float lifeSpan; // max allowed age of particle
    float gen; 	  // number of times particle has been involved in a SPLIT
    float bounceAge; // amount to age particle when it bounces off floor
    //		float bounceVel; // speed at impact
    int type;

    Particle reset(Vector3f _loc, Vector2f _vel){
        gen = 1;
        radius = Numeric.random(10 - gen, 50 - (gen - 1) * 10);
        loc.set(_loc);
        loc.x += Numeric.random(0, 1);
        loc.y += Numeric.random(0, 1);
        loc.z += Numeric.random(0, 1);
//			vel.set(_vel.x, _vel.y);
        float angle = Numeric.random(0, Numeric.PI * 2.0f);
        vel.set(0,0,0);
        vel.z += Math.cos(angle);
        vel.x += Math.sin(angle);
        vel.y += -Math.sqrt(1.0 - vel.x * vel.x - vel.y * vel.y);
        vel.scale(Numeric.random(10, 20));
        age = 0;
        bounceAge = 2;
        lifeSpan = radius * 0.222f;
        return this;
    }

    void store(ByteBuffer buf){
        loc.store(buf);
        vel.store(buf);
        buf.putFloat(radius);
        buf.putFloat(age);
        buf.putFloat(lifeSpan);
        buf.putFloat(gen);
        buf.putFloat(bounceAge);
        buf.putInt(0);
    }

    void load(ByteBuffer buf){
        loc.load(buf);
        vel.load(buf);
        radius = buf.getFloat();
        age = buf.getFloat();
        lifeSpan = buf.getFloat();
        gen = buf.getFloat();
        bounceAge = buf.getFloat();
        type = buf.getInt();
    }

    @Override
    public String toString() {
        return "Particle [loc=" + loc + ", vel=" + vel + ", radius=" + radius + ", age=" + age + ", lifeSpan="
                + lifeSpan + ", gen=" + gen + ", bounceAge=" + bounceAge + ", type=" + type + "]";
    }
}
