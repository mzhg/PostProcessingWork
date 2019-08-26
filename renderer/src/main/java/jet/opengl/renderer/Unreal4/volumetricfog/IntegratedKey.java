package jet.opengl.renderer.Unreal4.volumetricfog;

final class IntegratedKey {
    boolean bUseGlobalDistanceField;
    boolean bUseDistanceFieldSkyOcclusion;

    public IntegratedKey(boolean bUseGlobalDistanceField, boolean bUseDistanceFieldSkyOcclusion) {
        this.bUseGlobalDistanceField = bUseGlobalDistanceField;
        this.bUseDistanceFieldSkyOcclusion = bUseDistanceFieldSkyOcclusion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntegratedKey that = (IntegratedKey) o;

        if (bUseGlobalDistanceField != that.bUseGlobalDistanceField) return false;
        return bUseDistanceFieldSkyOcclusion == that.bUseDistanceFieldSkyOcclusion;
    }

    @Override
    public int hashCode() {
        int result = (bUseGlobalDistanceField ? 1 : 0);
        result = 31 * result + (bUseDistanceFieldSkyOcclusion ? 1 : 0);
        return result;
    }
}
