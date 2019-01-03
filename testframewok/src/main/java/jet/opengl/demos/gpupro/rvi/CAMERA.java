package jet.opengl.demos.gpupro.rvi;

import com.nvidia.developer.opengl.app.NvInputTransformer;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

final class CAMERA implements ICONST, Readable {
    static final int SIZE = Matrix4f.SIZE* 4+Vector4f.SIZE*6;

    // data for camera uniform-buffer
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f invTransposeViewMatrix = new Matrix4f();
    private final Matrix4f projMatrix = new Matrix4f();
    private final Matrix4f viewProjMatrix = new Matrix4f();
    private final Vector4f[] frustumRays = new Vector4f[4];
    private final Vector3f position = new Vector3f();
    private float nearClipDistance;
    private float farClipDistance;
    private float nearFarClipDistance;

    private final Vector3f rotation = new Vector3f();
    private final Vector3f direction = new Vector3f();
    private float fovy;
    private float halfFarWidth,halfFarHeight;
    private float aspectRatio;
    private final Matrix4f invProjMatrix = new Matrix4f();
    private BufferGL uniformBuffer;

    boolean Init(float fovy,float nearClipDistance,float farClipDistance){
        this.fovy = fovy;
        this.nearClipDistance = nearClipDistance;
        if(nearClipDistance==0.0f)
            return false;
        this.farClipDistance = farClipDistance;
        nearFarClipDistance = farClipDistance-nearClipDistance;
        aspectRatio = (float)SCREEN_WIDTH/(float)SCREEN_HEIGHT;
        halfFarHeight = (float) (Math.tan(fovy*Numeric.PI /360)*farClipDistance);
        halfFarWidth =  halfFarHeight*aspectRatio;
//        projMatrix.SetPerspective(fovy,aspectRatio,nearClipDistance,farClipDistance);
        Matrix4f.perspective(fovy,aspectRatio,nearClipDistance,farClipDistance, projMatrix);
//        invProjMatrix = projMatrix.GetInverse();
        Matrix4f.invert(projMatrix, invProjMatrix);

        /*UNIFORM_LIST uniformList;
        uniformList.AddElement("viewMatrix",MAT4_DT);
        uniformList.AddElement("invTransposeViewMatrix",MAT4_DT);
        uniformList.AddElement("projMatrix",MAT4_DT);
        uniformList.AddElement("viewProjMatrix",MAT4_DT);
        uniformList.AddElement("frustumRays",VEC4_DT,4);
        uniformList.AddElement("position",VEC3_DT);
        uniformList.AddElement("nearClipDistance",FLOAT_DT);
        uniformList.AddElement("farClipDistance",FLOAT_DT);
        uniformList.AddElement("nearFarClipDistance",FLOAT_DT);
        uniformBuffer = DEMO::renderer->CreateUniformBuffer(CAMERA_UB_BP,uniformList);
        if(!uniformBuffer)
            return false;*/
        uniformBuffer = new BufferGL();
        uniformBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, SIZE, null, GLenum.GL_STREAM_READ);

        UpdateUniformBuffer();

