package jet.opengl.demos.nvidia.hbaoplus;

import jet.opengl.postprocessing.util.Numeric;

final class Viewports {

	final InputViewport fullRes = new InputViewport();
	final InputViewport quarterRes = new InputViewport();

	void setFullResolution(int fullWidth, int fullHeight)
    {
		fullRes.topLeftX = 0.f;
		fullRes.topLeftY = 0.f;
		fullRes.width    = fullWidth;
		fullRes.height   = fullHeight;
        fullRes.minDepth = 0.f;
        fullRes.maxDepth = 1.f;

//        QuarterRes           = FullRes;
        quarterRes.topLeftX = 0.f;
        quarterRes.topLeftY = 0.f;
        quarterRes.minDepth = 0.f;
        quarterRes.maxDepth = 1.f;
        quarterRes.width     = Numeric.divideAndRoundUp(fullWidth,4);
        quarterRes.height    = Numeric.divideAndRoundUp(fullHeight,4);
    }
}
