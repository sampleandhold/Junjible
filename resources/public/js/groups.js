// groups.js

// start with empty group list... after getGroupList() completes
// we will update the UI
var groupList = {
    "groups": [{
        "groupname": " ",
        "groupmembers": [{
            "name": " "
        }],
    }],
    "players": [" "]
};
var currentmember = "";
var currentgroup = "";
var permissionsList = {};
$('.togglePerms.on').click(function () {
	$('#permissionslist :checkbox').attr('checked', 'checked');
        $.each(permissionsList.special, function (index, value) {
                this[1] = true;
        });
        $.each(permissionsList.basic, function (index, value) {
                this[1] = true;
        });
        $.each(permissionsList.plugin, function (index, value) {
                $.each(this['plugperms'], function (index, value) {
                        this[1] = true;
                });
	});
	
// junjible.log(permissionsList);
savePermissions()	
});
$('.togglePerms.off').click(function () {
	$('#permissionslist :checkbox').attr('checked', false);
	$.each(permissionsList.special, function (index, value) {
                this[1] = false;
        });
        $.each(permissionsList.basic, function (index, value) {
                this[1] = false;
        });
        $.each(permissionsList.plugin, function (index, value) {
                $.each(this['plugperms'], function (index, value) {
                        this[1] = false;
                });
	});
	
junjible.log(permissionsList);
savePermissions()	
});

function setPerms() {
$(".permbox").on("click", function () {
    if ($(this).attr('checked') == 'checked') {
        var thisPerm = false;
    } else {
        var thisPerm = true;
    }
    var what = $(this).attr('name');
    var whatValues = what.split('-');
    var permType = whatValues[0];
    switch (permType) {
    case 'special':
        var permName = whatValues[1];
        $.each(permissionsList.special, function (index, value) {
            if (this[0] == permName) {
                this[1] = thisPerm;
            }
        });
        break;
    case 'basic':
        var permName = whatValues[1];
        $.each(permissionsList.basic, function (index, value) {
            if (this[0] == permName) {
                this[1] = thisPerm;
            }
        });
        break;
    case 'plugin':
        var plugName = whatValues[1];
        var permName = whatValues[2];
        $.each(permissionsList.plugin, function (index, value) {
            if (this['name'] == plugName) {
                $.each(this['plugperms'], function (index, value) {
                    if (this[0] == permName) {
                        this[1] = thisPerm;
                    }
                });
                };
	});
            break;
        default:
	  //oh well
        }
junjible.log(permissionsList);
savePermissions()
    });
}
// forms for adding stuff --------------------------
function alternateLists() {
  $("div#memberlist ul.list li, div#grouplist ul.list li, div#permissionslist ul.list li").removeClass('even').removeClass('odd');
$("div#memberlist ul.list li:even").addClass("even");
    $("div#memberlist ul.list li:odd").addClass("odd");
$("div#grouplist ul.list li:even").addClass("even");
    $("div#grouplist ul.list li:odd").addClass("odd");
$("div#permissionslist ul.list li:even").addClass("even");
    $("div#permissionslist ul.list li:odd").addClass("odd");    
    }
