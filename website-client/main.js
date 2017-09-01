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

        /**
         * https://stackoverflow.com/questions/710586/json-stringify-array-bizarreness-with-prototype-js
         */
        if(window.Prototype) {
            //delete Object.prototype.toJSON;
            delete Array.prototype.toJSON;
            //delete Hash.prototype.toJSON;
            //delete String.prototype.toJSON;
        }

        if (!window.WebSocket) {
            alert("FATAL: WebSocket not natively supported. This demo will not work!");
        }

        let tableOfContentManager = new TableOfContent();
        let ruleTableManager = new RuleTable();
        // let ruleGenerator = new RuleGenerator();
        let tagInformationManager = new HeaderManager();
        let individualRuleManager = new IndividualRule();

        WebSocketHandler(tableOfContentManager, ruleTableManager, individualRuleManager, tagInformationManager);


        let hashManager = new UrlChangingHandling(ruleTableManager, individualRuleManager, tagInformationManager);

        if ("onhashchange" in window) { // event supported?
            window.onhashchange = function () {
                hashManager.hashManager(window.location.hash);
            }
        }
        else { // event not supported:
            let storedHash = window.location.hash;
            window.setInterval(function () {
                if (window.location.hash != storedHash) {
                    storedHash = window.location.hash;
                    hashManager.hashManager(storedHash);
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

