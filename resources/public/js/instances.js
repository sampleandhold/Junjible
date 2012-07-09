// instances.js

    if (typeof junjible['instance'] == 'undefined' ||
	typeof junjible['instances'] != 'object') {
	junjible.instance = -1;
	junjible.instanceCommand = "";
	junjible.instances = new Array();
	junjible.instances.push({
	    "name": "My Server",
	    "state": "stopped",
	    "msg": ""
	});
    }

function stopLight(color) {
    // junjible.log("stopLight(" + color + ")");
    if (color == 'green') {
        $("#top-right-indicator").removeClass().addClass("go");
    } else if (color == 'red') {
        $("#top-right-indicator").removeClass().addClass("stop");
    } else if (color == 'yellow') {
        $("#top-right-indicator").removeClass().addClass("yield");
    }
}

// sets the stopLight color based on the state
function stopLightState(state) {
    var color = 'yellow';
    if (state == 'running') {
	color = 'green';
    } else if (state == 'stopped') {
	color = 'red';
    }
    stopLight(color);
}

function instanceName() {
    var name = "";
    if (junjible.instance >= 0) {
	name = junjible.instances[junjible.instance].name;
    }
    return name;
}

function putInstances(notify){
    var host = "";
    var port = ""
    var msg = "";
    
    if (junjible.instance >= 0) {
	host = junjible.instances[junjible.instance].host;
	port = junjible.instances[junjible.instance].port;
	var state = junjible.instances[junjible.instance].state;
	msg = junjible.instances[junjible.instance].msg;
	if (msg != "") {
	    if (msg.indexOf(state) != 0) {
		// avoid repeating state, such as error:
		msg = state + ": " + msg;
	    }
	} else {
	    msg = state;
	}
	if (notify) {
	    junjible.notify.msg(msg);
	}
    }
    $("#hostname").html(host);
    $("#portname").html(port);
    $("#servermsg").html(msg);
}

function setInstanceMsgState(msg, state) {
    if (junjible.instance >= 0) {
	junjible.instances[junjible.instance].msg = msg;
	if (state) {
	    junjible.instances[junjible.instance].state = state;
	}
    }
    putInstances();
}

function getInstances(callback) {
    if (! junjibleLoggedIn()) {
	return; // don't bother if we are not logged in yet
    }
    junjible.notify.msg("updating server status...");
    setInstanceMsgState("updating...");
    $.get('/server/get-instances', function(theData) {
	if (typeof theData['instances'] != 'undefined' && 
	    typeof theData['instances'] != null && 
	    theData['instances'].length >=1) {
	    junjible.instances = theData.instances;
	    if (junjible.instances.length > 0) {
		junjible.instance = 0;
		stopLightState(junjible.instances[junjible.instance].state);
	    } else {
		junjible.instance = -1;
		stopLightState("error");
	    }
	}
	putInstances(true);
	if (callback) callback();
    });
}

function clearInstanceCommand() {
    junjible.instanceCommand = "";
}

function instanceAction(command) {
    var name;
    var gerund;
    // var pasttense;
    var color;

    if (junjible.instanceCommand != "") {
	junjible.notify.msg(junjible.instanceCommand + " already requested: please stand by for the server response...");
	return;
    }
    name = instanceName();
    if (name == "") {
	return; // we do not have an instance
    }
    if (command == 'start') {
	gerund = 'starting';
	// pasttense = 'started';
	color = 'green';
    } else if (command == 'stop') {
	gerund = 'stopping';
	// pasttense = 'stopped';
	color = 'red';
    } else if (command == 'restart') {
	gerund = 'restarting';
	// pasttense = 'restarted';
	color = 'green';
    } else {
	return; // unknown command
    }
    // Block additional commands while changing state...
    junjible.instanceCommand = command;
    var timeout = 30; // but ONLY for timeout seconds
    setTimeout("clearInstanceCommand();", timeout * 1000);
    stopLight('yellow');
    setInstanceMsgState(gerund + ' now, please stand by...');
    junjible.notify.msg(gerund + ' ...');
    $.post('/server/control', {
        "command": command,
        "instance": name
    }, function (result) {
	clearInstanceCommand();
	if (result.success) {
	    setInstanceMsgState(result.msg, result.state);
	    stopLight(color);
	} else {
	    setInstanceMsgState(result.msg, result.state);
	}
	// Detailed instance state probably changed (e.g host, port)
	// update it now
	getInstances();
    });
}

