// junjible-qunit.js

junjible.tests = {};
junjible.tests.previous = 'ready'; // first dependency is on ready
junjible.tests.count = 0; // number of (non done) dependent functions

function JQTimestamp(msg) {
    junjible.log(msg + " @ " + new Date().getTime());
}

function JQdone(tname) {
    JQTimestamp("DONE " + tname);
    var f;
    if (typeof junjible.tests[tname] != 'undefined') {
	f = junjible.tests[tname].shift();
	while (typeof f != 'undefined') {
	    f(); // run the next dependent function
	    if (tname != 'done') {
		junjible.tests.count--;
	    }
	    f = junjible.tests[tname].shift();
	}
    }
    // if there are no more tests left, then fire done
    if (tname != 'done' && junjible.tests.count == 0) {
	junjible.tests.count--;
	JQdone('done');
    }
}

function JQdepend(n, f) {
    if (typeof junjible.tests[n] == 'undefined') {
	junjible.tests[n] = new Array();
    }
    junjible.tests[n].push(f); // add a dependent function
    if (n != 'done') {
	junjible.tests.count++;
    }
}

function JQtest(tname, tdep, texp, tfun) {
    if (! tdep) { // if an explicit dependency isn't given
	tdep = junjible.tests.previous; // then depend on the previous test
    }
    junjible.tests.previous = tname;
    junjible.log("registering: " + tname + " which depends on: " + tdep + " expected: " + texp);
    var f = function() {
	JQTimestamp(tname);
	asyncTest(tname, texp, tfun);
    };
    JQdepend(tdep, f);
}

// fire when document ready
$(document).ready(function(){
    JQdone('ready');
});