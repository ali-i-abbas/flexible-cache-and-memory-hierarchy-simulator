class MainMemory implements Level {
    
    public int mainMemoryTraffic;

    public MainMemory() {
        this.mainMemoryTraffic = 0;
    }

    public CacheBlock read(long address) {
        mainMemoryTraffic++;
        CacheBlock cacheBlock = new CacheBlock();
        cacheBlock.address = address;
        cacheBlock.data = Long.toHexString(address);
        cacheBlock.isValid = true;
        return cacheBlock;
    }

    public void write(long address, String data) {
        mainMemoryTraffic++;
        // it's a simulation so we don't actually do anything
    }

    // this method is not meaningful for MainMemory so it does nothing
    public void invalidate(long address) {
        
    }
}