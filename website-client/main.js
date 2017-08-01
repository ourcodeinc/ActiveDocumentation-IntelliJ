/**
 * Created by saharmehrpour on 6/19/17.
 */


(function () {
    let instance = null;

    /**
     * Creates instances for every chart (classes created to handle each chart;
     * the classes are defined in the respective javascript files.
     */
    function init() {

        if (!window.WebSocket) {
            alert("FATAL: WebSocket not natively supported. This demo will not work!");
        }

        //NavBarHandler();

        let tableOfContentManager = new TableOfContent();
        let ruleTableManager = new RuleTable();
        let ruleGenerator = new RuleGenerator();

        WebSocketHandler(tableOfContentManager, ruleTableManager);


        let hashManager = new UrlChangingHandling();

        if ("onhashchange" in window) { // event supported?
            window.onhashchange = function () {
                hashManager.hashChangedHandler(window.location.hash);
            }
        }
        else { // event not supported:
            let storedHash = window.location.hash;
            window.setInterval(function () {
                if (window.location.hash != storedHash) {
                    storedHash = window.location.hash;
                    hashManager.hashChangedHandler(storedHash);
                }
            }, 100);
        }

        location.hash = "#/index";

    }

    /**
     *
     * @constructor
     */
    function Main() {
        if (instance !== null) {
            throw new Error("Cannot instantiate more than one Class");
        }
    }

    /**
     *
     * @returns {Main singleton class |*}
     */
    Main.getInstance = function () {
        let self = this;
        if (self.instance == null) {
            self.instance = new Main();

            //called only once when the class is initialized
            init();
        }
        return instance;
    };

    Main.getInstance();
})();

