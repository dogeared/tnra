package com.afitnerd.tnra.slack.service;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.model.command.Command;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.PostService;
import com.afitnerd.tnra.slack.model.SlackSlashCommandRequest;
import com.afitnerd.tnra.slack.model.SlackSlashCommandResponse;
import com.afitnerd.tnra.slack.model.attachment.Attachment;
import com.afitnerd.tnra.slack.model.attachment.BasicAttachment;
import com.afitnerd.tnra.util.CommandParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

@Service
public class SlackSlashCommandServiceImpl implements SlackSlashCommandService {

    @Value("#{ @environment['tnra.slack.token'] ?: '' }")
    private String slackVerificationToken;

    private PostService postService;
    private UserRepository userRepository;

    public SlackSlashCommandServiceImpl(PostService postService, UserRepository userRepository) {
        this.postService = postService;
        this.userRepository = userRepository;
    }

    @PostConstruct
    protected void setup() {
    }

    @Override
    public boolean isValidToken(SlackSlashCommandRequest request) {
        return slackVerificationToken.equals(request.getToken());
    }

    @Override
    public SlackSlashCommandResponse process(SlackSlashCommandRequest request) {
        SlackSlashCommandResponse response = new SlackSlashCommandResponse();
        Command command = CommandParser.parse(request.getText());

        String responseText = "`command:` /post " + request.getText() + "\n";

        // look up the user
        User user = userRepository.findBySlackUserId(request.getUserId());
        if (user == null) {
            user = new User(request.getUserId(), request.getUserName());
            userRepository.save(user);
        }

        Assert.notNull(user, "Cannot find or create user: " + user);

        Post post = null;
        switch (command.getAction()) {
            case START:
                post = postService.startPost(user);
                responseText += "```" + post + "```";
                break;
            case FINISH:
                post = postService.finishPost(user);
                responseText += "`post started:` " + post.getStart() + "\n";
                responseText += "`post finished:` " + post.getFinish() + "\n";
                responseText += "```" + post + "```";
                break;
            case UPDATE:
            case APPEND:
                post = handleUpdate(user, command);
                responseText += "```" + post + "```";
                break;
            case REPLACE:
                post = handleReplace(user, command);
                responseText += "```" + post + "```";
                break;
            case SHOW:
                // TODO - handle param
                post = postService.getInProgressPost(user);
                responseText += "`post started:` " + post.getStart() + "\n";
                responseText += "```" + post + "```";
                break;
            case HELP:
                responseText += "```" + CommandParser.help() + "```";
        }

        response.setText(responseText);
        return response;
    }

    private Post handleReplace(User user , Command command) {
        switch (command.getSection()) {
            case INTRO:
                Intro intro = makeIntro(command);
                return postService.replaceIntro(user, intro);
            case PERSONAL:
                Category personal = makeCategory(command);
                return postService.replacePersonal(user, personal);
            case FAMILY:
                Category family = makeCategory(command);
                return postService.replaceFamily(user, family);
            case WORK:
                Category work = makeCategory(command);
                return postService.replaceWork(user, work);
            case STATS:
                Stats stats = makeStats(command);
                return postService.replaceStats(user, stats);
        }
        return null;
    }


    private Post handleUpdate(User user, Command command) {
        switch (command.getSection()) {
            case INTRO:
                Intro intro = makeIntro(command);
                return postService.updateIntro(user, intro);
            case PERSONAL:
                Category personal = makeCategory(command);
                return postService.updatePersonal(user, personal);
            case FAMILY:
                Category family = makeCategory(command);
                return postService.updateFamily(user, family);
            case WORK:
                Category work = makeCategory(command);
                return postService.updateWork(user, work);
            case STATS:
                Stats stats = makeStats(command);
                return postService.replaceStats(user, stats);
        }
        return null;
    }

    private Intro makeIntro(Command command) {
        Intro intro = new Intro();
        try {
            PropertyDescriptor pd = new PropertyDescriptor(command.getSubSection().getProperty(), Intro.class);
            pd.getWriteMethod().invoke(intro, command.getParam());
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return intro;
    }

    private Category makeCategory(Command command) {
        Category category = new Category();
        try {
            PropertyDescriptor pd = new PropertyDescriptor(command.getSubSection().getProperty(), Category.class);
            pd.getWriteMethod().invoke(category, command.getParam());
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return category;
    }

    private Stats makeStats(Command command) {
        Stats stats = new Stats();
        command.getStats().forEach((stat, val) -> {
            try {
                PropertyDescriptor pd = new PropertyDescriptor(stat.name().toLowerCase(), Stats.class);
                pd.getWriteMethod().invoke(stats, val);
            } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });
        return stats;
    }

    private SlackSlashCommandResponse help() {
        SlackSlashCommandResponse response = new SlackSlashCommandResponse();
        response.setText("Help:");
        BasicAttachment attachment = new BasicAttachment(null, null, "placeholder", Attachment.MarkdownIn.TEXT);
        response.addAttachment(attachment);
        return response;
    }
}
