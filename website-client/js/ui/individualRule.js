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

    let tableData = [
        [{'type': 'label', 'value': 'Rule Description'}, {'type': 'ruleDescription', 'value': ruleI.ruleDescription}],
        [{'type': 'label', 'value': 'Rule Detail'}, {'type': 'ruleDetail', 'value': ruleI.detail}],
        [{'type': 'label', 'value': 'Rule Tags'}, {'type': 'tags', 'value': ruleI.tags}],
        [{'type': 'label', 'value': 'Quantifier'}, {'type': 'quantifierList', 'value': ruleI.quantifierTitle}],
        [{'type': 'label', 'value': 'Conditioned'}, {'type': 'conditionedList', 'value': ruleI.conditionedTitle}]];

    // sum up the number of satisfied and missing
    let totalSatisfied = 0, totalMissing = 0;
    for (let i = 0; i < ruleI['xPathQueryResult'].length; i++) {
        totalSatisfied += ruleI['xPathQueryResult'][i]['data']['satisfied'];
        totalMissing += ruleI['xPathQueryResult'][i]['data']['missing']
    }

    let table = this.div.append('div');

    // create a row for each object in the data
    let rows = table.selectAll('.tableRow')
        .data(tableData)
        .enter()
        .append('div')
        .classed('tableRow', true);

    // create a cell in each row for each column
    let cells = rows.selectAll('.tableCell')
        .data((d) => d)
        .enter()
        .append('div')
        .classed('tableCell', true)
        .classed('labelCell', (d, i) => i === 0)
        .classed('infoCell', (d, i) => i === 1);

    rows.selectAll('.tableCell')
        .each(function (d) {
            let element = d3.select(this);
            if (d.type === 'label')
                element
                    .append('h4')
                    .text(d.value);

            if (d.type === 'ruleDescription' || d.type === 'ruleDetail')
                element.append("textarea")
                    .attr("spellcheck", false)
                    .text(d.value)
                    .attr("id", d.type)
                    .on("change", () => self.updateRules(ruleI.index));

            if (d.type === 'tags') {
                let tagBoxes = element
                    .append('div')
                    .attr('name', 'tags')
                    .selectAll('.tagBoundingBox')
                    .data(ruleI.tags);

                tagBoxes.enter()
                    .append('div')
                    .classed('tagBoundingBox', true)
                    .append('div')
                    .classed('tagBox', true)
                    .html((d) => '<span class="link">' + d + '</span>')
                    .on('click', (d) => {
                        location.hash = `#/tag/${d}`;
                    });
            }

            if (d.type === 'quantifierList') {
                element.append('p')
                    .text(d.value);
                element.append('em')
                    .classed('link matches', true)
                    .text(() => (totalSatisfied + totalMissing) + ' matches')
                    .on("click", function () {
                        let parentNode = d3.select(this.parentNode).select('.quantifierList');
                        parentNode.classed('hidden', !parentNode.classed('hidden'));
                    });
                element.append('div')
                    .classed('quantifierList hidden', true)
                    .node()
                    .appendChild(self.listRender(ruleI, 'quantifier').node());
            }

            if (d.type === 'conditionedList') {
                element.append('p')
                    .text(d.value);
                let p = element.append('p');
                p.append('em')
                    .classed('link satisfied', true)
                    .text(() => totalSatisfied + ' satisfied ')
                    .on('click', function () {
                        let parentNode = d3.select(this.parentNode.parentNode).select('.conditionList');
                        parentNode.classed('hidden', !parentNode.classed('hidden'));
                    });
                p.append('em')
                    .classed('missing', true)
                    .text(() => totalMissing + ' missing');
                element.append('div')
                    .classed('conditionList hidden', true)
                    .node()
                    .appendChild(self.listRender(ruleI, 'conditioned').node());
            }
        });

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
                sendToServer(self.ws, "XML_RESULT", list[i]['xml'])
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

            let newObj = cloneJSON(this.rules[i]);
            delete newObj['xPathQueryResult'];

            this.rules[i].ruleDescription = document.getElementById(`ruleDescription`).value;
            this.rules[i].detail = document.getElementById(`ruleDetail`).value;
            sendToServer(this.ws, "MODIFIED_RULE", newObj);

            return;
        }
    }

    console.log('failed');

};
