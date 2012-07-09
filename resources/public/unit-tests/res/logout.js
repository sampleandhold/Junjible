// logout.js
// module("logout");

// logout testuser
JQtest("logout_1", null, 1, function() {
    $.get('/logout', function (html) {
	ok(true, "called /logout");
	junjibleLogout(true); // don't redirect
	JQdone("logout_1");
	start();
    });
});

// GetUser
JQtest("logout_2", null, 1, function() {
    GetUser(function() {
	ok(! junjibleLoggedIn(), "no user is logged in");
	JQdone("logout_2");
	start();
    });
});
