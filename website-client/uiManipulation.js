/**
 * Created by saharmehrpour on 7/6/17.
 */


/**
 * clear the table in the browser
 */
function clearRuleTable() {
    d3.select("#RT").selectAll('div').remove();
}


/**
 * display the data in the browser
 * @param data: rulJson + initialResult[] + conditionResult[] + match T/F
 */
function displayResult(data) {

    let tooltip = d3.select('.tooltip');

    let ruleDiv = d3.select("#RT")
        .append('div')
        .classed('paddedDiv', true)
        .append('div')
        .classed('ruleDiv', true);

    ruleDiv.append('div')
        .classed('paddedDiv', true)
        .on('click', function () {
            let node = d3.select(this.nextSibling)/*.select('.ruleResult')*/;
            node.classed('hidden', !node.classed('hidden'));
        })
        .append('div')
        .classed('ruleTitle', true)
        .html(() => {
            return `<p><b>${data.ruleDescription}</b></p><p>${data.detail}</p>`
        });

    let ruleResultDiv = ruleDiv.append('div')
        .classed('ruleResult hidden', true);

    let quantifierDiv = ruleResultDiv.append('div')
        .classed('quantifierDiv', true);
    quantifierDiv.append('p')
        .text(data.quantifierTitle);
    quantifierDiv.append('em')
        .classed('link matches', true)
        .text(() => {
            return (data.satisfied + data.missing) + ' matches'
        })
        .on("click", function () {
            let parentNode = d3.select(this.parentNode).select('.quantifierList');
            parentNode.classed('hidden', !parentNode.classed('hidden'));
        });
    quantifierDiv.append('div')
        .classed('quantifierList hidden', true)
        .node()
        .appendChild(listRender(data, 'quantifier').node());


    let conditionDiv = ruleResultDiv.append('div')
        .classed('conditionDiv', true);
    conditionDiv.append('p')
        .text(data.conditionedTitle);
    let p = conditionDiv.append('p');
    p.append('em')
        .classed('link satisfied', true)
        .text(() => {
            return data.satisfied + ' satisfied ';
        })
        .on('click', function () {
            let parentNode = d3.select(this.parentNode.parentNode).select('.conditionList');
            parentNode.classed('hidden', !parentNode.classed('hidden'));
        });
    p.append('em')
        .classed('missing', true)
        .text(() => {
            return data.missing + ' missing';
        });
    conditionDiv.append('div')
        .classed('conditionList hidden', true)
        .node()
        .appendChild(listRender(data, 'satisfied').node());


    // fixing the float div heights and widths
    ruleResultDiv.append('div')
        .style('clear', 'both');

}

/**
 * create a list div node
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


