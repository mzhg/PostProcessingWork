package assimp.importer.ase;

import java.util.Arrays;

/** Helper structure to represent the inheritance information of an ASE node */
final class InheritanceInfo {

	//! Inherit the parent's position?, axis order is x,y,z
	boolean[] abInheritPosition = new boolean[3];

	//! Inherit the parent's rotation?, axis order is x,y,z
	boolean[] abInheritRotation = new boolean[3];

	//! Inherit the parent's scaling?, axis order is x,y,z
	boolean[] abInheritScaling = new boolean[3];
	
	public InheritanceInfo() {
		Arrays.fill(abInheritPosition, true);
		Arrays.fill(abInheritRotation, true);
		Arrays.fill(abInheritScaling, true);
	}
}
