import java.io.*;
import java.util.*;

public class DecisionTree {
    
    // Different ways to select attributes
    enum Criterion {
        IG, IGR, NWIG
    }
    
    // Tree node structure
    static class TreeNode {
        String attribute;
        String value;
        String classification;
        HashMap<String, TreeNode> children;
        boolean isLeaf;
        int depth;


        // For numeric splits
        boolean isNumericSplit = false;
        double threshold = Double.NaN;

        TreeNode() {
            children = new HashMap<>();
            isLeaf = false;
            depth = 0;
        }

        TreeNode(String classification) {
            this.classification = classification;
            this.isLeaf = true;
            this.children = new HashMap<>();
            this.depth = 0;
        }
    }
    
    // To store results
    static class Result {
        String dataset;
        String criterion;
        int maxDepth;
        double accuracy;
        int nodes;
        int treeDepth;
        
        Result(String dataset, String criterion, int maxDepth, double accuracy, int nodes, int treeDepth) {
            this.dataset = dataset;
            this.criterion = criterion;
            this.maxDepth = maxDepth;
            this.accuracy = accuracy;
            this.nodes = nodes;
            this.treeDepth = treeDepth;
        }
    }
    
    // Dataset class
    static class Dataset {
        ArrayList<String[]> examples;
        ArrayList<String> attributes;
        String targetAttribute;
        int targetIndex;
        
        Dataset(ArrayList<String[]> examples, ArrayList<String> attributes, String targetAttribute) {
            this.examples = examples;
            this.attributes = attributes;
            this.targetAttribute = targetAttribute;
            this.targetIndex = attributes.indexOf(targetAttribute);
        }
        
        // Get subset of data for a particular attribute value
        Dataset subset(String attribute, String value) {
            int attributeIndex = attributes.indexOf(attribute);
            ArrayList<String[]> subsetExamples = new ArrayList<>();
            
            for (String[] example : examples) {
                if (example[attributeIndex].equals(value)) {
                    subsetExamples.add(example);
                }
            }
            
            return new Dataset(subsetExamples, attributes, targetAttribute);
        }

        Dataset subsetNumeric(String attribute, double threshold, boolean lessOrEqual) {
            int attributeIndex = attributes.indexOf(attribute);
            ArrayList<String[]> subsetExamples = new ArrayList<>();
            for (String[] example : examples) {
                try {
                    double val = Double.parseDouble(example[attributeIndex]);
                    if (lessOrEqual && val <= threshold) subsetExamples.add(example);
                    if (!lessOrEqual && val > threshold) subsetExamples.add(example);
                } catch (NumberFormatException e) {
                    // skip non-numeric
                }
            }
            return new Dataset(subsetExamples, attributes, targetAttribute);
        }
        
        // Get all possible values for an attribute
        HashSet<String> getValues(String attribute) {
            int attributeIndex = attributes.indexOf(attribute);
            HashSet<String> values = new HashSet<>();
            
            for (String[] example : examples) {
                values.add(example[attributeIndex]);
            }
            
            return values;
        }
        
        // Get most common class
        String getMostCommonClass() {
            HashMap<String, Integer> counts = new HashMap<>();
            
            for (String[] example : examples) {
                String targetValue = example[targetIndex];
                counts.put(targetValue, counts.getOrDefault(targetValue, 0) + 1);
            }
            
            String mostCommon = null;
            int maxCount = 0;
            for (String key : counts.keySet()) {
                if (counts.get(key) > maxCount) {
                    maxCount = counts.get(key);
                    mostCommon = key;
                }
            }
            
            return mostCommon;
        }
        
        // Check if all examples have same class
        boolean isPure() {
            if (examples.isEmpty()) return true;
            
            String firstClass = examples.get(0)[targetIndex];
            for (String[] example : examples) {
                if (!example[targetIndex].equals(firstClass)) {
                    return false;
                }
            }
            return true;
        }
    }
    
    private Criterion criterion;
    private int maxDepth;
    private TreeNode root;
    
    public DecisionTree(Criterion criterion, int maxDepth) {
        this.criterion = criterion;
        this.maxDepth = maxDepth;
    }
    
