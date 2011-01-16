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

package org.elacin.pdfextract.input.pdfbox;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.elacin.pdfextract.input.DocumentContent;
import org.elacin.pdfextract.input.PDFSource;
import org.elacin.pdfextract.util.Loggers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: 15.01.11
 * Time: 19.57
 * To change this template use File | Settings | File Templates.
 */
public class PDFBoxSource implements PDFSource {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(PDFBoxSource.class);

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface PDFSource ---------------------

public DocumentContent readPages(File pdfDocument, int startPage, int endPage, String password) {
    final long t0 = System.currentTimeMillis();
    final PDDocument doc = openPdfDocument(pdfDocument, password);

    PDFBoxIntegration stripper;
    try {
        stripper = new PDFBoxIntegration(doc, startPage, endPage);
        stripper.processDocument();
        doc.close();
    } catch (IOException e) {
        throw new RuntimeException("Error while reading document", e);
    }

    final long td = System.currentTimeMillis() - t0;
    Loggers.getInterfaceLog().info("LOG01190:Read document in " + td + " ms");
    return stripper.getContents();
}

// -------------------------- STATIC METHODS --------------------------

protected static PDDocument openPdfDocument(@NotNull final File pdfFile,
                                            @Nullable final String password) {
    long t0 = System.currentTimeMillis();
    MDC.put("doc", pdfFile.getName());
    Loggers.getInterfaceLog().info("LOG00120:Opening PDF file " + pdfFile + ".");

    try {
        final PDDocument document = PDDocument.load(pdfFile);
        if (document.isEncrypted()) {
            if (password != null) {
                try {
                    document.decrypt(password);
                } catch (Exception e) {
                    throw new RuntimeException("Error while reading encrypted PDF:", e);
                }
            } else {
                Loggers.getInterfaceLog().warn("File claims to be encrypted, a password should be provided");
            }
        }

        Loggers.getInterfaceLog().debug(
                "LOG00130:PDFBox load() took " + (System.currentTimeMillis() - t0) + "ms");
        return document;
    } catch (IOException e) {
        MDC.put("doc", "");
        throw new RuntimeException("Error while reading " + pdfFile + ".", e);
    }
}
}
