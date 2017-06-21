
function navBarHandler() {
    d3.select("#link_generate_rules").on("click", () => {
        d3.selectAll(".main").classed("hidden", true);
        d3.select("#ruleGeneration").classed("hidden", false);
    });

    d3.select("#link_rule_result").on("click", () => {
        d3.selectAll(".main").classed("hidden", true);
        d3.select("#connection").classed("hidden", false);
        d3.select("#ruleResults").classed("hidden", false);
    });
}


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

        connectionManager();

        navBarHandler();

        initRuleGenerator();

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

