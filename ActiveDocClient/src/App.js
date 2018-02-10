import React from 'react';
import './App.css';
import ReactDOM from 'react-dom';

import testClass from './testClass';
import * as chart from './Chart';
import * as d3 from 'd3';


export class App {

    thisNode;

    constructor(parent, EventAggregator) {
        this.thisNode = d3.select(parent);
        this.ea = EventAggregator;
    }

    init() {

        if (!window.WebSocket) {
            alert("FATAL: WebSocket not natively supported. This demo will not work!");
        }
        let ws = new WebSocket("ws://localhost:8887");

        ws.onmessage = function (e) {
            let message = JSON.parse(e.data);
            console.log(message);
        };

        return this.build();
    }

    build() {

        let ws = new WebSocket("ws://localhost:8887");

        this.thisNode.append('div').attr('id','testDiv');
        ReactDOM.render(
            React.createElement(testClass),
            document.getElementById('testDiv')
        );

        chart.create(this.thisNode.node());
    }

}

/**
 * Factory method to create a new app instance
 * @param parent
 * @returns {App}
 */
export function create(parent) {
    return new App(parent);
}
