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
 * Created by IntelliJ IDEA. User: elacin Date: Apr 8, 2010 Time: 8:56:45 AM To change this template
 * use File | Settings | File Templates.
 */
public class ParagraphNode extends AbstractParentNode<LineNode, PageNode> {
// ------------------------------ FIELDS ------------------------------

boolean containedInImage = false;

// --------------------- GETTER / SETTER METHODS ---------------------

public boolean isContainedInImage() {
	return containedInImage;
}

public void setContainedInImage(final boolean containedInImage) {
	this.containedInImage = containedInImage;
}

// -------------------------- PUBLIC METHODS --------------------------

/** Returns a Comparator which compares coordinates within a page */
@NotNull
@Override
public Comparator<LineNode> getChildComparator() {
	return new StandardNodeComparator();
}

@Override
public void writeXmlRepresentation(@NotNull final Appendable out,
                                   final int indent,
                                   final boolean verbose) throws IOException
{
	for (int i = 0; i < indent; i++) {
		out.append(" ");
	}
	out.append("<paragraph");

	getPos().writeXmlRepresentation(out, indent, verbose);

	out.append(">\n");
	for (LineNode child : getChildren()) {
		child.writeXmlRepresentation(out, indent + 4, verbose);
	}

	for (int i = 0; i < indent; i++) {
		out.append(" ");
	}
	out.append("</paragraph>\n");
}
}
