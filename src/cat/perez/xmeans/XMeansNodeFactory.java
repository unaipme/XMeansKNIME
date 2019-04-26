package cat.perez.xmeans;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "XMeans" Node.
 * 
 *
 * @author Unai & Vicent Perez
 */
public class XMeansNodeFactory 
        extends NodeFactory<XMeansNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public XMeansNodeModel createNodeModel() {
        return new XMeansNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<XMeansNodeModel> createNodeView(final int viewIndex,
            final XMeansNodeModel nodeModel) {
        return new XMeansNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new XMeansNodeDialog();
    }

}

