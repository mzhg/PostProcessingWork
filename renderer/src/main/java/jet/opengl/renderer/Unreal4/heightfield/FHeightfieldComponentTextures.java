package jet.opengl.renderer.Unreal4.heightfield;

import jet.opengl.postprocessing.texture.Texture2D;

public class FHeightfieldComponentTextures {

    public Texture2D HeightAndNormal;
    public Texture2D DiffuseColor;
    public Texture2D Visibility;

    public FHeightfieldComponentTextures(Texture2D heightAndNormal, Texture2D diffuseColor, Texture2D visibility) {
        HeightAndNormal = heightAndNormal;
        DiffuseColor = diffuseColor;
        Visibility = visibility;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FHeightfieldComponentTextures that = (FHeightfieldComponentTextures) o;

        if (HeightAndNormal != null ? !HeightAndNormal.equals(that.HeightAndNormal) : that.HeightAndNormal != null)
            return false;
        if (DiffuseColor != null ? !DiffuseColor.equals(that.DiffuseColor) : that.DiffuseColor != null)
            return false;
        return Visibility != null ? Visibility.equals(that.Visibility) : that.Visibility == null;
    }

    @Override
    public int hashCode() {
        int result = HeightAndNormal != null ? HeightAndNormal.hashCode() : 0;
        result = 31 * result + (DiffuseColor != null ? DiffuseColor.hashCode() : 0);
        result = 31 * result + (Visibility != null ? Visibility.hashCode() : 0);
        return result;
    }
}
