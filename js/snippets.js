$(function() {
  $(".snippet-button.copy-snippet").click(function() {
    var code = $(this).parent().find("code").text()
    navigator.clipboard.writeText(code)
  })
})