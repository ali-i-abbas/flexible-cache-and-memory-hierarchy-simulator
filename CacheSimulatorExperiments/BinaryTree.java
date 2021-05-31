class BinaryTree {
    private int[] binaryTreeArray;
    private int size;
    // starting index of where leaves are stored
    private int start;

    // number of leaves is equal to cache set associativity
    // we will store index of the block in the set from start index to end of array
    public BinaryTree(int numberOfLeaves) {
        size = numberOfLeaves * 2 - 1;
        binaryTreeArray = new int[size];
        start = numberOfLeaves - 1;
    }

    public void accessBlock(int blockIndex) {
        accessLeaf(start + blockIndex);
    }

    // traverse the tree from the leaf to the root and update node values on the path
    // 1 mean we came from right child and 0 means we came from left child
    private void accessLeaf(int leafIndex) {
        if (leafIndex == 0) {
            return;
        }

        int parentIndex = (leafIndex - 1) / 2;
        binaryTreeArray[parentIndex] = (leafIndex - 1) % 2;

        accessLeaf(parentIndex);
    }

    public int findBlockToReplace() {
        return findLeaf(0) - start;
    }

    // traverse the tree from the root to the leaf corresponding to block to be replaced and update node values on the path
    // return index of the leaf in binaryTreeArray
    private int findLeaf(int nodeIndex) {
        if (nodeIndex >= start && nodeIndex < size) {
            return nodeIndex;
        }

        // flip the bit (1 to 0 or 0 to 1)
        binaryTreeArray[nodeIndex] = 1 - binaryTreeArray[nodeIndex];

        if (binaryTreeArray[nodeIndex] == 0) {
            // go to left child
            return findLeaf(2 * nodeIndex + 1);
        } else {
            // go to right child
            return findLeaf(2 * nodeIndex + 2);
        }
    }
    
}