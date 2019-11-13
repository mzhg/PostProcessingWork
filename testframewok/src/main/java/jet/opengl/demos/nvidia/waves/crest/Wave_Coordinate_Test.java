package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.util.Numeric;

final class Wave_Coordinate_Test {

    private static float g_Scale = 2;
    private static int g_LodIdx = 5;
    private static final int lodDataResolution = 256;

    static float calcLodScale(float lodIndex) { return (float) (g_Scale * Math.pow(2f, lodIndex)); }

    private static final Vector3f PosScales = new Vector3f();
    private static final Vector4f Params = new Vector4f();
    private static final Vector3f g_EyePos = new Vector3f(100  ,0,818);

    private static final Matrix4f _worldToCameraMatrix = new Matrix4f();
    private static final Matrix4f _projectionMatrix = new Matrix4f();

    static void intilize(){
        float lodScale = calcLodScale(g_LodIdx);
        float camOrthSize = 2f * lodScale;

        // find snap period
        float _textureRes = lodDataResolution;
        float _texelWidth = 2f * camOrthSize / _textureRes;

        Vector3f _posSnapped = new Vector3f();

        // snap so that shape texels are stationary
        _posSnapped.x = g_EyePos.getX() - Numeric.fmod(g_EyePos.getX(), _texelWidth);
        _posSnapped.y = g_EyePos.getY();
        _posSnapped.z = g_EyePos.getZ() - Numeric.fmod(g_EyePos.getZ(), _texelWidth);

        Vector3f center = new Vector3f();

        ReadableVector3f cameraPos = _posSnapped;
        Vector3f.add(cameraPos, Vector3f.Z_AXIS_NEG, center);
        Matrix4f.lookAt(cameraPos, center, Vector3f.Y_AXIS, _worldToCameraMatrix);   // todo look down
        Matrix4f.ortho(-2f * lodScale, 2f * lodScale, 2f * lodScale, -2f * lodScale, -1000, 1000, _projectionMatrix);

        // NOTE: gets zeroed by unity, see https://www.alanzucconi.com/2016/10/24/arrays-shaders-unity-5-4/
        PosScales.set(_posSnapped.x, _posSnapped.z, calcLodScale(g_LodIdx));
        Params.set(_texelWidth, _textureRes, 1f, 1f / _textureRes);
    }

    private static final Vector2f LD_UVToWorld(Vector2f i_uv, Vector2f i_centerPos, float i_res, float i_texelSize)
    {
//        return i_texelSize * i_res * (i_uv - 0.5) + i_centerPos;
        Vector2f result = new Vector2f();
        result.x = i_texelSize * i_res * (i_uv.x - 0.5f) + i_centerPos.x;
        result.y = i_texelSize * i_res * (i_uv.y - 0.5f) + i_centerPos.y;

        return result;
    }

    // Conversions for world space from/to UV space. All these should *not* be clamped otherwise they'll break fullscreen triangles.
    private static final Vector2f LD_WorldToUV(Vector2f i_samplePos, Vector2f i_centerPos, float i_res, float i_texelSize)
    {
//        return (i_samplePos - i_centerPos) / (i_texelSize * i_res) + 0.5;
        Vector2f result = new Vector2f();
        result.x = (i_samplePos.x - i_centerPos.x) / (i_texelSize * i_res) + 0.5f;
        result.y = (i_samplePos.y - i_centerPos.y) / (i_texelSize * i_res) + 0.5f;

        return result;
    }

    static Vector2f uvToWorldByParams(Vector2f i_uv) { return LD_UVToWorld(i_uv, new Vector2f(PosScales), Params.y, Params.x); }

    static Vector2f uvToWorldByMatrix(Vector2f uv){
        Vector4f clipPos = new Vector4f(2* uv.x-1, 2*uv.y-1, 0, 1);
        Matrix4f clipToWorld = Matrix4f.mul(_projectionMatrix, _worldToCameraMatrix, null);
        clipToWorld.invert();

        Vector4f worldPos = Matrix4f.transform(clipToWorld, clipPos, clipPos);
        return new Vector2f(worldPos.x, worldPos.y);
    }

    static Vector2f worldToUvByParams(Vector2f worldXZ){
        Vector2f result = LD_WorldToUV(
                worldXZ,
                new Vector2f(PosScales),
                Params.y,
                Params.x
        );
        return result;
    }

    static Vector3f worldToUvByMatrix(Vector2f worldXZ){
        float lodScale = calcLodScale(g_LodIdx);
        float camOrthSize = 2f * lodScale;

        Vector4f worldPos = new Vector4f(worldXZ.x, 0, worldXZ.y, 1);
        Matrix4f rot = new Matrix4f();
        rot.rotate((float)Math.toRadians(90), Vector3f.X_AXIS, null);


        Matrix4f.transform(rot, worldPos, worldPos);
        Matrix4f.transform(_worldToCameraMatrix, worldPos, worldPos);
        Matrix4f.transform(_projectionMatrix, worldPos, worldPos);

        worldPos.x = worldPos.x/worldPos.w * 0.5f + 0.5f;
        worldPos.z = worldPos.z/worldPos.w * 0.5f + 0.5f;
        worldPos.y = worldPos.y/worldPos.w * 0.5f + 0.5f;

        return new Vector3f(worldPos);
    }

    public static void main(String[] args){
        intilize();

        Vector2f worldXZ = new Vector2f(20, 20);
        Vector2f uv1 = worldToUvByParams(worldXZ);
        Vector3f uv2 = worldToUvByMatrix(worldXZ);

        System.out.println("uv1 = " +uv1);
        System.out.println("uv2 = " +uv2);

        System.out.println("worldXZ = " + uvToWorldByParams(uv1));
//        System.out.println("worldXZ = " + uvToWorldByMatrix(uv2));
    }
}
