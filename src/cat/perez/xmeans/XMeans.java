package cat.perez.xmeans;

import static java.lang.Math.PI;
import static java.lang.Math.log10;
import static java.util.stream.Collectors.toMap;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;

import org.knime.core.node.NodeLogger;

import cat.perez.kmeans.KMeans;
import cat.perez.kmeans.KMeans.Row;

public class XMeans {
	
	private static final NodeLogger log10ger = NodeLogger.getLogger(XMeans.class);
	
	private Map<String, Row> dataset;
	private int lowerK;
	private int upperK;
	private int D;
	
	private Map<Integer, Double []> centroids = new HashMap<>();
	
	private List<Result> results = new ArrayList<>();
	
	public XMeans(Map<String, Row> dataset, int lowerK, int upperK) {
		this.dataset = dataset;
		this.lowerK = lowerK;
		this.upperK = upperK;
		this.D = dataset.values().iterator().next().getRow().size();
	}
	
	public static XMeans fromNumberList(Map<String, List<Number>> dataset, int lowerK, int upperK) {
		return new XMeans(dataset.entrySet().stream().map(e -> new SimpleEntry<>(e.getKey(), new Row(e.getValue()))).collect(toMap(Entry::getKey, Entry::getValue)), lowerK, upperK);
	}
	
	public void run() {
		int k = this.lowerK;
		boolean kChanged;
		initialize();
		do {
			improveParams(k);
			int newK = improveStructure(k);
			kChanged = newK != k;
			k = newK;
		} while (k <= this.upperK && kChanged);
	}
	
	private void initialize() {
		Random random = new Random();
		for (int i = 0; i < this.lowerK; i++) {
			Double [] centroid = new Double [this.D];
			Row row = new ArrayList<>(dataset.values()).get(random.nextInt(dataset.size()));
			for (int j = 0; j < this.D; j++) centroid[j] = row.getRow().get(j).doubleValue();
			centroids.put(i, centroid);
		}
	}
	
	private void improveParams(int k) {
		System.out.println("Improving Parameters (k=" + k + ")");
		KMeans kmeans = new KMeans(this.dataset, k, 100000);
		kmeans.setCentroids(this.centroids);
		kmeans.run();
		this.dataset = kmeans.getAssignments();
		this.centroids = kmeans.getCentroids();
	}
	
	private int improveStructure(int k) {
		System.out.println("Improving structure (k=" + k + ")");
		Random random = new Random();
		int newK = k;
		List<Map<String, Row>> subsets = new ArrayList<>();
		for (int i = 0; i < k; i++) subsets.add(this.subsetK(this.dataset, i));
		for (int i = 0; i < subsets.size(); i++) {
			System.out.println("\tIteration " + i);
			Map<String, Row> subset = subsets.get(i);
			Double [] centroid = centroids.get(i);
			Double [] newCentroid0 = dataset.values().toArray(new Row [0])[random.nextInt(dataset.size())].getRow().toArray(new Double [0]);
			Double [] newCentroid1 = new Double [newCentroid0.length];
			for (int j = 0; j < newCentroid0.length; j++) newCentroid1[j] = -(newCentroid0[j] - centroid[j]) + centroid[j];
			Map<Integer, Double []> newCentroids = new HashMap<>();
			newCentroids.put(0, newCentroid0);
			newCentroids.put(1, newCentroid1);
			KMeans kmeans = new KMeans(subset.entrySet().stream().map(e -> new SimpleEntry<>(e.getKey(), ((Row) e.getValue()).clone())).collect(Collectors.toMap(Entry::getKey, Entry::getValue)), 2, 100000);
			kmeans.setCentroids(newCentroids);
			kmeans.run();
			double originalClusterScore = this.BIC(subset.entrySet().stream().map(e -> new SimpleEntry<>(e.getKey(), new Row(e.getValue().getRow(), 0))).collect(Collectors.toMap(Entry::getKey, Entry::getValue)), 1);
			double newClusterScore = this.BIC(kmeans.getAssignments(), 2);
			if (newClusterScore > originalClusterScore) {
				System.out.println("New cluster found. New amount of clusters: " + (newK + 1));
				final int index = i;
				final int finalNewK = newK;
				kmeans.getAssignments().forEach((key, row) -> dataset.put(key, new Row(row.getRow(), row.getAssignment() == 0 ? index : finalNewK)));
				System.out.println("Adding centroid 0 in position " + i + " and centroid 1 in position " + (k + index));
				centroids.put(index, kmeans.getCentroids().get(0));
				centroids.put(newK, kmeans.getCentroids().get(1));
				newK++;
			}
		}
		results.add(new Result(this.BIC(dataset, newK), new HashMap<String, Row>(this.dataset), new HashMap<Integer, Double []>(this.centroids)));
		return newK;
	}
	
	private Map<String, Row> subsetK(Map<String, Row> set, final int k) {
		return set.entrySet().stream().filter(e -> e.getValue().getAssignment() == k).collect(toMap(Entry::getKey, Entry::getValue));
	}
	
	private double BIC(Map<String, Row> set, int K) {
		int N = set.size();
		int q = this.D * (K + 1);
		double bic = - (q / 2) * log10(N);
		for (int k = 0; k < K; k++) {
			Map<String, Row> subset = this.subsetK(set, k);
			int Rk = subset.size();
			bic += -(Rk / 2) * log10(2 * PI) - (Rk * this.D / 2) * log10(estimateStdDev(subset)) - ((Rk - K) / 2) + Rk * log10(Rk) - Rk * log10(N);
		}
		return bic;
	}
	
	private double estimateStdDev(Map<String, Row> subset) {
		int N = subset.size();
		double [] avgs = new double [this.D];
		for (int i = 0; i < D; i++) {
			final int index = i;
			avgs[i] = subset.entrySet().stream().mapToDouble(e -> e.getValue().getRow().get(index).doubleValue()).sum() / N;
		}
		Iterator<Entry<String, Row>> it = subset.entrySet().iterator();
		double sum = 0;
		while (it.hasNext()) {
			double distance = 0;
			Row row = it.next().getValue();
			for (int j = 0; j < D; j++) {
				distance += Math.pow(row.getRow().get(j).doubleValue() - avgs[j], 2);
			}
			sum += Math.sqrt(distance);
		}
		return (sum / (N - D));
	}
	
	public Result getBestAssignments() {
		return results.stream().max((r1, r2) -> Double.compare(r1.getScore(), r2.getScore())).get();
	}
	
	public static class Result {
		
		private double score;
		private Map<String, Row> rows;
		private Map<Integer, Double []> centroids;
		
		public Result(double score, Map<String, Row> rows, Map<Integer, Double []> centroids) {
			this.score = score;
			this.rows = rows;
			this.centroids = centroids;
		}

		public double getScore() {
			return this.score;
		}

		public Map<String, Row> getRows() {
			return this.rows;
		}

		public Map<Integer, Double[]> getCentroids() {
			return this.centroids;
		}
		
	}

}
