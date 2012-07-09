function displayUsers(data) {
    var xclass = "";
    var html = '<table id="usertable">';
    html += '<tr><td>username</td><td>password</td><td>minecraft</td><td>account type</td><td>account status</td><td>suspended</td><td>service level</td><td>usage</td><td>&nbsp;</td></tr>';
    $.each(data.users, function (index) {
        if (index % 2 == 0) {
            xclass = 'even'
        } else {
            xclass = 'odd'
        }
        html += '<tr id="row-' + this.username + '" class="' + xclass + '" title="' + this.username + '"><td>' + this.username + '</td><td><input type="button" value="reset password" class="userPass" /></td><td><input type="text" class="userMc" value="' + this.minecraft + '" />';
        if (this.type == "user") {
            html += '<td><select class="userType"><option selected="selected">user</option><option>friend</option><option>staff</option></select></td>';
        } else if (this.type == "friend"){
            html += '<td><select class="userType"><option>user</option><option selected="selected">friend</option><option>staff</option></select></td>';
	} else if (this.type == "staff"){
            html += '<td><select class="userType"><option>user</option><option>friend</option><option selected="selected">staff</option></select></td>';
        } else {
	    html += '<td><select class="userType"><option selected="selected">user</option><option>friend</option><option>staff</option></select></td>';
	}
	html += '<td>' + this.status + '</td>';
        if (this.suspended == true) {
            html += '<td><select class="userSusp"><option selected="selected" value="true">yes</option><option value="false">no</option></select></td>';
        } else {
            html += '<td><select class="userSusp"><option value="true">yes</option><option selected="selected" value="false">no</option></select></td>';
        }
	if (this.level == "10") {
            html += '<td><select class="userLev"><option>5</option><option selected="selected">10</option><option>15</option><option>20</option></select></td>';
        } else if (this.level == "15"){
            html += '<td><select class="userLev"><option>5</option><option>10</option><option selected="selected">15</option><option>20</option></select></td>';
	}else if (this.level == "20"){
            html += '<td><select class="userLev"><option>5</option><option>10</option><option>15</option><option selected="selected">20</option></select></td>';
        } else {
	    html += '<td><select class="userLev"><option selected="selected">5</option><option>10</option><option>15</option><option>20</option></select></td>';
	}
	html += '<td>' + this.usage + '</td>';
        html += '<td><input type="checkbox" class="userDel" />delete?&nbsp;&nbsp;<input type="button" value="save" class="userSave" /></td></tr>';
	
    });
    html += '</table>';
    $("#users-content").html(html);
    $(".userSave").on("click", function(){
     var which = $(this).parent().parent().attr("title");
     var x = false;
     if ($("#row-"+which+" .userDel").is(':checked')){
     x=window.confirm("Are you sure you want to delete this user?  This cannot be undone.");
    }
     var data = {};
     data.user = {}
     data.user.username = which;
     data.user.minecraft = $("#row-"+which+" .userMc").val();
     data.user.type = $("#row-"+which+" .userType option:selected").text();
     data.user.suspended = $("#row-"+which+" .userSusp option:selected").val() == "true";
     data.user.level = $("#row-"+which+" .userLev option:selected").text();
     data.user.delete = x;
     saveAdminUser(data);
      
    });
        $(".userPass").on("click", function(){
     var which = $(this).parent().parent().attr("title");
     var data = {};
     data.user = {}
     data.user.username = which;
     data.user.password = "reset";
     saveAdminUser(data);
      
    });
}
function saveAdminUser(row) {
    $.post('/server/save-user', row, function (data) {
        var msg;
        var errmsg = "user not updated";
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
    });
}

