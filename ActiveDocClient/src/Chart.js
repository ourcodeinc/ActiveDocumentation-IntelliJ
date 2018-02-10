/**
 * Created by saharmehrpour on 8/24/17.
 */

import * as d3 from 'd3';

export class Chart {
    thisNode;

    constructor(element) {
        this.thisNode = d3.select(element)
            .append('div')
            .classed('container', true)
            .html("<h3>test chart.js</h3>");
    }


}

export function create(parent) {
    return new Chart(parent);
}