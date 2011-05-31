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
    private static final Logger        log            = Logger.getLogger(TEIOutput.class);
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

// ------------------------ INTERFACE METHODS ------------------------
// --------------------- Interface XMLWriter ---------------------
    public void writeTree(@NotNull DocumentNode root, File destination) {

        long      t0  = System.currentTimeMillis();
        final TEI tei = OBJECT_FACTORY.createTEI();

        addHeader(root, tei);

        final Text text = OBJECT_FACTORY.createText();

        addFront(root, text);
        addBody(root, text);
        addBack(root, text);
        tei.setText(text);

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance("org.tei_c");
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
    private void addAbstract(final DocumentNode root, @NotNull Front front) {

        if (root.getAbstractParagraph() == null) {
            return;
        }

        final Div  div  = OBJECT_FACTORY.createDiv();
        final Head head = OBJECT_FACTORY.createHead();
        final P    p    = OBJECT_FACTORY.createP();

        div.setType("abs");
        head.getContent().add("ABSTRACT");
        div.getMeetingsAndBylinesAndDatelines().add(head);

        for (LineNode lineNode : root.getAbstractParagraph().getChildren()) {
            addLineToContent(p.getContent(), lineNode);
        }

        div.getMeetingsAndBylinesAndDatelines().add(p);
        front.getSetsAndProloguesAndEpilogues().add(div);
    }

    private void addBack(DocumentNode root, @NotNull Text text) {

        final Back back = OBJECT_FACTORY.createBack();

        /* references goes here */
        text.getIndicesAndSpenAndSpanGrps().add(back);
    }

    Div          currentDiv;
    Div1         currentDiv1;
    Div2         currentDiv2;
    int          divLevel;
    List<Object> currentContent;

    private void addBody(@NotNull DocumentNode root, @NotNull Text text) {

        final Body          body = OBJECT_FACTORY.createBody();
        List<ParagraphNode> prfs = new ArrayList<ParagraphNode>();

        for (PageNode pageNode : root.getChildren()) {
            prfs.addAll(pageNode.getChildren());
        }

        divLevel   = 0;
        currentDiv = OBJECT_FACTORY.createDiv();
        body.getIndicesAndSpenAndSpanGrps().add(currentDiv);
        currentContent = currentDiv.getMeetingsAndBylinesAndDatelines();

        boolean createNewP = false;
        P       currentP   = OBJECT_FACTORY.createP();

        currentContent.add(currentP);

        for (ParagraphNode prf : prfs) {
            boolean isHead = false;

            if (prf.hasRole(Role.DIV1)) {
                divLevel    = 1;
                currentDiv1 = OBJECT_FACTORY.createDiv1();
                body.withIndicesAndSpenAndSpanGrps(currentDiv1);
                currentContent = currentDiv1.getMeetingsAndBylinesAndDatelines();
                isHead         = true;
                createNewP     = true;
            } else if (prf.hasRole(Role.DIV2)) {
                divLevel    = 2;
                currentDiv2 = OBJECT_FACTORY.createDiv2();
                currentDiv1.getMeetingsAndBylinesAndDatelines().add(currentDiv2);
                currentContent = currentDiv2.getMeetingsAndBylinesAndDatelines();
                isHead         = true;
                createNewP     = true;
            }

            if (prf.hasRole(Role.FOOTNOTE)) {
                LineNode firstLine = prf.getChildren().get(0);
                WordNode firstWord = firstLine.getChildren().get(0);
                Note     note      = OBJECT_FACTORY.createNote();

                firstLine.removeChild(firstWord);
                addParagraphToContent(note.getContent(), prf);
                note.withPlaces("below").withNS(firstWord.getText());
//                currentContent.add(note);
                body.withIndicesAndSpenAndSpanGrps(note);

                continue;
            }

            if (isHead) {
                LineNode firstLine = prf.getChildren().get(0);
                String   divName   = "sec" + firstLine.getChildren().get(0).getText();
                Head     head      = OBJECT_FACTORY.createHead().withId(divName);

                firstLine.removeChild(firstLine.getChildren().get(0));
                addParagraphToContent(head.getContent(), prf);
                currentContent.add(head);
            } else {
                if (createNewP) {
                    currentP = OBJECT_FACTORY.createP();
                    currentContent.add(currentP);
                    createNewP = false;
                }

                for (LineNode line : prf.getChildren()) {
                    boolean indented = line.isIndented();

                    if (!currentP.getContent().isEmpty() && indented) {
                        currentP = OBJECT_FACTORY.createP();
                        currentContent.add(currentP);
                    }

                    addLineToContent(currentP.getContent(), line);
                }
            }
        }

        for (PageNode pageNode : root.getChildren()) {
            for (GraphicsNode graphicsNode : pageNode.getGraphics()) {
                if (graphicsNode.getText().isEmpty()){
                    continue;
                }

                Figure figure     = OBJECT_FACTORY.createFigure();
                Body   figureBody = OBJECT_FACTORY.createBody();
                Div    figureDiv  = OBJECT_FACTORY.createDiv();
                P      p          = OBJECT_FACTORY.createP();


                for (ParagraphNode paragraphNode : graphicsNode.getChildren()) {
                    addParagraphToContent(p.getContent(), paragraphNode);
                }

                figure.getHeadsAndPSAndAbs().add(p);
                body.getIndicesAndSpenAndSpanGrps().add(figure);
            }
        }

        text.getIndicesAndSpenAndSpanGrps().add(body);
    }

    private void addLineToContent(final List<Object> contentList, final LineNode line) {

        String content = line.getText();

        if (!contentList.isEmpty()) {
            String former = (String) contentList.get(contentList.size() - 1);

            if (former.endsWith("-")) {
                String combined = former.substring(0, former.length() - 1) + content;

                contentList.remove(contentList.size() - 1);
                contentList.add(combined);

                return;
            }
        }

        contentList.add(content);
    }

    private void addParagraphToContent(final List<Object> content, final ParagraphNode prf) {

        for (LineNode line : prf.getChildren()) {
            addLineToContent(content, line);
        }
    }

    private void addFront(DocumentNode root, @NotNull Text text) {

        final Front front = OBJECT_FACTORY.createFront();

        addAbstract(root, front);
        text.getIndicesAndSpenAndSpanGrps().add(front);
    }

    private void addHeader(DocumentNode root, @NotNull TEI tei) {

        ParagraphNode title1 = root.getTitle();

        if (title1 == null) {
            return;
        }

        final TeiHeader header   = OBJECT_FACTORY.createTeiHeader();
        final FileDesc  fileDesc = OBJECT_FACTORY.createFileDesc();

        /* title, author and editor */
        final TitleStmt titleStmt = OBJECT_FACTORY.createTitleStmt();
        final Title     title     = OBJECT_FACTORY.createTitle();

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
