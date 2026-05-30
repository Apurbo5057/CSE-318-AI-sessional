import java.io.*;
import java.util.*;

enum Criterion {
    IG, IGR, NWIG, MODIFIED_IG
}

class TreeNode {
    // The attribute on which the split is made at this node
    String attribute;
    
    // For numeric attributes
    boolean isNumericValue;
    double thresholdValue;
    
    // The category from the parent node that leads to this child node
    String category;
    
    // Boolean to indicate if this node is a leaf node
    boolean isLeaf;
    
    // The class label if this node is a leaf node
    String classLabel;
    
    // Map of child nodes
    HashMap<String, TreeNode> children;
    
    // Current node's depth in the tree
    int depth;
    
    // Flag to indicate if this node was forced to be a leaf due to depth limit
    boolean forcedLeaf;

    // Constructor for internal nodes
    TreeNode() {
        this.children = new HashMap<>();
        this.isLeaf = false;
        this.depth = 0;
        this.forcedLeaf = false;
    }

    // Constructor for leaf nodes
    TreeNode(String classification) {
        this.classLabel = classification;
        this.isLeaf = true;
        this.children = new HashMap<>();
        this.depth = 0;
        this.forcedLeaf = false;
    }
}

class Result {
    String dataset;
    String criterion;
    int maxDepth;
    double accuracy;
    int nodes;
    int treeDepth;
    int forcedLeafNodes; // New field for task 2

    Result(String dataset, String criterion, int maxDepth, double accuracy, int nodes, int treeDepth, int forcedLeafNodes) {
        this.dataset = dataset;
        this.criterion = criterion;
        this.maxDepth = maxDepth;
        this.accuracy = accuracy;
        this.nodes = nodes;
        this.treeDepth = treeDepth;
        this.forcedLeafNodes = forcedLeafNodes;
    }
}

class Dataset {
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

    // Get subset for categorical attributes
    Dataset subset(String attribute, String category) {
        int attributeIndex = attributes.indexOf(attribute);
        ArrayList<String[]> subsetExamples = new ArrayList<>();

        for (String[] example : examples) {
            if (example[attributeIndex].equalsIgnoreCase(category)) {
                subsetExamples.add(example);
            }
        }

        return new Dataset(subsetExamples, attributes, targetAttribute);
    }

    // Get subset for numeric attributes
    Dataset subsetNumeric(String attribute, double thresholdValue, boolean isLessThanOrEqual) {
        int attributeIndex = attributes.indexOf(attribute);
        ArrayList<String[]> subsetExamples = new ArrayList<>();

        for (String[] example : examples) {
            try {
                double value = Double.parseDouble(example[attributeIndex]);
                if (isLessThanOrEqual && value <= thresholdValue ||
                        !isLessThanOrEqual && value > thresholdValue) {
                    subsetExamples.add(example);
                }
            } catch (NumberFormatException e) {
                continue;
            }
        }

        return new Dataset(subsetExamples, attributes, targetAttribute);
    }

    // Get all possible categories for a given attribute
    HashSet<String> getCategories(String attribute) {
        int attributeIndex = attributes.indexOf(attribute);
        HashSet<String> categories = new HashSet<>();

        for (String[] example : examples) {
            categories.add(example[attributeIndex]);
        }

        return categories;
    }

    // Get the most common class label in the dataset
    String getMostCommonClassLabel() {
        HashMap<String, Integer> classCount = new HashMap<>();

        for (String[] example : examples) {
            String classLabel = example[targetIndex];
            classCount.put(classLabel, classCount.getOrDefault(classLabel, 0) + 1);
        }

        String mostCommonClass = null;
        int maxCount = 0;

        for (Map.Entry<String, Integer> entry : classCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostCommonClass = entry.getKey();
            }
        }

        return mostCommonClass;
    }

    // Check if all examples have the same class label
    boolean isPure() {
        if (examples.isEmpty()) {
            return false;
        }

        String firstClassLabel = examples.get(0)[targetIndex];
        for (String[] example : examples) {
            if (!example[targetIndex].equals(firstClassLabel)) {
                return false;
            }
        }

        return true;
    }
}

class DecisionTree {
    private Criterion criterion;
    private int maxDepth;
    private TreeNode root;
    private double lastBestThreshold = Double.NaN;
    private boolean lastBestIsNumeric = false;
    private int forcedLeafCount = 0; // Counter for forced leaf nodes
    private double lambda = 0.1; // Hyperparameter for modified IG

    public DecisionTree(Criterion criterion, int maxDepth) {
        this.criterion = criterion;
        this.maxDepth = maxDepth;
        this.forcedLeafCount = 0;
    }

