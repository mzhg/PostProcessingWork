package jet.opengl.particles;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.texture.Texture2D;

public class ParticleEmitter {
    /** The color scale of a particle over time. */
    final RangeVector3f colorScale = new RangeVector3f(Vector3f.ONE, Vector3f.ONE);
    /** The alpha scale of a particle over time */
    final RangeFloat alphaScale = new RangeFloat(1, 1);

    /** The color of a particle over time. */
    final RangeVector4f color = new RangeVector4f(Vector4f.ONE, Vector4f.ONE);

    final RangeFloat particleBornPerSecond = new RangeFloat(1,1);

    /** The texture sprite used for the particle, can be null. */
    Texture2D sprite;
    /** The normal map used for the particle, can be null. */
    Texture2D normal;

    private float mCurrentTime;
    private float mPreviousTime;

    public void setColorScale(ReadableVector3f start, ReadableVector3f end){
        colorScale.setStart(start);
        colorScale.setEnd(end);
    }

    public void setAlphaScale(float start, float end){
        alphaScale.setStart(start);
        alphaScale.setEnd(end);
    }

    public void setColor(ReadableVector4f start, ReadableVector4f end){
        color.setStart(start);
        color.setEnd(end);
    }

    public void setParticleBornPerSecond(float minParticleBorn, float maxParticleBorn){
        particleBornPerSecond.setStart(minParticleBorn);
        particleBornPerSecond.setEnd(maxParticleBorn);
    }

    public void setSprite(Texture2D sprite){
        this.sprite = sprite;
    }

    public void setNormal(Texture2D normal){
        this.normal = normal;
    }
}
