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



package org.elacin.pdfextract.xml;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.tree.*;
import org.elacin.pdfextract.tree.Role;
import org.jetbrains.annotations.NotNull;
import org.tei_c.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: elacin Date: 14.01.11 Time: 17.02 To change this template use
 * File | Settings | File Templates.
 */
public class TEIOutput implements XMLWriter {

// ------------------------------ FIELDS ------------------------------
    private static final Logger log = Logger.getLogger(TEIOutput.class);

// ------------------------ INTERFACE METHODS ------------------------
// --------------------- Interface XMLWriter ---------------------
    public void writeTree(@NotNull DocumentNode root, File destination) {

        long          t0  = System.currentTimeMillis();
        ObjectFactory of  = new ObjectFactory();
        final TEI     tei = of.createTEI();

        addHeader(root, of, tei);

        final Text text = of.createText();

        addFront(root, of, text);
        addBody(root, of, text);
        addBack(root, of, text);
        tei.setText(text);

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance("org.elacin.teischema");
            Marshaller  marshaller  = jaxbContext.createMarshaller();

            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(tei, new FileOutputStream(destination));
        } catch (JAXBException e) {
            log.warn("LOG01140:", e);

            return;
        } catch (FileNotFoundException e) {
            log.warn("LOG01120:", e);

            return;
        }

        long time = System.currentTimeMillis() - t0;

        if (log.isInfoEnabled()) {
            log.info("LOG01510:" + TEIOutput.class + " took " + time + "ms");
        }
    }

