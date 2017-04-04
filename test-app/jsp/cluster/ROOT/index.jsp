<%@page import="java.net.*"%>
<%@page contentType="application/json" pageEncoding="UTF-8"%>
<%
session.setAttribute("counter",
session.getAttribute("counter") == null
? 1
: 1 + (int) session.getAttribute("counter")
);
%>
{
  "counter": "<%=session.getAttribute("counter")%>",
  "id": "<%=session.getId()%>",
  "new": <%=session.isNew() ? "true" : "false"%>,
  "server": "<%=InetAddress.getLocalHost().getHostAddress()%>"
}
