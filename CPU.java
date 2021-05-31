class CPU implements Level {

    public Level nextLevel;

    public CacheBlock read(long address) {
        // cpu initiates the read to the next level which is level 1 cache
        return nextLevel.read(address);
    }

    public void write(long address, String data) {
        nextLevel.write(address, data);
    }

    // this method is not meaningful for CPU so it does nothing
    public void invalidate(long address) {
        
    }
    
}