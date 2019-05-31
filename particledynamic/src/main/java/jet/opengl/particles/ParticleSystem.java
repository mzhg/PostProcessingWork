package jet.opengl.particles;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.Disposeable;

public class ParticleSystem implements Disposeable {

    private List<ParticleEmitter> mEmitters = new ArrayList<>();

    public void addEmitter(ParticleEmitter emitter){
        if(emitter == null)
            return;

        if(!mEmitters.contains(emitter))
            mEmitters.add(emitter);
    }

    public boolean removeEmitter(ParticleEmitter emitter){
        return mEmitters.remove(emitter);
    }

    public void update(float dt){

    }

    public void render(){

    }

    @Override
    public void dispose() {

    }
}
