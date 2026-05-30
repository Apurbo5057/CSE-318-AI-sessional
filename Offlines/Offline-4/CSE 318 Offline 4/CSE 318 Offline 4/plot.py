import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
from matplotlib.patches import Rectangle

# Set style for better-looking plots
plt.style.use('seaborn-v0_8')
sns.set_palette("husl")

# Sample data structure - Replace with your actual experimental results
# You'll need to populate these with data from your experiments

# Iris Dataset Results (Average Accuracy over 20 runs)
iris_depths = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
iris_ig_accuracy = [0.933, 0.567, 0.900, 0.950, 0.933, 0.933, 0.933, 0.933, 0.933, 0.933, 0.933]
iris_igr_accuracy = [0.933, 0.567, 0.900, 0.945, 0.933, 0.933, 0.933, 0.933, 0.933, 0.933, 0.933]
iris_nwig_accuracy = [0.933, 0.567, 0.900, 0.940, 0.933, 0.933, 0.933, 0.933, 0.933, 0.933, 0.933]

# Iris Dataset Results (Node Count - single run)
iris_ig_nodes = [11, 3, 5, 9, 11, 11, 11, 11, 11, 11, 11]
iris_igr_nodes = [11, 3, 5, 9, 11, 11, 11, 11, 11, 11, 11]
iris_nwig_nodes = [11, 3, 5, 9, 11, 11, 11, 11, 11, 11, 11]

# Adult Dataset Results (Average Accuracy over 20 runs)
adult_depths = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
adult_ig_accuracy = [0.820, 0.744, 0.822, 0.837, 0.835, 0.837, 0.840, 0.829, 0.827, 0.822, 0.821]
adult_igr_accuracy = [0.831, 0.788, 0.788, 0.837, 0.837, 0.835, 0.841, 0.843, 0.842, 0.840, 0.837]
adult_nwig_accuracy = [0.835, 0.788, 0.788, 0.799, 0.838, 0.842, 0.845, 0.847, 0.846, 0.845, 0.837]

# Adult Dataset Results (Node Count - single run)
adult_ig_nodes = [7098, 7, 75, 379, 969, 1763, 2617, 3480, 4328, 5106, 5803]
adult_igr_nodes = [6257, 3, 12, 30, 73, 367, 725, 1028, 1407, 1949, 2735]
adult_nwig_nodes = [7106, 3, 7, 19, 49, 115, 299, 653, 1341, 2318, 3564]

def create_accuracy_plots():
    """Create accuracy vs depth plots for both datasets"""
    
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))
    
    # Iris Dataset
    ax1.plot(iris_depths, iris_ig_accuracy, 'o-', linewidth=2, markersize=6, label='IG')
    ax1.plot(iris_depths, iris_igr_accuracy, 's-', linewidth=2, markersize=6, label='IGR')
    ax1.plot(iris_depths, iris_nwig_accuracy, '^-', linewidth=2, markersize=6, label='NWIG')
    
    ax1.set_xlabel('Maximum Tree Depth', fontsize=12)
    ax1.set_ylabel('Average Accuracy', fontsize=12)
    ax1.set_title('Iris Dataset: Accuracy vs Maximum Tree Depth', fontsize=14, fontweight='bold')
    ax1.grid(True, alpha=0.3)
    ax1.legend(fontsize=10)
    ax1.set_ylim(0.5, 1.0)
    
    # Adult Dataset
    ax2.plot(adult_depths, adult_ig_accuracy, 'o-', linewidth=2, markersize=6, label='IG')
    ax2.plot(adult_depths, adult_igr_accuracy, 's-', linewidth=2, markersize=6, label='IGR')
    ax2.plot(adult_depths, adult_nwig_accuracy, '^-', linewidth=2, markersize=6, label='NWIG')
    
    ax2.set_xlabel('Maximum Tree Depth', fontsize=12)
    ax2.set_ylabel('Average Accuracy', fontsize=12)
    ax2.set_title('Adult Dataset: Accuracy vs Maximum Tree Depth', fontsize=14, fontweight='bold')
    ax2.grid(True, alpha=0.3)
    ax2.legend(fontsize=10)
    ax2.set_ylim(0.7, 0.9)
    
    plt.tight_layout()
    plt.savefig('accuracy_vs_depth.png', dpi=300, bbox_inches='tight')
    plt.show()
    
    print("Accuracy vs Depth plot saved as 'accuracy_vs_depth.png'")

def create_nodes_plots():
    """Create nodes vs depth plots for both datasets"""
    
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))
    
    # Iris Dataset
    ax1.plot(iris_depths, iris_ig_nodes, 'o-', linewidth=2, markersize=6, label='IG')
    ax1.plot(iris_depths, iris_igr_nodes, 's-', linewidth=2, markersize=6, label='IGR')
    ax1.plot(iris_depths, iris_nwig_nodes, '^-', linewidth=2, markersize=6, label='NWIG')
    
    ax1.set_xlabel('Maximum Tree Depth', fontsize=12)
    ax1.set_ylabel('Number of Nodes', fontsize=12)
    ax1.set_title('Iris Dataset: Number of Nodes vs Maximum Tree Depth', fontsize=14, fontweight='bold')
    ax1.grid(True, alpha=0.3)
    ax1.legend(fontsize=10)
    
    # Adult Dataset (log scale due to large range)
    ax2.semilogy(adult_depths, adult_ig_nodes, 'o-', linewidth=2, markersize=6, label='IG')
    ax2.semilogy(adult_depths, adult_igr_nodes, 's-', linewidth=2, markersize=6, label='IGR')
    ax2.semilogy(adult_depths, adult_nwig_nodes, '^-', linewidth=2, markersize=6, label='NWIG')
    
    ax2.set_xlabel('Maximum Tree Depth', fontsize=12)
    ax2.set_ylabel('Number of Nodes (log scale)', fontsize=12)
    ax2.set_title('Adult Dataset: Number of Nodes vs Maximum Tree Depth', fontsize=14, fontweight='bold')
    ax2.grid(True, alpha=0.3)
    ax2.legend(fontsize=10)
    
    plt.tight_layout()
    plt.savefig('nodes_vs_depth.png', dpi=300, bbox_inches='tight')
    plt.show()
    
    print("Nodes vs Depth plot saved as 'nodes_vs_depth.png'")

