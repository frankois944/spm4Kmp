document$.subscribe(function() {
  // ---- BANNER LOGIC ----
  const banner = document.getElementById('custom-banner');
  const closeButton = document.getElementById('close-banner');
  const bannerId = 'golden-kodee-2026';

  if (banner && !localStorage.getItem('banner-closed-' + bannerId)) {
    banner.style.display = 'block';
    document.body.setAttribute('data-banner-visible', 'true');
  } else if (banner) {
    banner.style.display = 'none';
    document.body.setAttribute('data-banner-visible', 'false');
  }

  if (closeButton) {
    closeButton.addEventListener('click', function() {
      banner.style.display = 'none';
      document.body.setAttribute('data-banner-visible', 'false');
      localStorage.setItem('banner-closed-' + bannerId, 'true');
    });
  }

  // ---- FEEDBACK LOGIC ----
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
