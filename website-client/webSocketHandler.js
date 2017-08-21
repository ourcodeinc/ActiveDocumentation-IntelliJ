/**
 * Created by saharmehrpour on 8/1/17.
 */

function WebSocketHandler(tableOfContentManager, ruleTableManager, individualRuleManager, tagInformationManager) {

    let xml = []; // object of `filePath` and `xml`
    let ruleTable = []; // retrieved from ruleJson.txt
    let tagTable = []; // retrieved from tagJson.txt
    let ws = new WebSocket("ws://localhost:8887");
    let filtered;

    ruleTableManager.setWS(ws);
    individualRuleManager.setWS(ws);
    tagInformationManager.setWS(ws);

    ws.onopen = function () {
        ruleTableManager.resetRuleTable();
    };

    function setAllRules(rules) {
        tableOfContentManager.setRules(rules);
        ruleTableManager.setRules(rules);
        individualRuleManager.setRules(rules);
    }

    function setAllTags(tags) {
        tagInformationManager.setTags(tags);
    }

    ws.onmessage = function (e) {
        let message = JSON.parse(e.data);

        switch (message.command) {

            // when the ruleJson.txt is changed
            // followed by VERIFY_RULES
            case "RULE_TABLE":
                eval(message.data);
                setAllRules(ruleTable);
                break;

            // when the tagJson.txt is changed
            // followed by VERIFY_RULES
            case "TAG_TABLE":
                eval(message.data);
                setAllTags(tagTable);
                break;

            case "VERIFY_RULES":
                ruleTableManager.resetRuleTable();
                tableOfContentManager.clearTableOfContent();
                tableOfContentManager.displayTableOfContent();

                ruleTable = verifyRules(xml, ruleTable);
                setAllRules(ruleTable);
                ruleTableManager.displayRules();

                break;

            // when the code changes
            // after UPDATE_XML
            case "CHECK_RULES_FOR_FILE":
                ruleTable = checkRules(xml, ruleTable, message.data);
                setAllRules(ruleTable);
                ruleTableManager.updateDisplayRules(message.data);

                location.hash = "#/codeChanged";
                break;

            // send initially on open
            case "XML":
                xml.push(message.data);
                break;

            // followed by CHECK_RULES_FOR_FILE
            case "UPDATE_XML":
                filtered = xml.filter((d) => d.filePath === message.data['filePath']);

                if (filtered.length === 0)
                    xml.push({'filePath': message.data['filePath'], 'xml': message.data['xml']});
                else
                    filtered[0].xml = message.data['xml'];

                break;

            // tagName and tag
            case "UPDATE_TAG":
                let newTag = JSON.parse(message.data);
                filtered = tagTable.filter((d) => d.tagName === newTag['tagName']);
                if (filtered.length === 0)
                    tagTable.push(newTag);
                else
                    filtered[0].detail = newTag['detail'];

                setAllTags(tagTable);
                tableOfContentManager.clearTableOfContent();
                tableOfContentManager.displayTableOfContent();
                ruleTableManager.resetRuleTable();
                ruleTableManager.displayRules();
                location.hash = `#/tag/${newTag['tagName']}`;
                break;

            // ruleIndex and rule
            case "UPDATE_RULE":
                let newRule = JSON.parse(message.data['rule']);
                filtered = ruleTable.filter((d) => d.index === +message.data['ruleIndex']);
                if (filtered.length === 0)
                    ruleTable.push(newRule);
                else
                    filtered[0] = newRule;

                setAllRules(ruleTable);
                tableOfContentManager.clearTableOfContent();
                tableOfContentManager.displayTableOfContent();
                ruleTableManager.resetRuleTable();
                ruleTableManager.displayRules();
                break;

            case "ENTER":
            case "LEFT":
            default:
        }

    };

}


