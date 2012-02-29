package phylonet.coalescent;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import phylonet.coalescent.MGDInference_DP.Vertex;
import phylonet.tree.model.TNode;
import phylonet.tree.model.Tree;
import phylonet.tree.model.sti.STITreeCluster;

public class DuplicationWeightCounter {
	

	HashMap<STBipartition, Integer> weights;
	
	String [] gtTaxa;
	String [] stTaxa;

	private List<Set<STBipartition>> geneTreeSTBBySize;
	private List<Set<STBipartition>> X;
	private HashMap<STITreeCluster, Set<STBipartition>> clusterToSTBs;

	private Map<STBipartition,Integer> STBCountInGeneTrees;

	private List<Set<STBipartition>> goodSTBs;

	private boolean rooted;
	
	private void addToClusters (Map<Integer, Set<Vertex>> clusters, Vertex v, int size) {
		if (!clusters.get(size).contains(v)){						
			v._min_cost = -1;
			clusters.get(size).add(v);
		}
	}
	
	int computeTreeSTBipartitions(List<Tree> trees, 
			Map<String, String> taxonMap, Map<Integer, Set<Vertex>> clusters) {

		int sigmaN = 0;
		int k = trees.size();
		String[] leaves = stTaxa;
		int n = leaves.length;
		geneTreeSTBBySize = new ArrayList<Set<STBipartition>>(leaves.length);
		X = new ArrayList<Set<STBipartition>>(leaves.length);
		goodSTBs = new ArrayList<Set<STBipartition>>(leaves.length);
		
		for (int i = 0; i <= leaves.length; i++) {
			geneTreeSTBBySize.add(new HashSet<STBipartition>());
			X.add(new HashSet<STBipartition>());
			goodSTBs.add(new HashSet<STBipartition>());
			clusters.put(i, new HashSet<Vertex>());
		}
		STBCountInGeneTrees = new HashMap<STBipartition, Integer>(k*n);
		clusterToSTBs = new HashMap<STITreeCluster, Set<STBipartition>>(k*n);
		
		STITreeCluster all = new STITreeCluster(stTaxa);
		String as[];
		int j = (as = stTaxa).length;
		for (int i = 0; i < j; i++) {
			String t = as[i];
			all.addLeaf(t);
		}
		Vertex v = new Vertex();
		v._cluster = all;
		v._el_num = -1; //me
		v._min_cost = -1;		
		addToClusters(clusters, v, leaves.length);
				
		for (Tree tr : trees) {
			sigmaN += tr.getLeafCount() - 1;
			Map<TNode,STITreeCluster> map = new HashMap<TNode, STITreeCluster>(n);
			for (Iterator<TNode> nodeIt = tr.postTraverse()
					.iterator(); nodeIt.hasNext();) {
				TNode node = nodeIt.next();		        
	            if(node.isLeaf())
	            {
	                String nodeName = node.getName();
	                if (taxonMap != null) {
	                	nodeName = taxonMap.get(nodeName);
	                }

	                STITreeCluster tb = new STITreeCluster(leaves);
	                tb.addLeaf(nodeName);	                	                	        			
	        			
        			v = new Vertex();
        			v._cluster = tb;
        			v._el_num = -1; //me
        			v._min_cost = -1;
        			addToClusters(clusters, v, 1);

        			map.put(node, tb);
	                
	                if (!rooted) {
		                addSTB(clusters, tb, tb.complementaryCluster(), all);
	                }
	            } else {
	                int childCount = node.getChildCount();
	                STITreeCluster childbslist[] = new STITreeCluster[childCount];
			        BitSet bs = new BitSet(leaves.length);
	                int index = 0;
	                for(Iterator iterator3 = node.getChildren().iterator(); iterator3.hasNext();)
	                {
	                    TNode child = (TNode)iterator3.next();
	                    childbslist[index++] = map.get(child);
	                    bs.or(map.get(child).getCluster());
	                }
	                	                		                
	                STITreeCluster cluster = new STITreeCluster(leaves);
	                cluster.setCluster((BitSet) bs.clone());	
	                
	                int size = cluster.getClusterSize();
	                
        			v = new Vertex();
        			v._cluster = cluster;
        			v._el_num = -1; //me
        			v._min_cost = -1;
        			addToClusters(clusters, v, size);
	                map.put(node, cluster);	                
	                
	                
	                if (rooted) {

	                	if (index > 2) {	                	
		                	throw new RuntimeException("None bifurcating tree: "+
		                			tr+ "\n" + node);
		                }

		                STITreeCluster l_cluster = childbslist[0];
		                
		                STITreeCluster r_cluster = childbslist[1];
		                
		                addSTB(clusters, l_cluster, r_cluster, cluster);
	                } else {		                	
	                	if (childCount == 2) {
			                STITreeCluster l_cluster = childbslist[0];
			                
			                STITreeCluster r_cluster = childbslist[1];
			                
			                STITreeCluster allMinuslAndr_cluster = cluster.complementaryCluster();
			                		                
			                STITreeCluster lAndr_cluster = cluster;
			                
			                // add Vertex STBs
			                addSTB(clusters, l_cluster, r_cluster, cluster);
			                addSTB(clusters, r_cluster, allMinuslAndr_cluster, l_cluster.complementaryCluster());
			                addSTB(clusters, l_cluster, allMinuslAndr_cluster, r_cluster.complementaryCluster());
			                
			                // Add the Edge STB
			                addSTB(clusters, lAndr_cluster, allMinuslAndr_cluster,  all);

	                	} else if (childCount == 3 && node.isRoot()) {
			                STITreeCluster l_cluster = childbslist[0];
			                
			                STITreeCluster m_cluster =  childbslist[1];
			                
			                STITreeCluster r_cluster = childbslist[2];

			                addSTB(clusters, l_cluster, r_cluster, m_cluster.complementaryCluster());
			                addSTB(clusters, r_cluster, m_cluster, l_cluster.complementaryCluster());
			                addSTB(clusters, l_cluster, m_cluster, r_cluster.complementaryCluster());
	                	}
	                }
	            }	           
			}

		}
		System.out.println("STBs in gene trees: " + STBCountInGeneTrees.keySet().size());
		
		int s = 0;
		for (Integer c: clusters.keySet()){
			s += clusters.get(c).size();
		}
		System.out.println("Number of gene tree Clusters: " +s);
		
		weights = new HashMap<STBipartition, Integer>(STBCountInGeneTrees.size());
		
		return sigmaN;
	}

