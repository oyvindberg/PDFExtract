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

public List<LineNode> segmentLines(PhysicalPageRegion region, float tolerance) {
	final Rectangle pos = region.getPosition();

	final List<LineNode> ret = new ArrayList<LineNode>();


	final Set<PhysicalContent> workingSet = new HashSet<PhysicalContent>();
	for (float y = pos.getY(); y < pos.getEndY(); y++) {
		final List<PhysicalContent> row = region.findContentAtYIndex(y);
		workingSet.addAll(row);

		final boolean isLineBoundary;
		if (row.isEmpty()) {
			isLineBoundary = true;
		} else if (row.size() < 3) {
			int ok = 0;
			for (PhysicalContent content : row) {
				/* if this content starts right over this line */
				if (content.getPosition().getY() - y <= tolerance) {
					ok++;

					/** or if it end right after this line. if it does, include it in the next
					 * line instead */
				} else if (content.getPosition().getEndY() - y <= tolerance) {
					ok++;
					workingSet.remove(content);
				}
			}

			isLineBoundary = (ok == row.size());
		} else {
			isLineBoundary = false;
		}


		if (isLineBoundary || (y + 1.0f) >= pos.getEndY()) {
			if (!workingSet.isEmpty()) {
				LineNode lineNode = new LineNode();
				for (PhysicalContent word : workingSet) {

					if (word.isText() && !word.getText().isAssignedBlock()) {
						lineNode.addChild(createWordNode(word.getText(), region.getPageNumber()));
						word.getText().setBlockNum(1);
					}
				}
				//				region.removeContent(workingSet);
				if (!lineNode.getChildren().isEmpty()) {
					ret.add(lineNode);
				}
				workingSet.clear();
			}
		}
	}

	//
	//	//	for (PhysicalPageRegion region : regions) {
	//	for (PhysicalContent content : region.getContents()) {
	//		if (content.isText() && content.getText().getContent().contains("inlunde")) {
	//			System.out.println("PhysicalPage.compileLogicalPage");
	//		}
	//	}
	//	//	}

	return ret;
}
}