    // Calculate entropy
    private double entropy(Dataset dataset) {
        if (dataset.examples.isEmpty()) return 0.0;
        
        HashMap<String, Integer> counts = new HashMap<>();
        for (String[] example : dataset.examples) {
            String targetValue = example[dataset.targetIndex];
            counts.put(targetValue, counts.getOrDefault(targetValue, 0) + 1);
        }
        
        double entropy = 0.0;
        int total = dataset.examples.size();
        
        for (int count : counts.values()) {
            if (count > 0) {
                double p = (double) count / total;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        
        return entropy;
    }
    
    // Calculate Information Gain
    private double informationGain(Dataset dataset, String attribute) {
        double baseEntropy = entropy(dataset);
        HashSet<String> values = dataset.getValues(attribute);
        
        double weightedEntropy = 0.0;
        for (String value : values) {
            Dataset subset = dataset.subset(attribute, value);
            double weight = (double) subset.examples.size() / dataset.examples.size();
            weightedEntropy += weight * entropy(subset);
        }
        
        return baseEntropy - weightedEntropy;
    }
    
    // Calculate Intrinsic Value
    private double intrinsicValue(Dataset dataset, String attribute) {
        HashSet<String> values = dataset.getValues(attribute);
        double iv = 0.0;
        
        for (String value : values) {
            Dataset subset = dataset.subset(attribute, value);
            if (subset.examples.size() > 0) {
                double ratio = (double) subset.examples.size() / dataset.examples.size();
                iv -= ratio * (Math.log(ratio) / Math.log(2));
            }
        }
        
        return iv;
    }
    
    // Calculate Information Gain Ratio
    private double informationGainRatio(Dataset dataset, String attribute) {
        double ig = informationGain(dataset, attribute);
        double iv = intrinsicValue(dataset, attribute);
        
        if (iv == 0) return 0;
        return ig / iv;
    }
    
    // Calculate NWIG
    private double nwig(Dataset dataset, String attribute) {
        double ig = informationGain(dataset, attribute);
        int k = dataset.getValues(attribute).size();
        int n = dataset.examples.size();
        
        double penalty = Math.log(k + 1) / Math.log(2);
        double adjustment = 1.0 - (double)(k - 1) / n;
        
        return (ig / penalty) * adjustment;
    }


    private double[] bestNumericSplit(Dataset dataset, String attribute) {
        int idx = dataset.attributes.indexOf(attribute);
        // Collect all values and class labels
        ArrayList<double[]> valueLabelPairs = new ArrayList<>();
        for (String[] example : dataset.examples) {
            try {
                double val = Double.parseDouble(example[idx]);
                String label = example[dataset.targetIndex];
                valueLabelPairs.add(new double[]{val, label.hashCode()});
            } catch (NumberFormatException e) {
                // skip
            }
        }
        // Sort by value
        valueLabelPairs.sort(Comparator.comparingDouble(a -> a[0]));
        double bestThreshold = Double.NaN;
        double bestGain = -1;
        for (int i = 1; i < valueLabelPairs.size(); i++) {
            double v1 = valueLabelPairs.get(i - 1)[0];
            double v2 = valueLabelPairs.get(i)[0];
            if (v1 == v2) continue;
            double threshold = (v1 + v2) / 2.0;
            Dataset left = dataset.subsetNumeric(attribute, threshold, true);
            Dataset right = dataset.subsetNumeric(attribute, threshold, false);
            double weightedEntropy = ((double)left.examples.size() / dataset.examples.size()) * entropy(left)
                                + ((double)right.examples.size() / dataset.examples.size()) * entropy(right);
            double gain = entropy(dataset) - weightedEntropy;
            if (gain > bestGain) {
                bestGain = gain;
                bestThreshold = threshold;
            }
        }
        return new double[]{bestThreshold, bestGain};
    }
    
    // Select best attribute
    private String selectBestAttribute(Dataset dataset, ArrayList<String> attributes) {
        String bestAttribute = null;
        double bestScore = -1;
        double bestThreshold = Double.NaN;
        boolean bestIsNumeric = false;

        for (String attribute : attributes) {
            if (attribute.equals(dataset.targetAttribute)) continue;

            double score = 0.0;
            boolean isNumeric = isNumericAttribute(dataset, attribute);
            double threshold = Double.NaN;

            if (isNumeric) {
                double[] split = bestNumericSplit(dataset, attribute);
                threshold = split[0];
                score = split[1];
            } else {
                if (criterion == Criterion.IG) {
                    score = informationGain(dataset, attribute);
                } else if (criterion == Criterion.IGR) {
                    score = informationGainRatio(dataset, attribute);
                } else if (criterion == Criterion.NWIG) {
                    score = nwig(dataset, attribute);
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestAttribute = attribute;
                bestThreshold = threshold;
                bestIsNumeric = isNumeric;
            }
        }

        // Store the best threshold and numeric flag for use in buildTree
        this.lastBestThreshold = bestThreshold;
        this.lastBestIsNumeric = bestIsNumeric;
        return bestAttribute;
    }

    private double lastBestThreshold = Double.NaN;
    private boolean lastBestIsNumeric = false;


    private boolean isNumericAttribute(Dataset dataset, String attribute) {
        int idx = dataset.attributes.indexOf(attribute);
        for (String[] example : dataset.examples) {
            try {
                Double.parseDouble(example[idx]);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }
    
    // Build the tree
    private TreeNode buildTree(Dataset dataset, ArrayList<String> attributes, int currentDepth) {
        // Base cases
        if (dataset.examples.isEmpty()) {
            return new TreeNode("Unknown");
        }
        
        if (dataset.isPure()) {
            TreeNode leaf = new TreeNode(dataset.examples.get(0)[dataset.targetIndex]);
            leaf.depth = currentDepth;
            return leaf;
        }
        
        if (attributes.isEmpty() || (maxDepth > 0 && currentDepth >= maxDepth)) {
            TreeNode leaf = new TreeNode(dataset.getMostCommonClass());
            leaf.depth = currentDepth;
            return leaf;
        }
        
        String bestAttribute = selectBestAttribute(dataset, attributes);
        if (bestAttribute == null) {
            TreeNode leaf = new TreeNode(dataset.getMostCommonClass());
            leaf.depth = currentDepth;
            return leaf;
        }
        
        TreeNode node = new TreeNode();
        node.attribute = bestAttribute;
        node.depth = currentDepth;

        ArrayList<String> newAttributes = new ArrayList<>(attributes);
        newAttributes.remove(bestAttribute);

        if (lastBestIsNumeric) {
            node.isNumericSplit = true;
            node.threshold = lastBestThreshold;
            Dataset left = dataset.subsetNumeric(bestAttribute, node.threshold, true);
            Dataset right = dataset.subsetNumeric(bestAttribute, node.threshold, false);
            node.children.put("le", buildTree(left, newAttributes, currentDepth + 1));
            node.children.put("gt", buildTree(right, newAttributes, currentDepth + 1));
        } else {
            HashSet<String> values = dataset.getValues(bestAttribute);
            for (String value : values) {
                Dataset subset = dataset.subset(bestAttribute, value);
                TreeNode childNode = buildTree(subset, newAttributes, currentDepth + 1);
                node.children.put(value, childNode);
            }
        }

        return node;
    }
    
    // Train the tree
    public void train(Dataset dataset) {
        ArrayList<String> attributes = new ArrayList<>(dataset.attributes);
        root = buildTree(dataset, attributes, 0);
    }


//======================================Tree Building Ends=========================================
    
    // Classify an example
    public String classify(String[] example, ArrayList<String> attributes) {
        TreeNode current = root;

        while (!current.isLeaf) {
            String attribute = current.attribute;
            int index = attributes.indexOf(attribute);
            String value = example[index];

            if (current.isNumericSplit) {
                double v;
                try {
                    v = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return getMostCommonFromSubtree(current);
                }
                if (v <= current.threshold) {
                    current = current.children.get("le");
                } else {
                    current = current.children.get("gt");
                }
            } else {
                if (current.children.containsKey(value)) {
                    current = current.children.get(value);
                } else {
                    // Handle unknown value
                    return getMostCommonFromSubtree(current);
                }
            }
        }

        return current.classification;
    }
    
    // Get most common class from subtree
    private String getMostCommonFromSubtree(TreeNode node) {
        if (node.isLeaf) {
            return node.classification;
        }
        
        HashMap<String, Integer> counts = new HashMap<>();
        collectClasses(node, counts);
        
        String mostCommon = "Unknown";
        int maxCount = 0;
        for (String key : counts.keySet()) {
            if (counts.get(key) > maxCount) {
                maxCount = counts.get(key);
                mostCommon = key;
            }
        }
        
        return mostCommon;
    }
    

//==========================Helper functions======================================
    // Helper to collect classes
    private void collectClasses(TreeNode node, HashMap<String, Integer> counts) {
        if (node.isLeaf) {
            counts.put(node.classification, counts.getOrDefault(node.classification, 0) + 1);
        } else {
            for (TreeNode child : node.children.values()) {
                collectClasses(child, counts);
            }
        }
    }
    
    // Count nodes
    public int countNodes() {
        return countNodes(root);
    }
    
    private int countNodes(TreeNode node) {
        if (node == null) return 0;
        
        int count = 1;
        for (TreeNode child : node.children.values()) {
            count += countNodes(child);
        }
        
        return count;
    }
    
    // Get tree depth
    public int getTreeDepth() {
        return getTreeDepth(root);
    }
    
    private int getTreeDepth(TreeNode node) {
        if (node == null) return 0;
        if (node.isLeaf) return node.depth;
        
        int maxDepth = node.depth;
        for (TreeNode child : node.children.values()) {
            maxDepth = Math.max(maxDepth, getTreeDepth(child));
        }
        
        return maxDepth;
    }

//===========================================Classification Ends=================================

    public static void debugDataset(Dataset dataset, String name) {
        System.out.println("\n=== DEBUG: " + name + " Dataset ===");
        System.out.println("Total examples: " + dataset.examples.size());
        System.out.println("Attributes: " + dataset.attributes);
        System.out.println("Target attribute: " + dataset.targetAttribute);
        System.out.println("Target index: " + dataset.targetIndex);
        
        System.out.println("\nFirst 5 examples:");
        for (int i = 0; i < Math.min(5, dataset.examples.size()); i++) {
            System.out.println("Example " + (i+1) + ": " + Arrays.toString(dataset.examples.get(i)));
        }
        
        HashMap<String, Integer> classCount = new HashMap<>();
        for (String[] example : dataset.examples) {
            String targetValue = example[dataset.targetIndex];
            classCount.put(targetValue, classCount.getOrDefault(targetValue, 0) + 1);
        }
        
        System.out.println("\nClass distribution:");
        for (String key : classCount.keySet()) {
            System.out.println("- " + key + ": " + classCount.get(key));
        }
        
        System.out.println("\nChecking for missing values...");
        for (int i = 0; i < dataset.attributes.size(); i++) {
            HashSet<String> values = new HashSet<>();
            for (String[] example : dataset.examples) {
                values.add(example[i]);
            }
            System.out.println("- " + dataset.attributes.get(i) + ": " + values.size() + " unique values");
            if (values.size() <= 10) {
                System.out.println("  Values: " + values);
            }
        }
    }
        
    // Load Iris dataset
    public static Dataset loadIris(String filename) throws IOException {
        ArrayList<String[]> examples = new ArrayList<>();
        ArrayList<String> attributes = null;
        
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line = br.readLine();
        if (line != null) {
            String[] parts = line.split(",");
            attributes = new ArrayList<>();
            for (String part : parts) {
                attributes.add(part.trim());
            }
            
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].trim();
                }
                examples.add(values);
            }
        }
        br.close();
        
        return new Dataset(examples, attributes, "Species");
    }
    
    // Load Adult dataset
    public static Dataset loadAdult(String filename) throws IOException {
        ArrayList<String[]> examples = new ArrayList<>();
        ArrayList<String> attributes = new ArrayList<>();
        attributes.add("age");
        attributes.add("workclass");
        attributes.add("fnlwgt");
        attributes.add("education");
        attributes.add("education-num");
        attributes.add("marital-status");
        attributes.add("occupation");
        attributes.add("relationship");
        attributes.add("race");
        attributes.add("sex");
        attributes.add("capital-gain");
        attributes.add("capital-loss");
        attributes.add("hours-per-week");
        attributes.add("native-country");
        attributes.add("income");
        
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String[] values = line.split(",");
            if (values.length != 15) continue;
            
            for (int i = 0; i < values.length; i++) {
                values[i] = values[i].trim();
            }
            
            // Skip rows with missing values
            boolean hasMissing = false;
            for (String value : values) {
                if (value.equals("?")) {
                    hasMissing = true;
                    break;
                }
            }
            
            if (!hasMissing) {
                examples.add(values);
            }
        }
        br.close();
        
        return new Dataset(examples, attributes, "income");
    }
    
    
    // Split dataset
    public static Dataset[] split(Dataset dataset, double trainRatio) {
        return split(dataset, trainRatio, new Random());
    }

    // Overloaded split method with Random parameter for reproducibility
    public static Dataset[] split(Dataset dataset, double trainRatio, Random random) {
        ArrayList<String[]> examples = new ArrayList<>(dataset.examples);
        Collections.shuffle(examples, random);
        int trainSize = (int) (examples.size() * trainRatio);
        ArrayList<String[]> trainExamples = new ArrayList<>(examples.subList(0, trainSize));
        // int testSize = trainSize + (int)((examples.size()- trainSize)*(1-trainRatio));
        ArrayList<String[]> testExamples = new ArrayList<>(examples.subList(trainSize, examples.size()));
        Dataset trainSet = new Dataset(trainExamples, dataset.attributes, dataset.targetAttribute);
        Dataset testSet = new Dataset(testExamples, dataset.attributes, dataset.targetAttribute);
        return new Dataset[]{trainSet, testSet};
    }
    

    // Calculate accuracy
    public double getAccuracy(Dataset testSet) {
        int correct = 0;
        int total = 0;
        
        for (String[] example : testSet.examples) {
            String predicted = classify(example, testSet.attributes);
            String actual = example[testSet.targetIndex];
            
            if (predicted.equals(actual)) {
                correct++;
            }
            total++;
        }
        
        return (double) correct / total;
    }
    
    // Run single experiment for comparison (nodes and depth analysis)
    public static void runSingleExperiment(Dataset dataset, String datasetName) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("SINGLE RUN COMPARISON - " + datasetName.toUpperCase());
        System.out.println("=".repeat(60));
        
        Criterion[] criteria = {Criterion.IG, Criterion.IGR, Criterion.NWIG};
        int[] depths = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        
        // Fixed seed for reproducible results in comparison
        Random random = new Random(42);
        Dataset[] split = split(dataset, 0.8, random);
        Dataset trainSet = split[0];
        Dataset testSet = split[1];
        
        System.out.println("Dataset: " + datasetName);
        System.out.println("Training examples: " + trainSet.examples.size());
        System.out.println("Test examples: " + testSet.examples.size());
        System.out.println();
        
        // Header
        System.out.printf("%-10s %-8s %-10s %-8s %-10s\n", "Criterion", "MaxDepth", "Accuracy", "Nodes", "TreeDepth");
        System.out.println("-".repeat(50));
        
        // Store results for analysis
        HashMap<String, Result> noPruningResults = new HashMap<>();
        
        for (Criterion crit : criteria) {
            for (int depth : depths) {
                DecisionTree tree = new DecisionTree(crit, depth);
                tree.train(trainSet);
                
                double accuracy = tree.getAccuracy(testSet);
                int nodes = tree.countNodes();
                int treeDepth = tree.getTreeDepth();
                
                System.out.printf("%-10s %-8d %-10.3f %-8d %-10d\n", 
                                crit.toString(), depth, accuracy, nodes, treeDepth);
                
                // Store no pruning results (depth = 0)
                if (depth == 0) {
                    noPruningResults.put(crit.toString(), 
                        new Result(datasetName, crit.toString(), depth, accuracy, nodes, treeDepth));
                }
            }
            System.out.println();
        }
        
        // Analysis of no pruning results
        System.out.println("\nANALYSIS - WITHOUT PRUNING (MaxDepth = 0):");
        System.out.println("-".repeat(50));
        System.out.printf("%-10s %-10s %-8s %-10s\n", "Criterion", "Accuracy", "Nodes", "TreeDepth");
        System.out.println("-".repeat(40));
        
        for (Criterion crit : criteria) {
            Result r = noPruningResults.get(crit.toString());
            System.out.printf("%-10s %-10.3f %-8d %-10d\n", 
                            r.criterion, r.accuracy, r.nodes, r.treeDepth);
        }
    }
    
