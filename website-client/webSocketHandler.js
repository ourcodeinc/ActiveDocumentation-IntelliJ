/**
 * Created by saharmehrpour on 8/1/17.
 */

function WebSocketHandler(tableOfContentManager, ruleTableManager, individualRuleManager, tagInformationManager) {

    let xml = []; // object of `filePath` and `xml`
    let ruleTable = []; // retrieved from ruleJson.txt
    let tagTable = []; // retrieved from ruleJson.txt
    let ws = new WebSocket("ws://localhost:8887");

    ruleTableManager.setWS(ws);
    individualRuleManager.setWS(ws);
    tagInformationManager.setWS(ws);

    ws.onopen = function () {
        ruleTableManager.resetRuleTable();
    };

    ws.onmessage = function (e) {

        let message = JSON.parse(e.data);

        switch (message.command) {

            // when the rules are changed
            case "RULE_TABLE":
                eval(message.data);

                tableOfContentManager.setRules(ruleTable);
                ruleTableManager.setRules(ruleTable);
                individualRuleManager.setRules(ruleTable);
                break;

            case "TAG_TABLE":
                eval(message.data);
                tagInformationManager.setTags(tagTable);
                break;

            case "VERIFY_RULES":

                ruleTableManager.resetRuleTable();
                tableOfContentManager.clearTableOfContent();
                tableOfContentManager.displayTableOfContent();

                ruleTable = verifyRules(xml, ruleTable);

                // console.log("start", ruleTable);

                tableOfContentManager.setRules(ruleTable);
                ruleTableManager.setRules(ruleTable);
                individualRuleManager.setRules(ruleTable);

                ruleTableManager.displayRules();

                break;

            // when the code changes
            case "CHECK_RULES":

                // console.log("before", ruleTable);

                ruleTable = checkRules(xml, ruleTable, message.data);

                // console.log("after", ruleTable);

                tableOfContentManager.setRules(ruleTable);
                ruleTableManager.setRules(ruleTable);
                individualRuleManager.setRules(ruleTable);

                ruleTableManager.updateDisplayRules(message.data);

                location.hash = "#/codeChanged";

                break;

            case "XML":
                xml.push(message.data);

                //let parser = new DOMParser();
                //console.log(parser.parseFromString(xml, "text/xml"));
                break;

            case "UPDATE_XML":
                xml.filter((d) =>
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


