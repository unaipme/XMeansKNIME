package cat.perez.kmeans;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "KMeansNode" Node.
 * 
 *
 * @author Unai & Vicent Perez
 */
public class KMeansNodeFactory 
        extends NodeFactory<KMeansNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public KMeansNodeModel createNodeModel() {
        return new KMeansNodeModel();
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
    public NodeView<KMeansNodeModel> createNodeView(final int viewIndex,
            final KMeansNodeModel nodeModel) {
        return null;
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
        return new KMeansNodeDialog();
    }

}

