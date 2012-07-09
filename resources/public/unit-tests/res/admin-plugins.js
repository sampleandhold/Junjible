var pluginName = '';

JQtest('get-plugins-count', null, 1, function tester () {
    $.get('/server/get-plugins-count', function reqHandler (response) {
	equal(response > 0, true, 'Counted Plugins: ' + response);
	JQdone('get-plugins-count');
	start();
    });
});

JQtest('get-plugins', 'get-plugins-count', 1, function tester () {
    $.post('/server/get-plugins', {page: 0}, function reqHandler (response) {
	var plugin = response['plugins'][0];
	var p = plugin;
	var name = p['name'];
	pluginName = name;
	var desc = p['desc'];
	var thumb = p['thumburl'];
	equal(name !== '' & desc !== '' & thumb !== '', true, 'Found plugin properties');
	JQdone('get-plugins');
	start();
    });
});

JQtest('modify-plugin', 'get-plugins', 1, function tester () {
    var plugins = {
	'plugins': [
	    {'featured': true,
	     'desc': 'unit-tested',
	     'name': pluginName,
	     'thumburl': '/images/plugins/puppies.jpg',
	     'available': true,
	     'settings': [{'unit-tested':true}],
	     'screenshoturl': ['/images/plugins/Minecraft3-thumb-640x359-44598.jpg'],
	     'category': 'unit-tested'} 
	] // plugins
    };

    $.post('/server/save-plugins', plugins, function reqHandler (response) {
	var r = response;
	var pass = 'updated settings for plugin ' + pluginName;
	equal(r.success === true && r.msg === pass, true, 'Updated plugin: ' + r.msg);
	JQdone('modify-plugin');
	start();
    });
});