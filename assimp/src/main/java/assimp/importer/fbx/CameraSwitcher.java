package assimp.importer.fbx;

import assimp.common.AssUtil;

final class CameraSwitcher extends NodeAttribute{

	private int cameraId;
	private String cameraName;
	private String cameraIndexName;
	
	public CameraSwitcher(long id, Element element, Document doc, String name) {
		super(id, element, doc, name);
		
		Scope sc = Parser.getRequiredScope(element);
		Element CameraId = sc.get("CameraId");
		Element CameraName = sc.get("CameraName");
		Element CameraIndexName = sc.get("CameraIndexName");

		if(CameraId != null) {
			cameraId = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(CameraId,0));
		}

		if(CameraName != null) {
			cameraName = Parser.getRequiredToken(CameraName,0).stringContents();
		}

		if(CameraIndexName != null && !AssUtil.isEmpty(CameraIndexName.tokens())) {
			cameraIndexName = Parser.getRequiredToken(CameraIndexName,0).stringContents();
		}
	}

	int CameraID() { return cameraId;}
	String cameraName() { return cameraName;}
	String cameraIndexName() {	return cameraIndexName;}
}
