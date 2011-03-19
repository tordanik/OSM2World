package org.openstreetmap.josm.plugins.graphview.core.data;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class EmptyTagGroup implements TagGroup {

	public static final EmptyTagGroup EMPTY_TAG_GROUP = new EmptyTagGroup();
	
	private EmptyTagGroup() { }
	
	private static final class EmptyTagIterator implements Iterator<Tag> {
		@Override
		public boolean hasNext() {
			return false;
		}
		@Override
		public Tag next() {
			throw new NoSuchElementException();
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}		
	};
	
	private static final EmptyTagIterator EMPTY_TAG_ITERATOR = new EmptyTagIterator();
	
	@Override
	public boolean contains(Tag tag) {
		return false;
	}
	
	@Override
	public boolean contains(String key, String value) {
		return false;
	}

	@Override
	public boolean containsKey(String key) {
		return false;
	}

	@Override
	public boolean containsValue(String value) {
		return false;
	}

	@Override
	public String getValue(String key) {
		return null;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Iterator<Tag> iterator() {
		return EMPTY_TAG_ITERATOR;
	}
	
	@Override
	public String toString() {
		return "{}";
	}
	
}
