package com.afitnerd.tnra.utils;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.service.PostRenderer;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FixtureUtils {

    public static String loadFixture(String fixturePath, String filename, Post post) {

        StringBuffer sb = new StringBuffer();
        try {
            ClassPathResource resource = new ClassPathResource("/fixtures/" + fixturePath + "/" + filename + ".txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }

        String ret = sb.toString();

        if (post.getStart() != null) {
            ret = ret.replace("{{start_date}}", PostRenderer.formatDate(post.getStart()));
        }

        if (post.getFinish() != null) {
            ret = ret.replace("{{finish_date}}", PostRenderer.formatDate(post.getFinish()));
        }

        return ret;
    }
}
