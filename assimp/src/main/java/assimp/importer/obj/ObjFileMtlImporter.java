package assimp.importer.obj;

import java.nio.ByteBuffer;
import java.util.StringTokenizer;

import org.lwjgl.util.vector.Vector3f;

import assimp.common.AssUtil;
import assimp.common.DefaultLogger;
import assimp.common.Material;

final class ObjFileMtlImporter extends ObjTools{
	
	// Material specific token
	static final String DiffuseTexture      = "map_kd";
	static final String AmbientTexture      = "map_ka";
	static final String SpecularTexture     = "map_ks";
	static final String OpacityTexture      = "map_d";
	static final String BumpTexture1        = "map_bump";
	static final String BumpTexture2        = "map_Bump";
	static final String BumpTexture3        = "bump";
	static final String NormalTexture       = "map_Kn";
	static final String DisplacementTexture = "disp";
	static final String SpecularityTexture  = "map_ns";

	// texture option specific token
	static final String BlendUOption		= "-blendu";
	static final String BlendVOption		= "-blendv";
	static final String BoostOption		= "-boost";
	static final String ModifyMapOption	= "-mm";
	static final String OffsetOption		= "-o";
	static final String ScaleOption		= "-s";
	static final String TurbulenceOption	= "-t";
	static final String ResolutionOption	= "-texres";
	static final String ClampOption		= "-clamp";
	static final String BumpOption			= "-bm";
	static final String ChannelOption		= "-imfchan";
	static final String TypeOption			= "-type";

	
	//!	USed model instance
	Model m_pModel;
	final float[] value = new float[1];
		
	public ObjFileMtlImporter(ByteBuffer data, Model pModel) {
		super(data);
		m_pModel = pModel;
		
		if(m_pModel.m_pDefaultMaterial == null){
			m_pModel.m_pDefaultMaterial = new ObjMaterial();
			m_pModel.m_pDefaultMaterial.materialName = "default";
		}
	}
	
	
	///	Load the whole material description
	void load(){
		if ( m_DataIt == m_DataItEnd )
			return;

		while ( m_DataIt != m_DataItEnd )
		{
			switch (data.get(m_DataIt))
			{
			case 'K':
				{
					++m_DataIt;
					byte b = data.get(m_DataIt);
					if (b == 'a') // Ambient color
					{
						++m_DataIt;
						getColorRGBA( m_pModel.m_pCurrentMaterial.ambient );
					}
					else if (b == 'd')	// Diffuse color
					{
						++m_DataIt;
						getColorRGBA( m_pModel.m_pCurrentMaterial.diffuse );
					}
					else if (b == 's')
					{
						++m_DataIt;
						getColorRGBA( m_pModel.m_pCurrentMaterial.specular );
					}
					else if (b == 'e')
					{
						++m_DataIt;
						getColorRGBA( m_pModel.m_pCurrentMaterial.emissive );
					}
					m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
				}
				break;

			case 'd':	// Alpha value
				{
					++m_DataIt;
					m_pModel.m_pCurrentMaterial.alpha = getFloatValue();
					m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
				}
				break;

			case 'N':	// Shineness
				{
					++m_DataIt;
					switch(data.get(m_DataIt)) 
					{
					case 's':
						++m_DataIt;
						m_pModel.m_pCurrentMaterial.shineness = getFloatValue();
						break;
					case 'i': //Index Of refraction 
						++m_DataIt;
						m_pModel.m_pCurrentMaterial.ior = getFloatValue();
						break;
					}
					m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
					break;
				}
			case 'm':	// Texture
			case 'b':   // quick'n'dirty - for 'bump' sections
				{
					getTexture();
					m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
				}
				break;

			case 'n':	// New material name
				{
					createMaterial();
					m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
				}
				break;

			case 'i':	// Illumination model
				{
					m_DataIt = getNextToken(m_DataIt, m_DataItEnd);
					m_pModel.m_pCurrentMaterial.illumination_model = getIlluminationModel();
					m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
				}
				break;

			default:
				{
					m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
				}
				break;
			}
		}
	}
	
