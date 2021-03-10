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

  // https://developer.mozilla.org/en-US/docs/Web/API/Document/cookie
  function setCookie(cookieName, cookieValue, daysToExpire) {
    if (!daysToExpire) daysToExpire = 365;
    const now = new Date();
    now.setDate(now.getDate() + daysToExpire);
    // The lax value will send the cookie for all same-site
    // requests and top-level navigation GET requests. This
    // is sufficient for user tracking, but it will prevent
    // many CSRF attacks. This is the default value in modern browsers.
    document.cookie = `${cookieName}=${encodeURIComponent(cookieValue)};expires=${now.toUTCString()};path=/;samesite=lax`;
  }

  // https://developer.mozilla.org/en-US/docs/Web/API/Document/cookie#Example_2_Get_a_sample_cookie_named_test2
  function getCookie(cookieName) {
    const cookieAttr = decodeURIComponent(document.cookie)
        .split(";")
        .find(row => row.trimStart().startsWith(cookieName))
    return cookieAttr ? cookieAttr.split("=")[1] : "";
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
    catalog[supergroup].forEach(peer => {
      if (peer === group) {
        $("." + group).show();
      } else {
        $("." + peer).hide();
      }
    })

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

    groupChangeListeners.forEach(listener => listener(group, supergroup, catalog));
  }

  function switchToTab(dt) {
    var dl = dt.parent("dl");
    dl.find(".current").removeClass("current").next("dd").removeClass("current").hide();
    dt.addClass("current");
    var currentContent = dt.next("dd").addClass("current").show();
    dl.css("height", dt.height() + currentContent.height());
  }

  function groupOf(elem) {
    const classAttribute = elem.next("dd").find("pre").attr("class");
    if (classAttribute) {
      const currentClasses = classAttribute.split(' ');
      const regex = new RegExp("^group-.*");
      const matchingClass = currentClasses.find(cc => regex.test(cc));
      if (matchingClass) return matchingClass;
    }

    // No class found? Then use the tab title
    return "group-" + elem.find('a').text().toLowerCase();
  }
});
