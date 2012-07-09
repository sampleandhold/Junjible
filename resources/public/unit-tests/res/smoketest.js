// smoketest.js
// module("smoketest");

JQtest("smoketest_1", null, 2, function() {
    // stop();
    ok( true, "this test is fine" );
    var value = "hello";
    equal( value, "hello", "We expect value to be hello" );
    JQdone("smoketest_1");
    start();
});

// JQtest("smoketest_2", "smoketest_1", 1, function() {
JQtest("smoketest_2", null, 1, function() {
    // stop();
    ok( true, "all pass" );
    JQdone("smoketest_2");
    start();
});

// JQtest("smoketest_3", "smoketest_2", 2, function() {
// JQtest("smoketest_3", null, 2, function() {
JQtest("smoketest_3", "ready", 2, function() {
    // stop();
    notEqual( 2, 3, "2 != 3" );
    ok( true, "all pass" );
    JQdone("smoketest_3");
    start();
});
