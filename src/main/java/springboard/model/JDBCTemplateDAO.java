package springboard.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestMapping;

@Repository
public class JDBCTemplateDAO {
	
	/*
	컨트롤러에서 @Autowired를 통해 자동 주입 받았던 빈을 정적변수인
	JdbcTemplateConst.template에 값을 할당하였으므로, DB연결정보를
	DAO에서 바로 사용할 수 있다.
	 */
	
	@Autowired
	JdbcTemplate template;
	
	/*
	Spring 프로젝트 전체에서 해당 변수를 사용하기 위해 static 타입으로 생성한다.
	static 타입의 변수는 프로그램 내에서 딱 하나만 생성되고,
	객체 생성 없이 클래스명으로 직접 접근할 수 있는 공유객체가 된다.
	 */
	
	public JDBCTemplateDAO() {
		System.out.println("JDBCTemplateDAO() 생성자 호출");
	}
	
	public void close() {
		/*
		JDBCTemplate에서는 자원해제를 하지 않는다.
		Spring 설정 파일에서 빈을 생성하므로 자언을 해제하면 다시
		new를 통해 생성해야 하므로 자원해체를 사용하지 않는다.
		 */
	}
	
	//게시물 수 카운트
	public int getTotalCount(Map<String, Object> map) {
		
		String sql = "SELECT COUNT(*) FROM springboard";
		
		if(map.get("Word")!=null) {
			sql +=" WHERE "+map.get("Column")+" "
				+"		LIKE '%"+map.get("Word")+"%' ";
		}
		//쿼리문에서 count(*)를 통해 반환되는 값을 정수형태로 가져온다.
		return template.queryForObject(sql, Integer.class);
	}
	//게시판 리스트 가져오기(페이지 처리 없음)
	public ArrayList<SpringBbsDTO> list(Map<String, Object> map){
		
		String sql= " "
				+ "SELECT * FROM springboard ";
			if(map.get("Word")!=null) {
				sql +=" WHERE "+map.get("Column")+" "
					+"		LIKE '%"+map.get("Word")+"%' ";
			}
			sql += " ORDER BY idx DESC";
		/*
		RowMapper가 select를 통해 얻어온 ResultSet을 DTO객체에
		저장하고, List컬렉션에 적재하여 반환한다. 그러므로 DAO에서
		개발자가 반복적으로 하던 작업을 자동으로 처리해 준다.	
		 */
		return (ArrayList<SpringBbsDTO>) template.query(sql, 
				new BeanPropertyRowMapper<SpringBbsDTO>(
						SpringBbsDTO.class));
	}
	
	
	public void write(final SpringBbsDTO springBbsDTO) {
		/*
		매개변수로 전달되는 값을 익명클래스 내에서 사용할 때는
		반드시 final로 선언하여 갓의 변경이 불가능하게 처리해야 한다.
		final로 선언하지 않으면 에러가 발생한다. (Java의 규칙)
		 */
		
		template.update(new PreparedStatementCreator() {
			@Override
			public PreparedStatement
				createPreparedStatement(Connection con) throws SQLException {
				
				/*
				답변형 게시판에서 원본글인 경우 idx아 bgroup은 반드시
				일치해야 한다.
				리스트에서 게시물 정렬시 bgroup을 통해 oreder by 절을
				구성하기 때문이다.
				또한, nextval은 한 문자에서 여러번 사용하더라도 항상 같은
				시퀀스를 반환한다.
				 */
				String sql = "INSERT INTO springboard ( "
					+ " idx, name, title, contents, hits "
					+ " ,bgroup, bstep, bindent, pass) "
					+ " VALUES ( "
					+ " springboard_seq.NEXTVAL,?,?,?,0, "
					+ " springboard_seq.NEXTVAL,0,0,?)";
				
				PreparedStatement psmt = con.prepareStatement(sql);
				psmt.setString(1, springBbsDTO.getName());
				psmt.setString(2, springBbsDTO.getTitle());
				psmt.setString(3, springBbsDTO.getContents());
				psmt.setString(4, springBbsDTO.getPass());
				//인파라미터를 설정한 후 객체를 반환한다.
				return psmt;
			}
		});
	}
	
