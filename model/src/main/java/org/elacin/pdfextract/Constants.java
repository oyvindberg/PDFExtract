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

package org.elacin.pdfextract;

import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: 16.01.11
 * Time: 19.57
 * To change this template use File | Settings | File Templates.
 */
public class Constants {
// ------------------------------ FIELDS ------------------------------

/* segmentation */
public static final boolean USE_EXISTING_WHITESPACE    = true;
public static final boolean SPLIT_PARAGRAPHS_BY_STYLES = true;

/* technical */
public static final boolean RECTANGLE_COLLECTION_CACHE_ENABLED = true;

/* rendering */
public static final boolean RENDER_ENABLED    = true;
public static final boolean RENDER_REAL_PAGE  = false;
public static final int     RENDER_RESOLUTION = 150;
public static final int     RENDER_DPI        = 72;
public static final boolean RENDER_WHITESPACE = true;

/* output */

public static final boolean VERBOSE_OUTPUT        = false;
public static final boolean SIMPLE_OUTPUT_ENABLED = true;
public static final boolean TEI_OUTPUT_ENABLED    = false;

@NotNull
public static String SIMPLE_OUTPUT_EXTENSION = ".xml";
@NotNull
public static String TEI_OUTPUT_EXTENSION    = ".tei.xml";
}
