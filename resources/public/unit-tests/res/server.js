// Stops server instance
JQtest('stop-server', null, 1, function tester () {
    var exe = {
	'command': 'stop',
	'instance': 'My Server'
    };

    $.post('/server/control', exe, function reqHandler (response) {
	var r = response;
	var c = exe.command;
	var i = exe.instance;
	equal(r.success, true, 'Stop the server: ' + c + ': ' + i);
	JQdone('stop-server');
	start();
    });
});

// Starts server instance
JQtest('start-server', null, 1, function tester () {
    var exe = {
	'command': 'start',
	'instance': 'My Server'
    };

    $.post('/server/control', exe, function reqHandler (response) {
	var r = response;
	var c = exe.command;
	var i = exe.instance;
	equal(r.success, true, 'Start the server: ' + c + ': ' + i);
	JQdone('start-server');
	start();
    });
});

// Restarts server instance
JQtest('restart-server', null, 1, function tester () {
    var exe = {
	'command': 'restart',
	'instance': 'My Server'
    };

    $.post('/server/control', exe, function reqHandler (response) {
	var r = response;
	var c = exe.command;
	var i = exe.instance;
	equal(r.success, true, 'Restart the server: ' + c + ': ' + i);
	JQdone('restart-server');
	start();
    });
});