    // Run full experiments with 20 repetitions
    public static void runFullExperiments(Dataset dataset, String datasetName) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("FULL EXPERIMENTS (20 RUNS) - " + datasetName.toUpperCase());
        System.out.println("=".repeat(60));
        
        Criterion[] criteria = {Criterion.IG, Criterion.IGR, Criterion.NWIG};
        int[] depths = { 5, 8, 9};
        int numRuns = 1;
        
        System.out.println("Running " + numRuns + " experiments for each criterion-depth combination...");
        System.out.println("This may take a while...\n");
        
        // Store results for averaging
        HashMap<String, HashMap<Integer, ArrayList<Double>>> accuracyResults = new HashMap<>();
        HashMap<String, HashMap<Integer, ArrayList<Integer>>> nodeResults = new HashMap<>();
        HashMap<String, HashMap<Integer, ArrayList<Integer>>> depthResults = new HashMap<>();
        
        for (Criterion crit : criteria) {
            accuracyResults.put(crit.toString(), new HashMap<>());
            nodeResults.put(crit.toString(), new HashMap<>());
            depthResults.put(crit.toString(), new HashMap<>());
            
            for (int depth : depths) {
                accuracyResults.get(crit.toString()).put(depth, new ArrayList<>());
                nodeResults.get(crit.toString()).put(depth, new ArrayList<>());
                depthResults.get(crit.toString()).put(depth, new ArrayList<>());
            }
        }
        
