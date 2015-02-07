package com.metawiring.load.generator;

/**
 * This is a tagging interface only, since Java doesn't actually know whether something
 * is inherently threadsafe.
 *
 * For the purposes of this toolkit, tag a generator with this interface if you intend
 * for it to be usable concurrently by multiple other threads without throwing exceptions
 * or giving incorrect results. The factory path which instantiates the generators will
 * throw an error if you try to use a generator by multiple threads which is not tagged
 * as threadsafe by this interface.
 */
public interface ThreadsafeGenerator {
}
