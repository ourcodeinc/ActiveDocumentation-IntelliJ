/**
 * Created by saharmehrpour on 6/26/17.
 */

/**
 * check whether two arrays are equals
 * @param array1
 * @param array2
 * @returns {boolean}
 */
function arraysEqual(array1, array2) {

    let arr1 = array1.slice(0);
    let arr2 = array2.slice(0);

    if (arr1.length !== arr2.length)
        return false;
    for (let i = arr2.length; i--;) {
        if (arr1.indexOf(arr2[i]) === -1)
            return false;
        arr1.splice(arr1.indexOf(arr2[i]), 1)
    }
    return true;
}

/**
 * check whether one arrays contains all elements of the other array
 * @param container
 * @param arr
 * @returns {boolean}
 */
function arrayContains(container, arr) {
    let arrContainer = container.slice(0);

    for (let i = arr.length; i--;) {
        if (arrContainer.indexOf(arr[i]) === -1)
            return false;
        arrContainer.splice(arrContainer.indexOf(arr[i]), 1)
    }
    return true;
}

/**
 * deep copy of an xml variable
 * @param xml
 * @returns {Document}
 */
function cloneXML(xml) {
    let newDocument = xml.implementation.createDocument(
        xml.namespaceURI, //namespace to use
        "",                     //name of the root element (or for empty document)
        null                      //doctype (null for XML)
    );
    let newNode = newDocument.importNode(
        xml.documentElement, //node to import
        true                         //clone its descendants
    );
    newDocument.appendChild(newNode);

    return newDocument;
}

function cloneJSON(json) {
    return JSON.parse(JSON.stringify(json));
}

/**
 * find the 'unit' parent of the node
 * @param node
 * @returns {*}
 */
function findUnitNode(node) {
    if (node.nodeName === 'unit')
        return node;
    else
        return findUnitNode(node.parentNode);
}