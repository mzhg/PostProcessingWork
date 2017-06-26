package com.nvidia.developer.opengl.demos.amdfx.common;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

public class AMD_Camera implements Readable, Writable{
	public static final int SIZE = 464;

	public final Matrix4f                     m_View = new Matrix4f();
	public final Matrix4f                     m_Projection = new Matrix4f();
	public final Matrix4f                     m_ViewProjection = new Matrix4f();
	public final Matrix4f                     m_View_Inv = new Matrix4f();
	public final Matrix4f                     m_Projection_Inv = new Matrix4f();
	public final Matrix4f                     m_ViewProjection_Inv = new Matrix4f();
	public final Vector3f                     m_Position = new Vector3f();
	public float                              m_Fov;
	public final Vector3f                     m_Direction = new Vector3f();
	public float                              m_FarPlane;
	public final Vector3f                     m_Right = new Vector3f();
	public float                              m_NearPlane;
	public final Vector3f                     m_Up = new Vector3f();
	public float                              m_Aspect;
	public final Vector4f                     m_Color = new Vector4f();
    
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

	@Override
	public ByteBuffer store(ByteBuffer buf) {
		m_View.store(buf);
		m_Projection.store(buf);
		m_ViewProjection.store(buf);
		m_View_Inv.store(buf);
		m_Projection_Inv.store(buf);
		m_ViewProjection_Inv.store(buf);
		m_Position.store(buf);
		buf.putFloat(m_Fov);
		m_Direction.store(buf);
		buf.putFloat(m_FarPlane);
		m_Right.store(buf);
		buf.putFloat(m_NearPlane);
		m_Up.store(buf);
		buf.putFloat(m_Aspect);
		m_Color.store(buf);
		return buf;
	}

	@Override
	public AMD_Camera load(ByteBuffer buf) {
		m_View.load(buf);
		m_Projection.load(buf);
		m_ViewProjection.load(buf);
		m_View_Inv.load(buf);
		m_Projection_Inv.load(buf);
		m_ViewProjection_Inv.load(buf);
		m_Position.load(buf);
		m_Fov = buf.getFloat();
		m_Direction.load(buf);
		m_FarPlane = buf.getFloat();
		m_Right.load(buf);
		m_NearPlane = buf.getFloat();
		m_Up.load(buf);
		m_Aspect = buf.getFloat();
		m_Color.load(buf);
		return this;
	}
}