function addUser() {
    var newname = $('#adduser-field').val();
    if (newname != "add a user to this group..." && newname != ""){
	if (currentgroup == "") {
	    junjible.notify.msg("please create a group first!");
	    return false
	}
	if (newname.indexOf(":") >= 0) {
	    junjible.notify.msg("cannot use : in a name!");
	    return false
	}
	if (newname.indexOf(";") >= 0) {
	    junjible.notify.msg("cannot use ; in a name!");
	    return false;
	}
	// avoid duplicate usernames
	$.each(groupList.groups, function (index, value) {
            if (typeof value != "undefined" && this.groupname == currentgroup){
		$.each(this.groupmembers, function (index, value) {
		    if (typeof value != "undefined" && newname == this.name) {
			junjible.notify.msg("the username " + newname + " is already in the group " + currentgroup);
			newname = "";
			return false;
		    }
		});
            }
	});
	if (newname == "") {
	    // could not return above in the anonymous inner function...
	    return false;
	}
	$('#adduser-field').attr('value','');
	$.each(groupList.groups, function (index, value) {
            if (typeof value != "undefined" && this.groupname == currentgroup){
		var end = this.groupmembers.length;
		this.groupmembers[end] = {'name':newname};
		$('div#memberlist ul li').remove();
                $.each(this.groupmembers, function (index, value) {
                    if (typeof value != "undefined"){
			if (this.name == newname) {
                            $('div#memberlist ul').append('<li class="pointer" title="' + this.name + '">' + this.name + '<a class="red-x member">x</a></li>');
			} else {
                            $('div#memberlist ul').append('<li class="pointer" title="' + this.name + '">' + this.name + '<a class="red-x member">x</a></li>');
			}
                    }
                });
                alternateLists();
            }
        }); 
    }
    saveGroupList(groupList);
    setMemberList();
    $('#memberlist').jScrollPane({showArrows: false});
    setRemovals();
    return false;
}

$('#adduser-button').on('click', addUser);

function addGroup() {
    var end = groupList.groups.length;
    var newgroup = $('#addgroup-field').val();
    if (newgroup.indexOf(":") >= 0) {
	junjible.notify.msg("cannot use : in a name!");
	return false
    }
    if (newgroup.indexOf(";") >= 0) {
	junjible.notify.msg("cannot use ; in a name!");
	return false
    }
    // avoid duplicate groupnames
    $.each(groupList.groups, function (index, value) {
        if (typeof value != "undefined" && newgroup == this.groupname) {
	    junjible.notify.msg("group name " + newgroup + " already in use!");
	    newgroup = "";
	}
    });
    if (newgroup == "") {
	// could not return above in the anonymous inner function...
	return false;
    }
    $('#addgroup-field').attr('value','');
    if (newgroup != "add a group..." && newgroup != "") {
        groupList.groups[end] = {
            "groupname": newgroup,
            "groupmembers": []
        };
        $('div#grouplist ul li').remove();
        $.each(groupList.groups, function (index, value) {
            if (typeof value != "undefined"){
		if (this.groupname == newgroup) {
		    $('div#grouplist ul').append('<li class="pointer" title="' + this.groupname + '">' + this.groupname + '<a class="red-x group">x</a></li>');
		} else {
		    $('div#grouplist ul').append('<li class="pointer" title="' + this.groupname + '">' + this.groupname + '<a class="red-x group">x</a></li>');
		}
            }
        });
    alternateLists();
        $('div#memberlist ul li').remove();
	saveGroupList(groupList);
	currentgroup = newgroup;
	setGroupList();
        $('#grouplist').jScrollPane({showArrows: false});
	setRemovals();
    }
    return false;
}

$('#addgroup-button').on('click', addGroup);

function setMemberList(){
    $('div#memberlist ul li').on('click', function () {
      currentmember = $(this).attr("title");
      getPermissionsList(currentmember, '');
      $('div#grouplist ul li').removeClass("selected");
     $('div#memberlist ul li').removeClass("selected");
     $(this).addClass("selected");
	$('#memberlist').jScrollPane({showArrows: false});
	$('#grouplist').jScrollPane({showArrows: false});
    });
}

function setGroupList(){
    $('div#grouplist ul li').on('click', function () {
        currentgroup = $(this).attr("title");
	getPermissionsList('', currentgroup);
        $('div#memberlist ul li').remove();
        $.each(groupList.groups, function (index, value) {
            if (this.groupname == currentgroup) {
                $.each(this.groupmembers, function (index, value) {
                    if (typeof value != "undefined"){
                        $('div#memberlist ul').append('<li class="pointer" title="' + this.name + '">' + this.name + '<a class="red-x member">x</a></li>');
                    }
                });
            }
        });
	   alternateLists();
        $('div#grouplist ul li').removeClass("selected");
     $(this).addClass("selected");
	setRemovals();
	setMemberList();
        $('#memberlist').jScrollPane({showArrows: false});
        $('#grouplist').jScrollPane({showArrows: false});
    });
    setMemberList();
    setRemovals();
}

