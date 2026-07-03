package com.enterprise.fabric.security

import com.enterprise.fabric.AppProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig(private val props: AppProperties) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.cors { }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/health", "/ready", "/version", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                if (props.devAuthEnabled) auth.anyRequest().permitAll() else auth.anyRequest().authenticated()
            }
        if (!props.devAuthEnabled) http.oauth2ResourceServer { it.jwt {} }
        return http.build()
    }
}
