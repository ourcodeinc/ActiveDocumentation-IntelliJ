/**
 * Created by saharmehrpour on 8/9/17.
 */

/**
 * @constructor
 */
function HeaderManager() {
    this.div = d3.select('#tagInfo');
}


/**
 * set the tag Table
 * @param tagTable
 */
HeaderManager.prototype.setTags = function (tagTable) {
    this.tags = tagTable;
};


/**
 * set the variable 'ws' the webSocket
 * @param webSocket
 */
HeaderManager.prototype.setWS = function (webSocket) {
    this.ws = webSocket;
};


/**
 * display the information of a specific tag
 * @param tagNames list of tag names
 */
HeaderManager.prototype.displayTagInformation = function (tagNames) {

    this.div.selectAll('div').remove();
    d3.select("#page_title").html("Tag <br><small>" + tagNames.join(", ") + "</small>");

    for (let i = 0; i < tagNames.length; i++) {
        let tag = this.tags.filter((d) => {
            return d['tagName'] === tagNames[i]
        })[0];

        let tagDivDiv = this.div
            .datum(tag)
            .append('div');

        let span = tagDivDiv.append('div')
            .append('span');

        span.append("textarea")
            .attr("spellcheck", false)
            .classed('form-control', true)
            .attr('rows', "5")
            .attr('id', `tag_${tag['tagName']}`)
            .on("change", () => this.updateTags(tag['tagName']))
            .text(tag.detail);

        tagDivDiv.append('hr');
    }
};


/**
 * update the tag information and send to server
 * @param tagName
 */
HeaderManager.prototype.updateTags = function (tagName) {
    for (let i = 0; i < this.tags.length; i++) {
        if (this.tags[i]['tagName'] === tagName) {
            this.tags[i].detail = document.getElementById(`tag_${tagName}`).value;

            sendToServer(this.ws, "MODIFIED_TAG", this.tags[i]);
            return;
        }
    }
};
