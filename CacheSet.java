import java.util.ArrayList;

class CacheSet {
    public int cacheAssoc;
    public CacheBlock[] blocks;
    public ArrayList<Integer> trace;
    public Cache cache;

    private int accessSequenceNumber;
    private BinaryTree pseudoLRUBinaryTree;

    public CacheSet(int cacheAssoc, Cache cache) {
        this.cacheAssoc = cacheAssoc;
        this.cache = cache;

        this.blocks = new CacheBlock[cacheAssoc];

        // create the blocks in this set
        for (int i = 0; i < blocks.length; i++) {
            this.blocks[i] = new CacheBlock();
            this.blocks[i].id = i;
        }

        this.accessSequenceNumber = 0;

        if (cache.replacementPolicy == 1) {
            pseudoLRUBinaryTree = new BinaryTree(cacheAssoc);
        } else if (cache.replacementPolicy == 2) {
            trace = new ArrayList<Integer>();
        }

        
    }

    private void accessCacheBlock(CacheBlock cacheBlock) {
        switch (cache.replacementPolicy) {
            // LRU
            case 0:
                // if replacement policy is LRU, then increament the sequence number by 1 and assign it to the accessed block
                accessSequenceNumber++;
                cacheBlock.accessSequenceNumber = accessSequenceNumber;
                break;

            // Pseudo LRU
            case 1: 
                // set appropriate flags in the binary tree when traversing it to reach the block               
                pseudoLRUBinaryTree.accessBlock(cacheBlock.id);
                break;

            // Optimal
            case 2:                
                accessSequenceNumber++;
                break;
        
            
        }
        
    }

    public CacheBlock getCacheBlock(int tag) {
        for (CacheBlock cacheBlock : blocks) {
            if (cacheBlock.isValid && cacheBlock.tag == tag) {                
                accessCacheBlock(cacheBlock);                
                return cacheBlock;
            }
        }
        return null;
    }

    public CacheBlock allocatCacheBlock() {
        // if at least one invalid cache block is available return it
        for (CacheBlock cacheBlock : blocks) {
            if (!cacheBlock.isValid) {
                accessCacheBlock(cacheBlock);
                return cacheBlock;
            }
        }

        
        // all cache blocks are valid so we must select a victim according to replacement policy
        CacheBlock victim = null;
        switch (cache.replacementPolicy) {
            // LRU
            case 0:
                // the victim is the block with lowest sequence number
                victim = blocks[0];
                int minAccessSequenceNumber = blocks[0].accessSequenceNumber;
                for (int i = 1; i < blocks.length; i++) {
                    if (blocks[i].accessSequenceNumber < minAccessSequenceNumber) {
                        victim = blocks[i];
                        minAccessSequenceNumber = blocks[i].accessSequenceNumber;
                    }
                }                
                break;

            // Pseudo LRU
            case 1:                
                victim = blocks[pseudoLRUBinaryTree.findBlockToReplace()];
                break;

            // Optimal
            case 2:                
                // victim = blocks[0];
                int[] traceIndex = new int[cacheAssoc];
                for (int i = 0; i < traceIndex.length; i++) {
                    traceIndex[i] = -1; // -1 means farthest position in the trace
                }
                // start from index accessSequenceNumber in trace and record for each block at which position in the trace, it first will be accessed
                int count = 0;
                for (int i = accessSequenceNumber; i < trace.size(); i++) {
                    for (int j = 0; j < cacheAssoc; j++) {
                        if (traceIndex[j] == -1 && blocks[j].tag == trace.get(i)) {
                            // this is the first time that the block is accessed in trace after accessSequenceNumber position
                            traceIndex[j] = i;
                            count++;
                            // because only one block tag will match trace at location i, so no need to check other blocks
                            break;
                        }
                    }
                    if (count == cacheAssoc) {
                        // we found traceIndex of all blocks so no need for more search in trace
                        break;
                    }
                }
                // find the traceIndex with -1 or largest value which will be the victim index in blocks
                int maxTraceIndexValue = Integer.MIN_VALUE;
                int victimIndex = 0;
                for (int i = 0; i < traceIndex.length; i++) {
                    if (traceIndex[i] == -1) {
                        // -1 means farthest position in the trace
                        victimIndex = i;
                        break;
                    }
                    if (traceIndex[i] > maxTraceIndexValue) {
                        maxTraceIndexValue = traceIndex[i];
                        victimIndex = i;
                    }
                }
                victim = blocks[victimIndex];
                break;
            
        }

        accessCacheBlock(victim);

        return victim;
    }
    
}