	///	Get color data.
	void getColorRGBA(Vector3f pColor){
		m_DataIt = getFloat( m_DataIt, m_DataItEnd, value);
		pColor.x = value[0];
		
		m_DataIt = getFloat( m_DataIt, m_DataItEnd, value);
		pColor.y = value[0];

		m_DataIt = getFloat( m_DataIt, m_DataItEnd, value);
		pColor.z = value[0];
	}
	///	Get illumination model from loaded data
	int getIlluminationModel(/* int &illum_model */){
		m_DataIt = copyNextWord( m_DataIt, m_DataItEnd, m_buffer, BUFFERSIZE );
		return AssUtil.strtoul10(m_buffer, 0, null);
	}
	///	Gets a float value from data.	
	float getFloatValue( /*float &value */){
		m_DataIt = copyNextWord( m_DataIt, m_DataItEnd, m_buffer, BUFFERSIZE );
		return AssUtil.fast_atof(m_buffer);
	}
	///	Creates a new material from loaded data.
	void createMaterial(){
		StringBuilder line = new StringBuilder();
		while ( !isNewLine(data.get(m_DataIt) ) ) {
//			line += *m_DataIt;
			line.append((char)data.get(m_DataIt));
			++m_DataIt;
		}
		
//		std::vector<std::string> token;
//		const unsigned int numToken = tokenize<std::string>( line, token, " " );
//		std::string name( "" );
		StringTokenizer token = new StringTokenizer(line.toString(), " ");
		int numToken = 0;
		String name;
		String secondToken = null;
		if(token.hasMoreTokens()){
			numToken ++;
			if(token.hasMoreElements()){
				numToken ++;
				secondToken = token.nextToken();
			}
		}
		if ( numToken == 1 ) {
//			name = AI_DEFAULT_MATERIAL_NAME;
			name = Material.AI_DEFAULT_MATERIAL_NAME;
		} else {
			name = secondToken;
		}

//		std::map<std::string, ObjMaterial*>::iterator it = m_pModel.m_MaterialMap.find( name );
		ObjMaterial it = m_pModel.m_MaterialMap.get(name);
		if (null == it) {
			// New Material created
			m_pModel.m_pCurrentMaterial = new ObjMaterial();	
			m_pModel.m_pCurrentMaterial.materialName = name;
			m_pModel.m_MaterialLib.add( name );
//			m_pModel.m_MaterialMap[ name ] = m_pModel.m_pCurrentMaterial;
			m_pModel.m_MaterialMap.put(name, m_pModel.m_pCurrentMaterial);
		} else {
			// Use older material
			m_pModel.m_pCurrentMaterial = it;
		}
	}
	
	boolean ASSIMP_strincmp(int str1, String str2, int length){
		for(int i = 0; i < length; i++){
			if(data.get(str1 + i) != str2.charAt(i))
				return true;
		}
		
		return false;
	}
	
	///	Get texture name from loaded data.
	void getTexture(){
//		String out;
		int clampIndex = -1;
		
//		const char *pPtr( &(*m_DataIt) );
		int pPtr = m_DataIt;
		if ( !ASSIMP_strincmp( pPtr, DiffuseTexture, DiffuseTexture.length() ) ) {
			// Diffuse texture
//			out =  m_pModel.m_pCurrentMaterial.texture;
			clampIndex = ObjMaterial.TextureDiffuseType;
		} else if ( !ASSIMP_strincmp( pPtr,AmbientTexture,AmbientTexture.length() ) ) {
			// Ambient texture
//			out =  m_pModel.m_pCurrentMaterial.textureAmbient;
			clampIndex = ObjMaterial.TextureAmbientType;
		} else if (!ASSIMP_strincmp( pPtr, SpecularTexture, SpecularTexture.length())) {
			// Specular texture
//			out =  m_pModel.m_pCurrentMaterial.textureSpecular;
			clampIndex = ObjMaterial.TextureSpecularType;
		} else if ( !ASSIMP_strincmp( pPtr, OpacityTexture, OpacityTexture.length() ) ) {
			// Opacity texture
//			out =  m_pModel.m_pCurrentMaterial.textureOpacity;
			clampIndex = ObjMaterial.TextureOpacityType;
		} else if (!ASSIMP_strincmp( pPtr,"map_ka",6)) {
			// Ambient texture
//			out =  m_pModel.m_pCurrentMaterial.textureAmbient;
			clampIndex = ObjMaterial.TextureAmbientType;
		} else if (!ASSIMP_strincmp(m_DataIt,"map_emissive",6)) {
			// Emissive texture
//			out =  m_pModel.m_pCurrentMaterial.textureEmissive;
			clampIndex = ObjMaterial.TextureEmissiveType;
		} else if ( !ASSIMP_strincmp( pPtr, BumpTexture1, BumpTexture1.length() ) ||
			        !ASSIMP_strincmp( pPtr, BumpTexture2, BumpTexture2.length() ) || 
			        !ASSIMP_strincmp( pPtr, BumpTexture3, BumpTexture3.length() ) ) {
			// Bump texture 
//			out =  m_pModel.m_pCurrentMaterial.textureBump;
			clampIndex = ObjMaterial.TextureBumpType;
		} else if (!ASSIMP_strincmp( pPtr,NormalTexture, NormalTexture.length())) { 
			// Normal map
//			out =  m_pModel.m_pCurrentMaterial.textureNormal;
			clampIndex = ObjMaterial.TextureNormalType;
		} else if (!ASSIMP_strincmp( pPtr, DisplacementTexture, DisplacementTexture.length() ) ) {
			// Displacement texture
//			out = m_pModel.m_pCurrentMaterial.textureDisp;
			clampIndex = ObjMaterial.TextureDispType;
		} else if (!ASSIMP_strincmp( pPtr, SpecularityTexture,SpecularityTexture.length() ) ) {
			// Specularity scaling (glossiness)
//			out =  m_pModel.m_pCurrentMaterial.textureSpecularity;
			clampIndex = ObjMaterial.TextureSpecularityType;
		} else {
			DefaultLogger.error("OBJ/MTL: Encountered unknown texture type");
			return;
		}

		boolean clamp = getTextureOption();
		m_pModel.m_pCurrentMaterial.clamp[clampIndex] = clamp;

		String[] strTexture = new String[1];
		m_DataIt = getName( m_DataIt, m_DataItEnd, strTexture );
		
		switch (clampIndex) {
		case ObjMaterial.TextureDiffuseType:
			m_pModel.m_pCurrentMaterial.texture = strTexture[0];
			break;
		case ObjMaterial.TextureAmbientType:
			m_pModel.m_pCurrentMaterial.textureAmbient = strTexture[0];
			break;
		case ObjMaterial.TextureSpecularType:
			m_pModel.m_pCurrentMaterial.textureSpecular = strTexture[0];
			break;
		case ObjMaterial.TextureOpacityType:
			m_pModel.m_pCurrentMaterial.textureOpacity = strTexture[0];
			break;
		case ObjMaterial.TextureEmissiveType:
			m_pModel.m_pCurrentMaterial.textureEmissive = strTexture[0];
			break;
		case ObjMaterial.TextureBumpType:
			m_pModel.m_pCurrentMaterial.textureBump = strTexture[0];
			break;
		case ObjMaterial.TextureNormalType:
			m_pModel.m_pCurrentMaterial.textureNormal = strTexture[0];
			break;
		case ObjMaterial.TextureDispType:
			m_pModel.m_pCurrentMaterial.textureDisp = strTexture[0];
			break;
		case ObjMaterial.TextureSpecularityType:
			m_pModel.m_pCurrentMaterial.textureSpecularity = strTexture[0];
			break;
		default:
			break;
		}
		
	}
	