	private void addSTB(Map<Integer, Set<Vertex>> clusters,
			STITreeCluster l_cluster,
			STITreeCluster r_cluster, STITreeCluster cluster) {
		
		//System.err.println("trying: " + l_cluster + " | " + r_cluster);
		
		if (l_cluster.isDisjoint(r_cluster)) {
			
			STBipartition stb = new STBipartition(l_cluster, r_cluster, cluster);		
			geneTreeSTBBySize.get(stb.c.getClusterSize()).add(stb);
			addSTBToX(clusters, stb);			
			STBCountInGeneTrees.put(stb, 
					STBCountInGeneTrees.containsKey(stb)? 
							STBCountInGeneTrees.get(stb)+1: 1);
		} else {
			// This case could happen for multiple-copy
			BitSet and  = (BitSet) l_cluster.getCluster().clone();
			and.and(r_cluster.getCluster());
			
			BitSet l_Minus_r  = (BitSet) and.clone();
			l_Minus_r.xor(l_cluster.getCluster());
			STITreeCluster lmr = new STITreeCluster(stTaxa);
			lmr.setCluster(l_Minus_r);

			BitSet r_Minus_l  = (BitSet) and.clone();
			r_Minus_l.xor(r_cluster.getCluster());
			STITreeCluster rml = new STITreeCluster(stTaxa);
			rml.setCluster(r_Minus_l);	                	
			
			if (!rml.getCluster().isEmpty()) {
				addSTBToX(clusters, new STBipartition(l_cluster, rml, cluster));
			}
			if (!lmr.getCluster().isEmpty()) {
				addSTBToX(clusters, new STBipartition(lmr, r_cluster, cluster));
			}
		}
	}
	