function panelActions(element) {
    if (element.hasClass("startServer")) {
	instanceAction('start');
    } else if (element.hasClass("stopServer")) {
	instanceAction('stop');
    } else if (element.hasClass("restartServer")) {
	instanceAction('restart');
    }
}

// set up world-picker
function setUpWorldPicker() {
    if (! junjibleLoggedIn()) {
	return; // don't bother if we are not logged in yet
    }
    if ( junjible.instance >= 0 ) {
	// junjible.log("setUpWorldPicker will highlight: " + junjible.instances[junjible.instance].world);
    } else {
	junjible.log("setUpWorldPicker will NOT highlight an instance.");
    }
    getWorlds(function () {
	$('#setworld').html('');
	$('#setworld').append('<h3>activated world</h3>');
	$('#setworld').append('<div id="setworld2">');
	$('#setworld2').css({'height':'200px','width':'300px','overflow':'auto'});
	$('#setworld2').append('<ul id="setworld-list" class="list">');
	var listHtml = "";
	if (junjible.worlds.length > 0) {
	    $.each(junjible.worlds, function(index, value) {
		if ( junjible.instance >= 0 &&
		     junjible.instances[junjible.instance].world == this.name ) {
		    var xtraClass = " selected";
		} else {
		    var xtraClass = "";
		}
		listHtml += '<li class="worldPicker'+xtraClass+'" rel="'+this.name+'">'+this.name+'</li>';
	    });
	}
	$('#setworld-list').append(listHtml);
	var firstChild = $("ul#setworld-list > li.selected");
	firstChild.prependTo(firstChild.parent("ul"));

	$("ul.list li:even").addClass("even");
	$("ul.list li:odd").addClass("odd");
	$('#setworld2').jScrollPane();
	$('.panelControl').unbind("click");
	$('.panelControl').on("click", function () {
	    panelActions($(this));
	    return false;
	});
	$('li.worldPicker:not(.selected)').on('click', function () {
	    var myNewWorld = $(this).attr('rel');
	    junjible.nag('Do you really want to make '+myNewWorld+' the active world on your server (will require a restart?)', myNewWorld, setNewWorld);

	});
    });
}

function setNewWorld(worldname) {
    if ( junjible.instance >= 0 ) {
	if (junjible.instanceCommand != "") {
	    junjible.notify.msg(junjible.instanceCommand + " already requested: please stand by for the server response...");
	    return;
	}
	junjible.instanceCommand = "activating";
	var timeout = 30; // but ONLY for timeout seconds
	setTimeout("clearInstanceCommand();", timeout * 1000);
	var oldWorld = junjible.instances[junjible.instance].world;
	junjible.log("requesting to set active world from " + oldWorld + " to " + worldname);
	junjible.instances[junjible.instance].world = worldname;
	saveInstance(oldWorld);
    }
}

// if oldWorld is defined we are trying to change the world to the 
// one in junjible.instances[junjible.instance].world
// if this doesn't work, we put the oldWorld back in place
function saveInstance(oldWorld) {
    $.post('/server/save-instances', 
	   { "instances": junjible.instances }, 
	   function (result) {
	       var msg;
	       if (result.success == true) {
		   msg="You have changed your activated world.  Please restart your server to deploy it.";
		   stopLight("red");
		   // update world list by name
		   putWorlds(0,junjible.instances[junjible.instance].world);
	       }else{
		   msg = result.msg;
		   if (oldWorld) {
		       junjible.instances[junjible.instance].world = oldWorld;
		   }
	       }
	       junjible.notify.msg(msg);
	       clearInstanceCommand();
	       setUpWorldPicker(); 
	   });
}

// Every time this file is loaded we will
// call getInstances and THEN call setUpWorldPicker once
// the junjible.instances structure has been updated
getInstances(setUpWorldPicker);
