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

package org.elacin.pdfextract.physical.segmentation.paragraph;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.style.Style;
import org.elacin.pdfextract.tree.LineNode;
import org.elacin.pdfextract.tree.ParagraphNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 17.11.10 Time: 04.45 To change this template use
 * File | Settings | File Templates.
 */
public class ParagraphSegmentator {

private static final Logger log = Logger.getLogger(ParagraphSegmentator.class);

// ------------------------------ FIELDS ------------------------------

private boolean setIsContainedInGraphic;
private final boolean SPLIT_BY_STYLES = true;

// --------------------- GETTER / SETTER METHODS ---------------------

public boolean isSetIsContainedInGraphic() {
	return setIsContainedInGraphic;
}

public void setSetIsContainedInGraphic(final boolean setIsContainedInGraphic) {
	this.setIsContainedInGraphic = setIsContainedInGraphic;
}

// -------------------------- PUBLIC METHODS --------------------------

public List<ParagraphNode> segmentParagraphs(final List<LineNode> lines) {
	List<ParagraphNode> ret = new ArrayList<ParagraphNode>();
	/* separate the lines by their dominant style into columns */

	if (!lines.isEmpty()) {
		ParagraphNode currentParagraph = new ParagraphNode();
		currentParagraph.setContainedInImage(setIsContainedInGraphic);

		if (SPLIT_BY_STYLES) {
			Style currentStyle = null;

			for (LineNode line : lines) {
				final Style lineStyle = line.findDominatingStyle();

				if (currentStyle == null) {
					currentStyle = lineStyle;
				}

				if (!currentStyle.isCompatibleWith(lineStyle)
						&& !line.containsWordWithStyle(currentStyle)) {
					if (!currentParagraph.getChildren().isEmpty()) {
						log.info("LOG00660:Splitting text: " + currentStyle + ", " + lineStyle);
						ret.add(currentParagraph);
					}
					currentParagraph = new ParagraphNode();
					currentParagraph.setContainedInImage(setIsContainedInGraphic);
					currentStyle = null;
				}
				currentParagraph.addChild(line);
			}
		} else {
			for (LineNode line : lines) {
				currentParagraph.addChild(line);
			}
		}

		if (!currentParagraph.getChildren().isEmpty()) {
			ret.add(currentParagraph);
		}
	}

	return ret;
}
}
