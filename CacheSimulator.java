import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

class CacheSimulator {
	public CacheSimulator(String[] args) {
		
		// # bits of address 
		int addressBitsSize = 32;
		
		// parse input arguments
		int blockSize = tryParseInt(args[0]);
		int l1Size = tryParseInt(args[1]);
		int l1Assoc = tryParseInt(args[2]);
		int l2Size = tryParseInt(args[3]);
		int l2Assoc = tryParseInt(args[4]);
		int replacementPolicy = tryParseInt(args[5]);
		int inclusionProperty = tryParseInt(args[6]);

		String traceFile = args[7];

		// create cpu and main memory objects
		CPU cpu = new CPU();
		MainMemory mainMemory = new MainMemory();

		// cacheLevels is 1 if L2 cache size is 0, otherwise it's 2
		int cacheLevels = l2Size == 0 ? 1 : 2;

		// caches array will hold as many caches as cacheLevels
		Cache[] caches = new Cache[cacheLevels];

		// arrays of cache sizes and cashes assoc, this will allow generalizing number of levels
		int[] cachesSize = new int[cacheLevels];
		int[] cachesAssoc = new int[cacheLevels];

		if (cacheLevels == 1) {
			cachesSize[0] = l1Size;
			cachesAssoc[0] = l1Assoc;
		} else {
			cachesSize[0] = l1Size;
			cachesAssoc[0] = l1Assoc;
			cachesSize[1] = l2Size;
			cachesAssoc[1] = l2Assoc;
		}

		// create caches with given parameters
		for (int i = 0; i < cachesAssoc.length; i++) {
			caches[i] = new Cache(cachesSize[i], cachesAssoc[i], blockSize, addressBitsSize, replacementPolicy, inclusionProperty, mainMemory);
		}

		linkMemoryHierarchyLevels(cpu, mainMemory, cacheLevels, caches);

		// preprocess trace for optimal policy
		if (replacementPolicy == 2) {
			try (Scanner scanner = new Scanner(new File(traceFile))) {
				while (scanner.hasNext()) {
					String readWriteToken = scanner.next().toLowerCase().replaceAll("[^r,^w]", "");
					String addressStr = scanner.next();
					
					long address = tryParseHexToLong(addressStr);

					// find the index and tag of each address for each cache to determine which cache set will process it
					// and the add the tag to the cache set trace array
					for (int i = 0; i < cachesAssoc.length; i++) {
						int index = caches[0].getAddressIndex(address);
						int tag = caches[0].getAddressTag(address);
						caches[i].sets[index].trace.add(tag);
					}
									
				}
			} catch (FileNotFoundException e) {
				System.out.println("Trace File <" + traceFile + "> not found.");
				System.exit(1); 
			}
			
		}

		runSimulation(traceFile, cpu);
		
		System.out.println("===== Simulator configuration =====");
		System.out.println("BLOCKSIZE:             " + blockSize);
		System.out.println("L1_SIZE:               " + l1Size);
		System.out.println("L1_ASSOC:              " + l1Assoc);
		System.out.println("L2_SIZE:               " + l2Size);
		System.out.println("L2_ASSOC:              " + l2Assoc);
		System.out.println("REPLACEMENT POLICY:    " + (replacementPolicy == 0 ? "LRU" : (replacementPolicy == 1 ? "Pseudo-LRU" : "Optimal")));
		System.out.println("INCLUSION PROPERTY:    " + (inclusionProperty == 0 ? "non-inclusive" : "inclusive"));
		System.out.println("trace_file:            " + traceFile);

		System.out.println("===== L1 contents =====");
		for (int i = 0; i < caches[0].sets.length; i++) {
			System.out.print("Set     " + i + ":\t");
			for (CacheBlock cacheBlock : caches[0].sets[i].blocks) {
				System.out.print(Integer.toHexString(cacheBlock.tag) + (cacheBlock.isDirty ? " D" : "  ") + "\t");
			}
			System.out.print("\n");
		}

		if (l2Size != 0) {
			System.out.println("===== L2 contents =====");
			for (int i = 0; i < caches[1].sets.length; i++) {
				System.out.print("Set     " + i + ":\t");
				for (CacheBlock cacheBlock : caches[1].sets[i].blocks) {
					System.out.print(Integer.toHexString(cacheBlock.tag) + (cacheBlock.isDirty ? " D" : "  ") + "\t");
				}
				System.out.print("\n");
			}
		}

		System.out.println("===== Simulation results (raw) =====");
		System.out.println("a. number of L1 reads:        " + caches[0].numberOfReads);
		System.out.println("b. number of L1 read misses:  " + caches[0].numberOfReadMisses);
		System.out.println("c. number of L1 writes:       " + caches[0].numberOfWrites);
		System.out.println("d. number of L1 write misses: " + caches[0].numberOfWriteMisses);
		System.out.println("e. L1 miss rate:              " + String.format("%6f", ((float)(caches[0].numberOfReadMisses + caches[0].numberOfWriteMisses) / (caches[0].numberOfReads + caches[0].numberOfWrites))));
		System.out.println("f. number of L1 writebacks:   " + caches[0].numberOfWritebacks);
		System.out.println("g. number of L2 reads:        " + (l2Size == 0 ? 0 : caches[1].numberOfReads));
		System.out.println("h. number of L2 read misses:  " + (l2Size == 0 ? 0 : caches[1].numberOfReadMisses));
		System.out.println("i. number of L2 writes:       " + (l2Size == 0 ? 0 : caches[1].numberOfWrites));
		System.out.println("j. number of L2 write misses: " + (l2Size == 0 ? 0 : caches[1].numberOfWriteMisses));
		System.out.println("k. L2 miss rate:              " + (l2Size == 0 ? "0" : String.format("%6f", ((float)caches[1].numberOfReadMisses / caches[1].numberOfReads))));
		System.out.println("l. number of L2 writebacks:   " + (l2Size == 0 ? 0 : caches[1].numberOfWritebacks));
		System.out.println("m. total memory traffic:      " + (caches[cacheLevels - 1].numberOfReadMisses + caches[cacheLevels - 1].numberOfWriteMisses + caches[cacheLevels - 1].numberOfWritebacks + caches[0].directMainMemoryTraffic ));
	}

