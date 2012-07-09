junjible.payments = {};
function getPaymentInfo(){
$.get('/server/get-userpayments', function (data) {
  junjible.payments = data;
  junjible.payments = jQuery.parseJSON(data);
$('#payment-status').html(junjible.payments.status);
$('input[name="plan"]').each(function(index) {

if ($(this).val() == junjible.payments.plan)
{
  $('input[name="plan"]').attr("checked","");
  $(this).attr("checked","checked");
}
});
if (junjible.payments.ccnum.length > 1){
$('input[name="ccnum"]').val("********"+junjible.payments.ccnum);
}else{
$('input[name="ccnum"]').val("");  
}
$('input[name="expmon"]').val(junjible.payments.expmon);  
$('input[name="expyear"]').val(junjible.payments.expyear);   
$('input[name="ccname"]').val(junjible.payments.ccname);
$('input[name="ccaddy1"]').val(junjible.payments.ccaddy1);
$('input[name="ccaddy2"]').val(junjible.payments.ccaddy2); 
$('input[name="city"]').val(junjible.payments.city);  
$('input[name="state"]').val(junjible.payments.state);
$('input[name="country"]').val(junjible.payments.country);
$('input[name="zip"]').val(junjible.payments.zip); 
  
});
}
getPaymentInfo();
$('#payment-submit').on('click', function () {
  $("#payment-msg").html("").fadeOut('fast');
 junjible.payments.plan = $('input[name="myplan"]').val(); 
 junjible.payments.ccnum = $('input[name="ccnum"]').val();
 junjible.payments.expmon = $('input[name="expmon"]').val();  
 junjible.payments.expyear = $('input[name="expyear"]').val();  
 junjible.payments.cvc = $('input[name="cvc"]').val();  
 junjible.payments.ccname = $('input[name="ccname"]').val();
 junjible.payments.ccaddy1 = $('input[name="ccaddy1"]').val();
 junjible.payments.ccaddy2 = $('input[name="ccaddy2"]').val();
  junjible.payments.city = $('input[name="city"]').val();  
 junjible.payments.state = $('select[name="state"]').val();
 junjible.payments.country = $('select[name="country"]').val();
 junjible.payments.zip = $('input[name="zip"]').val();
 submitPayment();
});

function submitPayment() {
    $.post('/server/submit-payment', junjible.payments, function(response){
      $("#payment-msg").html(response.msg).fadeIn('slow');
      getPaymentInfo();
    });
}