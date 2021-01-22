package my.domain;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class Stackoverflow {
	private static final String SEARCH_API = "https://api.stackexchange.com/2.2/similar?pageSize=30&page=%s&order=desc&sort=relevance&tagged=java&title=%s&site=stackoverflow&filter=!-MOiN_e9MJYANNGoGf9VS-AlBqHSUbO)y";

	public static class Parameter {
		private final String name;
		private final String type;

		public Parameter(String name, String type) {
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

	public static class FoundMethod {
		private final List<String> bodyCode;
		private final String name;
		private final String type;
		private final String sourceLink;
		private final List<Parameter> parameters;
		private final List<String> imports;

		public FoundMethod(List<String> bodyCode, String name, String type, String sourceLink, List<Parameter> parameters, List<String> imports) {
			this.bodyCode = bodyCode;
			this.name = name;
			this.type = type;
			this.sourceLink = sourceLink;
			this.parameters = parameters;
			this.imports = imports;
			if(!parameters.isEmpty()) {
				Parameter p = parameters.get(parameters.size() - 1);
				String s = p.getType();
				if(s.endsWith("[]")) {
					parameters.set(parameters.size() - 1, new Parameter(p.getName(), s.replace("[]", "") + "..."));
				}
			}
		}

		public List<String> getBodyCode() {
			return bodyCode;
		}

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}

		public List<Parameter> getParameters() {
			return parameters;
		}

		public List<String> getImports() {
			return imports;
		}

		public String getSourceLink() {
			return sourceLink;
		}
	}

	public static class Search {
		private final String searchText;
		private final List<FoundMethod> methods = new ArrayList<>();
		private int currentSearchPage = 0;
		private boolean keepSearching = true;

		public Search(String searchText) {
			this.searchText = searchText;
		}

		private void search() {
			currentSearchPage++;
			System.out.println("Searching for \"" + searchText + "\", page " + currentSearchPage);
			Gson gson = new Gson();
			JsonObject object;
			try {
				HttpsURLConnection connection = (HttpsURLConnection)new URL(String.format(SEARCH_API, currentSearchPage, URLEncoder.encode(searchText, StandardCharsets.UTF_8))).openConnection();
				try(InputStreamReader reader = new InputStreamReader(new GZIPInputStream(connection.getInputStream()))) {
					object = gson.fromJson(reader, JsonObject.class);
				}
			} catch(Exception e) {
				throw new RuntimeException(e); //lazy
			}

			if(object.get("items").getAsJsonArray().size() == 0) {
				keepSearching = false;
				return;
			}

			//this is awful. i'm so, so sorry.
			for(JsonElement element : object.get("items").getAsJsonArray()) {
				JsonObject object1 = element.getAsJsonObject();

				if(object1.has("answers"))
					for(JsonElement answer : object1.get("answers").getAsJsonArray()) {
						JsonObject answerO = answer.getAsJsonObject();
						try {
							String html = answerO.get("body").getAsString();
							Document d = Jsoup.parse(html);

							for(Element code : d.getElementsByTag("code")) {
								String text = code.text();


								List<String> currentMethod = new ArrayList<>();
								List<String> imports = new ArrayList<>();
								int depth = 0;

								fromCodeLoop:
								for(String s : text.split("\n")) {
									s = s.trim();
									if(depth > 0) {
										int i = 1;
										if(s.equals("}")) {
											i = 2;
										}
										StringBuilder sBuilder = new StringBuilder(s);
										for(; i < depth; i++) {
											sBuilder.insert(0, "\t");
										}
										s = sBuilder.toString();
									}

									if(s.contains("this") && !s.startsWith("//")) {
										break;
									}

									// 10 out of 10 parsing !!!
									if(s.contains("public") && s.contains(")") && s.contains("{")) {
										depth = 1;
									} else if(s.contains("{") && depth > 0) {
										depth++;
									} else if(s.contains("}")) {
										depth--;
									}

									if(s.startsWith("import")) {
										imports.add(s);
									}

									if(depth > 0) {
										currentMethod.add(s);
									}


									if(depth == 0 && !currentMethod.isEmpty()) {
										try {
											String declaration = currentMethod.get(0);
											currentMethod.remove(0);

											String methodName;
											{
												String eee = declaration.split("\\(", 2)[0].trim();
												if(eee.contains(" ")) {
													methodName = eee.substring(eee.lastIndexOf(" ")).trim();
												} else {
													methodName = eee.trim();
												}
											}

											String type;
											{
												String eee = declaration.substring(0, declaration.indexOf(methodName)).trim();
												if(eee.contains(" "))
													type = eee.substring(eee.lastIndexOf(" ")).trim();
												else
													break;
											}


											List<Parameter> parameters = new ArrayList<>();

											String parametersString = declaration.substring(declaration.indexOf("(") + 1, declaration.lastIndexOf(")"));

											if(parametersString.length() > 0) {
												for(String parameter : parametersString.split(",")) {
													parameter = parameter.trim();

													String[] eee = parameter.replace("...", "... ").replace("  ", " ").split(" ", 2);
													String paramType = eee[0];
													String paramName = eee[1];

													if(paramName.contains("[]")) {
														paramType = paramType + "[]";
														paramName = paramName.replace("[]", "");
													}

													parameters.add(new Parameter(paramName, paramType));
												}
											}

											for(String s1 : currentMethod) {
												if(s1.equals("...")) {
													break fromCodeLoop;
												}
											}

											if(methodName.equals("main")) {
												boolean usesArgs = false;
												for(String s1 : currentMethod) {
													if(s1.contains("args")) {
														usesArgs = true;
														break;
													}
												}
												if(!usesArgs) {
													parameters.clear();
												}
											}

											FoundMethod method = new FoundMethod(currentMethod, methodName, type, answerO.get("link").getAsString(), parameters, imports);
											methods.add(method);
										} finally {
											currentMethod = new ArrayList<>();
											depth = 0;
										}
									}
								}
							}
						} catch(Exception e) {
						}
					}
			}

			Collections.shuffle(methods);
		}

		public FoundMethod next() {
			if(methods.isEmpty() && keepSearching) {
				search();
			}

			FoundMethod method = methods.get(methods.size() - 1);
			methods.remove(method);
			return method;
		}
	}

	public static List<String> searchForMethod(String name, String command, String type, List<String> parameterTypes) {
		Search search = new Search(command);

		List<String> finalized = new ArrayList<>();
		FoundMethod method;
		try {
			while(!canBeType((method = search.next()).getType(), type) || !parametersApplyToExpected(method.getParameters(), parameterTypes)) ;
		} catch(Exception e) {
			List<String> paramsWithNames = new ArrayList<>();
			for(int i = 0; i < parameterTypes.size(); i++) {
				String parameterType = parameterTypes.get(i);
				paramsWithNames.add(parameterType + " var_" + i);
			}

			String params = String.join(", ", paramsWithNames);

			finalized.add("public static " + type + " " + name + "(" + params + ") {");
			finalized.add("\t//Could not find on StackOverflow!");
			finalized.add("}");
			return finalized;
		}


		String params = method.getParameters().stream().map(p -> p.getType() + " " + p.getName()).collect(Collectors.joining(", "));

		finalized.addAll(method.getImports());
		finalized.add("//" + method.getSourceLink());
		finalized.add("//Original method name: " + method.getName());
		finalized.add("public static " + method.getType() + " " + name + "(" + params + ") {");
		for(String s : method.getBodyCode()) {
			if(!s.isEmpty()) {
				s = s.replace(method.getName() + "(", name + "("); //fix recusrion
				finalized.add("\t" + s);
			}
		}

		finalized.add("}");
		return finalized;
	}

	public static boolean parametersApplyToExpected(List<Parameter> params, List<String> expectedParams) {
		if(params.size() > expectedParams.size())
			return false;

		if(params.size() == expectedParams.size()) {
			boolean bad = false;
			for(int i = 0; i < params.size(); i++) {
				Parameter p = params.get(i);
				if(i >= expectedParams.size())
					bad = true;
				else if(!p.getType().equals(expectedParams.get(i)) && !p.getType().replace("...", "").equals(expectedParams.get(i)))
					bad = true;
				if(bad)
					break;
			}
			return !bad;
		}


		if(params.size() > 0) {
			Parameter lastParam = params.get(params.size() - 1);
			if(lastParam.getType().endsWith("...")) {
				String notArrayType = lastParam.getType().replace("...", "");

				for(int i = params.size() - 1; i < expectedParams.size(); i++) {
					if(!notArrayType.equals(expectedParams.get(i)))
						return false;
				}
				return true;
			}
		}

		return false;
	}


	private static boolean canBeType(String type, String expected) {
		return expected == null || expected.equals("void") || type.equals(expected);
	}
}