	private void linkMemoryHierarchyLevels(CPU cpu, MainMemory mainMemory, int cacheLevels, Cache[] caches) {
		// create appropriate links to previous and next level caches
		for (int i = 0; i < cacheLevels; i++) {
			if (i < cacheLevels - 1) {
				caches[i].nextLevel = caches[i + 1];
			}
			if (i > 0) {
				caches[i].previousLevel = caches[i - 1];
			}
		}

		// create appropriate links to cpu and main memory
		cpu.nextLevel = caches[0];  // caches[0] is the L1 cache
		caches[0].previousLevel = cpu;
		caches[cacheLevels - 1].nextLevel = mainMemory;
	}

	private void runSimulation(String traceFile, CPU cpu) {
		try (Scanner scanner = new Scanner(new File(traceFile))) {
			while (scanner.hasNext()) {
				String readWriteToken = scanner.next().toLowerCase().replaceAll("[^r,^w]", "");
				String addressStr = scanner.next();
				
				// cpu initiates the read or write to the next level which is level 1 cache
				switch (readWriteToken) {
					case "r":
						cpu.read(tryParseHexToLong(addressStr));
						break;
					case "w":
						// we write the address as data but it can be anything 
						// since we're not actually using the data for any meaningful work
						cpu.write(tryParseHexToLong(addressStr), addressStr);
						break;
				
					default:
						System.out.println("Trace File contains invalid read or write token.");
						System.exit(1); 
						break;
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("Trace File <" + traceFile + "> not found.");
			System.exit(1); 
		}
	}

	// convert string to integer. If conversion fails end the program with error message
	int tryParseInt(String str) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			System.out.println("Input argument <" + str + "> is not in a valid format.");
			System.exit(1); 
			return 0;
		}
	}


	// convert string to long. If conversion fails end the program with error message
	long tryParseHexToLong(String str) {
		try {
			return Long.parseLong(str, 16);
		} catch (NumberFormatException e) {
			System.out.println("Trace input contains invalid address <" + str + ">.");
			System.exit(1); 
			return 0;
		}
	}

}