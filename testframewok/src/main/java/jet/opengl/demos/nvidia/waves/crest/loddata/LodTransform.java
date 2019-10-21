package jet.opengl.demos.nvidia.waves.crest.loddata;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.nvidia.waves.crest.CommandBuffer;
import jet.opengl.demos.nvidia.waves.crest.MonoBehaviour;
import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.demos.nvidia.waves.crest.helpers.IFloatingOrigin;
import jet.opengl.demos.nvidia.waves.crest.helpers.Time;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Rectf;

public class LodTransform extends MonoBehaviour implements IFloatingOrigin {

    protected int[] _transformUpdateFrame;

    static int s_paramsPosScale = 0; //Shader.PropertyToID("_LD_Pos_Scale");
    static int s_paramsPosScaleSource = 1; //Shader.PropertyToID("_LD_Pos_Scale_Source");
    static int s_paramsOcean = 2; //Shader.PropertyToID("_LD_Params");
    static int s_paramsOceanSource = 3; //Shader.PropertyToID("_LD_Params_Source");

    public class RenderData
    {
        public float _texelWidth;
        public float _textureRes;
        public final Vector3f _posSnapped = new Vector3f();
        public int _frame;

        public RenderData Validate(int frameOffset, Object context)
        {
            final int frameCount = Time.frameCount;
            // ignore first frame - this patches errors when using edit & continue in editor
            if (_frame > 0 && _frame != frameCount + frameOffset)
            {
                LogUtil.w(LogUtil.LogType.DEFAULT, String.format("RenderData validation failed: _frame of data (%d) != expected (%d), which may indicate some update functions are being called out of order, or script execution order is broken.", _frame, frameCount + frameOffset));
            }
            return this;
        }

        public Rectf RectXZ()
        {
            float w = _texelWidth * _textureRes;
            return new Rectf(_posSnapped.x - w / 2f, _posSnapped.z - w / 2f, w, w);
        }
    }

    public RenderData[] _renderData = null;
    public RenderData[] _renderDataSource = null;

    private int lodCount;
    public int LodCount() { return lodCount;}

    Matrix4f[] _worldToCameraMatrix;
    Matrix4f[] _projectionMatrix;
    public Matrix4f GetWorldToCameraMatrix(int lodIdx) { return _worldToCameraMatrix[lodIdx]; }
    public Matrix4f GetProjectionMatrix(int lodIdx) { return _projectionMatrix[lodIdx]; }

    public void InitLODData(int lodCount)
    {
        this.lodCount = lodCount;

        _renderData = new RenderData[lodCount];
        _renderDataSource = new RenderData[lodCount];
        _worldToCameraMatrix = new Matrix4f[lodCount];
        _projectionMatrix = new Matrix4f[lodCount];

        _transformUpdateFrame = new int[lodCount];
        for (int i = 0; i < _transformUpdateFrame.length; i++)
        {
            _renderData[i] = new RenderData();
            _renderDataSource[i] = new RenderData();
            _worldToCameraMatrix[i] = new Matrix4f();
            _projectionMatrix[i] = new Matrix4f();
            _transformUpdateFrame[i] = -1;
        }
    }

