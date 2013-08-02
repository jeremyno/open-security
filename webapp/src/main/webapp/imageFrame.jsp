<%@page import="com.github.opencam.process.ImageStatusWriter"%>
<%@page import="com.github.opencam.process.SecurityDeviceStatus"%>
<%@page import="com.github.opencam.process.OpenCamController"%>
<%
  OpenCamController opencam = (OpenCamController)request.getAttribute("opencam");
String width = request.getParameter("w") != null ?request.getParameter("w") : "640";
%>

<h2>System status is <%= opencam.isSystemArmed() ? "Armed" : "Disarmed" %> (<%= opencam.getStatusString() %>)</h2>

<%if (opencam.isSystemArmed()) { %>
<a href="?do=Disarm" class="command">Disarm</a>
<% } else { %>
<a href="?do=Arm" class="command">Arm</a>
<% } %>

<%
ImageStatusWriter.writeStatus(opencam, out);
%>

<% 
for (final String i : opencam.getCameraNames()) {
  String loc = "\"images/" + i +".jpg\"";
  out.println("<a href="+loc+"><img src="+loc+" width=\"" + width + "\"/></a>");
}
%>