def create_comparison_chart():
    """Create a comparison chart for different criteria without pruning"""
    
    # Data for comparison (MaxDepth = 0)
    datasets = ['Iris', 'Adult']
    criteria = ['IG', 'IGR', 'NWIG']
    
    # Accuracy data
    iris_no_pruning = [0.933, 0.933, 0.933]
    adult_no_pruning = [0.820, 0.831, 0.835]
    
    # Node count data
    iris_nodes_no_pruning = [11, 11, 11]
    adult_nodes_no_pruning = [7098, 6257, 7106]
    
    fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(15, 10))
    
    # Accuracy comparison
    x = np.arange(len(criteria))
    width = 0.35
    
    ax1.bar(x - width/2, iris_no_pruning, width, label='Iris', alpha=0.8)
    ax1.bar(x + width/2, adult_no_pruning, width, label='Adult', alpha=0.8)
    ax1.set_xlabel('Criteria', fontsize=12)
    ax1.set_ylabel('Accuracy', fontsize=12)
    ax1.set_title('Accuracy Comparison (No Pruning)', fontsize=14, fontweight='bold')
    ax1.set_xticks(x)
    ax1.set_xticklabels(criteria)
    ax1.legend()
    ax1.grid(True, alpha=0.3)
    
    # Node count comparison
    ax2.bar(x - width/2, iris_nodes_no_pruning, width, label='Iris', alpha=0.8)
    ax2.bar(x + width/2, adult_nodes_no_pruning, width, label='Adult', alpha=0.8)
    ax2.set_xlabel('Criteria', fontsize=12)
    ax2.set_ylabel('Number of Nodes', fontsize=12)
    ax2.set_title('Node Count Comparison (No Pruning)', fontsize=14, fontweight='bold')
    ax2.set_xticks(x)
    ax2.set_xticklabels(criteria)
    ax2.legend()
    ax2.grid(True, alpha=0.3)
    
    # Overfitting analysis - difference between no pruning and best pruning
    iris_best_pruning = [0.950, 0.945, 0.940]  # Best accuracy with pruning
    adult_best_pruning = [0.840, 0.843, 0.847]  # Best accuracy with pruning
    
    iris_improvement = [best - no_prune for best, no_prune in zip(iris_best_pruning, iris_no_pruning)]
    adult_improvement = [best - no_prune for best, no_prune in zip(adult_best_pruning, adult_no_pruning)]
    
    ax3.bar(x - width/2, iris_improvement, width, label='Iris', alpha=0.8)
    ax3.bar(x + width/2, adult_improvement, width, label='Adult', alpha=0.8)
    ax3.set_xlabel('Criteria', fontsize=12)
    ax3.set_ylabel('Accuracy Improvement', fontsize=12)
    ax3.set_title('Pruning Benefit (Best Pruned - No Pruning)', fontsize=14, fontweight='bold')
    ax3.set_xticks(x)
    ax3.set_xticklabels(criteria)
    ax3.legend()
    ax3.grid(True, alpha=0.3)
    ax3.axhline(y=0, color='black', linestyle='-', alpha=0.3)
    
    # Tree complexity reduction
    iris_best_nodes = [9, 9, 9]  # Nodes at best pruning depth
    adult_best_nodes = [653, 1028, 653]  # Nodes at best pruning depth
    
    iris_reduction = [(no_prune - best) / no_prune * 100 for no_prune, best in zip(iris_nodes_no_pruning, iris_best_nodes)]
    adult_reduction = [(no_prune - best) / no_prune * 100 for no_prune, best in zip(adult_nodes_no_pruning, adult_best_nodes)]
    
    ax4.bar(x - width/2, iris_reduction, width, label='Iris', alpha=0.8)
    ax4.bar(x + width/2, adult_reduction, width, label='Adult', alpha=0.8)
    ax4.set_xlabel('Criteria', fontsize=12)
    ax4.set_ylabel('Node Reduction (%)', fontsize=12)
    ax4.set_title('Tree Complexity Reduction with Pruning', fontsize=14, fontweight='bold')
    ax4.set_xticks(x)
    ax4.set_xticklabels(criteria)
    ax4.legend()
    ax4.grid(True, alpha=0.3)
    
    plt.tight_layout()
    plt.savefig('criteria_comparison.png', dpi=300, bbox_inches='tight')
    plt.show()
    
    print("Criteria comparison chart saved as 'criteria_comparison.png'")

def create_all_plots():
    """Create all required plots"""
    print("Generating all plots...")
    create_accuracy_plots()
    create_nodes_plots()
    create_comparison_chart()
    print("All plots generated successfully!")

if __name__ == "__main__":
    create_all_plots()