package assimp.importer.mdl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.DefaultLogger;
import assimp.common.ImporterDesc;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Scene;
import assimp.common.ShadingMode;
import assimp.common.Texture;
import assimp.common.TextureType;

public class MDLImporter extends BaseImporter{
	
	static final int	AI_MDL7_SKINTYPE_MIPFLAG		 = 0x08;
	static final int	AI_MDL7_SKINTYPE_MATERIAL		 = 0x10;
	static final int	AI_MDL7_SKINTYPE_MATERIAL_ASCDEF = 0x20;
	static final int	AI_MDL7_SKINTYPE_RGBFLAG		 = 0x80;
	static final String AI_MDL7_REFERRER_MATERIAL 		 = "&&&referrer&&&";

	private static final int RGBA4_MASK = 0B1111;
	private static final IntBuffer bad_texel = IntBuffer.wrap(AssUtil.EMPTY_INT);
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler,
			boolean checkSig) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected ImporterDesc getInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		// TODO Auto-generated method stub
		
	}
	
	// Check whether we can replace a texture with a single color
	static void replaceTextureWithColor(Texture pcTexture, Vector4f clrOut)
	{
		clrOut.x = Float.NaN;
		if (pcTexture.mHeight == 0 || pcTexture.mWidth == 0)
			return;

		final int iNumPixels = pcTexture.mHeight*pcTexture.mWidth;
//		const aiTexel* pcTexel = pcTexture->pcData+1;
//		const aiTexel* const pcTexelEnd = &pcTexture->pcData[iNumPixels];
		int pcTexel = 1;
		final int pcTexelEnd = /*pcTexture.pcData.limit()*/iNumPixels;
		IntBuffer buf = pcTexture.pcData;
		while (pcTexel != pcTexelEnd)
		{
			if (buf.get(pcTexel)!= buf.get((pcTexel-1)))
			{
				pcTexel = -1;
				break;
			}
			++pcTexel;
		}
		if (pcTexel >= 0)
		{
//			clrOut.r = pcTexture->pcData->r / 255.0f;
//			clrOut.g = pcTexture->pcData->g / 255.0f;
//			clrOut.b = pcTexture->pcData->b / 255.0f;
//			clrOut.a = pcTexture->pcData->a / 255.0f;
			AssUtil.convertColor(pcTexture.pcData.get(0), clrOut);
		}
	}
	
	// Get a skin from a MDL7 file - more complex than all other subformats
	public static void parseSkinLump_3DGS_MDL7(ByteBuffer szCurrent, Scene pScene,
//		const unsigned char* szCurrent,
//		const unsigned char** szCurrentOut,
		Material pcMatOut, int iType, int iWidth, int iHeight)
	{
		Texture pcNew = null;

		// get the type of the skin
		int iMasked = (iType & 0xF);

		if (0x1 ==  iMasked)
		{
			// ***** REFERENCE TO ANOTHER SKIN INDEX *****
			int referrer = (int)iWidth;
			pcMatOut.addProperty(referrer,AI_MDL7_REFERRER_MATERIAL, 0,0);
		}
		else if (0x6 == iMasked)
		{
			// ***** EMBEDDED DDS FILE *****
			if (1 != iHeight)
			{
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("Found a reference to an embedded DDS texture, but texture height is not equal to 1, which is not supported by MED");
			}

			pcNew = new Texture();
			pcNew.mHeight = 0;
			pcNew.mWidth = iWidth;

			// place a proper format hint
//			pcNew.achFormatHint[0] = 'd';
//			pcNew.achFormatHint[1] = 'd';
//			pcNew.achFormatHint[2] = 's';
//			pcNew.achFormatHint[3] = '\0';
			pcNew.achFormatHint = "dds";

//			pcNew.pcData = (aiTexel*) new unsigned char[pcNew.mWidth];
//			memcpy(pcNew.pcData,szCurrent,pcNew.mWidth);
			ByteBuffer data = MemoryUtil.createByteBuffer(pcNew.mWidth, AssimpConfig.MESH_USE_NATIVE_MEMORY);
			int old_limit = szCurrent.limit();
			szCurrent.limit(szCurrent.position() + data.remaining());
			data.put(szCurrent).flip();
			szCurrent.limit(old_limit);
			pcNew.pcData = data.asIntBuffer();
//			szCurrent += iWidth;
		}
		if (0x7 == iMasked)
		{
			// ***** REFERENCE TO EXTERNAL FILE *****
			if (1 != iHeight)
			{
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("Found a reference to an external texture, but texture height is not equal to 1, which is not supported by MED");
			}

//			aiString szFile;
//			const size_t iLen = strlen((const char*)szCurrent);
//			size_t iLen2 = iLen+1;
//			iLen2 = iLen2 > MAXLEN ? MAXLEN : iLen2;
//			memcpy(szFile.data,(const char*)szCurrent,iLen2);
//			szFile.length = iLen;
//
//			szCurrent += iLen2;
			int pos = szCurrent.position();
			while(szCurrent.get() != 0);
			byte[] bytes = new byte[szCurrent.position() - pos];
			szCurrent.position(pos);
			szCurrent.get(bytes);
			szCurrent.get();  // skip the ' ' character.
			// place this as diffuse texture
//			pcMatOut.addProperty(szFile,AI_MATKEY_TEXTURE_DIFFUSE(0));
			pcMatOut.addProperty(new String(bytes), Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_DIFFUSE.ordinal(), 0);
		}
		else if (iMasked!= 0 ||iType == 0 || (iType!=0 && iWidth!=0 && iHeight!=0))
		{
			// ***** STANDARD COLOR TEXTURE *****
			pcNew = new Texture();
			if (iHeight==0 || iWidth == 0)
			{
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("Found embedded texture, but its width an height are both 0. Is this a joke?");

				// generate an empty chess pattern
				pcNew.mWidth = pcNew.mHeight = 8;
				pcNew.pcData = MemoryUtil.createIntBuffer(64, AssimpConfig.MESH_USE_NATIVE_MEMORY);  //new aiTexel[64];
				for (int x = 0; x < 8;++x)
				{
					for (int y = 0; y < 8;++y)
					{
						final boolean bSet = ((0 == x % 2 && 0 != y % 2) ||
							(0 != x % 2 && 0 == y % 2));
					
//						aiTexel* pc = &pcNew.pcData[y * 8 + x];
//						pc->r = pc->b = pc->g = (bSet?0xFF:0);
//						pc->a = 0xFF;
						int r = bSet ? 0xFF : 0;
						int a = 0xFF;
						pcNew.pcData.put(y * 8 + x, AssUtil.makefourcc(r, r, r, a));
					}
				}
			}
			else
			{
				// it is a standard color texture. Fill in width and height
				// and call the same function we used for loading MDL5 files

				pcNew.mWidth = iWidth;
				pcNew.mHeight = iHeight;

				int iSkip = parseTextureColorData(szCurrent,iMasked,pcNew);

				// skip length of texture data
//				szCurrent += iSkip;
				szCurrent.position(iSkip + szCurrent.position());
			}
		}

		// sometimes there are MDL7 files which have a monochrome
		// texture instead of material colors ... posssible they have
		// been converted to MDL7 from other formats, such as MDL5
		Vector4f clrTexture = new Vector4f();
		if (pcNew != null)replaceTextureWithColor(pcNew, clrTexture);
		else clrTexture.x = Float.NaN;
		
		// check whether a material definition is contained in the skin
		if ((iType & AI_MDL7_SKINTYPE_MATERIAL) !=0)
		{
//			BE_NCONST MDL::Material_MDL7* pcMatIn = (BE_NCONST MDL::Material_MDL7*)szCurrent;
//			szCurrent = (unsigned char*)(pcMatIn+1);
//			VALIDATE_FILE_SIZE(szCurrent);
			Material_MDL7 pcMatIn = new Material_MDL7().load(szCurrent);
			
			Vector3f clrTemp = new Vector3f();
			
//	#define COLOR_MULTIPLY_RGB() \
//		if (is_not_qnan(clrTexture.r)) \
//			{ \
//			clrTemp.r *= clrTexture.r; \
//			clrTemp.g *= clrTexture.g; \
//			clrTemp.b *= clrTexture.b; \
//			}

			// read diffuse color
			clrTemp.x = pcMatIn.diffuse.x;
//			AI_SWAP4(clrTemp.r);  
			clrTemp.y = pcMatIn.diffuse.y;
//			AI_SWAP4(clrTemp.g);  
			clrTemp.z = pcMatIn.diffuse.z;
//			AI_SWAP4(clrTemp.b);  
//			COLOR_MULTIPLY_RGB();
			if(clrTexture.x == clrTexture.x){
				Vector3f.scale(clrTemp, clrTexture, clrTemp);
			}
			pcMatOut.addProperty(clrTemp,Material.AI_MATKEY_COLOR_DIFFUSE,0,0);

			// read specular color
			clrTemp.x = pcMatIn.specular.x;
//			AI_SWAP4(clrTemp.r);  
			clrTemp.y = pcMatIn.specular.y;
//			AI_SWAP4(clrTemp.g);  
			clrTemp.z = pcMatIn.specular.z;
//			AI_SWAP4(clrTemp.b);  
//			COLOR_MULTIPLY_RGB();
			if(clrTexture.x == clrTexture.x){
				Vector3f.scale(clrTemp, clrTexture, clrTemp);
			}
			pcMatOut.addProperty(clrTemp, Material.AI_MATKEY_COLOR_SPECULAR,0,0);

			// read ambient color
			clrTemp.x = pcMatIn.ambient.x;
//			AI_SWAP4(clrTemp.r);  
			clrTemp.y = pcMatIn.ambient.y;
//			AI_SWAP4(clrTemp.g);  
			clrTemp.z = pcMatIn.ambient.z;
//			AI_SWAP4(clrTemp.b);  
//			COLOR_MULTIPLY_RGB();
			if(clrTexture.x == clrTexture.x){
				Vector3f.scale(clrTemp, clrTexture, clrTemp);
			}
			pcMatOut.addProperty(clrTemp, Material.AI_MATKEY_COLOR_AMBIENT,0,0);

			// read emissive color
			clrTemp.x = pcMatIn.emissive.x;
//			AI_SWAP4(clrTemp.r);  
			clrTemp.y = pcMatIn.emissive.y;
//			AI_SWAP4(clrTemp.g);  
			clrTemp.z = pcMatIn.emissive.z;
//			AI_SWAP4(clrTemp.b);  
			pcMatOut.addProperty(clrTemp,Material.AI_MATKEY_COLOR_EMISSIVE,0,0);

//	#undef COLOR_MULITPLY_RGB

			// FIX: Take the opacity from the ambient color.
			// The doc say something else, but it is fact that MED exports the
			// opacity like this .... oh well.
			clrTemp.x = pcMatIn.ambient.w;
//			AI_SWAP4(clrTemp.r);  
			if (/*is_not_qnan*/clrTexture.x == clrTexture.x) {
				clrTemp.x *= clrTexture.w;
			}
			pcMatOut.addProperty(clrTemp.x,Material.AI_MATKEY_OPACITY,0,0);

			// read phong power
			int iShadingMode = ShadingMode.aiShadingMode_Gouraud.ordinal();
//			AI_SWAP4(pcMatIn->Power);  
			if (0.0f != pcMatIn.power)
			{
				iShadingMode = ShadingMode.aiShadingMode_Phong.ordinal();
				pcMatOut.addProperty(pcMatIn.power,Material.AI_MATKEY_SHININESS,0,0);
			}
			pcMatOut.addProperty(iShadingMode,Material.AI_MATKEY_SHADING_MODEL,0,0);
		}
		else if (/*is_not_qnan()*/ clrTexture.x == clrTexture.x)
		{
			pcMatOut.addProperty(clrTexture,Material.AI_MATKEY_COLOR_DIFFUSE,0,0);
			pcMatOut.addProperty(clrTexture,Material.AI_MATKEY_COLOR_SPECULAR,0,0);
		}
		// if the texture could be replaced by a single material color
		// we don't need the texture anymore
		if (/*is_not_qnan(clrTexture.r)*/ clrTexture.x==clrTexture.x)
		{
//			delete pcNew;
			pcNew = null;
		}

		// If an ASCII effect description (HLSL?) is contained in the file,
		// we can simply ignore it ...
		if ((iType & AI_MDL7_SKINTYPE_MATERIAL_ASCDEF) != 0)
		{
//			VALIDATE_FILE_SIZE(szCurrent);
//			int32_t iMe = *((int32_t*)szCurrent);
//			AI_SWAP4(iMe);  
//			szCurrent += sizeof(char) * iMe + sizeof(int32_t);
//			VALIDATE_FILE_SIZE(szCurrent);
			int iMe = szCurrent.getInt();
			szCurrent.position(szCurrent.position() + iMe);
		}

		// If an embedded texture has been loaded setup the corresponding
		// data structures in the aiScene instance
		if (pcNew != null && pScene.getNumTextures() <= 999)
		{

			// place this as diffuse texture
//			char szCurrent[5];
//			::sprintf(szCurrent,"*%i",this->pScene->mNumTextures);
//
//			aiString szFile;
//			const size_t iLen = strlen((const char*)szCurrent);
//			::memcpy(szFile.data,(const char*)szCurrent,iLen+1);
//			szFile.length = iLen;
			
			String szFile = "*" + pScene.getNumTextures();
			pcMatOut.addProperty(szFile,Material. _AI_MATKEY_TEXTURE_BASE,TextureType.aiTextureType_DIFFUSE.ordinal(), 0);

			// store the texture
//			if (!pScene->mNumTextures)
//			{
//				pScene->mNumTextures = 1;
//				pScene->mTextures = new aiTexture*[1];
//				pScene->mTextures[0] = pcNew;
//			}
//			else
//			{
//				aiTexture** pc = pScene->mTextures;
//				pScene->mTextures = new aiTexture*[pScene->mNumTextures+1];
//				for (unsigned int i = 0; i < pScene->mNumTextures;++i) {
//					pScene->mTextures[i] = pc[i];
//				}
//
//				pScene->mTextures[pScene->mNumTextures] = pcNew;
//				pScene->mNumTextures++;
//				delete[] pc;
//			}
			
			if(pScene.mTextures == null){
				pScene.mTextures = new Texture[]{pcNew};
			}else{
				pScene.mTextures = Arrays.copyOf(pScene.mTextures, pScene.mTextures.length + 1);
				pScene.mTextures[pScene.mTextures.length - 1] = pcNew;
			}
		}
//		VALIDATE_FILE_SIZE(szCurrent);
	}
	
	public static void skipSkinLump_3DGS_MDL7(ByteBuffer szCurrent,/*
			const unsigned char* szCurrent,
			const unsigned char** szCurrentOut,*/
			int iType, int iWidth, int iHeight)
		{
			// get the type of the skin
			final int iMasked = (iType & 0xF);

			if (0x6 == iMasked)
			{
				szCurrent.position(szCurrent.position() + iWidth);
			}
			if (0x7 == iMasked)
			{
//				const size_t iLen = ::strlen((const char*)szCurrent);
//				szCurrent += iLen+1;
				szCurrent.position(szCurrent.limit());
			}
			else if (iMasked!=0 || iType == 0)
			{
				if (iMasked!=0 || iType == 0 || (iType!=0 && iWidth!=0 && iHeight!=0))
				{
					// ParseTextureColorData(..., aiTexture::pcData == bad_texel) will simply
					// return the size of the color data in bytes in iSkip
//					int iSkip = 0;

					Texture tex = new Texture();
					tex.pcData = bad_texel;
					tex.mHeight = iHeight;
					tex.mWidth = iWidth;
					int iSkip = parseTextureColorData(szCurrent,iMasked,tex);

					// FIX: Important, otherwise the destructor will crash
					tex.pcData = null;

					// skip length of texture data
					szCurrent.position(iSkip + szCurrent.position());
				}
			}

			// check whether a material definition is contained in the skin
			if ((iType & AI_MDL7_SKINTYPE_MATERIAL) !=0)
			{
//				BE_NCONST MDL::Material_MDL7* pcMatIn = (BE_NCONST MDL::Material_MDL7*)szCurrent;
//				szCurrent = (unsigned char*)(pcMatIn+1);
				szCurrent.position(szCurrent.position() + Material_MDL7.SIZE);
			}

			// if an ASCII effect description (HLSL?) is contained in the file,
			// we can simply ignore it ...
			if ((iType & AI_MDL7_SKINTYPE_MATERIAL_ASCDEF) !=0)
			{
//				int32_t iMe = *((int32_t*)szCurrent);
//				AI_SWAP4(iMe);  
//				szCurrent += sizeof(char) * iMe + sizeof(int32_t);
				int iMe = szCurrent.getInt();
				szCurrent.position(szCurrent.position() + iMe);
			}
//			*szCurrentOut = szCurrent;
		}

	// ------------------------------------------------------------------------------------------------
	// Load color data of a texture and convert it to our output format
	public static int parseTextureColorData(ByteBuffer szData, int iType,Texture pcNew)
	{
		int piSkip = 0;
		final boolean do_read = (bad_texel != pcNew.pcData);
		int old_pos = szData.position();
		// allocate storage for the texture image
		if (do_read) {
			pcNew.pcData = MemoryUtil.createIntBuffer(pcNew.mWidth * pcNew.mHeight, AssimpConfig.MESH_USE_NATIVE_MEMORY);
		}

		// R5G6B5 format (with or without MIPs)
		// ****************************************************************
		if (2 == iType || 10 == iType)
		{
//			VALIDATE_FILE_SIZE(szData + pcNew.mWidth*pcNew.mHeight*2); TODO

			// copy texture data
			int i;
			if (do_read) 
			{
				RGB565 val = new RGB565();
				for (i = 0; i < pcNew.mWidth*pcNew.mHeight;++i)
				{
//					MDL::RGB565 val = ((MDL::RGB565*)szData)[i];
//					AI_SWAP2(val);    
//
//					pcNew.pcData[i].a = 0xFF;
//					pcNew.pcData[i].r = (unsigned char)val.b << 3;
//					pcNew.pcData[i].g = (unsigned char)val.g << 2;
//					pcNew.pcData[i].b = (unsigned char)val.r << 3;
					val.set(szData.getShort());
					int r = val.getR() << 3;
					int g = val.getG() << 2;
					int b = val.getR() << 3;
					pcNew.pcData.put(AssUtil.makefourcc(r, g, b, 0xFF));  //TODO RGBA -- ARGB 
				}
			} 
			else i = pcNew.mWidth*pcNew.mHeight;
			piSkip = i * 2;

			// apply MIP maps
			if (10 == iType)
			{
				piSkip += ((i >> 2) + (i >> 4) + (i >> 6)) << 1;
//				VALIDATE_FILE_SIZE(szData + *piSkip);  TODO
			}
		}
		// ARGB4 format (with or without MIPs)
		// ****************************************************************
		else if (3 == iType || 11 == iType)
		{
//			VALIDATE_FILE_SIZE(szData + pcNew.mWidth*pcNew.mHeight*4); TODO

			// copy texture data
			int i;
			if (do_read) 
			{
				for (i = 0; i < pcNew.mWidth*pcNew.mHeight;++i)
				{
//					MDL::ARGB4 val = ((MDL::ARGB4*)szData)[i];
//					AI_SWAP2(val);    
					short val = szData.getShort();
//					pcNew.pcData[i].a = (unsigned char)val.a << 4;
//					pcNew.pcData[i].r = (unsigned char)val.r << 4;
//					pcNew.pcData[i].g = (unsigned char)val.g << 4;
//					pcNew.pcData[i].b = (unsigned char)val.b << 4;
					int a = (val & RGBA4_MASK) << 4;
					int r = ((val >> 4) & RGBA4_MASK) << 4;
					int g = ((val >> 8) & RGBA4_MASK) << 4;
					int b = ((val >> 12) & RGBA4_MASK) << 4;
					pcNew.pcData.put(AssUtil.makefourcc(r, g, b, a)); //TODO RGBA -- ARGB
				}
			}
			else i = pcNew.mWidth*pcNew.mHeight;
			piSkip = i * 2;

			// apply MIP maps
			if (11 == iType)
			{
				piSkip += ((i >> 2) + (i >> 4) + (i >> 6)) << 1;
//				VALIDATE_FILE_SIZE(szData + *piSkip);
			}
		}
		// RGB8 format (with or without MIPs)
		// ****************************************************************
		else if (4 == iType || 12 == iType)
		{
//			VALIDATE_FILE_SIZE(szData + pcNew.mWidth*pcNew.mHeight*3);

			// copy texture data
			int i;
			if (do_read)
			{
				for (i = 0; i < pcNew.mWidth*pcNew.mHeight;++i)
				{
//					const unsigned char* _szData = &szData[i*3];
//					int _szData = szData.position() + i * 3;

//					pcNew.pcData[i].a = 0xFF;
//					pcNew.pcData[i].b = *_szData++;
//					pcNew.pcData[i].g = *_szData++;
//					pcNew.pcData[i].r = *_szData;
					int a = 0xFF;
					int b = szData.get(/*_szData++*/) & 0xFF;
					int g = szData.get(/*_szData++*/) & 0xFF;
					int r = szData.get(/*_szData*/) & 0xFF;
					pcNew.pcData.put(AssUtil.makefourcc(r, g, b, a)); //TODO RGBA -- ABGR
				}
			} 
			else i = pcNew.mWidth*pcNew.mHeight;


			// apply MIP maps
			piSkip = i * 3;
			if (12 == iType)
			{
				piSkip += ((i >> 2) + (i >> 4) + (i >> 6)) *3;
//				VALIDATE_FILE_SIZE(szData + *piSkip);
			}
		}
		// ARGB8 format (with ir without MIPs)
		// ****************************************************************
		else if (5 == iType || 13 == iType)
		{
//			VALIDATE_FILE_SIZE(szData + pcNew.mWidth*pcNew.mHeight*4);

			// copy texture data
			 int i;
			if (do_read)
			{
				for (i = 0; i < pcNew.mWidth*pcNew.mHeight;++i)
				{
//					const unsigned char* _szData = &szData[i*4];
//
//					pcNew.pcData[i].b = *_szData++;
//					pcNew.pcData[i].g = *_szData++;
//					pcNew.pcData[i].r = *_szData++;
//					pcNew.pcData[i].a = *_szData;
					
					int b = szData.get() & 0xFF;
					int g = szData.get() & 0xFF;
					int r = szData.get() & 0xFF;
					int a = szData.get() & 0xFF;
					pcNew.pcData.put(AssUtil.makefourcc(r, g, b, a)); //TODO
				}
			} 
			else i = pcNew.mWidth*pcNew.mHeight;

			// apply MIP maps
			piSkip = i << 2;
			if (13 == iType)
			{
				piSkip += ((i >> 2) + (i >> 4) + (i >> 6)) << 2;
			}
		}
		// palletized 8 bit texture. As for Quake 1
		// ****************************************************************
		else if (0 == iType)
		{
//			VALIDATE_FILE_SIZE(szData + pcNew.mWidth*pcNew.mHeight);

			// copy texture data
//			int i;  TODO doesn't support the color palette.
//			if (do_read) 
//			{
//
//				const unsigned char* szColorMap;
//				SearchPalette(&szColorMap);
//
//				for (i = 0; i < pcNew.mWidth*pcNew.mHeight;++i)
//				{
//					const unsigned char val = szData[i];
//					const unsigned char* sz = &szColorMap[val*3];
//
//					pcNew.pcData[i].a = 0xFF;
//					pcNew.pcData[i].r = *sz++;
//					pcNew.pcData[i].g = *sz++;
//					pcNew.pcData[i].b = *sz;
//				}
//				this->FreePalette(szColorMap);
//
//			} 
//			else i = pcNew.mWidth*pcNew.mHeight;
//			*piSkip = i;

			// FIXME: Also support for MIP maps?
		}
		
		szData.position(old_pos);
		return piSkip;
	}
}
