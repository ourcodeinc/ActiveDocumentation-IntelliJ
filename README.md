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


## ruleJson.txt and tagJson.txt

There must be two files named `ruleJson.txt` and `tagJson.txt` in the project folder. The example for these files can be found in `README.md` in **active-doc-client** repository.
