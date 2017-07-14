/**
 * runs the XPath query
 * @param xmlString
 * @param ruleTableI
 */
function runXPathQuery(xmlString, ruleTableI) {

    let parser = new DOMParser();
    let xml = parser.parseFromString(xmlString, "text/xml");
    let quantifierResult = [];
    let conditionedResult = [];

    function nsResolver(prefix) {
        let ns = {'src': 'http://www.srcML.org/srcML/src'};
        return ns[prefix] || null;
    }

    // checks validity of the XML
    if (!xml.evaluate) {
        console.log('error in xml.evaluate');
        return;
    }


    // run xpath queries
    let quantifierNodes = xml.evaluate(ruleTableI.quantifierXpath, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let quantifierNameNodes = xml.evaluate(ruleTableI.quantifierXpathName, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let resultQNode = quantifierNodes.iterateNext();
    let resultQNameNode = quantifierNameNodes.iterateNext();
    let index = 0;
    while (resultQNode) {
        let xmlAndText = getXmlData(xml, ruleTableI.quantifierXpath, index);
        quantifierResult.push({
            "result": new XMLSerializer().serializeToString(resultQNode),
            "xml": xmlAndText.xml,
            "xmlText": xmlAndText.xmlText,
            "name": resultQNameNode ? new XMLSerializer().serializeToString(resultQNameNode) : "error in xpath",
            "snippet": xmlAndText.snippet
        });
        resultQNode = quantifierNodes.iterateNext();
        resultQNameNode = quantifierNameNodes.iterateNext();
        index += 1;
    }

    let conditionedNodes = xml.evaluate(ruleTableI.conditionedXpath, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let conditionedNameNodes = xml.evaluate(ruleTableI.conditionedXpathName, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let resultCNode = conditionedNodes.iterateNext();
    let resultCNameNode = conditionedNameNodes.iterateNext();
    index = 0;
    while (resultCNode) {
        let xmlAndText = getXmlData(xml, ruleTableI.conditionedXpath, index);
        conditionedResult.push({
            "result": new XMLSerializer().serializeToString(resultCNode),
            "xml": xmlAndText.xml,
            "xmlText": xmlAndText.xmlText,
            "name": resultCNameNode ? new XMLSerializer().serializeToString(resultCNameNode) : "error in xpath",
            "snippet": xmlAndText.snippet
        });
        resultCNode = conditionedNodes.iterateNext();
        resultCNameNode = conditionedNameNodes.iterateNext();
        index += 1;
    }

    let data = cloneJSON(ruleTableI);
    data['quantifierResult'] = quantifierResult;
    data['conditionedResult'] = conditionedResult;
    let matching = compareResults(quantifierResult, conditionedResult);
    data['satisfied'] = matching;
    data['missing'] = quantifierResult.length - matching;

    displayResult(data);

}

/**
 * compare the quantifier and the result
 * @param quantifierResult
 * @param conditionedResult
 */
function compareResults(quantifierResult, conditionedResult) {

    let quantifierNames = quantifierResult.map(function (d) {
        return d["name"];
    });
    let conditionedNames = conditionedResult.map(function (d) {
        return d["name"];
    });

    return countMatchingInArray(quantifierNames, conditionedNames);
}


/**
 * remove the following nodes.The resulting xml is sent to the server to be processed by srcML
 * and find the line number.
 * @param mainXml
 * @param query
 * @param index
 */
function getXmlData(mainXml, query, index) {

    // passing the nodes and working with that changes the main XML
    // and produces error for next nodes in the same query.

    let xml = cloneXML(mainXml);

    function nsResolver(prefix) {
        let ns = {'src': 'http://www.srcML.org/srcML/src'};
        return ns[prefix] || null;
    }

    let nodes = xml.evaluate(query, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let res = nodes.iterateNext();
    let i = 0;
    while (i < index) {
        res = nodes.iterateNext();
        i += 1;
    }

    // get the first two line
    let resTextArray = new XMLSerializer().serializeToString(res).split(/\r?\n/);
    let resText = resTextArray.length > 1 ? resTextArray[0] + '\n' + resTextArray[1] : resTextArray[0];

    /**
     * remove first node sib, sib, parent sib, grandparent sib, grand-grandparent sib, ... <- recursive
     * @param node
     * @returns {*}
     */
    function removeSib(node) {
        if (node.nodeName === 'unit')
            return node;
        let sib = node.nextSibling;
        while (sib && sib.nodeType !== -1) {
            node.parentNode.removeChild(sib);
            sib = node.nextSibling;
        }
        return removeSib(node.parentNode);
    }

    let par, nameIndex;
    for (nameIndex = 0; nameIndex < res.children.length; nameIndex++)
        if (res.children[nameIndex].tagName.toString() === 'name') {
            break;
        }

    if (res.firstChild && res.firstChild.nodeType !== -1 && nameIndex !== -1)
        par = removeSib(res.children[nameIndex]);
    else
        par = removeSib(res.nextSibling);

    let fileName = par.getAttribute("filename");
    let temp = new XMLSerializer().serializeToString(par);
    return {
        'xml': "{\"fileName\":\"" + (fileName.replace(/['"]+/g, '\\"')) + "\" ,\"xml\":\""
        + (temp.replace(/['"]+/g, '\\"')) + "\"}",
        'xmlText': new XMLSerializer().serializeToString(par),
        'snippet': resText
    };

}
