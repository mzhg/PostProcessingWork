package jet.opengl.demos.scenes.outdoor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import jet.opengl.postprocessing.util.FileUtils;

final class Config {

	static final String CONFIG_PATH = "Scenes/Outdoor/";
	
	static void parseConfigurationFile(OutDoorScene demo, boolean out_print){
		String filename = CONFIG_PATH + "Default_Config.txt";
		Properties properties = new Properties();
		
		try (InputStream in = FileUtils.open(filename)){
			properties.load(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		demo.m_strRawDEMDataFile = CONFIG_PATH + properties.getProperty("RawDEMDataFile");
		demo.m_strMtrlMaskFile = CONFIG_PATH + properties.getProperty("MaterialMaskFile");
		
		if(out_print){
			System.out.println("RawDEMDataFile=" + demo.m_strRawDEMDataFile);
			System.out.println("MaterialMaskFile=" + demo.m_strMtrlMaskFile);
		}
		
		String texturePrefix = "TileTexture";
		for(int i = 0; i < CEarthHemsiphere.NUM_TILE_TEXTURES; i++){
			String name = texturePrefix + i;
			String value = properties.getProperty(name);
			if(value == null)
				break;
			
			demo.m_strTileTexPaths[i] = CONFIG_PATH + value;
			
			if(out_print){
				System.out.println(name+ "=" + demo.m_strTileTexPaths[i]);
			}
		}
		
		String tileNormalPrefix = "TileNormalMap";
		for(int i = 0; i < CEarthHemsiphere.NUM_TILE_TEXTURES; i++){
			String name = tileNormalPrefix + i;
			String value = properties.getProperty(name);
			if(value == null)
				break;
			
			demo.m_strNormalMapTexPaths[i] = CONFIG_PATH + value;
			if(out_print){
				System.out.println(name+ "=" + demo.m_strNormalMapTexPaths[i]);
			}
		}
		
		String tileScalePrefix = "TilingScale";
		for(int i = 0; i < CEarthHemsiphere.NUM_TILE_TEXTURES; i++){
			String name = tileScalePrefix + i;
			String value = properties.getProperty(name);
			if(value == null )
				break;
			
			if(i == 0){
				demo.m_TerrainRenderParams.m_TerrainAttribs.m_fBaseMtrlTilingScale = Float.parseFloat(value);
			}else{
				demo.m_TerrainRenderParams.m_TerrainAttribs.m_f4TilingScale.setValue(i - 1, Float.parseFloat(value));
			}
			
			if(out_print){
				System.out.println(name+ "=" + value);
			}
		}
		
		String textureMode = properties.getProperty("TexturingMode");
		switch (textureMode) {
		case "HeightBased":
			demo.m_TerrainRenderParams.m_TexturingMode = SRenderingParams.TM_HEIGHT_BASED;
			break;
		case "MaterialMask":
			demo.m_TerrainRenderParams.m_TexturingMode = SRenderingParams.TM_MATERIAL_MASK;
			break;
		case "MaterialMaskNM":
			demo.m_TerrainRenderParams.m_TexturingMode = SRenderingParams.TM_MATERIAL_MASK_NM;
			break;
		default:
			System.err.println("Unknown texturing mode: " + textureMode);
			break;
		}
		
		if(out_print){
			System.out.println("TexturingMode=" + textureMode);
		}
		
		demo.m_TerrainRenderParams.m_TerrainAttribs.m_fElevationSamplingInterval = Float.parseFloat(properties.getProperty("ElevationSamplingInterval"));
		demo.m_TerrainRenderParams.m_iRingDimension = Integer.parseInt(properties.getProperty("RingDimension"));
		demo.m_TerrainRenderParams.m_iNumRings = Integer.parseInt(properties.getProperty("NumRings"));
		demo.m_TerrainRenderParams.m_iColOffset = Integer.parseInt(properties.getProperty("ColOffset"));
		demo.m_TerrainRenderParams.m_iRowOffset = Integer.parseInt(properties.getProperty("RowOffset"));
		demo.m_bAnimateSun = Boolean.parseBoolean(properties.getProperty("AnimateSun"));
		
		if(out_print){
			System.out.println("ElevationSamplingInterval=" + demo.m_TerrainRenderParams.m_TerrainAttribs.m_fElevationSamplingInterval);
			System.out.println("RingDimension=" + demo.m_TerrainRenderParams.m_iRingDimension);
			System.out.println("NumRings=" + demo.m_TerrainRenderParams.m_iNumRings);
			System.out.println("ColOffset=" + demo.m_TerrainRenderParams.m_iColOffset);
			System.out.println("RowOffset=" + demo.m_TerrainRenderParams.m_iRowOffset);
			System.out.println("AnimateSun=" + demo.m_bAnimateSun);
		}
	}
}
