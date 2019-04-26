package cat.perez.kmeans;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

import cat.perez.Utils;
import cat.perez.xmeans.XMeans;


/**
 * This is the model implementation of KMeansNode.
 * 
 *
 * @author Unai & Vicent Perez
 */
public class KMeansNodeModel extends NodeModel {
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_CLUSTER_AMOUNT = "k";
	static final String CFGKEY_MAX_ITERATION = "Max iterations";

    /** initial default count value. */
    static final int DEFAULT_CLUSTER_AMOUNT = 3;
    
    static final Integer DEFAULT_MAX_ITERATION = 1000000;

    private final SettingsModelIntegerBounded m_cluster_amount =
    		new SettingsModelIntegerBounded(CFGKEY_CLUSTER_AMOUNT, DEFAULT_CLUSTER_AMOUNT, 0, 10);
    private final SettingsModelInteger m_max_iterations = 
    		new SettingsModelInteger(CFGKEY_MAX_ITERATION, DEFAULT_MAX_ITERATION);
    private DataTableSpec dataOutputSpec;
    private DataTableSpec centroidSpec;
    
    /**
     * Constructor for the node model.
     */
    protected KMeansNodeModel() {
        super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	BufferedDataTable table = inData[0];
    	BufferedDataContainer outTable = exec.createDataContainer(this.dataOutputSpec);
    	//Map<String, List<Number>> dataset = new HashMap<>();
    	Map<String, List<Number>> standardizedDataset = new HashMap<>();
    	int index = 0;
    	CloseableRowIterator it = table.iterator();
    	List<List<DataCell>> standardizedData = Utils.standardize(table);
    	while (it.hasNext()) {
    		List<DataCell> l = standardizedData.get(index);
    		DataRow dataRow = it.next();
    		//dataset.put(dataRow.getKey().getString(), dataRow.stream().map(c -> ((DoubleValue) c).getDoubleValue()).collect(Collectors.toList()));
    		standardizedDataset.put(dataRow.getKey().getString(), l.stream().map(c -> ((DoubleValue) c).getDoubleValue()).collect(Collectors.toList()));
    		index++;
    	}
    	it.close();
    	KMeans kmeans = KMeans.fromNumberList(standardizedDataset, this.m_cluster_amount.getIntValue(), this.m_max_iterations.getIntValue());
		kmeans.run();
		kmeans.getAssignments().forEach((k, v) -> standardizedDataset.get(k).add(v.getAssignment()));
		standardizedDataset.forEach((key, v) -> {
			List<DataCell> cells = v.stream().map(n -> {
				if (n instanceof Integer) return new IntCell((Integer) n); 
				else /*if (n instanceof Double)*/ return new DoubleCell((Double) n);
			}).collect(Collectors.toList());
			outTable.addRowToTable(new DefaultRow(key, cells));
		});
		it.close();
		outTable.close();
		BufferedDataContainer centroidTable = exec.createDataContainer(this.centroidSpec);
		kmeans.getCentroids().forEach((k, v) -> {
			List<DataCell> coordCells = Arrays.asList(v).stream().map(DoubleCell::new).collect(Collectors.toList());
			centroidTable.addRowToTable(new DefaultRow(k.toString(), coordCells));
		});
		centroidTable.close();
		return new BufferedDataTable[] {outTable.getTable(), centroidTable.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	DataTableSpec inSpec = inSpecs[0];
    	List<DataColumnSpec> outputColSpec = new ArrayList<>();
    	List<DataColumnSpec> centroidColSpec = new ArrayList<>();
    	inSpec.forEach(outputColSpec::add);
    	inSpec.forEach(c -> centroidColSpec.add(new DataColumnSpecCreator(c.getName(), DoubleCell.TYPE).createSpec()));
    	outputColSpec.add(new DataColumnSpecCreator("k", IntCell.TYPE).createSpec());
    	DataTableSpec outSpec = new DataTableSpec(outputColSpec.toArray(new DataColumnSpec [0]));
    	DataTableSpec centroidSpec = new DataTableSpec(centroidColSpec.toArray(new DataColumnSpec [0]));
    	this.dataOutputSpec = outSpec;
    	this.centroidSpec = centroidSpec;
    	return new DataTableSpec [] {outSpec, centroidSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_cluster_amount.saveSettingsTo(settings);
        m_max_iterations.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {        
        m_cluster_amount.loadSettingsFrom(settings);
    	m_max_iterations.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	
        m_cluster_amount.validateSettings(settings);
        m_max_iterations.validateSettings(settings);

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}

