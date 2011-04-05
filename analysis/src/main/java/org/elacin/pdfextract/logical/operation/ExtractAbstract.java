/*
 * Copyright 2010-2011 Øyvind Berg (elacin@gmail.com)
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



package org.elacin.pdfextract.logical.operation;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.logical.Operation;
import org.elacin.pdfextract.style.StyleDifference;
import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.tree.ParagraphNode;

import java.util.List;

import static org.elacin.pdfextract.style.StyleComparator.styleCompare;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 31.01.11 Time: 10.41 To change this template use
 * File | Settings | File Templates.
 */
public class ExtractAbstract implements Operation {

    private static final Logger log = Logger.getLogger(ExtractAbstract.class);

    public void doOperation(final DocumentNode root) {

        if (root.getWords().isEmpty() || root.getChildren().isEmpty()) {
            throw new RuntimeException("tried to analyze empty document");
        }

        PageNode            firstPage = root.getChildren().get(0);
        List<ParagraphNode> prfs      = firstPage.getChildren();

        for (int i = 0; i < prfs.size(); i++) {
            final ParagraphNode absTitlePrf = prfs.get(i);

            if (absTitlePrf.getText().trim().toLowerCase().equals("abstract")
                    && (i + 1 != prfs.size())) {
                ParagraphNode abstractParagraph = prfs.get(++i);

                i++;

                while (true) {
                    if (i == prfs.size()) {
                        break;
                    }

                    ParagraphNode   next = prfs.get(i);
                    StyleDifference diff = styleCompare(next.getStyle(), abstractParagraph.getStyle());

                    if (diff != StyleDifference.SAME_STYLE) {
                        break;
                    }

                    abstractParagraph.addChildren(next.getChildren());
                    prfs.remove(i);
                }

                root.setAbstractParagraph(abstractParagraph);
                prfs.remove(abstractParagraph);
                prfs.remove(absTitlePrf);

                if (log.isInfoEnabled()) {
                    String t    = abstractParagraph.getText();
                    String text = t.substring(0, Math.min(30, t.length()));

                    log.info("LOG01460:Found abstract with text " + text);
                }
            }
        }
    }
}
