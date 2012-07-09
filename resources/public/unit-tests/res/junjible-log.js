// junjible-log.js
// if this file is included, post the results of the Qunit tests

function JQlog(results) {
    var logname = 'qunit';
    if (document.title.indexOf(',') > 0) {
	// we have multiple tests, call them all
	logname = 'all';
    } else {
	var colon = document.title.indexOf(':');
	var star = document.title.indexOf('*');	// remove any trailing stars
	if (star < 0) {
	    star = document.title.length();
	}
	if (colon > 0) {
	    logname = document.title.substring(colon + 2, star);
	}
    }
    var logData = {
	"log": logname,
	"html": $('html').html(),
	"results": results,
    }
    junjible.log("JQlog =" + JSON.stringify(results) + "=");
    $.post('/unit-tests/log', logData, function(result) {
	if (result.success) {
            junjible.log("SUCCESS " + result.msg);
	} else {
            junjible.log("FAILURE " + result.msg);
	}
    });
}

// run after the last test has completed
// JQdepend("done", JQlog);
QUnit.done = JQlog;
