/**
 * Created by saharmehrpour on 6/19/17.
 */

function initRuleGenerator() {

    setupForm();

}

/**
 * setup the rule generating form by de/activating the inputs
 */
function setupForm() {
    d3.select("#ruleGenerator").on("click", generateNewRule);

    d3.select("#elements").on("change", function () {
        if (d3.select(this).property("value") === 'all')
            d3.select("#initElementName").attr("disabled", true);
        else
            d3.select("#initElementName").attr("disabled", null);
    });

    d3.select("#conditions").on("change", function () {
        if (d3.select(this).property("value") === 'constructor')
            d3.select("#condElementName").attr("disabled", true);
        else
            d3.select("#condElementName").attr("disabled", null);
    });
}

/**
 * generate the xpath query based on the input
 */
function generateNewRule() {
    let conditionedXpath = "//src:class";
    let quantifierXpath = "";
    let quantifierTitle = "";
    let conditionedTitle = "";

    switch (d3.select("#elements").property("value")) {
        case "variable":
            conditionedXpath = conditionedXpath + `[src:block/src:decl_stmt/src:decl/src:name[text()=\"${$("initElementName").value}\"]`;
            quantifierXpath = conditionedXpath + ']';
            conditionedXpath = conditionedXpath + ' and ';
            quantifierTitle = `All classes with Variable ${$("initElementName").value}`;
            break;
        case "annotation":
            conditionedXpath = conditionedXpath + `[src:annotation/src:name[text()=\"${$("initElementName").value}\"]`;
            quantifierXpath = conditionedXpath + ']';
            conditionedXpath = conditionedXpath + ' and ';
            quantifierTitle = `All @${$("initElementName").value} classes`;
            break;
        case "all":
            quantifierXpath = conditionedXpath;
            conditionedXpath = conditionedXpath + '[';
            quantifierTitle = `All classes`;
            break;
    }

    switch (d3.select("#conditions").property("value")) {
        case "method":
            conditionedXpath = conditionedXpath + `src:block/src:function/src:name[text()=\"${$("condElementName").value}\"]]`;
            conditionedTitle = `Classes with Method ${$("condElementName").value}`;
            break;
        case "constructor":
            conditionedXpath = conditionedXpath + "count(src:block/src:constructor)>0]";
            conditionedTitle = `Classes with constructors`;
            break;
        case "variable":
            conditionedXpath = conditionedXpath + `src:block/src:decl_stmt/src:decl/src:name[text()=\"${$("condElementName").value}\"]]`;
            conditionedTitle = `Classes with Variable ${$("condElementName").value}`;
            break;
    }

    let currentdate = new Date();
    let datetime = "Generated at: " + currentdate.getDate() + "/"
        + (currentdate.getMonth() + 1) + "/"
        + currentdate.getFullYear() + " @ "
        + currentdate.getHours() + ":"
        + currentdate.getMinutes() + ":"
        + currentdate.getSeconds();

    let newRule = {
        'ruleDescription': `New Rule: ${$("ruleDescription").value}`,
        'detail': datetime + ': ' + $("ruleDetail").value,
        'quantifierXpath': quantifierXpath,
        'quantifierTitle': quantifierTitle,
        'quantifierXpathName': quantifierXpath + "/src:name/text()",
        'conditionedXpath': conditionedXpath,
        'conditionedXpathName': conditionedXpath + "/src:name/text()",
        'conditionedTitle': conditionedTitle
    };

    ruleTable.push(newRule);

    let ruleTableString = "\"ruleTable=" + JSON.stringify(ruleTable).substr(1);


    sendToServer("NewRule", ruleTableString);

    // No need - the server sends the message to update the rule results
    // // display the result
    // clearRuleTable();
    // for (let i = 0; i < ruleTable.length; i++) {
    //     runXPathQuery(xml, ruleTable[i]);
    // }
}