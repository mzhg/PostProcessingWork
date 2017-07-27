// Copyright (c) 2011 NVIDIA Corporation. All rights reserved.
//
// TO  THE MAXIMUM  EXTENT PERMITTED  BY APPLICABLE  LAW, THIS SOFTWARE  IS PROVIDED
// *AS IS*  AND NVIDIA AND  ITS SUPPLIERS DISCLAIM  ALL WARRANTIES,  EITHER  EXPRESS
// OR IMPLIED, INCLUDING, BUT NOT LIMITED  TO, NONINFRINGEMENT,IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT SHALL  NVIDIA 
// OR ITS SUPPLIERS BE  LIABLE  FOR  ANY  DIRECT, SPECIAL,  INCIDENTAL,  INDIRECT,  OR  
// CONSEQUENTIAL DAMAGES WHATSOEVER (INCLUDING, WITHOUT LIMITATION,  DAMAGES FOR LOSS 
// OF BUSINESS PROFITS, BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, OR ANY 
// OTHER PECUNIARY LOSS) ARISING OUT OF THE  USE OF OR INABILITY  TO USE THIS SOFTWARE, 
// EVEN IF NVIDIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
//
// Please direct any bugs or questions to SDKFeedback@nvidia.com
package jet.opengl.demos.nvidia.waves.samples;

import java.io.IOException;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

import jet.util.buffer.GLUtil;
import jet.util.opengl.shader.GLSLProgram;
import jet.util.opengl.shader.loader.ShaderLoader;

public class FFT {

	private static final double TWO_PI = 2.0 * Math.PI;
	//Memory access coherency (in threads)
	public static final int COHERENCY_GRANULARITY = 128;
	
	public static final int FFT_DIMENSIONS = 3;
	public static final int FFT_PLAN_SIZE_LIMIT = 1 << 27;
	
	public static final int FFT_FORWARD = -1;
	public static final int FFT_INVERSE = 1;
	
	private static final String SHADER_FILE = "advance/OceanCSDemo/shaders/fft_512x512_c2c.glsl";
	
	private static void radix008A(CSFFT512x512_Plan fft_plan, int uav_dst, int srv_src, int thread_count, int istride){
		// Setup execution configuration
		int grid = thread_count / COHERENCY_GRANULARITY;
		
		fft_plan.setShaderResource(srv_src);
		fft_plan.setUnorderedAccessViews(uav_dst);
		
		if(istride > 1)
			fft_plan.enableCS();
		else
			fft_plan.enableCS2();
		
		GL43.glDispatchCompute(grid, 1, 1);
		
		GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
		
		fft_plan.setShaderResource(0);
		fft_plan.setUnorderedAccessViews(0);
	}
	
	public static void fft_512x512_c2c(CSFFT512x512_Plan fft_plan, int uav_dst, int srv_dst, int srv_src){
		final int thread_count = fft_plan.slices * (512 * 512) / 8;
		int pUAV_Tmp = fft_plan.srv_tmp;  // TODO
		int pSRV_Tmp = fft_plan.srv_tmp;
		
		fft_plan.use();  // TODO
		int istride = 512 * 512 / 8;
//		cs_cbs[0] = fft_plan->pRadix008A_CB[0];
//		pd3dContext->CSSetConstantBuffers(0, 1, &cs_cbs[0]);
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, fft_plan.radix008A_CB[0]);
		radix008A(fft_plan, pUAV_Tmp, srv_src, thread_count, istride);

