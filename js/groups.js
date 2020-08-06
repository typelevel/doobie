groupChangeListeners = [];

window.groupChanged = function(callback) {
  groupChangeListeners.push(callback);
}

$(function() {

  // Groups (like 'java' and 'scala') represent groups of 'switchable' content, either in tabs or in regular text.
  // The catalog of groups can be defined in the sbt parameters to initialize the group.

  var groupCookie = "paradoxGroups";
  var cookieTg = getCookie(groupCookie);
  var currentGroups = {};

  var catalog = {}
  var supergroupByGroup = {};

  if(cookieTg != "")
    currentGroups = JSON.parse(cookieTg);

  // http://www.w3schools.com/js/js_cookies.asp
  function setCookie(cname,cvalue,exdays) {
    if(!exdays) exdays = 365;
    var d = new Date();
    d.setTime(d.getTime() + (exdays*24*60*60*1000));
    var expires = "expires=" + d.toGMTString();
    document.cookie = cname + "=" + encodeURIComponent(cvalue) + ";" + expires + ";path=/";
  }

  // http://www.w3schools.com/js/js_cookies.asp
  function getCookie(cname) {
    var name = cname + "=";
    var decodedCookie = decodeURIComponent(document.cookie);
    var ca = decodedCookie.split(';');
    for(var i = 0; i < ca.length; i++) {
      var c = ca[i];
      while (c.charAt(0) == ' ') {
        c = c.substring(1);
      }
      if (c.indexOf(name) == 0) {
        return c.substring(name.length, c.length);
      }
    }
    return "";
  }

  $("dl").has("dd > pre").each(function() {
    var dl = $(this);
    dl.addClass("tabbed");
    var dts = dl.find("dt");
    dts.each(function(i) {
      var dt = $(this);
      dt.html("<a href=\"#tab" + i + "\">" + dt.text() + "</a>");
    });
    var dds = dl.find("dd");
    dds.each(function(i) {
      var dd = $(this);
      dd.hide();
      if (dd.find("blockquote").length) {
        dd.addClass("has-note");
      }
    });

    // Default to the first tab, for grouped tabs switch again later
    switchToTab(dts.first());

    dts.first().addClass("first");
    dts.last().addClass("last");
  });

  // Determine all supergroups, populate 'catalog' and 'supergroupByGroup' accordingly.
  $(".supergroup").each(function() {
    var supergroup = $(this).attr('name').toLowerCase();
    var groups = $(this).find(".group");

    catalog[supergroup] = [];

    groups.each(function() {
      var group = "group-" + $(this).text().toLowerCase();
      catalog[supergroup].push(group);
      supergroupByGroup[group] = supergroup;
    });

    $(this).on("change", function() {
      switchToGroup(supergroup, this.value);
    });
  });

  // Switch to the right initial groups
  for (var supergroup in catalog) {
    var current = queryParamGroup(supergroup) || currentGroups[supergroup] || catalog[supergroup][0];

    switchToGroup(supergroup, current);
  }

  $("dl.tabbed dt a").click(function(e){
    e.preventDefault();
    var currentDt = $(this).parent("dt");
    var currentDl = currentDt.parent("dl");

    var currentGroup = groupOf(currentDt);

    var supergroup = supergroupByGroup[currentGroup]
    if (supergroup) {
      switchToGroup(supergroup, currentGroup);
    } else {
      switchToTab(currentDt);
    }
  });

  function queryParamGroup(supergroup) {
    var value = new URLSearchParams(window.location.search).get(supergroup)
    if (value) {
      return "group-" + value.toLowerCase();
    } else {
      return "";
    }
  }

  function switchToGroup(supergroup, group) {
    currentGroups[supergroup] = group;
    setCookie(groupCookie, JSON.stringify(currentGroups));

    // Dropdown switcher:
    $("select")
      .has("option[value=" + group +"]")
      .val(group);

    // Inline snippets:
    for (var i = 0; i < catalog[supergroup].length; i++) {
      var peer = catalog[supergroup][i];
      if (peer == group) {
        $("." + group).show();
      } else {
        $("." + peer).hide();
      }
    }

    // Tabbed snippets:
    $("dl.tabbed").each(function() {
      var dl = $(this);
      dl.find("dt").each(function() {
        var dt = $(this);
        if(groupOf(dt) == group) {
          switchToTab(dt);
        }
      });
    });

    for (var i = 0; i < groupChangeListeners.length; i++) {
      groupChangeListeners[i](group, supergroup, catalog);
    }
  }

  function switchToTab(dt) {
    var dl = dt.parent("dl");
    dl.find(".current").removeClass("current").next("dd").removeClass("current").hide();
    dt.addClass("current");
    var currentContent = dt.next("dd").addClass("current").show();
    dl.css("height", dt.height() + currentContent.height());
  }

  function groupOf(elem) {
    var classAttribute = elem.next("dd").find("pre").attr("class");
    if (classAttribute) {
      var currentClasses = classAttribute.split(' ');
      var regex = new RegExp("^group-.*");
      for(var i = 0; i < currentClasses.length; i++) {
        if(regex.test(currentClasses[i])) {
          return currentClasses[i];
        }
      }
    }

    // No class found? Then use the tab title
    return "group-" + elem.find('a').text().toLowerCase();
  }
});
