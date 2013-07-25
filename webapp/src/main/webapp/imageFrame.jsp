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
<ul>
<%
long lastTimestamp =0;
long firstTimestamp = Long.MAX_VALUE;
for (final SecurityDeviceStatus i : opencam.getDeviceStatus()) {
  out.println("<li>" +i+"</li>");
  long timestamp = i.getChekinTimestamp();
  if (lastTimestamp < timestamp) {
    lastTimestamp = timestamp;
  }

  if (firstTimestamp > timestamp) {
    firstTimestamp = timestamp;
  }
}
%>
<li>Processing is running <%= (float)(System.currentTimeMillis()-lastTimestamp)/1000 %> to <%= (float)(System.currentTimeMillis()-firstTimestamp)/1000 %> seconds behind.</li>
<li>Current System time is <%= new java.util.Date() %></li>
</ul>

<% 
for (final String i : opencam.getCameraNames()) {
  String loc = "\"images/" + i +".jpg\"";
  out.println("<a href="+loc+"><img src="+loc+" width=\"" + width + "\"/></a>");
}
%>
