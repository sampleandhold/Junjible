// global variable junjible

var junjible = {};

// careful definition of junjible.log for cross browser compatibility
// see also http://stackoverflow.com/questions/5145032/whats-the-use-of-array-prototype-slice-callarray-0

junjible.log = function(arguments) {
  // here we use log_history as to not undo the definition of this function
  junjible.log_history = junjible.log_history || [];
  junjible.log_history.push(arguments);
  if(window.console){
    // console.log(Array.prototype.slice.call(arguments));
    // if we have a console we can just pass in the arguments
    console.log(arguments);
  }
};

junjible.instance = -1;
junjible.instances = 0;

junjible.worlds = new Array();
junjible.world = -1;

// notify object and message system
junjible.notify = new Array();
junjible.notify.queue = new Array();
junjible.notify.shown = false;
junjible.notify.timer = 0; // timer ID
junjible.notify.interval = 2000; // ms each message is shown
junjible.notify.update = function () {
    if (junjible.notify.queue.length == 0) {
        $('#notes').slideUp('slow');
        junjible.notify.shown = false;
    } else {
        $('#notify-text').html(junjible.notify.queue.shift());
        junjible.notify.timer = setTimeout("junjible.notify.update()", junjible.notify.interval);
        junjible.notify.shown = true;
    }
};
junjible.notify.msg = function (message) {
    if (message == "") return;
    if (junjible.debug) {
        junjible.log(message);
    }
    var lines = message.split("\n");
    // if (junjible.notify.shown) {
    //   clearTimeout(junjible.notify.timer); // short circuits display time
    // }
    if (!junjible.notify.shown) {
        $('#notes').slideDown('slow');
        $('#notify-text').html(lines.shift());
        junjible.notify.timer = setTimeout("junjible.notify.update()", junjible.notify.interval);
        junjible.notify.shown = true;
    }
    while (lines.length > 0) {
        junjible.notify.queue.push(lines.shift());
    }
}
junjible.notify.response = function (result) { // response from the srever
    // assume result always has success (boolean) and msg (one liner)
    if (result.success) {
        junjible.notify.msg(result.msg);
    } else if (result.msg.indexOf("error:") == 0) {
	// don't add another error:
        junjible.notify.msg(result.msg);
    } else {
        junjible.notify.msg("error: " + result.msg);
    }
}

function junjibleLogout(dontRedirect) {
    if (typeof junjible['user'] == 'object') {
	junjible.user = null; // forget all user data
    }
    junjible.log("junjible.user logged out");
    // go back to home page
    if (! dontRedirect) {
	window.location.href = '/';
    }
}

// junjible.nag takes 3 arguments:  1. a message, 2. a single argument, or an array of arguments to pass to the callback, and 3. a callback function to run if the user clicks "yes"

executeMe = function(dis, dat){
    if (dat && typeof(dat) === "function") {
	if ((typeof(dis)=='object')&&(dis instanceof Array)){dat.apply(this, dis);}else if (typeof(dis)=='string'){dat(dis);}
    }
    $('#obscura').fadeOut('slow');
    $('#flyover').fadeOut('slow'); 
}

junjible.nag = function (nag, arg1, callback) {
    $('#flyover-content').remove();
    $('#flyover').html('');
    $('#obscura').fadeIn('fast');
    $('#flyover').fadeIn('fast', function () {
	var html = "";
	html += '<div class="close-box"><img src="/images/close.png"></div>';
	html += '<p><center><h3 style="margin:20px 40px;">'+nag+'</h3>';
	html += '<input id="yes-button" type="submit" value="Yes">&nbsp;&nbsp;<input id="no-button" type="submit" value="No"></center></p>';
	$('#flyover').append(html);


	$('#no-button').on('click', function (){;
						$('#obscura').fadeOut('fast');
						$('#flyover').fadeOut('fast'); 

					       });

	$('#yes-button').on('click', function() {
	    executeMe(arg1, callback)
	});
	$('.close-box').on('click', function() {
	    $('#obscura').fadeOut('slow');
	    $('#flyover').fadeOut('slow'); 
	});

    });
}

