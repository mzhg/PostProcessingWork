package jet.opengl.demos.nvidia.waves.crest;

final class SamplingHeight {

    boolean valid;
    float height;

    SamplingHeight(){}

    SamplingHeight(boolean valid, float height) {
        this.valid = valid;
        this.height = height;
    }

    SamplingHeight(SamplingHeight ohs){
        this.valid = ohs.valid;
        this.height = ohs.height;
    }
}
