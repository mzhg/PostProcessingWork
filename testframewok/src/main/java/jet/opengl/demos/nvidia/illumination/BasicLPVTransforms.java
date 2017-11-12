package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by Administrator on 2017/11/12 0012.
 */

class BasicLPVTransforms {
    //lpv transform matrices
    private final Matrix4f m_worldToLPVBB = new Matrix4f(); //matrix to transform a unit cube centered at world center to the world region occupied by the LPV
    private final Matrix4f m_worldToLPVNormTex = new Matrix4f(); //matrix to transform from world to LPV normalized texture space ( which is 0 to 1 in all 3 dimensions)
    private final Matrix4f m_worldToLPVNormTexRender = new Matrix4f(); //matrix to transform from world to LPV normalized texture space without spatial snapping( real cascade position )

    private final Matrix4f m_inverseLPVXform = new Matrix4f();
    private final Matrix4f m_objTranslate = new Matrix4f();
    private final Vector3f m_cellSize=new Vector3f();

    public Matrix4f getWorldToLPVNormTex() {    return m_worldToLPVNormTex; }
    public Matrix4f getWorldToLPVNormTexRender() {    return m_worldToLPVNormTexRender; }
    public Matrix4f getWorldToLPVBB() {    return m_worldToLPVBB; }
    public Vector3f getCellSize() { return m_cellSize; }
    public float getCellSizeVolume() { return m_cellSize.x*m_cellSize.y*m_cellSize.z; }


    public void getLPVLightViewMatrices(Matrix4f ViewMatrix, Matrix4f viewToLPVMatrix, Matrix4f inverseViewToLPV)
    {
        /*D3DXMATRIX inverseViewMatrix;
        //matrix to transform from the light view to the LPV space
        D3DXMatrixInverse( &inverseViewMatrix, NULL, &ViewMatrix );
        D3DXMatrixMultiply( viewToLPVMatrix,&inverseViewMatrix, &m_worldToLPVNormTex);
        D3DXMatrixInverse( inverseViewToLPV, NULL, viewToLPVMatrix );*/
        Matrix4f inverseViewMatrix = Matrix4f.invert(ViewMatrix, null);
        Matrix4f.mul(m_worldToLPVNormTex, inverseViewMatrix, viewToLPVMatrix);
        Matrix4f.invert(viewToLPVMatrix, inverseViewToLPV);
    }

    public Matrix4f getViewToLPVMatrixGV(Matrix4f ViewMatrix)
    {
        Matrix4f viewToLPVMatrix = new Matrix4f();
        Matrix4f inverseViewMatrix = new Matrix4f();

        //matrix to transform from the light view to the LPV space
        /*D3DXMatrixInverse( &inverseViewMatrix, NULL, &ViewMatrix );
        D3DXMatrixMultiply( &viewToLPVMatrix,&inverseViewMatrix, &m_worldToLPVNormTex);*/
        Matrix4f.invert(ViewMatrix, inverseViewMatrix);
        Matrix4f.mul(m_worldToLPVNormTex, inverseViewMatrix, viewToLPVMatrix);

        //matrix to transform from the light view to the GV space
        //center the grid and then shift the GV half a cell so that GV cell centers line up with LPV cell vertices
        /*D3DXMATRIX ViewToLPVMatrixGV;
        D3DXMatrixMultiply(&ViewToLPVMatrixGV, &viewToLPVMatrix, &m_objTranslate);
        return ViewToLPVMatrixGV;*/
        return Matrix4f.mul(m_objTranslate, viewToLPVMatrix, viewToLPVMatrix);
    }


