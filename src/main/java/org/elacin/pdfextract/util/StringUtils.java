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

package org.elacin.pdfextract.util;

import org.apache.pdfbox.util.TextPosition;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA. User: elacin Date: May 19, 2010 Time: 3:46:09 AM To change this
 * template use File | Settings | File Templates.
 */
public class StringUtils {
// -------------------------- PUBLIC STATIC METHODS --------------------------

public static String getTextPositionString(final TextPosition position) {
    StringBuilder sb = new StringBuilder("pos{");
    sb.append("c=\"").append(position.getCharacter()).append("\"");
    sb.append(", XDirAdj=").append(position.getXDirAdj());
    sb.append(", YDirAdj=").append(position.getYDirAdj());
    sb.append(", endY=").append(position.getYDirAdj() + position.getHeightDir());
    sb.append(", endX=").append(position.getXDirAdj() + position.getWidthDirAdj());

    sb.append(", HeightDir=").append(position.getHeightDir());
    sb.append(", WidthDirAdj=").append(position.getWidthDirAdj());

    sb.append(", WidthOfSpace=").append(position.getWidthOfSpace());
    sb.append(", WordSpacing()=").append(position.getWordSpacing());
    sb.append(", FontSize=").append(position.getFontSize());
    sb.append(", getIndividualWidths=").append(Arrays.toString(position.getIndividualWidths()));
    sb.append(", font=").append(position.getFont().getBaseFont());

    sb.append("}");
    return sb.toString();
}
}
