//package com.elevens.loanservice.interceptor;
//
//import com.elevens.loanservice.util.CorrelationIdContext;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNull;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//public class RequestHeaderInterceptorTest {
//
//    @InjectMocks
//    private RequestHeaderInterceptor requestHeaderInterceptor;
//
//    @Mock
//    private HttpServletRequest request;
//
//    @Mock
//    private HttpServletResponse response;
//
//    @Test
//    public void testPreHandleWithCorrelationId() throws Exception {
//        String correlationId = "test-correlation-id";
//        when(request.getHeader(CorrelationIdContext.CORRELATION_ID)).thenReturn(correlationId);
//
//        requestHeaderInterceptor.preHandle(request, response, new Object());
//
//        assertEquals(correlationId, CorrelationIdContext.getCorrelationId());
//    }
//
//    @Test
//    public void testPreHandleWithoutCorrelationId() throws Exception {
//        CorrelationIdContext.clear();
//        when(request.getHeader(CorrelationIdContext.CORRELATION_ID)).thenReturn(null);
//
//        requestHeaderInterceptor.preHandle(request, response, new Object());
//
//        assertNull(CorrelationIdContext.getCorrelationId());
//    }
//
//    @Test
//    public void testAfterCompletion() throws Exception {
//        CorrelationIdContext.setCorrelationId("some-id");
//        requestHeaderInterceptor.afterCompletion(request, response, new Object(), null);
//        assertNull(CorrelationIdContext.getCorrelationId());
//    }
//}
