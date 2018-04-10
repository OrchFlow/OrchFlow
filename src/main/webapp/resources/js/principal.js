/**
 * 
 */
function iframe() {
	document.getElementById('iframe').setAttribute("style",
			"height:" + $(document).height() + "px; border: 0px;");
}

$(document).ready(function() {
	$(".panel-heading").click(function() {
		$("#alerta").alert('close');
		document.getElementById("formP:msgsP").style.display = 'none';
		document.getElementById("formR:msgsR").style.display = 'none';
	});
	$("#menu").click(function() {
		$("#alerta").alert('close');
	});
});