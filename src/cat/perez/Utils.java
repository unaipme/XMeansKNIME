package cat.perez;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;

public abstract class Utils {
	
	public static Map<Integer, Double> colAvg(BufferedDataTable in) {
    	Map<Integer, Double> columnAvg = new HashMap<>();
    	CloseableRowIterator it = in.iterator();
    	while (it.hasNext()) {
    		Iterator<DataCell> cellIterator = it.next().iterator();
    		int index = 0;
    		while (cellIterator.hasNext()) {
    			DataCell cell = cellIterator.next();
    			if (cell.getType().equals(IntCell.TYPE) || cell.getType().equals(DoubleCell.TYPE)) {
    				DoubleValue dValue = ((DoubleValue) cell);
    				columnAvg.put(index, columnAvg.getOrDefault(index, 0.0) + dValue.getDoubleValue() / in.size());
    			}
    			index++;
    		}
    	}
    	return columnAvg;
    }
    
    public static Map<Integer, Double> colStdDev(BufferedDataTable in) {
    	Map<Integer, Double> columnAvg = colAvg(in);
    	Map<Integer, Double> columnStdDev = new HashMap<>();
    	CloseableRowIterator it = in.iterator();
    	while (it.hasNext()) {
    		Iterator<DataCell> cellIterator = it.next().iterator();
    		int index = 0;
    		while (cellIterator.hasNext()) {
    			DataCell cell = cellIterator.next();
    			if (cell.getType().equals(IntCell.TYPE) || cell.getType().equals(DoubleCell.TYPE)) {
    				DoubleValue dValue = ((DoubleValue) cell);
    				double value = columnStdDev.getOrDefault(index, 0.0);
    				value += Math.pow(dValue.getDoubleValue() - columnAvg.get(index), 2) / (in.size() - 1);
    				columnStdDev.put(index, value);
    			}
    			index++;
    		}
    	}
    	columnStdDev.forEach((k, v) -> columnStdDev.put(k, Math.sqrt(v)));
    	it.close();
    	return columnStdDev;
    }
    
    public static List<List<DataCell>> standardize(BufferedDataTable in) {
    	List<List<DataCell>> out = new ArrayList<>();
		CloseableRowIterator it = in.iterator();
		Map<Integer, Double> columnAvg = colAvg(in);
		Map<Integer, Double> columnStdDev = colStdDev(in);
		while (it.hasNext()) {
			DataRow row = it.next();
			List<DataCell> cells = new ArrayList<>();
			Iterator<DataCell> cellIterator = row.iterator();
			int index = 0;
			while (cellIterator.hasNext()) {
				DataCell cell = cellIterator.next();
				if (!(cell.getType().equals(IntCell.TYPE) || cell.getType().equals(DoubleCell.TYPE))) {
					cells.add(cell);
				} else {
					DoubleValue dValue = ((DoubleValue) cell);
					cells.add(new DoubleCell((dValue.getDoubleValue() - columnAvg.get(index)) / columnStdDev.get(index)));
				}
				index++;
			}
			out.add(cells);
		}
    	return out;
    }
}
