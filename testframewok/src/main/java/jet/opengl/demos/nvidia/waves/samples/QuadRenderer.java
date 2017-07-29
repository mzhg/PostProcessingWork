package jet.opengl.demos.nvidia.waves.samples;

import com.nvidia.developer.opengl.utils.NvImage;
import com.nvidia.developer.opengl.utils.Pool;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

final class QuadRenderer {

	static final int FRESNEL_TEX_SIZE = 256;
	static final int PERLIN_TEX_SIZE = 64;
	
	// Mesh properties:

	// Mesh grid dimension, must be 2^n. 4x4 ~ 256x256
	int g_MeshDim = 128;
	// Side length of square shaped mesh patch
	float g_PatchLength;
	// Dimension of displacement map
	int g_DisplaceMapDim;
	// Subdivision thredshold. Any quad covers more pixels than this value needs to be subdivided.
	float g_UpperGridCoverage = 64.0f;
	// Draw distance = g_PatchLength * 2^g_FurthestCover
	int g_FurthestCover = 8;
	
	// Shading properties:
	// Two colors for waterbody and sky color
	final Vector3f g_SkyColor       = new Vector3f(0.38f, 0.45f, 0.56f);
	final Vector3f g_WaterbodyColor = new Vector3f(0.07f, 0.15f, 0.2f);
	// Blending term for sky cubemap
	float g_SkyBlending = 16.0f;

	// Perlin wave parameters
	float g_PerlinSize = 1.0f;
	float g_PerlinSpeed = 0.06f;
	final Vector3f g_PerlinAmplitude = new Vector3f(35, 42, 57);
	final Vector3f g_PerlinGradient = new Vector3f(1.4f, 1.6f, 2.2f);
	final Vector3f g_PerlinOctave = new Vector3f(1.12f, 0.59f, 0.23f);
	final Vector2f g_WindDir  = new Vector2f();

	final Vector3f g_BendParam = new Vector3f(0.1f, -0.4f, 0.2f);

	// Sunspot parameters
	final Vector3f g_SunDir = new Vector3f(0.936016f, -0.343206f, 0.0780013f);
	final Vector3f g_SunColor = new Vector3f(1.0f, 1.0f, 0.6f);
	float g_Shineness = 400.0f;
	
	// Quad-tree LOD, 0 to 9 (1x1 ~ 512x512) 
	int g_Lods = 0;
	// Pattern lookup array. Filled at init time.
	final QuadRenderParam g_mesh_patterns[][][][][] = new QuadRenderParam[9][3][3][3][3];
	// Pick a proper mesh pattern according to the adjacent patches.
	QuadRenderParam selectMeshPattern;
	
	// Rendering list
	final List<QuadNode> g_render_list = new ArrayList<>();
	final Frustum frustum = new Frustum();
	
	final Pool<QuadNode> g_QuadNodePool = new Pool<QuadNode>(QuadRenderer::quadNode);
	final Pool<Matrix4f> g_Mat4Pool;
	// buffers
	int g_pMeshVB;
	int g_pMeshIB;
	int g_pMeshVAO;
	
	// Color look up 1D texture
	int g_pSRV_Fresnel;
	
	// Distant perlin wave
	int g_pSRV_Perlin;

	// Environment maps
	int g_pSRV_ReflectCube;
	
	int g_pPerCallCB = 0;
	int g_pPerFrameCB = 0;
	int g_pShadingCB = 0;
	
	OceanShadingPorgram g_pOceanSurfaceFX;
	
	Matrix4f g_Projection;
	Matrix4f g_ModelView;
	Vector3f g_EyePosition;
	float mScreenArea;

	private GLFuncProvider gl;
	
	static QuadNode quadNode() { return new QuadNode();}
	
