/*
 * Copyright 2010 Øyvind Berg (elacin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elacin.pdfextract.physical.segmentation.word;

import org.elacin.pdfextract.util.FileWalker;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 01.12.10 Time: 07.05 To change this template use
 * File | Settings | File Templates.
 */
public class TestSpacing2 {


@Test
public void testSpacings() throws IOException {

	final List<File> files = FileWalker
			.getFileListing(new File("target/test-classes/spacings"), ".spacing");


	for (File file : files) {
		if (!file.getName().contains("article4")) {
			continue;
		}

		System.out.println("++++++++++++++++++++++++++");
		System.out.println("File " + file);
		System.out.println("--------------------------");

		int correct = 0, total = 0;
		BufferedReader reader = new BufferedReader(new FileReader(file));

		String s;
		String[] input = new String[4];
		int i = 0;
		while ((s = reader.readLine()) != null) {
			input[i++] = s;
			if (i == 4) {
				if (processInput(input)) {
					correct++;
				}
				total++;
				i = 0;
			}

		}


		System.out.println("File " + file + ": got " + correct + " of " + total);
		System.out.println("--------------------------");
	}


}

/**
 * INTRODUCTION 10.0 [1.1588593, 1.6600113, 1.5800095, 1.0545425, 1.8704376, 1.8097687, 1.7822418,
 * 1.8740387, 1.5200195, 1.408844, 1.8704224]
 */

private boolean processInput(final String[] input) {

	String answer = input[1];
	String base = answer.replaceAll(" ", "");
	float fontSize = Float.valueOf(input[2]);
	List<Float> distances = new ArrayList<Float>();

	float[] distancesArray = parseDistancesString(input[3], distances);

	boolean ligatureHack = false;
	if (distancesArray.length != base.length() - 1) {
		//		if ((base.length() - 1) - distancesArray.length == 1 && base.contains("fi")){
		//			ligatureHack = true;
		//		} else {
		//			System.out.println("bad input = " + Arrays.toString(input));
		return true;
		//		}
	}


	final String result = findResult(base, fontSize, distances, distancesArray, ligatureHack);

	final boolean equals = answer.equals(result);

	if (!equals) {
		System.out.println("wrong result: expected: '" + answer + "', got '" + result + "'");
		findResult(base, fontSize, distances, distancesArray, ligatureHack);
	}

	return equals;
}

private String findResult(final String base,
                          final float fontSize,
                          final List<Float> distances,
                          final float[] distancesArray,
                          final boolean hack)
{
	final float charspace = CharSpacingFinder.calculateCharspacingForDistances(distances, fontSize);

	StringBuffer sb = new StringBuffer();
	sb.append(base.charAt(0));
	int strIndex = 1;
	for (int i = 0; i < distancesArray.length; i++) {
		float distance = distancesArray[i];

		if (distance > charspace) {
			sb.append(" ");
		}

		final char c = base.charAt(strIndex++);
		sb.append(c);

		if (hack) {
			if (c == 'f' && strIndex < base.length()) {
				if (base.charAt(strIndex) == 'i') {
					sb.append(base.charAt(strIndex++));
				}
			}
		}

	}

	return sb.toString();
}

private static float[] parseDistancesString(final String s, final List<Float> distances) {
	String distancesString = s.trim().substring(1, s.length() - 2);
	for (StringTokenizer tokenizer = new StringTokenizer(distancesString, ", "); tokenizer
			.hasMoreTokens();) {
		distances.add(Float.valueOf(tokenizer.nextToken()));
	}

	float[] distancesArray = new float[distances.size()];
	for (int i = 0, size = distances.size(); i < size; i++) {
		distancesArray[i] = distances.get(i);
	}
	return distancesArray;
}
}
