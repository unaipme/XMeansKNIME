package cat.perez.kmeans;

import static java.util.stream.Collectors.toMap;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.function.Function;

import org.knime.core.data.DataRow;

import cat.perez.kmeans.KMeans.Row;

public class KMeans {
	
	private Map<String, Row> dataset;
	private int K;
	private Random random = new Random();
	private int maxIterations = 100000;
	private int D;
	
	
	private Map<Integer, Double []> centroids = new HashMap<>();
	
	public KMeans(Map<String, Row> dataset, int K, int maxIterations) {
		this.dataset = dataset;
		this.K = K;
		this.maxIterations = maxIterations;
		this.D = 0;
    	dataset.values().stream().findAny().ifPresent(r -> this.D = r.getRow().size());
	}
	
	public static KMeans fromNumberList(Map<String, List<Number>> dataset, int K, int maxIterations) {
		return new KMeans(dataset.entrySet().stream().map(e -> new SimpleEntry<>(e.getKey(), new Row(e.getValue()))).collect(toMap(Entry::getKey, Entry::getValue)), K, maxIterations);
	}
	
	public void run() {
		boolean convergence = false;
		int iteration = 0;
		initialize();
		do {
			convergence = !this.assign();
			this.recalculate();
		} while (!convergence && ++iteration < this.maxIterations);
	}
	
	private void initialize() {
		if (this.centroids == null || centroids.isEmpty()) {
			System.out.println("K-Means Centroids are initialized");
			for (int i = 0; i < this.K; i++) {
				Double [] centroid = new Double [this.D];
				Row row = new ArrayList<>(dataset.values()).get(random.nextInt(dataset.size()));
				for (int j = 0; j < this.D; j++) centroid[j] = row.getRow().get(j).doubleValue();
				centroids.put(i, centroid);
			}
		}
		for (Row row : dataset.values()) {
			row.setAssignment(-1);
		}
	}
	
	private boolean assign() {
    	boolean anIndividualChanged = false;
		
		// Assignation
		for (Row row : dataset.values()) {
			List<Number> observation = row.getRow();
			
			int k = centroids.entrySet().stream().map(entry -> {
				double sum = 0;
				for (int i = 0; i < D; i++) sum += Math.pow(observation.get(i).doubleValue() - entry.getValue()[i], 2);
				return new SimpleEntry<Integer, Double>(entry.getKey(), Math.sqrt(sum));
			}).min((a1, a2) -> ((Double) a1.getValue()).compareTo((Double) a2.getValue())).get().getKey();
			
			if (row.getAssignment() != k) {
				row.setAssignment(k);
				anIndividualChanged = true;
			}
		}
		return anIndividualChanged;
	}
	
	private void recalculate() {
		// Recalculation
		Map<Integer, List<List<Number>>> clusters = new HashMap<>();
		dataset.forEach((k, row) -> {
			List<List<Number>> c = clusters.getOrDefault(row.getAssignment(), new ArrayList<>());
			c.add(row.getRow());
			clusters.put(row.getAssignment(), c);
		});
		clusters.forEach((cluster, pob) -> {
			Double [] coord = centroids.get(cluster);
			for (int c = 0; c < coord.length; c++) {
				final int c2 = c;
				coord[c] = pob.stream().mapToDouble(ob -> ob.get(c2).doubleValue()).average().getAsDouble();
			}
			centroids.put(cluster, coord);
		});
	}
	
	public Map<Integer, Double []> getCentroids() {
		return this.centroids;
	}
	
	public void setCentroids(Map<Integer, Double []> centroids) {
		this.centroids = centroids;
	}
	
	public Map<String, Row> getAssignments() {
		return this.dataset;
	}
	
	public static class NotYetRunException extends RuntimeException {}
	
	public static class Row implements Cloneable {
		
		private List<Number> row;
		private Integer assignment;
		
		public Row(List<Number> row, Integer assignment) {
			this.row = row;
			this.assignment = assignment;
		}
		
		public Row(List<Number> row) {
			this(row, -1);
		}
		
		public List<Number> getRow() {
			return row;
		}
		
		public Integer getAssignment() {
			return assignment;
		}
		
		public void setAssignment(Integer assignment) {
			this.assignment = assignment;
		}
		
		@Override
		public String toString() {
			return "K=" + assignment + ",SIZE=" + row.size();
		}

		@Override
		public Row clone() {
			// TODO Auto-generated method stub
			try {
				return (Row) super.clone();
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}
		
	}
	
}
