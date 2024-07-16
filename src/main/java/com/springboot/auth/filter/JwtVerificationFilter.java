package com.springboot.auth.filter;

import com.springboot.auth.jwt.JwtTokenizer;
import com.springboot.auth.utils.JwtAuthorityUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JwtVerificationFilter extends OncePerRequestFilter {
    private final JwtTokenizer jwtTokenizer;
    private final JwtAuthorityUtils jwtAuthorityUtils;

    public JwtVerificationFilter(JwtTokenizer jwtTokenizer, JwtAuthorityUtils jwtAuthorityUtils) {
        this.jwtTokenizer = jwtTokenizer;
        this.jwtAuthorityUtils = jwtAuthorityUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 검증된 claims 를 데리고 와서
        Map<String, Object> claims = verifyJws(request);
        // claims 를 넣고 Context 를 만들어줌.
        setAuthenticationToContext(claims);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter (HttpServletRequest request) throws ServletException {
        String authorization = request.getHeader("Authorization");
        return authorization == null || !authorization.startsWith("Bearer");
    }

    // 요청 헤더에서 Jws 를 데리고 옴.
    // getClaims 로 검증을 함.
    private Map<String, Object> verifyJws (HttpServletRequest request) {
        String jws = request.getHeader("Authorization").replace("Bearer ", "");
        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey());
        // 검증된 claims 를 받음. claims 에 있는 body 를 데리고 오기 때문에 getBody()
        Map<String, Object> claims = jwtTokenizer.getClaims(jws, base64EncodedSecretKey).getBody();
        return claims;
    }

    private void setAuthenticationToContext (Map<String, Object> claims) {
        String username = (String) claims.get("username");
        // claims 에 있는 Roles 정보를 데리고 와서, Authorities 를 만든다.
        List<GrantedAuthority> authorities = jwtAuthorityUtils.createAuthorities((List)claims.get("roles"));
        // 이걸 Authentication 객체로 만듦.
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username, null, authorities);
        //Security Context 에 만든 authentication 을 넣음.
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}