let ws;
let xml;
let rules;
let ruleTable = [];


document.observe("dom:loaded", function() {

    if (!window.WebSocket) {
        alert("FATAL: WebSocket not natively supported. This demo will not work!");
    }

    $("uriForm").observe("submit", function (e) {
        e.stop();
        ws = new WebSocket("ws://localhost:8887");
        ws.onopen = function () {
            clearRuleTable();
        };
        ws.onmessage = function (e) {

            let message = JSON.parse(e.data);
            //console.log(message);

            let messageInfo = message.data;

            switch (message.command) {

                case "UPDATE_RULE_TABLE_AND_CONTAINER":
                    rules = eval(JSON.parse(message.data).text);
                    break;

                case "VERIFY_RULES":
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

        ws.onclose = function () {
            $("disconnect").disable();
            $("connect").enable();
            ws = null;
        };
        //$("uri", "connect").invoke("disable");
        $("disconnect").enable();
        $("connect").disable();
    });

    // $("sendForm").observe("submit", function (e) {
    //     e.stop();
    //     if (ws) {
    //         let textField = $("textField");
    //         //ws.send(textField.value);
    //
    //         runXPathQuery(xml, textField.value);
    //
    //         //log("[WebSocket#send]      Send:    '" + textField.value + "'\n");
    //         //textField.value = "";
    //         //textField.focus();
    //     }
    // });

    $("disconnect").observe("click", function (e) {
        e.stop();
        if (ws) {
            ws.close();
            ws = null;
            $("disconnect").disable();
            $("connect").enable();
        }
    });
});
