package org.openstreetmap.josm.plugins.graphview.core.data;

import java.util.Collections;
import java.util.Iterator;

public final class EmptyTagGroup implements TagGroup {

	public static final EmptyTagGroup EMPTY_TAG_GROUP = new EmptyTagGroup();

	private EmptyTagGroup() { }

	@Override
	public boolean contains(Tag tag) {
		return false;
	}

	@Override
	public boolean containsAny(Iterable<Tag> tag) {
		return false;
	}

	@Override
	public boolean contains(String key, String value) {
		return false;
	}

	@Override
	public boolean containsAny(Iterable<String> key, String value) {
		return false;
	}

	@Override
	public boolean containsAny(String key, Iterable<String> values) {
		return false;
	}

	@Override
	public boolean containsAny(Iterable<String> keys, Iterable<String> values) {
		return false;
	}

	@Override
	public boolean containsAnyKey(Iterable<String> keys) {
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
	public boolean containsAnyValue(Iterable<String> value) {
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
	public boolean isEmpty() {
		return true;
	}

	@Override
	public Iterator<Tag> iterator() {
		return Collections.<Tag>emptyList().iterator();
	}

	@Override
	public String toString() {
		return "{}";
	}

}
