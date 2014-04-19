<%@page import="java.security.Principal"%>
<%@page import="com.github.opencam.process.PoolingOpenCamController"%>
<%
  PoolingOpenCamController opencam = (PoolingOpenCamController)request.getAttribute("opencam");
String act = request.getParameter("do"); 
if (act != null) {
  String name = request.getRemoteUser() + "@" + request.getRemoteAddr();
  
  Principal  principal = request.getUserPrincipal();
  if (principal != null && principal.getName() != null) {
    name = principal.getName();
  }
  
  if (act.equals("Disarm")) {
    opencam.setSystemArmed(false, name);
  } else if (act.equals("Arm")){
    opencam.setSystemArmed(true, name);
  }
}

String width = request.getParameter("w") != null ?request.getParameter("w") : "640";
String rate = request.getParameter("r") != null ? request.getParameter("r") : "5000";
%>
<html>
<head>
<title>Camera System</title>
<script type="text/javascript" src="javascript/jquery-2.0.3.min.js"></script>
<script type="text/javascript">
var func = function() {
	$("#images").load("imageFrame.jsp?w=<%= width %>", function() {
		$(".command","#images").click(function(e) {
			e.preventDefault();
			var href = this.href;
			$.ajax(href).success(function() {
				window.status="Success calling " + href;
			});
		});
		
	});
};
window.setInterval(func,<%= rate %>);
$(function() { 
	func();
});
</script>
</head>
<body>
Width: <a href="?w=640">640</a> <a href="?w=320">320</a> <a href="?w=160">160</a>

<div id="images">
  &nbsp;
</div>


</body>
</html>
