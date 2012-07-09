// delete.js
// module("delete");

// GetUser
JQtest("delete_1", null, 2, function() {
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
	JQdone("delete_1");
	start();
    });
});

// delete testuser
JQtest("delete_2", null, 2, function() {
    var deluser = {
	'user': {
	    'password': 'password',
	    'newpassword': 'password',
	    'newpassword2': 'password',
	    'email': 'testuser@email.com',
	    'minecraft': 'testuser',
	    'delete-account': true
	}
    };
    $.post('/server/save-user', deluser, function (data) {
        var errmsg;
        if (typeof data.error == "undefined") {
	    errmsg = "";
        } else {
	    errmsg = data.error;
	}
	equal(errmsg, '', "no error in processing /server/save-user");
	ok(data.saved, "testuser account successfully deleted");
	junjibleLogout(true); // don't redirect
	JQdone("delete_2");
	start();
    });
});
