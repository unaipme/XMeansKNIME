package cat.perez.xmeans;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * <code>NodeDialog</code> for the "XMeans" Node.
 * 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Unai & Vicent Perez
 */
public class XMeansNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring XMeans node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected XMeansNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                    XMeansNodeModel.CFGKEY_LOWER_K,
                    XMeansNodeModel.DEFAULT_LOWER_K,
                    3, 100),
                    "K Lower bound:", /*step*/ 1, /*componentwidth*/ 5));
        
        addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                    XMeansNodeModel.CFGKEY_UPPER_K,
                    XMeansNodeModel.DEFAULT_UPPER_K,
                    4, 1000),
                    "K Upper bound:", /*step*/ 1, /*componentwidth*/ 5));
        
        addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                    XMeansNodeModel.CFGKEY_MAX_ITERATIONS,
                    XMeansNodeModel.DEFAULT_MAX_ITERATIONS,
                    1000, 1000000000),
                    "Max iterations for K-Means:", /*step*/ 1000, /*componentwidth*/ 10));
                    
    }
}