function getGroupList(){
    $.get('/server/get-groups', function(groupData) {
	groupList = groupData;
	;
	if (groupList.groups.length > 0) {
	  currentgroup = groupList.groups[0].groupname;
	    $.each(groupList.groups, function (index, value) {
		$('div#grouplist ul').append('<li class="pointer" title="' + this.groupname + '">' + this.groupname + '<a class="red-x group">x</a></li>');
	    });

	    $.each(groupList.groups[0].groupmembers, function (index, value) {
		$('div#memberlist ul').append('<li class="pointer" title="' + this.name + '">' + this.name + '<a class="red-x member">x</a></li>');
	    });
	    currentgroup = groupList.groups[0].groupname;

	}
	$('#grouplist').jScrollPane({showArrows: false});
	$('#memberlist').jScrollPane();

	$('div#grouplist ul li.initial').remove();
	$('div#memberlist ul li.initial').remove();
alternateLists();   
    $('div#grouplist ul li, div#memberlist ul li').removeClass('selected');
	$('div#grouplist ul li').first().addClass('selected');

	setGroupList();
	getPermissionsList('',currentgroup);
    });
}
getGroupList();

function getPermissionsList(member, group){
    if (member =='' && group==''){$("#permInfo").html("no parameters specified");return false;}
    if (member == '' && group != ''){$("#permInfo").html("editing permissions for group: "+group);} 
    if (member != '' && group == ''){$("#permInfo").html("editing permissions for member: "+member);}
    $.post('/server/get-permissions', {
        'member': member,
	'group': group      
    }, function (data) {
	if (data) {
	    permissionsList = data;
	} else {
	    permissionsList = {'member':'test', 'group':'test', 'special':[['ban',true],['whitelist',true]],
			       'basic':[['gamemode',true],['give',true],['kick',false],['pardon',true],['save',false],['say',false],['stop',true],['time',false],['tp',true],['wand',false],['weather',false],['xp',true]],
			       'plugin':[{'name':'superplug','plugperms':[['splish',false],['splash',true],['take a bath',true]]},
					 {'name':'wonderplug','plugperms':[['life',false],['liberty',true],['happiness',true]]},
					 {'name':'nastyplug','plugperms':[['bleed',true],['itch',false],['hyperventilate',true]]}
					]
			      };
	    junjible.notify.msg("no data from server, using test list");
	}
	// junjible.notify.msg("getPermissionsList = " + JSON.stringify(permissionsList));
	putPermissionsList(permissionsList);
	setPerms();
    });
}

function putPermissionsList(list) {
    var checked = "";
    $('div#permissionslist ul li').remove(); 
    $('div#permissionslist ul').append('<li class="list-guide">special permissions</li>');
    $.each(permissionsList.special, function (index, value) {
	checked = "";
	if (this[1] == true){checked = ' checked="checked"';}
	$('div#permissionslist ul').append('<li class="indent2" title="' + this[0] + '">' + this[0] + '<input type="checkbox"' +checked+ ' class="list-check permbox" name="special-' + this[0] + '" value="' + this[0] + '" /></li>');
	
    });
    $('div#permissionslist ul').append('<li class="list-guide">basic permissions</li>');
    $.each(permissionsList.basic, function (index, value) {
	checked = "";
	if (this[1] == true){checked = ' checked="checked"';}
	$('div#permissionslist ul').append('<li class="indent2" title="' + this[0] + '">' + this[0] + '<input type="checkbox"' +checked+ ' class="list-check permbox" name="basic-' + this[0] + '" value="' + this[0] + '" /></li>');
	
    });
    $('div#permissionslist ul').append('<li class="list-guide">plugin permissions</li>');
    $.each(permissionsList.plugin, function (index, value) {
	$('div#permissionslist ul').append('<li class="indent1">&#8226; ' + this.name + '</li>');
	var currentPlug = this.name;
	$.each(this.plugperms, function (index, value) {
	    checked = "";
	    if (this[1] == true){checked = ' checked="checked"';}
	    $('div#permissionslist ul').append('<li class="indent2" title="' + this[0] + '">' + this[0] + '<input type="checkbox"' +checked+ ' class="list-check permbox" name="plugin-' + currentPlug + '-' + this[0] + '" value="' + this[0] + '" /></li>'); 
	});
    });
    alternateLists();
    $('#permissionslist').jScrollPane();
    $('input:checkbox').checkbox();
}

