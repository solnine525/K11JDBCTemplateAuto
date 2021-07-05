package springboard.command;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import springboard.controller.BbsCommandImpl;
import springboard.model.JDBCTemplateDAO;

@Service
public class DeleteActionCommand implements BbsCommandImpl{
	
	@Autowired
	JDBCTemplateDAO dao;
	
	@Override
	public void execute(Model model) {
		
		//요청 한번에 받기
		Map<String, Object> map = model.asMap();
		HttpServletRequest req = (HttpServletRequest)map.get("req");
		
		//일련번호와 패스워드를 받아온 후
		String idx = req.getParameter("idx");
		String pass = req.getParameter("pass");
		
		//delete 메소드로 전달.
//		JDBCTemplateDAO dao = new JDBCTemplateDAO();
		
		dao.delete(idx, pass);
	}
}