    public void setLPVTransformsRotatedAndOffset(float LPVscale, ReadableVector3f LPVtranslate, Matrix4f cameraViewMatrix, ReadableVector3f viewVector,
                                                 int width3D, int height3D, int depth3D)
    {
        /*D3DXMATRIX scale;
        *//*
        // animating the view vector over time for debugging
        static float angle = 0;
        viewVector = D3DXVECTOR3(sin(angle),0, -cos(angle));
        D3DXVec3Normalize(&viewVector,&viewVector);
        angle += 0.0001745; //0.01 degrees
        *//*
        D3DXMatrixScaling(&scale,LPVscale,LPVscale,LPVscale);
        //scale the LPV by the total amount it needs to be scaled
        m_worldToLPVBB = scale;*/
        m_worldToLPVBB.setIdentity();
        m_worldToLPVBB.m30 = m_worldToLPVBB.m31 = m_worldToLPVBB.m32 = LPVscale;

        //construct a translation matrix to translate the LPV along the view vector (so that 80% of the LPV is infront of the camera)
        float offsetScale = 0.8f;
        /*D3DXVECTOR3 originalOffset = D3DXVECTOR3(offsetScale*0.5f*viewVector.x, offsetScale*0.5f*viewVector.y, offsetScale*0.5f*viewVector.z);
        D3DXVECTOR4 transformedOffset;
        D3DXVec3Transform(&transformedOffset,&originalOffset,&m_worldToLPVBB);*/
        Vector3f transformedOffset = Vector3f.scale(viewVector, offsetScale*0.5f*LPVscale, null);

        //the total translation is the translation amount for the grid center + the translation amount for shifting the grid along the view vector
        //in addition, we also SNAP the translation to be a multiple of the grid cell size (we only translate in full cell sizes to avoid flickering)
        /*m_cellSize.set(LPVscale/width3D, LPVscale/height3D, LPVscale/depth3D );
        D3DXMATRIXA16 modifiedTranslate;
        D3DXMatrixTranslation(&modifiedTranslate, floorf(( LPVtranslate.x+transformedOffset.x)/m_cellSize.x)*m_cellSize.x,
                floorf(( LPVtranslate.y+transformedOffset.y)/m_cellSize.y)*m_cellSize.y,
                floorf(( LPVtranslate.z+transformedOffset.z)/m_cellSize.z)*m_cellSize.z );
        //further transform the LPV by applying the offset calculated and then translating it to the final position
        m_worldToLPVBB = m_worldToLPVBB * modifiedTranslate;*/
        m_worldToLPVBB.m30 = (float)Math.floor(( LPVtranslate.getX()+transformedOffset.x)/m_cellSize.x)*m_cellSize.x;  // TODO
        m_worldToLPVBB.m31 = (float)Math.floor(( LPVtranslate.getY()+transformedOffset.y)/m_cellSize.y)*m_cellSize.y;
        m_worldToLPVBB.m32 = (float)Math.floor(( LPVtranslate.getZ()+transformedOffset.z)/m_cellSize.z)*m_cellSize.z;

        /*D3DXMATRIXA16 translate;
        D3DXMatrixInverse( &m_inverseLPVXform, NULL, &m_worldToLPVBB );
        D3DXMatrixTranslation(&translate, 0.5f, 0.5f, 0.5f);
        D3DXMatrixMultiply(&m_worldToLPVNormTex, &m_inverseLPVXform, &translate);*/
        Matrix4f.invert(m_worldToLPVBB, m_inverseLPVXform);
        m_worldToLPVNormTex.setTranslate(0.5f, 0.5f, 0.5f);
        Matrix4f.mul(m_worldToLPVNormTex, m_inverseLPVXform, m_worldToLPVNormTex);

        {
            /*D3DXMatrixTranslation(&modifiedTranslate,    ( LPVtranslate.x+transformedOffset.x) - floorf(( LPVtranslate.x+transformedOffset.x)/m_cellSize.x)*m_cellSize.x,
                    ( LPVtranslate.y+transformedOffset.y) - floorf(( LPVtranslate.y+transformedOffset.y)/m_cellSize.y)*m_cellSize.y,
                    ( LPVtranslate.z+transformedOffset.z) - floorf(( LPVtranslate.z+transformedOffset.z)/m_cellSize.z)*m_cellSize.z );
            //further transform the LPV by applying the offset calculated and then translating it to the final position
            D3DXMATRIXA16 worldToLPVBB = m_worldToLPVBB * modifiedTranslate;
            D3DXMATRIXA16 inverseLPVXform;
            D3DXMatrixInverse( &inverseLPVXform, NULL, &worldToLPVBB );
            D3DXMatrixTranslation(&translate, 0.5f, 0.5f, 0.5f);
            D3DXMatrixMultiply(&m_worldToLPVNormTexRender, &inverseLPVXform, &translate);*/
            Matrix4f worldToLPVBB = new Matrix4f();
            worldToLPVBB.setTranslate(( LPVtranslate.getX()+transformedOffset.x) - (float)Math.floor(( LPVtranslate.getX()+transformedOffset.x)/m_cellSize.x)*m_cellSize.x,
                    ( LPVtranslate.getY()+transformedOffset.y) - (float)Math.floor(( LPVtranslate.getY()+transformedOffset.y)/m_cellSize.y)*m_cellSize.y,
                    ( LPVtranslate.getZ()+transformedOffset.z) - (float)Math.floor(( LPVtranslate.getZ()+transformedOffset.z)/m_cellSize.z)*m_cellSize.z);
            Matrix4f.mul(worldToLPVBB, m_worldToLPVBB, worldToLPVBB);
            Matrix4f inverseLPVXform = Matrix4f.invert(worldToLPVBB, worldToLPVBB);
            m_worldToLPVNormTexRender.setTranslate(0.5f, 0.5f, 0.5f);
            Matrix4f.mul(m_worldToLPVNormTexRender, inverseLPVXform, m_worldToLPVNormTexRender);
        }


//        D3DXMatrixTranslation(&m_objTranslate,0.5f/width3D,0.5f/height3D,0.5f/depth3D);
        m_objTranslate.setTranslate(0.5f/width3D,0.5f/height3D,0.5f/depth3D);
    };
}