	void addExtraBipartitionsByHeuristics(Map<Integer, Set<Vertex>> clusters) {
		HashSet<STBipartition> bipToAddToX = new HashSet<STBipartition>();
		//goodSTBs = X;
		//if (true) return;
		int added = 0;
		for (int i=1; i<goodSTBs.size(); i++)
		{			
			Set<STBipartition> curr_set = goodSTBs.get(i);
			for (STBipartition stb1:curr_set)
			{
				//if (Math.random() < 0.70) continue;
				for (int j=i; j<goodSTBs.size(); j++)
				{
					Set<STBipartition> other_set = goodSTBs.get(j);
					//if (Math.random() < 0.70) continue;
					for (STBipartition stb2:other_set) {
						//System.out.println(stb1 +" **AND** " + stb2);
						if (stb1.cluster1.getClusterSize() < 3 ||
								stb1.cluster2.getClusterSize() < 3 ||
								stb2.cluster1.getClusterSize() < 3 ||
								stb2.cluster2.getClusterSize() < 3) {							
							if (tryToAdd(stb1,stb2,bipToAddToX) != null) added++;						
						}
					}
					System.err.println(bipToAddToX.size() + " " + i);
				}
			}
		}
				
		for (STBipartition stb: bipToAddToX)
		{
			//System.err.println( "Adding: " + stb);
			addSTBToX(clusters, stb);
		}
		System.out.println("\n\nAdded " + added+ " bipartitions:\n");

		int s = 0;
		for (Integer c: clusters.keySet()){
			s += clusters.get(c).size();
		}
		System.out.println("Number of Clusters After Addition: " +s);

	}

	void addExtraBipartitionsByInput(Map<Integer, Set<Vertex>> clusters,
			List<Tree> extraTrees) {
		
		if (extraTrees == null) {
			return;
		}
		
		Vertex v;
		for (Tree tr : extraTrees) {
			Map<TNode,STITreeCluster> map = new HashMap<TNode, STITreeCluster>();
			for (Iterator<TNode> nodeIt = tr.postTraverse()
					.iterator(); nodeIt.hasNext();) {
				TNode node = nodeIt.next();		        
	            if(node.isLeaf())
	            {
	                String nodeName = node.getName();

	                STITreeCluster tb = new STITreeCluster(stTaxa);
	                tb.addLeaf(nodeName);	                	                	        			
	        			
        			map.put(node, tb);
	                
	            } else {
	                int childCount = node.getChildCount();
	                STITreeCluster childbslist[] = new STITreeCluster[childCount];
			        BitSet bs = new BitSet(stTaxa.length);
	                int index = 0;
	                for(Iterator iterator3 = node.getChildren().iterator(); iterator3.hasNext();)
	                {
	                    TNode child = (TNode)iterator3.next();
	                    childbslist[index++] = map.get(child);
	                    bs.or(map.get(child).getCluster());
	                }
	                	                		                
	                STITreeCluster cluster = new STITreeCluster(stTaxa);
	                cluster.setCluster((BitSet) bs.clone());	
	                
	                int size = cluster.getClusterSize();
	                
        			v = new Vertex();
        			v._cluster = cluster;
        			v._el_num = -1; //me
        			v._min_cost = -1;
        			addToClusters(clusters, v, size);
	                map.put(node, cluster);	                
	                
	                
                	if (index > 2) {	                	
	                	throw new RuntimeException("None bifurcating tree: "+
	                			tr+ "\n" + node);
	                }

	                STITreeCluster l_cluster = childbslist[0];
	                
	                STITreeCluster r_cluster = childbslist[1];
	                
	                STBipartition stb = new STBipartition(l_cluster, r_cluster, cluster);
	                
	    			addSTBToX(clusters, stb);
	              
	            }	           
			}

		}

	}