        // Run experiments
        int totalExperiments = criteria.length * depths.length * numRuns;
        int currentExp = 0;
        
        for (Criterion crit : criteria) {
            System.out.println("Running experiments for " + crit.toString() + "...");
            
            for (int depth : depths) {
                for (int run = 0; run < numRuns; run++) {
                    currentExp++;
                    if (currentExp % 50 == 0) {
                        System.out.printf("Progress: %d/%d experiments completed\n", currentExp, totalExperiments);
                    }
                    
                    Dataset[] split = split(dataset, 0.8);
                    Dataset trainSet = split[0];
                    Dataset testSet = split[1];
                    
                    DecisionTree tree = new DecisionTree(crit, depth);
                    tree.train(trainSet);
                    
                    double accuracy = tree.getAccuracy(testSet);
                    int nodes = tree.countNodes();
                    int treeDepth = tree.getTreeDepth();
                    
                    accuracyResults.get(crit.toString()).get(depth).add(accuracy);
                    nodeResults.get(crit.toString()).get(depth).add(nodes);
                    depthResults.get(crit.toString()).get(depth).add(treeDepth);
                }
            }
        }
        
        System.out.println("\nCompleted all experiments!\n");
        
        // Calculate and display averages
        System.out.println("AVERAGE ACCURACY RESULTS:");
        System.out.println("-".repeat(50));
        System.out.printf("%-8s %-10s %-10s %-10s\n", "Depth", "IG", "IGR", "NWIG");
        System.out.println("-".repeat(40));
        
