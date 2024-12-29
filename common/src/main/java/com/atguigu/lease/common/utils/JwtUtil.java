package com.atguigu.lease.common.utils;

import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {
    private static long tokenExpiration = 60 * 60 * 1000L; // 过期时间
    private static SecretKey tokenSignKey = Keys.hmacShaKeyFor("M0PKKI6pYGVWWfDZw90a0lTpGYX1d4AQ".getBytes());// 加密算法
    public static String getToken(Long userId, String username){
        String token = Jwts.builder()
                // header 头部
                .setSubject("USER_INFO")
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpiration))
                // payload 负载
                .claim("userId", userId)
                .claim("username", username)
                //签名组装
                .signWith(tokenSignKey)
                .compact();
        return token;
    }
    public static Claims parseToken(String token){
        if (token==null){
            throw new LeaseException(ResultCodeEnum.ADMIN_LOGIN_AUTH);
        }

        try{
            JwtParser jwtParser = Jwts.parserBuilder()
                    .setSigningKey(tokenSignKey)
                    .build();
            return jwtParser.parseClaimsJws(token).getBody();
        }catch (ExpiredJwtException e){
            throw new LeaseException(ResultCodeEnum.TOKEN_EXPIRED);
        }catch (JwtException e){
            throw new LeaseException(ResultCodeEnum.TOKEN_INVALID);
        }
    }

    public static void main(String[] args) {
        System.out.println(getToken(2L, "user"));
    }
}
