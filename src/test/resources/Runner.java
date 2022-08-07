class Runner {
	private static String key = "key";

	public static void main(String[] args) throws Exception {
		while (true) {
			String v = getProperty(key);
			if (v == null) {
				handleNoProperty();
			} else {
				handleWithProperty(v);
			}
			Thread.sleep(1000);
		}
	}
	
	private static void handleWithProperty(String v) {
		System.out.println("Property = " + v);
	}

	private static void handleNoProperty() {
		System.out.println("No property");
	}

	private static String getProperty(String key) {
		return System.getProperty(key);
	}
}