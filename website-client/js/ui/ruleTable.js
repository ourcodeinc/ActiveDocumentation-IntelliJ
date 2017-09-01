/**
 * Created by saharmehrpour on 8/1/17.
 */

/**
 * @constructor
 */
function RuleTable() {
    this.div = d3.select("#RT");

}


/**
 * Set the variable 'rules'
 * @param ruleList
 */
RuleTable.prototype.setRules = function (ruleList) {
    this.rules = ruleList;
};


/**
 * set the variable 'ws' the webSocket
 * @param webSocket
 */
RuleTable.prototype.setWS = function (webSocket) {
    this.ws = webSocket;
};

/**
 * clear the table - .ruleContainer - in the browser
 * called in "VERIFY_RULES"
 */
RuleTable.prototype.clearRuleTable = function () {
    this.div.selectAll('div').remove();
};


/**
 * remove the changes caused by updateDisplayRules
 */
RuleTable.prototype.cleanRuleTable = function () {

    this.div.selectAll('.partResultDiv, .ruleContainer')
        .classed('hidden', false);

    this.div
        .selectAll('.ruleDiv')
        .classed('changed', false)
        .classed('hidden', false);

    this.div
        .selectAll('em')
        .classed('changedText', false);

    this.div.selectAll('.ruleDiv')
        .each(function (ruleI) {

            // sum up the number of satisfied and violated
            let totalSatisfied = 0, totalMissing = 0;
            for (let i = 0; i < ruleI['xPathQueryResult'].length; i++) {
                totalSatisfied += ruleI['xPathQueryResult'][i]['data']['satisfied'];
                totalMissing += ruleI['xPathQueryResult'][i]['data']['violated']
            }

            d3.select(this).selectAll('.general_').select('.badge')
                .text(() => totalSatisfied + totalMissing);

            d3.select(this).selectAll('.satisfied_').select('.badge')
                .text(() => totalSatisfied);

            d3.select(this).selectAll('.violated_').select('.badge')
                .text(() => totalMissing);
        });
};


/**
 * display all rules
 */
RuleTable.prototype.displayRules = function () {

    for (let i = 0; i < this.rules.length; i++) {
        this.displayResult(this.rules[i]);
    }
};


/**
 * display the data in the browser
 * rules: ruleJson + initialResult[] + conditionResult[]
 * @param ruleI
 */
