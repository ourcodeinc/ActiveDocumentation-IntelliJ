/**
 * Created by saharmehrpour on 7/6/17.
 */


/**
 * event handler for when the title[tag] is changed
 * @param rules
 */
function managePageTitleChange(rules) {
    let targetTag = document.getElementById(`page_title`).value.split(" ");

    for (let i = 0; i < rules.length; i++) {
        if (targetTag[0] === 'All') {
            d3.select(`#rule_result_${rules[i].index}`).classed('hidden', false);
        }
        else
            d3.select(`#rule_result_${rules[i].index}`).classed('hidden', !arrayContains(rules[i].tags, targetTag));
    }

}

/**
 * clear the table in the browser
 */
function clearRuleTable() {
    d3.select("#RT").selectAll('div').remove();
}


/**
 * clear the table of content
 */
function clearTableOfContent() {
    d3.select("#rules_list").selectAll('li').remove();
    d3.select("#tags_list").selectAll('li').remove();
}


/**
 * display the data in the browser
 * @param rules: ruleJson + initialResult[] + conditionResult[] + match T/F
 * @param ruleIndex
 */
function displayResult(rules, ruleIndex) {

    //let displayData = rules[ruleIndex];
    let displayData = rules.filter((d) => {
        return d.index === ruleIndex
    })[0];

    let ruleDiv = d3.select("#RT")
        .append('div')
        .classed('paddedDiv', true)
        .attr('id', `rule_result_${ruleIndex}`)
        .append('div')
        .classed('ruleDiv', true);

    // rule Description and Detail
    let span = ruleDiv.append('div')
        .classed('paddedDiv', true)
        .append('div')
        .classed('ruleTitleDiv', true)
        .append('span');

    span.append("textarea")
        .attr("spellcheck", false)
        //.append('input')
        //.attr('type', 'text')
        //.attr('value', displayData.ruleDescription)
        .text(displayData.ruleDescription)
        .attr("id", `rule_desc_${displayData.index}`)
        .classed('ruleDescription', true)
        .on("change", () => updateRules(rules, displayData.index));

    span.append('br');
    span.append('textarea')
        .text(displayData.detail)
        .attr("spellcheck", false)
        .attr("id", `rule_detail_${displayData.index}`)
        .classed('ruleDetail', true)
        .on("change", () => updateRules(rules, displayData.index));

    // rule result = quantifier + condition
    let ruleResultDiv = ruleDiv.append('div');

    // quantifier
    let quantifierDiv = ruleResultDiv.append('div')
        .classed('quantifierDiv', true);
    quantifierDiv.append('p')
        .text(displayData.quantifierTitle);
    quantifierDiv.append('em')
        .classed('link matches', true)
        .text(() => {
            return (displayData.satisfied + displayData.missing) + ' matches'
        })
        .on("click", function () {
            let parentNode = d3.select(this.parentNode).select('.quantifierList');
            parentNode.classed('hidden', !parentNode.classed('hidden'));
        });
    quantifierDiv.append('div')
        .classed('quantifierList hidden', true)
        .node()
        .appendChild(listRender(displayData, 'quantifier').node());

    // condition
    let conditionDiv = ruleResultDiv.append('div')
        .classed('conditionDiv', true);
    conditionDiv.append('p')
        .text(displayData.conditionedTitle);
    let p = conditionDiv.append('p');
    p.append('em')
        .classed('link satisfied', true)
        .text(() => {
            return displayData.satisfied + ' satisfied ';
        })
        .on('click', function () {
            let parentNode = d3.select(this.parentNode.parentNode).select('.conditionList');
            parentNode.classed('hidden', !parentNode.classed('hidden'));
        });
    p.append('em')
        .classed('missing', true)
        .text(() => {
            return displayData.missing + ' missing';
        });
    conditionDiv.append('div')
        .classed('conditionList hidden', true)
        .node()
        .appendChild(listRender(displayData, 'satisfied').node());


    // fixing the float div heights and widths
    ruleResultDiv.append('div')
        .style('clear', 'both');

}


/**
 * update the vis after a change in the code
 * @param rules
 * @param ruleIndex
 */
