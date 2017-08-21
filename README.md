# Active Documentation

This tool is an [IntelliJ IDEA](https://www.jetbrains.com/idea/) plugin. 

To run the plugin:

* Open the project in IntelliJ IDEA. 

* Set the run configuration to `plugin`. The fields must be filled out automatically (The classpath is `IntelliJTestPlugin`)

* Set another run configuration in `JavaScript Debug`. The URL must be set to `chat.html`.

* Run the plugin. A new IDEA window will open. Open some Java project in this window if needed. To run the plugin, find and run it in the `View -> Tool Windows -> ActiveDocs`. 

* Run `chat.html`. It will open a browser. Click on `CONNECT` to connect the client and server via `websocket`.


## Difference with the original version of the plugin

In this version, the core of the plugin is based on [SRCML](http://www.srcml.org/). In essence, the source-code of the project is encoded in an XML file. This XML file is then processed via XPath queries for different rules. The main XML file is a variable in the plugin code and is not accessible out of it. However, the temporary process results are written in files in the project folder. Removing these files are safe.


## ruleJson.txt

There must be a file name `ruleJson.txt` in the project folder. In this file, there exists a JSON object named `ruleTable`. Here is the example for this file:

```
[
    {
		"ruleDescription": "All classes must have a constructors",
		"detail": "No detail.",
		"quantifierXpath": "//src:class",
		"quantifierTitle": "All classes",
		"quantifierXpathName": "//src:class/src:name/text()",
		"conditionedTitle": "Classes with constructors",
		"conditionedXpath": "//src:class[count(src:block/src:constructor)>0]",
		"conditionedXpathName": "//src:class[count(src:block/src:constructor)>0]/src:name/text()",
		"tags": [
			"class",
			"constructor"
		],
		"index": 1
	},
	{
		"ruleDescription": "All @Entity classes must be registered.",
		"detail": "All @Entity classes must register themselves so that they can be used with Objectify. You need the statement ObjectifyService.register(TheEntityClassInQuestion);",
		"quantifierXpath": "//src:class[src:annotation/src:name/text()=\"Entity\"]",
		"quantifierXpathName": "//src:class[src:annotation/src:name/text()=\"Entity\"]/src:name/text()",
		"quantifierTitle": "All @Entity classes",
		"conditionedXpath": "//src:call[src:name//text()=\"register\"]",
		"conditionedXpathName": "//src:call[src:name//text()=\"register\"]/src:argument_list//src:name[position()=1]/text()",
		"conditionedTitle": "Registered classes",
		"tags": [
			"class",
			"annotation",
			"entity",
			"register",
			"function"
		],
		"index": 2
	}
]
```

The format of this file is not final.

## tagJson.txt

There is also another json file named `tagJson.txt`. In this file we store information
about tags. Here is an example for this file:

```
[
	{
		"tagName": "class",
		"detail": "detail about tag 'class'"
	},
	{
		"tagName": "constructor",
		"detail": "detail about tag 'constructor'"
	}
```

The format of this file is not final.