function displayPlugins(data) {
    var xclass = "";
    var html = '<table id="plugintable">';
    html += '<tr><td style="text-align:left;">name</td><td style="text-align:left;">category</td><td style="text-align:left;">featured</td><td style="text-align:left;">available</td><td></td></tr>';
    var catopts;
    // junjible.notify.msg("data=" + JSON.stringify(data));
    $.each(data.plugins, function (index) {
        catopts = "";
        var mycat = this.category;
        $.each(junjible.categoryList, function (i) {
            if (this == mycat) {
                var cattxt = ' selected="selected"';
            } else {
                var cattxt = '';
            }
            catopts += '<option' + cattxt + '>' + this + '</option>';
        });
	// defensive
	if (typeof this.name == 'undefined') {
	    junjible.notify.msg("displayPlugins name was undefined for plugin=" + this.plugin);
	    this.name = this.plugin;
	}
        if (index % 2 == 0) {
            xclass = 'even'
        } else {
            xclass = 'odd'
        }
        html += '<tr class="' + xclass + '" title="' + this.name + '">';
        html += '<td style="text-align:left;">' + this.name + '</td>';
        html += '<td style="text-align:left;"><select name="cat-select" rel="' + this.name + '">' + catopts + '</select></td>';
        if (this.featured == true) {
            html += '<td><input checked="checked" name="featured-toggle" type="checkbox" rel="' + this.name + '" /></td>';
        } else {
            html += '<td><input name="featured-toggle" type="checkbox" rel="' + this.name + '" /></td>';
        }
        if (this.available == true) {
            html += '<td><input checked="checked" name="available-toggle" type="checkbox" rel="' + this.name + '" /></td>';
        } else {
            html += '<td><input name="available-toggle" type="checkbox" rel="' + this.name + '" /></td>';
        }
        html += '<td><input class="editplug" rel="' + this.name + '" type="button" value="details" /> <input type="button" class="save-plugin" value="save" rel="' + this.name + '" name="' + this.plugin + '" />';
        html += '<div class="plugedit" rel="' + this.name + '"><a class="red-x" onclick="$(this).parent().fadeOut();return false;">x</a></span><table><tr><td>name</td><td style="text-align:left;">' + this.name + '</td></tr>';
        html += '<tr><td>description</td><td style="text-align:left;"><textarea rows="5" cols="60" rel="' + this.name + '" name="description-text">' + this.desc + '</textarea></td></tr>';
        html += '<tr><td>icon path</td><td style="text-align:left;"><input type="text" rel="' + this.name + '" value="' + this.thumburl + '" name="icon-path" /></td></tr>';
        html += '<tr><td>screenshot paths<br />(separate each path with *return*)</td><td style="text-align:left;"><textarea rows="5" cols="60" rel="' + this.name + '" name="screenshot-paths">';
        var scrText = "";
        $.each(this.screenshoturl, function (i) {
            if (this != "") {
                scrText += this + '\n';
            }
        });
        html += scrText;
        html += '</textarea></td></tr>';
        html += '<tr><td>settings<br /></td><td style="text-align:left;"><div class="settingsList" rel="' + plugName + '">';
        var setText = '<p style="height:130px;width:450px;overflow:auto;">';
        var plugin = this.plugin;
        var plugName = this.name;
        $.each(this.settings, function () {
            if (this != "") {
	       $.each(this, function (key, value) {
		 setText += '<span><a onclick="$(this).parent().fadeOut(function(){$(this).remove()});return false;" class="red-x" style="margin-right:10px">x</a>';
                setText += '<input type="text" name="settings-name" rel="' + plugName + '" value="' + key + '" />';
                if (value == true) {
                    setText += '&nbsp;boolean?&nbsp;<input name="settings-type" checked="checked" type="checkbox" rel="' + plugName + '" plugin="' + plugin + '" /></span><br />';
                } else {
                    setText += '&nbsp;boolean?&nbsp;<input name="settings-type" type="checkbox" rel="' + plugName + '" /></span><br />';
                }
           
	    });
	    }
        });
        setText += "</p>";
        html += setText;
        html += '<input type="button" class="add-setting" value="add new setting" rel="' + plugName + '" />';
        html += '</div></td></tr>';
        html += '<tr><td><input type="button" class="save-plugin" value="save" rel="' + plugName + '" name="' + plugin + '" /></td><td></td></tr>';
        html += '</table>';
        html += '</td></tr>';
    });
    html += '</table><br /><input class="saveplug" type="button" value="save" />';
    $("#plugins-content").html(html);
    $('input:checkbox').checkbox();
    $('.save-plugin').unbind('click');
    $('.save-plugin').on('click', function () {
      
                setPlugSave($(this));
    });
    $("input.editplug").on('click', function () {
        $('input.add-setting').on('click', function () {
	var plugName = $(this).attr('rel');
        $(this).parent().children("p").append('<input type="text" name="settings-name" value="" rel="' + plugName + '" />&nbsp;boolean?&nbsp;<input name="settings-type" checked="checked" type="checkbox" rel="' + plugName + '" /><br />');
        $('input:checkbox').checkbox();
    });
        $(this).parent().children("div.plugedit").fadeIn('slow', function () {
	  $('.save-plugin').unbind('click');
	    $('.save-plugin').on('click', function () {
	      
                setPlugSave($(this));

	    });
	});
    });

    $('#catcont').remove();
    $('#pluginadmin').append('<div id="catcont"><br /><br /><h4>Plugin Categories</h4><div id="plugincats"><p>separate each category with *return*</p></div></div>');
    var theHtml = '<textarea rows="8" cols="60" id="plugincategories">';
    $.each(junjible.categoryList, function (i) {
        if (this != "") {
            theHtml += this + '\n';
        }
    });
    theHtml += '</textarea><br /><input type="button" id="save-plugcats" value="save categories" />';
    $('#plugincats').append(theHtml);
    $('#save-plugcats').on('click', function () {
        var catList = $('#plugincategories').val().split(/[\n]+/);
        catList = $.grep(catList, function (n) {
            return (n);
        });
        saveCategories(catList);
    });
}
function setPlugSave(ele) {
  	      var plugin = ele.attr('name');
  	      var targetPlug = ele.attr('rel');
	      // get the settings as an array of objects
	      var mySettings = [];
	      var settingNames = [];
	      $("input[name='settings-name'][rel='"+targetPlug+"']").each(function() { settingNames.push($(this).val()) });
	      var settingTypes = [];
	      $("input[name='settings-type'][rel='"+targetPlug+"']").each(function() { if($(this).is(':checked')){settingTypes.push(true)}else{settingTypes.push(false)} });
	      var i=0;
	      for (i=0;i<settingNames.length;i++)
	      {
		if(settingNames[i] != ""){
		var set = {};
		set[settingNames[i]] = settingTypes[i];
		mySettings.push(set)
	        }
	      }
	      var scrList = $("textarea[name='screenshot-paths'][rel='"+targetPlug+"']").first().val().split(/[\n]+/);
              scrList = $.grep(scrList, function (n) {
              return (n);
              });
	      
	      var saveThis = [];
	      saveThis[0] = {};
	      saveThis[0].plugin = plugin;
	      saveThis[0].name = targetPlug;
	      saveThis[0].featured = (function () {if ($("input[name='featured-toggle'][rel='"+targetPlug+"']").first().is(':checked')){return true;}else{return false;}})();
	      saveThis[0].desc = $("textarea[name='description-text'][rel='"+targetPlug+"']").first().val();
	      saveThis[0].thumburl = $("input[name='icon-path'][rel='"+targetPlug+"']").first().val();
	      saveThis[0].available = (function () {if ($("input[name='available-toggle'][rel='"+targetPlug+"']").first().is(':checked')){return true;}else{return false;}})();
	      saveThis[0].settings = mySettings;
	      saveThis[0].screenshoturl = scrList;
	      saveThis[0].category = $("select[name='cat-select'][rel='"+targetPlug+"'] option:selected").first().text();
	      savePlugins(saveThis);
}
function generateRows(selected, type) {
    var pages = $("#" + type + "-page_count").val();
    if (pages <= 5) {
        var pagers = "<div id='" + type + "-paginator'>";
        var selected = 1;
        for (i = 1; i <= pages; i++) {
            if (i == selected) {
                pagers += "<a href='#' class='pagor selected'>" + i + "</a>";
            } else {
                pagers += "<a href='#' class='pagor'>" + i + "</a>";
            }
        }
        pagers += "<div style='clear:both;'></div></div>";
        $("#" + type + "-content").after(pagers);
        $(".pagor").click(function () {
            var index = $("#" + type + "-paginator .pagor").index(this);
            var p = {
                "page": index
            };
            $.post('/server/get-' + type, p, function (data) {
                var obj = data
                if (type == "users") {
                    displayUsers(obj);
                }
                if (type == "plugins") {
                    displayPlugins(obj);
                }
            });
            $("#" + type + "-paginator .pagor").removeClass("selected");
            $(this).addClass("selected");
        });
    } else {
        if (selected < 5) {
            // Draw the first 5 then have ... link to last
            var pagers = "<div id='" + type + "-paginator'>";
            for (i = 1; i <= 5; i++) {
                if (i == selected) {
                    pagers += "<a href='#' class='pagor selected'>" + i + "</a>";
                } else {
                    pagers += "<a href='#' class='pagor'>" + i + "</a>";
                }
            }
            pagers += "<div style='float:left;padding-left:6px;padding-right:6px;'>...</div><a href='#' class='pagor'>" + Number(pages) + "</a><div style='clear:both;'></div></div>";
            $("#" + type + "-paginator").remove();
            $("#" + type + "-content").after(pagers);
            $("#" + type + "-paginator .pagor").click(function () {
                updatePage(this, type);
            });
        } else if (selected > (Number(pages) - 4)) {
            // Draw ... link to first then have the last 5
            var pagers = "<div id='" + type + "-paginator'><a href='#' class='pagor'>1</a><div style='float:left;padding-left:6px;padding-right:6px;'>...</div>";
            for (i = (Number(pages) - 4); i <= Number(pages); i++) {
                if (i == selected) {
                    pagers += "<a href='#' class='pagor selected'>" + i + "</a>";
                } else {
                    pagers += "<a href='#' class='pagor'>" + i + "</a>";
                }
            }
            pagers += "<div style='clear:both;'></div></div>";
            $("#" + type + "-paginator").remove();
            $("#" + type + "-content").after(pagers);
            $("#" + type + "-paginator .pagor").click(function () {
                updatePage(this, type);
            });
        } else {
            // Draw the number 1 element, then draw ... 2 before and two after and ... link to last
            var pagers = "<div id='paginator'><a href='#' class='pagor'>1</a><div style='float:left;padding-left:6px;padding-right:6px;'>...</div>";
            for (i = (Number(selected) - 2); i <= (Number(selected) + 2); i++) {
                if (i == selected) {
                    pagers += "<a href='#' class='pagor selected'>" + i + "</a>";
                } else {
                    pagers += "<a href='#' class='pagor'>" + i + "</a>";
                }
            }
            pagers += "<div style='float:left;padding-left:6px;padding-right:6px;'>...</div><a href='#' class='pagor'>" + pages + "</a><div style='clear:both;'></div></div>";
            $("#" + type + "-paginator").remove();
            $("#" + type + "-content").after(pagers);
            $("#" + type + "-paginator .pagor").click(function () {
                updatePage(this, type);
            });
        }
    }
}

function updatePage(elem, type) {
    // Retrieve the number stored and position elements based on that number
    var selected = $(elem).text();
    // First update content
    var p = {
        "page": selected - 1
    };
    $.post('/server/get-' + type, p, function (data) {
        var obj = data;
        if (type == "users") {
            displayUsers(obj);
        }
        if (type == "plugins") {
            displayPlugins(obj);
        }
    });
    // Then update links
    generateRows(selected, type);
}

function saveCategories(catList) {
    var catListObj = {};
    catListObj.categories = catList;
    $.post('/server/save-categories', catListObj, junjible.notify.response);
}
function savePlugins(plugDelta) {
    var plugListObj = {};
    plugListObj.plugins = plugDelta;
    $.post('/server/save-plugins', plugListObj, junjible.notify.response);
}
function getDashboard() {
$.get('/server/get-dashboard', function (theData) {
var html = "<table>";
var blorg = theData.dashboard;
$.each(blorg, function (index) {

$.each( this, function(k, v){

html += "<tr><td>"+k+" : </td><td>"+v+"</td></tr>"
 });

});
html += "</table>";
	$('#dash').html(html);
});
}