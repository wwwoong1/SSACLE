package S12P11D110.ssacle.domain.auth.filter;


import S12P11D110.ssacle.global.exception.AuthErrorException;
import S12P11D110.ssacle.global.dto.ResultDto;
import S12P11D110.ssacle.global.exception.HttpStatusCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.FilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;



import java.io.IOException;

@Component
public class JwtExceptionFilter extends OncePerRequestFilter{
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        //JwtFilter 를 호출하는데, 이 필터에서 JwtErrorException 이 떨어진다.
//        filterChain.doFilter(request, response);
//    }
//
//    private static void errorResponse(HttpServletResponse response, AuthErrorException e) throws IOException {
//        // response 객체를 사용하여 HTTP 응답을 설정
//        response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 상태 코드 설정 (예: 200 OK)
//        response.setContentType("application/json"); // 응답 형식 설정 (JSON 등)
//        response.setCharacterEncoding("UTF-8"); // 문자 인코딩 설정
//
//        // 응답 본문 작성
//        ResultDto<Object> responseBody = ResultDto.of(e.getCode(), e.getErrorMsg(), null);
//
//        final ObjectMapper mapper = new ObjectMapper();
//        mapper.writeValue(response.getOutputStream(), responseBody);
//    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
    try {
        filterChain.doFilter(request, response);
    } catch (AuthErrorException e) {
        handleException(response, String.valueOf(e.getCode()), e.getErrorMsg());
    } catch (Exception e) {
        handleException(response, "UNKNOWN_ERROR", "서버 내부 오류가 발생했습니다.");
    }
}

    private void handleException(HttpServletResponse response, String errorCode, String errorMessage) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        if ("UNAUTHORIZED".equals(errorCode)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else if ("FORBIDDEN".equals(errorCode)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ResultDto<Object> responseBody = ResultDto.of(HttpStatusCode.valueOf(errorCode), errorMessage, null);
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), responseBody);
    }
}
