$(function() {

  // add magellan targets to anchor headers, up to depth 3
  $("a.anchor").each(function() {
    var anchor = $(this);
    var name = anchor.attr("name");
    var header = anchor.parent();
    if (header.is("h1") || header.is("h2") || header.is("h3")) {
      header.attr("id", name).attr("data-magellan-target", name);
    }
  });

  // enable magellan plugin on the page navigation
  $(".page-nav").each(function() {
    var nav = $(this);

    // strip page navigation links down to just the hash fragment
    nav.find("a").attr('href', function(_, current){
        return this.hash ? this.hash : current;
    });

    new Foundation.Magellan(nav);
  });

});
