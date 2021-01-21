# stacklang
Search for stuff on StackOverflow to (maybe) generate poorly written Java source code, heavily inspired by [stacksort](https://github.com/gkoberger/stacksort).
This repo's source code is also poorly written. Don't look at it too hard.

## How does it work?
Stacklang finds every search it needs to make, and creates a new method for them. It figures out what types the parameters are, and what the return type should be. Then, it searches for it on StackOverflow, goes through the code of every answer, and tries to find a method that matches.

Oh, also, the methods it finds are randomized. So, continuing the chaotic trend, compilations are random. It was done like this because half the time the first result isn't even what you wanted. You can just compile again.
## What does the code look like?
Awful.
### Types
The only types supported by doing literals are Strings, ints, and doubles. Everything else must be declared as a variable type. It can technically support any class type, but I don't recommend getting too fancy as it's unlikely to find a method for anything obscure.

### Variables
Variables are initialized and assigned similar to in Java.
```
int i = 0
double d = 6.87
String hello = "hello"
```

### Method calls
This is where it starts getting wacky. Remember, every method is a search on StackOverflow.
```
do a thing < "parameter"
```
This would be like `doAThing("parameter")` in Java. It will search StackOverflow for "do a thing". Using multiple parameters is like this:
```
some method < "parameter 1", "parameter 2"
```
(Warning! Don't put commas in strings because I'm too lazy to fix it lol)

You can also have method calls that return something.
```
int x = do some random thing
```

You *might* be able to do multiple method calls on the same line. It probably won't work, but, you can try!

### Example
Here's code to find the average of some numbers, then print it:
```
double average = find the average of numbers < 10, 100, 2, 5, 0
String string = convert double to string < average
print something to console < string
```
(python's got some competition!)

I was able to compile this to the Java code:
```java
public class FindAverage {
	public static void main(String[] args) {
		double average = method_0(10, 100, 2, 5, 0);
		String string = method_1(average);
		method_2(string);
	}

	//find the average of numbers
	//https://stackoverflow.com/questions/12002332/how-to-manipulate-arrays-find-the-average-beginner-java/12003793#12003793
	//Original method name: average
	public static double method_0(int... data) {
		int sum = 0;
		double average;
		for(int i=0; i < data.length; i++){
			sum = sum + data[i];
		}
		average = (double)sum/data.length;
		return average;
	}

	//convert double to string
	//https://stackoverflow.com/questions/3678008/double-to-string-formatting/3678580#3678580
	//Original method name: format
	public static String method_1(double number) {
		DecimalFormat formatter = new DecimalFormat("#");
		formatter.setRoundingMode(RoundingMode.DOWN);
		number *= number < 0.0 ? 100 : 1000;
		String result = formatter.method_1(number);
		return result;
	}

	//print something to console
	//https://stackoverflow.com/questions/49932547/how-to-print-something-from-another-class/49932620#49932620
	//Original method name: Add
	public static int method_2(String... args) {
		int a = Integer.parseInt(args[0]);
		int b = Integer.parseInt(args[1]);
		int c = a+b;
		return c;
	}
}
```

As you can see, it *kinda* works, but will easily do something stupid. You may also get different results trying yourself.

You can find more examples in the examples folder.

## How do I use this?
Run the program with the name of your file as the first argument. The output will be a Java file of the same name (capitalized, if it isn't).

While there probably isn't a huge risk here (probably the worst that would happen is that the code doesn't work), please do look at the code before you run it. I hope that's obvious by now.

## Building
Stacklang uses Gson for parsing StackOverflow API requests, and Jsoup for parsing the HTML of the answers. It should automatically do everything with Maven, though, I just build through my IDE.

## Contributing
Please don't. I mean, I would accept PRs if you REALLY want to work on this for some reason, but *why*.

## Practical Use-Cases?
Right now? None. Maybe at some point it'd be interesting to have an IntelliJ plugin that auto-generates methods with StackOverflow requests similar to this, but it'd have to be rewritten.
