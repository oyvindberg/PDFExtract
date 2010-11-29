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
import org.elacin.pdfextract.style.StyleComparator;
import org.elacin.pdfextract.tree.LineNode;
import org.elacin.pdfextract.tree.ParagraphNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 17.11.10 Time: 04.45 To change this template use
 * File | Settings | File Templates.
 */
public class ParagraphSegmentator {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(ParagraphSegmentator.class);
private boolean containedInGraphic;
private final boolean SPLIT_BY_STYLES       = true;
private       int     medianVerticalSpacing = -1;

// --------------------- GETTER / SETTER METHODS ---------------------

public void setContainedInGraphic(final boolean containedInGraphic) {
	this.containedInGraphic = containedInGraphic;
}

public void setMedianVerticalSpacing(final int medianVerticalSpacing) {
	this.medianVerticalSpacing = medianVerticalSpacing;
}

// -------------------------- PUBLIC METHODS --------------------------

public List<ParagraphNode> segmentParagraphs(final List<LineNode> lines) {
	if (medianVerticalSpacing == -1) {
		throw new RuntimeException("set medianVerticalSpacing!");
	}

	List<ParagraphNode> ret = new ArrayList<ParagraphNode>();
	/* separate the lines by their dominant style into columns */

	if (!lines.isEmpty()) {
		ParagraphNode currentParagraph = new ParagraphNode();
		currentParagraph.setContainedInImage(containedInGraphic);

		if (SPLIT_BY_STYLES) {
			Style currentStyle = null;
			LineNode lastLine = null;

			for (LineNode line : lines) {
				final Style lineStyle = line.findDominatingStyle();

				if (currentStyle == null) {
					currentStyle = lineStyle;
					lastLine = line;
				}


				final float distance = line.getPos().getY() - lastLine.getPos().getEndY();
				//				final float size = Math.min(line.getPos().getHeight(),
				//				                            lastLine.getPos().getHeight());

				final boolean split;
				switch (StyleComparator.styleCompare(currentStyle, lineStyle)) {
					case SAME_STYLE:
						/** if the styles are similar, only split if there seems to be much space
						 between the two lines */
						split = distance > medianVerticalSpacing * 1.25f;
						break;
					case SUBTLE_DIFFERENCE:
						/** if the difference is subtle, do split if there seems to be some space
						 between the two lines */
						split = distance > medianVerticalSpacing * 0.75f;
						break;
					case BIG_DIFFERENCE:
						split = true;
						break;
					default:
						throw new RuntimeException("made compiler happy :)");
				}

				if (split) {
					if (!currentParagraph.getChildren().isEmpty()) {
						log.info("LOG00660:Split/style: " + currentStyle + ", " + lineStyle);
						ret.add(currentParagraph);
					}
					currentParagraph = new ParagraphNode();
					currentParagraph.setContainedInImage(containedInGraphic);
					currentStyle = lineStyle;
				}

				currentParagraph.addChild(line);
				lastLine = line;
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
