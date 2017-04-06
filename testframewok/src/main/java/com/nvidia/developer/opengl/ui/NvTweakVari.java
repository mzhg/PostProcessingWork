package com.nvidia.developer.opengl.ui;

import com.nvidia.developer.opengl.utils.FieldControl;

public class NvTweakVari extends NvTweakVarBase{

	/** Reference to variable being tracked/tweaked. */
	protected FieldControl mValRef;
	/** Initial value, useful for 'reset' to starting point. */
	protected int mValInitial;
	/** A member of our datatype, for self-referencing NvTweakVar to point into itself. */
	protected int mValSelf;
	
	/** Minimum value for a variable with clamped range. */
	protected int mValMin;
	/** Maximum value for a variable with clamped range. */
	protected int mValMax;
	/** Value step for the variable (increment/decrement). */
	protected int mValStep;
	
	/** Whether value adjustments to this variable are clamped/bounded. */
	protected boolean mValClamped;
	/** When value is clamped, does value loop around when increment/decrement hits 'ends'. */
	protected boolean mValLoop;
	
	/** Clamped constructor, typically used for scalar variables. */
	protected NvTweakVari(FieldControl refVal, String name, int minVal, int maxVal, int step, String description) {
		super(name, description);
		
		mValRef = refVal;
		mValMin = minVal;
		mValMax = maxVal;
		mValStep = step == 0 ? 1 : step;
		mValClamped = true;
	}
	
	/** Clamped constructor, used for self-referential scalars. */
	protected NvTweakVari(String name, int minVal, int maxVal, int step, String description) {
		super(name, description);
		
//		mValRef = new FieldControl(this, "mValSelf", FieldControl.CALL_FIELD);
		mValMin = minVal;
		mValMax = maxVal;
		mValStep = step == 0 ? 1 : step;
		mValClamped = true;
	}
	
	/** Specialized unclamped constructor, generally used for bool variable. */
	protected NvTweakVari(FieldControl refVal, String name, String description) {
		super(name, description);
		
		mValRef = refVal;
		mValMin = 0;
		mValMax = 0;
		mValStep = 1;
		mValClamped = false;
	}
	
	/** Specialized unclamped constructor, generally used for self-referential bool variable. */
	protected NvTweakVari(String name, String description) {
		super(name, description);
		
//		mValRef = new FieldControl(this, "mValSelf", FieldControl.CALL_FIELD);
		mValMin = 0;
		mValMax = 0;
		mValStep = 1;
		mValClamped = false;
	}
	
	/** Accessor for getting at the current value when we hold onto a typed subclass. */
    public int get() { return mValRef != null ? (Integer) mValRef.getValue() : mValSelf; }

    /** Assignment operator to set (once) the internal variable we will point to. */
    public NvTweakVari set(int val){
    	if(mValRef!= null)
    		mValRef.setValue(val); 
    	else
    		mValSelf = val;
    	return this;
    }
    
    /** Set whether or not to loop clamped value when increment/decrement reach ends of range. */
    public void setValLoop(boolean loop) { mValLoop = loop; }

    /** Specific implementation of increment for the templated datatype. */
    public void increment(){
    	int valRef = get();
    	if (mValClamped) // check to see if we'll exceed max.
            if (valRef == mValMax || valRef+mValStep > mValMax)
            {// if already at max, or would exceed max, loop or clamp.
                if (mValLoop)
                	valRef = mValMin;
                else
                	valRef = mValMax;
                set(valRef);
                return;
            }

    	valRef += mValStep;
    	set(valRef);
    }
    /** Specific implementation of decrement for the templated datatype. */
    public void decrement(){
    	int valRef = get();
    	if (mValClamped) // check to see if we'll drop below min.
            if (valRef == mValMin || valRef-mValStep < mValMin)
            {// if already at min, or would drop under min, loop or clamp.
                if (mValLoop)
                	valRef = mValMax;
                else
                	valRef = mValMin;
                set(valRef);
                return;
            }

    	valRef -= mValStep;
    	set(valRef);
    }

    /** Reset the managed variable to its initial value. */
    public void reset() {
    	if(mValRef != null)
            mValRef.setValue(mValInitial);
    	else
    		mValSelf = mValInitial;
    }

    /** Specific implementation of equals that each templated type must override appropriately. */
    public boolean equals(boolean val){
    	return false;
    }
    /** Specific implementation of equals that each templated type must override appropriately. */
    public boolean equals(float val){
    	return false;
    }
    /** Specific implementation of equals that each templated type must override appropriately. */
    public boolean equals(int val){
    	return get() == val;
    }

}
