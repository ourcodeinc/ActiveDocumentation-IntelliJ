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

                ruleTable = verifyRules(xml, ruleTable);

                // console.log("start", ruleTable);

                tableOfContentManager.setRules(ruleTable);
                ruleTableManager.setRules(ruleTable);
                ruleTableManager.displayRules();

                break;

            // when the code changes
            case "CHECK_RULES":

                // console.log("before", ruleTable);

                ruleTable = checkRules(xml, ruleTable, message.data);

                // console.log("after", ruleTable);

                tableOfContentManager.setRules(ruleTable);
                ruleTableManager.setRules(ruleTable);
                ruleTableManager.updateDisplayRules(message.data);

                location.hash = "#/codeChanged";

                break;

            case "XML":
                xml.push(messageInfo);

                //let parser = new DOMParser();
                //console.log(parser.parseFromString(xml, "text/xml"));
                break;

            case "UPDATE_XML":
                xml.filter((d)=>
                    d.filePath === message.data['filePath']
                )[0].xml = message.data['xml'];

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


