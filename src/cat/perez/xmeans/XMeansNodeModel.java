package cat.perez.xmeans;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

import cat.perez.Utils;


/**
 * This is the model implementation of XMeans.
 * 
 *
 * @author Unai & Vicent Perez
 */
public class XMeansNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(XMeansNodeModel.class);
            
    static final String CFGKEY_LOWER_K = "K Lower bound";
    static final String CFGKEY_UPPER_K = "K Upper bound";
    static final String CFGKEY_MAX_ITERATIONS = "Max iterations for K-Means";
    
    static final int DEFAULT_LOWER_K = 3;
    static final int DEFAULT_UPPER_K = 15;
    static final int DEFAULT_MAX_ITERATIONS = 100000;
    
    private final SettingsModelIntegerBounded m_lower_k = 
    		new SettingsModelIntegerBounded(CFGKEY_LOWER_K, DEFAULT_LOWER_K, 3, 100);
    
    private final SettingsModelIntegerBounded m_upper_k = 
    		new SettingsModelIntegerBounded(CFGKEY_UPPER_K, DEFAULT_UPPER_K, 4, 1000);
    
    private final SettingsModelIntegerBounded m_max_iterations =
    		new SettingsModelIntegerBounded(CFGKEY_MAX_ITERATIONS, DEFAULT_MAX_ITERATIONS, 1000, 1000000000);
    
    private DataTableSpec dataOutputSpec;
    private DataTableSpec centroidSpec;
    
    
    
    
    /**
     * Constructor for the node model.
     */
    protected XMeansNodeModel() {
    
        // TODO one incoming port and one outgoing port is assumed
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
    	Map<String, List<Number>> standardizedDataset = new HashMap<>();
    	int index = 0;
    	CloseableRowIterator it = table.iterator();
    	List<List<DataCell>> standardizedData = Utils.standardize(table);
    	while (it.hasNext()) {
    		List<DataCell> l = standardizedData.get(index);
    		DataRow dataRow = it.next();
    		standardizedDataset.put(dataRow.getKey().getString(), l.stream().map(c -> ((DoubleValue) c).getDoubleValue()).collect(Collectors.toList()));
    		index++;
    	}
    	it.close();
    	XMeans xmeans = XMeans.fromNumberList(standardizedDataset, m_lower_k.getIntValue(), m_upper_k.getIntValue());
    	xmeans.run();
    	XMeans.Result bestResult = xmeans.getBestAssignments();
    	bestResult.getRows().forEach((k, v) -> standardizedDataset.get(k).add(v.getAssignment()));
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
		bestResult.getCentroids().forEach((k, v) -> {
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
    	m_lower_k.saveSettingsTo(settings);
    	m_upper_k.saveSettingsTo(settings);
    	m_max_iterations.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_lower_k.loadSettingsFrom(settings);
        m_upper_k.loadSettingsFrom(settings);
        m_max_iterations.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {        
    	if (m_lower_k.getIntValue() >= m_upper_k.getIntValue()) {
    		throw new InvalidSettingsException("Lower K must be lower than upper K");
    	}
    	m_lower_k.validateSettings(settings);
    	m_upper_k.validateSettings(settings);
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

