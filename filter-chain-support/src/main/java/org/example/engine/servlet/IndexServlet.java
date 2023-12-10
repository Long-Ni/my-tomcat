package org.example.engine.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = "/")
public class IndexServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 获取请求
        String sessionId = req.getRequestedSessionId();
        if (sessionId == null) {
            sessionId = "default";
        }

        // 响应体
        String html = "<h1>Index Page: You are " + sessionId + ".</h1>";

        // 设置响应
        resp.setContentType("text/html");
        PrintWriter pw = resp.getWriter();
        pw.write(html);
        pw.close();
    }
}
