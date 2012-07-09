/*
 * aycs.chat v1.0.1
 * Release Date: April 14, 2010
 * 
 * Copyright (c) 2010 Juergen Schwind (http://juergen-schwind.de)
 *  
 * Dual licensed under the MIT (http://www.opensource.org/licenses/mit-license.php)
 * and GPL (http://www.opensource.org/licenses/gpl-license.php) licenses.
 */

var chat_username = '';
var chat_message = '';
var chat_token = '';
var chat_lastmsgid = 0;
var chat_timer = 0;
var chat_loadinterval = 2000;
var chat_loaduserinterval = 5000;

function chat_login() {
	//chat_username = $('#login_username').val();
	  chat_username = window.username;
	  //alert('logging into chat: '+chat_username+' '+chat_token);
	$.ajax({
		async: false,
		type: "POST",
		url: '/jquery.aycs.chat.php',
		data: 'action=checkusername&chat_username='+chat_username,
		dataType: 'json',
		timeout: 5000,
		success: function(result){
			if (result.status==true) {
				chat_token=result.token;
				//alert(chat_token);
				//$('#chat_login').css('display', 'none');
				//$('#chat_conversation').css('display', 'block');
				chat_timer = 1;
				chat_update();
				chat_userupdate();
			} else {
				//alert('chat-login: '+result.message);
			}
		},
		error: function(){
			//console.log(error);
		}, 
	});
}

function chat_send() {
	chat_message = $('#message_input').val();
	$.ajax({
		async: false,
		type: "POST",
		url: '/jquery.aycs.chat.php',
		data: 'action=sendmessage&token='+chat_token+'&chat_message='+$.URLEncode(chat_message)+'&chat_username='+window.username,
		dataType: 'json',
		timeout: 5000,
		success: function(result){
			if (result.status==true) {
				$('#message_input').attr("value","");
			} else {
				//alert('chat-message: '+result.message);
			}
		},
		error: function(){
			//console.log('chat send - script-error');
		}, 
	});
}

function chat_update() {
//console.log('chat update token: '+chat_token+' '+chat_lastmsgid);
	$.ajax({
		type: "POST",
		url: '/jquery.aycs.chat.php',
		data: 'action=getmessages&token='+chat_token+'&chat_lastmsgid='+chat_lastmsgid+'&chat_username='+window.username,
		dataType: 'json',
		timeout: 5000,
		success: function(result){
			if (result.status==true) {
			//console.log('chat update successful');
				if (result.messages.length>0) {
					for (var i = 0; i < result.messages.length; i++) {
						chat_lastmsgid=result.messages[i].messageid;
						$("#conversation_messages").append(result.messages[i].content+'<br/>');
					}
					//if ($("#message_autoscroll:checked").length==1) {
						// need a fix?
						$("#conversation_messages").scrollTop($("#conversation_messages")[0].scrollHeight);
						var autoScroll = $('.chat-pane').data('jScrollPanePosition') == $('.chat-pane').data('jScrollPaneMaxScroll');
   
			        $('.chat-pane').data('jsp').reinitialise();
							$('.chat-pane').data('jsp').scrollToBottom();
							
					//}
				}
			} else {
				//alert('chat-update: '+result.message);
			}
		},
		error: function(){
			//alert('chat update - script-error');
		}, 
	});	
	
	if (chat_timer==1) {
		window.setTimeout("chat_update()", chat_loadinterval);		
	}
};

function chat_userupdate() {
	$.ajax({
		type: "POST",
		url: '/jquery.aycs.chat.php',
		data: 'action=getusers&token='+chat_token,
		dataType: 'json',
		timeout: 5000,
		success: function(result){
		//console.log('chat userupdate');
			if (result.status==true) {
				if (result.users.length>0) {
					$("#conversation_users").html('');
					for (var i = 0; i < result.users.length; i++) {
						$("#conversation_users").append(result.users[i]+'&nbsp;&nbsp;');
					}
				}
			} else {
				//alert('chat-userupdate: '+result.message);
			}
		},
		error: function(){
			//alert('chat userupdate - script-error');
		}, 
	});	
	
	if (chat_timer==1) {
		window.setTimeout("chat_userupdate()", chat_loaduserinterval);		
	}
};
