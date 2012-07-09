// tabswitch.js

function guidGenerator() {
    var S4 = function() {
	return (((1+Math.random())*0x10000)|0).toString(16).substring(1);
    };
    return (S4()+S4()+"-"+S4());
}

$(document).ready(function () {
    $('ul#tabNav a').click(function () {
        var whichTab = $(this).attr('id');
        var curChildIndex = $(this).parent().prevAll().length + 1;
        $(this).parent().parent().children('.current').
            removeClass('current');
        $(this).parent().addClass('current');
        $('#panelContainer').children().children('#tabContainer').children('.current').
            fadeOut('fast', function () {
		$(this).removeClass('current');
		$(this).parent().children('div:nth-child(' + curChildIndex + ')').fadeIn('fast', function () {
                    $(this).addClass('current');
                    $('#world-types').jScrollPane();
                    $('.world-pane').jScrollPane();
                    $('.backups-pane').jScrollPane();
                    $('.plugcats-pane').jScrollPane();
                    $('#pluginfo').jScrollPane();
                    $('#grouplist').jScrollPane();
                    $('#memberlist').jScrollPane();
                    $('#permissionslist').jScrollPane();
                    $('#whitelist-select').jScrollPane();
                    $("ul.list li:even").addClass("even");
                    $("ul.list li:odd").addClass("odd");
                    if (document.URL.indexOf("panel") >= 0) {
			alternateLists();
                    }
                    //junjible.log('tab: ' + whichTab);
                    switch (whichTab) {
                    case "server-tab":
			getInstances(setUpWorldPicker);
			// var instanceList = {};
			// var currentInstance = "";
			break;
                    case "worlds-tab":
			putWorlds(); // highlight the active world
			$('a#new-world').unbind('click');
			$('a#new-world').on('click', function () {
                            junjible.worlds.push({
				'name': 'New World '+guidGenerator(),
				'motd': 'Message of the Day',
				'online': true,
				'whitelist': '',
				'private': false
                            });
                            saveWorlds(junjible.worlds.length - 1);
                            return false;
			});
			break;
                    case "groups-tab":
			alternateLists();
			break;
                    case "plugins-tab":
			var pluginList = {};
			getPlugins('Featured Plugins', '');
			$("#plugin-search-button").on('click', function () {
                            var searchterm = $('#plugin-search-field').val();
                            if (searchterm != "Search Plugins..." && searchterm != "") {
				getPlugins('All Plugins', searchterm);
                            }
			});
			break;
                    case "settings-tab":
			GetUser();
			break;
                    case "admin1-tab":
			getDashboard();
			break;			
                    case "admin2-tab":
			$("#users-paginator").remove();
			var p = { "page": 0}; $.post('/server/get-users', p, function(data) {
                            var obj = data;
                            displayUsers(obj);
			});
			$.get('/server/get-users-count', function(data) { 
                            $("#users-page_count").val(Math.ceil(data / 10));
                            generateRows(1, "users");
			});
			break;
                    case "admin3-tab":
			junjible.categoryList = [];
			$.get('/server/get-categories', function(data) {
			    junjible.categoryList = data.categories;
			    $("#plugins-paginator").remove();
			    var p = { "page": 0}; $.post('/server/get-plugins', p, function(data) {
				var obj = data;
				displayPlugins(obj, junjible.categoryList);
			    });
			    $.get('/server/get-plugins-count', function(data) {
				$("#plugins-page_count").val(Math.ceil(data / 10));
				generateRows(1, "plugins");
			    });
			});

			break;
                    default:
			//
                    }
		});
            });
        return false;
    });
    if (window.location.href.indexOf("#login") != -1) {
        $('#obscura, #flyover').fadeIn('slow');
    }
    $.get('/server/intro', function (data) {
        $('#serverpane-container').html(data);
    });
    // Instances
    $('.serverpane ul.panel-sub-menu li a').click(function (event) {
        event.preventDefault();
        $.get($(this).attr('href'), function (data) {
            $('#serverpane-container').html(data);
            return false;
        });
    });
});

