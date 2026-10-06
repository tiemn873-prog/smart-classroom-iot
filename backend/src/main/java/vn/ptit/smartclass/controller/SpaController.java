package vn.ptit.smartclass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Frontend React dung React Router: cac duong dan nhu /dashboard chi ton tai
 * trong trinh duyet, may chu khong co file tuong ung. Neu khong chuyen huong,
 * mo thang localhost:8080/dashboard se ra loi 404.
 *
 * Lop nay tra ve index.html cho cac duong dan do, de React tu dieu huong tiep.
 * Chi liet ke dung 4 trang thay vi bat tat ca, tranh nuot nham /api.
 */
@Controller
public class SpaController {

    @GetMapping({ "/", "/dashboard", "/data-sensor", "/action-history", "/profile" })
    public String forwardToReact() {
        return "forward:/index.html";
    }
}
