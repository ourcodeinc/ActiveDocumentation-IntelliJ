/**
 * Created by saharmehrpour on 8/24/17.
 */

import React, {Component} from 'react';
// import logo from './logo.svg';
import './App.css';

// import ReactDOM from 'react-dom';

export class testClass extends Component {

    render() {
        return (<h5>Testing the outer classes.</h5>)
    }

    componentDidMount() {
        console.log("testing componentDidMount");
    }
}


export default testClass;