// update the DOM
function updateWorlds(worldName) {
    if (worldName) { // may need to update junjible.world
	var i;
	for (i = 0; i < junjible.worlds.length; i++) {
	    if (junjible.worlds[i].name == worldName) {
		if (junjible.world != i) {
		    // junjible.log("world changed position: " + worldName);
		    junjible.world = i;
		}
		break;
	    }
	}
    }
    // this really shouldn't happen, but if it does... 
    if (junjible.world < 0) {
	junjible.log("position of world unknown, setting junjible.world = 0");
	junjible.world = 0;
    }
    $('ul#world-list li').remove();
    if (junjible.worlds.length > 0) {
	$.each(junjible.worlds, function (index) {
	    this.id = index;
	    if (index == junjible.world) {
		var xtraclass = "selected";
	    } else {
		var xtraclass = "";
	    }
	    $('ul#world-list').append('<li class="' + xtraclass + '"><span class="oo" rel="'+index+'" style="cursor:pointer;">' + this.name + '</span><a class="red-x world" rel="' + index + '">x</a></li>');
	});
    }
    $("ul.list li:even").addClass("even");
    $("ul.list li:odd").addClass("odd");
    $('.world-pane').jScrollPane();
    $("ul.list li:even").addClass("even");
    $("ul.list li:odd").addClass("odd");
    if (junjible.worlds.length > 0) {
        $('input[name="worldname"]').val(junjible.worlds[junjible.world].name);
        $('input[name="welcome"]').val(junjible.worlds[junjible.world].motd);
        if (junjible.worlds[junjible.world].private == true || junjible.worlds[junjible.world].private == "true") {
	    $('input[name=private]').attr('checked','checked');
        } else {$('input[name=private]').removeAttr('checked');}
    } else {
        $('input[name="worldname"]').val("");
        $('input[name="welcome"]').val("");
    }
    // because the position MIGHT have changed make SURE the "Save"
    // button refers to the current world
    $("#editworld-button").attr('val', junjible.world);
    // if you click on a world.. highlight it...
    $('ul#world-list li span.oo').on("click", function () {
        var newWorld = $(this).attr('rel');
	$("#editworld-button").attr('val', newWorld);
        putWorlds(newWorld);
    });
    // if you click "Save" update the values of the current world
    $('#editworld-button').unbind('click');
    $('#editworld-button').on('click', function () {
	var editMe = $(this).attr('val');
        editWorld(editMe);
    });
    setWorldRemovals();
}

// if neither worldIndex or worldName are given
//   then highlight the active world (from the instance)
// if worldName is given then use that instead of the index
// (because the order can change)
function putWorlds(worldIndex, worldName) {
    var first = false;
    if (worldIndex) {
	junjible.world = worldIndex;
    } else {
	// junjible.log("putWorlds() and junjible.instance = " + junjible.instance + " junjible.world = " + junjible.world);
	// if ( junjible.instance < 0 &&
	if ( junjible.world < 0 ) {
	    // first time -- we must find active instance
	    first = true;
	}
	// else do NOT change highlighted world
    }
    if (first) {
	// junjible.log("putWorlds() getting instances, then worlds, to set correct active world");
	getInstances(function() {
	    getWorlds(function() {
		if (junjible.instance >= 0) {
		    updateWorlds(junjible.instances[junjible.instance].world);
		} else {
		    updateWorlds();
		}
	    });
	});
    } else {
	if (worldName) {
	    // junjible.log("putWorlds() getting worlds, then highlighting = " + worldName);
	    getWorlds(function () {
		updateWorlds(worldName);
	    });
	} else {
	    // junjible.log("putWorlds() getting worlds");
	    getWorlds(updateWorlds);
	}
    }
}

function saveWorlds(worldIndex) {
    // NOTE without UID we rely on the name...
    var worldName = junjible.worlds[worldIndex].name;
    // junjible.log("saveWorlds(" + worldIndex + ") = " + JSON.stringify(junjible.worlds));
    $.post("/server/save-worlds", 
	   { "worlds": junjible.worlds }, 
	   function(data) {
	       junjible.notify.msg(data.msg);
	       // the position may have changed... give the name
	       putWorlds(worldIndex, worldName);
	   });
}

