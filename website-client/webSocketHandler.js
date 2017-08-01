/**
 * Created by saharmehrpour on 8/1/17.
 */

function WebSocketHandler(tableOfContentManager, ruleTableManager) {
    
    let xml = []; // object of `filePath` and `xml`
    let ruleTable = [];
    let ws = new WebSocket("ws://localhost:8887");

    ruleTableManager.setWS(ws);

    ws.onopen = function () {
        ruleTableManager.clearRuleTable();
    };

    ws.onmessage = function (e) {

        let message = JSON.parse(e.data);
        let messageInfo = message.data;

        switch (message.command) {

            // when the rules are changed
            case "UPDATE_RULE_TABLE_AND_CONTAINER":
                eval(JSON.parse(message.data).text);

                tableOfContentManager.setRules(ruleTable);
                ruleTableManager.setRules(ruleTable);

                break;

            case "VERIFY_RULES":

                ruleTableManager.clearRuleTable();
                tableOfContentManager.clearTableOfContent();
                tableOfContentManager.displayTableOfContent();

                //document.getElementById(`page_title`).value = 'All rules'; // TODO replace by hash

                for (let i = 0; i < ruleTable.length; i++) {
                    ruleTable = runXPathQuery(xml, ruleTable, ruleTable[i].index);
                }

                tableOfContentManager.setRules(ruleTable);
                ruleTableManager.setRules(ruleTable);
                ruleTableManager.displayRules();


                break;

            // when the code changes
            case "CHECK_RULES":
                for (let i = 0; i < ruleTable.length; i++) {
                    ruleTable = checkRules(xml, ruleTable, ruleTable[i].index);
                }

                tableOfContentManager.setRules(ruleTable);
                ruleTableManager.setRules(ruleTable);
                ruleTableManager.updateDisplayRules();

                break;

            case "XML":
                xml.push(messageInfo);

                //let parser = new DOMParser();
                //console.log(parser.parseFromString(xml, "text/xml"));
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


