package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.Rectf;

final class Wave_LOD_Transform {
    final class RenderData
    {
        public float _texelWidth;
        public float _textureRes;
        public final Vector3f _posSnapped = new Vector3f();

        private final Rectf tempRect = new Rectf();

        public Rectf RectXZ()
        {
            float w = _texelWidth * _textureRes;
            tempRect.set(_posSnapped.x - w / 2f, _posSnapped.z - w / 2f, w, w);
            return tempRect;
        }
    }

    public RenderData[] _renderData = null;

    private int lodCount;
    public int LodCount() { return lodCount;}

    /*Matrix4f[] _worldToCameraMatrix;
    Matrix4f[] _projectionMatrix;
    public Matrix4f GetWorldToCameraMatrix(int lodIdx) { return _worldToCameraMatrix[lodIdx]; }
    public Matrix4f GetProjectionMatrix(int lodIdx) { return _projectionMatrix[lodIdx]; }*/

    public void InitLODData(int lodCount)
    {
        this.lodCount = lodCount;

        _renderData = new RenderData[lodCount];
//        _worldToCameraMatrix = new Matrix4f[lodCount];
//        _projectionMatrix = new Matrix4f[lodCount];

        for (int i = 0; i < lodCount; i++)
        {
            _renderData[i] = new RenderData();
        }
    }

    public void UpdateTransforms(int lodDataResolution, float lodScale, ReadableVector3f eyePos, float seaLevel)
    {
        for (int lodIdx = 0; lodIdx < LodCount(); lodIdx++)
        {
            float camOrthSize = 2f * lodScale;

            // find snap period
            _renderData[lodIdx]._textureRes = OceanRenderer.Instance.LodDataResolution();
            _renderData[lodIdx]._texelWidth = 2f * camOrthSize / _renderData[lodIdx]._textureRes;

            // snap so that shape texels are stationary
            _renderData[lodIdx]._posSnapped.x = eyePos.getX() - (eyePos.getX() % _renderData[lodIdx]._texelWidth);
            _renderData[lodIdx]._posSnapped.y = seaLevel;
            _renderData[lodIdx]._posSnapped.z = eyePos.getZ() - (eyePos.getZ() % _renderData[lodIdx]._texelWidth);

        }
    }

    public float MaxWavelength(int lodIdx)
    {
        float oceanBaseScale = OceanRenderer.Instance.Scale;
        float maxDiameter = (float) (4f * oceanBaseScale * Math.pow(2f, lodIdx));
        float maxTexelSize = maxDiameter / OceanRenderer.Instance.LodDataResolution();
        return 2f * maxTexelSize * OceanRenderer.Instance.MinTexelsPerWave;
    }



}
