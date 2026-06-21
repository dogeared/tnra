package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.afitnerd.tnra.model.GroupSettings;
import com.afitnerd.tnra.model.PersonalStatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PersonalStatDefinitionRepository;
import com.afitnerd.tnra.repository.StatDefinitionRepository;
import com.afitnerd.tnra.service.FileStorageService;
import com.afitnerd.tnra.service.GroupSettingsService;
import com.afitnerd.tnra.service.PostDataExportService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.HttpClientErrorException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Route(value = "profile", layout = MainLayout.class)
@PermitAll
@CssImport("./styles/profile-view.css")
@PageTitle("Profile | TNRA")
public class ProfileView extends VerticalLayout {

    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final StatDefinitionRepository statDefinitionRepository;
    private final PersonalStatDefinitionRepository personalStatDefinitionRepository;
    private final GroupSettingsService groupSettingsService;
    private final PostDataExportService postDataExportService;
    private final transient Optional<BillingClient> billingClient;
    private User currentUser;
    private VerticalLayout myStatsList;
    VerticalLayout myStatsTabContent; // package-private for testing

    private TextField firstNameField;
    private TextField lastNameField;
    private TextField phoneNumberField;
    private Image profileImage;
    private Upload imageUpload;
    private Button saveButton;
    Button notificationsSaveButton; // package-private for testing
    private Checkbox notifyNewPostsCheckbox;

    // Slack publishing — package-private for testing
    VerticalLayout slackPublishSection;
    Checkbox slackPublishStatsCheckbox;
    Checkbox slackPublishPostBodyCheckbox;
    Span slackPublishStatsOverrideBadge;
    Span slackPublishPostBodyOverrideBadge;

    // Data export — package-private for testing
    DatePicker exportFromDatePicker;
    DatePicker exportToDatePicker;
    Checkbox exportAllDataCheckbox;
    Anchor exportDownloadLink;
    Button exportDownloadButton;
    private StreamResource exportStreamResource;

    // Billing tab — package-private for testing. Present only when billing is enabled.
    // Membership section (mirrors the /billing page):
    H3 billingTitle;
    Paragraph billingBlurb;
    HorizontalLayout billingPayButtons;
    Button billingMonthlyButton;
    Button billingYearlyButton;
    Button billingUpdatePaymentButton;
    // Gift a membership section:
    ComboBox<User> giftRecipientCombo;
    Button giftContinueButton;
    Paragraph giftDisabledNotice;
    // "Members you're covering" section (gifts you pay for):
    VerticalLayout coveringSection;

    // The app's external base URL — used to build the post-checkout redirect (LS needs an absolute URL).
    private final String baseUrl;
    
    // Phone number validation pattern
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\(?([0-9]{3})\\)?[-.\\s]?([0-9]{3})[-.\\s]?([0-9]{4})$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("[^0-9]");

    public ProfileView(
        UserService userService, FileStorageService fileStorageService,
        StatDefinitionRepository statDefinitionRepository,
        PersonalStatDefinitionRepository personalStatDefinitionRepository,
        GroupSettingsService groupSettingsService,
        PostDataExportService postDataExportService,
        Optional<BillingClient> billingClient,
        @Value("${tnra.app.base-url:http://localhost:8080}") String baseUrl
    ) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.statDefinitionRepository = statDefinitionRepository;
        this.personalStatDefinitionRepository = personalStatDefinitionRepository;
        this.groupSettingsService = groupSettingsService;
        this.postDataExportService = postDataExportService;
        this.billingClient = billingClient;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("profile-view");
        
