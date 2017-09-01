/**
 * Created by saharmehrpour on 8/1/17.
 */

/**
 * @constructor
 */
function TableOfContent() {
    this.div = d3.select("#tableOfContent");
    this.createAlphabetIndex();
}

/**
 * Set the variable 'rules'
 * @param ruleList
 */
TableOfContent.prototype.setRules = function (ruleList) {
    this.rules = ruleList;
};


/**
 * Set the variable 'tags'
 * @param tagList
 */
TableOfContent.prototype.setTags = function (tagList) {
    this.tags = tagList;
};

/**
 * clear the table of content - items
 * called on "VERIFY_RULES"
 */
TableOfContent.prototype.clearTableOfContent = function () {
    this.div
        .select("#tags_list")
        .selectAll('li').remove();

    this.div
        .select("#rules_list")
        .selectAll('a').remove();

};

/**
 * display the list of rules and list aof tags
 */
TableOfContent.prototype.displayTableOfContent = function () {

    let self = this;

    // tag list
    let tagList = this.tags.map(function (d) {
        return d['tagName']
    });
    let uniqueTags = [...new Set(tagList)];
    uniqueTags.sort(function (a, b) {
        return d3.ascending(a, b);
    });


    d3.select("#tags_list").selectAll("li")
        .data(uniqueTags)
        .enter()
        .append('li')
        .html((d) => d)
        .on("click", (d) => {
            location.hash = `#/tag/${d}`;
        });

    d3.select("#rules_list").selectAll("a")
        .data(self.rules)
        .enter()
        .append('a')
        .classed('list-group-item', true)
        .html((d) => d.ruleDescription)
        .on("click", (d) => {
            location.hash = `#/rule/${d.index}`;
        });

};

/**
 * This function creates an alphabet list on top
 */
TableOfContent.prototype.createAlphabetIndex = function () {

    let self = this;

    let alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');
    alphabet.push('All');

    self.div.select("#alphabet_index")
        .selectAll("li")
        .data(alphabet)
        .enter()
        .append("li")
        .html(function (d) {
            return d;
        })
        .classed("selected", function (d) {
            return d === 'All';
        })
        .on("click", function (d) {
            self.div.select("#alphabet_index")
                .selectAll("li")
                .classed("selected", false);
            d3.select(this)
                .classed("selected", true);

            self.div.select("#tags_list")
                .selectAll("li")
                .style("display", function (g) {
                    if (d === "All") {
                        return null;
                    }
                    if (g.charAt(0).toUpperCase() === d)
                        return null;
                    return "none";
                });

        });
};