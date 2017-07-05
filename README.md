# Active Documentation

This tool is an [IntelliJ IDEA](https://www.jetbrains.com/idea/) plugin. 

To run the plugin:

* Open the project in IntelliJ IDEA. 

* Set the run configuration to `plugin`. The fields must be filled out automatically (The classpath is `IntelliJTestPlugin`)

* Set another run configuration in `JavaScript Debug`. The URL must be set to `chat.html`.

* Run the plugin. A new IDEA window will open. Open some Java project in this window if needed. To run the plugin, find and run it in the `View -> Tool Windows -> ActiveDocs`. 

* Run `chat.html`. It will open a browser. Click on `CONNECt` to connect the client and server via `websocket`.


## Difference with the original version of the plugin

In this version, the core of the plugin is based on [SRCML](http://www.srcml.org/). In essence, the source-code of the project is encoded in an xml file. This XML file is then processed via XPath queries for different rules. The main XML file is a variable in the plugin code and is not accessible out of it. However, the temporary proccess results are written in files in the project folder. Removing these files are safe.


## ruleJson.txt

There must be a file name `ruleJson.txt`. In this file there exists a Json object named `ruleTable`. Here is the example for this file:

```
ruleTable=
[
	{
		"ruleDescription": "All classes must have a constructor",
		"detail": "No detail!",
		"verification": "count",
		"initialGroup": "//src:class",
		"countInitialGroup": "count(//src:class)",
		"titleInitialGroup": "All classes",
		"conditionedGroup": "//src:class[count(src:block/src:constructor)>0]",
		"countConditionedGroup": "count(//src:class[count(src:block/src:constructor)>0])",
		"titleConditionedGroup": "Classes with constructors"
	},
	{
		"ruleDescription": "All @Entity classes must be registered.",
		"detail": "All @Entity classes must register themselves so that they can be used with Objectify. You need the statement ObjectifyService.register(TheEntityClassInQuestion);",
		"verification": "array",
		"initialGroup": "//src:class[src:annotation/src:name/text()=\"Entity\"]",
		"arrayInitialGroup": "//src:class[src:annotation/src:name/text()=\"Entity\"]/src:name/text()",
		"titleInitialGroup": "All @Entity classes",
		"conditionedGroup": "//src:call[src:name//text()=\"register\"]",
		"arrayConditionedGroup": "//src:call[src:name//text()=\"register\"]/src:argument_list//src:name[position()=1]/text()",
		"titleConditionedGroup": "Registered classes"
	}
]
```

The format of this file is not final. But in the current format, ther exists an attribute `verification` which is either `count` or `array`. If the vlue is `count`, there must be two variables `countInitialGroup` and `countConditionedGroup`. Otherwise, arrtibutes `arrayInitialGroup` and `arrayConditionedGroup` must exist. The reason is that for some rules, one XPath query is not suufient as the logic of the rule consists of two parts (or more); searching and verifying.
