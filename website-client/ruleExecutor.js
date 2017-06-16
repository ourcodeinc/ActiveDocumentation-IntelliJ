/**
 * runs the XPath query
 * @param xmlString
 * @param ruleTableI
 */
function runXPathQuery(xmlString, ruleTableI) {

    let parser = new DOMParser();
    let xml = parser.parseFromString(xmlString, "text/xml");
    let queryResult = [];
    let linkData = "";
    let xmlResult = cloneXML(xml);

    let query = ruleTableI.xPathQuery;
    let xPathQueryResult = ruleTableI.expectedXPathQueryResult;

    function nsResolver(prefix) {
        let ns = {'src': 'http://www.srcML.org/srcML/src'};
        return ns[prefix] || null;
    }

    if (xml.evaluate) {
        let nodes = xml.evaluate(query, xml, nsResolver, XPathResult.ANY_TYPE, null);
        switch (nodes.resultType) {
            case XPathResult.NUMBER_TYPE:
                queryResult.push(nodes.numberValue.toString());
                break;

            case XPathResult.STRING_TYPE:
                queryResult.push(nodes.stringValue);
                break;

            case XPathResult.BOOLEAN_TYPE:
                queryResult.push(nodes.booleanValue.toString());
                break;

            case XPathResult.UNORDERED_NODE_ITERATOR_TYPE:
            case XPathResult.ORDERED_NODE_ITERATOR_TYPE:
                let result = nodes.iterateNext();
                while (result) {
                    queryResult.push(new XMLSerializer().serializeToString(result));
                    result = nodes.iterateNext();
                }

                linkData = getXmlData(xmlResult, query);

                break;

            case XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE:
            case XPathResult.ORDERED_NODE_SNAPSHOT_TYPE:
                for (let i = 0; i < nodes.snapshotLength; i++)
                    console.log(nodes.snapshotItem(i));
                break;

            default:
                console.log("Any Unordered Node Type, Any Type");
                console.log(nodes);
                break;
        }

        if (arraysEqual(queryResult.sort(), xPathQueryResult)) {
            console.log("Expected result!");
            console.log(queryResult);
        }
        else {
            console.log("Not matching ...");
            console.log("queryResult", queryResult);
            console.log("xPathQueryResult", xPathQueryResult);
        }


        let data = {
            'header': ruleTableI.header, 'description': ruleTableI.description,
            'query': ruleTableI.xPathQuery, 'expectedResult': ruleTableI.expectedXPathQueryResult,
            'result': queryResult, 'linkData': linkData
        };

        displayRulesAndResult(data);

    }
    else {
        console.log('error in xml.evaluate')
    }

}

/**
 * display the data in the browser
 * @param data
 */
function displayRulesAndResult(data) {

    d3.select("#RT")
        .append('tr')
        .append('th')
        .html(data.header);

    d3.select("#RT")
        .append('tr')
        .selectAll('td')
        .data([data.description, data.expectedResult])
        .enter()
        .append('td')
        .html((d) => d);

    d3.select("#RT")
        .append('tr')
        .selectAll('td')
        .data([data.result, data.linkData])
        .enter()
        .append('td')
        .html((d, i) => {
            if (i === 0) return d;
            return "<p> Jump to line </p>"
        })
        .on("click", (d, i) => {
            if (i === 0) return;
            if (ws) {
                ws.send(d);
            }
        })
        .classed("link", (d, i) => i === 1);

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
 * @param xml
 * @param query
 */
function getXmlData(xml, query) {

    function nsResolver(prefix) {
        let ns = {'src': 'http://www.srcML.org/srcML/src'};
        return ns[prefix] || null;
    }

    let nodes = xml.evaluate(query, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let res = nodes.iterateNext();

    console.log("res", res);

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
    return "{\"source\":\"WEB\",\"destination\":\"IDEA\",\"command\":\"xmlResult\",\"data\":{\"fileName\":\""
        + (fileName.replace(/['"]+/g, '\\"')) + "\" ,\"xml\":\""
        + (temp.replace(/['"]+/g, '\\"')) + "\"}}";

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