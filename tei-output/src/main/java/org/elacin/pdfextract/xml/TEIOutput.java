package org.elacin.pdfextract.xml;/*
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

import org.apache.log4j.Logger;
import org.elacin.pdfextract.teischema.*;
import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.tree.LayoutRegionNode;
import org.elacin.pdfextract.tree.PageNode;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: 14.01.11
 * Time: 17.02
 * To change this template use File | Settings | File Templates.
 */
public class TEIOutput implements XMLWriter {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(TEIOutput.class);

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface XMLWriter ---------------------

public void writeTree(@NotNull DocumentNode root, File destination) {
    ObjectFactory of = new ObjectFactory();
    final TEI tei = of.createTEI();

    addHeader(root, of, tei);

    final Text text = of.createText();

    addFront(root, of, text);
    addBody(root, of, text);
    addBack(root, of, text);
    tei.setText(text);


    try {
        JAXBContext jaxbContext = JAXBContext.newInstance("org.elacin.pdfextract.teischema");
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(tei, new FileOutputStream(destination));
    } catch (JAXBException e) {
        log.warn("LOG01140:", e);
    } catch (FileNotFoundException e) {
        log.warn("LOG01120:", e);
    }
}

// -------------------------- OTHER METHODS --------------------------

private void addAbstract(ObjectFactory of, Front front) {
    final Div div = of.createDiv();
    div.setType("abs");

    final Head head = of.createHead();
    head.getContent().add("ABSTRACT");
    div.getMeetingsAndBylinesAndDatelines().add(head);

    final P p = of.createP();
    p.getContent().add("Hey! I live in ");
    final Country country = of.createCountry();
    country.getContent().add("Norway");
    p.getContent().add(country);
    p.getContent().add(". :D");

    div.getMeetingsAndBylinesAndDatelines().add(p);

    front.getSetsAndProloguesAndEpilogues().add(div);
}

private void addBack(DocumentNode root, ObjectFactory of, Text text) {
    final Back back = of.createBack();

    /* references goes here */

    text.getIndicesAndSpenAndSpanGrps().add(back);
}

private void addBody(DocumentNode root, ObjectFactory of, Text text) {
    final Body body = of.createBody();

    /* add content here */
    for (PageNode pageNode : root.getChildren()) {
        for (LayoutRegionNode regionNode : pageNode.getChildren()) {
        }
    }


    text.getIndicesAndSpenAndSpanGrps().add(body);
}

private void addFront(DocumentNode root, ObjectFactory of, Text text) {
    final Front front = of.createFront();

    addAbstract(of, front);
    text.getIndicesAndSpenAndSpanGrps().add(front);
}

private void addHeader(DocumentNode root, ObjectFactory of, TEI tei) {
    final TeiHeader header = of.createTeiHeader();
    final FileDesc fileDesc = of.createFileDesc();

    /* title, author and editor*/
    final TitleStmt titleStmt = of.createTitleStmt();
    final Title title = of.createTitle();
    title.getContent().add("TITLE! :D");
    titleStmt.getTitles().add(title);


    final Author author = of.createAuthor();
    author.getContent().add("meg");
    author.setRole("author-rolle");
    titleStmt.getAuthorsAndEditorsAndRespStmts().add(author);
    final Editor editor = of.createEditor();
    editor.setBase("editoren");
    editor.setRole("editor-rolle");
    titleStmt.getAuthorsAndEditorsAndRespStmts().add(editor);
    fileDesc.setTitleStmt(titleStmt);

    /* publication details */
    final PublicationStmt publicationStmt = of.createPublicationStmt();
    final Address address = of.createAddress();
    address.setBase("laueveien");
    final Publisher publisher = of.createPublisher();
    publisher.setBase("publishern");
    publicationStmt.getAddressesAndDatesAndPublishers().add(address);
    publicationStmt.getAddressesAndDatesAndPublishers().add(publisher);
    fileDesc.setPublicationStmt(publicationStmt);

    header.setFileDesc(fileDesc);
    tei.setTeiHeader(header);
}
}
