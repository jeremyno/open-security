<%@page import="java.io.PrintWriter"%>
<%@page import="com.github.opencam.process.SystemStatusWriter"%>
<%@page import="com.github.opencam.process.SecurityDeviceStatus"%>
<%@page import="com.github.opencam.process.OpenCamController"%>
<%@page import="com.github.opencam.imagegrabber.Resource" %>
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
SystemStatusWriter.writeStatus(opencam, new PrintWriter(out));
%>
<% 
for (final String i : opencam.getCameraNames()) {
  String loc = "\"images/" + i +".jpg\"";
  out.println("<div style=\"display:inline-block\">");
  out.println("<a href="+loc+"><img src="+loc+" width=\"" + width + "\"/></a><br />");
  Resource r = opencam.getLastImage(i);
  if (r != null && !r.getNotes().isEmpty()) {
	  out.println("<ul>");
	  for(String note : r.getNotes()) {
	    out.println("<li>"+note+"</li>");
	  }
	  out.println("</ul>");
  }
  out.println("</div>");
}
%>
