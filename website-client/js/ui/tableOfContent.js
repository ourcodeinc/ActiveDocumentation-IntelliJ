/**
 * Created by saharmehrpour on 8/1/17.
 */

function TableOfContent() {
    this.div = d3.select("#tableOfContent");
}

TableOfContent.prototype.setRules = function (ruleList) {
    this.rules = ruleList;
};

/**
 * clear the table of content
 */
TableOfContent.prototype.clearTableOfContent = function () {
    this.div
        .select("#rules_list")
        .selectAll('li').remove();

    this.div
        .select("#tags_list")
        .selectAll('li').remove();
};

/**
 * display the list of rules and list aof tags
 */
TableOfContent.prototype.displayTableOfContent = function () {

    let tagList = [];
    let self = this;

    for (let i = 0; i < self.rules.length; i++) {
        tagList = tagList.concat(self.rules[i].tags);
    }

    let uniqueTags = [...new Set(tagList)];

    d3.select("#rules_list").selectAll("li")
        .data(self.rules)
        .enter()
        .append('li')
        .classed('link', true)
        .html((d) => {
            return '&#9656; ' + d.ruleDescription
        })
        .on("click", (d) => {
            location.hash = `#/rules/${d.index}`;
        });

    d3.select("#tags_list").selectAll("li")
        .data(uniqueTags)
        .enter()
        .append('li')
        .classed('link', true)
        .html((d) => {
            return '&#9656; ' + d
        })
        .on("click", (d) => {
            location.hash = `#/tags/${d}`;
        });

};