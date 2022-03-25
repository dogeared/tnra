package com.afitnerd.tnra.controller;

import com.afitnerd.tnra.model.GoToGuySet;
import com.afitnerd.tnra.model.JsonViews;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.GoToGuySetRepository;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.EMailService;
import com.afitnerd.tnra.service.PostService;
import com.afitnerd.tnra.service.SlackPostRenderer;
import com.afitnerd.tnra.slack.service.SlackAPIService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class APIController {

    private final PostService postService;
    private final EMailService eMailService;
    private final SlackAPIService slackAPIService;
    private final SlackPostRenderer slackPostRenderer;

    private final UserRepository userRepository;
    private final GoToGuySetRepository gtgSetRepository;

    public APIController(
        PostService postService, EMailService eMailService,
        SlackAPIService slackAPIService, SlackPostRenderer slackPostRenderer,
        UserRepository userRepository, GoToGuySetRepository gtgSetRepository
    ) {
        this.postService = postService;
        this.eMailService = eMailService;
        this.slackAPIService = slackAPIService;
        this.slackPostRenderer = slackPostRenderer;
        this.userRepository = userRepository;
        this.gtgSetRepository = gtgSetRepository;
    }

    @GetMapping("/me")
    Principal me(Principal me) {
        return me;
    }

    @JsonView(JsonViews.Sparse.class)
    @GetMapping("/users")
    public  Iterable<User> users() {
        return userRepository.findByActiveTrue();
    }

    @JsonView(JsonViews.Sparse.class)
    @GetMapping("/gtg_latest")
    public GoToGuySet gtgLatest() {
        return gtgSetRepository.findTopByOrderByStartDateDesc();
    }

    @JsonView(JsonViews.Sparse.class)
    @GetMapping("/notify_what_and_whens")
    public GoToGuySet notifyWhatAndWhens() {
        GoToGuySet goToGuySet = gtgLatest();
        goToGuySet.getGoToGuyPairs().forEach(gtgPair -> {
            //if (gtgPair.getCallee().getFirstName().equalsIgnoreCase("micah")) {
            Post callerPost = postService.getLastFinishedPost(gtgPair.getCaller());
            eMailService.sendTextViaMail(gtgPair.getCallee(), callerPost);
            //}
        });
        return goToGuySet;
    }

    @Scheduled(cron = "${tnra.notify.schedule}")
    public void notifyWhatAndWhensOnSchedule() {
        GoToGuySet goToGuySet = gtgLatest();
        goToGuySet.getGoToGuyPairs().forEach(gtgPair -> {
            //if (gtgPair.getCallee().getFirstName().equalsIgnoreCase("micah")) {
            Post callerPost = postService.getLastFinishedPost(gtgPair.getCaller());
            eMailService.sendTextViaMail(gtgPair.getCallee(), callerPost);
            //}
        });
    }


//    @JsonView(JsonViews.Sparse.class)
//    @GetMapping("/gtg_test")
//    public GoToGuySet gtg() {
//        GoToGuySet gtgSet = new GoToGuySet();
//        List<GoToGuyPair> gtgPairs = new ArrayList<>();
//        gtgSet.setGoToGuyPairs(gtgPairs);
//        gtgSet.setStartDate(new Date());
//        users().forEach(user -> {
//            GoToGuyPair gtgPair = new GoToGuyPair();
//            gtgPair.setCallee(user);
//            gtgPair.setGoToGuySet(gtgSet);
//            gtgPairs.add(gtgPair);
//        });
//        for (int i=0; i<gtgPairs.size(); i++) {
//            int j = (i == gtgPairs.size()-1) ? 0 : (i+1);
//            gtgPairs.get(i).setCaller(gtgPairs.get(j).getCallee());
//        }
//
//        gtgSetRepository.save(gtgSet);
//
//        return gtgSet;
//    }

    @GetMapping("/in_progress")
    Optional<Post> getInProgressPost(Principal me) {
        User user = userRepository.findByEmail(me.getName());
        return postService.getOptionalInProgressPost(user);
    }

    @PostMapping("/in_progress")
    Post updatePost(Principal me, @RequestBody Post post) {
        return postService.savePost(post);
    }

    @GetMapping("/complete")
    Optional<Post> getCompletePost(Principal me) {
        User user = userRepository.findByEmail(me.getName());
        return postService.getOptionalCompletePost(user);
    }

    @GetMapping("/start_from_app")
    Post startPost(Principal me) {
        User user = userRepository.findByEmail(me.getName());
        return postService.startPost(user);
    }

    @PostMapping("/finish_from_app")
    Post finishPost(Principal me) {
        User user = userRepository.findByEmail(me.getName());
        Post post = postService.finishPost(user);
        eMailService.sendMailToAll(post);
        // use chat api to send to general
        Map<String, Object> charRes = slackAPIService.chat(slackPostRenderer.render(post));

        return post;
    }
}
