/**
 * Created by saharmehrpour on 8/8/17.
 */

/**
 * @constructor
 */
function IndividualRule() {
    this.div = d3.select('#individualRule').select('.container');
}

/**
 * display a specific rule
 * @param ruleIndex
 */
IndividualRule.prototype.displayRule = function (ruleIndex) {

    let self = this;
    let ruleI = this.rules.filter((d) => d.index === ruleIndex)[0];

    this.div.selectAll('div').remove();
    document.getElementById(`page_title`).value = `Rule ${ruleIndex}`;

    let ruleDiv = this.div
        .append('div')
        .classed('paddedDiv ruleContainer', true)
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
 * Set the variable 'rules'
 * @param ruleList
 */
IndividualRule.prototype.setRules = function (ruleList) {
    this.rules = ruleList;
};


/**
 * set the variable 'ws' the webSocket
 * @param webSocket
 */
IndividualRule.prototype.setWS = function (webSocket) {
    this.ws = webSocket;
};

/**
 * create a list div node for quantifier and conditioned result
 * @param data
 * @param group
 * @returns {string}
 */
IndividualRule.prototype.listRender = function (data, group) {

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
                self.sendToServer("xmlResult", list[i]['xml'])
            });
    }

    return detached;
};


/**
 * update the rule and send to server
 * @param index
 */
IndividualRule.prototype.updateRules = function (index) {
    for (let i = 0; i < this.rules.length; i++) {

        if (this.rules[i].index === index) {
            delete this.rules[i]['xPathQueryResult'];
            this.rules[i].ruleDescription = document.getElementById(`rule_desc_${index}`).value;
            this.rules[i].detail = document.getElementById(`rule_detail_${index}`).value;
            // sendToServer(this.ws, "MODIFIED_RULE", `{\"index\":${index},\"ruleText\":${JSON.stringify(this.rules[i])}}`);
            sendToServer(this.ws, "MODIFIED_RULE", this.rules[i]);
            return;
        }
    }

};
