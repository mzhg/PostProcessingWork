//----------------------------------------------------------------------------------
// File:        NvAppBase/NvInputHandler_CameraFly.h
// SDK Version: v3.00 
// Email:       gameworks@nvidia.com
// Site:        http://developer.nvidia.com/
//
// Copyright (c) 2014-2015, NVIDIA CORPORATION. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//  * Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//  * Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//  * Neither the name of NVIDIA CORPORATION nor the names of its
//    contributors may be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
// EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
// OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
//----------------------------------------------------------------------------------
package com.nvidia.developer.opengl.app;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Maps touch, mouse and gamepad into useful state for
 * first person-ish fly camera (camera-relative pitch and 
 * translation, world yaw)
 */
public class NvInputHandler_CameraFly extends NvInputHandler{

	// Current state of camera transform
    protected final Vector3f m_currentPos = new Vector3f();
    protected float m_currentPitch;
    protected float m_currentYaw;
    
    private final Vector3f m_currentLookAt = new Vector3f();

    // Current matrices
    protected final Matrix4f m_currentCameraMat = new Matrix4f();
    protected final Matrix4f m_currentViewMat = new Matrix4f();

    // Number of touch points currently active
    protected int m_touchPointsCount;

    // Position of the last input
    protected final Vector2f m_lastInput = new Vector2f();
    // Distance separating the last touch points if multitouch is active
    protected float m_lastMultitouchDist;

    // Mouse
    //      Mouse Input Scaling
    protected float m_rotSpeed_Mouse = 0.001f;
    protected float m_transSpeed_Mouse = 0.01f;

    //      Current Mouse Input Values
    protected float m_xRotDelta_Mouse;
    protected float m_yRotDelta_Mouse;
    protected float m_zRotDelta_Mouse;

    protected float m_xTransDelta_Mouse;
    protected float m_yTransDelta_Mouse;
    protected float m_zTransDelta_Mouse;

    // Keyboard
    //      Keyboard Input Scaling
    protected float m_rotSpeed_KB = 0.5f;
    protected float m_transSpeed_KB = 1.0f;
    protected boolean  m_accelerate_KB;

    //      Current Keyboard Input Values
    // Keep separate values for positive and negative movement/rotation via keyboard
    // to handle missed keyboard events (keep them from getting stuck, fix state by
    // repeatedly hitting all keys, etc.
    protected byte m_xPlusRotVel_KB;
    protected byte m_yPlusRotVel_KB;
    protected byte m_zPlusRotVel_KB;
    protected byte m_xNegRotVel_KB;
    protected byte m_yNegRotVel_KB;
    protected byte m_zNegRotVel_KB;

    protected byte m_yPlusTransVel_KB;
    protected byte m_xPlusTransVel_KB;
    protected byte m_zPlusTransVel_KB;
    protected byte m_yNegTransVel_KB;
    protected byte m_xNegTransVel_KB;
    protected byte m_zNegTransVel_KB;

    // Gamepad
    protected byte m_lastActiveGamepad;

    //      Gamepad Input Scaling
    protected float m_rotSpeed_GP = 1.0f;
    protected float m_transSpeed_GP = 1.0f;
    protected boolean  m_accelerate_GP;

    //      Current Gamepad Input Values
    protected float m_xRotVel_GP;
    protected float m_yRotVel_GP;
    protected float m_zRotVel_GP;

    protected float m_xTransVel_GP;
    protected float m_yTransVel_GP;
    protected float m_zTransVel_GP;
    
    /**
     * Sets the current position of the camera in world space
     * @param pos The world position at which to place the camera
     */
    public void setPosition(ReadableVector3f pos) { m_currentPos.set(pos); updateMatrices(); }

    /// 
    /// \return 
    /**
     * Retrieves the current position of the camera in world space
     * @return The current world-space position of the camera
     */
    public ReadableVector3f getPosition() { return m_currentPos; }

    /**
     * Sets the current pitch of the camera
     * @param pitch New pitch of the camera, in radians
     */
    public void setPitch(float pitch) { m_currentPitch = pitch; updateMatrices(); }

