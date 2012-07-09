function createTicket() {
    var name = document.getElementById('name').value;
    var email = document.getElementById('email').value;
    var comments = document.getElementById('comments').value;
    var send = { name: name, email: email, comments: comments };

    if(name === '' || email === '' || comments === '') {
	alert('The form is incomplete! Please try again.');
    }else{
      $("#ticket-number").html("Submitting your request...<br />");
	$.post('/support/submit', send, function supportHandler (data) {
	  $("#ticket-number").html(data+"<br />");
	});
	//document.location = '/panel';
   }
}