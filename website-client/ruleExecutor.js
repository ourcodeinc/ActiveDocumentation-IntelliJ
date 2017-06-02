



function runRules(){

	let ruleBox = document.getElementById("ruleBox").firstElementChild.firstElementChild.firstElementChild;
	ruleBox.innerHTML = "";
	let a;
	let b;
	
	for(a = 0; a < ruleTable.length; a++){
		let ruleResultsOfA = RuleContainer[ruleTable[a].ruleFunc](); // ruleTable[a].ruleFunc();

		let theDiv = document.createElement("div");
		theDiv.setAttribute("class", "card blue-grey darken-1");
		theDiv.setAttribute("title", ruleTable[a].ruleFunc);
        ruleBox.appendChild(theDiv); // Temp

        if(ruleResultsOfA.length == 0){
            continue;
        }
		/////////// child 1 - add title and description ///////////////

		let ch1OfDiv = document.createElement("div");
		ch1OfDiv.setAttribute("class", "card-content white-text");
		theDiv.appendChild(ch1OfDiv);

		let theSpanHeader = document.createElement("span");
		theSpanHeader.setAttribute("class", "card-title");
		theSpanHeader.innerHTML = ruleTable[a].header;
		ch1OfDiv.appendChild(theSpanHeader);

		let theDescriptionP = document.createElement("p");
		theDescriptionP.innerHTML = ruleTable[a].description;
		ch1OfDiv.appendChild(theDescriptionP);

		/////////// child 2 - the links ///////////////

		let ch2OfDiv = document.createElement("div");
		ch2OfDiv.setAttribute("class", "card-action");
		theDiv.appendChild(ch2OfDiv);

		for(b = 0; b < ruleResultsOfA.length; b++){
			let anALink = document.createElement("a");
			let containingFileName = getContainingFileGivenASTNode(ruleResultsOfA[b]).properties.fileName;
			anALink.onclick = function(){
				// alert(this.innerHTML + "\n" + this.getAttribute("data-class-of-line-num"));
				let messageToSend = {"source":"WEB", "destination":"IDEA", "command":"JUMP_TO_CLASS_WITH_LINE_NUM"};

				let dataOfMessage = {};
				dataOfMessage["fileName"] = this.getAttribute("data-class-of-line-num");
				dataOfMessage["lineNumber"] = parseInt(this.getAttribute("data-line-num"));

				messageToSend["data"] = dataOfMessage;
				ws.send(JSON.stringify(messageToSend));
			};
			anALink.setAttribute("data-class-of-line-num", containingFileName);
			anALink.setAttribute("data-line-num", ruleResultsOfA[b].properties.textOffset);
			anALink.innerHTML = containingFileName + ":" + ruleResultsOfA[b].properties.textOffset; // need to find the class where the error is
			ch2OfDiv.appendChild(anALink);
		}

		ruleBox.appendChild(theDiv);

	}

	return [];

}