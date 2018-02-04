package com.afitnerd.tnra.slack.service;

import com.afitnerd.tnra.exception.PostException;
import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.model.command.Command;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.PostRenderer;
import com.afitnerd.tnra.service.PostService;
import com.afitnerd.tnra.slack.model.SlackSlashCommandRequest;
import com.afitnerd.tnra.slack.model.SlackSlashCommandResponse;
import com.afitnerd.tnra.util.CommandParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

@Service
public class SlackSlashCommandServiceImpl implements SlackSlashCommandService {

    @Value("#{ @environment['tnra.slack.token'] ?: '' }")
    private String slackVerificationToken;

    private PostService postService;
    private UserRepository userRepository;
    private PostRenderer slackPostRenderer;

    private static final Logger log = LoggerFactory.getLogger(SlackSlashCommandServiceImpl.class);

    public SlackSlashCommandServiceImpl(
        PostService postService, UserRepository userRepository,
        @Qualifier("slackPostRenderer") PostRenderer slackPostRenderer
    ) {
        this.postService = postService;
        this.userRepository = userRepository;
        this.slackPostRenderer = slackPostRenderer;
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

        // look up the user
        User user = findOrCreateUser(request);

        Assert.notNull(user, "Cannot find or create user: " + user);

        Post post = null;
        switch (command.getAction()) {
            case START:
                post = postService.startPost(user);
                response.setText(slackPostRenderer.render(post));
                break;
            case FINISH:
                post = postService.finishPost(user);
                response.setResponseType(SlackSlashCommandResponse.ResponseType.IN_CHANNEL);
                response.setText(slackPostRenderer.render(post));
                break;
            case UPDATE:
            case APPEND:
                post = handleUpdate(user, command);
                response.setText(slackPostRenderer.render(post));
                break;
            case REPLACE:
                post = handleReplace(user, command);
                response.setText(slackPostRenderer.render(post));
                break;
            case SHOW:
                if (command.getParam() == null) {
                    post = postService.getInProgressPost(user);
                } else {
                    String slackUsername = command.getParam();
                    post = postService.getLastFinishedPost(findOtherUser(slackUsername));
                }
                response.setText(slackPostRenderer.render(post));
                break;
            case HELP:
                response.setText("```" + CommandParser.help() + "```");
        }

        return response;
    }

    private User findOrCreateUser(SlackSlashCommandRequest request) {
        User user = userRepository.findBySlackUserId(request.getUserId());
        if (user == null) {
            user = new User(request.getUserId(), request.getUserName());
            userRepository.save(user);
        }
        return user;
    }

    private User findOtherUser(String slackUsername) {
        if (slackUsername.startsWith("@")) {
            slackUsername = slackUsername.substring(1);
        } else if (slackUsername.startsWith("<")) {
            int begIndex = slackUsername.indexOf("|") + 1;
            int endIndex = slackUsername.indexOf(">");
            slackUsername = slackUsername.substring(begIndex, endIndex);
        }
        User user = userRepository.findBySlackUsername(slackUsername);
        if (user == null) {
            throw new PostException("No user: " + slackUsername);
        }
        return user;
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
            log.error("Couldn't setup intro for: {}", command.getSubSection().getProperty(), e);
        }
        return intro;
    }

    private Category makeCategory(Command command) {
        Category category = new Category();
        try {
            PropertyDescriptor pd = new PropertyDescriptor(command.getSubSection().getProperty(), Category.class);
            pd.getWriteMethod().invoke(category, command.getParam());
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
            log.error("Couldn't setup category for: {}", command.getSubSection().getProperty(), e);
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
                log.error("Couldn't setup stats for: {}", stat.name().toLowerCase(), e);
            }
        });
        return stats;
    }
}
