package assimp.importer.blender;

final class FactoryPair {

	AllocProcPtr first;
	ConvertProcPtr second;
	
	public FactoryPair() {
	}

	public FactoryPair(AllocProcPtr first, ConvertProcPtr second) {
		this.first = first;
		this.second = second;
	}
	
	interface ConvertProcPtr{
		void call(Structure s, ElemBase in, FileDatabase fdb);
	}
	
	interface AllocProcPtr{
		ElemBase call();
	}
}
