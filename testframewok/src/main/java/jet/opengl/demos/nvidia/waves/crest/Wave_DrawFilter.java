package jet.opengl.demos.nvidia.waves.crest;

interface Wave_DrawFilter {
    /** float in the first, isTransition in the seocnd */
    void Filter(Wave_LodData_Input data, Wave_CDClipmap clipmap, Wave_FilterData result);
}
