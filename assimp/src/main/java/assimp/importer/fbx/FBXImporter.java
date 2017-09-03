package assimp.importer.fbx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.DeadlyImportError;
import assimp.common.FileUtils;
import assimp.common.Importer;
import assimp.common.ImporterDesc;
import assimp.common.Scene;

public class FBXImporter extends BaseImporter{
	static final ImporterDesc desc = new ImporterDesc(
		"Autodesk FBX Importer",
		"",
		"",
		"",
		ImporterDesc.aiImporterFlags_SupportTextFlavour,
		0,
		0,
		0,
		0,
		"fbx" 
	);
	
	final ImportSettings settings = new ImportSettings();
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) throws IOException {
		String extension = getExtension(pFile);
		if (extension.equals("fbx")) {
			return true;
		}

		else if ((extension.length() == 0|| checkSig) && pIOHandler != null)	{
			// at least ascii FBX files usually have a 'FBX' somewhere in their head
			String tokens[] = {"FBX"};
			return searchFileHeaderForToken(pIOHandler,pFile,tokens);
		}
		return false;
	}
	
	@Override
	public void setupProperties(Importer pImp) {
		settings.readAllLayers = pImp.getPropertyBoolean(AssimpConfig.AI_CONFIG_IMPORT_FBX_READ_ALL_GEOMETRY_LAYERS, true);
		settings.readAllMaterials = pImp.getPropertyBoolean(AssimpConfig.AI_CONFIG_IMPORT_FBX_READ_ALL_MATERIALS, false);
		settings.readMaterials = pImp.getPropertyBoolean(AssimpConfig.AI_CONFIG_IMPORT_FBX_READ_MATERIALS, true);
		settings.readCameras = pImp.getPropertyBoolean(AssimpConfig.AI_CONFIG_IMPORT_FBX_READ_CAMERAS, true);
		settings.readLights = pImp.getPropertyBoolean(AssimpConfig.AI_CONFIG_IMPORT_FBX_READ_LIGHTS, true);
		settings.readAnimations = pImp.getPropertyBoolean(AssimpConfig.AI_CONFIG_IMPORT_FBX_READ_ANIMATIONS, true);
		settings.strictMode = pImp.getPropertyBoolean(AssimpConfig.AI_CONFIG_IMPORT_FBX_STRICT_MODE, false);
		settings.preservePivots = pImp.getPropertyBoolean(AssimpConfig.AI_CONFIG_IMPORT_FBX_PRESERVE_PIVOTS, true);
		settings.optimizeEmptyAnimationCurves = pImp.getPropertyBoolean(AssimpConfig.AI_CONFIG_IMPORT_FBX_OPTIMIZE_EMPTY_ANIMATION_CURVES, true);
	}

	@Override
	protected ImporterDesc getInfo() { return desc;}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		// read entire file into memory - no streaming for this, fbx
		// files can grow large, but the assimp output data structure
		// then becomes very large, too. Assimp doesn't support
		// streaming for its output data structures so the net win with
		// streaming input data would be very low.
		ByteBuffer contents = FileUtils.loadText(pFile, false, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
		// broadphase tokenizing pass in which we identify the core
		// syntax elements of FBX (brackets, commas, key:value mappings)
		ArrayList<Token> tokens = new ArrayList<Token>(); 
		try {

			boolean is_binary = false;
			if (AssUtil.equals(contents,"Kaydara FBX Binary", 0, 18)) {
				is_binary = true;
				FBXBinaryTokenizer.tokenizeBinary(tokens,contents);
			}
			else {
				FBXTokenizer.tokenize(tokens,contents);
			}

			// use this information to construct a very rudimentary 
			// parse-tree representing the FBX scope structure
			Parser parser = new Parser(tokens, is_binary);

			// take the raw parse-tree and convert it to a FBX DOM
			Document doc = new Document(parser,settings);

			// convert the FBX DOM to aiScene
			FBXConverter.convertToAssimpScene(pScene,doc);
		}
		catch(Exception e) {
//			std::for_each(tokens.begin(),tokens.end(),Util::delete_fun<Token>());
//			throw;
			throw new DeadlyImportError(e);
		}
	}

}