        initComponents();
        loadUserData();
    }

    private void initComponents() {
        // Header
        H2 header = new H2("Profile");
        header.addClassName("profile-title");
        add(header);

        // Build all field components first so cross-tab loads/saves can reference them
        buildBasicInfoFields();
        buildNotificationFields();

        TabSheet tabs = new TabSheet();
        tabs.setSizeFull();
        tabs.addClassName("profile-tabs");
        tabs.add("Basic Info", createBasicInfoTabContent());
        tabs.add("Notifications", createNotificationsTabContent());
        myStatsTabContent = createMyStatsSection();
        tabs.add("My Stats", myStatsTabContent);
        tabs.add("Export", createDataExportSection());
        // Billing tab only when billing is enabled (the central service is wired in).
        if (billingClient.isPresent()) {
            Tab billingTab = tabs.add("Billing", createBillingTabContent());
            // Lazy-load: hit the billing service only when the Billing tab is actually opened (not on
            // every profile visit), and refresh on each open so the covering list stays current.
            tabs.addSelectedChangeListener(e -> {
                if (e.getSelectedTab() == billingTab) {
                    applyBillingTabState();
                }
            });
        }

        add(tabs);
    }

    /**
     * "Billing" tab: a membership section mirroring the {@code /billing} page (pay options shown only
     * when the member has no active subscription; "Update payment method" always shown), followed by the
     * "Gift a membership" section. {@link #applyBillingTabState()} (from {@link #loadUserData()}) sets pay
     * visibility and gift gating from the member's entitlement.
     */
    private VerticalLayout createBillingTabContent() {
        coveringSection = new VerticalLayout();
        coveringSection.setPadding(false);
        coveringSection.setSpacing(false);
        coveringSection.setVisible(false); // populated lazily in applyBillingTabState()

        VerticalLayout tab = new VerticalLayout(
            createMembershipSection(), new Hr(), createGiftSection(), coveringSection);
        tab.setPadding(true);
        tab.setSpacing(true);
        return tab;
    }

    /** Membership status + pay/manage controls — the same surface as the {@code /billing} page. */
    private VerticalLayout createMembershipSection() {
        billingTitle = new H3("Your membership");
        billingBlurb = new Paragraph(
            "Activate your membership to access posts, stats, and the call chain. "
                + "Choose monthly or yearly — you can change or cancel anytime.");

        billingMonthlyButton = new Button("Pay $7 / month", e -> startMembershipCheckout("monthly"));
        billingMonthlyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        billingYearlyButton = new Button("Pay $60 / year", e -> startMembershipCheckout("yearly"));
        billingYearlyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        billingPayButtons = new HorizontalLayout(billingMonthlyButton, billingYearlyButton);
        billingPayButtons.setVisible(false); // shown by applyBillingTabState() only if no active sub

        billingUpdatePaymentButton = new Button("Update payment method", e -> openMembershipPortal());

        VerticalLayout section = new VerticalLayout(
            billingTitle, billingBlurb, billingPayButtons, billingUpdatePaymentButton);
        section.setPadding(false);
        section.setSpacing(true);
        return section;
    }

    /** "Gift a membership" — pick another member and continue to {@code /billing?gift=<email>}. */
    private VerticalLayout createGiftSection() {
        H3 header = new H3("Gift a membership");
        Paragraph intro = new Paragraph(
            "Cover another member's membership. Pick someone below and continue to checkout — "
                + "you pay, and their membership activates once payment completes.");

        giftDisabledNotice = new Paragraph("Gifting is available once your own membership is active.");
        giftDisabledNotice.getStyle().set("color", "var(--lumo-error-text-color)");
        giftDisabledNotice.setVisible(false);

        giftRecipientCombo = new ComboBox<>("Member");
        giftRecipientCombo.setWidth("100%");
        giftRecipientCombo.setPlaceholder("Select a member to gift");
        giftRecipientCombo.setItemLabelGenerator(ProfileView::recipientLabel);
        giftRecipientCombo.addValueChangeListener(e ->
            giftContinueButton.setEnabled(e.getValue() != null));

        giftContinueButton = new Button("Continue to checkout", VaadinIcon.GIFT.create());
        giftContinueButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        giftContinueButton.setEnabled(false);
        giftContinueButton.addClickListener(e -> continueToGiftCheckout());

        VerticalLayout section = new VerticalLayout(header, intro, giftDisabledNotice,
            giftRecipientCombo, giftContinueButton);
        section.setPadding(false);
        section.setSpacing(true);
        return section;
    }

    /** Self-pay checkout from the Billing tab — mirrors {@code BillingView}. */
    void startMembershipCheckout(String variant) {
        if (currentUser == null || billingClient.isEmpty()) {
            return;
        }
        String me = currentUser.getEmail();
        try {
            String url = billingClient.get().createCheckout(me, variant, me, baseUrl + "/billing/activating");
            getUI().ifPresent(ui -> ui.getPage().setLocation(url));
        } catch (HttpClientErrorException.Conflict ex) {
            AppNotification.error(
                "You're already a member. Use \"Update payment method\" to manage your subscription.");
        } catch (Exception ex) {
            AppNotification.error("Sorry, we couldn't start checkout. Please try again.");
        }
    }

    /** Open the hosted Lemon Squeezy customer portal for the current member. */
    void openMembershipPortal() {
        if (currentUser == null || billingClient.isEmpty()) {
            return;
        }
        try {
            String url = billingClient.get().portalUrl(currentUser.getEmail());
            getUI().ifPresent(ui -> ui.getPage().setLocation(url));
        } catch (Exception ex) {
            AppNotification.error("Sorry, we couldn't open the payment portal. Please try again.");
        }
    }

    /** Navigate to the billing view in gift mode for the selected member. */
    void continueToGiftCheckout() {
        User recipient = giftRecipientCombo == null ? null : giftRecipientCombo.getValue();
        if (recipient == null || recipient.getEmail() == null) {
            return;
        }
        getUI().ifPresent(ui -> ui.navigate("billing",
            QueryParameters.simple(Map.of("gift", recipient.getEmail()))));
    }

    /**
     * Drives the Billing tab from the member's entitlement: pay options are shown only when they have no
     * active subscription, and the gift section is enabled only when they're entitled themselves (the
     * dropdown lists every other active member). One entitlement call serves both.
     */
    void applyBillingTabState() {
        if (billingClient.isEmpty() || giftRecipientCombo == null || currentUser == null) {
            return;
        }
        BillingClient.Entitlement ent = billingClient.get().entitlement(currentUser.getEmail());

        // Membership options only when there's no active subscription (ACTIVE or dunning). "Update
        // payment method" always stays visible.
        boolean hasLiveSubscription = "ACTIVE".equals(ent.status()) || "ON_GRACE_PERIOD".equals(ent.status());
        billingPayButtons.setVisible(!hasLiveSubscription);

        // Reflect the member's actual state in the blurb: active (and, if so, gifted by whom) vs. the
        // call-to-action to activate.
        boolean gifted = ent.payerEmail() != null && !ent.payerEmail().isBlank()
            && !ent.payerEmail().equalsIgnoreCase(currentUser.getEmail());
        if (hasLiveSubscription && gifted) {
            billingBlurb.setText("Your membership is active — gifted by " + gifterLabel(ent.payerEmail())
                + ", who manages the payment.");
        } else if (hasLiveSubscription) {
            billingBlurb.setText("Your membership is active. You can change or cancel it anytime "
                + "via \"Update payment method\".");
        } else {
            billingBlurb.setText("Activate your membership to access posts, stats, and the call chain. "
                + "Choose monthly or yearly — you can change or cancel anytime.");
        }

        // Gift section: list every other active member; enabled only when the member is entitled.
        List<User> others = userService.getAllActiveUsers().stream()
            .filter(u -> u.getEmail() != null && !u.getEmail().equalsIgnoreCase(currentUser.getEmail()))
            .toList();
        giftRecipientCombo.setItems(others);
        giftRecipientCombo.setEnabled(ent.entitled());
        giftContinueButton.setEnabled(false); // re-enabled by the combo listener once a member is picked
        giftDisabledNotice.setVisible(!ent.entitled());

        populateCoveringSection(currentUser.getEmail());
    }

    /**
     * Renders the "Members you're covering" list (gifts this member pays for), hidden when they cover
     * no one. Rebuilt from scratch each refresh — cheap (a handful of rows), and keeps state simple.
     * Managing/cancelling a specific gift happens in the Lemon Squeezy portal via "Update payment method".
     */
    private void populateCoveringSection(String email) {
        coveringSection.removeAll();
        coveringSection.setVisible(false);
        if (email == null) {
            return;
        }
        try {
            List<BillingClient.CoveredMember> covered = billingClient.get().covering(email);
            if (covered.isEmpty()) {
                return;
            }
            coveringSection.add(new H3("Members you're covering"));
            for (BillingClient.CoveredMember m : covered) {
                coveringSection.add(new Paragraph(m.email() + " — " + friendlyStatus(m.status())));
            }
            coveringSection.add(new Paragraph(
                "To change a card or stop covering someone, use \"Update payment method\" above — "
                + "the payment portal lists every subscription you pay for."));
            coveringSection.setVisible(true);
        } catch (Exception e) {
            // Non-critical: if the covering list can't load, just leave it hidden.
        }
    }

    private static String friendlyStatus(String status) {
        return status == null ? "" : status.toLowerCase().replace('_', ' ');
    }

    /** Resolve the gifter's email to a friendly "Name (email)" label, falling back to the email. */
    private String gifterLabel(String payerEmail) {
        User payer = userService.getUserByEmail(payerEmail);
        return payer == null ? payerEmail : recipientLabel(payer);
    }

    static String recipientLabel(User u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName().trim();
        String last = u.getLastName() == null ? "" : u.getLastName().trim();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? u.getEmail() : name + " (" + u.getEmail() + ")";
    }

    private void buildBasicInfoFields() {
        profileImage = new Image();
        profileImage.setWidth("120px");
        profileImage.setHeight("120px");
        profileImage.addClassName("profile-image");

        UI ui = UI.getCurrent();
        imageUpload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            if (ui != null) {
                ui.access(() -> processProfileImageUpload(metadata.fileName(), metadata.contentType(), data));
            } else {
                processProfileImageUpload(metadata.fileName(), metadata.contentType(), data);
            }
        }));
        imageUpload.setAcceptedFileTypes("image/*");
        imageUpload.setMaxFileSize(5 * 1024 * 1024); // 5MB limit
        imageUpload.setDropLabel(new Div(new Paragraph("Drop image here or click to upload")));
        imageUpload.setUploadButton(new Button("Upload Image"));

        firstNameField = new TextField("First Name");
        firstNameField.setWidth("100%");
        firstNameField.setMaxLength(50);

        lastNameField = new TextField("Last Name");
        lastNameField.setWidth("100%");
        lastNameField.setMaxLength(50);

        phoneNumberField = createPhoneNumberField();
    }

    private void buildNotificationFields() {
        notifyNewPostsCheckbox = new Checkbox("Notify me when someone posts a weekly update");
        notifyNewPostsCheckbox.setValue(true);

        // Slack publishing — visibility/disabled state computed in loadUserData()
        slackPublishStatsCheckbox = new Checkbox("Publish my stats to Slack when I finish a post");
        slackPublishPostBodyCheckbox = new Checkbox("Publish my post body to Slack when I finish a post");
        slackPublishStatsOverrideBadge = createOverrideBadge();
        slackPublishPostBodyOverrideBadge = createOverrideBadge();
    }

    private VerticalLayout createBasicInfoTabContent() {
        VerticalLayout imageSection = new VerticalLayout(profileImage, imageUpload);
        imageSection.setSpacing(true);
        imageSection.setPadding(false);
        imageSection.addClassName("profile-image-section");

        // The single Save button persists every field on the user — fields
        // outside Basic Info (notifications, slack) still load with the user,
        // so saving here doesn't lose anything edited on another tab.
        saveButton = new Button("Save Changes", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveProfile());

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSpacing(true);
        formLayout.setPadding(false);
        formLayout.addClassName("form-section");
        formLayout.add(firstNameField, lastNameField, phoneNumberField, saveButton);

        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSpacing(true);
        mainLayout.setPadding(false);
        mainLayout.add(imageSection, formLayout);
        mainLayout.setFlexGrow(1, formLayout);

        VerticalLayout tab = new VerticalLayout(mainLayout);
        tab.setPadding(true);
        tab.setSpacing(true);
        return tab;
    }

    private VerticalLayout createNotificationsTabContent() {
        H3 emailHeader = new H3("Email Notifications");
        VerticalLayout emailSection = new VerticalLayout(emailHeader, notifyNewPostsCheckbox);
        emailSection.setSpacing(true);
        emailSection.setPadding(false);

        H3 slackHeader = new H3("Slack publishing");
        HorizontalLayout statsRow = checkboxRow(slackPublishStatsCheckbox, slackPublishStatsOverrideBadge);
        HorizontalLayout bodyRow = checkboxRow(slackPublishPostBodyCheckbox, slackPublishPostBodyOverrideBadge);
        slackPublishSection = new VerticalLayout(slackHeader, statsRow, bodyRow);
        slackPublishSection.setSpacing(true);
        slackPublishSection.setPadding(false);
        slackPublishSection.setVisible(false); // default hidden; loadUserData() reveals if group allows

        notificationsSaveButton = new Button("Save Changes", VaadinIcon.CHECK.create());
        notificationsSaveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        notificationsSaveButton.addClickListener(e -> saveProfile());

        VerticalLayout tab = new VerticalLayout(emailSection, slackPublishSection, notificationsSaveButton);
        tab.setPadding(true);
        tab.setSpacing(true);
        return tab;
    }

    private VerticalLayout createDataExportSection() {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(false);
        section.addClassName("data-export-section");

        H3 header = new H3("Download my data");

        Paragraph warning = new Paragraph(
            "⚠️ Heads up: your post content is encrypted at rest in our database. "
                + "Downloading a CSV copy means that data leaves the encrypted system "
                + "and lives on your device in plaintext. Treat the downloaded file as "
                + "you would any other sensitive document."
        );
        warning.getStyle()
            .set("background-color", "var(--lumo-error-color-10pct)")
            .set("border-left", "4px solid var(--lumo-error-color)")
            .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
            .set("border-radius", "var(--lumo-border-radius-m)");

        exportFromDatePicker = new DatePicker("From");
        exportToDatePicker = new DatePicker("To");
        exportAllDataCheckbox = new Checkbox("All my data (ignore date range)");
        exportAllDataCheckbox.addValueChangeListener(ev -> {
            boolean all = Boolean.TRUE.equals(ev.getValue());
            exportFromDatePicker.setEnabled(!all);
            exportToDatePicker.setEnabled(!all);
            updateExportDownloadState();
        });
        exportFromDatePicker.addValueChangeListener(ev -> updateExportDownloadState());
        exportToDatePicker.addValueChangeListener(ev -> updateExportDownloadState());

        HorizontalLayout rangeRow = new HorizontalLayout(exportFromDatePicker, exportToDatePicker);
        rangeRow.setSpacing(true);
        rangeRow.setPadding(false);
        rangeRow.setAlignItems(Alignment.END);

        // The Anchor wraps a download button. Clicking the button bubbles up
        // to the Anchor, the browser follows the href, which streams the CSV.
        // The StreamResource supplier runs at fetch time, so it always reads
        // the CURRENT field values — each click yields a fresh CSV. The
        // `download` attribute is recomputed in updateExportDownloadState()
        // whenever the selection changes so the filename reflects the range.
        exportStreamResource = new StreamResource("tnra-posts.csv", () -> new ByteArrayInputStream(buildExportCsv()));
        exportStreamResource.setContentType("text/csv;charset=UTF-8");

        // Start with no href so the disabled-by-default download is also
        // non-navigable. updateExportDownloadState() attaches the resource
        // once the user has selected something.
        exportDownloadLink = new Anchor();
        exportDownloadButton = new Button("Download CSV", VaadinIcon.DOWNLOAD.create());
        exportDownloadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        exportDownloadLink.add(exportDownloadButton);

        section.add(header, warning, rangeRow, exportAllDataCheckbox, exportDownloadLink);
        updateExportDownloadState();
        return section;
    }

    /**
     * Recomputes the download button's enabled state, the link's href, and the
     * file's filename from current field values. When nothing is selected the
     * button is disabled <em>and</em> the {@link Anchor}'s href is removed, so
     * clicks have nothing to navigate to — a disabled button inside an Anchor
     * with a live href would still trigger the link.
     */
    void updateExportDownloadState() {
        boolean all = exportAllDataCheckbox != null && Boolean.TRUE.equals(exportAllDataCheckbox.getValue());
        LocalDate from = exportFromDatePicker == null ? null : exportFromDatePicker.getValue();
        LocalDate to = exportToDatePicker == null ? null : exportToDatePicker.getValue();
        boolean enabled = shouldEnableExportDownload(all, from, to);
        if (exportDownloadButton != null) {
            exportDownloadButton.setEnabled(enabled);
        }
        if (exportDownloadLink != null) {
            if (enabled && exportStreamResource != null) {
                exportDownloadLink.setHref(exportStreamResource);
                exportDownloadLink.getElement().setAttribute("download", buildExportFilename(all, from, to));
            } else {
                exportDownloadLink.removeHref();
                exportDownloadLink.getElement().removeAttribute("download");
            }
        }
    }

    /**
     * Pure decision logic for whether the export download is meaningful.
     * Extracted so tests can verify both directions without hitting Vaadin's
     * detached-component {@code isEnabled()} cascade quirk.
     */
    static boolean shouldEnableExportDownload(boolean all, LocalDate from, LocalDate to) {
        return all || from != null || to != null;
    }

    /**
     * Composes the download filename from the current selection:
     *   tnra-posts-all.csv             when "All my data" is checked
     *   tnra-posts-from-FROM-to-TO.csv when both date bounds are set
     *   tnra-posts-from-FROM.csv       when only the from date is set
     *   tnra-posts-through-TO.csv      when only the to date is set
     */
    static String buildExportFilename(boolean all, LocalDate from, LocalDate to) {
        if (all) {
            return "tnra-posts-all.csv";
        }
        if (from != null && to != null) {
            return "tnra-posts-from-" + from + "-to-" + to + ".csv";
        }
        if (from != null) {
            return "tnra-posts-from-" + from + ".csv";
        }
        if (to != null) {
            return "tnra-posts-through-" + to + ".csv";
        }
        return "tnra-posts.csv"; // never used — button is disabled in this state
    }

    /**
     * Reads current selections off the date pickers and "all data" checkbox
     * and calls the export service. Invoked by the {@link StreamResource} at
     * fetch time, so the latest UI state wins on every click.
     */
    byte[] buildExportCsv() {
        if (currentUser == null) {
            return new byte[0];
        }
        boolean all = Boolean.TRUE.equals(exportAllDataCheckbox.getValue());
        LocalDate from = all ? null : exportFromDatePicker.getValue();
        LocalDate to = all ? null : exportToDatePicker.getValue();
        return postDataExportService.exportToCsv(currentUser, from, to);
    }

    private VerticalLayout createMyStatsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(false);

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setAlignItems(Alignment.CENTER);
        H3 statsHeader = new H3("My Stats");
        Button addStatBtn = new Button("Add Stat", VaadinIcon.PLUS.create());
        addStatBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        addStatBtn.addClickListener(e -> openAddPersonalStatDialog());
        headerRow.add(statsHeader, addStatBtn);

        myStatsList = new VerticalLayout();
        myStatsList.setSpacing(false);
        myStatsList.setPadding(false);

        section.add(headerRow, myStatsList);
        return section;
    }

    void refreshMyStatsList() {
        if (myStatsList == null || currentUser == null) return;
        myStatsList.removeAll();

        java.util.List<PersonalStatDefinition> allStats = personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(currentUser);

        if (allStats.isEmpty()) {
            Paragraph empty = new Paragraph("No personal stats yet. Add your first to track something just for you.");
            myStatsList.add(empty);
            return;
        }

        java.util.List<PersonalStatDefinition> activeStats = allStats.stream().filter(s -> !s.getArchived()).toList();
        java.util.List<PersonalStatDefinition> archivedStats = allStats.stream().filter(PersonalStatDefinition::getArchived).toList();

        for (int i = 0; i < activeStats.size(); i++) {
            PersonalStatDefinition stat = activeStats.get(i);
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setAlignItems(Alignment.CENTER);

            Span emoji = new Span(stat.getEmoji() != null ? stat.getEmoji() : "");
            Span label = new Span(stat.getLabel());

            Button upBtn = new Button(VaadinIcon.ARROW_UP.create());
            upBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            upBtn.setEnabled(i > 0);
            upBtn.addClickListener(e -> movePersonalStatUp(stat));

            Button downBtn = new Button(VaadinIcon.ARROW_DOWN.create());
            downBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            downBtn.setEnabled(i < activeStats.size() - 1);
            downBtn.addClickListener(e -> movePersonalStatDown(stat));

            Button archiveBtn = new Button(VaadinIcon.CLOSE_SMALL.create());
            archiveBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            archiveBtn.addClickListener(e -> archivePersonalStat(stat));

            row.add(emoji, label, upBtn, downBtn, archiveBtn);
            myStatsList.add(row);
        }

        for (PersonalStatDefinition stat : archivedStats) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setAlignItems(Alignment.CENTER);
            row.getStyle().set("opacity", "0.5");

            Span emoji = new Span(stat.getEmoji() != null ? stat.getEmoji() : "");
            Span label = new Span(stat.getLabel());
            Span badge = new Span("archived");
            badge.getStyle().set("font-size", "0.8em").set("color", "gray");

            Button restoreBtn = new Button("Restore");
            restoreBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            restoreBtn.addClickListener(e -> restorePersonalStat(stat));

            row.add(emoji, label, badge, restoreBtn);
            myStatsList.add(row);
        }
    }

    void openAddPersonalStatDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Personal Stat");

        FormLayout form = new FormLayout();
        TextField nameField = new TextField("Internal Name");
        nameField.setHelperText("Lowercase, no spaces (e.g., 'guitar_practice')");
        nameField.setPattern("[a-z_]+");

        TextField labelField = new TextField("Display Label");
        labelField.setHelperText("What you see (e.g., 'Guitar Practice')");

        TextField emojiField = new TextField("Emoji");
        emojiField.setMaxLength(10);
        emojiField.setWidth("80px");

        form.add(nameField, labelField, emojiField);

        Button saveBtn = new Button("Add Stat", VaadinIcon.CHECK.create());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(e -> {
            String name = nameField.getValue().trim().toLowerCase();
            String label = labelField.getValue().trim();
            String emoji = emojiField.getValue().trim();
            handleAddPersonalStat(name, label, emoji, dialog);
        });

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    void handleAddPersonalStat(String name, String label, String emoji, Dialog dialog) {
        if (name.isEmpty() || label.isEmpty()) {
            AppNotification.error("Name and label are required");
            return;
        }

        boolean globalNameExists = statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()
            .stream().anyMatch(s -> name.equals(s.getName()));
        if (globalNameExists) {
            AppNotification.error("A group stat named '" + name + "' already exists");
            return;
        }

        boolean personalNameExists = personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(currentUser)
            .stream().anyMatch(s -> name.equals(s.getName()));
        if (personalNameExists) {
            AppNotification.error("You already have a stat named '" + name + "'");
            return;
        }

        java.util.List<PersonalStatDefinition> activeStats = personalStatDefinitionRepository
            .findByUserAndArchivedFalseOrderByDisplayOrderAsc(currentUser);
        int nextOrder = activeStats.stream()
            .mapToInt(PersonalStatDefinition::getDisplayOrder)
            .max().orElse(-1) + 1;

        PersonalStatDefinition newStat = new PersonalStatDefinition(
            name, label, emoji.isEmpty() ? null : emoji, nextOrder, currentUser
        );
        personalStatDefinitionRepository.save(newStat);
        refreshMyStatsList();
        dialog.close();

        AppNotification.success(label + " added");
    }

    void movePersonalStatUp(PersonalStatDefinition stat) {
        java.util.List<PersonalStatDefinition> activeStats = personalStatDefinitionRepository
            .findByUserAndArchivedFalseOrderByDisplayOrderAsc(currentUser);
        for (int i = 1; i < activeStats.size(); i++) {
            if (activeStats.get(i).getId().equals(stat.getId())) {
                PersonalStatDefinition prev = activeStats.get(i - 1);
                int temp = stat.getDisplayOrder();
                stat.setDisplayOrder(prev.getDisplayOrder());
                prev.setDisplayOrder(temp);
                personalStatDefinitionRepository.save(stat);
                personalStatDefinitionRepository.save(prev);
                break;
            }
        }
        refreshMyStatsList();
    }

    void movePersonalStatDown(PersonalStatDefinition stat) {
        java.util.List<PersonalStatDefinition> activeStats = personalStatDefinitionRepository
            .findByUserAndArchivedFalseOrderByDisplayOrderAsc(currentUser);
        for (int i = 0; i < activeStats.size() - 1; i++) {
            if (activeStats.get(i).getId().equals(stat.getId())) {
                PersonalStatDefinition next = activeStats.get(i + 1);
                int temp = stat.getDisplayOrder();
                stat.setDisplayOrder(next.getDisplayOrder());
                next.setDisplayOrder(temp);
                personalStatDefinitionRepository.save(stat);
                personalStatDefinitionRepository.save(next);
                break;
            }
        }
        refreshMyStatsList();
    }

    void archivePersonalStat(PersonalStatDefinition stat) {
        stat.setArchived(true);
        personalStatDefinitionRepository.save(stat);
        refreshMyStatsList();
        AppNotification.success(stat.getLabel() + " archived");
    }

    void restorePersonalStat(PersonalStatDefinition stat) {
        // Check if a global stat now has this name — in-memory after encryption
        boolean globalNameExists = statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()
            .stream().anyMatch(s -> stat.getName().equals(s.getName()));
        if (globalNameExists) {
            AppNotification.error("Can't restore: a group stat named '" + stat.getName() + "' now exists");
            return;
        }

        stat.setArchived(false);
        java.util.List<PersonalStatDefinition> activeStats = personalStatDefinitionRepository
            .findByUserAndArchivedFalseOrderByDisplayOrderAsc(currentUser);
        int maxOrder = activeStats.stream()
            .mapToInt(PersonalStatDefinition::getDisplayOrder)
            .max().orElse(-1);
        stat.setDisplayOrder(maxOrder + 1);
        personalStatDefinitionRepository.save(stat);
        refreshMyStatsList();
        AppNotification.success(stat.getLabel() + " restored");
    }

    void processProfileImageUpload(String fileName, String contentType, byte[] data) {
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            if (currentUser.getProfileImage() != null && !currentUser.getProfileImage().isEmpty()) {
                fileStorageService.deleteFile(currentUser.getProfileImage());
            }

            String storedFileName = fileStorageService.storeFile(inputStream, fileName, contentType);
            currentUser.setProfileImage(storedFileName);
            userService.saveUser(currentUser);

            String imageUrl = fileStorageService.getFileUrl(storedFileName);
            profileImage.setSrc(imageUrl);

            AppNotification.success("Image uploaded successfully");
        } catch (IOException e) {
            AppNotification.error("Error uploading image");
        }
    }
    
    private TextField createPhoneNumberField() {
        TextField phoneField = new TextField("Phone Number");
        phoneField.setWidth("100%");
        phoneField.setMaxLength(20);
        phoneField.setPlaceholder("(555) 123-4567");
        
        // Set value change mode to EAGER for real-time formatting
        phoneField.setValueChangeMode(ValueChangeMode.EAGER);
        phoneField.setValueChangeTimeout(100);
        
        // Add value change listener for formatting
        phoneField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value != null && !value.trim().isEmpty()) {
                String formatted = formatPhoneNumber(value);
                if (!formatted.equals(value)) {
                    // Update the field with formatted value
                    phoneField.setValue(formatted);
                }
                
                // Validate and show error if invalid
                if (!isValidPhoneNumber(formatted)) {
                    phoneField.setErrorMessage("Please enter a valid phone number");
                    phoneField.setInvalid(true);
                } else {
                    phoneField.setErrorMessage(null);
                    phoneField.setInvalid(false);
                }
            } else {
                phoneField.setErrorMessage(null);
                phoneField.setInvalid(false);
            }
        });
        
        return phoneField;
    }
    
    String formatPhoneNumber(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        
        // Remove all non-digit characters
        String digitsOnly = DIGITS_ONLY.matcher(input).replaceAll("");
        
        // Format based on number of digits
        if (digitsOnly.length() == 0) {
            return "";
        } else if (digitsOnly.length() <= 3) {
            return digitsOnly;
        } else if (digitsOnly.length() <= 6) {
            return "(" + digitsOnly.substring(0, 3) + ") " + digitsOnly.substring(3);
        } else if (digitsOnly.length() <= 10) {
            return "(" + digitsOnly.substring(0, 3) + ") " + digitsOnly.substring(3, 6) + "-" + digitsOnly.substring(6);
        } else {
            // If more than 10 digits, truncate to 10
            return "(" + digitsOnly.substring(0, 3) + ") " + digitsOnly.substring(3, 6) + "-" + digitsOnly.substring(6, 10);
        }
    }
    
    boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return true; // Allow empty phone numbers
        }
        return PHONE_PATTERN.matcher(phoneNumber).matches();
    }
    
    String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return "";
        }
        // Remove all formatting and return digits only
        return DIGITS_ONLY.matcher(phoneNumber).replaceAll("");
    }

    private void loadUserData() {
        currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            firstNameField.setValue(currentUser.getFirstName() != null ? currentUser.getFirstName() : "");
            lastNameField.setValue(currentUser.getLastName() != null ? currentUser.getLastName() : "");
            
            // Format the phone number when loading
            String phoneNumber = currentUser.getPhoneNumber();
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                phoneNumberField.setValue(formatPhoneNumber(phoneNumber));
            } else {
                phoneNumberField.setValue("");
            }

            notifyNewPostsCheckbox.setValue(Boolean.TRUE.equals(currentUser.getNotifyNewPosts()));

            applySlackPublishSectionState();

            if (currentUser.getProfileImage() != null && !currentUser.getProfileImage().isEmpty()) {
                String imageUrl = fileStorageService.getFileUrl(currentUser.getProfileImage());
                profileImage.setSrc(imageUrl);
            } else {
                // Set default avatar
                profileImage.setSrc("/uploads/placeholder.png");
            }

            refreshMyStatsList();
            // Billing tab data is loaded lazily when that tab is opened (see initComponents).
        }
    }

    void saveProfile() {
        if (currentUser != null) {
            // Validate phone number before saving
            String phoneNumber = phoneNumberField.getValue();
            if (phoneNumber != null && !phoneNumber.trim().isEmpty() && !isValidPhoneNumber(phoneNumber)) {
                AppNotification.error("Please enter a valid phone number");
                return;
            }
            
            currentUser.setFirstName(firstNameField.getValue());
            currentUser.setLastName(lastNameField.getValue());
            // Store normalized phone number (digits only)
            currentUser.setPhoneNumber(normalizePhoneNumber(phoneNumber));
            currentUser.setNotifyNewPosts(notifyNewPostsCheckbox.getValue());

            // Persist whichever value the user can actually change. When a group-level
            // override is on, the corresponding checkbox is disabled in the UI and its
            // value is force-checked; we keep the previously stored value so that
            // turning the global override off later restores the member's choice.
            GroupSettings settings = groupSettingsService.getSettings();
            if (settings.isSlackPublishPostData() && !settings.isSlackPublishStats()) {
                currentUser.setSlackPublishStats(slackPublishStatsCheckbox.getValue());
            }
            if (settings.isSlackPublishPostData() && !settings.isSlackPublishPostBody()) {
                currentUser.setSlackPublishPostBody(slackPublishPostBodyCheckbox.getValue());
            }

            try {
                userService.saveUser(currentUser);
                AppNotification.success("Profile saved successfully");
            } catch (Exception e) {
                AppNotification.error("Error saving profile");
            }
        }
    }

    /**
     * Computes visibility + enabled state for the Slack publishing section based on
     * the group's master toggle and per-channel overrides. When the master is off,
     * the whole section is hidden. When a per-channel override is on, the matching
     * checkbox is shown checked and read-only, with a visible "Required by group"
     * badge so the override is obvious without hovering.
     */
    void applySlackPublishSectionState() {
        GroupSettings settings = groupSettingsService.getSettings();
        boolean masterOn = settings.isSlackPublishPostData();
        slackPublishSection.setVisible(masterOn);
        if (!masterOn) {
            return;
        }
        boolean statsForced = settings.isSlackPublishStats();
        slackPublishStatsCheckbox.setValue(statsForced || Boolean.TRUE.equals(currentUser.getSlackPublishStats()));
        // setEnabled(false) paints the greyed/disabled styling on first render.
        // setReadOnly(true) prevents edits but Vaadin applies the disabled theme
        // on a follow-up property push, so the initial paint shows primary
        // color until the next round-trip — confusing.
        slackPublishStatsCheckbox.setEnabled(!statsForced);
        slackPublishStatsOverrideBadge.setVisible(statsForced);

        boolean bodyForced = settings.isSlackPublishPostBody();
        slackPublishPostBodyCheckbox.setValue(bodyForced || Boolean.TRUE.equals(currentUser.getSlackPublishPostBody()));
        slackPublishPostBodyCheckbox.setEnabled(!bodyForced);
        slackPublishPostBodyOverrideBadge.setVisible(bodyForced);
    }

    private Span createOverrideBadge() {
        Span badge = new Span("(Required by group settings)");
        badge.getElement().getThemeList().add("badge contrast small");
        badge.getStyle().set("margin-inline-start", "var(--lumo-space-s)");
        badge.setVisible(false);
        return badge;
    }

    private HorizontalLayout checkboxRow(Checkbox checkbox, Span badge) {
        HorizontalLayout row = new HorizontalLayout(checkbox, badge);
        row.setSpacing(false);
        row.setPadding(false);
        row.setAlignItems(Alignment.CENTER);
        return row;
    }
} 
