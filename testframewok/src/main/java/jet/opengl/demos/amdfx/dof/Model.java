package jet.opengl.demos.amdfx.dof;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/6/24.
 */

final class Model {
    final Matrix4f m_World = new Matrix4f();
    final Matrix4f m_World_Inv = new Matrix4f();
    final Matrix4f m_WorldView = new Matrix4f();
    final Matrix4f m_WorldView_Inv = new Matrix4f();
    final Matrix4f m_WorldViewProjection = new Matrix4f();
    final Matrix4f m_WorldViewProjection_Inv = new Matrix4f();
    final Vector4f m_Position = new Vector4f();
    final Vector4f m_Orientation = new Vector4f();
    final Vector4f m_Scale = new Vector4f();
    final Vector4f m_Ambient = new Vector4f();
    final Vector4f m_Diffuse = new Vector4f();
    final Vector4f m_Specular = new Vector4f();


}
