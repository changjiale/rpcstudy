package com.gdut.rpcstudy.demo.protocol.http;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.spi.http.HttpHandler;
import java.io.IOException;

/**
 * @author: lele
 * @date: 2019/11/15 下午6:51
 * 相当于拦截器的功能，请求都要经过这个servlet处理
 */
public class DispatcherServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        new HttpServerHandler().handle(req,resp);
    }
}
