/**
 * Created by saharmehrpour on 8/1/17.
 */

/**
 * @constructor
 */
function RuleTable() {
    this.div = d3.select("#RT");

    d3.select("#page_title")
        .on("change", () => {
            let targetTag = document.getElementById(`page_title`).value.split(" ");
            location.hash = '#/tag/' + targetTag.join('+');
        });

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
 * clear the table in the browser
 */
RuleTable.prototype.resetRuleTable = function () {
    this.div.selectAll('div').remove();
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
 * rules: ruleJson + initialResult[] + conditionResult[] + match T/F
 * @param ruleI
 */
RuleTable.prototype.displayResult = function (ruleI) {

    let self = this;

    // console.log(ruleI);

    let ruleDiv = this.div
        .append('div')
        .classed('largePaddedDiv ruleContainer', true)
        .attr('id', `rule_result_${ruleI.index}`)
        .datum(ruleI)
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
        .text(ruleI.ruleDescription)
        .attr("id", `rule_desc_${ruleI.index}`)
        .classed('ruleDescription', true)
        .on("change", () => this.updateRules(ruleI.index));

    span.append('br');
    span.append('textarea')
        .text(ruleI.detail)
        .attr("spellcheck", false)
        .attr("id", `rule_detail_${ruleI.index}`)
        .classed('ruleDetail', true)
        .on("change", () => this.updateRules(ruleI.index));

    // tag boxes
    let tagBoxesDiv = ruleDiv.append('div');
    let tagBoxes = tagBoxesDiv.selectAll('.tagBoundingBox')
        .data(ruleI['tags']);

    tagBoxes.enter()
        .append('div')
        .classed('tagBoundingBox', true)
        .append('div')
        .classed('tagBox', true)
        .html((d) => {
            return '<span class="link">' + d + '</span>';
        })
        .on('click', (d) => {
            location.hash = `#/tag/${d}`;
        });


    // rule result = quantifier + condition
    let ruleResultDiv = ruleDiv.append('div');

    // sum up the number of satisfied and missing
    let totalSatisfied = 0, totalMissing = 0;
    for (let i = 0; i < ruleI['xPathQueryResult'].length; i++) {
        totalSatisfied += ruleI['xPathQueryResult'][i]['data']['satisfied'];
        totalMissing += ruleI['xPathQueryResult'][i]['data']['missing']
    }

    // quantifier
    let quantifierDiv = ruleResultDiv.append('div')
        .classed('quantifierDiv', true);
    quantifierDiv.append('p')
        .text(ruleI.quantifierTitle);
    quantifierDiv.append('em')
        .classed('link matches', true)
        .text(() => {
            return (totalSatisfied + totalMissing) + ' matches'
        })
        .on("click", function () {
            let parentNode = d3.select(this.parentNode).select('.quantifierList');
            parentNode.classed('hidden', !parentNode.classed('hidden'));
        });
    quantifierDiv.append('div')
        .classed('quantifierList hidden', true)
        .node()
        .appendChild(self.listRender(ruleI, 'quantifier').node());

    // condition
    let conditionDiv = ruleResultDiv.append('div')
        .classed('conditionDiv', true);
    conditionDiv.append('p')
        .text(ruleI.conditionedTitle);
    let p = conditionDiv.append('p');
    p.append('em')
        .classed('link satisfied', true)
        .text(() => {
            return totalSatisfied + ' satisfied ';
        })
        .on('click', function () {
            let parentNode = d3.select(this.parentNode.parentNode).select('.conditionList');
            parentNode.classed('hidden', !parentNode.classed('hidden'));
        });
    p.append('em')
        .classed('missing', true)
        .text(() => {
            return totalMissing + ' missing';
        });
    conditionDiv.append('div')
        .classed('conditionList hidden', true)
        .node()
        .appendChild(self.listRender(ruleI, 'conditioned').node());


    // fixing the float div heights and widths
    ruleResultDiv.append('div')
        .style('clear', 'both');

};


/**
 * update display of the rules for a specific file
 * @param filePath
 */
RuleTable.prototype.updateDisplayRules = function (filePath) {

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

    let ruleDiv = this.div
        .select(`#rule_result_${ruleI.index}`)
        .select('.ruleDiv')
        .classed('changed', () => ruleIfile.changed)
        .classed('hidden', () => !ruleIfile.changed && ruleIfile.missing === 0 && ruleIfile.satisfied === 0)
        .datum(ruleI);

    // quantifier
    let quantifierDiv = ruleDiv.select('.quantifierDiv');

    quantifierDiv.select('em')
        .classed('changedText', ruleIfile.allChanged)
        .text(() => `${ruleIfile['satisfied'] + ruleIfile['missing']} matches`);

    let partResultDiv = quantifierDiv
        .selectAll('.partResultDiv')
        .classed('hidden', true);

    partResultDiv.filter(function (d) {
        return d['filePath'] === filePath;
    })
        .remove();

    quantifierDiv
        .select('.quantifierList')
        .node()
        .appendChild(self.listRender(ruleIfile, 'quantifierChanged').node());

    // condition
    let conditionDiv = ruleDiv.select('.conditionDiv');

    conditionDiv
        .selectAll('em')
        .each(function (d, i) {
            if (i === 0)
                d3.select(this)
                    .classed('changedText', ruleI.satisfiedChanged)
                    .text(() => `${ruleIfile['satisfied']} satisfied `);
            else
                d3.select(this)
                    .classed('changedText', ruleI.missingChanged)
                    .text(() => `${ruleIfile['missing']} missing `);
        });

    partResultDiv = conditionDiv
        .selectAll('.partResultDiv')
        .classed('hidden', true);

    partResultDiv.filter(function (d) {
        return d['filePath'] === filePath;
    })
        .remove();

    conditionDiv
        .select('.conditionList')
        .node()
        .appendChild(self.listRender(ruleIfile, 'conditionedChanged').node());


};


/**
 * update the rule and send to server
 * @param index
 */
RuleTable.prototype.updateRules = function (index) {

    // let data = [];

    for (let i = 0; i < this.rules.length; i++) {
        delete this.rules[i]['xPathQueryResult'];

        if (this.rules[i].index === index) {

            this.rules[i].ruleDescription = document.getElementById(`rule_desc_${index}`).value;
            this.rules[i].detail = document.getElementById(`rule_detail_${index}`).value;

            //sendToServer(this.ws, "MODIFIED_RULE", `{\"index\":${index},\"ruleText\":${JSON.stringify(this.rules[i])}}`);
            sendToServer(this.ws, "MODIFIED_RULE", this.rules[i]);
            return;
        }
    }

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

            // sum up the number of satisfied and missing
            let totalSatisfied = 0, totalMissing = 0;
            for (let i = 0; i < ruleI['xPathQueryResult'].length; i++) {
                totalSatisfied += ruleI['xPathQueryResult'][i]['data']['satisfied'];
                totalMissing += ruleI['xPathQueryResult'][i]['data']['missing']
            }

            d3.select(this).selectAll('.matches')
                .text(() => {
                    return (totalSatisfied + totalMissing) + ' matches'
                });

            d3.select(this).selectAll('.satisfied')
                .text(() => {
                    return totalSatisfied + ' satisfied ';
                });

            d3.select(this).selectAll('.missing')
                .text(() => {
                    return totalMissing + ' missing ';
                });
        });

    document.getElementById(`page_title`).value = "All Rules";
};


/**
 * event handler for when the title[tag] is changed
 */
RuleTable.prototype.updateTagRules = function () {
    let targetTag = document.getElementById(`page_title`).value.split(" ");
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
 * create a list div node for quantifier and conditioned result
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
                list = list.concat(data['xPathQueryResult'][i]['data'].quantifierResult)
            }
            break;
        case 'conditioned':
            for (let i = 0; i < data['xPathQueryResult'].length; i++) {
                list = list.concat(data['xPathQueryResult'][i]['data'].conditionedResult)
            }
            break;
        case 'quantifierChanged':
            list = data['quantifierResult'];
            break;
        case 'conditionedChanged':
            list = data['conditionedResult'];
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

