package com.nvidia.developer.opengl.ui;

/**
 * A UI element that 'proxies' calls to another element. This is used to create
 * further specialized subclasses of NvUIElement that have an existing type of
 * widget they want to use for their visual and iteractive methods, but can't
 * subclass as it could be ANY class and isn't known up front which ones would
 * be used.
 * 
 * @author Nvidia 2014-9-4
 * 
 */
public class NvUIProxy extends NvUIElement {

	private NvUIElement m_proxy;
	
	public NvUIProxy(NvUIElement m_proxy) {
		this.m_proxy = m_proxy;
	}
	
	@Override
	public void dispose() {
		m_proxy.dispose();
	}

	@Override
	public void draw(NvUIDrawState drawState) {
		m_proxy.draw(drawState);
	}

	public int handleEvent(NvGestureEvent ev, long timeUST,
			NvUIElement hasInteract) {
		return m_proxy.handleEvent(ev, timeUST, hasInteract);
	}

	public int handleReaction(NvUIReaction react) {
		return m_proxy.handleReaction(react);
	}

	public int handleFocusEvent(int evt) {
		return m_proxy.handleFocusEvent(evt);
	}

	public void setOrigin(float x, float y) {
		m_proxy.setOrigin(x, y);
	}

	public void setDimensions(float w, float h) {
		m_proxy.setDimensions(w, h);
	}

	public void setDepth(float z) {
		m_proxy.setDepth(z);
	}

	public boolean hasDepth() {
		return m_proxy.hasDepth();
	}

	public void getScreenRect(NvUIRect rect) {
		m_proxy.getScreenRect(rect);
	}
	
	@Override
	public NvUIRect getScreenRect() {
		return m_proxy.getScreenRect();
	}

	public void getFocusRect(NvUIRect rect) {
		m_proxy.getFocusRect(rect);
	}

	public float getWidth() {
		return m_proxy.getWidth();
	}

	public float getHeight() {
		return m_proxy.getHeight();
	}

	public void setVisibility(boolean show) {
		m_proxy.setVisibility(show);
	}

	public void setAlpha(float a) {
		m_proxy.setAlpha(a);
	}

	public void lostInteract() {
		m_proxy.lostInteract();
	}

	public void dropFocus() {
		m_proxy.dropFocus();
	}

	public int getSlideInteractGroup() {
		return m_proxy.getSlideInteractGroup();
	}

	public int getUID() {
		return m_proxy.getUID();
	}

	public boolean hit(float x, float y) {
		return m_proxy.hit(x, y);
	}
}
