// Removes everything but the default world
JQtest('reset-worlds', null, 1, function tester () {
    var worlds = {
	'worlds[0][private]': true,
	'worlds[0][motd]': 'Message of the day',
	'worlds[0][whitelist]': '',
	'worlds[0][name]': 'My New World' 
    };
    $.post('/server/save-worlds', worlds, function reqHandler (response) {
	var r = response;
	equal(r.success, true, 'Remove all worlds but the default: ' + r.msg);
	JQdone('reset-worlds');
	start();
    });
});

// Creates new world
JQtest('create-world', null, 1, function tester () {
    var worlds = {
	'worlds[0][private]': true,
	'worlds[0][motd]': 'Message of the day',
	'worlds[0][whitelist]': '',
	'worlds[0][name]': 'My New World',
	'worlds[1][private]': true,
	'worlds[1][motd]': 'Test world motd',
	'worlds[1][whitelist]': '',
	'worlds[1][name]': 'Frolic with sheep' 
    };

    $.post('/server/save-worlds', worlds, function reqHandler (response) {
	var r = response;
	equal(r.success, true, 'Create world: ' + r.msg);
	JQdone('create-world');
	start();
    });
});


// Delete the test world
JQtest('delete-world', 'create-world', 1, function tester () {
    var worlds = {
	'worlds[0][private]': true,
	'worlds[0][motd]': 'Message of the day',
	'worlds[0][whitelist]': '',
	'worlds[0][name]': 'My New World'
    };

    $.post('/server/save-worlds', worlds, function reqHandler (response) {
	var r = response;
	equal(r.success, true, 'Delete world: ' + r.msg);
	JQdone('delete-world');
	start();
    });
});

