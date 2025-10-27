document$.subscribe(function() {
  var feedback = document.forms.feedback
  if (typeof feedback === "undefined") return
  feedback.hidden = false
  feedback.addEventListener("submit", function(ev) {
    ev.preventDefault()
    var page = document.location.pathname 
    var data = ev.submitter.getAttribute("data-md-value")

 // ---- MATOMO EVENT LOGGING ----
    if (typeof _paq !== "undefined") {
      _paq.push([
        "trackEvent",
        "feedback",              // Category
        data === "1" ? "Good" : "Bad", // Action
        page,                     // Label (page path),
      ])
    }

    feedback.firstElementChild.disabled = true 
    var note = feedback.querySelector(
      ".md-feedback__note [data-md-value='" + data + "']"
    )
    if (note)
      note.hidden = false 
  })
})