// -------------------------- OTHER METHODS --------------------------
    private void addAbstract(final DocumentNode root, @NotNull ObjectFactory of, @NotNull Front front) {

        if (root.getAbstractParagraph() == null) {
            return;
        }

        final Div div = of.createDiv();

        div.setType("abs");

        final Head head = of.createHead();

        head.getContent().add("ABSTRACT");
        div.getMeetingsAndBylinesAndDatelines().add(head);

        final P p = of.createP();

        for (LineNode lineNode : root.getAbstractParagraph().getChildren()) {
            addLineToP(p, lineNode);
        }

        div.getMeetingsAndBylinesAndDatelines().add(p);
        front.getSetsAndProloguesAndEpilogues().add(div);
    }

    private void addBack(DocumentNode root, @NotNull ObjectFactory of, @NotNull Text text) {

        final Back back = of.createBack();

//        for (PageNode page : root.getChildren()) {
//            for (ParagraphNode prf : page.getChildren()) {
//                if (prf.hasRole(Role.FOOTNOTE)){
//                    FloatingText floatingText = of.createFloatingText();
//
//                    P p = addTextToP(of, prf);
//
//                    floatingText.getIndicesAndSpenAndSpanGrps().add(p);
//                    back.getSetsAndProloguesAndEpilogues().add(floatingText);
//                }
//            }
//        }
//

        /* references goes here */
        text.getIndicesAndSpenAndSpanGrps().add(back);
    }

    Div          currentDiv  = null;
    Div1         currentDiv1 = null;
    Div2         currentDiv2 = null;
    int          divLevel    = 0;
    List<Object> currentContent;

    private void addBody(@NotNull DocumentNode root, @NotNull ObjectFactory of, @NotNull Text text) {

        final Body          body = of.createBody();
        List<ParagraphNode> prfs = new ArrayList<ParagraphNode>();

        for (PageNode pageNode : root.getChildren()) {
            prfs.addAll(pageNode.getChildren());
        }

        divLevel   = 0;
        currentDiv = of.createDiv();
        body.getIndicesAndSpenAndSpanGrps().add(currentDiv);
        currentContent = currentDiv.getMeetingsAndBylinesAndDatelines();

        boolean createNewP = false;
        P       currentP   = of.createP();

        currentContent.add(currentP);

        for (ParagraphNode prf : prfs) {
            boolean isHead = false;

            if (prf.hasRole(Role.DIV1)) {
                divLevel    = 1;
                currentDiv1 = of.createDiv1();
                body.getIndicesAndSpenAndSpanGrps().add(currentDiv1);
                currentContent = currentDiv1.getMeetingsAndBylinesAndDatelines();
                isHead         = true;
                createNewP     = true;
            } else if (prf.hasRole(Role.DIV2)) {
                divLevel    = 2;
                currentDiv2 = of.createDiv2();
                currentDiv1.getMeetingsAndBylinesAndDatelines().add(currentDiv2);
                currentContent = currentDiv2.getMeetingsAndBylinesAndDatelines();
                isHead         = true;
                createNewP     = true;
            }

            if (prf.hasRole(Role.FOOTNOTE)) {

                Note note = of.createNote();
//                note.set
                FloatingText floatingText = of.createFloatingText();
                Body         floatBody    = of.createBody();
                Div          floatDiv     = of.createDiv();
                P            p            = of.createP();
                LineNode     firstLine    = prf.getChildren().get(0);

                floatingText.setType("footnote");

                WordNode firstWord = firstLine.getChildren().get(0);

                firstLine.removeChild(firstWord);
                floatingText.setId("footnote" + firstWord.getText());
                addTextToP(of, prf, p);
                floatDiv.getMeetingsAndBylinesAndDatelines().add(p);
                floatBody.getIndicesAndSpenAndSpanGrps().add(floatDiv);
                floatingText.getIndicesAndSpenAndSpanGrps().add(floatBody);
                body.getIndicesAndSpenAndSpanGrps().add(floatingText);

                continue;
            }

            if (isHead) {
                Head     head      = of.createHead();
                LineNode firstLine = prf.getChildren().get(0);
                String   divName   = "sec" + firstLine.getChildren().get(0).getText();

                head.setId(divName);
                firstLine.removeChild(firstLine.getChildren().get(0));
                head.getContent().add(prf.getText());
                currentContent.add(head);
            } else {
                if (createNewP) {
                    currentP = of.createP();
                    currentContent.add(currentP);
                    createNewP = false;
                }

                for (LineNode line : prf.getChildren()) {
                    if (!currentP.getContent().isEmpty() && line.isIndented()) {
                        currentP = of.createP();
                        currentContent.add(currentP);
                    }

                    addLineToP(currentP, line);
                }
            }
        }

        for (PageNode pageNode : root.getChildren()) {
            for (GraphicsNode graphicsNode : pageNode.getGraphics()) {
                Figure figure     = of.createFigure();
                Body   figureBody = of.createBody();
                Div    figureDiv  = of.createDiv();
                P      p          = of.createP();

                for (ParagraphNode paragraphNode : graphicsNode.getChildren()) {
                    addTextToP(of, paragraphNode, p);
                }

//                figureDiv.getMeetingsAndBylinesAndDatelines().add(p);
//                figureBody.getIndicesAndSpenAndSpanGrps().add(figureDiv);
                figure.getHeadsAndPSAndAbs().add(p);
                body.getIndicesAndSpenAndSpanGrps().add(figure);
            }
        }

        text.getIndicesAndSpenAndSpanGrps().add(body);
    }

    private void addLineToP(final P currentP, final LineNode line) {

        String content = line.getText();

        if (!currentP.getContent().isEmpty()) {
            String former = (String) currentP.getContent().get(currentP.getContent().size() - 1);

            if (former.endsWith("-")) {
                String combined = former.substring(0, former.length() - 1) + content;

                currentP.getContent().remove(currentP.getContent().size() - 1);
                currentP.getContent().add(combined);

                return;
            }
        }

        currentP.getContent().add(content);
    }

    private void addTextToP(final ObjectFactory of, final ParagraphNode prf, P p) {

        for (LineNode line : prf.getChildren()) {
            addLineToP(p, line);
        }

//        if (!p.getContent().isEmpty()) {
//            p.getContent().remove(p.getContent().size() - 1);
//        }
    }

    private void addFront(DocumentNode root, @NotNull ObjectFactory of, @NotNull Text text) {

        final Front front = of.createFront();

        addAbstract(root, of, front);
        text.getIndicesAndSpenAndSpanGrps().add(front);
    }

    private void addHeader(DocumentNode root, @NotNull ObjectFactory of, @NotNull TEI tei) {

        ParagraphNode title1 = root.getTitle();

        if (title1 == null) {
            return;
        }

        final TeiHeader header   = of.createTeiHeader();
        final FileDesc  fileDesc = of.createFileDesc();

        /* title, author and editor */
        final TitleStmt titleStmt = of.createTitleStmt();
        final Title     title     = of.createTitle();

        for (LineNode lineNode : title1.getChildren()) {
            title.getContent().add(lineNode.getText());
        }

        titleStmt.getTitles().add(title);

//        final Author author = of.createAuthor();
//
//        author.getContent().add("meg");
//        author.setRole("author-rolle");
//        titleStmt.getAuthorsAndEditorsAndRespStmts().add(author);
//
//        final Editor editor = of.createEditor();
//
//        editor.setBase("editoren");
//        editor.setRole("editor-rolle");
//        titleStmt.getAuthorsAndEditorsAndRespStmts().add(editor);
//
//
//        /* publication details */
//        final PublicationStmt publicationStmt = of.createPublicationStmt();
//        final Address         address         = of.createAddress();
//
//        address.setBase("laueveien");
//
//        final Publisher publisher = of.createPublisher();
//
//        publisher.setBase("publishern");
//        publicationStmt.getAddressesAndDatesAndPublishers().add(address);
//        publicationStmt.getAddressesAndDatesAndPublishers().add(publisher);
//
        fileDesc.setTitleStmt(titleStmt);

//        fileDesc.setPublicationStmt(publicationStmt);
        header.setFileDesc(fileDesc);
        tei.setTeiHeader(header);
    }
}
