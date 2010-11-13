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

package org.elacin.pdfextract.physical.segmentation.line;

import org.elacin.pdfextract.physical.content.PhysicalContent;
import org.elacin.pdfextract.physical.content.PhysicalPageRegion;
import org.elacin.pdfextract.physical.content.PhysicalText;
import org.elacin.pdfextract.tree.LineNode;
import org.elacin.pdfextract.tree.WordNode;
import org.elacin.pdfextract.util.Rectangle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 12.11.10 Time: 20.30 To change this template use
 * File | Settings | File Templates.
 */
public class LineSegmentator {
// -------------------------- STATIC METHODS --------------------------

@NotNull
private static WordNode createWordNode(@NotNull final PhysicalText text, int pageNumber) {
	return new WordNode(text.getPosition(), pageNumber, text.style, text.content, text.charSpacing);
}

// -------------------------- PUBLIC METHODS --------------------------

public List<LineNode> segment(PhysicalPageRegion region) {
	final Rectangle pos = region.getPosition();

	final List<LineNode> ret = new ArrayList<LineNode>();


	final Set<PhysicalContent> workingSet = new HashSet<PhysicalContent>();
	for (float y = pos.getY(); y < pos.getEndY(); y++) {
		final List<PhysicalContent> row = region.findContentAtYIndex(y);
		workingSet.addAll(row);

		if (row.isEmpty()) {
			LineNode lineNode = new LineNode();
			for (PhysicalContent word : workingSet) {
				if (word.isText()) {
					lineNode.addChild(createWordNode(word.getText(), region.getPageNumber()));
				}
			}
			if (!workingSet.isEmpty()) {
				workingSet.clear();
				ret.add(lineNode);
			}
		}
	}

	return ret;
}
}
