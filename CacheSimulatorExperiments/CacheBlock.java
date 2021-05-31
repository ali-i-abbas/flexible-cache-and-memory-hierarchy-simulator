class CacheBlock {
    public int id;
    public int tag;
    public boolean isDirty;
    public boolean isValid;

    // this is to make simulation easier and is not part of actual block
    public long address;

    // for convenience we simulate data as a string but for more accurate representation it should be 
    // a byte array with size blockSize that can be selected for read or write with offset bits of the address
    public String data;

    public int accessSequenceNumber;

    public CacheBlock() {
        this.isDirty = false;
        this.isValid = false;
        this.accessSequenceNumber = 0;
    }
}