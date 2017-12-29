/* 
* Copyright (c) 2008-2016, NVIDIA CORPORATION. All rights reserved. 
* 
* NVIDIA CORPORATION and its licensors retain all intellectual property 
* and proprietary rights in and to this software, related documentation 
* and any modifications thereto. Any use, reproduction, disclosure or 
* distribution of this software and related documentation without an express 
* license agreement from NVIDIA CORPORATION is strictly prohibited. 
*/
package jet.opengl.demos.nvidia.hbaoplus;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

final class States {

	//--------------------------------------------------------------------------------
	static void setSharedBlendState()
	{
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		gl.glDisable(GLenum.GL_SAMPLE_COVERAGE);
		gl.glDisable(GLenum.GL_SAMPLE_ALPHA_TO_COVERAGE);
	}

	//--------------------------------------------------------------------------------
	static void setBlendStateMultiplyPreserveAlpha()
	{
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		gl.glColorMask(true, true, true, true);
		gl.glEnable(GLenum.GL_BLEND);
		gl.glBlendEquationSeparate(GLenum.GL_FUNC_ADD, GLenum.GL_FUNC_ADD);
		gl.glBlendFuncSeparate(GLenum.GL_ZERO, GLenum.GL_SRC_COLOR, GLenum.GL_ZERO, GLenum.GL_ONE);
//	    ASSERT_GL_ERROR(GL);
	}

	//--------------------------------------------------------------------------------
	static void setBlendStateDisabledPreserveAlpha()
	{
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		gl.glColorMask( true, true, true, true);
		gl.glDisable(GLenum.GL_BLEND);
//	    ASSERT_GL_ERROR(GL);
	}

	//--------------------------------------------------------------------------------
	static void setBlendStateDisabled()
	{
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		gl.glColorMask(true, true, true, true);
		gl.glDisable(GLenum.GL_BLEND);
//	    ASSERT_GL_ERROR(GL);
	}

	//--------------------------------------------------------------------------------
	static void setDepthStencilStateDisabled()
	{
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		gl.glDisable(GLenum.GL_DEPTH_TEST);
		gl.glDisable(GLenum.GL_STENCIL_TEST);
	}

	//--------------------------------------------------------------------------------
	static void setRasterizerStateFullscreenNoScissor()
	{
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		gl.glDisable(GLenum.GL_CULL_FACE);
		gl.glPolygonOffset(0.f, 0.f);
		gl.glDisable(GLenum.GL_SCISSOR_TEST);
		gl.glDisable(GLenum.GL_MULTISAMPLE);
	}

	//--------------------------------------------------------------------------------
	static void setCustomBlendState(GFSDK_SSAO_CustomBlendState_GL CustomBlendState)
	{
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		gl.glEnable(GLenum.GL_BLEND);

		gl.glBlendEquationSeparate(
	        CustomBlendState.modeRGB,
	        CustomBlendState.modeAlpha);

		gl.glBlendFuncSeparate(
	        CustomBlendState.srcRGB,
	        CustomBlendState.dstRGB,
	        CustomBlendState.srcAlpha,
	        CustomBlendState.dstAlpha);

		gl.glBlendColor(
	        CustomBlendState.blendColorR,
	        CustomBlendState.blendColorG,
	        CustomBlendState.blendColorB,
	        CustomBlendState.blendColorA);

		gl.glColorMask(
	        CustomBlendState.colorMaskR,
	        CustomBlendState.colorMaskG,
	        CustomBlendState.colorMaskB,
	        CustomBlendState.colorMaskA);

//	    ASSERT_GL_ERROR(GL);
	}
}
