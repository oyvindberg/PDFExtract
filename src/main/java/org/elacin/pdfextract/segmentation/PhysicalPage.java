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

package org.elacin.pdfextract.segmentation;

import org.elacin.pdfextract.segmentation.column.ColumnFinder;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.tree.WordNode;
import org.elacin.pdfextract.util.Rectangle;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Oct 6, 2010 Time: 1:19:56 PM To change this template use File | Settings
 * | File Templates.
 */
public class PhysicalPage {

    final List<WordNode> words;
    //    final WordNode[][] wordRefs;
    private final float height;
    private final float width;
    private final int pageNumber;

    public PhysicalPage(List<WordNode> words, final float height, final float width, int pageNumber) {
        //        wordRefs = new WordNode[width][height];
        this.height = height;
        this.width = width;
        this.words = words;
        this.pageNumber = pageNumber;


        //        for (WordNode word : words) {
        //            final Rectangle position = word.getPosition();
        //            for (int x = position.getX(); x < position.getEndX(); x++) {
        //                for (int y = position.getY(); y < position.getEndY(); y++) {
        //                    wordRefs[x][y] = word;
        //                }
        //            }
        //        }
        //
    }


    public PageNode compileLogicalPage() {

        PageNode ret = new PageNode(pageNumber);
        final List<Rectangle> whitespaces = ColumnFinder.findColumnsFromWordNodes(words, width, height);


        /* use the testpositions to determine column boundaries */

        /* segment w*/
        //        for (WordNode word : words) {
        //            ret.addWord(word);
        //        }

        ret.setWhitespaces(whitespaces);
        return ret;
    }


}
