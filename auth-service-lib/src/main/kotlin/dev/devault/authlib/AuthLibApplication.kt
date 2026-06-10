package dev.devault.authlib

import dev.devault.authlib.config.JwksProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(JwksProperties::class)
class AuthLibApplication
