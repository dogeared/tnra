package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.Stats;
import org.springframework.stereotype.Service;

@Service
public class SlackPostRenderer implements PostRenderer {

    @Override
    public String render(Post post) {

        StringBuilder sb = new StringBuilder();

        sb.append("`user:` ").append(post.getUser().getSlackUsername());
        sb.append(", `post started:` ").append(PostRenderer.formatDate(post.getStart()));

        if (post.getState() == PostState.COMPLETE) {
            sb.append(", `post finished:` ").append(PostRenderer.formatDate(post.getFinish()));
        }
        sb.append("\n");

        Intro intro = post.getIntro();
        sb.append("*Intro:*\n\n");

        sb.append("*WIDWYTK:*\n");
        sb.append("\t").append(doAppend(intro.getWidwytk()));

        sb.append("*Kryptonite:*\n");
        sb.append("\t").append(doAppend(intro.getKryptonite()));

        sb.append("*What and When:*\n");
        sb.append("\t").append(doAppend(intro.getWhatAndWhen()));

        Category personal = post.getPersonal();
        sb.append("*Personal:*\n\n");

        sb.append("\t*Best:* ").append(doAppend(personal.getBest()));
        sb.append("\t*Worst:* ").append(doAppend(personal.getWorst()));


        Category family = post.getFamily();
        sb.append("*Family:*\n\n");

        sb.append("\t*Best:* ").append(doAppend(family.getBest()));
        sb.append("\t*Worst:* ").append(doAppend(family.getWorst()));

        Category work = post.getWork();
        sb.append("*Work:*\n\n");

        sb.append("\t*Best:* ").append(doAppend(work.getBest()));
        sb.append("\t*Worst:* ").append(doAppend(work.getWorst()));

        Stats stats = post.getStats();
        sb.append("*Stats:*\n\n");

        sb.append("\t*exercise:* ")
            .append(((stats.getExercise() != null) ? stats.getExercise() : "not set"));
        sb.append(", *gtg:* ")
            .append(((stats.getGtg() != null) ? stats.getGtg() : "not set"));
        sb.append(", *meditate:* ")
            .append(((stats.getMeditate() != null) ? stats.getMeditate() : "not set"));
        sb.append(", *meetings:* ")
            .append(((stats.getMeetings() != null) ? stats.getMeetings() : "not set"));
        sb.append(", *pray:* ")
            .append(((stats.getPray() != null) ? stats.getPray() : "not set"));
        sb.append(", *read:* ")
            .append(((stats.getRead() != null) ? stats.getRead() : "not set"));
        sb.append(", *sponsor:* ")
            .append(((stats.getSponsor() != null) ? stats.getSponsor() : "not set")).append("\n\n");

        return sb.toString();
    }

    private StringBuffer doAppend(String toAppend) {
        StringBuffer sb = new StringBuffer();
        return sb
                .append((toAppend != null) ? toAppend.replace("\n", "\n\t") : "not set")
                .append("\n\n");
    }
}
