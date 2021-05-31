import java.io.IOException;

class sim_cache {
	public static void main(String[] args) {
		try {
			new CacheSimulator();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