function editWorld(worldIndex) {
    // junjible.log('editWorld worldIndex= ' + worldIndex);
    if (worldIndex >= junjible.worlds.length) {
	worldIndex = junjible.worlds.length - 1;
	// junjible.log('editWorld TOO HIGH worldIndex= ' + worldIndex);
    }
    // junjible.log("edit world name" + junjible.worlds[worldIndex].name);
    if ( junjible.instance >= 0 &&
	 junjible.worlds[worldIndex].name ==
	 junjible.instances[junjible.instance].world &&
	 junjible.instances[junjible.instance].state == "running") {
	junjible.notify.msg("cannot edit world " + junjible.instances[junjible.instance].world + " while running!");
	return;
    }
    var oldName = junjible.worlds[worldIndex].name;
    var newName = $('input[name="worldname"]').val();
    if ( newName.indexOf(":") >= 0 ||
	 newName.indexOf(";") >= 0 ) {
	junjible.notify.msg("a world name must NOT contain : or ; please remove");
	return;
    }
    var i;
    for (i = 0; i < junjible.worlds.length; i++) {
	if ( i != worldIndex &&  newName == junjible.worlds[i].name ) {
	    junjible.notify.msg("the world name \"" + newName + "\" is already taken, please make the name unique");
	    return;
	}
    }
    junjible.worlds[worldIndex].name = newName;
    if (junjible.worlds[worldIndex].name != oldName) {
	junjible.log("a world changed name from " + oldName + " to " + junjible.worlds[worldIndex].name);
	junjible.worlds[worldIndex].oldname = oldName;
    }
    junjible.worlds[worldIndex].motd = $('input[name="welcome"]').val();

    if ($('input[name="private"]').is(':checked')) {
        junjible.worlds[worldIndex].private = 'true';
    } else {
        junjible.worlds[worldIndex].private = 'false';
    }
    saveWorlds(worldIndex);
    
    return false;
}

function setWorldRemovals() {
    $('a.red-x.world').unbind('click');
    $('a.red-x.world').on('click', function () {
        var removeThis = $(this).attr("rel");
	junjible.log("delete removeThis=" + removeThis + " junjible.world=" + junjible.world); // DEBUG
	if ( junjible.instance >= 0 &&
	     junjible.worlds[removeThis].name ==
	     junjible.instances[junjible.instance].world ) {
	    junjible.notify.msg("cannot delete world " + junjible.instances[junjible.instance].world + " while active!");
	} else if (junjible.worlds.length <= 1) {
	    junjible.notify.msg("cannot delete the only world!");
	} else {
	    if ( ( (junjible.world > removeThis) ||
		   ( junjible.world == removeThis &&
		     junjible.world == junjible.worlds.length - 1 ) ) &&
		 (junjible.world > 0) ) {
		junjible.world--;
	    }
            junjible.worlds.splice(removeThis, 1);
            saveWorlds(junjible.world);
	}
        return false;
    });
}
// Plugins-------------------------------
function getPlugins(cat, term) {
    $.get('/server/get-userplugins', {'category': cat, 'search': term},
	  function(data) {
	      pluginList = data;
	      putPlugins(cat, term);
	  });

}