        for (int depth : depths) {
            System.out.printf("%-8d ", depth);
            
            for (Criterion crit : criteria) {
                ArrayList<Double> accuracies = accuracyResults.get(crit.toString()).get(depth);
                double sum = 0;
                for (double acc : accuracies) {
                    sum += acc;
                }
                double avg = sum / accuracies.size();
                System.out.printf("%-10.3f ", avg);
            }
            System.out.println();
        }
        
        System.out.println("\nAVERAGE NUMBER OF NODES:");
        System.out.println("-".repeat(50));
        System.out.printf("%-8s %-10s %-10s %-10s\n", "Depth", "IG", "IGR", "NWIG");
        System.out.println("-".repeat(40));
        
        for (int depth : depths) {
            System.out.printf("%-8d ", depth);
            
            for (Criterion crit : criteria) {
                ArrayList<Integer> nodes = nodeResults.get(crit.toString()).get(depth);
                double sum = 0;
                for (int node : nodes) {
                    sum += node;
                }
                double avg = sum / nodes.size();
                System.out.printf("%-10.1f ", avg);
            }
            System.out.println();
        }
        
        System.out.println("\nAVERAGE TREE DEPTH:");
        System.out.println("-".repeat(50));
        System.out.printf("%-8s %-10s %-10s %-10s\n", "Depth", "IG", "IGR", "NWIG");
        System.out.println("-".repeat(40));
        
