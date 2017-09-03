package assimp.importer.blender;

import java.util.ArrayList;

import assimp.common.AssUtil;
import assimp.common.DefaultLogger;
import assimp.common.Node;

/** Manage all known modifiers and instance and apply them if necessary */
final class BlenderModifierShowcase {
	
	final static String[] creators = {
		BlenderModifier_Mirror.class.getName(),
		BlenderModifier_Subdivision.class.getName()
	};

	final ArrayList<BlenderModifier> cached_modifiers = new ArrayList<>();
	
	/** Apply all requested modifiers provided we support them. */
	void applyModifiers(Node out, ConversionData conv_data,  BLEScene in, BLEObject orig_object ){
		int cnt = 0, ful = 0;

		// NOTE: this cast is potentially unsafe by design, so we need to perform type checks before
		// we're allowed to dereference the pointers without risking to crash. We might still be
		// invoking UB btw - we're assuming that the ModifierData member of the respective modifier
		// structures is at offset sizeof(vftable) with no padding.
//		const SharedModifierData* cur = boost::static_pointer_cast<const SharedModifierData> ( orig_object.modifiers.first.get() );
		SharedModifierData cur = (SharedModifierData) orig_object.modifiers.first;
//		for (; cur; cur =  boost::static_pointer_cast<const SharedModifierData> ( cur->modifier.next.get() ), ++ful) {
		for (; cur != null; cur = (SharedModifierData)cur.modifier.next, ++ful){
//			ai_assert(cur->dna_type);

			Structure s = conv_data.db.dna.get( cur.dna_type );
			if (s == null) {
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("BlendModifier: could not resolve DNA name:  "+ cur.dna_type);
				continue;
			}

			// this is a common trait of all XXXMirrorData structures in BlenderDNA
			Field f = s.get("modifier");
			if (f == null || f.offset != 0) {
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("BlendModifier: expected a `modifier` member at offset 0");
				continue;
			}

			s = conv_data.db.dna.get( f.type );
			if (s == null|| !s.name.equals("ModifierData")) {
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("BlendModifier: expected a ModifierData structure as first member");
				continue;
			}

			// now, we can be sure that we should be fine to dereference *cur* as
			// ModifierData (with the above note).
			ModifierData dat = cur.modifier;

//			const fpCreateModifier* curgod = creators;
//			std::vector< BlenderModifier* >::iterator curmod = cached_modifiers->begin(), endmod = cached_modifiers->end();
//
//			for (;*curgod;++curgod,++curmod) { // allocate modifiers on the fly
//				if (curmod == endmod) {
//					cached_modifiers->push_back((*curgod)());
//
//					endmod = cached_modifiers->end();
//					curmod = endmod-1;
//				}
//
//				BlenderModifier* const modifier = *curmod;
//				if(modifier->IsActive(dat)) {
//					modifier->DoIt(out,conv_data,*boost::static_pointer_cast<const ElemBase>(cur),in,orig_object);
//					cnt++;
//
//					curgod = NULL;
//					break;
//				}
//			}
			int curgod = 0;
			int curmod = 0, endmod = cached_modifiers.size();
			for(; curgod < creators.length; curgod ++, curmod ++){// allocate modifiers on the fly
				if (curmod == endmod) {
					cached_modifiers.add((BlenderModifier)AssUtil.newInstance(creators[curgod]));

					endmod = cached_modifiers.size();
					curmod = endmod-1;
				}

				BlenderModifier modifier =cached_modifiers.get(curmod);
				if(modifier.isActive(dat)) {
					modifier.doIt(out,conv_data,(ElemBase)(cur),in,orig_object);
					cnt++;

					curgod = -1;
					break;
				}
			}
			
			if (curgod != -1) {
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("Couldn't find a handler for modifier: " + dat.name);
			}
		}

		// Even though we managed to resolve some or all of the modifiers on this
		// object, we still can't say whether our modifier implementations were
		// able to fully do their job.
		if (ful != 0 && DefaultLogger.LOG_OUT) {
//			ASSIMP_LOG_DEBUG_F("BlendModifier: found handlers for ",cnt," of ",ful," modifiers on `",orig_object.id.name,
//				"`, check log messages above for errors");
			DefaultLogger.debug("BlendModifier: found handlers for " + cnt + " of " + ful + " modifiers on `" + orig_object.id.name + 
					"`, check log messages above for errors");
		}
	}
}
