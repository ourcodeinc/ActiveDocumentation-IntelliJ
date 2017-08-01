/**
 * Created by saharmehrpour.
 */

/**
 * runs the XPath query and then call the function to display them
 * @param xmlFiles
 * @param rules
 * @param ruleIndex
 */
function runXPathQuery(xmlFiles, rules, ruleIndex) {
    let parser = new DOMParser();
    let quantifierResult = [];
    let conditionedResult = [];
    let rulesI = rules.filter((d) => {
        return d.index === ruleIndex
    })[0];

    function nsResolver(prefix) {
        let ns = {'src': 'http://www.srcML.org/srcML/src'};
        return ns[prefix] || null;
    }

    for (let j = 0; j < xmlFiles.length; j++) {

        // printLog("file " + j);

        // checks validity of the XML
        let xml = parser.parseFromString(xmlFiles[j].xml, "text/xml");
        if (!xml.evaluate) {
            console.log('error in xml.evaluate');
            return;
        }

        // run xpath queries
        let quantifierNodes = xml.evaluate(rulesI.quantifierXpath, xml, nsResolver, XPathResult.ANY_TYPE, null);
        let quantifierNameNodes = xml.evaluate(rulesI.quantifierXpathName, xml, nsResolver, XPathResult.ANY_TYPE, null);
        let resultQNode = quantifierNodes.iterateNext();
        let resultQNameNode = quantifierNameNodes.iterateNext();
        let index = 0;
        while (resultQNode) {
            let xmlAndText = getXmlData(xml, rulesI.quantifierXpath, index);
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

        let conditionedNodes = xml.evaluate(rulesI.conditionedXpath, xml, nsResolver, XPathResult.ANY_TYPE, null);
        let conditionedNameNodes = xml.evaluate(rulesI.conditionedXpathName, xml, nsResolver, XPathResult.ANY_TYPE, null);
        let resultCNode = conditionedNodes.iterateNext();
        let resultCNameNode = conditionedNameNodes.iterateNext();
        index = 0;
        while (resultCNode) {
            let xmlAndText = getXmlData(xml, rulesI.conditionedXpath, index);
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
    }

    rulesI['quantifierResult'] = quantifierResult;
    rulesI['conditionedResult'] = conditionedResult;
    let matching = compareResults(quantifierResult, conditionedResult);
    rulesI['satisfied'] = matching;
    rulesI['missing'] = quantifierResult.length - matching;

    return rules;

}


/**
 * re-run the xpath queries and detect changes.
 * @param xmlString
 * @param rules
 * @param ruleIndex
 */
function checkRules(xmlString, rules, ruleIndex) {
    let rulesI = rules.filter((d) => {
        return d.index === ruleIndex
    })[0];

    let prevQuantifierResult = rulesI['quantifierResult'].slice(0);
    let prevConditionedResult = rulesI['conditionedResult'].slice(0);
    let prevMatching = rulesI['satisfied'];
    let prevMissing = rulesI['missing'];

    rules = runXPathQuery(xmlString, rules, ruleIndex);

    rulesI = rules.filter((d) => {
        return d.index === ruleIndex
    })[0];

    rulesI['changed'] = (!ResultArraysEqual(prevQuantifierResult, rulesI['quantifierResult']) ||
    !ResultArraysEqual(prevConditionedResult, rulesI['conditionedResult']) ||
    prevMatching !== rulesI['satisfied'] ||
    prevMissing !== rulesI['missing']);

    rulesI['missingChanged'] = prevMissing !== rulesI['missing'];
    rulesI['satisfiedChanged'] = prevMatching !== rulesI['satisfied'];
    rulesI['allChanged'] = (prevMatching + prevMissing) !== (rulesI['missing'] + rulesI['satisfied']);

    return rules;
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

    if (res.firstChild && res.firstChild.nodeType !== -1 && nameIndex !== -1 && nameIndex !== res.children.length)
        par = removeSib(res.children[nameIndex]);
    else if (res.nextSibling)
        par = removeSib(res.nextSibling);
    else
        par = res;


    let fileName = par.getAttribute("filename");
    let temp = new XMLSerializer().serializeToString(par);
    return {
        'xml': "{\"fileName\":\"" + (fileName.replace(/['"]+/g, '\\"')) + "\" ,\"xml\":\""
        + (temp.replace(/['"]+/g, '\\"')) + "\"}",
        'xmlText': new XMLSerializer().serializeToString(par),
        'snippet': resText
    };

}