        for (int depth : depths) {
            System.out.printf("%-8d ", depth);
            
            for (Criterion crit : criteria) {
                ArrayList<Integer> treeDepths = depthResults.get(crit.toString()).get(depth);
                double sum = 0;
                for (int td : treeDepths) {
                    sum += td;
                }
                double avg = sum / treeDepths.size();
                System.out.printf("%-10.1f ", avg);
            }
            System.out.println();
        }
        
        // Analysis
        System.out.println("\nANALYSIS:");
        System.out.println("-".repeat(30));
        
        // Find best performing criterion at each depth
        System.out.println("Best performing criterion by depth (accuracy):");
        for (int depth : depths) {
            String bestCrit = "";
            double bestAcc = 0;
            
            for (Criterion crit : criteria) {
                ArrayList<Double> accuracies = accuracyResults.get(crit.toString()).get(depth);
                double sum = 0;
                for (double acc : accuracies) {
                    sum += acc;
                }
                double avg = sum / accuracies.size();
                
                if (avg > bestAcc) {
                    bestAcc = avg;
                    bestCrit = crit.toString();
                }
            }
            
            System.out.printf("Depth %d: %s (%.3f)\n", depth, bestCrit, bestAcc);
        }
    }
    // Run single experiment with specific criterion and depth
    public static void runSpecificExperiment(Dataset dataset, String datasetName, Criterion criterion, int maxDepth, int numRuns) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("EXPERIMENT: " + criterion + " with MaxDepth=" + maxDepth + " on " + datasetName);
        System.out.println("=".repeat(60));
        
        ArrayList<Double> accuracies = new ArrayList<>();
        ArrayList<Integer> nodes = new ArrayList<>();
        ArrayList<Integer> treeDepths = new ArrayList<>();
        
        System.out.println("Running " + numRuns + " experiments...");
        
        for (int run = 0; run < numRuns; run++) {
            if ((run + 1) % 5 == 0) {
                System.out.printf("Completed %d/%d runs\n", run + 1, numRuns);
            }
            
            Dataset[] split = split(dataset, 0.8);
            Dataset trainSet = split[0];
            Dataset testSet = split[1];
            
            DecisionTree tree = new DecisionTree(criterion, maxDepth);
            tree.train(trainSet);
            
            double accuracy = tree.getAccuracy(testSet);
            int nodeCount = tree.countNodes();
            int treeDepth = tree.getTreeDepth();
            
            accuracies.add(accuracy);
            nodes.add(nodeCount);
            treeDepths.add(treeDepth);
        }
        
        // Calculate statistics
        double avgAccuracy = accuracies.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double minAccuracy = accuracies.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double maxAccuracy = accuracies.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        
        double avgNodes = nodes.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double avgTreeDepth = treeDepths.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        
        // Display results
        System.out.println("\nRESULTS:");
        System.out.println("-".repeat(40));
        System.out.printf("Dataset: %s\n", datasetName);
        System.out.printf("Criterion: %s\n", criterion);
        System.out.printf("Max Depth: %d\n", maxDepth);
        System.out.printf("Number of runs: %d\n", numRuns);
        System.out.println();
        System.out.printf("Average Accuracy: %.3f\n", avgAccuracy);
        System.out.printf("Min Accuracy: %.3f\n", minAccuracy);
        System.out.printf("Max Accuracy: %.3f\n", maxAccuracy);
        System.out.printf("Average Nodes: %.1f\n", avgNodes);
        System.out.printf("Average Tree Depth: %.1f\n", avgTreeDepth);
        
        // Show individual results
        System.out.println("\nINDIVIDUAL RUN RESULTS:");
        System.out.println("-".repeat(40));
        System.out.printf("%-4s %-10s %-8s %-10s\n", "Run", "Accuracy", "Nodes", "TreeDepth");
        System.out.println("-".repeat(35));
        
        for (int i = 0; i < numRuns; i++) {
            System.out.printf("%-4d %-10.3f %-8d %-10d\n", 
                            i + 1, accuracies.get(i), nodes.get(i), treeDepths.get(i));
        }
    }
    
    // Print usage information
    public static void printUsage() {
        System.out.println("Decision Tree Learning Algorithm");
        System.out.println("Usage Options:");
        System.out.println();
        System.out.println("1. Run specific experiment:");
        System.out.println("   java DecisionTree <criterion> <maxDepth> [numRuns]");
        System.out.println("   Example: java DecisionTree IG 3");
        System.out.println("   Example: java DecisionTree IGR 5 10");
        System.out.println();
        System.out.println("2. Run comparison of all criteria:");
        System.out.println("   java DecisionTree compare");
        System.out.println();
        System.out.println("3. Run full experiments (20 runs each):");
        System.out.println("   java DecisionTree full");
        System.out.println();
        System.out.println("Parameters:");
        System.out.println("- criterion: IG, IGR, or NWIG");
        System.out.println("- maxDepth: 0 for no pruning, positive integer for max depth");
        System.out.println("- numRuns: number of runs (default: 20)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("- java DecisionTree IG 0        # IG with no pruning, 20 runs");
        System.out.println("- java DecisionTree NWIG 5 10    # NWIG with depth 5, 10 runs");
        System.out.println("- java DecisionTree compare      # Compare all criteria (single run)");
        System.out.println("- java DecisionTree full         # Full experiments (20 runs each)");
    }
    
    // Main method for command line usage
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        try {
            // Load datasets
            Dataset iris = loadIris("Iris.csv");
            Dataset adult = loadAdult("newData.data");

            
            // Add this to your main method right after loading datasets
            debugDataset(iris, "Iris");
            debugDataset(adult, "Adult");

            // Also add a simple test
            System.out.println("\n=== SIMPLE TEST ===");
            Dataset[] split = split(iris, 0.8, new Random(42));
            DecisionTree tree = new DecisionTree(Criterion.IG, 0);
            tree.train(split[0]);
            double accuracy = tree.getAccuracy(split[1]);
            System.out.println("Simple test accuracy: " + accuracy);
            System.out.println("Tree nodes: " + tree.countNodes());
            System.out.println("Tree depth: " + tree.getTreeDepth());
            
            System.out.println("Decision Tree Learning Algorithm");
            System.out.println("Datasets loaded successfully!");
            System.out.println("Iris dataset: " + iris.examples.size() + " examples");
            System.out.println("Adult dataset: " + adult.examples.size() + " examples");
            
            String mode = args[0].toLowerCase();
            
            if (mode.equals("compare")) {
                // Run single comparison (for analyzing nodes and depth)
                runSingleExperiment(iris, "Iris");
                runSingleExperiment(adult, "Adult");
                
            } else if (mode.equals("full")) {
                // Run full experiments (20 runs for accuracy analysis)
                runFullExperiments(iris, "Iris");
                runFullExperiments(adult, "Adult");
                
            } else {
                // Run specific experiment
                if (args.length < 2) {
                    System.out.println("Error: Missing maxDepth parameter");
                    printUsage();
                    return;
                }
                
                Criterion criterion = Criterion.valueOf(args[0].toUpperCase());
                int maxDepth = Integer.parseInt(args[1]);
                int numRuns = 20; // default
                
                if (args.length >= 3) {
                    numRuns = Integer.parseInt(args[2]);
                }
                
                // Run experiment on both datasets
                runSpecificExperiment(iris, "Iris", criterion, maxDepth, numRuns);
                runSpecificExperiment(adult, "Adult", criterion, maxDepth, numRuns);
            }
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("EXPERIMENTS COMPLETED!");
            System.out.println("=".repeat(60));
            
        } catch (IllegalArgumentException e) {
            System.out.println("Error: Invalid criterion or number format. Use IG, IGR, or NWIG, and valid numbers for maxDepth/numRuns");
            printUsage();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}