/**
 * Created by saharmehrpour on 8/1/17.
 */

/**
 * @param ruleTableManager
 * @param individualRuleManager
 * @param tagInformationManager
 * @constructor
 */
function UrlChangingHandling(ruleTableManager, individualRuleManager, tagInformationManager) {

    this.history = [];
    this.clicked = false;
    this.activeHash = -1;

    this.ruleTableManager = ruleTableManager;
    this.individualRuleManager = individualRuleManager;
    this.tagInformationManager = tagInformationManager;

    this.navBarHandler();

}

/**
 * This class updates the view based on changes in Hash address
 * @param hash
 */
UrlChangingHandling.prototype.hashChangedHandler = function (hash) {

    let self = this;

    self.updateHistory(hash);

    document.body.scrollTop = 0;
    document.documentElement.scrollTop = 0;

    d3.selectAll(".main").classed("hidden", true);
    d3.select("#model_nav").classed("hidden", true);
    d3.select("#tagInfo").classed("hidden", true);

    let splittedHash = hash.split("/");

    switch (splittedHash[1]) {
        case 'index':
            d3.selectAll(".main").classed("hidden", true);
            d3.select("#header_2").classed("hidden", true);
            d3.select("#tableOfContent").classed("hidden", false);
            break;

        case 'tag':
            d3.select("#header_2").classed("hidden", false);
            d3.select("#ruleResults").classed("hidden", false);
            d3.select("#tagInfo").classed("hidden", false);
            this.tagInformationManager.displayTagInformation(splittedHash[2].split('+'));
            this.ruleTableManager.updateTagRules();
            break;

        case 'ruleGenerating':
            d3.selectAll(".main").classed("hidden", true);
            d3.select("#header_2").classed("hidden", true);
            d3.select("#ruleGeneration").classed("hidden", false);
            break;

        case 'rules':
            this.ruleTableManager.cleanRuleTable();
            d3.selectAll(".main").classed("hidden", true);
            d3.select("#header_2").classed("hidden", false);
            d3.select("#ruleResults").classed("hidden", false);
            break;

        case 'codeChanged':
            d3.selectAll(".main").classed("hidden", true);
            d3.select("#header_2").classed("hidden", false);
            d3.select("#ruleResults").classed("hidden", false);
            break;

        case 'rule':
            d3.selectAll(".main").classed("hidden", true);
            d3.select("#header_2").classed("hidden", false);
            d3.select("#individualRule").classed("hidden", false);
            this.individualRuleManager.displayRule(+splittedHash[2]);
            break;

    }

};

/**
 * adding listeners to tabs on the nav bar
 */
UrlChangingHandling.prototype.navBarHandler = function () {
    let self = this;

    d3.select("#link_generate_rules").on("click", () => {
        location.hash = '#/ruleGenerating';
    });

    d3.select("#link_rule_result").on("click", () => {
        location.hash = '#/rules';
    });

    d3.select("#link_lists").on("click", () => {
        location.hash = '#/index';
    });

    d3.select("#back_button").on("click", () => {
        if (self.activeHash > 0) {
            self.activeHash = self.activeHash - 1;
            self.clicked = true;

            location.hash = self.history[self.activeHash];
            d3.select('#forward_button').classed('inactive', false);
        }
        if (self.activeHash === 0) {
            d3.select('#back_button').classed('inactive', true);
        }
    });

    d3.select("#forward_button").on("click", () => {
        if (self.activeHash < self.history.length - 1) {
            self.activeHash = self.activeHash + 1;
            self.clicked = true;

            location.hash = self.history[self.activeHash];
            d3.select('#back_button').classed('inactive', false);
        }
        if (self.activeHash === self.history.length - 1) {
            d3.select('#forward_button').classed('inactive', true);
        }
    });
};


/**
 * up date the hash list and 'active hash'
 * @param hash
 */
UrlChangingHandling.prototype.updateHistory = function (hash) {
    let self = this;

    if (!self.clicked) {
        if (self.history.length - 1 > self.activeHash) {
            for (let i = self.history.length - 1; i > self.activeHash; i--)
                console.log(self.history.pop());
        }
        self.history.push(hash);
        self.activeHash += 1;
        d3.select('#back_button').classed('inactive', self.activeHash === 0);
        d3.select('#forward_button').classed('inactive', true);
    }
    self.clicked = false;

};
