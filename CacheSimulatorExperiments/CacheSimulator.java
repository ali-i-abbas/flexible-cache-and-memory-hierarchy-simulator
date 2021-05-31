import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

class CacheSimulator {
	public CacheSimulator() throws java.io.IOException {

		FileWriter statsOutput = new FileWriter("stats.csv");
		statsOutput.write("l1Size,l1Assoc,l2Size,replacement,inclusion,MRL1,MRL2\n");

		// # bits of address
		int addressBitsSize = 32;

		int blockSize = 32;
		String traceFile = "gcc_trace.txt";

		// graph 1 and 2
		// int[] l1SizeArray = new int[11];
		// for (int i = 0; i < l1SizeArray.length; i++) {
		// 	l1SizeArray[i] = (int) Math.pow(2, 10 + i);
		// }
		// int[] l1AssocArray = new int[] { 1, 2, 4, 8, -1 };
		// int[] l2SizeArray = new int[] { 0 };
		// int l2Assoc = 0;
		// int[] replacementPolicyArray = new int[] { 0 };
		// int[] inclusionPropertyArray = new int[] { 0 };

		// graph 3
		// int[] l1SizeArray = new int[9];
		// for (int i = 0; i < l1SizeArray.length; i++) {
		// 	l1SizeArray[i] = (int) Math.pow(2, 10 + i);
		// }
		// int[] l1AssocArray = new int[] { 4 };
		// int[] l2SizeArray = new int[] { 0 };
		// int l2Assoc = 0;
		// int[] replacementPolicyArray = new int[] { 0, 1, 2 };
		// int[] inclusionPropertyArray = new int[] { 0 };

		// graph 4
		int[] l1SizeArray = new int[] { 1024 };
		int[] l1AssocArray = new int[] { 4 };		
		int[] l2SizeArray = new int[6];
		for (int i = 0; i < l2SizeArray.length; i++) {
			l2SizeArray[i] = (int) Math.pow(2, 11 + i);
		}
		int l2Assoc = 8;
		int[] replacementPolicyArray = new int[] { 0 };
		int[] inclusionPropertyArray = new int[] { 0, 1 };

		for (int inclusionProperty : inclusionPropertyArray) {
			for (int replacementPolicy : replacementPolicyArray) {
				for (int l1Assoc : l1AssocArray) {
					for (int l1Size : l1SizeArray) {
						for (int l2Size : l2SizeArray) {
							simulate(statsOutput, addressBitsSize, blockSize, l1Size, l1Assoc, l2Size, l2Assoc,
									replacementPolicy, inclusionProperty, traceFile);
						}
					}
				}
			}
		}

		statsOutput.close();
	}

	private void simulate(FileWriter statsOutput, int addressBitsSize, int blockSize, int l1Size, int l1Assoc,
			int l2Size, int l2Assoc, int replacementPolicy, int inclusionProperty, String traceFile)
			throws IOException {
		CPU cpu = new CPU();
		MainMemory mainMemory = new MainMemory();

		// fully associative
		if (l1Assoc == -1) {
			l1Assoc = l1Size / blockSize;
		}

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
			// ArrayList<Integer> a = caches[0].sets[16].trace;
			// ArrayList<Integer> b = caches[1].sets[48].trace;
		}

		runSimulation(traceFile, cpu);
		
		float MRL1 = ((float)(caches[0].numberOfReadMisses + caches[0].numberOfWriteMisses) / (caches[0].numberOfReads + caches[0].numberOfWrites));
		float MRL2 = (l2Size == 0 ? 0 : (float)caches[1].numberOfReadMisses / caches[1].numberOfReads);
		statsOutput.write(l1Size + "," + l1Assoc + "," + l2Size + "," + replacementPolicy + "," + inclusionProperty + "," + MRL1 + "," + MRL2 + "\n");

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
				//System.out.println("<" + readWriteToken + "> : <" + addressStr + ">");


				//debug
				// if (getAddressIndex(tryParseHexToLong(addressStr)) == 127) {
				// 	System.out.println(readWriteToken + " " + addressStr);
				// }

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