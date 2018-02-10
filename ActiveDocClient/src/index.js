// import React from 'react';
// import ReactDOM from 'react-dom';
import './index.css';
import {create} from './App';
import registerServiceWorker from './registerServiceWorker';

//ReactDOM.render(<App />, document.getElementById('root'));

// New
const parent = document.querySelector('#root');
create(parent).init();

registerServiceWorker();


