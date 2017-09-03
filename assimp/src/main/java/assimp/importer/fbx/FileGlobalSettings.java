package assimp.importer.fbx;

import org.lwjgl.util.vector.Vector3f;

/** DOM class for global document settings, a single instance per document can
 *  be accessed via Document.Globals(). */
final class FileGlobalSettings {

//	enum FrameRate {
	static final int 
		FrameRate_DEFAULT = 0,
		FrameRate_120 = 1,
		FrameRate_100 = 2,
		FrameRate_60 = 3,
		FrameRate_50 = 4,
		FrameRate_48 = 5,
		FrameRate_30 = 6,
		FrameRate_30_DROP = 7,
		FrameRate_NTSC_DROP_FRAME = 8,
		FrameRate_NTSC_FULL_FRAME = 9,
		FrameRate_PAL = 10,
		FrameRate_CINEMA = 11,
		FrameRate_1000 = 12,
		FrameRate_CINEMA_ND = 13,
		FrameRate_CUSTOM = 14,

		FrameRate_MAX = 15;// end-of-enum sentinel
//	};
	
	private final PropertyTable props;
	private final Document doc;
	
	public FileGlobalSettings(Document doc, PropertyTable props) {
		this.props = props;
		this.doc = doc;
	}
	
	PropertyTable props() {	return props;}
	Document getDocument() { return doc;}
	
	int upAxis() { return PropertyTable.propertyGet(props(), "UpAxis",  1);}

	int upAxisSign() { return PropertyTable.propertyGet(props(), "UpAxisSign",  1);}

	int frontAxis() { return PropertyTable.propertyGet(props(), "FrontAxis",  2);}

	int frontAxisSign() { return PropertyTable.propertyGet(props(), "FrontAxisSign",  1);}

	int coordAxis() { return PropertyTable.propertyGet(props(), "CoordAxis",  0);}

	int coordAxisSign() { return PropertyTable.propertyGet(props(), "CoordAxisSign",  1);}

	int originalUpAxis() { return PropertyTable.propertyGet(props(), "OriginalUpAxis",  0);}

	int originalUpAxisSign() { return PropertyTable.propertyGet(props(), "OriginalUpAxisSign",  1);}

	double unitScaleFactor() { return PropertyTable.propertyGet(props(), "UnitScaleFactor",  1.0);}

	double originalUnitScaleFactor() { return PropertyTable.propertyGet(props(), "OriginalUnitScaleFactor",  1.0);}

	Vector3f ambientColor() { return PropertyTable.propertyGet(props(), "AmbientColor", new  Vector3f(0,0,0));}

	String defaultCamera() { return PropertyTable.propertyGet(props(), "DefaultCamera",  "");}

	int timeMode() {
		final int ival = PropertyTable.propertyGet(props(), "TimeMode", Integer.valueOf( FrameRate_DEFAULT));
		if (ival < 0 || ival >= FrameRate_MAX/*AI_CONCAT(type, _MAX)*/) {
			assert( FrameRate_DEFAULT >= 0 &&  FrameRate_DEFAULT < FrameRate_MAX/*AI_CONCAT(type, _MAX)*/);
			return ( FrameRate_DEFAULT);
		}
		return (ival);
	}

	long timeSpanStart() { return PropertyTable.propertyGet(props(), "TimeSpanStart",  0L);}

	long timeSpanStop() { return PropertyTable.propertyGet(props(), "TimeSpanStop",  0L);}

	float customFrameRate() { return PropertyTable.propertyGet(props(), "CustomFrameRate",  -1.0f);}
}
