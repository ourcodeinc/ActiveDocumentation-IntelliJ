/**
 * runs the XPath query
 * @param xmlString
 * @param ruleTableI
 */
function runXPathQuery(xmlString, ruleTableI) {

    let parser = new DOMParser();
    let xml = parser.parseFromString(xmlString, "text/xml");
    let initialResult = [];
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


    // first compare the 'count' of groups
    let count1Node = xml.evaluate(ruleTableI.countInitialGroup, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let count1 = count1Node.numberValue;

    let count2Node = xml.evaluate(ruleTableI.countConditionedGroup, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let count2 = count2Node.numberValue;

    if (count1 !== count2)
        console.log("Not matching!");


    // find the links
    let initialGroupNodes = xml.evaluate(ruleTableI.initialGroup, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let result1 = initialGroupNodes.iterateNext();
    let index = 0;
    while (result1) {
        initialResult.push({
            "result": new XMLSerializer().serializeToString(result1),
            "xml": getXmlData(xml, ruleTableI.initialGroup, index)
        });
        result1 = initialGroupNodes.iterateNext();
        index += 1;
    }

    let conditionedGroupNodes = xml.evaluate(ruleTableI.conditionedGroup, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let result2 = conditionedGroupNodes.iterateNext();
    index = 0;
    while (result2) {
        conditionedResult.push({
            "result": new XMLSerializer().serializeToString(result2),
            "xml": getXmlData(xml, ruleTableI.conditionedGroup, index)
        });
        result2 = conditionedGroupNodes.iterateNext();
        index += 1;
    }


    let data = {
        'ruleDescription': ruleTableI.ruleDescription, 'detail': ruleTableI.detail,
        'initialGroup': ruleTableI.initialGroup, 'initialResult': initialResult,
        'conditionedGroup': ruleTableI.conditionedGroup, 'conditionedResult': conditionedResult
    };

    console.log(data);

    displayRulesAndResult(data);

}

/**
 * display the data in the browser
 * @param data
 */
function displayRulesAndResult(data) {

    d3.select("#RT")
        .append('tr')
        .append('th')
        .html(data.ruleDescription);

    d3.select("#RT")
        .append('tr')
        .append('td')
        .html(data.detail);

    d3.select("#RT")
        .append('tr')
        .selectAll('td')
        .data([data.initialGroup, data.initialResult])
        .enter()
        .append('td')
        .each(function (d, i) {
            if (i === 0)
                d3.select(this)
                    .append("p")
                    .text(d);
            else
                d3.select(this)
                    .selectAll(".link")
                    .data(d)
                    .enter()
                    .append("p")
                    .text("Jump to line!")
                    .on("click", (g) => {
                        sendToServer("xmlResult", g.xml)
                    })
                    .classed("link", true);
        });

    d3.select("#RT")
        .append('tr')
        .selectAll('td')
        .data([data.conditionedGroup, data.conditionedResult])
        .enter()
        .append('td')
        .each(function (d, i) {
            if (i === 0)
                d3.select(this)
                    .append("p")
                    .text(d);
            else
                d3.select(this)
                    .selectAll(".link")
                    .data(d)
                    .enter()
                    .append("p")
                    .text("Jump to line!")
                    .on("click", (g) => {
                        sendToServer("xmlResult", g.xml)
                    })
                    .classed("link", true);
        });
}


/**
 * clear the table in the browser
 */
 function clearRuleTable() {
    d3.select("#RT").selectAll('tr').remove();
}


/**
 * check whether two arrays are equals
 * @param array1
 * @param array2
 * @returns {boolean}
 */
function arraysEqual(array1, array2) {

    let arr1 = array1.slice(0);
    let arr2 = array2.slice(0);

    if (arr1.length !== arr2.length)
        return false;
    for (let i = arr2.length; i--;) {
        if (arr1.indexOf(arr2[i]) === -1)
            return false;
        arr1.splice(arr1.indexOf(arr2[i]), 1)
    }

    return true;
}

/**
 * deep copy of an xml variable
 * @param xml
 * @returns {Document}
 */
function cloneXML(xml) {
    let newDocument = xml.implementation.createDocument(
        xml.namespaceURI, //namespace to use
        "",                     //name of the root element (or for empty document)
        null                      //doctype (null for XML)
    );
    let newNode = newDocument.importNode(
        xml.documentElement, //node to import
        true                         //clone its descendants
    );
    newDocument.appendChild(newNode);

    return newDocument;
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

    /**
     * remove first child sib, sib, parent sib, grandparent sib, grand-grandparent sib, ... <- recursive
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

    let par;
    if (res.firstChild && res.firstChild.nodeType !== -1)
        par = removeSib(res.firstChild);
    else
        par = removeSib(res);


    let fileName = par.getAttribute("filename");
    let temp = new XMLSerializer().serializeToString(par);
    return "{\"fileName\":\"" + (fileName.replace(/['"]+/g, '\\"')) + "\" ,\"xml\":\""
        + (temp.replace(/['"]+/g, '\\"')) + "\"}";

}

/**
 * find the 'unit' parent of the node
 * @param node
 * @returns {*}
 */
function findUnitNode(node) {
    if (node.nodeName === 'unit')
        return node;
    else
        return findUnitNode(node.parentNode);
}