        return true;
    }

    void Update(NvInputTransformer transformer){
        transformer.getModelViewMat(viewMatrix);
        Matrix4f.decompseRigidMatrix(viewMatrix, position, null, null, direction);
        direction.scale(-1);

        Matrix4f.mul(projMatrix, viewMatrix, viewProjMatrix);
        Matrix4f.invert(viewProjMatrix, invTransposeViewMatrix);

        // left/ lower corner
        frustumRays[0].set(-1, -1, 1, 1);
        Matrix4f.transformCoord(invTransposeViewMatrix, frustumRays[0], frustumRays[0]);

        // right/ lower corner
        frustumRays[1].set(1, -1, 1, 1);
        Matrix4f.transformCoord(invTransposeViewMatrix, frustumRays[1], frustumRays[1]);

        // left/ upper corner
        frustumRays[2].set(-1, 1, 1, 1);
        Matrix4f.transformCoord(invTransposeViewMatrix, frustumRays[2], frustumRays[2]);

        // right/ upper corner
        frustumRays[3].set(1, 1, 1, 1);
        Matrix4f.transformCoord(invTransposeViewMatrix, frustumRays[3], frustumRays[3]);

        for(int i = 0; i < frustumRays.length; i++){
            Vector3f.sub(frustumRays[i], position, frustumRays[i]);
        }

        UpdateUniformBuffer();
    }

    void Update(Vector3f position,Vector3f rotation){
        /*this.position.set(position);
        this.rotation.set(rotation);
        MATRIX4X4 xRotMatrix,yRotMatrix,zRotMatrix,transMatrix,rotMatrix;
        xRotMatrix.SetRotation(VECTOR3D(0.0f,1.0f,0.0f),-rotation.x);
        yRotMatrix.SetRotation(VECTOR3D(1.0f,0.0f,0.0f),rotation.y);
        zRotMatrix.SetRotation(VECTOR3D(0.0f,0.0f,1.0f),rotation.z);
        transMatrix.SetTranslation(-position);
        rotMatrix = zRotMatrix*yRotMatrix*xRotMatrix;
        viewMatrix = rotMatrix*transMatrix;
        viewProjMatrix = projMatrix*viewMatrix;
        invTransposeViewMatrix = viewMatrix.GetInverseTranspose();

        direction.Set(-viewMatrix.entries[2],-viewMatrix.entries[6],-viewMatrix.entries[10]);
        direction.Normalize();
        VECTOR3D up(viewMatrix.entries[1],viewMatrix.entries[5],viewMatrix.entries[9]);
        up.Normalize();
        VECTOR3D right(viewMatrix.entries[0],viewMatrix.entries[4],viewMatrix.entries[8]);
        right.Normalize();

        VECTOR3D farDir = direction*farClipDistance;
        VECTOR3D tmp;

        // left/ lower corner
        tmp = farDir-(up*halfFarHeight)-(right*halfFarWidth);
        frustumRays[0].Set(tmp);

        // right/ lower corner
        tmp = farDir-(up*halfFarHeight)+(right*halfFarWidth);
        frustumRays[1].Set(tmp);

        // left/ upper corner
        tmp = farDir+(up*halfFarHeight)-(right*halfFarWidth);
        frustumRays[2].Set(tmp);

        // right/ upper corner
        tmp = farDir+(up*halfFarHeight)+(right*halfFarWidth);
        frustumRays[3].Set(tmp);

        UpdateUniformBuffer();*/
    }

    BufferGL GetUniformBuffer()
    {
        return uniformBuffer;
    }

    Matrix4f GetViewMatrix()
    {
        return viewMatrix;
    }

    Matrix4f GetInvTransposeViewMatrix()
    {
        return invTransposeViewMatrix;
    }

    Matrix4f GetProjMatrix()
    {
        return projMatrix;
    }

    Matrix4f GetInvProjMatrix()
    {
        return invProjMatrix;
    }

    Matrix4f GetViewProjMatrix()
    {
        return viewProjMatrix;
    }

    Vector3f GetPosition()
    {
        return position;
    }

    Vector3f GetRotation()
    {
        return rotation;
    }

    Vector3f GetDirection()
    {
        return direction;
    }

    float GetFovy()
    {
        return fovy;
    }

    float GetAspectRatio()
    {
        return aspectRatio;
    }

    float GetNearClipDistance()
    {
        return nearClipDistance;
    }

    float GetFarClipDistance()
    {
        return farClipDistance;
    }

    float GetNearFarClipDistance()
    {
        return nearFarClipDistance;
    }

    void UpdateUniformBuffer()
    {
        /*float *uniformBufferData = viewMatrix;
        uniformBuffer->Update(uniformBufferData);*/

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(SIZE);
        store(buffer);
        buffer.flip();

        uniformBuffer.update(0, buffer);
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        viewMatrix.store(buf);
        invTransposeViewMatrix.store(buf);
        projMatrix.store(buf);
        viewProjMatrix.store(buf);
        for(int i = 0; i <  frustumRays.length; i++){
            frustumRays[i].store(buf);
        }

        position.store(buf);
        buf.putFloat(nearClipDistance);
        buf.putFloat(farClipDistance);
        buf.putFloat(nearFarClipDistance);
        buf.putFloat(0);
        buf.putFloat(0);
        return buf;
    }
}