		istride /= 8;
//		cs_cbs[0] = fft_plan->pRadix008A_CB[1];
//		pd3dContext->CSSetConstantBuffers(0, 1, &cs_cbs[0]);
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, fft_plan.radix008A_CB[1]);
		radix008A(fft_plan, uav_dst, pSRV_Tmp, thread_count, istride);

		istride /= 8;
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, fft_plan.radix008A_CB[2]);
		radix008A(fft_plan, pUAV_Tmp, srv_dst, thread_count, istride);

		istride /= 8;
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, fft_plan.radix008A_CB[3]);
		radix008A(fft_plan, uav_dst, pSRV_Tmp, thread_count, istride);

		istride /= 8;
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, fft_plan.radix008A_CB[4]);
		radix008A(fft_plan, pUAV_Tmp, srv_dst, thread_count, istride);

		istride /= 8;
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, fft_plan.radix008A_CB[5]);
		radix008A(fft_plan, uav_dst, pSRV_Tmp, thread_count, istride);
	}
	
	public static void fft512x512_create_plan(CSFFT512x512_Plan plan, int slices){
		plan.slices = slices;
		
		CharSequence source = null;
		try {
			source = ShaderLoader.loadShaderFile(SHADER_FILE, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		GLSLProgram _program = new GLSLProgram();
		GLSLProgram.ShaderSourceItem item = new GLSLProgram.ShaderSourceItem(source, GL43.GL_COMPUTE_SHADER);
		_program.setSourceFromStrings(new GLSLProgram.ShaderSourceItem[]{item});
		int program = _program.getProgram();
		
		plan.program = program;
		plan.radix008A_CS = GL40.glGetSubroutineIndex(program, GL43.GL_COMPUTE_SHADER, "Radix008A_CS");
		plan.radix008A_CS2 = GL40.glGetSubroutineIndex(program, GL43.GL_COMPUTE_SHADER, "Radix008A_CS2");
		
		plan.thread_count = GL20.glGetUniformLocation(program, "thread_count");
		plan.ostride = GL20.glGetUniformLocation(program, "ostride");
		plan.istride = GL20.glGetUniformLocation(program, "istride");
		plan.pstride = GL20.glGetUniformLocation(program, "pstride");
		plan.phase_base = GL20.glGetUniformLocation(program, "phase_base");
		
		// Constants
		// Create 6 cbuffers for 512x512 transform
		create_cbuffers_512x512(plan, slices);
		
		// Temp buffer
		int tmp_buf = GL15.glGenBuffers();
		GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, tmp_buf);
		GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, 4 * 2 * (512 * slices) * 512, GL15.GL_DYNAMIC_COPY);
		GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
		
		int texture = GL11.glGenTextures();
		GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, texture);
		GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RG32F, tmp_buf);
		GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
		
		plan.srv_tmp = texture;
		plan.buffer_tmp = tmp_buf;
	}
	
	public static void fft512x512_destroy_plan(CSFFT512x512_Plan plan){
		if(plan.srv_tmp != 0) {GL11.glDeleteTextures(plan.srv_tmp); plan.srv_tmp = 0;}
		if(plan.buffer_tmp != 0) {GL15.glDeleteBuffers(plan.buffer_tmp); plan.buffer_tmp = 0;}
		
		if(plan.program != 0) {GL20.glDeleteProgram(plan.program); plan.program = 0;}
		
		for(int i = 0 ; i < 6; i++){
			if(plan.radix008A_CB[i] != 0){
				GL15.glDeleteBuffers(plan.radix008A_CB[i]);
				plan.radix008A_CB[i] = 0;
			}
		}
		
	}
	
	private static void create_cbuffers_512x512(CSFFT512x512_Plan plan, int slices){
		// Create 6 cbuffers for 512x512 transform.
		
		final int target = GL43.GL_SHADER_STORAGE_BUFFER;
		final int useage = GL15.GL_STATIC_READ;
		FloatBuffer buf = GLUtil.getCachedFloatBuffer(5 * 4);
		
		// Buffer 0
		final int thread_count = slices * (512 * 512) / 8;
		int ostride = 512 * 512 / 8;
		int istride = ostride;
		double phase_base = -TWO_PI / (512.0 * 512.0);
		
		buf.put(Float.intBitsToFloat(thread_count));
		buf.put(Float.intBitsToFloat(ostride));
		buf.put(Float.intBitsToFloat(istride));
		buf.put(Float.intBitsToFloat(512));
		buf.put((float)phase_base);
		buf.flip();
		
		plan.radix008A_CB[0] = createBuffer(target, buf, useage);
		
		// Buffer 1
		istride /= 8;
		phase_base *= 8.0;
		
		buf.put(Float.intBitsToFloat(thread_count));
		buf.put(Float.intBitsToFloat(ostride));
		buf.put(Float.intBitsToFloat(istride));
		buf.put(Float.intBitsToFloat(512));
		buf.put((float)phase_base);
		buf.flip();
		
		plan.radix008A_CB[1] = createBuffer(target, buf, useage);
		
		// Buffer 2
		istride /= 8;
		phase_base *= 8.0;
		
		buf.put(Float.intBitsToFloat(thread_count));
		buf.put(Float.intBitsToFloat(ostride));
		buf.put(Float.intBitsToFloat(istride));
		buf.put(Float.intBitsToFloat(512));
		buf.put((float)phase_base);
		buf.flip();
		
		plan.radix008A_CB[2] = createBuffer(target, buf, useage);
		
		// Buffer 3
		istride /= 8;
		phase_base *= 8.0;
		ostride /= 512;
		
		buf.put(Float.intBitsToFloat(thread_count));
		buf.put(Float.intBitsToFloat(ostride));
		buf.put(Float.intBitsToFloat(istride));
		buf.put(Float.intBitsToFloat(1));
		buf.put((float)phase_base);
		buf.flip();
		
		plan.radix008A_CB[3] = createBuffer(target, buf, useage);
		
		// Buffer 4
		istride /= 8;
		phase_base *= 8.0;
		
		buf.put(Float.intBitsToFloat(thread_count));
		buf.put(Float.intBitsToFloat(ostride));
		buf.put(Float.intBitsToFloat(istride));
		buf.put(Float.intBitsToFloat(1));
		buf.put((float)phase_base);
		buf.flip();
		
		plan.radix008A_CB[4] = createBuffer(target, buf, useage);
		
		// Buffer 5
		istride /= 8;
		phase_base *= 8.0;
		
		buf.put(Float.intBitsToFloat(thread_count));
		buf.put(Float.intBitsToFloat(ostride));
		buf.put(Float.intBitsToFloat(istride));
		buf.put(Float.intBitsToFloat(1));
		buf.put((float)phase_base);
		buf.flip();
		
		plan.radix008A_CB[5] = createBuffer(target, buf, useage);
	}
	
	private static int createBuffer(int target, FloatBuffer buf_data, int useage){
		int buf = GL15.glGenBuffers();
		GL15.glBindBuffer(target, buf);
		GL15.glBufferData(target, buf_data, useage);
		GL15.glBindBuffer(target, 0);
		
		return buf;
	}
	
	public static final class CSFFT512x512_Plan{
		public int radix008A_CS;
		public int radix008A_CS2;
		public int program;
		
		// More than one array can be transformed at same time
		public int slices;
		
		// For 512x512 config, we need 6 constant buffers
		public final int[] radix008A_CB = new int[6];
		
		// Temporary buffers
		int buffer_tmp;
		int uav_tmp;
		int srv_tmp;
		
		int thread_count;
		int ostride;
		int istride;
		int pstride;
		int phase_base;
		
		void use(){ GL20.glUseProgram(program);}
		void enableCS() { GL40.glUniformSubroutinesui(GL43.GL_COMPUTE_SHADER, radix008A_CS);}
		void enableCS2() { GL40.glUniformSubroutinesui(GL43.GL_COMPUTE_SHADER, radix008A_CS2);}
		
		void setShaderResource(int texture){ GL42.glBindImageTexture(1, texture, 0, false, 0, GL15.GL_READ_ONLY, GL30.GL_RG32F);}
		void setUnorderedAccessViews(int texture){GL42.glBindImageTexture(2, texture, 0, false, 0, GL15.GL_WRITE_ONLY, GL30.GL_RG32F);}
	}
}