	public QuadRenderer(String shader_prefix, String texture_prefix, OceanParameter ocean_param, Pool<Matrix4f> matPool){
		g_Mat4Pool = matPool;
		gl = GLFuncProviderFactory.getGLFuncProvider();
		
		g_PatchLength = ocean_param.patch_length;
		g_DisplaceMapDim = ocean_param.dmap_dim;
		g_WindDir.set(ocean_param.wind_dir);
		
		// D3D buffers
		createSurfaceMesh();
		createFresnelMap();
		try {
			loadTextures(texture_prefix);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		g_pOceanSurfaceFX = new OceanShadingPorgram(shader_prefix);
		
		// Constants
		g_pOceanSurfaceFX.enable();
		
		// Grid side length * 2
		g_pOceanSurfaceFX.setTexelLength_x2(g_PatchLength / g_DisplaceMapDim * 2);
		// Color
		g_pOceanSurfaceFX.setSkyColor(g_SkyColor);
		g_pOceanSurfaceFX.setWaterbodyColor(g_WaterbodyColor);
		// Texcoord
		g_pOceanSurfaceFX.setUVScale(1.0f/g_PatchLength);
		g_pOceanSurfaceFX.setUVOffset(0.5f/g_DisplaceMapDim);
		// Perlin
		g_pOceanSurfaceFX.setPerlinSize(g_PerlinSize);
		g_pOceanSurfaceFX.setPerlinAmplitude(g_PerlinAmplitude);
		g_pOceanSurfaceFX.setPerlinGradient(g_PerlinGradient);
		g_pOceanSurfaceFX.setPerlinOctave(g_PerlinOctave);
		// Multiple reflection workaround
		g_pOceanSurfaceFX.setBendParam(g_BendParam);
		// Sun streaks
		g_pOceanSurfaceFX.setSunColor(g_SunColor);
		g_pOceanSurfaceFX.setSunDir(g_SunDir);
		g_pOceanSurfaceFX.setShineness(g_Shineness);
		
		gl.glUseProgram(0);
	}
	
	void createSurfaceMesh(){
		// --------------------------------- Vertex Buffer -------------------------------
		int num_verts = (g_MeshDim + 1) * (g_MeshDim + 1);
//		ocean_vertex* pV = new ocean_vertex[num_verts];
		FloatBuffer pV = CacheBuffer.getCachedFloatBuffer(num_verts * 2);
		
		int i, j;
		for (i = 0; i <= g_MeshDim; i++)
		{
			for (j = 0; j <= g_MeshDim; j++)
			{
//				pV[i * (g_MeshDim + 1) + j].index_x = (float)j;
//				pV[i * (g_MeshDim + 1) + j].index_y = (float)i;
				pV.put(j).put(i);
			}
		}
		
		pV.flip();
		
		g_pMeshVB = gl.glGenBuffer();
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, g_pMeshVB);
		gl.glBufferData(GLenum.GL_ARRAY_BUFFER, pV, GLenum.GL_STATIC_DRAW);
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
		
		// --------------------------------- Index Buffer -------------------------------
		// The index numbers for all mesh LODs (up to 256x256)
		final int index_size_lookup[] = {0, 0, 4284, 18828, 69444, 254412, 956916, 3689820, 14464836};
		
		g_Lods = 0;
		for (i = g_MeshDim; i > 1; i >>= 1)
			g_Lods ++;
		
		// Generate patch meshes. Each patch contains two parts: the inner mesh which is a regular
		// grids in a triangle strip. The boundary mesh is constructed w.r.t. the edge degrees to
		// meet water-tight requirement.
		int[] index_array = new int[index_size_lookup[g_Lods]];
		
		int offset = 0;
		int level_size = g_MeshDim;
		
		Rect inner_rect = new Rect();
		// Enumerate patterns
		for (int level = 0; level <= g_Lods - 2; level ++)
		{
			int left_degree = level_size;

			for (int left_type = 0; left_type < 3; left_type ++)
			{
				int right_degree = level_size;

				for (int right_type = 0; right_type < 3; right_type ++)
				{
					int bottom_degree = level_size;

					for (int bottom_type = 0; bottom_type < 3; bottom_type ++)
					{
						int top_degree = level_size;

						for (int top_type = 0; top_type < 3; top_type ++)
						{
							QuadRenderParam pattern = new QuadRenderParam();

							// Inner mesh (triangle strip)
							inner_rect.left   = (left_degree   == level_size) ? 0 : 1;
							inner_rect.right  = (right_degree  == level_size) ? level_size : level_size - 1;
							inner_rect.bottom = (bottom_degree == level_size) ? 0 : 1;
							inner_rect.top    = (top_degree    == level_size) ? level_size : level_size - 1;

							int num_new_indices = generateInnerMesh(inner_rect, index_array, offset);

							pattern.inner_start_index = offset;
							pattern.num_inner_verts = (level_size + 1) * (level_size + 1);
							pattern.num_inner_faces = num_new_indices - 2;
							offset += num_new_indices;

							// Boundary mesh (triangle list)
							int l_degree = (left_degree   == level_size) ? 0 : left_degree;
							int r_degree = (right_degree  == level_size) ? 0 : right_degree;
							int b_degree = (bottom_degree == level_size) ? 0 : bottom_degree;
							int t_degree = (top_degree    == level_size) ? 0 : top_degree;

							Rect outer_rect = new Rect(0, level_size, level_size, 0);
							num_new_indices = generateBoundaryMesh(l_degree, r_degree, b_degree, t_degree, outer_rect, index_array, offset);

							pattern.boundary_start_index = offset;
							pattern.num_boundary_verts = (level_size + 1) * (level_size + 1);
							pattern.num_boundary_faces = num_new_indices / 3;
							offset += num_new_indices;

							top_degree /= 2;
							g_mesh_patterns[level][left_type][right_type][bottom_type][top_type] = pattern;
						}
						bottom_degree /= 2;
					}
					right_degree /= 2;
				}
				left_degree /= 2;
			}
			level_size /= 2;
		}
		
		if(offset != index_size_lookup[g_Lods]){
			System.err.println("createSurfaceMesh: Inner Error");
		}
		
		g_pMeshIB = gl.glGenBuffer();
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, g_pMeshIB);
		gl.glBufferData(GLenum.GL_ELEMENT_ARRAY_BUFFER, CacheBuffer.wrap(index_array), GLenum.GL_STATIC_DRAW);
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
		