    public void UpdateTransforms()
    {
        int frameCount = Time.frameCount;
        for (int lodIdx = 0; lodIdx < LodCount(); lodIdx++)
        {
            if (_transformUpdateFrame[lodIdx] == frameCount) continue;

            _transformUpdateFrame[lodIdx] = frameCount;

            _renderDataSource[lodIdx] = _renderData[lodIdx];

            float lodScale = OceanRenderer.Instance.CalcLodScale(lodIdx);
            float camOrthSize = 2f * lodScale;

            // find snap period
            _renderData[lodIdx]._textureRes = OceanRenderer.Instance.LodDataResolution();
            _renderData[lodIdx]._texelWidth = 2f * camOrthSize / _renderData[lodIdx]._textureRes;
            // snap so that shape texels are stationary
//            _renderData[lodIdx]._posSnapped = OceanRenderer.Instance.transform.position
//                    - new Vector3(Mathf.Repeat(OceanRenderer.Instance.transform.position.x, _renderData[lodIdx]._texelWidth), 0f, Mathf.Repeat(OceanRenderer.Instance.transform.position.z, _renderData[lodIdx]._texelWidth));

            _renderData[lodIdx]._posSnapped.x = OceanRenderer.Instance.transform.getPositionX() - (OceanRenderer.Instance.transform.getPositionX() % _renderData[lodIdx]._texelWidth);
            _renderData[lodIdx]._posSnapped.y = OceanRenderer.Instance.transform.getPositionY();
            _renderData[lodIdx]._posSnapped.z = OceanRenderer.Instance.transform.getPositionZ() - (OceanRenderer.Instance.transform.getPositionZ() % _renderData[lodIdx]._texelWidth);
            _renderData[lodIdx]._frame = frameCount;

            // detect first update and populate the render data if so - otherwise it can give divide by 0s and other nastiness
            if (_renderDataSource[lodIdx]._textureRes == 0f)
            {
                _renderDataSource[lodIdx]._posSnapped.set(_renderData[lodIdx]._posSnapped);
                _renderDataSource[lodIdx]._texelWidth = _renderData[lodIdx]._texelWidth;
                _renderDataSource[lodIdx]._textureRes = _renderData[lodIdx]._textureRes;
            }

            Vector3f position = new Vector3f(_renderData[lodIdx]._posSnapped);
            position.y += 100;

            Quaternion rot = new Quaternion();
            rot.setFromAxisAngle(Vector3f.X_AXIS, (float)Math.toRadians(90));

            CalculateWorldToCameraMatrixRHS(/*_renderData[lodIdx]._posSnapped + Vector3.up * 100f, Quaternion.AngleAxis(90f, Vector3.right)*/position, rot, _worldToCameraMatrix[lodIdx]);

            Matrix4f.ortho(-2f * lodScale, 2f * lodScale, -2f * lodScale, 2f * lodScale, 1f, 500f, _projectionMatrix[lodIdx]);
        }
    }

    // Borrowed from LWRP code: https://github.com/Unity-Technologies/ScriptableRenderPipeline/blob/2a68d8073c4eeef7af3be9e4811327a522434d5f/com.unity.render-pipelines.high-definition/Runtime/Core/Utilities/GeometryUtils.cs
    public static void CalculateWorldToCameraMatrixRHS(Vector3f position, Quaternion rotation, Matrix4f view)
    {
        rotation.toMatrix(view);
        view.m30 = position.x;
        view.m31 = position.y;
        view.m32 = position.z;

        view.transpose();

//        return Matrix4x4.Scale(new Vector3(1, 1, -1)) * Matrix4x4.TRS(position, rotation, Vector3.one).inverse;
    }

    public void SetViewProjectionMatrices(int lodIdx, CommandBuffer buf)
    {
        buf.SetViewProjectionMatrices(GetWorldToCameraMatrix(lodIdx), GetProjectionMatrix(lodIdx));
    }

    public float MaxWavelength(int lodIdx)
    {
        float oceanBaseScale = OceanRenderer.Instance.Scale;
        float maxDiameter = (float) (4f * oceanBaseScale * Math.pow(2f, lodIdx));
        float maxTexelSize = maxDiameter / OceanRenderer.Instance.LodDataResolution();
        return 2f * maxTexelSize * OceanRenderer.Instance.MinTexelsPerWave;
    }

    public static int ParamIdPosScale(boolean sourceLod /*= false*/)
    {
        if (sourceLod)
        {
            return s_paramsPosScaleSource;
        }
        else
        {
            return s_paramsPosScale;
        }
    }

    public static int ParamIdOcean(boolean sourceLod /*= false*/)
    {
        if (sourceLod)
        {
            return s_paramsOceanSource;
        }
        else
        {
            return s_paramsOcean;
        }
    }

    public void SetOrigin(ReadableVector3f newOrigin)
    {
        for (int lodIdx = 0; lodIdx < LodCount(); lodIdx++)
        {
//            _renderData[lodIdx]._posSnapped -= newOrigin;
//            _renderDataSource[lodIdx]._posSnapped -= newOrigin;

            Vector3f.sub(_renderData[lodIdx]._posSnapped, newOrigin, _renderData[lodIdx]._posSnapped);
            Vector3f.sub(_renderDataSource[lodIdx]._posSnapped, newOrigin, _renderDataSource[lodIdx]._posSnapped);
        }
    }
}