	//조회수 증가
	public void updateHit(final String idx) {
		//쿼리문 작성
		String sql = "UPDATE springboard SET "
				+ " hits = hits+1 "
				+ " WHERE idx=? ";
		//update의 첫번째 인자로 쿼리문, 두번재 인자는 익명클래스를 통해
		//인파라미터 설정
		template.update(sql, new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				//인파라미터 설정
				ps.setInt(1, Integer.parseInt(idx));
			}
		});
	}
	
	//상세보기 위한 레코드 가져오기
	public SpringBbsDTO view (String idx) {
		//조회수 증가를 위한 메소드 호출
		updateHit(idx);
		
		//쿼리문 작성
		SpringBbsDTO dto = new SpringBbsDTO();
		String sql = "SELECT * FROM springboard "
				+ " WHERE idx=" + idx;
		try {
			/*
			하나의 결과를 반환하는 select 계열의 쿼리문을 실행할 때는
			queryForObject() 메소드를 사용한다. 단, 반환 결과가 0 이거나
			2 이상이면 예외를 발생시키므로 반드시 예외 처리를 하는 것이 좋다.
			 */
			dto = template.queryForObject(sql, new BeanPropertyRowMapper<SpringBbsDTO>(
					
					SpringBbsDTO.class));
		}
		catch(Exception e) {
			System.out.println("View() 실행시 예외발생");
		}
		
		return dto;
	}
	
	//패스워드 검증하기
	public int password(String idx, String pass) {
		int retNum = 0;
		String sql = "SELECT * FROM springboard "
				+ " WHERE pass=" + pass + " AND idx="+idx;
		
		try { 
			SpringBbsDTO dto = template.queryForObject(sql,
					new BeanPropertyRowMapper<SpringBbsDTO>(SpringBbsDTO.class));
			/*
			일련번호는 시퀀스를 사용하므로 반드시 1 이상의 값을
			가지게 된다. 따라서 0 이하의 값이 반환된다면 패스워드
			검증 실패라 판단할 수 있다.
			 */
			retNum = dto.getIdx();
		}
		catch(Exception e) {
			System.out.println("password() 예외발생");
		}
		
		return retNum;
	}
	
	public void edit(final SpringBbsDTO dto) {
		
		String sql = "UPDATE springboard "
				+ " SET name=?, title=?, contents=? "
				+ " WHERE idx=? AND pass=?";
		
		template.update(sql, new PreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				
				ps.setString(1, dto.getName());
				ps.setString(2, dto.getTitle());
				ps.setString(3, dto.getContents());
				ps.setInt(4, dto.getIdx());
				ps.setString(5, dto.getPass());
			}
		});
	}
	
	public void delete(final String idx, final String pass) {
		
		String sql = "DELETE FROM springboard "
				+ " WHERE idx=? AND pass=?";
		
		template.update(sql, new PreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setString(1,idx);
				ps.setString(2, pass);
			}
		});
	}
	
	public void reply(final SpringBbsDTO dto) {
		/*
		답변글의 경우라도 새로운 게시물이므로 일련번호에 새로운
		시퀀스를 입력해야 한다.
		글쓰기(write)와 다른 점은 원본글의 bgroup번호를 그대로 입력하는
		것이다.
		 */
		String sql = "INSERT INTO springboard "
				+ " (idx, name, title, contents, pass, "
				+ " bgroup, bstep, bindent) "
				+ " VALUES "
				+ " (springboard_seq.NEXTVAL, ?, ?, ?, ?,"
				+ " ?, ?, ?)";
		
		template.update(sql, new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				
				ps.setString(1, dto.getName());
				ps.setString(2, dto.getTitle());
				ps.setString(3, dto.getContents());
				ps.setString(4, dto.getPass());
				//원본글의 bgroup번호를 그대로 입력한다
				ps.setInt(5, dto.getBgroup());
				//원본글의 bstep, bindent를 +1 한 후 입력한다.
				ps.setInt(6, dto.getBstep()+1); //동일 그룹 내에서의 정렬순서
				ps.setInt(7, dto.getBindent()+1); //답변글의 깊이
			}
		});
				
	}
	
	public ArrayList<SpringBbsDTO> listPage(Map<String, Object> map){

		int start = Integer.parseInt(map.get("start").toString());
		int end = Integer.parseInt(map.get("end").toString());
		
		String sql = ""
				+"SELECT * FROM ("
				+"    SELECT Tb.*, rownum rNum FROM ("
				+"        SELECT * FROM springboard ";				
			if(map.get("Word")!=null){
				sql +=" WHERE "+map.get("Column")+" "
					+ " LIKE '%"+map.get("Word")+"%' ";				
			}			
			sql += " ORDER BY bgroup DESC, bstep ASC"
			+"    ) Tb"
			+")"
			+" WHERE rNum BETWEEN "+start+" and "+end;
		
		return (ArrayList<SpringBbsDTO>)
			template.query(sql, 
				new BeanPropertyRowMapper<SpringBbsDTO>(
				SpringBbsDTO.class));
	}
	
	/*
	답변글을 입력하기 전 현재 step보다 큰 게시물을 일괄적으로
	step+1해서 뒤로 밀어주는 작업을 진행한다.
	 */
	public void replyPrevUpdate(final int strGroup, final int strStep) {
		
		String sql = "UPDATE springboard "
				+ " SET bstep = bstep+1 "
				+ " WHERE bgroup=? AND bstep>?";
		template.update(sql, new PreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setInt(1, strGroup);
				ps.setInt(2, strStep);
			}
		});
	}
}
