package com.nvidia.developer.opengl.demos.amdfx.common;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class AMD_Camera {
	final Matrix4f                     m_View = new Matrix4f();
	final Matrix4f                     m_Projection = new Matrix4f();
	final Matrix4f                     m_ViewProjection = new Matrix4f();
	final Matrix4f                     m_View_Inv = new Matrix4f();
	final Matrix4f                     m_Projection_Inv = new Matrix4f();
	final Matrix4f                     m_ViewProjection_Inv = new Matrix4f();
    final Vector3f                     m_Position = new Vector3f();
    float                              m_Fov;
    final Vector3f                     m_Direction = new Vector3f();
    float                              m_FarPlane;
    final Vector3f                     m_Right = new Vector3f();
    float                              m_NearPlane;
    final Vector3f                     m_Up = new Vector3f();
    float                              m_Aspect;
    final Vector4f                     m_Color = new Vector4f();
    
    public AMD_Camera() {}
	
	public AMD_Camera(AMD_Camera o) {
		set(o);
	}
	
	public void set(AMD_Camera o){
		m_View.load(o.m_View);
		m_Projection.load(o.m_Projection);
		m_ViewProjection.load(o.m_ViewProjection);
		m_View_Inv.load(o.m_View_Inv);
		m_Projection_Inv.load(o.m_Projection_Inv);
		m_ViewProjection_Inv.load(o.m_ViewProjection_Inv);
		m_Position.set(o.m_Position);
		m_Fov = o.m_Fov;
		m_Direction.set(o.m_Direction);
		m_FarPlane = o.m_FarPlane;
		m_Right.set(o.m_Right);
		m_NearPlane = o.m_NearPlane;
		m_Up.set(o.m_Up);
		m_Aspect = o.m_Aspect;
		m_Color.set(o.m_Color);

	}
}
