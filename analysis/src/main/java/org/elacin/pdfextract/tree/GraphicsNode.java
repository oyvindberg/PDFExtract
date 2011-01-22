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

import java.util.Comparator;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 22.01.11 Time: 00.46 To change this template use
 * File | Settings | File Templates.
 */
public class GraphicsNode extends AbstractParentNode<ParagraphNode, PageNode> {
@NotNull
@Override
public Comparator<ParagraphNode> getChildComparator() {
    return new Comparator<ParagraphNode>() {
        public int compare(final ParagraphNode o1, final ParagraphNode o2) {
            final int thisRegion = o1.getSeqNo() / 1000;
            final int thatRegion = o2.getSeqNo() / 1000;

            if (thisRegion < thatRegion) {
                return -1;
            } else if (thisRegion > thatRegion) {
                return 1;
            }

            return (o1.getPos().getY() < o2.getPos().getY() ? -1 :
                            (o1.getSeqNo() == o2.getSeqNo() ? 0 : 1));
        }
    };
}
}