RuleTable.prototype.displayResult = function (ruleI) {

    let self = this;

    // console.log(ruleI);

    let ruleDiv = this.div
        .append('div')
        .classed('largePaddedDiv ruleContainer', true)
        .attr('id', `rule_result_${ruleI['index']}`)
        .datum(ruleI)
        .append('div')
        .classed('ruleDiv', true);

    // rule Description and Detail
    let span = ruleDiv.append('div')
        .classed('form-group', true)
        .append('span');

    span.append("textarea")
        .attr("spellcheck", false)
        .text(ruleI['ruleDescription'])
        .attr("id", `rule_desc_${ruleI['index']}`)
        .classed('form-control', true)
        .on("change", () => this.updateRules(ruleI['index']));

    // span.append('br');
    span.append('textarea')
        .text(ruleI['detail'])
        .attr("spellcheck", false)
        .attr("id", `rule_detail_${ruleI['index']}`)
        .classed('form-control', true)
        .on("change", () => this.updateRules(ruleI['index']));

    // tag boxes
    let tagBoxesDiv = ruleDiv.append('div');
    let tagBoxes = tagBoxesDiv.selectAll('.label')
        .data(ruleI['tags']);

    tagBoxes.enter()
        .append('div')
        .classed('buttonDiv', true)
        .append('span')
        .classed('label label-default', true)
        .html((d) => d)
        .on('click', (d) => {
            location.hash = `#/tag/${d}`;
        });


    // rule result = quantifier + condition
    let ruleResultDiv = ruleDiv.append('div');

    // sum up the number of satisfied and violated
    let totalSatisfied = 0, totalMissing = 0;
    for (let i = 0; i < ruleI['xPathQueryResult'].length; i++) {
        totalSatisfied += ruleI['xPathQueryResult'][i]['data']['satisfied'];
        totalMissing += ruleI['xPathQueryResult'][i]['data']['violated']
    }

    let tabData = [
        {'title': 'All', 'xQuery': 'quantifier', 'type': 'general_', 'count': (totalMissing + totalSatisfied)},
        {'title': 'Satisfied', 'xQuery': 'satisfied', 'type': 'satisfied_', 'count': totalSatisfied},
        {'title': 'Violated', 'xQuery': 'violated', 'type': 'violated_', 'count': totalMissing}
    ];

    // tabs

    let container = ruleResultDiv.append('div')
        .style('padding-top', '10px')
        .style('clear', 'both');

    let tabs = container.append('ul')
        .classed('nav nav-tabs', true)
        .selectAll('li')
        .data(tabData);

    tabs.enter().append('li')
    //.classed('active', (d, i) => i === 0)
        .append('a')
        .attr('data-toggle', `tab`)
        .attr('class', (d) => d['type'])
        .attr('href', (d) => `#${d['title']}_${ruleI['index']}`)
        .html((d) => `${d['title']} <span class="badge">${d['count']}</span>`);

    let contents = container.append('div')
        .classed('tab-content', true)
        .selectAll('tab-pane')
        .data(tabData);

    contents.enter()
        .append('div')
        .classed('tab-pane fade', true)
        //.classed('in active', (d, i) => i === 0)
        .attr('id', (d) => `${d['title']}_${ruleI['index']}`)
        .each(function (d) {
            this.appendChild(self.listRender(ruleI, d['xQuery']).node());
        });
};


/**
 * update display of the rules for a specific file
 * @param filePath
 */
RuleTable.prototype.updateDisplayRules = function (filePath) {

    d3.select("#page_title").html("Rules for <br><small>" + filePath + "</small>");

    for (let i = 0; i < this.rules.length; i++) {
        this.updateDisplayResult(this.rules[i], filePath);
    }
};


/**
 * update the vis after a change in the code
 * @param ruleI
 * @param filePath
 */
RuleTable.prototype.updateDisplayResult = function (ruleI, filePath) {

    let self = this;

    let ruleIfile = ruleI['xPathQueryResult'].filter((d) => d['filePath'] === filePath)[0]['data'];

    console.log(ruleIfile);

    let ruleDiv = this.div
        .select(`#rule_result_${ruleI['index']}`)
        .select('.ruleDiv')
        //.classed('changed', () => ruleIfile['changed'])
        .classed('blue-bg', () => ruleIfile['allChanged'] === 'greater' && ruleIfile['satisfiedChanged'] === ruleIfile['violatedChanged'] === 'none')
        .classed('green-bg', () => ruleIfile['satisfiedChanged'] === 'greater')
        .classed('red-bg', () => ruleIfile['violatedChanged'] === 'greater')
        // no result for the file AND unchanged
        .classed('hidden', () => !ruleIfile['changed'] && ruleIfile['violated'] === 0 && ruleIfile['satisfied'] === 0)
        .datum(ruleI);

    // tabs
    ruleDiv.select('.general_').select('.badge').text(`${ruleIfile['satisfied'] + ruleIfile['violated']}`);
    ruleDiv.select('.satisfied_').select('.badge').text(`${ruleIfile['satisfied']}`);
    ruleDiv.select('.violated_').select('.badge').text(`${ruleIfile['violated']}`);

    //*** quantifier
    let quantifierDiv = ruleDiv.select(`#All_${ruleI['index']}`);

    let partResultDiv = quantifierDiv
        .selectAll('.partResultDiv')
        .classed('hidden', true);

    // rewrite the snippets for the changed file
    partResultDiv.filter(function (d) {
        return d['filePath'] === filePath;
    })
        .remove();

    quantifierDiv
        .select('div')
        .node()
        .appendChild(self.listRender(ruleIfile, 'quantifierChanged').node());

    //*** satisfied
    let satisfiedDiv = ruleDiv.select(`#Satisfied_${ruleI['index']}`);

    partResultDiv = satisfiedDiv
        .selectAll('.partResultDiv')
        .classed('hidden', true);

    // rewrite the snippets for the changed file
    partResultDiv.filter(function (d) {
        return d['filePath'] === filePath;
    })
        .remove();

    satisfiedDiv
        .select('div')
        .node()
        .appendChild(self.listRender(ruleIfile, 'satisfiedChanged').node());


    //*** satisfied
    let violatedDiv = ruleDiv.select(`#Violated_${ruleI['index']}`);

    partResultDiv = violatedDiv
        .selectAll('.partResultDiv')
        .classed('hidden', true);

    // rewrite the snippets for the changed file
    partResultDiv.filter(function (d) {
        return d['filePath'] === filePath;
    })
        .remove();

    violatedDiv
        .select('div')
        .node()
        .appendChild(self.listRender(ruleIfile, 'violatedChanged').node());

};


