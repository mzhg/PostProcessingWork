package assimp.common;

/** Meta information about a particular importer. Importers need to fill
 *  this structure, but they can freely decide how talkative they are. 
 *  A common use case for loader meta info is a user interface
 *  in which the user can choose between various import/export file
 *  formats. Building such an UI by hand means a lot of maintenance
 *  as importers/exporters are added to Assimp, so it might be useful
 *  to have a common mechanism to query some rough importer
 *  characteristics. */
public class ImporterDesc {

	/** Indicates that there is a textual encoding of the
	 *  file format; and that it is supported.*/
	public static final int aiImporterFlags_SupportTextFlavour = 0x1;

	/** Indicates that there is a binary encoding of the
	 *  file format; and that it is supported.*/
	public static final int aiImporterFlags_SupportBinaryFlavour = 0x2;

	/** Indicates that there is a compressed encoding of the
	 *  file format; and that it is supported.*/
	public static final int aiImporterFlags_SupportCompressedFlavour = 0x4;

	/** Indicates that the importer reads only a very particular
	  * subset of the file format. This happens commonly for
	  * declarative or procedural formats which cannot easily
	  * be mapped to #aiScene */
	public static final int aiImporterFlags_LimitedSupport = 0x8;

	/** Indicates that the importer is highly experimental and
	  * should be used with care. This only happens for trunk
	  * (i.e. SVN) versions, experimental code is not included
	  * in releases. */
	public static final int aiImporterFlags_Experimental = 0x10;
	
	/** Full name of the importer (i.e. Blender3D importer)*/
	public final String mName;

	/** Original author (left blank if unknown or whole assimp team) */
	public final String mAuthor;

	/** Current maintainer, left blank if the author maintains */
	public final String mMaintainer;

	/** Implementation comments, i.e. unimplemented features*/
	public final String mComments;

	/** Any combination of the #aiLoaderFlags enumerated values.
	    These flags indicate some characteristics common to many
		importers. */
	public final int mFlags;

	/** Minimum format version that can be loaded im major.minor format,
	    both are set to 0 if there is either no version scheme 
		or if the loader doesn't care. */
	public final int mMinMajor;
	public final int mMinMinor;

	/** Maximum format version that can be loaded im major.minor format,
	    both are set to 0 if there is either no version scheme 
		or if the loader doesn't care. Loaders that expect to be
		forward-compatible to potential future format versions should 
		indicate  zero, otherwise they should specify the current
		maximum version.*/
	public final int mMaxMajor;
	public final int mMaxMinor;

	/** List of file extensions this importer can handle.
	    List entries are separated by space characters.
		All entries are lower case without a leading dot (i.e.
		"xml dae" would be a valid value. Note that multiple
		importers may respond to the same file extension -
		assimp calls all importers in the order in which they
		are registered and each importer gets the opportunity
		to load the file until one importer "claims" the file. Apart
		from file extension checks, importers typically use
		other methods to quickly reject files (i.e. magic
		words) so this does not mean that common or generic
		file extensions such as XML would be tediously slow. */
	public final String mFileExtensions;
	
	public ImporterDesc(String name, String author, String maintainer, String comments, int flags, int minMajor,
			int minMinor, int maxMajor, int maxMinor, String fileExtensions) {
		this.mName = name;
		this.mAuthor = author;
		this.mMaintainer = maintainer;
		this.mComments = comments;
		this.mFlags = flags;
		this.mMinMajor = minMajor;
		this.mMinMinor = minMinor;
		this.mMaxMajor = maxMajor;
		this.mMaxMinor = maxMinor;
		this.mFileExtensions = fileExtensions;
	}
	
	
}
