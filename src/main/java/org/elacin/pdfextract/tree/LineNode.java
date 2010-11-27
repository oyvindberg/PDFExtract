/*
 * Copyright 2010 Øyvind Berg (elacin@gmail.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.elacin.pdfextract.tree;

import org.elacin.pdfextract.logical.text.Role;
import org.elacin.pdfextract.style.Style;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Apr 8, 2010 Time: 8:29:43 AM To change this template
 * use File | Settings | File Templates.
 */
public class LineNode extends AbstractParentNode<WordNode, ParagraphNode> {
// --------------------------- CONSTRUCTORS ---------------------------

public LineNode(@NotNull final WordNode child) {
	super(child);
}

public LineNode() {
	super();
}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface XmlPrinter ---------------------

@Override
public void writeXmlRepresentation(@NotNull final Appendable out,
                                   final int indent,
                                   final boolean verbose) throws IOException
{
	for (int i = 0; i < indent; i++) {
		out.append(" ");
	}

	if (lineSeemsToBeFormula()) {
		out.append("<formula>");
		out.append(getText());
		out.append("</formula>\n");
	} else {
		out.append("<line");
		out.append(" styleRef=\"").append(String.valueOf(findDominatingStyle().id)).append("\"");

		if (verbose) {
			getPosition().writeXmlRepresentation(out, indent, verbose);
			out.append(">\n");

			for (WordNode child : getChildren()) {
				child.writeXmlRepresentation(out, indent + 4, verbose);
			}
			for (int i = 0; i < indent; i++) {
				out.append(" ");
			}
			out.append("</line>\n");
		} else {
			out.append(">");
			out.append(getText());
			out.append("</line>\n");
		}
	}
}

// ------------------------ OVERRIDING METHODS ------------------------

@NotNull
@Override
public String getText() {
	StringBuilder sb = new StringBuilder();

	for (int i = 0; i < getChildren().size(); i++) {
		final WordNode word = getChildren().get(i);
		sb.append(word.getText());
		if (i != getChildren().size() - 1 && !word.isPartOfSameWordAs(getChildren().get(i + 1))) {
			sb.append(" ");
		}
	}

	return sb.toString();
}

// -------------------------- PUBLIC METHODS --------------------------

public boolean containsWordWithStyle(final Style style) {
	for (WordNode node : getChildren()) {
		if (node.getStyle().isCompatibleWith(style)) {
			return true;
		}
	}
	return false;
}

@NotNull
public Style findDominatingStyle() {
	if (lineSeemsToBeFormula()){
		return Style.FORMULA;
	}

	boolean textFound = false;
	Map<Style, Integer> letterCountPerStyle = new HashMap<Style, Integer>(10);
	for (WordNode word : getChildren()) {
		final Style style = word.getStyle();
		if (!letterCountPerStyle.containsKey(style)) {
			letterCountPerStyle.put(style, 0);
		}
		final int numChars = word.getText().length();
		letterCountPerStyle.put(style, letterCountPerStyle.get(style) + numChars);
		textFound = true;
	}

	assert textFound;

	int highestNumChars = -1;
	Style style = null;
	for (Map.Entry<Style, Integer> entry : letterCountPerStyle.entrySet()) {
		if (entry.getValue() > highestNumChars) {
			style = entry.getKey();
		}
	}
	return style;
}

/** Returns a Comparator which compares only X coordinates */
@NotNull
@Override
public Comparator<WordNode> getChildComparator() {
	return new Comparator<WordNode>() {
		public int compare(@NotNull final WordNode o1, @NotNull final WordNode o2) {
			if (o1.getPosition().getX() < o2.getPosition().getX()) {
				return -1;
			} else if (o1.getPosition().getX() > o2.getPosition().getX()) {
				return 1;
			}

			return 0;
		}
	};
}

// -------------------------- OTHER METHODS --------------------------

private boolean isIndented() {
	if (getParent() == null){
		return false;
	}

	final float paragraphX = getParent().getPosition().getX();
	return getPosition().getX() > paragraphX + (float) findDominatingStyle().xSize * 2.0f;
}

private boolean lineSeemsToBeFormula() {

	if (getText().length() < 4){
		return false;
	}

	int looksLikeMath = 0;
	int wordCount = 0;

	int containedGraphics = 0;
	for (WordNode word : getChildren()) {

		if (word.getStyle().equals(Style.GRAPHIC)){
			containedGraphics++;
			continue;
		}

		wordCount += word.getText().length();

		/* first check whether the whole word seems to be formatted in a math font */
		if (word.getStyle().isMathFont()){
			looksLikeMath += 3* word.getText().length();
			continue;
		}

		for (int i = 0; i < word.getText().length(); i++) {
			final char c = word.getText().charAt(i);
			if (Character.getType(c) == (int) Character.MATH_SYMBOL){
				looksLikeMath += 5;
			} else if (Character.isDigit(c)){
				looksLikeMath += 2;
			}
		}
	}

	looksLikeMath += containedGraphics * (looksLikeMath *0.1);

//	if (isIndented()){
//		/* add a bit to the probability if the text seems to be indented */
//		looksLikeMath += looksLikeMath * 0.1f;
//	}

	return looksLikeMath > wordCount;
}
}
