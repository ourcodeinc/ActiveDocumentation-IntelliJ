let ws;

document.observe("dom:loaded", function() {
    function log(text) {
        $("log").innerHTML = (new Date).getTime() + ": " + (!Object.isUndefined(text) && text !== null ? text.escapeHTML() : "null") + $("log").innerHTML;
    }

    if (!window.WebSocket) {
        alert("FATAL: WebSocket not natively supported. This demo will not work!");
    }

    // let ws;

    $("uriForm").observe("submit", function (e) {
        e.stop();
        ws = new WebSocket($F("uri"));
        ws.onopen = function () {
            log("[WebSocket#onopen]\n");
        };
        ws.onmessage = function (e) {
            // log("[WebSocket#onmessage] Message: '" + e.data + "'\n"); // CONTROLS WHETHER MESSAGES ARE DISPLAYED OR NOT\

            let message = JSON.parse(e.data);
            console.log(message.command);
            console.log(message);

            let messageInfo = message.data;
            let targetFileName = messageInfo.canonicalPath;
            let curr = projectHierarchy;

            switch(message.command) {
                case "INITIAL_PROJECT_HIERARCHY":
                    projectHierarchy = message.data;
                    addParentPropertyToNodes(projectHierarchy);
                    console.log("initial project hierarchy added");
                    break;

                case "UPDATE_RULE_TABLE_AND_CONTAINER":
                    console.log("In UPDATE_RULE_TABLE_AND_CONTAINER");
                    //console.log(message.data.text);
                    eval(message.data.text);

                    console.log("Test in UPDATE_RULE_TABLE_AND_CONTAINER");
                    console.log(runRules());

                    break;

                case "UPDATE_CODE_IN_FILE":

                    messageInfo = message.data;
                    // console.log("1");
                    targetFileName = messageInfo.canonicalPath;
                    // console.log("2");
                    curr = projectHierarchy;
                    // console.log("3");

                    while (!(curr.properties.canonicalPath === targetFileName)) {

                        let i;
                        for (i = 0; i < curr.children.length; i++) {
                            if (targetFileName.startsWith(curr.children[i].properties.canonicalPath)) {
                                curr = curr.children[i];
                                break;
                            }
                        }
                    }

                    curr.properties.code = messageInfo.code;
                    curr.properties.ast = messageInfo.ast;
                    addParentPropertyToNodes(curr); // update parent relationships again
                    console.log("Code updated in " + curr.properties.canonicalPath);

                    break;

                case "INITIAL_PROJECT_CLASS_TABLE":
                    projectClassTable = message.data;
                    console.log("Added initial project class table.");
                    break;

                case "UPDATE_PROJECT_CLASS_TABLE":
                    let newClassTable = message.data;

                    let property;
                    for (property in newClassTable) {
                        if (newClassTable.hasOwnProperty(property)) {
                            // console.log(property);
                            projectClassTable[property] = newClassTable[property];
                        }
                    }

                    console.log("Updated project class table.");
                    console.log(runRules());
                    break;

                case "DELETE_FILE":

                    messageInfo = message.data;
                    targetFileName = messageInfo.canonicalPath;
                    curr = projectHierarchy;

                    while (curr != null && !(curr.properties.canonicalPath === targetFileName)) {
                        let i;
                        for (i = 0; i < curr.children.length; i++) {
                            if (targetFileName === curr.children[i].properties.canonicalPath) {
                                curr.children.splice(i, 1);
                                curr = null;
                                break;
                            } else if (targetFileName.startsWith(curr.children[i].properties.canonicalPath)) {
                                curr = curr.children[i];
                                break;
                            }
                        }
                    }

                    console.log("Deleted item at " + targetFileName);
                    console.log(runRules());
                    break;

                case "RENAME_FILE":

                    messageInfo = message.data;
                    targetFileName = messageInfo.oldCanonicalPath;
                    curr = projectHierarchy;

                    while (!(curr.properties.canonicalPath === targetFileName)) {
                        let i;
                        for (i = 0; i < curr.children.length; i++) {
                            if (targetFileName.startsWith(curr.children[i].properties.canonicalPath)) {
                                curr = curr.children[i];
                                break;
                            }
                        }
                    }

                    curr.properties.name = messageInfo.name;
                    curr.properties.canonicalPath = messageInfo.newCanonicalPath;
                    console.log("Renamed " + targetFileName + " to " + curr.properties.canonicalPath);
                    break;

                case "CREATE_FILE":

                    messageInfo = message.data;
                    targetFileName = messageInfo.parent;
                    curr = projectHierarchy;

                    while (!(curr.properties.canonicalPath === targetFileName)) {
                        console.log(curr.properties.canonicalPath);
                        let i;
                        for (i = 0; i < curr.children.length; i++) {
                            if (targetFileName.startsWith(curr.children[i].properties.canonicalPath)) {
                                curr = curr.children[i];
                                break;
                            }
                        }
                    }

                    curr.children.push(messageInfo);
                    addParentPropertyToNodes(curr); // update parent relationships again
                    console.log("created file at " + messageInfo.canonicalPath);
                    console.log(runRules());
                    break;

                default:
                    console.log("Oops.");
                    console.log(message.command);
                   // don't know what to do with this type of message

            }

        };

        ws.onclose = function () {
            log("[WebSocket#onclose]\n");
            $("uri", "connect").invoke("enable");
            $("disconnect").disable();
            ws = null;
        };
        $("uri", "connect").invoke("disable");
        $("disconnect").enable();
    });

    $("sendForm").observe("submit", function (e) {
        e.stop();
        if (ws) {
            let textField = $("textField");
            ws.send(textField.value);
            log("[WebSocket#send]      Send:    '" + textField.value + "'\n");
            textField.value = "";
            textField.focus();
        }
    });

    $("disconnect").observe("click", function (e) {
        e.stop();
        if (ws) {
            ws.close();
            ws = null;
        }
    });
});
