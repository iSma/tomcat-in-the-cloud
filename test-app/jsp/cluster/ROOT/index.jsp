<%@page import="java.net.*"%>
<%@page import="java.util.List"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>TST Page</title>
    </head>
    <body>
    <h1> Tomcat Session Testing</h1> <br/>
    Session ID: <%=request.getSession().getId() %> <br/>
    Node adress: <% String node = InetAddress.getLocalHost().toString();
    out.println(node); %> <br/>
    Request counter: <% 
        if (request.getSession().getAttribute("Accesses") == null){
            out.println("<i>new session</i>");
            request.getSession().setAttribute("Accesses", 1);
        } else {
            int a = ((int) request.getSession().getAttribute("Accesses")) + 1;
            out.println(a);
            request.getSession().setAttribute("Accesses", a);
    }
    %> <br/><br/>
    <form action="index.jsp" method="post">
        <input type="submit" value="RESET" name="submit">
        <%
        if (request.getParameter("submit") != null) {
        session.invalidate();
        }
        %>
    </form>
    </body>
</html>
