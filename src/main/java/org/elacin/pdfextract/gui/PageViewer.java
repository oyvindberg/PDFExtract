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

package org.elacin.pdfextract.gui;

import org.elacin.pdfextract.renderer.PageRenderer;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Jun 17, 2010 Time: 4:55:09 AM To change this
 * template use File | Settings | File Templates.
 */
public class PageViewer extends JPanel {
// ------------------------ OVERRIDING METHODS ------------------------

@Override
public Dimension getMinimumSize() {
    return new Dimension(500, 500);
}

@Override
public Dimension getPreferredSize() {
    return new Dimension(700, 1000);
}

// -------------------------- PUBLIC METHODS --------------------------

public void setData(PageRenderer renderer) {
    //        BufferedImage myPicture = ImageIO.read(new File("path-to-file"));
    //        JLabel picLabel = new JLabel(new ImageIcon(myPicture))
}
}