/**
 * update the rule and send to server
 * @param index
 */
RuleTable.prototype.updateRules = function (index) {

    for (let i = 0; i < this.rules.length; i++) {

        if (this.rules[i].index === index) {
            let newObj = cloneJSON(this.rules[i]);
            delete newObj['xPathQueryResult'];

            this.rules[i].ruleDescription = document.getElementById(`rule_desc_${index}`).value;
            this.rules[i].detail = document.getElementById(`rule_detail_${index}`).value;

            sendToServer(this.ws, "MODIFIED_RULE", newObj);

            return;
        }
    }

};


/**
 * event handler for when the 'tag' is changed
 */
RuleTable.prototype.filterByTag = function (targetTag) {
    if (targetTag[0] === 'All') {
        d3.selectAll(`.ruleContainer`).classed('hidden', false);
    }
    else {
        d3.selectAll(`.ruleContainer`).classed('hidden', true);
        d3.selectAll(`.ruleContainer`)
            .classed('hidden', function (d) {
                return !arrayContains(d['tags'], targetTag)
            });
    }

};


/**
 * create a list div node for quantifier and satisfied result
 * @param data
 * @param group
 * @returns {string}
 */
RuleTable.prototype.listRender = function (data, group) {

    let self = this;

    //let regex = /(<([^>]+)>)/ig;
    let detached = d3.select(document.createElement('div'));

    let list = [];
    switch (group) {
        case 'quantifier':
            for (let i = 0; i < data['xPathQueryResult'].length; i++) {
                list = list.concat(data['xPathQueryResult'][i]['data']['quantifierResult'])
            }
            break;
        case 'satisfied':
            for (let i = 0; i < data['xPathQueryResult'].length; i++) {
                list = list.concat(data['xPathQueryResult'][i]['data']['satisfiedResult'])
            }
            break;
        case 'violated':
            for (let i = 0; i < data['xPathQueryResult'].length; i++) {
                list = list.concat(data['xPathQueryResult'][i]['data']['violatedResult'])
            }
            break;
        case 'quantifierChanged':
            list = data['quantifierResult'];
            break;
        case 'satisfiedChanged':
            list = data['satisfiedResult'];
            break;
        case 'violatedChanged':
            list = data['violatedResult'];
            break;
    }

    for (let i = 0; i < list.length; i++) {
        detached
            .append('div')
            .classed('partResultDiv', true)
            .datum(list[i])
            .append('pre')
            .classed('link', true)
            .html((list[i]['snippet']))
            .on('click', () => {
                sendToServer(self.ws, "XML_RESULT", list[i]['xml'])
            });
    }

    return detached;
};

