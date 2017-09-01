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
    d3.select('#page_title').text(`Rule ${ruleIndex}`);
    //document.getElementById(`page_title`).value = `Rule ${ruleIndex}`;

    let tableData = [
        [{'type': 'label', 'value': 'Rule Description'}, {'type': 'ruleDescription', 'value': ruleI.ruleDescription}],
        [{'type': 'label', 'value': 'Rule Detail'}, {'type': 'ruleDetail', 'value': ruleI.detail}],
        [{'type': 'label', 'value': 'Rule Tags'}, {'type': 'tags', 'value': ruleI.tags}],
        [{'type': 'label', 'value': 'Quantifier'}, {'type': 'quantifierList', 'value': ruleI.quantifierTitle}],
        [{'type': 'label', 'value': 'Conditioned'}, {'type': 'conditionedList', 'value': ruleI.conditionedTitle}]];

    // sum up the number of satisfied and violated
    let totalSatisfied = 0, totalMissing = 0;
    for (let i = 0; i < ruleI['xPathQueryResult'].length; i++) {
        totalSatisfied += ruleI['xPathQueryResult'][i]['data']['satisfied'];
        totalMissing += ruleI['xPathQueryResult'][i]['data']['violated']
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
                    .classed('form-control', true)
                    .attr("spellcheck", false)
                    .text(d.value)
                    .attr("id", d.type)
                    .on("change", () => self.updateRules(ruleI.index));

            if (d.type === 'tags') {
                let tagBoxesDiv = element.append('div');
                let tagBoxes = tagBoxesDiv.selectAll('.label')
                    .data(ruleI.tags);

                tagBoxes.enter()
                    .append('div')
                    .classed('buttonDiv', true)
                    .append('span')
                    .classed('label label-default', true)
                    .html((d) => d)
                    .on('click', (d) => {
                        location.hash = `#/tag/${d}`;
                    });
            }


            if (d.type === 'quantifierList') {
                element.append('p')
                    .text(d.value);
                element.append('em')
                    .classed('btn btn-primary btn-xs', true)
                    .attr('data-toggle', 'collapse')
                    .attr('href', '#quantifierList')
                    .text(() => (totalSatisfied + totalMissing) + ' matches');

                element.append('div')
                    .classed('collapse', true)
                    .attr('id', 'quantifierList')
                    .node()
                    .appendChild(self.listRender(ruleI, 'quantifier').node());
            }


            if (d.type === 'conditionedList') {
                element.append('p')
                    .text(d.value);
                element.append('div')
                    .classed('buttonDiv', true)
                    .append('em')
                    .classed('btn btn-success btn-xs', true)
                    .attr('data-toggle', 'collapse')
                    .attr('href', '#satisfiedList')
                    .text(() => totalSatisfied + ' satisfied ');
                element
                    .append('div')
                    .classed('buttonDiv', true)
                    .append('em')
                    .classed('btn btn-danger btn-xs', true)
                    .attr('data-toggle', 'collapse')
                    .attr('href', '#violatedList')
                    .text(() => totalMissing + ' violated');
                element.append('div')
                    .style('clear', 'both');
                element.append('div')
                    .classed('collapse', true)
                    .attr('id', 'satisfiedList')
                    .node()
                    .appendChild(self.listRender(ruleI, 'satisfied').node());
                element.append('div')
                    .classed('collapse', true)
                    .attr('id', 'violatedList')
                    .node()
                    .appendChild(self.listRender(ruleI, 'violated').node());
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
 * create a list div node for quantifier and satisfied result
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
    }

    for (let i = 0; i < list.length; i++) {
        detached
            .append('div')
            .classed('partResultDiv', true)
            .datum(list[i])
            .append('pre')
            .classed('blue-bg', group === 'quantifier')
            .classed('green-bg', group === 'satisfied')
            .classed('red-bg', group === 'violated')
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