		g_pMeshVAO = gl.glGenVertexArray();
		gl.glBindVertexArray(g_pMeshVAO);
		{
			gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, g_pMeshVB);

			gl.glVertexAttribPointer(0, 2, GLenum.GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(0);

			gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, g_pMeshIB);
		}
		gl.glBindVertexArray(0);
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
	}
	
	void createFresnelMap(){
		IntBuffer buffer = CacheBuffer.getCachedIntBuffer(FRESNEL_TEX_SIZE);
		for(int i = 0; i < FRESNEL_TEX_SIZE; i++){
			float cos_a = (float)i / FRESNEL_TEX_SIZE;
			// Using water's refraction index 1.33
			int fresnel = (int)(Numeric.fresnelTerm(cos_a, 1.33f) * 255);  // TODO

			int sky_blend = (int)(Math.pow(1.0 / (1 + cos_a), g_SkyBlending) * 255);

			buffer.put((sky_blend << 8) | fresnel);
		}
		buffer.flip();
		
		g_pSRV_Fresnel = gl.glGenTexture();
		gl.glBindTexture(GLenum.GL_TEXTURE_1D, g_pSRV_Fresnel);
		gl.glTexImage1D(GLenum.GL_TEXTURE_1D, 0, GLenum.GL_RGBA8, FRESNEL_TEX_SIZE, 0, GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, buffer);
//		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
//		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
//		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		gl.glBindTexture(GLenum.GL_TEXTURE_1D, 0);
	}
	
	void loadTextures(String prefix) throws IOException{
		g_pSRV_Perlin = NvImage.uploadTextureFromDDSFile(prefix + "perlin_noise.dds");
//		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
//		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
//		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
//		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		g_pSRV_ReflectCube = NvImage.uploadTextureFromDDSFile(prefix + "reflect_cube.dds");
//		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
//		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
//		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
//		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
//		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL12.GL_TEXTURE_WRAP_R, GL11.GL_REPEAT);

		gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
		gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, 0);
	}
	
	// Generate boundary mesh for a patch. Return the number of generated indices
		int generateBoundaryMesh(int left_degree, int right_degree, int bottom_degree, int top_degree, Rect vert_rect, int[] output, int offset)
		{
			// Triangle list for bottom boundary
			int i, j;
			int counter = 0;
			int width = vert_rect.right - vert_rect.left;

			if (bottom_degree > 0)
			{
				int b_step = width / bottom_degree;

				for (i = 0; i < width; i += b_step)
				{
					output[offset + counter++] = mesh_index_2d(i, 0, vert_rect);
					output[offset + counter++] = mesh_index_2d(i + b_step / 2, 1, vert_rect);
					output[offset + counter++] = mesh_index_2d(i + b_step, 0, vert_rect);

					for (j = 0; j < b_step / 2; j ++)
					{
						if (i == 0 && j == 0 && left_degree > 0)
							continue;

						output[offset + counter++] = mesh_index_2d(i, 0, vert_rect);
						output[offset + counter++] = mesh_index_2d(i + j, 1, vert_rect);
						output[offset + counter++] = mesh_index_2d(i + j + 1, 1, vert_rect);
					}

					for (j = b_step / 2; j < b_step; j ++)
					{
						if (i == width - b_step && j == b_step - 1 && right_degree > 0)
							continue;

						output[offset + counter++] = mesh_index_2d(i + b_step, 0, vert_rect);
						output[offset + counter++] = mesh_index_2d(i + j, 1, vert_rect);
						output[offset + counter++] = mesh_index_2d(i + j + 1, 1, vert_rect);
					}
				}
			}

			// Right boundary
			int height = vert_rect.top - vert_rect.bottom;

			if (right_degree > 0)
			{
				int r_step = height / right_degree;

				for (i = 0; i < height; i += r_step)
				{
					output[offset + counter++] = mesh_index_2d(width, i, vert_rect);
					output[offset + counter++] = mesh_index_2d(width - 1, i + r_step / 2, vert_rect);
					output[offset + counter++] = mesh_index_2d(width, i + r_step, vert_rect);

					for (j = 0; j < r_step / 2; j ++)
					{
						if (i == 0 && j == 0 && bottom_degree > 0)
							continue;

						output[offset + counter++] = mesh_index_2d(width, i, vert_rect);
						output[offset + counter++] = mesh_index_2d(width - 1, i + j, vert_rect);
						output[offset + counter++] = mesh_index_2d(width - 1, i + j + 1, vert_rect);
					}

					for (j = r_step / 2; j < r_step; j ++)
					{
						if (i == height - r_step && j == r_step - 1 && top_degree > 0)
							continue;

						output[offset + counter++] = mesh_index_2d(width, i + r_step, vert_rect);
						output[offset + counter++] = mesh_index_2d(width - 1, i + j, vert_rect);
						output[offset + counter++] = mesh_index_2d(width - 1, i + j + 1, vert_rect);
					}
				}
			}

			// Top boundary
			if (top_degree > 0)
			{
				int t_step = width / top_degree;

				for (i = 0; i < width; i += t_step)
				{
					output[offset + counter++] = mesh_index_2d(i, height, vert_rect);
					output[offset + counter++] = mesh_index_2d(i + t_step / 2, height - 1, vert_rect);
					output[offset + counter++] = mesh_index_2d(i + t_step, height, vert_rect);

					for (j = 0; j < t_step / 2; j ++)
					{
						if (i == 0 && j == 0 && left_degree > 0)
							continue;

						output[offset + counter++] = mesh_index_2d(i, height, vert_rect);
						output[offset + counter++] = mesh_index_2d(i + j, height - 1, vert_rect);
						output[offset + counter++] = mesh_index_2d(i + j + 1, height - 1, vert_rect);
					}

					for (j = t_step / 2; j < t_step; j ++)
					{
						if (i == width - t_step && j == t_step - 1 && right_degree > 0)
							continue;

						output[offset + counter++] = mesh_index_2d(i + t_step, height, vert_rect);
						output[offset + counter++] = mesh_index_2d(i + j, height - 1, vert_rect);
						output[offset + counter++] = mesh_index_2d(i + j + 1, height - 1, vert_rect);
					}
				}
			}

			// Left boundary
			if (left_degree > 0)
			{
				int l_step = height / left_degree;

				for (i = 0; i < height; i += l_step)
				{
					output[offset + counter++] = mesh_index_2d(0, i, vert_rect);
					output[offset + counter++] = mesh_index_2d(1, i + l_step / 2, vert_rect);
					output[offset + counter++] = mesh_index_2d(0, i + l_step, vert_rect);

					for (j = 0; j < l_step / 2; j ++)
					{
						if (i == 0 && j == 0 && bottom_degree > 0)
							continue;

						output[offset + counter++] = mesh_index_2d(0, i, vert_rect);
						output[offset + counter++] = mesh_index_2d(1, i + j, vert_rect);
						output[offset + counter++] = mesh_index_2d(1, i + j + 1, vert_rect);
					}

					for (j = l_step / 2; j < l_step; j ++)
					{
						if (i == height - l_step && j == l_step - 1 && top_degree > 0)
							continue;

						output[offset + counter++] = mesh_index_2d(0, i + l_step, vert_rect);
						output[offset + counter++] = mesh_index_2d(1, i + j, vert_rect);
						output[offset + counter++] = mesh_index_2d(1, i + j + 1, vert_rect);
					}
				}
			}

			return counter;
		}
		
		private int mesh_index_2d(int x, int y, Rect vert_rect){
			return (((y) + vert_rect.bottom) * (g_MeshDim + 1) + (x) + vert_rect.left);
		}
		
		// Generate boundary mesh for a patch. Return the number of generated indices
		int generateInnerMesh(Rect vert_rect, int[] output, int offset)
		{
			int i, j;
			int counter = 0;
			int width = vert_rect.right - vert_rect.left;
			int height = vert_rect.top - vert_rect.bottom;

			boolean reverse = false;
			for (i = 0; i < height; i++)
			{
				if (reverse == false)
				{
					output[offset + counter++] = mesh_index_2d(0, i, vert_rect);
					output[offset + counter++] = mesh_index_2d(0, i + 1, vert_rect);
					for (j = 0; j < width; j++)
					{
						output[offset + counter++] = mesh_index_2d(j + 1, i, vert_rect);
						output[offset + counter++] = mesh_index_2d(j + 1, i + 1, vert_rect);
					}
				}
				else
				{
					output[offset + counter++] = mesh_index_2d(width, i, vert_rect);
					output[offset + counter++] = mesh_index_2d(width, i + 1, vert_rect);
					for (j = width - 1; j >= 0; j--)
					{
						output[offset + counter++] = mesh_index_2d(j, i, vert_rect);
						output[offset + counter++] = mesh_index_2d(j, i + 1, vert_rect);
					}
				}

				reverse = !reverse;
			}

			return counter;
		}
		
		boolean checkNodeVisibility(QuadNode quad_node, Matrix4f matProj){
			frustum.reset(matProj);
			return frustum.contains(quad_node, g_ModelView);
		}
		
		void setMatrices(Matrix4f proj, Matrix4f modelView, Vector3f eyePos, float screenArea){
			g_Projection = proj;
			g_ModelView = modelView;
			g_EyePosition = eyePos;
			mScreenArea = screenArea;
		}
		
		final Vector3f eyePos = new Vector3f();
		void renderShaded(int displacemnet_map, int gradient_map, float time){
			// Build rendering list
			g_QuadNodePool.freeAll(g_render_list);
			g_render_list.clear();
			
			float ocean_extent = g_PatchLength * (1 << g_FurthestCover);
//			QuadNode root_node = new QuadNode(-ocean_extent * 0.5f, -ocean_extent * 0.5f, ocean_extent, 0, new int[] {-1, -1, -1, -1});
			QuadNode root_node = node(-ocean_extent * 0.5f, -ocean_extent * 0.5f, ocean_extent, 0);
			
//			buildNodeList(root_node, mCam.proj(), (Vector3f)mCam.getPosition(), mScreenArea);
			buildNodeList(root_node, g_Projection, g_EyePosition, mScreenArea);
			
			// Matrices
			Matrix4f matView = g_ModelView; //Matrix4f.mul(mCam.view(), m_const_mat, m_model);
			
			// VS & PS
			g_pOceanSurfaceFX.enable();
			g_pOceanSurfaceFX.enableOceanSurfPS();
			
			// Textures
			g_pOceanSurfaceFX.setTexDisplacement(displacemnet_map, OceanSamplers.g_pHeightSampler);
			g_pOceanSurfaceFX.setTexPerlin(g_pSRV_Perlin, OceanSamplers.g_pPerlinSampler);
			g_pOceanSurfaceFX.setTexGradient(gradient_map, OceanSamplers.g_pGradientSampler);
			g_pOceanSurfaceFX.setTexFresnel(g_pSRV_Fresnel, OceanSamplers.g_pFresnelSampler);
			g_pOceanSurfaceFX.setSamplerCube(g_pSRV_ReflectCube, OceanSamplers.g_pCubeSampler);
			
			gl.glBindVertexArray(g_pMeshVAO);
			
			// TODO State blocks
			// Uniforms we had set up already
			gl.glEnable(GLenum.GL_DEPTH_TEST);
			
			Matrix4f matScale = g_Mat4Pool.obtain();
			Matrix4f matWorld = g_Mat4Pool.obtain();
			Matrix4f matMVP = g_Mat4Pool.obtain();
			
			matWorld.setIdentity();
			// We assume the center of the ocean surface at (0, 0, 0).
			for(int i = 0; i < g_render_list.size(); i++){
				QuadNode node = g_render_list.get(i);
				
				if (!isLeaf(node))
					continue;

				// Check adjacent patches and select mesh pattern
				QuadRenderParam render_param = selectMeshPattern(node);

				// Find the right LOD to render
				int level_size = g_MeshDim;
				for (int lod = 0; lod < node.lod; lod++)
					level_size >>= 1;
				
				// Matrices and constants
				// Expand of the local coordinate to world space patch size
				matScale.setIdentity();
				matScale.m00 = node.length/level_size;
				matScale.m11 = node.length/level_size;
				matScale.m22 = 0;
				g_pOceanSurfaceFX.setMatLocal(matScale);
				
				// WVP matrix
				matWorld.m30 = node.bottom_left_x;
				matWorld.m31 = node.bottom_left_y;
				matWorld.m32 = 0;
				
				// Construct the matMVP
				// matMVP = Projection * ModelView * MatWorld
				Matrix4f.mul(g_ModelView, matWorld, matMVP);
				Matrix4f.decompseRigidMatrix(matMVP, eyePos, null, null);
				Matrix4f.mul(g_Projection, matMVP, matMVP);
				
				g_pOceanSurfaceFX.setMatWorldViewProj(matMVP);
				
				// Texcoord for perlin noise
				float uv_base_x = node.bottom_left_x/g_PatchLength * g_PerlinSize;
				float uv_base_y = node.bottom_left_y/g_PatchLength * g_PerlinSize;
				g_pOceanSurfaceFX.setUVBase(uv_base_x, uv_base_y);
				
				// Constant g_PerlinSpeed need to be adjusted mannually
//				D3DXVECTOR2 perlin_move = -g_WindDir * time * g_PerlinSpeed;
				float perlin_move_x = -g_WindDir.x * time * g_PerlinSpeed;
				float perlin_move_y = -g_WindDir.y * time * g_PerlinSpeed;
				g_pOceanSurfaceFX.setPerlinMovement(perlin_move_x, perlin_move_y);
				
				// Eye point
				g_pOceanSurfaceFX.setLocalEye(eyePos);
				
				// Perform draw call
				if (render_param.num_inner_faces > 0){
					// Inner mesh of the patch
					gl.glDrawElements(GLenum.GL_TRIANGLE_STRIP, render_param.num_inner_faces + 2, GLenum.GL_UNSIGNED_INT, render_param.inner_start_index << 2);
				}
				
				if(render_param.num_boundary_faces > 0){
					// Boundary mesh of the patch
					gl.glDrawElements(GLenum.GL_TRIANGLES, render_param.num_boundary_faces * 3, GLenum.GL_UNSIGNED_INT, render_param.boundary_start_index << 2);
				}
			}
			
			g_pOceanSurfaceFX.disable();
			gl.glBindVertexArray(0);
			gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
			gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
			g_Mat4Pool.free(matScale);
			g_Mat4Pool.free(matWorld);
			g_Mat4Pool.free(matMVP);
		}
		
		void renderWireframe(int displacemnet_map, float time){
			// Build rendering list
			g_render_list.clear();
			float ocean_extent = g_PatchLength * (1 << g_FurthestCover);
			QuadNode root_node = node(-ocean_extent * 0.5f, -ocean_extent * 0.5f, ocean_extent, 0);
//			buildNodeList(root_node, mCam.proj(), (Vector3f)mCam.getPosition(), mScreenArea);
			buildNodeList(root_node, g_Projection, g_EyePosition, mScreenArea);
			
			// Matrices
//			Matrix4f matView = Matrix4f.mul(mCam.view(), m_const_mat, m_model);
			
			// VS & PS
			g_pOceanSurfaceFX.enable();
			g_pOceanSurfaceFX.enableWireframePS();
			
			// Textures
			g_pOceanSurfaceFX.setTexDisplacement(displacemnet_map, OceanSamplers.g_pHeightSampler);
			g_pOceanSurfaceFX.setTexPerlin(g_pSRV_Perlin, OceanSamplers.g_pPerlinSampler);

			gl.glBindVertexArray(g_pMeshVAO);
			gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);

			gl.glEnable(GLenum.GL_DEPTH_TEST);
			
			Matrix4f matScale = g_Mat4Pool.obtain();
			Matrix4f matWorld = g_Mat4Pool.obtain();
			Matrix4f matMVP = g_Mat4Pool.obtain();
			
			for(int i = 0; i < g_render_list.size(); i++){
				QuadNode node = g_render_list.get(i);
				
				if (!isLeaf(node))
					continue;

				// Check adjacent patches and select mesh pattern
				QuadRenderParam render_param = selectMeshPattern(node);

				// Find the right LOD to render
				int level_size = g_MeshDim;
				for (int lod = 0; lod < node.lod; lod++)
					level_size >>= 1;
				
				// Matrices and constants
				// Expand of the local coordinate to world space patch size
				matScale.setIdentity();
				matScale.m00 = node.length/level_size;
				matScale.m11 = node.length/level_size;
				matScale.m22 = 0;
				g_pOceanSurfaceFX.setMatLocal(matScale);
				
				// WVP matrix
				matWorld.m30 = node.bottom_left_x;
				matWorld.m31 = node.bottom_left_y;
				matWorld.m32 = 0;
				
				// Construct the matMVP
				// matMVP = Projection * ModelView * MatWorld
				Matrix4f.mul(g_ModelView, matWorld, matMVP);
				Matrix4f.decompseRigidMatrix(matMVP, eyePos, null, null);
				Matrix4f.mul(g_Projection, matMVP, matMVP);
				
				g_pOceanSurfaceFX.setMatWorldViewProj(matMVP);
				
				// Texcoord for perlin noise
				float uv_base_x = node.bottom_left_x/g_PatchLength * g_PerlinSize;
				float uv_base_y = node.bottom_left_y/g_PatchLength * g_PerlinSize;
				g_pOceanSurfaceFX.setUVBase(uv_base_x, uv_base_y);
				
				// Constant g_PerlinSpeed need to be adjusted mannually
//				D3DXVECTOR2 perlin_move = -g_WindDir * time * g_PerlinSpeed;
				float perlin_move_x = -g_WindDir.x * time * g_PerlinSpeed;
				float perlin_move_y = -g_WindDir.y * time * g_PerlinSpeed;
				g_pOceanSurfaceFX.setPerlinMovement(perlin_move_x, perlin_move_y);
				
				// Eye point
				g_pOceanSurfaceFX.setLocalEye(eyePos);
				
				// Perform draw call
				if (render_param.num_inner_faces > 0){
					// Inner mesh of the patch
					gl.glDrawElements(GLenum.GL_TRIANGLE_STRIP, render_param.num_inner_faces + 2, GLenum.GL_UNSIGNED_INT, render_param.inner_start_index * 4);
				}
				
				if(render_param.num_boundary_faces > 0){
					// Boundary mesh of the patch
					gl.glDrawElements(GLenum.GL_TRIANGLES, render_param.num_boundary_faces * 3, GLenum.GL_UNSIGNED_INT, render_param.boundary_start_index * 4);
				}
			}
			
			g_pOceanSurfaceFX.disable();
			gl.glBindVertexArray(0);
			gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
			gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
			gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
			
			g_Mat4Pool.free(matScale);
			g_Mat4Pool.free(matWorld);
			g_Mat4Pool.free(matMVP);
		}
		
		public void dispose(){
			gl.glDeleteBuffer(g_pMeshIB);
			gl.glDeleteBuffer(g_pMeshVB);
			gl.glDeleteVertexArray(g_pMeshVAO);
			
			g_pOceanSurfaceFX.dispose();

			gl.glDeleteTexture(g_pSRV_Fresnel);
			gl.glDeleteTexture(g_pSRV_Perlin);
			gl.glDeleteTexture(g_pSRV_ReflectCube);
		}
		
		// Test 16 points on the quad and find out the biggest one.
		private static final float sample_pos[][] =
		{
			{0, 0},
			{0, 1},
			{1, 0},
			{1, 1},
			{0.5f, 0.333f},
			{0.25f, 0.667f},
			{0.75f, 0.111f},
			{0.125f, 0.444f},
			{0.625f, 0.778f},
			{0.375f, 0.222f},
			{0.875f, 0.556f},
			{0.0625f, 0.889f},
			{0.5625f, 0.037f},
			{0.3125f, 0.37f},
			{0.8125f, 0.704f},
			{0.1875f, 0.148f},
		};
			
		float estimateGridCoverage(QuadNode quad_node, Matrix4f matProj, Vector3f eye_point, float screen_area){
			float grid_len_world = quad_node.length / g_MeshDim;
			
			float  max_area_proj = 0;
			final Vector3f test_point = new Vector3f(); // TODO need cache?
			for (int i = 0; i < 16; i++)
			{
				test_point.set(quad_node.bottom_left_x + quad_node.length * sample_pos[i][0], quad_node.bottom_left_y + quad_node.length * sample_pos[i][1], 0);
//				D3DXVECTOR3 eye_vec = test_point - eye_point;
//				float dist = D3DXVec3Length(&eye_vec);
				float dist = Vector3f.distance(test_point, eye_point);

				float area_world = grid_len_world * grid_len_world;// * abs(eye_point.z) / sqrt(nearest_sqr_dist);
				float area_proj = area_world * matProj.m00 * matProj.m11 / (dist * dist);

				if (max_area_proj < area_proj)
					max_area_proj = area_proj;
			}
			
			float pixel_coverage = max_area_proj * screen_area * 0.25f;
			return pixel_coverage;
		}
		
		boolean isLeaf(QuadNode quad_node)
		{
			return (quad_node.sub_node[0] == -1 && quad_node.sub_node[1] == -1 && quad_node.sub_node[2] == -1 && quad_node.sub_node[3] == -1);
		}

		int searchLeaf(List<QuadNode> node_list, Vector2f point)
		{
			int index = -1;
			
			int size = node_list.size();
			QuadNode node = node_list.get(size - 1);

			while (!isLeaf(node))
			{
				boolean found = false;

				for (int i = 0; i < 4; i++)
				{
					index = node.sub_node[i];
					if (index == -1)
						continue;

					QuadNode sub_node = node_list.get(index);
					if (point.x >= sub_node.bottom_left_x && point.x <= sub_node.bottom_left_x + sub_node.length &&
						point.y >= sub_node.bottom_left_y && point.y <= sub_node.bottom_left_y + sub_node.length)
					{
						node = sub_node;
						found = true;
						break;
					}
				}

				if (!found)
					return -1;
			}

			return index;
		}
		
		QuadRenderParam selectMeshPattern(QuadNode quad_node)
		{
			// Check 4 adjacent quad.
			Vector2f point_left = vec2(-g_PatchLength * 0.5f + quad_node.bottom_left_x, quad_node.length * 0.5f + quad_node.bottom_left_y);
			int left_adj_index = searchLeaf(g_render_list, point_left);

			Vector2f point_right = vec2(quad_node.length + g_PatchLength * 0.5f + quad_node.bottom_left_x, quad_node.length * 0.5f + quad_node.bottom_left_y);
			int right_adj_index = searchLeaf(g_render_list, point_right);

			Vector2f point_bottom = vec2(quad_node.length * 0.5f + quad_node.bottom_left_x, -g_PatchLength * 0.5f + quad_node.bottom_left_y);
			int bottom_adj_index = searchLeaf(g_render_list, point_bottom);

			Vector2f point_top =  vec2(quad_node.length * 0.5f + quad_node.bottom_left_x, quad_node.length + g_PatchLength * 0.5f + quad_node.bottom_left_y);
			int top_adj_index = searchLeaf(g_render_list, point_top);

			int left_type = 0;
			if (left_adj_index != -1 && g_render_list.get(left_adj_index).length > quad_node.length * 0.999f)
			{
				QuadNode adj_node = g_render_list.get(left_adj_index);
				float scale = adj_node.length / quad_node.length * (g_MeshDim >> quad_node.lod) / (g_MeshDim >> adj_node.lod);
				if (scale > 3.999f)
					left_type = 2;
				else if (scale > 1.999f)
					left_type = 1;
			}

			int right_type = 0;
			if (right_adj_index != -1 && g_render_list.get(right_adj_index).length > quad_node.length * 0.999f)
			{
				QuadNode adj_node = g_render_list.get(right_adj_index);
				float scale = adj_node.length / quad_node.length * (g_MeshDim >> quad_node.lod) / (g_MeshDim >> adj_node.lod);
				if (scale > 3.999f)
					right_type = 2;
				else if (scale > 1.999f)
					right_type = 1;
			}

			int bottom_type = 0;
			if (bottom_adj_index != -1 && g_render_list.get(bottom_adj_index).length > quad_node.length * 0.999f)
			{
				QuadNode adj_node = g_render_list.get(bottom_adj_index);
				float scale = adj_node.length / quad_node.length * (g_MeshDim >> quad_node.lod) / (g_MeshDim >> adj_node.lod);
				if (scale > 3.999f)
					bottom_type = 2;
				else if (scale > 1.999f)
					bottom_type = 1;
			}

			int top_type = 0;
			if (top_adj_index != -1 && g_render_list.get(top_adj_index).length > quad_node.length * 0.999f)
			{
				QuadNode adj_node = g_render_list.get(top_adj_index);
				float scale = adj_node.length / quad_node.length * (g_MeshDim >> quad_node.lod) / (g_MeshDim >> adj_node.lod);
				if (scale > 3.999f)
					top_type = 2;
				else if (scale > 1.999f)
					top_type = 1;
			}

			// Check lookup table, [L][R][B][T]
			return g_mesh_patterns[quad_node.lod][left_type][right_type][bottom_type][top_type];
		}
		
		final Vector2f tmp2 = new Vector2f();
		final Vector2f vec2(float x, float y){ tmp2.set(x, y);	return tmp2;}
		
		// Return value: if successful pushed into the list, return the position. If failed, return -1.
		int buildNodeList(QuadNode quad_node, Matrix4f matProj, Vector3f eye_pos, final float screen_area)
		{
			// Check against view frustum
			if (!checkNodeVisibility(quad_node, matProj))
				return -1;

			// Estimate the min grid coverage
//			int num_vps = 1;
//			D3D11_VIEWPORT vp;
//			DXUTGetD3D11DeviceContext()->RSGetViewports(&num_vps, &vp);
//			int width, height;
//			IntBuffer buf = GLUtil.getCachedIntBuffer(16);
//			GL11.glGetInteger(GL11.GL_VIEWPORT, buf);
//			width = buf.get(); height = buf.get();
			float min_coverage = estimateGridCoverage(quad_node, matProj, eye_pos, screen_area);

			// Recursively attatch sub-nodes.
			boolean visible = true;
			if (min_coverage > g_UpperGridCoverage && quad_node.length > g_PatchLength)
			{
				// Recursive rendering for sub-quads.
				QuadNode sub_node_0 = node(quad_node.bottom_left_x, quad_node.bottom_left_y,quad_node.length / 2, 0);
				quad_node.sub_node[0] = buildNodeList(sub_node_0, matProj, eye_pos, screen_area);
				if(quad_node.sub_node[0] < 0) g_QuadNodePool.free(sub_node_0);

				QuadNode sub_node_1 = node(quad_node.bottom_left_x + quad_node.length/2, quad_node.bottom_left_y, quad_node.length / 2, 0);
				quad_node.sub_node[1] = buildNodeList(sub_node_1, matProj, eye_pos, screen_area);
				if(quad_node.sub_node[1] < 0) g_QuadNodePool.free(sub_node_1);
				
				QuadNode sub_node_2 = node(quad_node.bottom_left_x + quad_node.length/2, quad_node.bottom_left_y + quad_node.length/2, quad_node.length / 2, 0);
				quad_node.sub_node[2] = buildNodeList(sub_node_2, matProj, eye_pos, screen_area);
				if(quad_node.sub_node[2] < 0) g_QuadNodePool.free(sub_node_2);
				
				QuadNode sub_node_3 = node(quad_node.bottom_left_x, quad_node.bottom_left_y + quad_node.length/2, quad_node.length / 2, 0);
				quad_node.sub_node[3] = buildNodeList(sub_node_3, matProj, eye_pos, screen_area);
				if(quad_node.sub_node[3] < 0) g_QuadNodePool.free(sub_node_3);
				
				visible = !isLeaf(quad_node);
			}

			if (visible)
			{
				// Estimate mesh LOD
				int lod = 0;
				for (lod = 0; lod < g_Lods - 1; lod++)
				{
					if (min_coverage > g_UpperGridCoverage)
						break;
					min_coverage *= 4;
				}

				// We don't use 1x1 and 2x2 patch. So the highest level is g_Lods - 2.
				quad_node.lod = Math.min(lod, g_Lods - 2);
			}
			else
				return -1;

			// Insert into the list
			int position = g_render_list.size();
			g_render_list.add(quad_node);

			return position;
		}
	
		static float sin(float angle) { return (float)Math.sin(angle);}
		static float cos(float angle) { return (float)Math.cos(angle);}
		
		QuadNode node(float left_x, float left_y, float len, int lod){
			QuadNode node = g_QuadNodePool.obtain();
			node.set(left_x, left_y, len, lod);
			return node;
		}
		
	private static final class Rect{
		int left, top, right, bottom;
		public Rect() {}
		public Rect(int left, int top, int right, int bottom) {
			this.left = left;
			this.top = top;
			this.right = right;
			this.bottom = bottom;
		}
	}

	
}
