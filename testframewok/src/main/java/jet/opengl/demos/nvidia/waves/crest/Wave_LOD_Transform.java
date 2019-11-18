package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.Rectf;

final class Wave_LOD_Transform {
    static final class RenderData
    {
        public float _texelWidth;
        public float _textureRes;
        public final Vector3f _posSnapped = new Vector3f();

        private final Rectf tempRect = new Rectf();

        RenderData(){}
        public RenderData(RenderData ohs){ set(ohs);}


        public Rectf RectXZ()
        {
            float w = _texelWidth * _textureRes;
            tempRect.set(_posSnapped.x - w / 2f, _posSnapped.z - w / 2f, w, w);
            return tempRect;
        }

        void set(RenderData ohs){
            _texelWidth = ohs._texelWidth;
            _textureRes = ohs._textureRes;
            _posSnapped.set(ohs._posSnapped);
        }
    }

    RenderData[] _renderData = null;
    RenderData[] _renderDataSource = null;

    private Vector4f[] _BindData_paramIdPosScales;
    // Used in child
    private Vector4f[] _BindData_paramIdOceans;
    private int lodCount;
    private Wave_CDClipmap m_Clipmap;

    Wave_LOD_Transform(Wave_CDClipmap clipmap) {m_Clipmap = clipmap;}
    public int LodCount() { return lodCount;}

    private Matrix4f[] _worldToCameraMatrix;
    private Matrix4f[] _projectionMatrix;
    public Matrix4f GetWorldToCameraMatrix(int lodIdx) { return _worldToCameraMatrix[lodIdx]; }
    public Matrix4f GetProjectionMatrix(int lodIdx) { return _projectionMatrix[lodIdx]; }


    public void InitLODData(int lodCount)
    {
        this.lodCount = lodCount;

        _renderData = new RenderData[lodCount];
        _renderDataSource = new RenderData[lodCount];
        _BindData_paramIdPosScales = new Vector4f[lodCount];
        _BindData_paramIdOceans = new Vector4f[lodCount];
        _worldToCameraMatrix = new Matrix4f[lodCount];
        _projectionMatrix = new Matrix4f[lodCount];

        for (int i = 0; i < lodCount; i++)
        {
            _renderData[i] = new RenderData();
            _renderDataSource[i] = new RenderData();
            _BindData_paramIdPosScales[i] = new Vector4f();
            _BindData_paramIdOceans[i] = new Vector4f();

            _worldToCameraMatrix[i] = new Matrix4f();
            _projectionMatrix[i] = new Matrix4f();
        }
    }

    public void updateTransforms(int lodDataResolution, Wave_CDClipmap waveClipmap, ReadableVector3f eyePos, float seaLevel)
    {
        Vector3f center = new Vector3f();
        for (int lodIdx = 0; lodIdx < LodCount(); lodIdx++)
        {
            _renderDataSource[lodIdx].set(_renderData[lodIdx]);
            float lodScale = waveClipmap.calcLodScale(lodIdx);
            float camOrthSize = 2f * lodScale;

            // find snap period
            _renderData[lodIdx]._textureRes = lodDataResolution;
            _renderData[lodIdx]._texelWidth = 2f * camOrthSize / _renderData[lodIdx]._textureRes;

            // snap so that shape texels are stationary
            _renderData[lodIdx]._posSnapped.x = eyePos.getX() - Numeric.fmod(eyePos.getX(), _renderData[lodIdx]._texelWidth);
            _renderData[lodIdx]._posSnapped.y = seaLevel;
            _renderData[lodIdx]._posSnapped.z = eyePos.getZ() - Numeric.fmod(eyePos.getZ(), _renderData[lodIdx]._texelWidth);


            // detect first update and populate the render data if so - otherwise it can give divide by 0s and other nastiness
            if (_renderDataSource[lodIdx]._textureRes == 0f)
            {
                _renderDataSource[lodIdx]._posSnapped.set(_renderData[lodIdx]._posSnapped);
                _renderDataSource[lodIdx]._texelWidth = _renderData[lodIdx]._texelWidth;
                _renderDataSource[lodIdx]._textureRes = _renderData[lodIdx]._textureRes;
            }

            ReadableVector3f cameraPos = _renderData[lodIdx]._posSnapped;
            Vector3f.add(cameraPos, Vector3f.Y_AXIS, center);
            Matrix4f.lookAt(cameraPos, center, Vector3f.Z_AXIS, _worldToCameraMatrix[lodIdx]);   // todo look down
//            _worldToCameraMatrix[lodIdx].scale(1,1,-1);
            Matrix4f.ortho(-2f * lodScale, 2f * lodScale, -2f * lodScale, 2f * lodScale, -1000, 1000, _projectionMatrix[lodIdx]);
        }

        for (int lodIdx = 0; lodIdx < LodCount(); lodIdx++)
        {
            // NOTE: gets zeroed by unity, see https://www.alanzucconi.com/2016/10/24/arrays-shaders-unity-5-4/
            _BindData_paramIdPosScales[lodIdx].set(
                    _renderData[lodIdx]._posSnapped.x, _renderData[lodIdx]._posSnapped.z,
                    waveClipmap.calcLodScale(lodIdx), 0f);
            _BindData_paramIdOceans[lodIdx].set(_renderData[lodIdx]._texelWidth, _renderData[lodIdx]._textureRes, 1f, 1f / _renderData[lodIdx]._textureRes);
        }
    }

    /*public void bindData(Wave_Simulation_ShaderData shaderData, boolean sourceLod){
        if(sourceLod){
            shaderData._LD_Pos_Scale_Source = _BindData_paramIdPosScales;
            shaderData._LD_Params_Source = _BindData_paramIdOceans;
        }else{
            shaderData._LD_Pos_Scale = _BindData_paramIdPosScales;
            shaderData._LD_Params = _BindData_paramIdOceans;
        }
    }*/

    public Vector4f[] getPosScales(){ return _BindData_paramIdPosScales;}

    public float MaxWavelength(int lodIdx)
    {
        float oceanBaseScale = m_Clipmap.getScale();
        float maxDiameter = (float) (4f * oceanBaseScale * Math.pow(2f, lodIdx));
        float maxTexelSize = maxDiameter / m_Clipmap.getLodDataResolution();
        return 2f * maxTexelSize * /*OceanRenderer.Instance.MinTexelsPerWave*/m_Clipmap.getMinTexelsPerWave();
    }



}
