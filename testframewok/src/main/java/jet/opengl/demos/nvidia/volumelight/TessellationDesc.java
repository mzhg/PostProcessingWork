package jet.opengl.demos.nvidia.volumelight;

final class TessellationDesc {
    private int meshMode;
    private int tesslationFactor;

    TessellationDesc(TessellationDesc other){
        set(other);
    }

    void set(TessellationDesc other){
        meshMode = other.meshMode;
        tesslationFactor = other.tesslationFactor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TessellationDesc that = (TessellationDesc) o;

        if (meshMode != that.meshMode) return false;
        return tesslationFactor == that.tesslationFactor;
    }

    @Override
    public int hashCode() {
        int result = meshMode;
        result = 31 * result + tesslationFactor;
        return result;
    }
}
