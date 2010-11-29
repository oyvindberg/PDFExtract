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

package org.elacin.pdfextract.tree;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Comparator;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 29.11.10 Time: 04.27 To change this template use
 * File | Settings | File Templates.
 */
public class LayoutRegionNode extends AbstractParentNode<ParagraphNode, PageNode> {

private final boolean pictureRegion;


// ------------------------ INTERFACE METHODS ------------------------

public LayoutRegionNode(final boolean region) {
	pictureRegion = region;
}


// --------------------- Interface XmlPrinter ---------------------

@Override
public void writeXmlRepresentation(@NotNull final Appendable out,
                                   final int indent,
                                   final boolean verbose) throws IOException
{

	if (pictureRegion) {
		for (int i = 0; i < indent; i++) {
			out.append(" ");
		}
		out.append("<graphic");
		getPos().writeXmlRepresentation(out, indent, verbose);
		out.append(">");

		out.append(">\n");

		for (ParagraphNode node : getChildren()) {
			for (LineNode child : node.getChildren()) {
				child.writeXmlRepresentation(out, indent + 4, verbose);
			}
		}

		for (int i = 0; i < indent; i++) {
			out.append(" ");
		}
		out.append("</graphic>\n");

	} else {
		for (ParagraphNode child : getChildren()) {
			child.writeXmlRepresentation(out, indent, verbose); // <-- no extra indentation
		}
	}
}

// -------------------------- PUBLIC METHODS --------------------------

@NotNull
@Override
public Comparator<ParagraphNode> getChildComparator() {
	return new StandardNodeComparator();
}

}
