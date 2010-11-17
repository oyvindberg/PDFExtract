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

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Comparator;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Apr 8, 2010 Time: 8:29:43 AM To change this template
 * use File | Settings | File Templates.
 */
public class LineNode extends AbstractParentNode<WordNode, ParagraphNode> {
// ------------------------------ FIELDS ------------------------------

private final boolean SHOW_DETAILS = false;

// --------------------------- CONSTRUCTORS ---------------------------

public LineNode(@NotNull final WordNode child) {
	super(child);
}

public LineNode() {
	super();
}

// ------------------------ OVERRIDING METHODS ------------------------

@Override
protected void appendLocalInfo(@NotNull final Appendable out, final int indent) throws IOException {
	if (SHOW_DETAILS) {
		super.appendLocalInfo(out, indent);
	} else {
		for (int i = 0; i < indent; i++) {
			out.append(" ");
		}
		out.append(getClass().getSimpleName());
		out.append(": \"");
		writeTextTo(out);
		out.append("\"");
		out.append("\n");
	}
}

@NotNull
@Override
public String getText() {
	StringBuilder sb = new StringBuilder();
	writeTextTo(sb);

	return sb.toString();
}

// -------------------------- PUBLIC METHODS --------------------------

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

private void writeTextTo(@NotNull final Appendable sb) {
	try {
		for (int i = 0; i < getChildren().size(); i++) {
			final WordNode word = getChildren().get(i);
			sb.append(word.getText());
			if (i != getChildren().size() - 1 && !word.isPartOfSameWordAs(getChildren().get(
					i + 1))) {
				sb.append(" ");
			}
		}
	} catch (IOException e) {
		throw new RuntimeException("something went wrong while writing text", e);
	}
}

}
