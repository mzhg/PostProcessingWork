package assimp.importer.blender;

import assimp.common.DefaultLogger;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.Subdivider;

final class BlenderModifier_Subdivision extends BlenderModifier{

	@Override
	boolean isActive(ModifierData md) {
		return md.type == ModifierData.eModifierType_Subsurf;
	}
	
	@Override
	void doIt(Node out, ConversionData conv_data, ElemBase orig_modifier,
			BLEScene in, BLEObject orig_object) {
		// hijacking the ABI, see the big note in BlenderModifierShowcase::ApplyModifiers()
		SubsurfModifierData mir = (SubsurfModifierData)(orig_modifier);
//		ai_assert(mir.modifier.type == ModifierData::eModifierType_Subsurf);

		int algo;
		switch (mir.subdivType) 
		{
		case SubsurfModifierData.TYPE_CatmullClarke:
			algo = Subdivider.CATMULL_CLARKE;
			break;

		case SubsurfModifierData.TYPE_Simple:
			DefaultLogger.warn("BlendModifier: The `SIMPLE` subdivision algorithm is not currently implemented, using Catmull-Clarke");
			algo = Subdivider.CATMULL_CLARKE;
			break;

		default:
			DefaultLogger.warn("BlendModifier: Unrecognized subdivision algorithm: " + mir.subdivType);
			return;
		};

//		boost::scoped_ptr<Subdivider> subd(Subdivider::Create(algo));
//		ai_assert(subd);
		Subdivider subd = Subdivider.create(algo);

//		aiMesh** const meshes = &conv_data.meshes[conv_data.meshes->size() - out.mNumMeshes];
//		boost::scoped_array<aiMesh*> tempmeshes(new aiMesh*[out.mNumMeshes]());
//
//		subd->Subdivide(meshes,out.mNumMeshes,tempmeshes.get(),std::max( mir.renderLevels, mir.levels ),true);
//		std::copy(tempmeshes.get(),tempmeshes.get()+out.mNumMeshes,meshes);
//
//		ASSIMP_LOG_INFO_F("BlendModifier: Applied the `Subdivision` modifier to `",
//			orig_object.id.name,"`");
		final int numMeshes = out.getNumMeshes();
		final Mesh[] meshes = new Mesh[numMeshes];
		final int offset = conv_data.meshes.size() - numMeshes;
		for(int i = 0; i < meshes.length; i++){
			meshes[i] = conv_data.meshes.get(i + offset);
		}
		
		Mesh[] tempmeshes = new Mesh[out.getNumMeshes()];
		subd.subdivide(meshes, numMeshes, tempmeshes, Math.max(mir.renderLevels, mir.levels), true);
		
		for(int i = offset; i < conv_data.meshes.size(); i++)
			conv_data.meshes.set(i, tempmeshes[i - offset]);
		
		if(DefaultLogger.LOG_OUT)
			DefaultLogger.info("BlendModifier: Applied the `Subdivision` modifier to `" +
			orig_object.id.name + "`");
	}
}
