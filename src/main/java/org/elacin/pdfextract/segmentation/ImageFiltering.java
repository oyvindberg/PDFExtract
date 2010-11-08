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

package org.elacin.pdfextract.segmentation;

import org.apache.log4j.Logger;
import org.elacin.pdfextract.util.RectangleCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/* filter out some things:
        - pictures which are (partly) covered by text which escapes the picture -
            these will be considered background and hence irelevant to this analysis
        - figures which covers the whole page, this happens every once in a while
     */
public class ImageFiltering {
// ------------------------------ FIELDS ------------------------------

private static final Logger log = Logger.getLogger(ImageFiltering.class);

// -------------------------- PUBLIC STATIC METHODS --------------------------

public static void filterImages(final RectangleCollection wholePage,
                                final Collection<Picture> pictures,
                                final Collection<Figure> figures)
{
    Collection<Picture> picturesToDelete = new ArrayList<Picture>();
    Collection<Figure> figuresToDelete = new ArrayList<Figure>();

    for (Picture picture : pictures) {
        if (isConsideredBackgroundPicture(wholePage, picture)) {
            picturesToDelete.add(picture);
            log.info("Filtering out picture as wallpaper " + picture.getPosition());

            //            /* sometimes these background are surrounded by a figure - filter out that as well */
            //            for (Figure figure : figures) {
            //                if (figure.getPosition().equals(picture.getPosition())) {
            //                    log.info("Filtering out figure as it covers wallpaper " + figure.getPosition());
            //                    figuresToDelete.add(figure);
            //                }
            //            }
        } else if (isTooBigPicture(wholePage, picture)) {
            picturesToDelete.add(picture);
            log.info("Filtering out picture as too big " + picture.getPosition());
        }
    }
    pictures.removeAll(picturesToDelete);
    picturesToDelete.clear();
    //    figures.removeAll(figuresToDelete);
    //    figuresToDelete.clear();

    for (Figure figure : figures) {
        if (isConsideredBackgroundPicture(wholePage, figure)) {
            figuresToDelete.add(figure);
            log.info("Filtering out figure as wallpaper " + figure.getPosition());
        } else if (isTooBigPicture(wholePage, figure)) {
            log.info("Filtering out figure as too big " + figure.getPosition());
            figuresToDelete.add(figure);
        }
    }
    figures.removeAll(figuresToDelete);
    figuresToDelete.clear();
}

// -------------------------- STATIC METHODS --------------------------

private static float HEIGHTLIMIT = 30.0f;
private static float HIT_PER_PIXELS = 50.0f;

private static boolean isConsideredBackgroundPicture(RectangleCollection wholePage,
                                                     final PhysicalContent picture)
{
    //    if (true){
    //        return false;
    //    }

    /* it doesnt make sense to label this small/thin pictures as background */
    if (picture.getPosition().getHeight() < HEIGHTLIMIT
            || picture.getPosition().getWidth() < HEIGHTLIMIT) {
        return false;
    }

    List<PhysicalContent> list = wholePage.findContentAtXIndex(picture.getPosition().getX());
    list.addAll(wholePage.findContentAtXIndex(picture.getPosition().getEndX()));

    int intersecting = 0;
    //    int intersectingLimit = Math.max((int) (picture.getPosition().getHeight() / HIT_PER_PIXELS), 2);
    int intersectingLimit = 2;

    for (PhysicalContent content : list) {
        if (content.isText()) {
            boolean makesFiltered = false;
            /* starts left of picture, and ends within it */
            if (content.getPosition().getX() < picture.getPosition().getX() - 1.0f
                    && content.getPosition().getEndX() > picture.getPosition().getX() + 1.0f) {
                makesFiltered = true;
                if (log.isInfoEnabled()) {
                    log.info("LOG00300: wholePage = " + wholePage + ", content = " + content);
                }
            }
            /* starts inside picture, and ends right of it */
            if ((content.getPosition().getEndX() > picture.getPosition().getEndX() + 1.0f
                    && content.getPosition().getX() < picture.getPosition().getEndX())) {
                makesFiltered = true;
                if (log.isInfoEnabled()) {
                    log.info("LOG00310:wholePage = " + wholePage + ", content = " + content);
                }
            }

            if (makesFiltered) {
                intersecting++;
                if (intersecting >= intersectingLimit) {
                    return true;
                }
            }
        }
    }
    return false;
}

private static boolean isTooBigPicture(final RectangleCollection wholePage,
                                       final PhysicalContent picture)
{
    return picture.getPosition().area() >= wholePage.getPosition().area();
}
}