    /**
     * Retrieves the current pitch of the camera
     * @return The current pitch of the camera, in radians
     */
    public float getPitch() { return m_currentPitch; }

    /**
     * Sets the current yaw of the camera
     * @param yaw pitch New yaw of the camera, in radians
     */
    public void setYaw(float yaw) { m_currentYaw = yaw; updateMatrices(); }

    /**
     * Retrieves the current yaw of the camera
     * @return The current yaw of the camera, in radians
     */
    public float getYaw() { return m_currentYaw; }

    /**
     * Returns the current look-at vector of the camera
     * @return the look-at vector
     */
    public ReadableVector3f getLookVector() { 
//    	nv::vec4f look = -m_currentCameraMat.get_column(2); return nv::vec3f(look.x, look.y, look.z); 
    	Matrix4f.decompseRigidMatrix(m_currentViewMat, null, null, null, m_currentLookAt);
    	m_currentLookAt.scale(-1);
    	return m_currentLookAt;
    }

    /**
     * Retrieves the current world matrix of the camera's transform
     * @return A matrix containing a representation of the camera's current position and orientation in world space
     */
    public Matrix4f getCameraMatrix() { return m_currentCameraMat; }

    /**
     * Retrieves the current view matrix from the camera
     * @return A matrix containing a representation of the transform required to transform a position from world space into camera view space
     */
    public Matrix4f getViewMatrix() { return m_currentViewMat; }

    /**
     * Sets the speed of rotation that maps to mouse movements 
     * @param speed speed Speed of mouse driven rotation, in radians per pixel
     */
    public void setMouseRotationSpeed(float speed) { m_rotSpeed_Mouse = speed; }

    /**
     * Sets the speed of translation that maps to mouse movements
     * @param speed Speed of mouse driven translation, in meters per pixel
     */
    public void setMouseTranslationSpeed(float speed) { m_transSpeed_Mouse = speed; }

    /**
     * Sets the speed of rotation that maps to key presses
     * @param speed Speed of keyboard driven rotation, in radians per second
     */
    public void setKeyboardRotationSpeed(float speed) { m_rotSpeed_KB = speed; }

    /**
     * Sets the speed of translation that maps to key presses
     * @param speed Speed of keyboard driven translation, in meters per second
     */
    public void setKeyboardTranslationSpeed(float speed) { m_transSpeed_KB = speed; }

    /**
     * Sets the speed of rotation that maps to gamepad sticks
     * @param speed Speed of gamepad driven rotation, in radians per second at full deflection
     */
    public void setGamepadRotationSpeed(float speed) { m_rotSpeed_GP = speed; }

    /**
     * Sets the speed of translation that maps to gamepad sticks
     * @param speed Speed of gamepad driven translation, in meters per second at full deflection
     */
    public void setGamepadTranslationSpeed(float speed) { m_transSpeed_GP = speed; }
    
    protected void updateMatrices(){
    	Matrix4f.rotationYawPitchRoll(m_currentYaw, m_currentPitch, 0, m_currentCameraMat);  // TODO
    	m_currentCameraMat.m30 = m_currentPos.x;
    	m_currentCameraMat.m31 = m_currentPos.y;
    	m_currentCameraMat.m32 = m_currentPos.z;
    	
    	// update our current view matrix
    	Matrix4f.invert(m_currentCameraMat, m_currentViewMat);
    }
    
