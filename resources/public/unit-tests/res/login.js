// login.js
// module("login");

// login testuser
JQtest("login_1", null, 1, function() {
    var login = {
	'user': 'testuser',
	'pass': 'password',
	'remember': false,
    };
    $.post('/login', login, function (html) {
	// NOTE: we don't have a great way of determing
	// if the user is logged in... normally they should
	// get to the /panel
	equal($('div#server-tab > ul.panel-sub-menu > li > a', html).attr('href'),
		    "/server/manage",
		    "testuser successfully logged in");
	JQdone("login_1");
	start();
    });
});

// GetUser
JQtest("login_2", null, 2, function() {
    GetUser(function() {
	ok(junjibleLoggedIn(), "testuser is logged in");
	if (typeof junjible['user'] == 'object' &&
	    junjible.user != null &&
	    typeof junjible.user['name'] == 'string' &&
	    junjible.user.name == "testuser") {
	    ok(true, "junjible data structure shows testuser");
	} else {
	    ok(false, "junjible data structure shows testuser");
	}
	JQdone("login_2");
	start();
    });
});
