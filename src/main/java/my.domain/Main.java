package my.domain;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class Main {
	public static void main(String[] args) throws Exception {
		Path p = Paths.get(args[0]);
		String className = p.getFileName().toString();

		className = className.substring(0, 1).toUpperCase() + className.substring(1);

		if(className.contains(".")) {
			className = className.substring(0, className.indexOf("."));
		}

		List<String> output = new Compiler().compile(Files.readAllLines(p), className);

		Path outputFile = p.resolveSibling(className + ".java");

		Files.write(outputFile, output, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
	}
}
