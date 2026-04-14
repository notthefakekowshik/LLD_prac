package com.lldprep.foundations.behavioral.iterator.good;

import java.util.Iterator;

/**
 * Marker interface for tree iterators.
 * Extends java.util.Iterator so the tree works in for-each loops natively.
 */
public interface TreeIterator<T> extends Iterator<T> { }