function putPlugins(cat, term) {
    $('.plugcats-pane ul.list li').remove();
    $('.plcont').remove();
    var xtraClass = "";
    $.get('/server/get-categories', function(d) {
	var categories = d.categories;
	categories.unshift('My Plugins', 'All Plugins', 'Featured Plugins');
	$.each(categories, function (i) {
            if (i % 2 === 0) {
		xtraClass1 = 'even';
            } else {
		xtraClass1 = 'odd';
            }
            if (this == cat) {
		xtraClass2 = " selected"
            } else {
		xtraClass2 = ""
            };
            $('.plugcats-pane ul.list').append('<li class="pointer plugCatList ' + xtraClass1 + xtraClass2 + '">' + this + '</li>');
	});
	$('li.plugCatList').on('click', function () {
            var targetCat = $(this).html();
            getPlugins(targetCat, '');
	});
	$('.plugcats-pane').jScrollPane();
    });
    if (term && term != "") {
        var header = 'Searching for "' + term + '" in ';
    } else {
        var header = "";
    }
    header += cat;
    $('h3.cat-browser').html(header);
    var plugHtml = "";
    var featHtml = "";
    $.each(pluginList.plugins, function (i) {
        this.id = i;
    });
    $.each(pluginList.plugins, function (i) {
        if (cat == "All Plugins" || (cat == "My Plugins" && this.activated == true) || cat == this.category || (cat == "Featured Plugins" && this.featured == true)) {
            if (this.activated == true) {
                var activatedText = "remove this plugin";
            } else {
                var activatedText = "install this plugin";
            }
            if (cat == "Featured Plugins" && this.featured == true) {
                featHtml += '<li><span class="featdesc"><h3>' + this.name + '</h3>' + this.desc + '</span><img class="featimg pointer" name="' + this.id + '" src="' + this.screenshoturl[0] + '" /></li>';
            }
            plugHtml += '<div class="plcont" id="plugin-' + i + '">\n';
            plugHtml += '<img class="plthumb" name="' + this.id + '" src="' + this.thumburl + '" />\n';
            plugHtml += '<div name="' + this.id + '" class="pltitle">' + this.name + '</div>\n';
            plugHtml += '<div class="plcat">' + this.category + '</div>\n';
            /*
              plugHtml += '<div class="ratings-container">\n';
              plugHtml += '<input class="star {split:2}" name="plugin-' + i + '-rating-3" type="radio" value="1.0" />\n';
              plugHtml += '<input class="star {split:2}" name="plugin-' + i + '-rating-3" type="radio" value="2.0" />\n';
              plugHtml += '<input class="star {split:2}" name="plugin-' + i + '-rating-3" type="radio" value="3.0" />\n';
              plugHtml += '<input class="star {split:2}" name="plugin-' + i + '-rating-3" type="radio" value="4.0" />\n';
              plugHtml += '<input class="star {split:2}" name="plugin-' + i + '-rating-3" type="radio" value="5.0" />\n';
              plugHtml += '<span class="ratings-count">' + this.numratings + ' ratings</span>\n';
              plugHtml += '</div>\n';
	    */
            plugHtml += '<input name="' + this.id + '" class="searchbutton pointer getplugz" type="button" value="' + activatedText + '" /></p>';
            plugHtml += '</div>\n';
        }
    });
    if (plugHtml == "") {
        plugHtml = "<p>No Plugins Found</p>"
    }
    $("#plug-container").html("");
    if (cat == "Featured Plugins") {
        $("#plug-container").append('<div class="featshow"><ul id="featshots">');
        $("#featshots").append(featHtml);
        $("#plug-container").append('<div style="clear:both;">');
        $('#featshots').anythingSlider({
            autoplay: true,
            delay: 4000,
	    hashTags : false,
            buildNavigation : false,
            pauseOnHover: true,
            resizeContents: false,
            // If true, solitary images/objects in the panel will expand to fit the viewport
            navigationSize: false,
            // Set this to the maximum number of visible navigation tabs; false to disable
            onSlideBegin: function (e, slider) {
                // keep the current navigation tab in view
                slider.navWindow(slider.targetPage);
            }
        });
        $('#featshots').data('AnythingSlider').startStop(true);
    }
    $("#plug-container").append(plugHtml);
    $('#pluginfo').jScrollPane();
    //$('input[type=radio].star').rating();
    $('.plthumb, .pltitle, .featimg').on('click', function () {
        $('#pluginDetails').remove();
        var target = $(this).attr('name');
        $.each(pluginList.plugins, function () {
            if (this.id == target) {
                var plugContent = "";
                plugContent += '<div id="pluginDetails" style="display:none;">';
                plugContent += '<div class="plugshow"><ul id="shots">';
                $.each(this.screenshoturl, function () {
                    plugContent += '<li><img src="' + this + '" class="scrshot" /></li>';
                });
                plugContent += '</ul></div>';
                plugContent += '<span id="closePlugdeets" class="pointer"><img src="/images/close.png" /></span>';
                plugContent += '<p>' + this.category + '</p>';
                plugContent += '<h3>' + this.name + '</h3>';
                plugContent += '<p>' + this.desc + '<p>';
                plugContent += '<div id="theSettings" style="display:none;">';
                plugContent += '<h3>settings</h3>';
                plugContent += '<p>';
                plugContent += '<ul class="plugsettingslist list">';
                $.each(this.settings, function (i) {
                    if (i % 2 === 0) {
                        xtraClass1 = 'even';
                    } else {
                        xtraClass1 = 'odd';
                    }
                    $.each(this, function (key, value) {
                        if (typeof value == "boolean") {
                            if (value == true) {
                                var checked = ' checked="checked"';
                            } else {
                                var checked = "";
                            }
                            plugContent += '<li class="' + xtraClass1 + '">' + key + '<input type="checkbox"' + checked + ' class="list-check permbox plperm" name="' + key + '" value="' + value + '" /><div style="clear:both;"></div>';
                        }
                        if (typeof value == "string") {
                            plugContent += '<li class="' + xtraClass1 + '">' + key + '<input type="text" class="plperm" style="width:100px;" name="' + key + '" value="' + value + '" /><div style="clear:both;"></div>';
                        }
                    });
                });
                plugContent += '</ul></p><p><input id="editplugs-button" class="searchbutton" name="'+this.name+'" type="button" value="Save" /></p>';
                plugContent += '</div>';
                if (this.activated == true) {
                    plugContent += '<p><input id="detTog" name="' + this.id + '" class="searchbutton pointer getplugz" type="button" value="remove this plugin" /></p>';
                } else {
                    plugContent += '<p><input id="detTog" name="' + this.id + '" class="searchbutton pointer getplugz" type="button" value="install this plugin" /></p>';
                }
                plugContent += '</p>';
                plugContent += '<div style="clear:both;"></div>';
                plugContent += '</div>';
                $("#plug-container").prepend(plugContent);
                if (this.activated == true) {
                    $("#theSettings").slideDown('slow');
		    setPlugSave();
                }
                if (cat == "Featured Plugins") {
                    $('.featshow').slideUp('slow', function () {})
                }
                $('#pluginfo').data('jsp').scrollTo(0,0,true);
                $('#pluginDetails').slideDown('slow', function () {
                    $('#pluginfo').jScrollPane();
                    $('input:checkbox').checkbox();
                    $('#closePlugdeets').on('click', function () {
                        $('#pluginDetails').slideUp('slow', function () {
                            if (cat == "Featured Plugins") {
                                $('.featshow').slideDown('slow', function () {
                                    $('#pluginfo').jScrollPane();
                                })
                            } else {
                                $('#pluginfo').jScrollPane();
                            }
                        });
                    });
                });
            }
        });
	$('.getplugz').unbind('click');
        $('.getplugz').on('click', function () {
            var togglethis = $(this).attr('name');
            if ($(this).attr('value') == 'install this plugin') {
                $(this).attr('value', 'remove this plugin');
                if ($('#detTog').attr('name') == togglethis) {
                    $('#theSettings').slideDown('slow');
		    setPlugSave();
                }
            } else {
                $(this).attr('value', 'install this plugin');
                if ($('#detTog').attr('name') == togglethis) {
                    $('#theSettings').slideUp('slow');
                }
            }
            var newstate = $(this).attr('value');
            $('.getplugz').each( function (){
		if ($(this).attr('name') == togglethis){
		    $(this).attr('value', newstate); 
		}
	    });
            $.each(pluginList.plugins, function () {
                if (this.id == togglethis) {
                    if (this.activated == true) {
                        this.activated = false;
                    } else {
                        this.activated = true;
                    }
                    savePlugins();
                }
            });
        });
        $('#shots').anythingSlider({
            resizeContents: false,
	    hashTags : false,
            // If true, solitary images/objects in the panel will expand to fit the viewport
            navigationSize: false,
            // Set this to the maximum number of visible navigation tabs; false to disable
            onSlideBegin: function (e, slider) {
                // keep the current navigation tab in view
                slider.navWindow(slider.targetPage);
            }
        });
    });
    
    $('.getplugz').unbind('click');
    $('.getplugz').on('click', function () {
        var togglethis = $(this).attr('name');
        if ($(this).attr('value') == 'install this plugin') {
            $(this).attr('value', 'remove this plugin');
            if ($('#detTog').attr('name') == togglethis) {
                $('#theSettings').slideDown('slow');
		setPlugSave();
            }
        } else {
            $(this).attr('value', 'install this plugin');
            if ($('#detTog').attr('name') == togglethis) {
                $('#theSettings').slideUp('slow');
            }
        }
        var newstate = $(this).attr('value');
        $('.getplugz').each( function (){
	    if ($(this).attr('name') == togglethis){
		$(this).attr('value', newstate); 
	    }
	});
        $.each(pluginList.plugins, function () {
            if (this.id == togglethis) {
                if (this.activated == true) {
                    this.activated = false;
                } else {
                    this.activated = true;
                }
                savePlugins();
            }
        });
    });
}
function setPlugSave () {
    $('#editplugs-button').on('click', function(){
	var target = $(this).attr('name');
	var newsettings = [];
	$('input.plperm').each(function(index) {
	    var thisname = $(this).attr('name');
	    var theobj = {};
	    if($(this).attr('type')=="checkbox"){

		if($(this).is(':checked')){var h = true}else{var h=false}
		theobj[thisname] = h;
		newsettings.push(theobj);
	    }
	    if($(this).attr('type')=="text"){
		theobj[thisname] = $(this).val()
		newsettings.push(theobj);
	    }
	});
	//junjible.log(newsettings);
        $.each(pluginList.plugins, function () {
	    if(this.name == target){this.settings = newsettings;}
            
            
        });
	// junjible.log(pluginList); 
	savePlugins();
    });
}
function savePlugins() {
    $.post('/server/save-userplugins', pluginList, junjible.notify.response);
}
(function ($) {
    /* Little trick to remove event bubbling that causes events recursion */
    var CB = function (e) {
        if (!e) var e = window.event;
        e.cancelBubble = true;
        if (e.stopPropagation) e.stopPropagation();
    };
    $.fn.checkbox = function (options) {
        /* IE6 background flicker fix */
        try {
            document.execCommand('BackgroundImageCache', false, true);
        } catch (e) {}
        /* Default settings */
        var settings = {
            cls: 'jquery-checkbox',
            /* checkbox  */
            empty: '/images/empty.png' /* checkbox  */
        };
        /* Processing settings */
        settings = $.extend(settings, options || {});
        /* Adds check/uncheck & disable/enable events */
        var addEvents = function (object) {
            var checked = object.checked;
            var disabled = object.disabled;
            var $object = $(object);
            if (object.stateInterval) clearInterval(object.stateInterval);
            object.stateInterval = setInterval(

                function () {
                    if (object.disabled != disabled) $object.trigger((disabled = !! object.disabled) ? 'disable' : 'enable');
                    if (object.checked != checked) $object.trigger((checked = !! object.checked) ? 'check' : 'uncheck');
                }, 500 /* in miliseconds. Low numbers this can decrease performance on slow computers, high will increase responce time */ );
            return $object;
        };
        //try { junjible.log(this); } catch(e) {}
        /* Wrapping all passed elements */
        return this.each(function () {
            var ch = this; /* Reference to DOM Element*/
            var $ch = addEvents(ch); /* Adds custom events and returns, jQuery enclosed object */
            /* Removing wrapper if already applied  */
            if (ch.wrapper) ch.wrapper.remove();
            /* Creating wrapper for checkbox and assigning "hover" event */
            ch.wrapper = $('<span class="' + settings.cls + '"><span class="mark"><img src="' + settings.empty + '" /></span></span>');
            ch.wrapperInner = ch.wrapper.children('span:eq(0)');
            ch.wrapper.hover(

		function (e) {
                    ch.wrapperInner.addClass(settings.cls + '-hover');
                    CB(e);
		}, function (e) {
                    ch.wrapperInner.removeClass(settings.cls + '-hover');
                    CB(e);
		});
            /* Wrapping checkbox */
            $ch.css({
                position: 'absolute',
                zIndex: -1,
                visibility: 'hidden'
            }).after(ch.wrapper);
            /* Ttying to find "our" label */
            var label = false;
            if ($ch.attr('id')) {
                label = $('label[for=' + $ch.attr('id') + ']');
                if (!label.length) label = false;
            }
            if (!label) {
                /* Trying to utilize "closest()" from jQuery 1.3+ */
                label = $ch.closest ? $ch.closest('label') : $ch.parents('label:eq(0)');
                if (!label.length) label = false;
            }
            /* Labe found, applying event hanlers */
            if (label) {
                label.hover(

                    function (e) {
			ch.wrapper.trigger('mouseover', [e]);
                    }, function (e) {
			ch.wrapper.trigger('mouseout', [e]);
                    });
                label.click(function (e) {
                    $ch.trigger('click', [e]);
                    CB(e);
                    return false;
                });
            }
            ch.wrapper.click(function (e) {
                $ch.trigger('click', [e]);
                CB(e);
                return false;
            });
            $ch.click(function (e) {
                CB(e);
            });
            $ch.bind('disable', function () {
                ch.wrapperInner.addClass(settings.cls + '-disabled');
            }).bind('enable', function () {
                ch.wrapperInner.removeClass(settings.cls + '-disabled');
            });
            $ch.bind('check', function () {
                ch.wrapper.addClass(settings.cls + '-checked');
            }).bind('uncheck', function () {
                ch.wrapper.removeClass(settings.cls + '-checked');
            });
            /* Disable image drag-n-drop for IE */
            $('img', ch.wrapper).bind('dragstart', function () {
                return false;
            }).bind('mousedown', function () {
                return false;
            });
            /* Firefox antiselection hack */
            if (window.getSelection) ch.wrapper.css('MozUserSelect', 'none');
            /* Applying checkbox state */
            if (ch.checked) ch.wrapper.addClass(settings.cls + '-checked');
            if (ch.disabled) ch.wrapperInner.addClass(settings.cls + '-disabled');
        });
    }
})(jQuery);
