# Active Documentation

This tool is an [IntelliJ IDEA](https://www.jetbrains.com/idea/) plugin. 

## Difference with the original version of the plugin

In this version, the core of the plugin is based on [SRCML](http://www.srcml.org/). In essence, the source-code of the project is encoded in an XML file. This XML file is then processed via XPath queries for different rules. The main XML file is a variable in the plugin code and is not accessible out of it. However, the temporary process results are written in files in the project folder. Removing these files are safe.


## ruleJson.txt and tagJson.txt

There must be two files named `ruleJson.txt` and `tagJson.txt` in the project folder. The example for these files can be found in `README.md` in **active-doc-client** repository.
