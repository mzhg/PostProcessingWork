package jet.opengl.demos.nvidia.volumelight;

public enum Status {

	/** Success */
	OK					/*=  0*/,
	/** Unspecified Failure */
	FAIL				/*= -1*/,
	/** Mismatch between header and dll */
	INVALID_VERSION		/*= -2*/,
	/** API call made before the library has been properly initialized */
    UNINITIALIZED       /*= -3*/,
    /** Call not implemented for platform */
	UNIMPLEMENTED		/*= -4*/,
	/** One or more invalid parameters */
	INVALID_PARAMETER	/*= -5*/,
	/** Device doesn't support necessary features */
	UNSUPPORTED_DEVICE 	/*= -6*/,
	/** Failed to allocate a resource */
	RESOURCE_FAILURE	/*= -7*/,
	/** The platform API returned an error to the library */
	API_ERROR	        /*= -8*/,
}
