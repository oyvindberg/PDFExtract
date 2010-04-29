package org.elacin.extract.tree;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Mar 24, 2010
 * Time: 12:17:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class DocumentNode extends AbstractParentNode<PageNode, DocumentNode> {
    private PDDocument pdf;

    public final List<AbstractNode> allNodes = new ArrayList<AbstractNode>();
    public final TreeSet<TextNode> textNodes = new TreeSet<TextNode>();

    public DocumentNode(final PDDocument pdf) {
        this.pdf = pdf;
    }


    public PDDocument getPdf() {
        return pdf;
    }

    @Override
    public boolean addTextNode(final TextNode node) {
        //TODO: implement pages and blabla here instead
        return false;
    }

    /**
     * Returns a Comparator which will compare pagenumbers of the pages
     *
     * @return
     */
    @Override
    public Comparator<PageNode> getChildComparator() {
        return new Comparator<PageNode>() {
            public int compare(final PageNode o1, final PageNode o2) {
                if (o1.getPage().getPageNumber() < o2.getPage().getPageNumber()) return -1;
                else if (o1.getPage().getPageNumber() > o2.getPage().getPageNumber()) return 1;

                return 0;
            }
        };
    }

    @Override
    public void combineChildren() {
        for (PageNode pageNode : getChildren()) {
            pageNode.combineChildren();
        }
    }
}
