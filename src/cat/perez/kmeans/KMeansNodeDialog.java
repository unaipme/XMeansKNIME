package cat.perez.kmeans;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;

/**
 * <code>NodeDialog</code> for the "KMeansNode" Node.
 * 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Unai & Vicent Perez
 */
public class KMeansNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring KMeansNode node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected KMeansNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentNumber(
                new SettingsModelInteger(
                    KMeansNodeModel.CFGKEY_CLUSTER_AMOUNT,
                    KMeansNodeModel.DEFAULT_CLUSTER_AMOUNT),
                    "Choose cluster amount:", /*step*/ 1, /*componentwidth*/ 30));
        
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelInteger(
        			KMeansNodeModel.CFGKEY_MAX_ITERATION,
        			KMeansNodeModel.DEFAULT_MAX_ITERATION),
        		"Choose max iteration amount:", 100, 20));
    }
}

