  // JavaScript to add a class when the element is in the viewport
  if (document.readyState !== 'loading') {
      console.log('document is already ready, just execute code here');
      myInitCode();
  } else {
      document.addEventListener('DOMContentLoaded', function () {
          console.log('document was not ready, place code here');
          myInitCode();
      });
  }

  function myInitCode() {

  var elementNames='.animate-init';
    var elementsToBeStyled = document.querySelectorAll(elementNames);
    console.log("madhu is printing document status" + document.readyState);

const ot = {
            rootMargin: "0px",
            threshold: [0, 1]
        }
 const observer = new IntersectionObserver(entries => {
            entries.forEach(entry => {
                // If entry intersects with viewport
                if (entry.isIntersecting) {
                    entry.target.classList.add('animate-start');
                    observer.unobserve(entry.target);
                }
            });
        }, ot);
// Start observing each target element
        elementsToBeStyled.forEach(element => {
            observer.observe(element);
        });
  }
//Code to show navigation bar when page is scrolled down
window.onscroll = function() {handleNavigationVisibility()};

function handleNavigationVisibility() {
  // Check if the user has scrolled more than 50px from the top
  if (document.body.scrollTop > 50 || document.documentElement.scrollTop > 50) {
    // If scrolled, set the navbar's top position to 0
    document.querySelector(".PageHeader ").classList.add("visible");
       document.querySelector(".PageHeader ").classList.remove("hidden");
  } else {
    // If not scrolled (i.e., at the top of the page), set the navbar's top position to its original value
     document.querySelector(".PageHeader ").classList.add("hidden");
       document.querySelector(".PageHeader ").classList.remove("visible");
  }
}