// user settings
function SaveUser() {
    $.post('/server/save-user', {
        "user": {
            "password": $('[name=curpass]').val(),
            "newpassword": $('[name=newpass]').val(),
            "newpassword2": $('[name=newpass2]').val(),
            "email": $('[name=email]').val(),
            "minecraft": $('[name=minecraft]').val(),
            "delete-account": $('[name=delete-account]').is(':checked')
        }
    }, function (data) {
        var msg;
        var errmsg = "account not updated";
        if (typeof data.error != "undefined") {
            errmsg = data.error;
        }
        // junjible.log*("saveUser response: " + JSON.stringify(data)); // DEBUG
        if (data.saved) {
            msg = "account updated successfully";
        } else {
            msg = "error: " + errmsg;
        }
        junjible.notify.msg(msg);
	if ($('[name=delete-account]').is(':checked')) {
	    junjible.log("user account deleted");
	    junjibleLogout();
	}
    });
}

function GetUser(callback) {
    $.get('/server/get-user', function (userData) {
	if (typeof userData['user'] == 'object') {
            junjible.user =  userData.user;
	}
        // junjible.log("GetUser junjible: " + JSON.stringify(junjible)); // DEBUG
	if (typeof junjible['user'] == 'object' &&
	    junjible.user != null &&
	    typeof junjible.user['name'] == 'string') {
            $('[name=curpass]').val("");
            $('[name=newpass]').val("");
            $('[name=email]').val(junjible.user.email);
            $('[name=minecraft]').val(junjible.user.minecraft);
            $('[name=delete-account]').attr('checked',false);
	    if (junjible.user.admin == true) {

		$('#admin-link[style="display:none;"]').fadeIn('slow');
	    }
	} else {
            junjible.user =  null;
	}
	if (callback) callback();
    });
    // undo any existing bindings
    $('#user-submit').unbind('click');
    // bind "Edit Account"n
    $('#user-submit').on('click', SaveUser);
}

function junjibleLogin() {
    junjible.debug = true;
}
// Since we don't have a universal place to put a login hook
// we will simply initialize the basics of the junjible structure here
// on load... and wait for GetUser() to fill in the details
junjibleLogin();

// Note for this to work.. you must have called GetUser() first
// and waited for the response
function junjibleLoggedIn() {
    var loggedin = false;
    if (typeof junjible['user'] == 'object' &&
	junjible.user != null &&
	typeof junjible.user['name'] == 'string') {
	junjible.log("junjible.user = " + junjible.user.name);
	loggedin = true;
    } else {
	junjible.log("junjible.user not logged in");
    }
    return loggedin;
}

// returns true of the user is an administrator
function junjibleAdmin(forceGet) {
    if (forceGet && ! junjibleLoggedIn()) {
	GetUser();
    }
    return junjibleLoggedIn() && junjible.user.admin;
}

function getWorlds(callback) {
    if (! junjibleLoggedIn()) {
	if (callback) callback(); // this may not work :(
	return; // don't bother if we are not logged in yet
    }
    $.get('/server/get-worlds', function (theData) {
	if (typeof theData['worlds'] != 'undefined' && typeof theData['worlds'] != null && theData['worlds'].length >=1) {
	    // junjible.log("getWorlds worlds.. were = " + JSON.stringify(junjible.worlds));
	    junjible.worlds = theData.worlds;
	    // junjible.log("getWorlds now... worlds = " + JSON.stringify(junjible.worlds));
	    if (junjible.worlds.length > 0) {
		if (junjible.world >= junjible.worlds.length) {
		    junjible.world = junjible.worlds.length - 1;
		}
	    } else {
		junjible.world = -1;
	    }
	}
	if (callback) callback();
    });
}

// on loading a page get the current user then call getWorlds
GetUser(getWorlds);

