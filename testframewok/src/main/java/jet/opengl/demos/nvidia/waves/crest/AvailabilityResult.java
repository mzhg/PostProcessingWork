package jet.opengl.demos.nvidia.waves.crest;

/**
 * Collision diagnostics result.
 */
public enum AvailabilityResult {
    /** Collision data available, good to go */
    DataAvailable,
    /** Collision provider is not fully initialised. */
    NotInitialisedYet,
    /**
     * There is no data available (yet) that covers the query position. This might be because the query was made
     * before async data started flowing back to the CPU, or the query position may be outside the largest LOD.
     */
    NoDataAtThisPosition,
    /** A min spatial width was specified with the expectation that wavelengths much smaller than this width would
     * be filtered out. There is currently no LOD big enough that filters out these wavelengths. Data will still
     * be return but it will contain wavelengths smaller than expected.
     */
    NoLODsBigEnoughToFilterOutWavelengths,

    /** This should never be hit, and indicates that the validation logic is broken. */
    ValidationFailed,
}
