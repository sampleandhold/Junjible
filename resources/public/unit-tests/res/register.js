// register.js
// module("register");

// simply load home page
JQtest("register_1", null, 1, function() {
    $.get('/', function (html) {
	equal($('.contact-form > form', html).attr('action'),
	      "/signup",
	      "Home page loaded, found signup form" );
	JQdone("register_1");
	start();
    });
});

// register testuser
JQtest("register_2", null, 1, function() {
    var signup = {
	'user': 'testuser',
	'pass': 'password',
	'pass2': 'password',
	'email': 'testuser@email.com',
    };
    $.post('/signup', signup, function (html) {
	equal($('h3', html).text(),
	      "Registered!",
	      "testuser successfully registered" );
	JQdone("register_2");
	start();
    });
});
