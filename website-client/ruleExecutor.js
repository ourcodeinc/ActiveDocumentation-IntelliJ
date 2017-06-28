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
    let match = false;

    function nsResolver(prefix) {
        let ns = {'src': 'http://www.srcML.org/srcML/src'};
        return ns[prefix] || null;
    }

    // checks validity of the XML
    if (!xml.evaluate) {
        console.log('error in xml.evaluate');
        return;
    }


    switch (ruleTableI.verification) {
        case "count":
            // compare the 'count' of groups
            match = countGroups(xml, ruleTableI);
            break;
        case "array":
            // compare arrays of groups
            match = arrayGroups(xml, ruleTableI);
            break;
    }


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

    let data = cloneJSON(ruleTableI);
    data['initialResult'] = initialResult;
    data['conditionedResult'] = conditionedResult;
    data['match'] = match;

    console.log(data);

    displayRulesAndResult(data);

}

/**
 * count and compare results
 * @param xml
 * @param ruleTableI
 */
function countGroups(xml, ruleTableI) {
    function nsResolver(prefix) {
        let ns = {'src': 'http://www.srcML.org/srcML/src'};
        return ns[prefix] || null;
    }

    let count1Node = xml.evaluate(ruleTableI.countInitialGroup, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let count1 = count1Node.numberValue;

    let count2Node = xml.evaluate(ruleTableI.countConditionedGroup, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let count2 = count2Node.numberValue;

    return (count1 === count2);
}

/**
 * compare array of results
 * @param xml
 * @param ruleTableI
 */
function arrayGroups(xml, ruleTableI) {
    function nsResolver(prefix) {
        let ns = {'src': 'http://www.srcML.org/srcML/src'};
        return ns[prefix] || null;
    }

    let array1 = [], array2 = [];

    let array1Node = xml.evaluate(ruleTableI.arrayInitialGroup, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let res1 = array1Node.iterateNext();
    while (res1) {
        array1.push(new XMLSerializer().serializeToString(res1));
        res1 = array1Node.iterateNext();
    }

    let array2Node = xml.evaluate(ruleTableI.arrayConditionedGroup, xml, nsResolver, XPathResult.ANY_TYPE, null);
    let res2 = array2Node.iterateNext();
    while (res2) {
        array2.push(new XMLSerializer().serializeToString(res2));
        res2 = array2Node.iterateNext();
    }

    return (arrayContains(array2, array1));
}


/**
 * display the data in the browser
 * @param data
 */
function displayRulesAndResult(data) {

    d3.select("#RT")
        .append('tr')
        .append('th')
        .html(data.ruleDescription)
        .classed('matching', data.match)
        .classed('mismatch', !data.match);

    d3.select("#RT")
        .append('tr')
        .append('td')
        .html(data.detail);

    d3.select("#RT")
        .append('tr')
        .selectAll('td')
        .data([data.titleInitialGroup, data.initialResult])
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
        .data([data.titleConditionedGroup, data.conditionedResult])
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