	@Override
	public void reset() {
		m_currentPos.set(0.0f, 0.0f, 0.0f);
	    m_currentPitch = 0.0f;
	    m_currentYaw = 0.0f;
	    m_touchPointsCount = 0;
	    m_lastInput.set(0.0f, 0.0f);
	    m_lastMultitouchDist = 0;
	    m_rotSpeed_Mouse = 0.001f;
	    m_transSpeed_Mouse = 0.01f;
	    m_xRotDelta_Mouse = 0.0f;
	    m_yRotDelta_Mouse = 0.0f;
	    m_zRotDelta_Mouse = 0.0f;
	    m_xTransDelta_Mouse = 0.0f;
	    m_yTransDelta_Mouse = 0.0f;
	    m_zTransDelta_Mouse = 0.0f;
	    m_rotSpeed_KB = 0.5f;
	    m_transSpeed_KB = 1.0f;
	    m_accelerate_KB = false;
	    m_xPlusRotVel_KB = 0;
	    m_yPlusRotVel_KB = 0;
	    m_zPlusRotVel_KB = 0;
	    m_xNegRotVel_KB = 0;
	    m_yNegRotVel_KB = 0;
	    m_zNegRotVel_KB = 0;
	    m_yPlusTransVel_KB = 0;
	    m_xPlusTransVel_KB = 0;
	    m_zPlusTransVel_KB = 0;
	    m_yNegTransVel_KB = 0;
	    m_xNegTransVel_KB = 0;
	    m_zNegTransVel_KB = 0;
	    m_rotSpeed_GP = 1.0f;
	    m_transSpeed_GP = 1.0f;
	    m_accelerate_GP = false;
	    m_xRotVel_GP = 0.0f;
	    m_yRotVel_GP = 0.0f;
	    m_zRotVel_GP = 0.0f;
	    m_xTransVel_GP = 0.0f;
	    m_yTransVel_GP = 0.0f;
	    m_zTransVel_GP = 0.0f;
	}

	@Override
	public void update(float deltaTime) {
		// Calculate the deltas for each access to create our new relative transform
	    float yaw = (m_yRotDelta_Mouse * m_rotSpeed_Mouse) + ((m_yPlusRotVel_KB - m_yNegRotVel_KB) * m_rotSpeed_KB * deltaTime) + (m_yRotVel_GP * m_rotSpeed_GP * deltaTime);
	    float pitch = (m_xRotDelta_Mouse * m_rotSpeed_Mouse) + ((m_xPlusRotVel_KB - m_xNegRotVel_KB) * m_rotSpeed_KB * deltaTime) + (m_xRotVel_GP * m_rotSpeed_GP * deltaTime);

	    float kbAccelerate = m_accelerate_KB? 5.0f : 1.0f;
	    float gpAccelerate = m_accelerate_GP? 5.0f : 1.0f;
	    float xDelta = (m_xTransDelta_Mouse * m_transSpeed_Mouse) + ((m_xPlusTransVel_KB - m_xNegTransVel_KB) * m_transSpeed_KB * kbAccelerate * deltaTime) + (m_xTransVel_GP * m_transSpeed_GP * gpAccelerate * deltaTime);
	    float yDelta = (m_yTransDelta_Mouse * m_transSpeed_Mouse) + ((m_yPlusTransVel_KB - m_yNegTransVel_KB) * m_transSpeed_KB * kbAccelerate * deltaTime) + (m_yTransVel_GP * m_transSpeed_GP * gpAccelerate * deltaTime);
	    float zDelta = (m_zTransDelta_Mouse * m_transSpeed_Mouse) + ((m_zPlusTransVel_KB - m_zNegTransVel_KB) * m_transSpeed_KB * kbAccelerate * deltaTime) + (m_zTransVel_GP * m_transSpeed_GP * gpAccelerate * deltaTime);

	    // Translation provided by input is relative to the camera. 
	    // Convert to world space translation before adding it.
//	    nv::matrix4f currentRotation = m_currentCameraMat;
//	    currentRotation.set_translate(nv::vec3f(0.0f, 0.0f, 0.0f));
	    final Matrix4f currentRotation = m_currentCameraMat;
	    currentRotation.m30=0;
	    currentRotation.m31=0;
	    currentRotation.m32=0;

//	    nv::vec4f worldTranslation = currentRotation * nv::vec4f(xDelta, yDelta, zDelta, 1.0f);
	    float currentPosX = m_currentPos.x;
	    float currentPosY = m_currentPos.y;
	    float currentPosZ = m_currentPos.z;
	    
	    m_currentPos.set(xDelta, yDelta, zDelta);
	    Matrix4f.transformVector(currentRotation, m_currentPos, m_currentPos);

	    m_currentPos.x += currentPosX;
	    m_currentPos.y += currentPosY;
	    m_currentPos.z += currentPosZ;

	    // Update our current yaw and pitch, clamping as needed
	    m_currentYaw += yaw;
	    final float NV_PI = Numeric.PI;
	    float twopi = (NV_PI * 2.0f);
	    while (m_currentYaw > twopi)
	    {
	        m_currentYaw -= twopi;
	    }

	    m_currentPitch += pitch;
	    if (m_currentPitch > NV_PI)
	    {
	        m_currentPitch = NV_PI;
	    }
	    else if (m_currentPitch < -NV_PI)
	    {
	        m_currentPitch = -NV_PI;
	    }

	    // Clear out the accumulated mouse values
	    m_xRotDelta_Mouse = m_yRotDelta_Mouse = m_zRotDelta_Mouse = 0.0f;
	    m_xTransDelta_Mouse = m_yTransDelta_Mouse = m_zTransDelta_Mouse = 0.0f;

	    updateMatrices();
	}
	
