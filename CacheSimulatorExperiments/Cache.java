class Cache implements Level {
    public int cacheSize;
    public int cacheAssoc;
    public int blockSize;
    public int replacementPolicy;
    public int inclusionProperty;


    // # sets 
    public int setsSize;

    public Level previousLevel;
    public Level nextLevel;

    public MainMemory mainMemory;

    // array of sets
    public CacheSet[] sets;

    public int numberOfReads;
    public int numberOfReadMisses;
    public int numberOfWrites;
    public int numberOfWriteMisses;
    public int numberOfWritebacks;
    public int directMainMemoryTraffic;

    private int addressBitsSize;
    private int blockOffsetBitsSize;
    private int indexBitsSize;
    private int tagBitsSize;

    public Cache(int cacheSize, int cacheAssoc, int blockSize, int addressBitsSize, int replacementPolicy, int inclusionProperty, MainMemory mainMemory){
        this.cacheSize = cacheSize;
        this.cacheAssoc = cacheAssoc;
        this.blockSize = blockSize;
        this.addressBitsSize = addressBitsSize;
        this.replacementPolicy = replacementPolicy;
        this.inclusionProperty = inclusionProperty;
        this.mainMemory = mainMemory;

        this.setsSize = cacheSize / (blockSize * cacheAssoc);

        this.blockOffsetBitsSize = (int)(Math.log(blockSize) / Math.log(2)); 
        this.indexBitsSize = (int)(Math.log(this.setsSize) / Math.log(2)); 
        this.tagBitsSize = addressBitsSize - indexBitsSize - blockOffsetBitsSize;

        this.sets = new CacheSet[this.setsSize];

        // create cache sets
        for (int i = 0; i < sets.length; i++) {
            this.sets[i] = new CacheSet(cacheAssoc, this);
        }

        // initialize statistics
        this.numberOfReads = 0;
        this.numberOfReadMisses = 0;
        this.numberOfWrites = 0;
        this.numberOfWriteMisses = 0;
        this.numberOfWritebacks = 0;
        this.directMainMemoryTraffic = 0;
    }

    public int getAddressIndex(long address) {
        long mask = this.setsSize - 1;
        return (int) ((address >> blockOffsetBitsSize) & mask);
    }

    public int getAddressTag(long address) {
        return (int) (address >> (blockOffsetBitsSize + indexBitsSize));
    }

    // allocate a chache block for read or write requests
    private CacheBlock allocatCacheBlock(long address, CacheSet cacheSet) {
        
        // allocate a cache block for data to be read or written
        CacheBlock cacheBlock = cacheSet.allocatCacheBlock();

        // if cache is inclusive and the cach block is valid which means it needs to be evicted, 
        // then we must also invalidate it in previous level
        if (inclusionProperty == 1 && cacheBlock.isValid) {
            previousLevel.invalidate(cacheBlock.address);
        }

        // if cach block is valid and is dirty, then cache block data must be written to next level
        if (cacheBlock.isValid && cacheBlock.isDirty) {
            numberOfWritebacks++;
            nextLevel.write(cacheBlock.address, cacheBlock.data);
        }

        return cacheBlock;
    }
    
    public CacheBlock read(long address) {
        // System.out.println(address + " - " + getAddressTag(address) + " - " + getAddressIndex(address));
        
        numberOfReads++;

        // get index and tag parts of the address
        int index = getAddressIndex(address);
        int tag = getAddressTag(address);

        // String tagStr = Integer.toHexString(tag);
        // String add = Long.toHexString(address);

        // try to find the cache block
        CacheBlock cacheBlock = sets[index].getCacheBlock(tag);

        
        if (cacheBlock == null) {
            // this is a miss since we didnt find a valid cache block

            numberOfReadMisses++;
            
            // allocate a cache block for data
            cacheBlock = allocatCacheBlock(address, sets[index]);

            // get the cache block that has the data at address from next level
            CacheBlock readCacheBlock = nextLevel.read(address);

            // copy readCacheBlock data to allocated cache block
            cacheBlock.tag = tag;
            cacheBlock.address = readCacheBlock.address;
            cacheBlock.data = readCacheBlock.data;
            cacheBlock.isValid = true;
            cacheBlock.isDirty = false;

        } 
        // if cacheBlock != null then it is a hit since we found a valid cache block
            

        return cacheBlock;
    }
    
    public void write(long address, String data) {

        numberOfWrites++;
        
        // get index and tag parts of the address
        int index = getAddressIndex(address);
        int tag = getAddressTag(address);

        // String tagStr = Integer.toHexString(tag);
        // String add = Long.toHexString(address);

        // try to find the cache block
        CacheBlock cacheBlock = sets[index].getCacheBlock(tag);
        
        if (cacheBlock == null) {
            // this is a miss since we didnt find a valid cache block

            numberOfWriteMisses++;
            
            // allocate a cache block for data
            cacheBlock = allocatCacheBlock(address, sets[index]);

            // get the cache block that has the data at address from next level
            CacheBlock readCacheBlock = nextLevel.read(address);

            // write data to allocated cache block and set dirty to true
            cacheBlock.tag = tag;
            cacheBlock.address = address;
            cacheBlock.data = data;
            cacheBlock.isValid = true;
            cacheBlock.isDirty = true;

        } else {
            // this is a hit since we found a valid cache block
            
            // put data in cache block and mark it dirty
            cacheBlock.data = data;
            cacheBlock.isDirty = true;
        }

    }

    public void invalidate(long address) {

        // get index and tag parts of the address
        int index = getAddressIndex(address);
        int tag = getAddressTag(address);

        // try to find the cache block
        CacheBlock cacheBlock = sets[index].getCacheBlock(tag);

        // cache block is found
        if (cacheBlock != null) {
            // if cache block is dirty we need to write its data directly to main memory
            if (cacheBlock.isDirty) {
                directMainMemoryTraffic++;
                mainMemory.write(cacheBlock.address, cacheBlock.data);
                cacheBlock.isDirty = false;
            }
            cacheBlock.isValid = false;
        }
    }
}