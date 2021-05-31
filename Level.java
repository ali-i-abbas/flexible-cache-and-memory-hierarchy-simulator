// abstract cpu, cache, and memory as objects that can read and write
interface Level {
    public CacheBlock read(long address);
    public void write(long address, String data);
    public void invalidate(long address);
}