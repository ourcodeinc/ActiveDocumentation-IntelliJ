/**
 * Created by saharmehrpour on 8/9/17.
 */

/**
 * @constructor
 */
function TagInformation() {
    this.div = d3.select('#tagInfo');
}


/**
 * set the tag Table
 * @param tagTable
 */
TagInformation.prototype.setTags = function (tagTable) {
    this.tags = tagTable;
};


/**
 * set the variable 'ws' the webSocket
 * @param webSocket
 */
TagInformation.prototype.setWS = function (webSocket) {
    this.ws = webSocket;
};


/**
 * display the information of a specific tag
 * @param tagNames list of tag names
 */
TagInformation.prototype.displayTagInformation = function (tagNames) {

    this.div.selectAll('div').remove();

    for (let i = 0; i < tagNames.length; i++) {
        let tag = this.tags.filter((d) => {
            return d['tagName'] === tagNames[i]
        })[0];

        let tagDivDiv = this.div
            .append('div')
            .classed('largePaddedDiv', true)
            .datum(tag)
            .append('div');

        let span = tagDivDiv.append('div')
            .classed('paddedDiv darkThickBorderBottom', true)
            .append('span');

        span.append("textarea")
            .attr("spellcheck", false)
            .attr('id', `tag_${tag['tagName']}`)
            .text(tag.detail)
            .on("change", () => this.updateTags(tag));
    }
};


/**
 * update the tag ingormation and send to server
 * @param tagName
 */
TagInformation.prototype.updateTags = function (tagName) {
    for (let i = 0; i < this.tags.length; i++) {
        if (this.tags[i]['tagName'] === tagName) {
            this.tags[i].detail = document.getElementById(`tag_${tag['tagName']}`).value;

            // sendToServer(this.ws, "MODIFIED_TAG", `{\"tagName\":${tagName},\"tagText\":${JSON.stringify(this.tags[i])}}`);
            sendToServer(this.ws, "MODIFIED_TAG", this.tags[i]);
            return;
        }
    }
};
