package org.bpel4chor.splitprocess.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * RandomIdGenerator generates a system wide unique Id string whose length is
 * pre-set and consists of number and character.
 * 
 * @since Mar 9, 2012
 * @author Daojun Cui
 */
public class RandomIdGenerator {

	/** used id set */
	private static Set<String> usedIds = new HashSet<String>();

	/** how long the id */
	private final static int idLength = 3;

	/** counter for limit check */
	private static int counter = 0;

	/**
	 * Base of randomised characters [0-9, a-z, A-Z], it provides > [default]
	 * 62^3>200000 possible ids. If more ids are desired, just set the idLength to
	 * higher number.
	 */
	private static char[] randBase = new char[62];

	private static int randBaseSize = randBase.length;

	/** maximum size of the possible ids */
	private static int maxSize = (int) Math.pow(randBaseSize, idLength);

	static {
		// init random base
		char numZero = '0';
		for (int i = 0; i < 10; i++) {
			randBase[i] = numZero++;
		}
		char downCaseA = 'a';
		for (int i = 10; i < 36; i++) {
			randBase[i] = downCaseA++;
		}
		char upCaseA = 'A';
		for (int i = 36; i < 62; i++) {
			randBase[i] = upCaseA++;
		}
	}

	/**
	 * Get a system wide id with the length given
	 * 
	 * @param lengthMinFour
	 * @return
	 */
	public static String getId() {

		if (isOutOfLimit()) {
			reset();
			throw new RuntimeException("RandomIdGenerator has ran out of ids for the length " + idLength
					+ ". Set longer length for ids. The id set is reset now.");

		}

		StringBuffer sb = null;
		do {
			sb = new StringBuffer();
			for (int i = 0; i < idLength; i++) {
				char c = randBase[(int) (Math.random() * randBaseSize)];
				sb.append(c);
			}
		} while (usedIds.contains(sb.toString()));
		usedIds.add(sb.toString());
		counter++;

		return sb.toString();
	}

	/**
	 * Test whether the ids whose length as same as the given one has reach the
	 * limit.
	 * 
	 * @return
	 */
	private static boolean isOutOfLimit() {
		return counter < maxSize ? false : true;
	}

	public static int idLength() {
		return idLength;
	}

	public static int randBaseSize() {
		return randBase.length;
	}

	public static int maxSize() {
		return maxSize;
	}

	public static int usedSize() {
		return counter;
	}

	public static void reset() {
		counter = 0;
		usedIds.clear();
	}
}