	private STBipartition tryToAdd(STBipartition stb1, STBipartition stb2, Set<STBipartition> bipToAddToX)
	{
		if (stb1.equals(stb2)) return null;
		if ( stb1.isDominatedBy(stb2) || stb2.isDominatedBy(stb1) )
			return null;

		if ( stb1.c.isDisjoint(stb2.c) ) return null;

		if ( stb1.cluster1.isDisjoint(stb2.cluster2) && 
				stb1.cluster2.isDisjoint(stb2.cluster1))
		{			
			STITreeCluster cl1 = new STITreeCluster(stb1.cluster1);
			cl1 = cl1.merge(stb2.cluster1);
			STITreeCluster cl2 = new STITreeCluster(stb1.cluster2);
			cl2 = cl2.merge(stb2.cluster2);
			STITreeCluster cl = new STITreeCluster(stb1.c);
			cl = cl.merge(stb2.c);
			STBipartition r = new STBipartition(cl1,cl2,cl);
			bipToAddToX.add(r);
			return r;
		}
		else if ( stb1.cluster1.isDisjoint(stb2.cluster1) && 
				stb1.cluster2.isDisjoint(stb2.cluster2) )
		{
			STITreeCluster cl1 = new STITreeCluster(stb1.cluster1);
			cl1 = cl1.merge(stb2.cluster2);
			STITreeCluster cl2 = new STITreeCluster(stb1.cluster2);
			cl2 = cl2.merge(stb2.cluster1);
			STITreeCluster cl = new STITreeCluster(stb1.c);
			cl = cl.merge(stb2.c);
			STBipartition r = new STBipartition(cl1,cl2,cl);
			bipToAddToX.add(r);
			return r;
		}
		return null;
	}

	private void addSTBToX(Map<Integer, Set<Vertex>> clusters, STBipartition stb) {
		int size = stb.c.getClusterSize();
		if (!X.get(size).contains(stb)){
			X.get(size).add(stb);
			STBCountInGeneTrees.put(stb, 0);
			STITreeCluster c = stb.c;
			Vertex nv = new Vertex();
			nv._cluster = c;
			addToClusters(clusters, nv, size);
			Set<STBipartition> stbs = clusterToSTBs.get(c);
			stbs = (stbs== null)? new HashSet<STBipartition>() : stbs;
			stbs.add(stb);
			clusterToSTBs.put(c, stbs);
		}
	}

	void calculateWeights(String[] leaves) {		
		/*weights.putAll(STBCountInGeneTrees);						
		
		for (int i = leaves.length; i > 1; i--) {
			Set<STBipartition> biggerSTBs = X.get(i);			
			for (STBipartition biggerSTB : biggerSTBs) {
				int weight = weights.get(biggerSTB);
				for (int j = i - 1; j > 1; j--) {
					Set<STBipartition> smallerSTBs = geneTreeSTBBySize.get(j);
					for (STBipartition smallerSTB : smallerSTBs) {
						if (smallerSTB.isDominatedBy(biggerSTB)) {
							weight+= STBCountInGeneTrees.get(smallerSTB);
						}
					}
				}
				weights.put(biggerSTB, weight);
			}
		}*/
	}

	int calculateWeight(STBipartition biggerSTB) {

		Integer count = STBCountInGeneTrees.get(biggerSTB);
		int weight = count;
		for (int j = biggerSTB.c.getClusterSize() - 1; j > 1; j--) {
			Set<STBipartition> smallerSTBs = geneTreeSTBBySize.get(j);
			for (STBipartition smallerSTB : smallerSTBs) {
				if (smallerSTB.isDominatedBy(biggerSTB)) {
					weight+= STBCountInGeneTrees.get(smallerSTB);
				}
			}
		}		
		if (!rooted && biggerSTB.c.getClusterSize() == stTaxa.length) {
			weight -= 2 * count;
			if (biggerSTB.cluster1.getClusterSize() != 1 &&
					biggerSTB.cluster2.getClusterSize() != 1) {
				weight -= 2 * count;				
			}
		}
		return weight;
	}
	
