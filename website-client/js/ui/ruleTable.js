/**
 * Created by saharmehrpour on 8/1/17.
 */

function RuleTable() {
    this.div = d3.select("#ruleResults");
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
        this.displayResult(this.rules[i].index);
    }
};


/**
 * display the data in the browser
 * rules: ruleJson + initialResult[] + conditionResult[] + match T/F
 * @param ruleIndex
 */
RuleTable.prototype.displayResult = function (ruleIndex) {

    let self = this;

    let displayData = this.rules.filter((d) => {
        return d.index === ruleIndex
    })[0];

    let ruleDiv = d3.select("#RT")
        .append('div')
        .classed('paddedDiv ruleContainer', true)
        .attr('id', `rule_result_${ruleIndex}`)
        .attr('data-tags', displayData.tags)
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
        .on("change", () => this.updateRules(this.rules, displayData.index));

    span.append('br');
    span.append('textarea')
        .text(displayData.detail)
        .attr("spellcheck", false)
        .attr("id", `rule_detail_${displayData.index}`)
        .classed('ruleDetail', true)
        .on("change", () => this.updateRules(this.rules, displayData.index));

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
        .appendChild(self.listRender(displayData, 'quantifier').node());

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
        .appendChild(self.listRender(displayData, 'satisfied').node());


    // fixing the float div heights and widths
    ruleResultDiv.append('div')
        .style('clear', 'both');

};


/**
 * update display of all rules
 */
RuleTable.prototype.updateDisplayRules = function () {

    for (let i = 0; i < this.rules.length; i++) {
        this.updateDisplayResult(this.rules[i].index);
    }
};


/**
 * update the vis after a change in the code
 * @param ruleIndex
 */
RuleTable.prototype.updateDisplayResult = function (ruleIndex) {

    let self = this;

    let displayData = self.rules.filter((d) => {
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
            .appendChild(self.listRender(displayData, 'quantifier').node());

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
            .appendChild(self.listRender(displayData, 'satisfied').node());

    }

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
 * create a list div node for quantifier and conditioned result
 * @param data
 * @param group
 * @returns {string}
 */
RuleTable.prototype.listRender = function (data, group) {

    let self = this;

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
                        self.sendToServer("xmlResult", data.quantifierResult[i]['xml'])
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
                        self.sendToServer("xmlResult", data.conditionedResult[i]['xml'])
                    });
            }
            break;
    }
    return detached;
}

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