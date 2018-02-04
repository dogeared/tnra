package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import org.springframework.stereotype.Service;

@Service("emailPostRenderer")
public class EMailPostRenderer implements PostRenderer {

    @Override
    public String render(Post post) {

        StringBuilder sb = new StringBuilder();

        User user = post.getUser();
        sb.append("<strong>Post From:</strong> ")
            .append(user.getFirstName()).append(" - ").append(user.getEmail()).append("<br/>\n");
        sb.append("<strong>Post Started:</strong> ").append(PostRenderer.formatDate(post.getStart()));
        if (post.getState() == PostState.COMPLETE) {
            sb.append(", <strong>Post Finished:</strong> ").append(PostRenderer.formatDate(post.getFinish()));
        }
        sb.append("<p/><br/><p/>\n\n");

        Intro intro = post.getIntro();
        sb.append("<h2>Intro:</h2>\n\n");

        sb.append("<h3>WIDWYTK:</h3>\n");
        sb.append(doAppend(intro.getWidwytk())).append("\n\n");

        sb.append("<h3>Kryptonite:</h3>\n");
        sb.append(doAppend(intro.getKryptonite())).append("\n\n");

        sb.append("<h3>What and When:</h3>\n");
        sb.append(doAppend(intro.getWhatAndWhen())).append("\n\n");

        Category personal = post.getPersonal();
        sb.append("<h2>Personal:</h2>\n\n");

        sb.append("<h3>Best:<h3>\n").append(doAppend(personal.getBest())).append("\n\n");
        sb.append("<h3>Worst:</h3>\n").append(doAppend(personal.getWorst())).append("\n\n");

        Category family = post.getFamily();
        sb.append("<h2>Family:</h2>\n\n");

        sb.append("<h3>Best:</h3>\n").append(doAppend(family.getBest())).append("\n\n");
        sb.append("<h3>Worst:</h3>\n").append(doAppend(family.getWorst())).append("\n\n");

        Category work = post.getWork();
        sb.append("<h2>Work:</h2>\n\n");

        sb.append("<h3>Best:</h3>\n").append(doAppend(work.getBest())).append("\n\n");
        sb.append("<h3>Worst:</h3>\n").append(doAppend(work.getWorst())).append("\n\n");

        Stats stats = post.getStats();
        sb.append("<h2>Stats:</h2>\n\n");

        sb.append("<p><strong>exercise:<strong> ")
            .append(((stats.getExercise() != null) ? stats.getExercise() : "not set"));
        sb.append(", <strong>gtg:<strong> ")
            .append(((stats.getGtg() != null) ? stats.getGtg() : "not set"));
        sb.append(", <strong>meditate:<strong> ")
            .append(((stats.getMeditate() != null) ? stats.getMeditate() : "not set"));
        sb.append(", <strong>meetings:<strong> ")
            .append(((stats.getMeetings() != null) ? stats.getMeetings() : "not set"));
        sb.append(", <strong>pray:<strong> ")
            .append(((stats.getPray() != null) ? stats.getPray() : "not set"));
        sb.append(", <strong>read:<strong> ")
            .append(((stats.getRead() != null) ? stats.getRead() : "not set"));
        sb.append(", <strong>sponsor:<strong> ")
            .append(((stats.getSponsor() != null) ? stats.getSponsor() : "not set")).append("</p>\n");

        return sb.toString();
    }

    private StringBuffer doAppend(String toAppend) {
        StringBuffer sb = new StringBuffer();
        return sb
            .append("<p>")
            .append((toAppend != null) ? toAppend : "not set")
            .append("</p>");
    }
}