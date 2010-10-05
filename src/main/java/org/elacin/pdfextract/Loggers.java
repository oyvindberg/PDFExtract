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

package org.elacin.pdfextract;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Apr 19, 2010
 * Time: 2:50:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class Loggers {
    static {
        PropertyConfigurator.configure(Loggers.class.getClassLoader().getResource("log4j.xml"));
    }

// -------------------------- PUBLIC STATIC METHODS --------------------------

    public static Logger getCreateTreeLog() {
        return Logger.getLogger("org.elacin.pdfextract.createTree");
    }

    public static Logger getPdfExtractorLog() {
        return Logger.getLogger("org.elacin.pdfextract");
    }

    public static Logger getPdfboxLog() {
        return Logger.getLogger("org.apache.pdfbox");
    }

    public static Logger getWordBuilderLog() {
        return Logger.getLogger("org.elacin.pdfextract.wordBuilder");
    }
}
