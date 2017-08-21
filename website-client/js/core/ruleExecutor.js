/**
 * Created by saharmehrpour.
 */

/**
 * verify the rules for all xml files
 * @param xmlFiles
 * @param ruleTable
 */
function verifyRules(xmlFiles, ruleTable) {
    for (let i = 0; i < ruleTable.length; i++) {
        for (let j = 0; j < xmlFiles.length; j++)
            ruleTable[i] = runXPathQuery(xmlFiles[j], ruleTable[i]);
    }
    return ruleTable;
}


/**
 * runs the XPath query and then call the function to display them
 * @param xmlFile
 * @param ruleI
 */
function runXPathQuery(xmlFile, ruleI) {
    let parser = new DOMParser();
    let quantifierResult = [];
    let conditionedResult = [];

    function nsResolver(prefix) {
        let ns = {'src': 'http://www.srcML.org/srcML/src'};
        return ns[prefix] || null;
    }

    // checks validity of the XML
    let xml = parser.parseFromString(xmlFile['xml'], "text/xml");
    if (!xml.evaluate) {
        console.log('error in xml.evaluate');
        return;
    }

    // run xpath queries
    let quantifierNodes = xml.evaluate(ruleI.quantifierXpath, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let quantifierNameNodes = xml.evaluate(ruleI.quantifierXpathName, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let resultQNode = quantifierNodes.iterateNext();
    let resultQNameNode = quantifierNameNodes.iterateNext();
    let index = 0;
    while (resultQNode) {
        let xmlAndText = getXmlData(xml, ruleI.quantifierXpath, index);
        quantifierResult.push({
            "filePath": xmlFile['filePath'],
            "result": new XMLSerializer().serializeToString(resultQNode),
            "xml": xmlAndText.xmlJson,
            "xmlText": xmlAndText.xmlText,
            "name": resultQNameNode ? new XMLSerializer().serializeToString(resultQNameNode) : "error in xpath",
            "snippet": xmlAndText.snippet
        });
        resultQNode = quantifierNodes.iterateNext();
        resultQNameNode = quantifierNameNodes.iterateNext();
        index += 1;
    }

    let conditionedNodes = xml.evaluate(ruleI.conditionedXpath, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let conditionedNameNodes = xml.evaluate(ruleI.conditionedXpathName, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let resultCNode = conditionedNodes.iterateNext();
    let resultCNameNode = conditionedNameNodes.iterateNext();
    index = 0;
    while (resultCNode) {
        let xmlAndText = getXmlData(xml, ruleI.conditionedXpath, index);
        conditionedResult.push({
            "filePath": xmlFile['filePath'],
            "result": new XMLSerializer().serializeToString(resultCNode),
            "xml": xmlAndText.xmlJson,
            "xmlText": xmlAndText.xmlText,
            "name": resultCNameNode ? new XMLSerializer().serializeToString(resultCNameNode) : "error in xpath",
            "snippet": xmlAndText.snippet
        });
        resultCNode = conditionedNodes.iterateNext();
        resultCNameNode = conditionedNameNodes.iterateNext();
        index += 1;
    }

    let matching = compareResults(quantifierResult, conditionedResult);

    let resultData = {
        'quantifierResult': quantifierResult,
        'conditionedResult': conditionedResult,
        'satisfied': matching,
        'missing': quantifierResult.length - matching
    };

    if (!ruleI.hasOwnProperty('xPathQueryResult'))
        ruleI['xPathQueryResult'] = [];

    let resultArray = ruleI['xPathQueryResult'].filter((d) => {
        return d['filePath'] === xmlFile['filePath']
    });

    if (resultArray.length === 0)
        ruleI['xPathQueryResult'].push({'filePath': xmlFile['filePath'], 'data': resultData});
    else // TODO error prone! check this later!
        ruleI['xPathQueryResult'].filter((d) => {
            return d['filePath'] === xmlFile['filePath']
        })[0]['data'] = resultData;

    return ruleI;

}


/**
 * re-run the xpath queries and detect changes for one file.
 * @param xmlFiles
 * @param ruleTable
 * @param filePath
 */
function checkRules(xmlFiles, ruleTable, filePath) {

    let targetXml = xmlFiles.filter((d) => {
        return d['filePath'] === filePath
    })[0];

    for (let i = 0; i < ruleTable.length; i++) {

        let ruleResultI = ruleTable[i]['xPathQueryResult'].filter((d) => {
            return d['filePath'] === filePath;
        })[0]['data'];

        // console.log(ruleResultI);

        let prevQuantifierResult = ruleResultI['quantifierResult'].slice(0);
        let prevConditionedResult = ruleResultI['conditionedResult'].slice(0);
        let prevSatisfied = ruleResultI['satisfied'];
        let prevMissing = ruleResultI['missing'];

        // console.log(prevSatisfied, prevMissing);

        ruleTable[i] = runXPathQuery(targetXml, ruleTable[i]);

        ruleResultI = ruleTable[i]['xPathQueryResult'].filter((d) => {
            return d['filePath'] === filePath;
        })[0]['data'];

        // console.log(ruleResultI);

        ruleResultI['changed'] = (!ResultArraysEqual(prevQuantifierResult, ruleResultI['quantifierResult']) ||
        !ResultArraysEqual(prevConditionedResult, ruleResultI['conditionedResult']) ||
        prevSatisfied !== ruleResultI['satisfied'] ||
        prevMissing !== ruleResultI['missing']);

        ruleResultI['missingChanged'] = prevMissing !== ruleResultI['missing'];
        ruleResultI['satisfiedChanged'] = prevSatisfied !== ruleResultI['satisfied'];
        ruleResultI['allChanged'] = (prevSatisfied + prevMissing) !== (ruleResultI['missing'] + ruleResultI['satisfied']);

        // if (ruleResultI['changed']) {
        //     console.log("changed", ruleTable[i])
        // }
        //
        // console.log("========");
    }
    return ruleTable;
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
        'xmlJson': {
            'fileName': fileName,
            'xml': temp
        },
        'xmlText': new XMLSerializer().serializeToString(par),
        'snippet': resText
    };

}
