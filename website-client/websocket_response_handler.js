/**
 * Modified the file by @saharmehrpour
 */

let ws;
let xml;

// Unfortunately the intelliJ plugin doesn't understand global variables.
// So we need to pass on the variable to functions and update the variable
// by processing the returned value
let ruleTable = [];

function connectionManager() {

    ws = new WebSocket("ws://localhost:8887");
    ws.onopen = function () {
        clearRuleTable();
    };
    ws.onmessage = function (e) {

        let message = JSON.parse(e.data);
        // console.log(message);

        let messageInfo = message.data;

        switch (message.command) {

            // when the rules are changed
            case "UPDATE_RULE_TABLE_AND_CONTAINER":
                eval(JSON.parse(message.data).text);

                break;

            case "VERIFY_RULES":
                clearRuleTable();
                clearTableOfContent();
                displayTableOfContent(ruleTable);
                d3.select(`#page_title`).on("change", () => managePageTitleChange(ruleTable));
                document.getElementById(`page_title`).value = 'All rules';

                for (let i = 0; i < ruleTable.length; i++) {
                    ruleTable = runXPathQuery(xml, ruleTable, ruleTable[i].index);
                    displayResult(ruleTable, ruleTable[i].index);
                }



                break;

            // when the code changes
            case "CHECK_RULES":

                for (let i = 0; i < ruleTable.length; i++) {
                    ruleTable = checkRules(xml, ruleTable, ruleTable[i].index);
                }

                console.log(ruleTable);

                break;

            case "XML":
                xml = messageInfo;

                // test
                let parser = new DOMParser();
                console.log(parser.parseFromString(xml, "text/xml"));
                break;

            case "ENTER":
            case "LEFT":
                //console.log(message.data);
                break;

            default:
            //console.log("Oops. Invalid command: " + message.command);
            // don't know what to do with this type of message
        }

    };

}


/**
 * send the message to the server
 * @param command without quotation
 * @param data with quotation
 */
function sendToServer(command, data) {

    if (ws) {
        let message = "{\"source\":\"WEB\",\"destination\":\"IDEA\",\"command\":\"" + command + "\",\"data\":"
            + data + "}";

        //console.log(message);

        ws.send(message.toString());
    }
}