function savePermissions() {
    $.post('/server/save-permissions', permissionsList, function (data) {
	var msg;
	var errmsg = "permissions updated successfully";
	if (typeof data.error != "undefined") {
	    errmsg = data.error;
	}
	if (data.saved) {
	    msg = "permissions updated successfully";
	} else {
	    msg = errmsg;
	}
	junjible.notify.msg(msg);
    });
} 

//////////// red x removals
function setRemovals(){
    $('a.red-x').unbind('click');
    $('a.red-x').on('click', function () {
	var removeThis = $(this).parent().attr("title");
        var context = "member";
	if ($(this).hasClass("group")) {
            context = "group";
	}
	removeItem(removeThis, context);
	saveGroupList(groupList);
	return false;
    });
}

function removeItem(item, context){
    if (context == "member"){
	var done = false;
	var thismember = item;
	$.each(groupList.groups, function (index, value) {
	    if (this.groupname == currentgroup){
		var thisgroup = this;
		$.each(thisgroup.groupmembers, function (index, value) {
		    
		    if (this.name == thismember){
			// do not delete... splice!
			// http://www.javascriptkit.com/jsref/arrays.shtml
			thisgroup.groupmembers.splice(index,1);
		    }
		    
		});
	    }
	});
	$('div#memberlist ul li').remove();
	$.each(groupList.groups, function (index, value) {
	    if (this.groupname == currentgroup){
		$.each(this.groupmembers, function (index, value) {
		    if (typeof value != "undefined"){
			$('div#memberlist ul').append('<li class="pointer" title="' + this.name + '">' + this.name + '<a class="red-x member">x</a></li>');
		    }
		});
    alternateLists();
	    }
	});
    }

    if (context == "group"){
	var done = false;
	$.each(groupList.groups, function (index, value) {
	    if (this.groupname == item){
		// do not delete... splice!
		groupList.groups.splice(index,1);
	    }
	});
	$('div#grouplist ul li').remove();
	$.each(groupList.groups, function (index, value) {
	    if (typeof value != "undefined"){
		$('div#grouplist ul').append('<li class="pointer" title="' + this.groupname + '">' + this.groupname + '<a class="red-x group">x</a></li>');
	    }
	});
	alternateLists();
	var counter = 0;
	currentgroup = "";
	$.each(groupList.groups, function (index, value) {
	    if(typeof this.groupname != "undefined"){
		$('div#memberlist ul li').remove();
		$.each(this.groupmembers, function (index, value) {
		    if (typeof value != "undefined"){
                        $('div#memberlist ul').append('<li class="pointer" title="' + this.name + '">' + this.name + '<a class="red-x member">x</a></li>');
                    }
                });
		    $("div#memberlist ul.list li:even").addClass("even");
    $("div#memberlist ul.list li:odd").addClass("odd");
                done = true;
                currentgroup = this.groupname;
                return false;
	    }
	});
	if (done == false){$('div#memberlist ul li').remove();}
    }
    $('#memberlist').jScrollPane({showArrows: false});
    $('#grouplist').jScrollPane({showArrows: false});
    setRemovals();
    setGroupList();
}

    
function saveGroupList(theData){
    $.post("/server/save-groups", theData, junjible.notify.response);
}
setRemovals();
alternateLists();
