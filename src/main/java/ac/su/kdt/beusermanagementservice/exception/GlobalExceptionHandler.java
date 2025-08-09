package ac.su.kdt.beusermanagementservice.exception;

import ac.su.kdt.beusermanagementservice.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 모든 @RestController에서 발생하는 예외를 전역적으로 처리하는 클래스임을 선언
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 특정 예외 클래스를 처리할 메서드임을 지정
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(IllegalArgumentException ex) {
        // 존재하지 않는 ID 등 으로 요청 시 404 Not Found 상태 코드와 에러 메시지를 반환
        ErrorResponseDTO errorResponse = new ErrorResponseDTO("NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalStateException(IllegalStateException ex) {
        // 팀 인원 초과시 409 Conflict 상태 코드와 에러 메시지를 반환
        ErrorResponseDTO errorResponse = new ErrorResponseDTO("CONFLICT", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
}