function updateDisplayResult(rules, ruleIndex) {

    let displayData = rules.filter((d) => {
        return d.index === ruleIndex
    })[0];

    let ruleDiv = d3.select("#RT")
        .select(`#rule_result_${ruleIndex}`)
        .select('.ruleDiv')
        .classed('changed', () => displayData.changed);

    if (displayData.changed) {

        // quantifier
        let quantifierDiv = ruleDiv.select('.quantifierDiv');

        quantifierDiv.select('em')
            .classed('changedText', displayData.allChanged)
            .text(() => `${displayData.satisfied + displayData.missing} matches`);

        quantifierDiv.select('.quantifierList').remove();
        quantifierDiv.append('div')
            .classed('quantifierList hidden', true)
            .node()
            .appendChild(listRender(displayData, 'quantifier').node());

        // condition
        let conditionDiv = ruleDiv.select('.conditionDiv', true);

        conditionDiv
            .selectAll('em')
            .each(function (d, i) {
                if (i === 0)
                    d3.select(this)
                        .classed('changedText', displayData.satisfiedChanged)
                        .text(() => `${displayData.satisfied} satisfied `);
                else
                    d3.select(this)
                        .classed('changedText', displayData.missingChanged)
                        .text(() => `${displayData.missing} missing `);

            });

        conditionDiv.select('.conditionList').remove();
        conditionDiv.append('div')
            .classed('conditionList hidden', true)
            .node()
            .appendChild(listRender(displayData, 'satisfied').node());

    }

}


/**
 * create a list div node for quantifier and conditioned result
 * @param data
 * @param group
 * @returns {string}
 */
function listRender(data, group) {

//let regex = /(<([^>]+)>)/ig;
    let detached = d3.select(document.createElement('div'));

    switch (group) {
        case 'quantifier':
            for (let i = 0; i < data.quantifierResult.length; i++) {
                detached
                    .append('div')
                    .classed('partResultDiv', true)
                    .append('pre')
                    .classed('link', true)
                    .html((data.quantifierResult[i]['snippet']))
                    .on('click', () => {
                        sendToServer("xmlResult", data.quantifierResult[i]['xml'])
                    });
            }
            break;

        case 'satisfied':
            for (let i = 0; i < data.conditionedResult.length; i++) {
                detached
                    .append('div')
                    .classed('partResultDiv', true)
                    .append('pre')
                    .classed('link', true)
                    .html((data.conditionedResult[i]['snippet']))
                    .on('click', () => {
                        sendToServer("xmlResult", data.conditionedResult[i]['xml'])
                    });
            }
            break;
    }
    return detached;
}


/**
 * update the rule and send to server
 * @param rules
 * @param index
 */
function updateRules(rules, index) {
    let data = []; // TODO remove unnecessary attributes

    for (let i = 0; i < rules.length; i++) {
        if (rules[i].index === index) {

            let modifiedRule = cloneJSON(rules[i]);

            modifiedRule.ruleDescription = document.getElementById(`rule_desc_${index}`).value;
            modifiedRule.detail = document.getElementById(`rule_detail_${index}`).value;

            data.push(modifiedRule);
        }
        else {
            data.push(rules[i])
        }
    }

    let ruleTableString = "\"ruleTable=" + JSON.stringify(data).substr(1);
    sendToServer("NewRule", ruleTableString);

}


/**
 * display the list of rules and list aof tags
 * @param rules
 */
function displayTableOfContent(rules) {
    let tagList = [];

    for (let i = 0; i < rules.length; i++) {
        tagList = tagList.concat(rules[i].tags);
    }

    let uniqueTags = [...new Set(tagList)];

    d3.select("#rules_list").selectAll("li")
        .data(rules)
        .enter()
        .append('li')
        .classed('link', true)
        .html((d) => {
            return '&#9656; ' + d.ruleDescription
        })
        .on("click", (d) => {
            document.getElementById(`page_title`).value = d.ruleDescription;
            displayResult(rules, d.index);
            d3.selectAll(".main").classed("hidden", true);
            d3.select("#individualRule").classed("hidden", false);
        });

    d3.select("#tags_list").selectAll("li")
        .data(uniqueTags)
        .enter()
        .append('li')
        .classed('link', true)
        .html((d) => {
            return '&#9656; ' + d
        })
        .on("click", (d) => {
            document.getElementById(`page_title`).value = d;
            managePageTitleChange(rules);
            d3.selectAll(".main").classed("hidden", true);
            d3.select("#header_2").classed("hidden", false);
            d3.select("#ruleResults").classed("hidden", false);
        });

}


/**
 * print the logs in the textarea
 * the related div must be un-commented in the chat.html
 * @param dataString
 */
function printLog(dataString) {
    let previousLog = document.getElementById(`debug_output`).value;
    d3.select("#debug_output").html(previousLog + '\n' + dataString);
}
