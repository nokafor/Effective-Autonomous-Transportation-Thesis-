import java.util.*;

public class Practice {
	public static void main(String[] args) {
		List<String> test = new ArrayList<String>();
		test.add("Hello");
		test.add("Goodbye");

		String hello = "Hello";
		// String s = test.get(test.indexOf(hello));
		// s += " and Goodbye";

		// test.remove(hello);
		// test.add(s);

		Iterator<String> iter = test.iterator();
		while (iter.hasNext()) {
			String s = iter.next();
			if (s.equals(hello)) {
				iter.remove();
				// System.out.println(s);
			}
		}

		for (String string : test)
			System.out.println(string);
	}
}