    // Set lambda for modified IG
    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    // Calculate entropy using the formula: Entropy(S) = -Σ(p(x) * log2(p(x)))
    private double entropy(Dataset dataset) {
        if (dataset.examples.isEmpty()) {
            return 0.0;
        }

        HashMap<String, Integer> classCount = new HashMap<>();

        for (String[] example : dataset.examples) {
            String classLabel = example[dataset.targetIndex];
            classCount.put(classLabel, classCount.getOrDefault(classLabel, 0) + 1);
        }

        double entropy = 0.0;
        int totalExamples = dataset.examples.size();

        for (int count : classCount.values()) {
            double probability = (double) count / totalExamples;
            entropy -= probability * Math.log(probability) / Math.log(2);
        }

        return entropy;
    }

    // Calculate Information Gain: IG(S, A) = Entropy(S) - Σ(|Sv|/|S| * Entropy(Sv))
    private double informationGain(Dataset dataset, String attribute) {
        double totalEntropy = entropy(dataset);
        double weightedEntropy = 0.0;

        HashSet<String> categories = dataset.getCategories(attribute);
        int totalExamples = dataset.examples.size();

        for (String category : categories) {
            Dataset subset = dataset.subset(attribute, category);
            double subsetEntropy = entropy(subset);
            weightedEntropy += ((double) subset.examples.size() / totalExamples) * subsetEntropy;
        }

        return totalEntropy - weightedEntropy;
    }

    // Calculate Intrinsic Value for IGR
    private double intrinsicValue(Dataset dataset, String attribute) {
        double intrinsicValue = 0.0;
        HashSet<String> categories = dataset.getCategories(attribute);
        int totalExamples = dataset.examples.size();

        for (String category : categories) {
            Dataset subset = dataset.subset(attribute, category);
            double proportion = (double) subset.examples.size() / totalExamples;
            if (proportion > 0) {
                intrinsicValue -= proportion * Math.log(proportion) / Math.log(2);
            }
        }

        return intrinsicValue;
    }

    // Calculate Information Gain Ratio: IGR(S, A) = IG(S, A) / IntrinsicValue(S, A)
    private double informationGainRatio(Dataset dataset, String attribute) {
        double ig = informationGain(dataset, attribute);
        double iv = intrinsicValue(dataset, attribute);
        return iv == 0 ? 0 : ig / iv;
    }

    // Calculate Normalized Weighted Information Gain
    private double normalizedWeightedInformationGain(Dataset dataset, String attribute) {
        double ig = informationGain(dataset, attribute);
        int totalExamples = dataset.examples.size();
        int k = dataset.getCategories(attribute).size();
        double penalty = Math.log(k + 1) / Math.log(2);
        double adjustment = 1.0 - (double) (k - 1) / totalExamples;

        return (ig / penalty) * adjustment;
    }

    // Task 1: Modified Information Gain with penalty term
    private double modifiedInformationGain(Dataset dataset, String attribute) {
        double ig = informationGain(dataset, attribute);
        int v = dataset.getCategories(attribute).size();
        int totalExamples = dataset.examples.size();
        double penalty = lambda * (double) (v - 1) / totalExamples;
        
        return ig - penalty;
    }

    // Improved numeric split algorithm with better performance
    private double[] bestNumericSplit(Dataset dataset, String attribute) {
        int idx = dataset.attributes.indexOf(attribute);
        
        // Create a list of unique values sorted
        Set<Double> uniqueValues = new TreeSet<>();
        for (String[] example : dataset.examples) {
            try {
                double value = Double.parseDouble(example[idx]);
                uniqueValues.add(value);
            } catch (NumberFormatException e) {
                // Skip non-numeric values
            }
        }
        
        List<Double> sortedValues = new ArrayList<>(uniqueValues);
        
        if (sortedValues.size() < 2) {
            return new double[]{Double.NaN, -1};
        }
        
        double bestThreshold = Double.NaN;
        double bestGain = -1;
        
        // Only check thresholds between consecutive unique values
        for (int i = 0; i < sortedValues.size() - 1; i++) {
            double threshold = (sortedValues.get(i) + sortedValues.get(i + 1)) / 2.0;
            
            // Calculate gain for this threshold
            Dataset leftSubset = dataset.subsetNumeric(attribute, threshold, true);
            Dataset rightSubset = dataset.subsetNumeric(attribute, threshold, false);
            
            if (leftSubset.examples.isEmpty() || rightSubset.examples.isEmpty()) {
                continue;
            }
            
            double weightedEntropy = ((double) leftSubset.examples.size() / dataset.examples.size()) * entropy(leftSubset)
                    + ((double) rightSubset.examples.size() / dataset.examples.size()) * entropy(rightSubset);
            double gain = entropy(dataset) - weightedEntropy;
            
            if (gain > bestGain) {
                bestGain = gain;
                bestThreshold = threshold;
            }
        }
        
        return new double[]{bestThreshold, bestGain};
    }

