package assimp.importer.fbx;

import java.util.ArrayList;

final class Skin extends Deformer {

	private float accuracy;
	private ArrayList<Cluster> clusters;
	
	public Skin(long id, Element element, Document doc, String name) {
		super(id, element, doc, name);
	}

	float deformAccuracy() {
		return accuracy;
	}

	ArrayList<Cluster> clusters() {
		return clusters;
	}
}
