/**
 * Created by saharmehrpour on 8/1/17.
 */

function UrlChangingHandling() {

    this.history = [];

    this.navBarHandler();

}

/**
 * This class updates the view based on changes in Hash address
 * @param hash
 */
UrlChangingHandling.prototype.hashChangedHandler = function (hash) {

    let self = this;

    if (self.activeHash === self.history.length - 1 || self.history.length === 0) {
        self.history.push(hash);
        self.activeHash = self.history.length - 1;
        d3.select('#back_button').classed('inactive', false);
    }

    document.body.scrollTop = document.documentElement.scrollTop = 0;

    d3.selectAll(".main_view").classed("hidden", true);
    d3.select("#model_nav").classed("hidden", true);

    let splittedHash = hash.split("/");

    switch (splittedHash[1]) {
        case 'index':
            d3.selectAll(".main").classed("hidden", true);
            d3.select("#header_2").classed("hidden", true);
            d3.select("#tableOfContent").classed("hidden", false);
            break;

        case 'rules':
            d3.selectAll(".main").classed("hidden", true);
            d3.select("#header_2").classed("hidden", false);
            d3.select("#ruleResults").classed("hidden", false);
            break;

        case 'tags':
            document.getElementById(`page_title`).value = splittedHash[2];
            this.titleChangeHandler();
            break;

        case 'ruleGenerating':
            d3.selectAll(".main").classed("hidden", true);
            d3.select("#header_2").classed("hidden", true);
            d3.select("#ruleGeneration").classed("hidden", false);
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

    d3.select(`#page_title`).on("change", () => this.titleChangeHandler());

    d3.select("#back_button").on("click", () => {
        if (self.activeHash > 0) {
            self.activeHash = self.activeHash - 1;
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
            location.hash = self.history[self.activeHash];
            d3.select('#back_button').classed('inactive', false);
        }
        if (self.activeHash === self.history.length - 1) {
            d3.select('#forward_button').classed('inactive', true);
        }
    });
};

/**
 * event handler for when the title[tag] is changed
 */
UrlChangingHandling.prototype.titleChangeHandler = function () {
    let targetTag = document.getElementById(`page_title`).value.split(" ");

    if (targetTag[0] === 'All') {
        d3.selectAll(`.ruleContainer`).classed('hidden', false);
    }
    else {
        d3.selectAll(`.ruleContainer`).classed('hidden', true);
        d3.selectAll(`.ruleContainer`)
            .classed('hidden', function () {
                let tags = d3.select(this).attr('data-tags').split(',');
                return !arrayContains(tags, targetTag)
            });
    }

    d3.selectAll(".main").classed("hidden", true);
    d3.select("#header_2").classed("hidden", false);
    d3.select("#ruleResults").classed("hidden", false);

};