	private static final float length(NvPointerEvent p0, NvPointerEvent p1){
		return Vector2f.length(p0.m_x - p1.m_x, p0.m_y - p1.m_y);
	}
	
	private final Vector2f temp2 = new Vector2f();

	@Override
	public boolean processPointer(NvInputDeviceType device, int action, int modifiers, int count, NvPointerEvent[] points) {
		int button = points[0].m_id;
	    boolean needsUpdate = false;
	    final Vector2f deltaMotion = temp2;
	    deltaMotion.set(0.0f, 0.0f);
	    float pinchDelta = 0.0f;

	    switch (action)
	    {
	    case NvPointerActionType.UP:
	    {
	        m_touchPointsCount = 0;
	        break;
	    } 
	    case NvPointerActionType.MOTION:
	    {
	        if (m_touchPointsCount == 1)
	        {
	            // Single point, so update the delta...
	            deltaMotion.x = points[0].m_x - m_lastInput.x;
	            deltaMotion.y = points[0].m_y - m_lastInput.y;

	            // ...and store the last position
	            m_lastInput.set(points[0].m_x, points[0].m_y);
	            needsUpdate = true;
	        }
	        else if ((m_touchPointsCount >= 2) && (count >= 2))
	        { 
	            // >1 points during this 'gesture'
	            // Capture the pinch distance
//	            nv::vec2f multitouchDelta = nv::vec2f(points[0].m_x - points[1].m_x, points[0].m_y - points[1].m_y);
//	            float pinchDist = nv::length(multitouchDelta);
	        	float pinchDist = length(points[0], points[1]);
	            pinchDelta = pinchDist - m_lastMultitouchDist;
	            m_lastMultitouchDist = pinchDist;

	            // Calculate the center of the pinch to get the motion delta
//	            nv::vec2f newCenter = nv::vec2f(points[0].m_x + points[1].m_x, points[0].m_y + points[1].m_y) * 0.5f;
	            float newCenterX = (points[0].m_x + points[1].m_x) * 0.5f;
	            float newCenterY = (points[0].m_y + points[1].m_y) * 0.5f;
	            deltaMotion.x = newCenterX - m_lastInput.x;
	            deltaMotion.y = newCenterY - m_lastInput.y;
	            m_lastInput.set(newCenterX, newCenterY);

	            needsUpdate = true;
	        }
	        break;
	    }
	    case NvPointerActionType.DOWN:
	    {
	        // Beginning single touch gesture
	        m_touchPointsCount = 1;
	        m_lastInput.set(points[0].m_x, points[0].m_y);
	        m_lastMultitouchDist = 0.0f;
	        break;
	    } 
	    case  NvPointerActionType.EXTRA_DOWN:
	    {
	        // Beginning multi touch gesture, track number of touches
	        m_touchPointsCount = count;

	        m_lastInput.set(points[0].m_x + points[1].m_x, points[0].m_y + points[1].m_y);
	        m_lastInput.scale(0.5f);

	        // Cache initial length of pinch, also the current length
//	        nv::vec2f touchDelta = nv::vec2f(points[0].m_x - points[1].m_x, points[0].m_y - points[1].m_y);
//	        m_lastMultitouchDist = nv::length(touchDelta);
	        m_lastMultitouchDist = length(points[0], points[1]);

	        break;
	    }
	    case NvPointerActionType.EXTRA_UP:
	    {
	        if (count == 1)
	        {
	            // We're dropping out of multi-touch gesture, so we need
	            // to reinitialize our gesture start values, otherwise there
	            // could be a jump when the next to last touch is removed
	            m_lastInput.set(points[0].m_x, points[0].m_y);
	            m_lastMultitouchDist = 0.0f;
	        }
	        m_touchPointsCount = count;
	        break;
	    }
	    }

	    if (needsUpdate)
	    {
	        if (m_touchPointsCount == 1)
	        {
	            if (device == NvInputDeviceType.MOUSE)
	            {
	                if ((button & TouchEventListener.BUTTON_LEFT)!=0)
	                {
	                    // Rotating in yaw(mouseX), pitch(mouseY)
	                    m_yRotDelta_Mouse -= deltaMotion.x;
	                    m_xRotDelta_Mouse -= deltaMotion.y;
	                }
	                else if ((button & TouchEventListener.BUTTON_RIGHT)!=0)
	                {
	                    // Moving in x(mouseX), y(mouseY)
	                    m_xTransDelta_Mouse -= deltaMotion.x;
	                    m_yTransDelta_Mouse += deltaMotion.y;
	                }
	                else if ((button & TouchEventListener.BUTTON_MIDDLE)!=0)
	                {
	                    // Moving in x(mouseX), z(mouseY)
	                    m_xTransDelta_Mouse += deltaMotion.x;
	                    m_zTransDelta_Mouse += deltaMotion.y;
	                }
	            }
	            else
	            {
	                // Touch with a single point
	                // Rotating in yaw(touchX), pitch(touchY)
	                m_yRotDelta_Mouse += deltaMotion.x;
	                m_xRotDelta_Mouse += deltaMotion.y;
	            }
	        }
	        else
	        {
	            // Touch with multiple points
	            // Translating in z(pinch)
	            m_zTransDelta_Mouse -= pinchDelta;

	            // Translating in x(touchX), y(touchY)
	            m_xTransDelta_Mouse -= deltaMotion.x;
	            m_yTransDelta_Mouse += deltaMotion.y;
	        }
	    }
	    return true;
	}

