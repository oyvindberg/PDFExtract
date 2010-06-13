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

package org.elacin.pdfextract.util.xml;

import com.thoughtworks.xstream.converters.SingleValueConverter;
import org.elacin.pdfextract.util.Rectangle;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Jun 4, 2010
 * Time: 3:48:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class RectangleConverter implements SingleValueConverter {

    public boolean canConvert(final Class type) {
        return type.isAssignableFrom(Rectangle.class);
    }

    public String toString(final Object obj) {
        return obj.toString();
    }

    public Object fromString(final String str) {
        return null;
    }
}