    // Select the best attribute to split on based on the chosen criterion
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
                double[] result = bestNumericSplit(dataset, attribute);
                threshold = result[0];
                score = result[1];
            } else {
                switch (criterion) {
                    case IG:
                        score = informationGain(dataset, attribute);
                        break;
                    case IGR:
                        score = informationGainRatio(dataset, attribute);
                        break;
                    case NWIG:
                        score = normalizedWeightedInformationGain(dataset, attribute);
                        break;
                    case MODIFIED_IG:
                        score = modifiedInformationGain(dataset, attribute);
                        break;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestAttribute = attribute;
                bestThreshold = threshold;
                bestIsNumeric = isNumeric;
            }
        }

        this.lastBestThreshold = bestThreshold;
        this.lastBestIsNumeric = bestIsNumeric;
        return bestAttribute;
    }

    // Check if an attribute has numeric values
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

    // Build the decision tree recursively
    private TreeNode buildTree(Dataset dataset, ArrayList<String> attributes, int currentDepth) {
        // Base case: empty dataset
        if (dataset.examples.isEmpty()) {
            return new TreeNode("unknown");
        }

        // Base case: pure dataset (all examples have the same class)
        if (dataset.isPure()) {
            TreeNode leafNode = new TreeNode(dataset.examples.get(0)[dataset.targetIndex]);
            leafNode.depth = currentDepth;
            return leafNode;
        }

        // Check if we should create a forced leaf due to depth limit
        boolean shouldCreateForcedLeaf = (maxDepth > 0 && currentDepth >= maxDepth);
        
        // Base case: no more attributes or depth limit reached
        if (attributes.isEmpty() || shouldCreateForcedLeaf) {
            TreeNode leafNode = new TreeNode(dataset.getMostCommonClassLabel());
            leafNode.depth = currentDepth;
            
            // Task 2: Count forced leaf nodes
            if (shouldCreateForcedLeaf && !attributes.isEmpty() && dataset.examples.size() > 1) {
                leafNode.forcedLeaf = true;
                forcedLeafCount++;
            }
            
            return leafNode;
        }

        // Select the best attribute to split on
        String bestAttribute = selectBestAttribute(dataset, attributes);
        
        if (bestAttribute == null) {
            TreeNode leafNode = new TreeNode(dataset.getMostCommonClassLabel());
            leafNode.depth = currentDepth;
            return leafNode;
        }

        // Create internal node
        TreeNode node = new TreeNode();
        node.attribute = bestAttribute;
        node.depth = currentDepth;

        // Remove the selected attribute from the list for child nodes
        ArrayList<String> newAttributes = new ArrayList<>(attributes);
        newAttributes.remove(bestAttribute);

        // Handle numeric attributes
        if (lastBestIsNumeric) {
            node.isNumericValue = true;
            node.thresholdValue = lastBestThreshold;
            
            Dataset leftSubset = dataset.subsetNumeric(bestAttribute, lastBestThreshold, true);
            Dataset rightSubset = dataset.subsetNumeric(bestAttribute, lastBestThreshold, false);
            
            TreeNode leftChild = buildTree(leftSubset, newAttributes, currentDepth + 1);
            TreeNode rightChild = buildTree(rightSubset, newAttributes, currentDepth + 1);
            
            leftChild.category = "left";
            rightChild.category = "right";
            
            node.children.put("left", leftChild);
            node.children.put("right", rightChild);
        } else {
            // Handle categorical attributes
            HashSet<String> categories = dataset.getCategories(bestAttribute);
            for (String category : categories) {
                Dataset subset = dataset.subset(bestAttribute, category);
                TreeNode childNode = buildTree(subset, newAttributes, currentDepth + 1);
                childNode.category = category;
                node.children.put(category, childNode);
            }
        }

        return node;
    }

    // Train the decision tree
    public void train(Dataset dataset) {
        forcedLeafCount = 0; // Reset counter
        ArrayList<String> attributes = new ArrayList<>(dataset.attributes);
        this.root = buildTree(dataset, attributes, 0);
    }

    // Predict the class label for a given example
    public String predict(String[] example, ArrayList<String> attributes) {
        return predictHelper(root, example, attributes);
    }

    // Helper method for prediction
    private String predictHelper(TreeNode node, String[] example, ArrayList<String> attributes) {
        if (node.isLeaf) {
            return node.classLabel;
        }

        int attributeIndex = attributes.indexOf(node.attribute);
        
        if (node.isNumericValue) {
            try {
                double value = Double.parseDouble(example[attributeIndex]);
                if (value <= node.thresholdValue) {
                    return predictHelper(node.children.get("left"), example, attributes);
                } else {
                    return predictHelper(node.children.get("right"), example, attributes);
                }
            } catch (NumberFormatException e) {
                // If parsing fails, return the most common class
                return "unknown";
            }
        } else {
            String attributeValue = example[attributeIndex];
            TreeNode childNode = node.children.get(attributeValue);
            
            if (childNode != null) {
                return predictHelper(childNode, example, attributes);
            } else {
                // If we encounter an unseen category, return unknown
                return "unknown";
            }
        }
    }

    // Calculate accuracy on a test dataset
    public double calculateAccuracy(Dataset testDataset) {
        int correct = 0;
        int total = testDataset.examples.size();

        for (String[] example : testDataset.examples) {
            String predicted = predict(example, testDataset.attributes);
            String actual = example[testDataset.targetIndex];
            
            if (predicted.equals(actual)) {
                correct++;
            }
        }

        return (double) correct / total;
    }

    // Count total number of nodes in the tree
    public int countNodes() {
        return countNodesHelper(root);
    }

    private int countNodesHelper(TreeNode node) {
        if (node == null) return 0;
        
        int count = 1;
        for (TreeNode child : node.children.values()) {
            count += countNodesHelper(child);
        }
        
        return count;
    }

    // Get the maximum depth of the tree
    public int getTreeDepth() {
        return getTreeDepthHelper(root);
    }

    private int getTreeDepthHelper(TreeNode node) {
        if (node == null || node.isLeaf) return node != null ? node.depth : 0;
        
        int maxDepth = node.depth;
        for (TreeNode child : node.children.values()) {
            maxDepth = Math.max(maxDepth, getTreeDepthHelper(child));
        }
        
        return maxDepth;
    }

    // Get the number of forced leaf nodes
    public int getForcedLeafCount() {
        return forcedLeafCount;
    }

    // Print the tree structure (for debugging)
    public void printTree() {
        printTreeHelper(root, "");
    }

    private void printTreeHelper(TreeNode node, String indent) {
        if (node == null) return;
        
        if (node.isLeaf) {
            System.out.println(indent + "Leaf: " + node.classLabel + 
                             (node.forcedLeaf ? " (FORCED)" : ""));
        } else {
            if (node.isNumericValue) {
                System.out.println(indent + node.attribute + " <= " + node.thresholdValue);
                printTreeHelper(node.children.get("left"), indent + "  ");
                System.out.println(indent + node.attribute + " > " + node.thresholdValue);
                printTreeHelper(node.children.get("right"), indent + "  ");
            } else {
                System.out.println(indent + node.attribute + ":");
                for (Map.Entry<String, TreeNode> entry : node.children.entrySet()) {
                    System.out.println(indent + "  " + entry.getKey() + " -> ");
                    printTreeHelper(entry.getValue(), indent + "    ");
                }
            }
        }
    }

    // Main method for testing and running experiments
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java DecisionTree <criterion> <maxDepth>");
            System.out.println("Criterion: IG, IGR, NWIG, MODIFIED_IG");
            return;
        }

        String criterionStr = args[0].toUpperCase();
        int maxDepth = Integer.parseInt(args[1]);

        Criterion criterion;
        try {
            criterion = Criterion.valueOf(criterionStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid criterion. Use: IG, IGR, NWIG, or MODIFIED_IG");
            return;
        }

        // Example usage - you would need to implement dataset loading
        System.out.println("Decision Tree with " + criterion + " criterion and max depth " + maxDepth);
        
        // Run experiments here
        runExperiments(criterion, maxDepth);
    }

    // Run experiments with different criteria and depths
    private static void runExperiments(Criterion criterion, int maxDepth) {
        // This method would implement the 20 random splits experiment
        // and collect results for analysis
        
        int numExperiments = 20;
        double totalAccuracy = 0;
        int totalNodes = 0;
        int totalDepth = 0;
        int totalForcedLeaves = 0;
        
        for (int i = 0; i < numExperiments; i++) {
            // TODO: Load dataset and split randomly
            // Dataset dataset = loadDataset();
            // Dataset[] split = randomSplit(dataset, 0.8);
            // Dataset trainData = split[0];
            // Dataset testData = split[1];
            
            DecisionTree tree = new DecisionTree(criterion, maxDepth);
            // tree.train(trainData);
            
            // double accuracy = tree.calculateAccuracy(testData);
            // totalAccuracy += accuracy;
            // totalNodes += tree.countNodes();
            // totalDepth += tree.getTreeDepth();
            // totalForcedLeaves += tree.getForcedLeafCount();
        }
        
        // Print results
        System.out.println("Average Accuracy: " + (totalAccuracy / numExperiments));
        System.out.println("Average Nodes: " + (totalNodes / numExperiments));
        System.out.println("Average Depth: " + (totalDepth / numExperiments));
        System.out.println("Average Forced Leaves: " + (totalForcedLeaves / numExperiments));
    }
}