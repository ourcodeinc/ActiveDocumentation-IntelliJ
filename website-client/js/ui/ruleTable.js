/**
 * Created by saharmehrpour on 8/1/17.
 */

function RuleTable() {
    this.div = d3.select("#ruleResults");

    d3.select("#page_title")
        .on("change", () => {
            let targetTag = document.getElementById(`page_title`).value.split(" ");
            location.hash = '#/tags/' + targetTag.join('+');
        });
}


RuleTable.prototype.setRules = function (ruleList) {
    this.rules = ruleList;
};

RuleTable.prototype.setWS = function (webSocket) {
    this.ws = webSocket;
};

/**
 * clear the table in the browser
 */
RuleTable.prototype.clearRuleTable = function () {
    d3.select("#ruleResults")
        .select("#RT").selectAll('div').remove();
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

    let ruleDiv = d3.select("#RT")
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
 * update display of all rules
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

    let ruleDiv = d3.select("#RT")
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

    let self = this;

    let data = []; // TODO remove unnecessary attributes

    for (let i = 0; i < self.rules.length; i++) {
        if (self.rules[i].index === index) {

            let modifiedRule = cloneJSON(self.rules[i]);

            modifiedRule.ruleDescription = document.getElementById(`rule_desc_${index}`).value;
            modifiedRule.detail = document.getElementById(`rule_detail_${index}`).value;

            data.push(modifiedRule);
        }
        else {
            data.push(self.rules[i])
        }
    }

    let ruleTableString = "\"ruleTable=" + JSON.stringify(data).substr(1);
    this.sendToServer("NewRule", ruleTableString);

};


/**
 * remove the changes caused by updateDisplayRules
 */
RuleTable.prototype.cleanRuleTable = function () {

    this.div.selectAll('.partResultDiv, .ruleContainer')
        .classed('hidden', false);

    let ruleDiv = d3.select("#RT")
        .selectAll('.ruleDiv')
        .classed('changed', false)
        .classed('hidden', false);

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

    d3.selectAll(".main").classed("hidden", true);
    d3.select("#header_2").classed("hidden", false);
    d3.select("#ruleResults").classed("hidden", false);

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
                self.sendToServer("xmlResult", list[i]['xml'])
            });
    }

    return detached;
};

/**
 * send the message to the server
 * @param command without quotation
 * @param data with quotation
 */
RuleTable.prototype.sendToServer = function (command, data) {

    if (this.ws) {
        let message = "{\"source\":\"WEB\",\"destination\":\"IDEA\",\"command\":\"" + command + "\",\"data\":"
            + data + "}";

        //console.log(message);

        this.ws.send(message.toString());
    }
};