let ws;
let xml;
let rules;
let ruleTable = [];


function connectionManager() {

    //d3.select("#connect").on("click", () => {
    ws = new WebSocket("ws://localhost:8887");
    ws.onopen = function () {
        clearRuleTable();
    };
    ws.onmessage = function (e) {

        let message = JSON.parse(e.data);
        // console.log(message);

        let messageInfo = message.data;

        switch (message.command) {

            case "UPDATE_RULE_TABLE_AND_CONTAINER":
                rules = eval(JSON.parse(message.data).text);
                break;

            case "VERIFY_RULES":
                clearRuleTable();
                for (let i = 0; i < ruleTable.length; i++) {
                    runXPathQuery(xml, ruleTable[i]);
                }
                break;

            case "XML":
                xml = messageInfo;

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

    // ws.onclose = function () {
    //     $("disconnect").disable();
    //     $("connect").enable();
    //     ws = null;
    // };
    // //$("uri", "connect").invoke("disable");
    // $("disconnect").enable();
    // $("connect").disable();
    // });
    //
    // d3.select("#disconnect").on("click", () => {
    //     if (ws) {
    //         ws.close();
    //         ws = null;
    //         $("disconnect").disable();
    //         $("connect").enable();
    //     }
    // });
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