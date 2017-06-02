//removes all child nodes that are not also elements or text
function getElements(childNodes) {
  var array = [];
  for(var i = 0; i < childNodes.length; i++) {
    if(childNodes[i].nodeType === 1 || childNodes[i].data.trim() !== "") {
      array.push(childNodes[i]);
    }
  }
  return array;
}

function attributes(attributes) {
  var str = "";
  for(var i = 0; i < attributes.length; i++) {
    str += "<span class=attr>" + attributes[i].nodeName + "</span>"
      + "<span class=sign>=\"</span><span class=attrValue>" + attributes[i].nodeValue
      + "</span>" + "<span class=sign>\"</span> ";
  }
  if(str.length > 1)
    return " " + str;
  else
    return str.trim();//.substring(str.length-1);
}

// convert DOM tree into HTML collapsible list
function generateDOMTree(parent, flag=true) {
  var children = getElements(parent.childNodes);
  var html = "";
  if(children.length !== 0) {
    if(flag) {
      html += "<ul class=collapsibleList>";
      flag = false;
    }
    else {
      html += "<ul>";
    }
    for(var i = 0; i < children.length; i++) {

      html += "<li";

      if(i === children.length - 1) {
        html += " class=lastChild"
      }

      html += ">"
      if(children[i].nodeType !== 1) {
        html += "<span class=textNode>\"" +  HtmlEncode(children[i].data.trim()) + "\"</span>";
      }
      else {
        var attr = attributes(children[i].attributes).trim();
        if(attr.length > 1)
          attr = " " + attr;
        html += "<span class=sign>&lt;</span>"
          + "<span class=elementNode>"
          + children[i].nodeName.toLowerCase()
          + "</span><span class=sign>" + attr + "&gt;</span>"
          // + "..."
          // + "<span class=sign>&lt;/</span><span class=elementNode>"
          // + children[i].nodeName.toLowerCase()
          // + "</span><span class=sign>&gt;</span>";
      }

      html += generateDOMTree(children[i], false) + "</li>";
    }
    return html + "</ul>";
  }
  // else if(parent.innerHTML !== undefined){
  //   return html + "<ul><li class=lastChild>" + "\"" + parent.innerHTML + "\"" + "</li></ul>";
  // }
  else {
    return "";
  }
}

// convert object tree into HTML collapsible list
function generateObjectTree(object, flag=true) {
  var keys = Object.keys(object);
  var html = "";
  if(typeof object === "object" && keys.length !== 0) {
    if(flag) {
      html += "<ul class=collapsibleList>"
      flag = false;
    }
    else {
      html += "<ul>"
    }
    for(var i = 0; i < keys.length; i++) {
      if(i === keys.length - 1) {
        html += "<li class=lastChild>"
      }
      else {
        html += "<li>";
      }
      var key = "<span class=key>";
      var value;

      if(typeof keys[i] === "object") {
        key += keys[i].constructor.name;
      }
      else {
        key += keys[i];
      }

      key += "</span>"

      if(typeof object[keys[i]] === "object") {
        value = object[keys[i]].constructor.name;
        if(object[keys[i]] instanceof Array)
          value += "[" + object[keys[i]].length + "]";
      }
      else if(typeof object[keys[i]] === "string") {
        value = "<span class=stringLiteral> \"" + object[keys[i]] + "\" </span>";
      }
      else {
        value = "<span class=literal>" + object[keys[i]] + "</span>";
      }

      html += key + ": " + value;
      html += generateObjectTree(object[keys[i]], false) + "</li>";
    }
    return html + "</ul>";
  }
  else {
    return "";
  }
}

function setup(node) {
  var a = "<ul class=treeView><li>" + "<span class=sign>&lt;</span>"
    + "<span class=elementNode>" + node.nodeName.toLowerCase() + "</span><span class=sign>&gt;</span>";
  a += generateDOMTree(node);
  a += "</li></ul>";
  printh(a);
  CollapsibleLists.apply();
}

function dispObject(object) {
  var a = "<ul class=treeView><li>" + object.constructor.name;
  a += generateObjectTree(object);
  a += "</li></ul>";
  printh(a);
  CollapsibleLists.apply();
}

function HtmlEncode(s) {
  var el = document.createElement("div");
  el.innerText = el.textContent = s;
  s = el.innerHTML;
  return s;
}

function printh(tree) {
  var element = document.createElement("div");
  element.innerHTML = tree;
  console.log(tree);
  document.body.appendChild(element);
}