	@Override
	public boolean processKey(int code, NvKeyActionType action) {
		// FPS mode uses WASD for motion, so that UI can own arrows for focus control.
	    switch(code) 
	    {
	    case NvKey.K_W:
	    {
	        if (action == NvKeyActionType.UP)
	        {
	            if (m_zNegTransVel_KB > 0)
	            {
	                --m_zNegTransVel_KB;
	            }
	        }
	        else if (action == NvKeyActionType.DOWN)
	        {
	            ++m_zNegTransVel_KB;
	        }
	        return true;
	    }
	    case NvKey.K_S:
	    {
	        if (action == NvKeyActionType.UP)
	        {
	            if (m_zPlusTransVel_KB > 0)
	            {
	                --m_zPlusTransVel_KB;
	            }
	        }
	        else if (action == NvKeyActionType.DOWN)
	        {
	            ++m_zPlusTransVel_KB;
	        }
	        return true;
	    }
	    case NvKey.K_A:
	    {
	        if (action == NvKeyActionType.UP)
	        {
	            if (m_xNegTransVel_KB > 0)
	            {
	                --m_xNegTransVel_KB;
	            }
	        }
	        else if (action == NvKeyActionType.DOWN)
	        {
	            ++m_xNegTransVel_KB;
	        }
	        return true;
	    }
	    case NvKey.K_D:
	    {
	        if (action == NvKeyActionType.UP)
	        {
	            if (m_xPlusTransVel_KB > 0)
	            {
	                --m_xPlusTransVel_KB;
	            }
	        }
	        else if (action == NvKeyActionType.DOWN)
	        {
	            ++m_xPlusTransVel_KB;
	        }
	        return true;
	    }
	    }
	    if ((code & NvKey.K_SHIFT_ANY) != 0)
	    {
	        if (action == NvKeyActionType.UP)
	        {
	            m_accelerate_KB = false;
	        }
	        else if (action == NvKeyActionType.DOWN)
	        {
	            m_accelerate_KB = true;
	        }
	    }

	    return false;
	}

}
