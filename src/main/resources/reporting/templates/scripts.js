var els = document.querySelectorAll(".has-toggle");
[].forEach.call(els, function(el) {
    el.addEventListener('click', function() {
        el.classList.toggle('is-closed')
    }, !1)
});
var els = document.querySelectorAll('button[for]');
[].forEach.call(els, function(el) {
    el.addEventListener('click', function() {
        var id = el.getAttribute('for');
        var item = document.getElementById(id);
        var event = new MouseEvent('click');
        item.dispatchEvent(event);
        if (item.parentNode.classList.contains('is-closed') === !1) {
            window.location.href = '#' + id
        }
    }, !1)
});
var els = document.querySelectorAll('a[href^="#"]');
[].forEach.call(els, function(el) {
    el.addEventListener('click', function() {
        var id = el.getAttribute('href').replace('#', '');
        var item = document.getElementById(id);
        while (item != null) {
            if (item.classList != null && item.classList.contains('is-closed') === !0) {
                item.classList.remove('is-closed')
            }
            var sibling = item.previousElementSibling;
            if (sibling != null && sibling.classList != null && sibling.classList.contains('is-closed') === !0) {
                sibling.classList.remove('is-closed')
            }
            item = item.parentNode
        }
        window.location.href = '#' + id
    }, !1)
});
var closeAll = !0;
var els = document.querySelectorAll("#expandCollapse");
[].forEach.call(els, function(el) {
    el.addEventListener('click', function() {
        var expandableElements = document.querySelectorAll(".has-toggle");
        if (closeAll) {
            var i = 0;
            while (i < expandableElements.length) {
                expandableElements[i].classList.add('is-closed');
                i = i + 1
            }
        } else {
            var i = 0;
            while (i < expandableElements.length) {
                expandableElements[i].classList.remove('is-closed');
                i = i + 1
            }
        }
        closeAll = !closeAll
    }, !1)
});
var expandableElements = document.querySelectorAll(".has-toggle");
if (expandableElements.length == 0) {
    var els = document.querySelectorAll("#expandCollapse");
    els[0].style.visibility = 'hidden'
}