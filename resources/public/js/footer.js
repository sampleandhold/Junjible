// footer.js
$('#nav-list a, a.navjs').click(function() {
    var action = $(this).attr('rel');
    if (action == "logout") {
	junjibleLogout();
	return false;
    }
    else if (action == "login"){
      $('#obscura, #flyover').fadeIn('slow');
      return false;
    }
    else{
    }
});
$('.close-box').click(function() {
    $('#obscura, #flyover').fadeOut('slow');
});
