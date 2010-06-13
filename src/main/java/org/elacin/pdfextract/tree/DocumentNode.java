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

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.elacin.pdfextract.Loggers;
import org.elacin.pdfextract.builder.DocumentStyles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Mar 24, 2010
 * Time: 12:17:02 AM
 * To change this template use File | Settings | File Templates.
 */
@XStreamAlias("document")
public class DocumentNode extends AbstractParentNode<PageNode, DocumentNode> {
    // ------------------------------ FIELDS ------------------------------

    @XStreamOmitField
    public final List<WordNode> words = new ArrayList<WordNode>();

    /**
     * this contains all the different styles used in the document
     */
    protected final DocumentStyles styles = new DocumentStyles();

    // --------------------------- CONSTRUCTORS ---------------------------

    public DocumentNode() {
        setRoot(this);
    }

    // --------------------- GETTER / SETTER METHODS ---------------------

    public DocumentStyles getStyles() {
        return styles;
    }

    // -------------------------- PUBLIC METHODS --------------------------

    @Override
    public boolean addWord(final WordNode node) {
        if (node.getText().trim().length() == 0) {
            if (Loggers.getCreateTreeLog().isTraceEnabled()) {
                Loggers.getCreateTreeLog().trace("Ignoring word " + node + " because it contains no text");
            }
            return false;
        }


        for (PageNode pageNode : getChildren()) {
            if (pageNode.addWord(node)) return true;
        }

        final PageNode newPage = new PageNode(node.getPageNum());
        addChild(newPage);
        newPage.addWord(node);
        return false;
    }

    @Override
    public void combineChildren() {
        for (PageNode pageNode : getChildren()) {
            pageNode.combineChildren();
        }
    }

    /**
     * Returns a Comparator which will compare pagenumbers of the pages
     *
     * @return
     */
    @Override
    public Comparator<PageNode> getChildComparator() {
        return new Comparator<PageNode>() {
            public int compare(final PageNode o1, final PageNode o2) {
                if (o1.getPage().getPageNumber() < o2.getPage().getPageNumber()) return -1;
                else if (o1.getPage().getPageNumber() > o2.getPage().getPageNumber()) return 1;

                return 0;
            }
        };
    }
}