	/*public void calculateWeights(String[] leaves, List<Tree> trees) {
	
		weights = new HashMap<STBipartition, Integer>();
		weights.putAll(STBCountInGeneTrees);						
		
		for (int i =0 ; i < leaves.length; i++) {
			Set<STBipartition> gtSTBs = geneTreeSTBBySize.get(i);
			for (STBipartition gtSTB : gtSTBs) {
				STITreeCluster c = new STITreeCluster(gtSTB.cluster1);
				c.merge(gtSTB.cluster2);
				Set<STBipartition> stSTBs = clusterToSTBs.get(c);
			}
		}
		
		for (int i = leaves.length; i > 1; i--) {
			Set<STBipartition> biggerSTBs = X.get(i);
			for (STBipartition biggerSTB : biggerSTBs) {
				for (int j = i - 1; j > 1; j--) {
					Set<STBipartition> smallerSTBs = geneTreeSTBBySize.get(j);
					for (STBipartition smallerSTB : smallerSTBs) {
						if (smallerSTB.isDominatedBy(biggerSTB)) {
							weights.put(biggerSTB, weights.get(biggerSTB)+weights.get(smallerSTB));
						}
					}
				}
			}
		}
	}
*/
	public DuplicationWeightCounter(String[] stTaxa, boolean rooted){
		this.stTaxa = stTaxa;
		this.rooted = rooted;
	}

	public DuplicationWeightCounter(String[] gtTaxa, String[] stTaxa, boolean rooted2){
		this.gtTaxa = gtTaxa;		
		this.stTaxa = stTaxa;
		this.rooted = rooted2;
	}
	
	public int getBiPartitionDPWeight(STITreeCluster cluster1, STITreeCluster cluster2, STITreeCluster cluster) {		
		STBipartition bi = new STBipartition(cluster1,cluster2, cluster);
		if (!weights.containsKey(bi)){
			weights.put(bi,calculateWeight(bi));
		}
		return weights.get(bi);
	}

	public void addGoodSTB (STBipartition good, int size) {		
		goodSTBs.get(size).add(good);
	}
	
	public Set<STBipartition> getClusterBiPartitions(STITreeCluster cluster) {
						
		return clusterToSTBs.get(cluster);
	}
	static class STBipartition {
		
		STITreeCluster cluster1;
		STITreeCluster cluster2;		
		private STITreeCluster c;
		
		public STBipartition(STITreeCluster c1, STITreeCluster c2, STITreeCluster cluster) {
			cluster1 = c1;
			cluster2 = c2;
			c = cluster;
		}
		@Override
		public boolean equals(Object obj) {
			STBipartition stb2 = (STBipartition) obj; 
			
			return
					((stb2.cluster1.equals(this.cluster1) && stb2.cluster2.equals(this.cluster2)) ||
					stb2.cluster2.equals(this.cluster1) && stb2.cluster1.equals(this.cluster2));					
		}
		@Override
		public int hashCode() {
			return cluster1.hashCode() + cluster2.hashCode();
		}
		@Override
		public String toString() {		
			return cluster1.toString()+"|"+cluster2.toString();
		}
		
		public boolean isDominatedBy(STBipartition dominant) {
			if (! dominant.c.containsCluster(this.c)) {
				return false;
			} 
			return (dominant.cluster1.containsCluster(this.cluster1) && dominant.cluster2.containsCluster(this.cluster2)) ||
					(dominant.cluster2.containsCluster(this.cluster1) && dominant.cluster1.containsCluster(this.cluster2));
		}
		
	}
	
}