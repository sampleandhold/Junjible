// Removes all groups but the default
JQtest("reset-groups", null, 1, function tester () {
    var groups = { "groups[0][groupname]": "My Group" };
    $.post('/server/save-groups', groups, function reqHandler (response) {
	var r = response;
	equal(r.success, true, "Remove all groups but the default: " + r.msg);
	JQdone("reset-groups");
	start();
    });
});

// Add user to group
JQtest("add-member", "reset-groups", 1, function tester() {
    var groups = {
	"groups[0][groupname]": "My Group",
	"groups[0][groupmembers][0][name]": "testuser"
    };
    $.post('/server/save-groups', groups, function reqHandler (response) {
	var r = response;
	equal(r.success, true, 'Add User to default group: ' + r.msg);
	JQdone('add-member');
	start();
    });
});

// Change user permissions
JQtest('change-user-permissions', 'add-member', 1, function tester() {
    var groups = {
	'member':'testuser',
        'group':'',
        'special':[['banned',true]['whitelist',true]],
	'basic':[
		['gamemode',true],['give',true],
		['kick',true],['pardon',true],
		['save',true],['say',true],
		['stop',true],['time',true],
		['tp',true],['wand',true],
		['weather',true],['xp',true]],
		'plugin':[
			{'name':'superplug','plugperms':[['splish',true],
				['splash',true],['take a bath',true]]},
			{'name':'wonderplug','plugperms':[['life',true],
				['liberty',true],['happiness',true]]},
			{'name':'nastyplug','plugperms':[['bleed',true],
				['itch',false],['hyperventilate',true]]}]
    };
    $.post('/server/save-permissions', groups, function reqHandler (response) {
	var r = response;
	equal(r.success, true, 'Change user permissions: ' + r.msg);
	JQdone('change-user-permissions');
	start();
    });
});

// Remove user from group
JQtest('remove-member', 'add-member', 1, function tester() {
    var groups = { 'groups[0][groupname]': 'My Group' };
    $.post('/server/save-groups', groups, function reqHandler (response) {
	var r = response;
	equal(r.success, true, 'Remove User from default group: ' + r.msg);
	JQdone('remove-member');
	start();
    });
});

// Change group permissions
JQtest('change-group-permissions', 'add-member', 1, function tester() {
    var groups = {
	'member':'',
        'group':'My Group',
        'special':[['banned',true]['whitelist',true]],
	'basic':[
		['gamemode',true],['give',true],
		['kick',true],['pardon',true],
		['save',true],['say',true],
		['stop',true],['time',true],
		['tp',true],['wand',true],
		['weather',true],['xp',true]],
		'plugin':[
			{'name':'superplug','plugperms':[['splish',true],
				['splash',true],['take a bath',true]]},
			{'name':'wonderplug','plugperms':[['life',true],
				['liberty',true],['happiness',true]]},
			{'name':'nastyplug','plugperms':[['bleed',true],
				['itch',false],['hyperventilate',true]]}]
    };
    $.post('/server/save-permissions', groups, function reqHandler (response) {
	var r = response;
	equal(r.success, true, 'Change My Group permissions: ' + r.msg);
	JQdone('change-group-permissions');
	start();
    });
});

// Delete all groups                                                                                    
JQtest("delete-groups", "change-group-permissions", 1, function tester() {
    var groups = {};
    $.post('/server/save-groups', groups, function reqHandler (response) {
        var r = response;
        equal(r.success, true, 'Deleted all groups');
        JQdone('delete-groups');
        start();
    });
});