	/* /////////////////////////////////////////////////////////////////////////////
	 * Texture Option
	 * /////////////////////////////////////////////////////////////////////////////
	 * According to http://en.wikipedia.org/wiki/Wavefront_.obj_file#Texture_options
	 * Texture map statement can contains various texture option, for example:
	 *
	 *	map_Ka -o 1 1 1 some.png
	 *	map_Kd -clamp on some.png
	 *
	 * So we need to parse and skip these options, and leave the last part which is 
	 * the url of image, otherwise we will get a wrong url like "-clamp on some.png".
	 *
	 * Because aiMaterial supports clamp option, so we also want to return it
	 * /////////////////////////////////////////////////////////////////////////////
	 */
	boolean getTextureOption(){
		boolean clamp = false;;
		m_DataIt = getNextToken(m_DataIt, m_DataItEnd);

		byte[] value = new byte[3];
		//If there is any more texture option
		while (!isEndOfBuffer(m_DataIt, m_DataItEnd) && data.get(m_DataIt) == '-')
		{
//			const char *pPtr( &(*m_DataIt) );
			int pPtr = m_DataIt;
			//skip option key and value
			int skipToken = 1;

			if (!ASSIMP_strincmp(pPtr, ClampOption, ClampOption.length()))
			{
				int it = getNextToken(m_DataIt, m_DataItEnd);
//				char value[3];
				copyNextWord(it, m_DataItEnd, value, 3);
//				if (!ASSIMP_strincmp(value, "on", 2))
//				{
//					clamp = true;
//				}
				
				if(value[0] == 'o' && value[1] == 'n')
					clamp = true;

				skipToken = 2;
			}
			else if (  !ASSIMP_strincmp(pPtr, BlendUOption, BlendUOption.length())
					|| !ASSIMP_strincmp(pPtr, BlendVOption, BlendVOption.length())
					|| !ASSIMP_strincmp(pPtr, BoostOption, BoostOption.length())
					|| !ASSIMP_strincmp(pPtr, ResolutionOption, ResolutionOption.length())
					|| !ASSIMP_strincmp(pPtr, BumpOption, BumpOption.length())
					|| !ASSIMP_strincmp(pPtr, ChannelOption, ChannelOption.length())
					|| !ASSIMP_strincmp(pPtr, TypeOption, TypeOption.length()) )
			{
				skipToken = 2;
			}
			else if (!ASSIMP_strincmp(pPtr, ModifyMapOption, ModifyMapOption.length()))
			{
				skipToken = 3;
			}
			else if (  !ASSIMP_strincmp(pPtr, OffsetOption, OffsetOption.length())
					|| !ASSIMP_strincmp(pPtr, ScaleOption, ScaleOption.length())
					|| !ASSIMP_strincmp(pPtr, TurbulenceOption, TurbulenceOption.length())
					)
			{
				skipToken = 4;
			}

			for (int i = 0; i < skipToken; ++i)
			{
				m_DataIt = getNextToken(m_DataIt, m_DataItEnd);
			}
		}
		
		return clamp;
	}
}
