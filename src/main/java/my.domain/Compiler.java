package my.domain;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Compiler {
	public static class StackMethod {
		private final String search;
		private final String returnType;
		private final List<String> parameterTypes;

		public StackMethod(String search, String returnType, List<String> parameterTypes) {
			this.search = search;
			this.returnType = returnType;
			this.parameterTypes = parameterTypes;
		}

		public String getReturnType() {
			return returnType;
		}

		public String getSearch() {
			return search;
		}

		public List<String> getParameterTypes() {
			return parameterTypes;
		}
	}

	public static class Value {
		public String getValue() {
			return value;
		}

		public String getType() {
			return type;
		}

		private final String value;
		private final String type;

		public Value(String value, String type) {
			this.value = value;
			this.type = type;
		}
	}

	public static class Variable {
		private final String name;
		private final String type;

		public Variable(String name, String type) {
			this.name = name;
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}
	}

	Map<Integer, StackMethod> requiredMethods = new HashMap<>();
	List<String> imports = new ArrayList<>();
	List<String> mainCode = new ArrayList<>();
	List<Variable> variables = new ArrayList<>();

	public List<String> compile(List<String> lines, String className) throws Exception {
		for(int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
			String line = lines.get(lineNumber);
			line = line.trim();

			if(line.startsWith("//"))
				continue;

			if(line.startsWith("import")) {
				imports.add(line + ";");
			} else {
				if(line.contains("=")) {
					String[] data = line.split("=", 2);
					String assignee = data[0].trim();
					String assignment = data[1].trim();

					Variable v = null;
					if(assignee.contains(" ")) {
						String[] moreData = assignee.split(" ", 2);
						String name = moreData[1];
						String type = moreData[0];
						for(Variable variable : variables) {
							if(variable.getName().equals(name)) {
								die("Variable is already defined", lineNumber);
							}
						}

						if(name.equals("")) {
							die("Variable name is required", lineNumber);
						}

						v = new Variable(name, type);
						variables.add(v);
					} else {
						for(Variable variable : variables) {
							if(variable.getName().equals(assignee)) {
								v = variable;
								break;
							}
						}
						if(v == null) {
							die("Cannot find variable", lineNumber);
						}
					}

					mainCode.add(assignee + " = " + parseValue(assignment, v.getType()).getValue() + ";");
				} else if(line.isEmpty()) {
					mainCode.add("");
				} else {
					mainCode.add(parseMethodCall(line, null) + ";");
				}
			}
		}

		List<String> finalized = new ArrayList<>();
		finalized.add("public class " + className + " {");
		finalized.add("\tpublic static void main(String[] args) {");
		for(String s : mainCode) {
			finalized.add("\t\t" + s);
		}
		finalized.add("\t}");
		for(Map.Entry<Integer, StackMethod> entry : requiredMethods.entrySet()) {
			finalized.add("");


			StackMethod method = entry.getValue();
			String params = String.join(", ", method.getParameterTypes());
			String methodType = method.getReturnType() == null ? "void" : method.getReturnType();

			finalized.add("\t//" + method.getSearch());



			List<String> strings = Stackoverflow.searchForMethod("method_" + entry.getKey(), method.getSearch(), methodType, method.getParameterTypes());

			for(String s : strings) {
				if(s.startsWith("import")) {
					imports.add(s);
				} else {
					finalized.add("\t" + s);
				}
			}


//			finalized.add("\tpublic static " + methodType + " method_" + entry.getKey() + "(" + params + ") {}");
		}
		finalized.add("}");

		if(imports.size() > 0) {
			List<String> newList = new ArrayList<>(imports);
			newList.add("");
			newList.addAll(finalized);
			return newList;
		}

		return finalized;
	}

	private void die(String error, int lineNumber) throws Exception {
		throw new Exception(error + " (line " + lineNumber + ")");
	}

	private Value parseValue(String s, String type) {
		s = s.trim();
		if(s.startsWith("\"") && s.endsWith("\"") && canBeType("String", type)) {
			return new Value(s, "String");
		} else if(isInt(s) && canBeType("int", type)) {
			return new Value(s, "int");
		} else if(isDouble(s) && canBeType("double", type)) {
			return new Value(s, "double");
		} else {
			for(Variable variable : variables) {
				if(variable.name.equals(s)) {
					return new Value(s, variable.type);
				}
			}

			return new Value(parseMethodCall(s, type), type);
		}
	}

	private boolean canBeType(String type, String expected) {
		return expected == null || type.equals(expected);
	}

	private String parseMethodCall(String toParse, String expectedType) {
		List<Value> parameters = new ArrayList<>();
		if(toParse.contains("<")) {
			String[] data = toParse.split("<", 2);
			toParse = data[0].trim();
			for(String s : data[1].split(",")) {
				s = s.trim();
				parameters.add(parseValue(s, null));
			}
		}

		int num = requiredMethods.size();

		requiredMethods.put(num, new StackMethod(toParse, expectedType, parameters.stream().map(Value::getType).collect(Collectors.toList())));

		String methodName = "method_" + num;
		return methodName + "(" + parameters.stream().map(Value::getValue).collect(Collectors.joining(", ")) + ")";
	}

	private boolean isInt(String s) {
		try {
			Integer.parseInt(s);
		} catch(NumberFormatException e) {
			return false;
		}
		return true;
	}

	private boolean isDouble(String s) {
		try {
			Double.parseDouble(s);
		} catch(NumberFormatException e) {
			return false;
		}
		return true;
	}
}
