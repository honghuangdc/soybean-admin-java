package com.soybean.framework.boot.base;


import com.baomidou.dynamic.datasource.exception.CannotFindDataSourceException;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.soybean.framework.commons.entity.Result;
import com.soybean.framework.commons.entity.enums.CommonError;
import com.soybean.framework.commons.exception.CheckedException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.validation.UnexpectedTypeException;
import javax.validation.ValidationException;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;


/**
 * @author wenxina
 * @since 2019-01-21
 */
@Slf4j
@Configuration
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {


    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ResponseEntity<Result<ResponseEntity<Void>>> jsonErrorHandler(HttpServletRequest request, Exception e) {
        HttpStatus defaultErrorResult = HttpStatus.OK;
        log.error("???????????? - {} - {}", request.getRequestURI(), e.getLocalizedMessage());
        if (e instanceof CheckedException) {
            CheckedException exception = (CheckedException) e;
            return new ResponseEntity<>(Result.fail(exception.getCode(), exception.getMessage()), defaultErrorResult);
        } else if (e instanceof IllegalArgumentException) {
            IllegalArgumentException exception = (IllegalArgumentException) e;
            return new ResponseEntity<>(Result.fail(exception.getMessage()), defaultErrorResult);
        } else if (e instanceof MultipartException) {
            return new ResponseEntity<>(Result.fail("????????????,?????????????????????"), defaultErrorResult);
        } else if (e instanceof InternalAuthenticationServiceException) {
            InternalAuthenticationServiceException exception = (InternalAuthenticationServiceException) e;
            if (exception.getCause() instanceof SQLSyntaxErrorException) {
                return new ResponseEntity<>(Result.fail(exception.getCause().getMessage()), defaultErrorResult);
            }
            if (e.getCause() instanceof MyBatisSystemException) {
                if (e.getCause().getCause() instanceof PersistenceException) {
                    if (e.getCause().getCause().getCause().getCause() instanceof SQLSyntaxErrorException) {
                        SQLSyntaxErrorException sqlSyntaxErrorException = (SQLSyntaxErrorException) e.getCause().getCause().getCause().getCause();
                        return new ResponseEntity<>(Result.fail("??????????????????" + sqlSyntaxErrorException.getMessage()), defaultErrorResult);
                    }
                }
                if (e.getCause().getCause().getCause() instanceof CannotFindDataSourceException) {
                    CannotFindDataSourceException sourceException = (CannotFindDataSourceException) e.getCause().getCause().getCause();
                    return new ResponseEntity<>(Result.fail("??????????????????" + sourceException.getMessage()), defaultErrorResult);
                }
                return new ResponseEntity<>(Result.fail("SQL ??????,??????????????? " + e.getCause().getMessage()), defaultErrorResult);
            }
            return new ResponseEntity<>(Result.fail(exception.getMessage()), defaultErrorResult);
        } else if (e instanceof RuntimeException) {
            log.error("????????????", e);
            RuntimeException exception = (RuntimeException) e;
            return new ResponseEntity<>(Result.fail(exception.getMessage()), defaultErrorResult);
        }
        return new ResponseEntity<>(Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()), defaultErrorResult);
    }

    @ExceptionHandler(UnexpectedTypeException.class)
    @ResponseBody
    public final Result<ResponseEntity<Void>> unexpectedTypeException(UnexpectedTypeException e) {
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public final Result<ResponseEntity<Void>> accessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        log.warn("\n[================================================================]\n" +
                "[????????????] - [{}]\n" +
                "[????????????] - [{}] - [{}]\n" +
                "[????????????] - [{}]\n" +
                "[================================================================]", e.getLocalizedMessage(), method, uri, CommonError.ACCESS_DENIED.desc());
        return Result.fail(CommonError.ACCESS_DENIED);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    public final Result<ResponseEntity<Void>> dataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("\n[================================================================]\n" +
                "[????????????] - [{}]\n" +
                "[================================================================]", e.getLocalizedMessage());
        if (e.getCause() instanceof SQLException) {
            return Result.fail(e.getCause().getMessage());
        }
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    @ResponseBody
    public final Result<ResponseEntity<Void>> insufficientAuthenticationException(InsufficientAuthenticationException e) {
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseBody
    public final Result<ResponseEntity<Void>> duplicateKeyException(DuplicateKeyException e) {
        log.error("[????????????]", e);
        return Result.fail("??????????????????");
    }

    // InternalAuthenticationServiceException

    @ExceptionHandler(MyBatisSystemException.class)
    @ResponseBody
    public final Result<ResponseEntity<Void>> myBatisSystemException(MyBatisSystemException e) {
        log.error("[Mybatis ????????????]", e);
        if (e.getCause() instanceof PersistenceException) {
            if (e.getCause().getCause() instanceof MybatisPlusException) {
                if (e.getCause().getCause().getCause() instanceof SQLSyntaxErrorException) {
                    SQLSyntaxErrorException sqlSyntaxErrorException = (SQLSyntaxErrorException) e.getCause().getCause().getCause();
                    return Result.fail("SQL ??????,??????????????? " + sqlSyntaxErrorException.getMessage());
                }
                return Result.fail(e.getLocalizedMessage());
            }
            return Result.fail(e.getLocalizedMessage());
        }
        return Result.fail(e.getLocalizedMessage());
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    @ResponseBody
    public final Result<ResponseEntity<Void>> badSqlGrammarException(BadSqlGrammarException e) {
        log.error("[Mybatis SQL ??????]", e);
        if (e.getCause() instanceof SQLSyntaxErrorException) {
            SQLSyntaxErrorException sqlSyntaxErrorException = (SQLSyntaxErrorException) e.getCause();
            return Result.fail("Mybatis SQL????????????,??????????????? " + sqlSyntaxErrorException.getMessage());
        }
        return Result.fail(e.getLocalizedMessage());
    }

    @ExceptionHandler(MybatisPlusException.class)
    @ResponseBody
    public final Result<ResponseEntity<Void>> mybatisPlusException(MybatisPlusException e) {
        log.error("mybatis-plus ex {}", e.getCause().getClass());
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(RetryableException.class)
    @ResponseBody
    public final Result<ResponseEntity<Void>> retryableException(RetryableException e) {
        log.error("????????????", e);
        return Result.fail(CommonError.INNER_SERVICE_ERROR);
    }


    @ExceptionHandler(ValidationException.class)
    @ResponseBody
    public final Result<ResponseEntity<Void>> handlerValidationException(final Exception e) {
        ValidationException exception = (ValidationException) e;
        if (exception.getCause() instanceof CheckedException) {
            return Result.fail(CommonError.REQUEST_PARAM_ERROR.type(), exception.getCause().getMessage());
        }
        return Result.fail(CommonError.REQUEST_PARAM_ERROR.type(), exception.getMessage());
    }


    /**
     * ???????????????????????????????????????
     */
    @Override
    @ResponseBody
    @Nonnull
    protected ResponseEntity<Object> handleExceptionInternal(@Nonnull Exception ex, Object body, @Nonnull HttpHeaders headers,
                                                             @Nonnull HttpStatus status, @Nonnull WebRequest request) {
        String uri = ((ServletWebRequest) request).getRequest().getRequestURI();
        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException exception = (MethodArgumentNotValidException) ex;
            String message = exception.getBindingResult().getAllErrors().get(0).getDefaultMessage();
            log.warn("[??????????????????] - [{}] - [{}]", uri, message);
            return new ResponseEntity<>(Result.fail(CommonError.REQUEST_PARAM_ERROR.type(), message), HttpStatus.OK);
        } else if (ex instanceof HttpRequestMethodNotSupportedException) {
            HttpRequestMethodNotSupportedException e = (HttpRequestMethodNotSupportedException) ex;
            final String method = e.getMethod();
            return new ResponseEntity<>(Result.fail("%s ???????????? %s ?????????", uri, method), HttpStatus.OK);
        } else if (ex instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException exception = (MethodArgumentTypeMismatchException) ex;
            logger.error("??????????????????????????????" + exception.getParameter().getMethod().getName() + "????????????" + exception.getName()
                    + ",?????????" + exception.getLocalizedMessage());
            if (ex.getCause() instanceof ConversionFailedException &&
                    ex.getCause().getCause() instanceof IllegalArgumentException) {
                return new ResponseEntity<>(Result.fail(ex.getCause().getCause().getLocalizedMessage()), HttpStatus.OK);
            }
            return new ResponseEntity<>(Result.fail("??????????????????"), HttpStatus.OK);
        } else if (ex instanceof HttpMessageNotReadableException) {
            HttpMessageNotReadableException e = (HttpMessageNotReadableException) ex;
            logger.error("??????????????????" + ex.getLocalizedMessage());
            if (e.getCause() instanceof InvalidFormatException) {
                InvalidFormatException invalid = (InvalidFormatException) e.getCause();
                return new ResponseEntity<>(Result.fail("???????????????????????? " + invalid.getMessage()), HttpStatus.OK);
            }
        } else if (ex instanceof NoHandlerFoundException) {
            NoHandlerFoundException e = (NoHandlerFoundException) ex;
            logger.error("????????????" + e.getLocalizedMessage());
            return new ResponseEntity<>(Result.fail("???????????? " + e.getMessage()), HttpStatus.OK);
        }
        final String contextPath = request.getContextPath();
        logger.error("?????????????????? - [" + contextPath + "]", ex);
        return new ResponseEntity<>(Result.fail("??????????????????"), HttpStatus.OK);
    }
}