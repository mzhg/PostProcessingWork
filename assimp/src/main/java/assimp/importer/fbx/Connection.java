package assimp.importer.fbx;

/** Represents a link between two FBX objects. */
final class Connection implements Comparable<Connection>{

	long insertionOrder;
	String prop;

	long src, dest;
	final Document doc;
	
	Connection(long insertionOrder,  long src, long dest, String prop, Document doc){
		this.insertionOrder = insertionOrder;
		this.prop = prop;
		this.src = src;
		this.dest = dest;
		this.doc = doc;
	}

	// note: a connection ensures that the source and dest objects exist, but
	// not that they have DOM representations, so the return value of one of
	// these functions can still be NULL.
	FBXObject sourceObject() { return doc.getObject(src).get(false);}
	FBXObject destinationObject() {return doc.getObject(dest).get(false);}

	// these, however, are always guaranteed to be valid
	LazyObject lazySourceObject(){ return doc.getObject(src);}
	LazyObject lazyDestinationObject() { return doc.getObject(dest);}

	/** return the name of the property the connection is attached to.
	  * this is an empty string for object to object (OO) connections. */
	String propertyName() {	return prop;}
	long insertionOrder() {	return insertionOrder;}
	
	@Override
	public int compareTo(Connection c) {
//		// note: can't subtract because this would overflow uint64_t
		if(insertionOrder > c.insertionOrder) {
			return 1;
		}
		else if(insertionOrder < c.insertionOrder) {
			return -1;
		}
		return 0;
	}

//	int CompareTo(const Connection* c) const {
//		// note: can't subtract because this would overflow uint64_t
//		if(InsertionOrder() > c->InsertionOrder()) {
//			return 1;
//		}
//		else if(InsertionOrder() < c->InsertionOrder()) {
//			return -1;
//		}
//		return 0;
//	}
//
//	bool Compare(const Connection* c) const {
//		return InsertionOrder() < c->InsertionOrder();
//	}
}
