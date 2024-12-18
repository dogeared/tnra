package com.afitnerd.tnra.slack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SlackSlashCommandConverterConfiguration implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        SlackSlashCommandConverter slackSlashCommandConverter = new SlackSlashCommandConverter();
        MediaType mediaType = new MediaType("application","x-www-form-urlencoded", Charset.forName("UTF-8"));
        slackSlashCommandConverter.setSupportedMediaTypes(Arrays.asList(mediaType));
        converters.add(slackSlashCommandConverter);
    }
}