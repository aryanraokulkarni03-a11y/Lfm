package com.campus.lostfound;

import com.campus.lostfound.config.*;
import com.campus.lostfound.domain.enums.*;
import com.campus.lostfound.domain.model.*;
import com.campus.lostfound.exception.*;
import com.campus.lostfound.repository.jdbc.*;
import com.campus.lostfound.security.PasswordHasher;
import com.campus.lostfound.service.*;
import com.campus.lostfound.validation.EmailValidator;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public final class App extends Application {
    private final ConnectionProvider connections = new ConnectionProvider(DatabaseConfig.load());
    private final AuthenticationService authentication = new AuthenticationService(new JdbcUserRepository(connections), new PasswordHasher(), new EmailValidator());
    private final ItemService itemService = new ItemService(new JdbcItemRepository(connections), new JdbcItemImageRepository(connections));
    private final ClaimService claimService = new ClaimService(new JdbcClaimRepository(connections));
    private final AdminService adminService = new AdminService(new JdbcUserRepository(connections));
    private final ProfileService profileService = new ProfileService(new JdbcProfileRepository(connections));
    private Stage stage;
    private User currentUser;
    private final Preferences localPreferences = Preferences.userNodeForPackage(App.class);

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        loadTypography();
        showLogin();
        stage.setTitle("SRM AP Lost and Found Management");
        stage.show();
        restoreRememberedSession();
    }
    private void loadTypography() {
        for (var font : List.of("PlusJakartaSans-Regular.ttf", "PlusJakartaSans-Medium.ttf", "PlusJakartaSans-SemiBold.ttf", "PlusJakartaSans-Bold.ttf", "PlusJakartaSans-ExtraBold.ttf")) {
            try (var stream = getClass().getResourceAsStream("/fonts/" + font)) {
                if (stream != null) Font.loadFont(stream, 14);
            } catch (IOException ignored) { }
        }
    }
    private void showLogin() {
        var email = new TextField(localPreferences.get("lastEmail", ""));
        email.setPromptText("you@srmap.edu.in");
        var password = passwordInput("Password");
        var remember = new CheckBox("Keep me signed in on this device");
        remember.setSelected(localPreferences.get("sessionEmail", "")
                .equalsIgnoreCase(email.getText().trim()));
        var feedback = feedback();
        var signIn = primaryButton("Sign in");
        var register = secondaryButton("Create student account");

        signIn.setOnAction(event -> runTask(signIn, feedback, "Signing in…",
                () -> authentication.login(email.getText(), password.text()), user -> {
                    localPreferences.put("lastEmail", email.getText().trim().toLowerCase());
                    if (remember.isSelected()) {
                        localPreferences.put("sessionEmail", user.email());
                    } else {
                        localPreferences.remove("sessionEmail");
                    }
                    currentUser = user;
                    showDashboard();
                }));
        register.setOnAction(event -> showRegistration());

        var form = new VBox(16, styled("LOGIN", "auth-title"),
                stackedField("University email", email),
                stackedField("Password", password.view), remember, signIn, register, feedback);
        form.getStyleClass().add("auth-form");
        showScene(loginLayout(form));
    }
    private void restoreRememberedSession() {
        var rememberedEmail = localPreferences.get("sessionEmail", "");
        if (rememberedEmail.isBlank()) return;
        runTask(null, null, "", () -> authentication.restore(rememberedEmail), user -> { currentUser = user; showDashboard(); });
    }
    private void showRegistration() {
        var email = new TextField();
        email.setPromptText("you@srmap.edu.in");
        var password = passwordInput("At least 8 characters");
        var confirm = passwordInput("Repeat password");
        var feedback = feedback();
        var create = primaryButton("Create account");
        var back = secondaryButton("Back to sign in");

        create.setOnAction(event -> {
            if (!password.text().equals(confirm.text())) {
                error(feedback, "Passwords do not match.");
                return;
            }
            runTask(create, feedback, "Creating account…",
                    () -> authentication.register(email.getText(), password.text()), user -> {
                        feedback.getStyleClass().remove("error");
                        feedback.setText("Account created. Sign in and complete your profile.");
                    });
        });
        back.setOnAction(event -> showLogin());

        showScene(authCard("Join SRM AP LFM.",
                field("University email", email), field("Password", password.view),
                field("Confirm password", confirm.view), create, back, feedback));
    }
    private void showDashboard() {
        var sidebar = new VBox(18, logo());
        sidebar.getStyleClass().add("sidebar");
        var content = new StackPane();
        content.getStyleClass().add("workspace");

        addNav(sidebar, "My profile", "profile", () -> showProfile(content));
        addNav(sidebar, "Search listings", "search", () -> showSearch(content));
        addNav(sidebar, "Report an item", "report", () -> showReport(content));
        String claimsLabel = currentUser.role() == Role.ADMIN ? "Claims overview" : "My claims";
        addNav(sidebar, claimsLabel, "claims", () -> showClaimsForCurrentUser(content));

        if (currentUser.role() == Role.ADMIN) {
            addNav(sidebar, "Admin claims", "admin", () -> showAdminClaims(content));
            addNav(sidebar, "All reports", "reports", () -> showAllReports(content));
            addNav(sidebar, "Users", "users", () -> showUsers(content));
        }

        var spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        var logout = sidebarButton("Logout", "logout");
        logout.setOnAction(event -> {
            currentUser = null;
            localPreferences.remove("sessionEmail");
            showLogin();
        });
        sidebar.getChildren().add(logout);

        var root = new BorderPane(content);
        root.setLeft(sidebar);
        showScene(root);
        showProfile(content);
    }
    private void showSearch(StackPane target) {
        var keyword = new TextField(); keyword.setPromptText("Search title, description, or location"); ComboBox<String> category = selectControl(); category.getItems().addAll("All categories", "Electronics", "Documents / ID cards", "Clothing", "Accessories", "Keys", "Bags", "Other"); category.getSelectionModel().selectFirst(); ComboBox<ReportType> type = selectControl(); type.getItems().addAll(ReportType.values()); type.setPromptText("Type"); var results = new VBox(10); results.getStyleClass().add("results"); var search = searchButton();
        search.setOnAction(e -> runTask(search, new Label(), "Searching…", () -> itemService.search(keyword.getText(), category.getValue(), type.getValue(), ItemStatus.OPEN, false), items -> renderItems(results, items, true))); target.getChildren().setAll(view("Find a reported item", searchGroup(category, keyword, type, search), results)); runTask(search, new Label(), "Loading…", () -> itemService.search("", "All categories", null, ItemStatus.OPEN, false), items -> renderItems(results, items, true));
    }
    private void showReport(StackPane target) {
        ComboBox<ReportType> type = selectControl(); type.getItems().addAll(ReportType.values()); type.getSelectionModel().selectFirst(); var title = new TextField(); ComboBox<String> category = selectControl(); category.getItems().addAll("Electronics", "Documents / ID cards", "Clothing", "Accessories", "Keys", "Bags", "Other"); category.getSelectionModel().selectFirst(); var location = new TextField(); var date = new DatePicker(LocalDate.now()); var description = new TextArea(); var marks = new TextArea(); var contact = new TextField(); var images = new Label("No images selected · PNG, JPG or GIF"); var selected = new ArrayList<Path>(); var previews = new FlowPane(8, 8); previews.getStyleClass().add("report-image-previews"); var choose = secondaryButton("Choose images");
        choose.setOnAction(e -> { var chooser = new FileChooser(); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")); var files = chooser.showOpenMultipleDialog(stage); if (files != null) { selected.clear(); selected.addAll(files.stream().map(java.io.File::toPath).toList()); images.setText(selected.size() + (selected.size() == 1 ? " image selected" : " images selected")); previews.getChildren().setAll(selected.stream().map(App::reportImagePreview).toList()); } }); var imagePicker = new HBox(12, choose, images); imagePicker.setAlignment(Pos.CENTER_LEFT); imagePicker.setMaxWidth(Double.MAX_VALUE); imagePicker.getStyleClass().add("report-image-picker"); var imageField = new VBox(8, imagePicker, previews); imageField.setMaxWidth(Double.MAX_VALUE); var feedback = feedback(); var submit = primaryButton("Publish report");
        submit.setOnAction(e -> runTask(submit, feedback, "Saving report…", () -> { var stored = copyImages(selected); return itemService.report(new Item(0, currentUser.id(), currentUser.email(), type.getValue(), title.getText(), description.getText(), category.getValue(), location.getText(), date.getValue(), marks.getText(), contact.getText(), ItemStatus.OPEN, stored)); }, item -> { feedback.setText("Report #" + item.id() + " published successfully."); clear(title, location, description, marks, contact); selected.clear(); previews.getChildren().clear(); images.setText("No images selected · PNG, JPG or GIF"); }));
        var form = new GridPane(); form.setHgap(12); form.setVgap(10); form.setPrefWidth(760); form.setMaxWidth(760); form.getStyleClass().add("report-form"); var labels = new ColumnConstraints(); labels.setMinWidth(145); labels.setPrefWidth(145); var inputs = new ColumnConstraints(); inputs.setMinWidth(360); inputs.setPrefWidth(560); inputs.setMaxWidth(560); inputs.setHgrow(Priority.ALWAYS); form.getColumnConstraints().addAll(labels, inputs); form.addRow(0, label("Report type"), type); form.addRow(1, label("Title"), title); form.addRow(2, label("Category"), category); form.addRow(3, label("Location"), location); form.addRow(4, label("Date"), date); form.addRow(5, label("Description"), description); form.addRow(6, label("Identifying marks"), marks); form.addRow(7, label("Contact details"), contact); form.addRow(8, label("Images"), imageField); form.add(submit, 0, 9); GridPane.setMargin(submit, new Insets(16, 0, 0, 0)); form.add(feedback, 1, 10); description.setPrefRowCount(3); marks.setPrefRowCount(2); for (var control : List.of(type, title, category, location, date, description, marks, contact)) control.setMaxWidth(Double.MAX_VALUE); choose.setMinHeight(46); choose.setPrefHeight(46); submit.setMinHeight(46); submit.setPrefHeight(46); var formCard = card(form); formCard.setMaxWidth(1080); var formScroll = new ScrollPane(formCard); formScroll.setFitToWidth(true); formScroll.setPrefViewportHeight(640); target.getChildren().setAll(view("Tell the campus what happened", formScroll));
    }
    private static Node reportImagePreview(Path path) {
        var image = new Image(path.toUri().toString(), 104, 72, true, true, true);
        var imageView = new ImageView(image);
        imageView.setFitWidth(104);
        imageView.setFitHeight(72);
        imageView.setPreserveRatio(true);
        imageView.setAccessibleText(path.getFileName().toString());
        var frame = new StackPane(imageView);
        frame.getStyleClass().add("report-image-preview-frame");
        var fileName = new Label(path.getFileName().toString());
        fileName.setMaxWidth(112);
        fileName.setTextOverrun(OverrunStyle.ELLIPSIS);
        fileName.getStyleClass().add("report-image-name");
        var preview = new VBox(4, frame, fileName);
        preview.setMaxWidth(112);
        preview.getStyleClass().add("report-image-preview");
        return preview;
    }
    private void showMyClaims(StackPane target) { var box = new VBox(10); target.getChildren().setAll(view("Your private ownership requests", box)); runTask(null, new Label(), "Loading…", () -> claimService.mine(currentUser.id()), claims -> renderClaimsWithHandoff(box, claims)); }
    private void showClaimsForCurrentUser(StackPane target) { if (currentUser.role() == Role.ADMIN) showAllClaims(target); else showMyClaims(target); }
    private void showAllClaims(StackPane target) { var box = new VBox(10); target.getChildren().setAll(view("Claim outcomes", box)); runTask(null, new Label(), "Loading…", claimService::all, claims -> renderClaimResults(box, claims)); }
    private void showProfile(StackPane target) {
        var loading = new Label("Loading profile…");
        target.getChildren().setAll(view("Your campus identity", loading));
        runTask(null, loading, "Loading…", () -> profileService.find(currentUser.id()), profile -> profile.ifPresentOrElse(p -> showProfileCard(target, p), () -> showProfileEditor(target, null)));
    }

    private void showProfileCard(StackPane target, UserProfile profile) {
        var edit = new Button();
        var pencil = new javafx.scene.shape.SVGPath();
        pencil.setContent("M 4 16.5 L 16.6 3.9 L 20.1 7.4 L 7.5 20 H 4 Z M 14.8 5.7 L 18.3 9.2");
        pencil.getStyleClass().add("profile-edit-glyph");
        edit.setGraphic(pencil);
        edit.setAccessibleText("Edit profile");
        edit.setAccessibleHelp("Opens the profile editor");
        edit.setTooltip(new Tooltip("Edit profile"));
        edit.getStyleClass().add("profile-edit-button");
        edit.setOnAction(e -> showProfileEditor(target, profile));
        var details = new GridPane();
        details.setHgap(34);
        details.setVgap(7);
        var leftColumn = new ColumnConstraints();
        leftColumn.setPercentWidth(50);
        leftColumn.setHgrow(Priority.ALWAYS);
        leftColumn.setMinWidth(0);
        var rightColumn = new ColumnConstraints();
        rightColumn.setPercentWidth(50);
        rightColumn.setHgrow(Priority.ALWAYS);
        rightColumn.setMinWidth(0);
        details.getColumnConstraints().setAll(leftColumn, rightColumn);
        details.add(profileDatum("Degree:", profile.degree()), 0, 0);
        details.add(profileDatum("Reg. No.:", profile.registrationNumber()), 1, 0);
        details.add(profileDatum("Branch:", profile.branch()), 0, 1);
        details.add(profileDatum("Semester:", profile.semester()), 1, 1);
        details.add(profileDatum("Section:", profile.section()), 0, 2);
        details.add(profileDatum("Batch:", profile.batch()), 1, 2);
        details.add(profileDatum("Hostel Name:", profile.hostelName()), 0, 3);
        details.add(profileDatum("Room No.:", profile.roomNumber()), 1, 3);
        for (var datum : details.getChildren()) {
            var row = (HBox) datum;
            row.setMinWidth(0);
            row.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(row, Priority.ALWAYS);
        }
        var contacts = new VBox(9, profileContact("phone", profile.phone()), profileContact("email", currentUser.email()), profileContact("gender", profile.gender()));
        var name = styled(profile.fullName().toUpperCase(Locale.ROOT), "profile-name");
        name.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(name, Priority.ALWAYS);
        var header = new HBox(16, name, edit);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(Double.MAX_VALUE);
        var content = new VBox(13, header, details, contacts);
        content.getStyleClass().add("profile-card-content");
        var card = new StackPane();
        card.getStyleClass().add("profile-card-frame");
        var imageStream = getClass().getResourceAsStream("/images/profile-card-atmosphere.png");
        if (imageStream != null) {
            var background = new ImageView(new Image(imageStream));
            background.setManaged(false);
            background.setPreserveRatio(true);
            var imageClip = new javafx.scene.shape.Rectangle();
            imageClip.widthProperty().bind(card.widthProperty());
            imageClip.heightProperty().bind(card.heightProperty());
            imageClip.setArcWidth(10);
            imageClip.setArcHeight(10);
            background.setClip(imageClip);
            card.widthProperty().addListener((obs, oldValue, newValue) -> fitCover(background, background.getImage(), card));
            card.heightProperty().addListener((obs, oldValue, newValue) -> fitCover(background, background.getImage(), card));
            card.getChildren().add(background);
        }
        card.getChildren().add(content);
        target.getChildren().setAll(view("Your campus identity", card));
    }

    private void showProfileEditor(StackPane target, UserProfile existing) {
        var name = new TextField(existing == null ? "" : existing.fullName());
        var degree = new TextField(existing == null ? "" : existing.degree());
        var branch = new TextField(existing == null ? "" : existing.branch());
        var registration = new TextField(existing == null ? "" : existing.registrationNumber());
        ComboBox<String> semester = selectControl();
        semester.getItems().addAll("I", "II", "III", "IV", "V", "VI", "VII", "VIII");
        semester.setValue(existing == null ? null : existing.semester());
        var batch = new TextField(existing == null ? "" : existing.batch());
        var section = new TextField(existing == null ? "" : existing.section());
        var hostel = new TextField(existing == null ? "" : existing.hostelName());
        var room = new TextField(existing == null ? "" : existing.roomNumber());
        var phone = new TextField(existing == null ? "" : existing.phone());
        ComboBox<String> gender = selectControl();
        gender.getItems().addAll("Male", "Female", "Other", "Prefer not to say");
        gender.setValue(existing == null ? null : existing.gender());
        var feedback = feedback();
        var save = primaryButton("Save profile");
        var cancel = secondaryButton("Cancel");

        save.setOnAction(event -> runTask(save, feedback, "Saving…", () -> {
            var profile = new UserProfile(currentUser.id(), name.getText(), degree.getText(),
                    branch.getText(), registration.getText(), semester.getValue(), batch.getText(),
                    section.getText(), hostel.getText(), room.getText(), phone.getText(),
                    gender.getValue());
            profileService.save(profile);
            return profileService.find(currentUser.id()).orElseThrow();
        }, saved -> showProfileCard(target, saved)));
        cancel.setOnAction(event -> {
            if (existing == null) {
                showProfile(target);
            } else {
                showProfileCard(target, existing);
            }
        });

        var fields = new VBox(9,
                field("University email", styled(currentUser.email(), "body")),
                field("Full name *", name), field("Degree *", degree), field("Branch *", branch),
                field("Registration number *", registration), field("Semester *", semester),
                field("Batch *", batch), field("Section *", section), field("Hostel name *", hostel),
                field("Room number *", room), field("Phone (10 digits) *", phone),
                field("Gender *", gender), new HBox(8, save, cancel), feedback);
        var scroll = new ScrollPane(fields);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(620);
        target.getChildren().setAll(view("Update your campus identity", scroll));
    }
    private void showAdminClaims(StackPane target) { var box = new VBox(10); target.getChildren().setAll(view("Protect fair ownership decisions", box)); runTask(null, new Label(), "Loading…", claimService::pending, claims -> renderAdminClaims(box, claims)); }
    private void showAllReports(StackPane target) { var box = new VBox(10); target.getChildren().setAll(view("Keep the recovery network accurate", box)); runTask(null, new Label(), "Loading…", () -> itemService.search("", "All categories", null, null, true), items -> renderAdminItems(box, items)); }
    private void showUsers(StackPane target) { var box = new VBox(10); target.getChildren().setAll(view("Keep accounts safe", box)); runTask(null, new Label(), "Loading…", adminService::users, users -> renderUsers(box, users)); }
    private void renderItems(VBox box, List<Item> items, boolean canClaim) { box.getChildren().clear(); if (items.isEmpty()) { box.getChildren().add(styled("No matching reports yet.", "empty")); return; } for (Item item : items) { var claim = primaryButton("Submit private claim"); claim.setOnAction(e -> claimDialog(item)); if (item.reporterId() == currentUser.id() || item.status() != ItemStatus.OPEN) claim.setDisable(true); box.getChildren().add(card(styled(item.reportType() + "  •  " + item.status(), "status"), styled(item.title(), "card-title"), styled(item.category() + "  •  " + item.location() + "  •  " + item.eventDate(), "body"), styled(item.description(), "body"), canClaim ? claim : new Label())); } }
    private void renderClaimsWithHandoff(VBox box, List<Claim> claims) { box.getChildren().clear(); if (claims.isEmpty()) { box.getChildren().add(styled("No claims to show.", "empty")); return; } for (Claim c : claims) { var handoff = new VBox(6); if (c.status() == ClaimStatus.APPROVED) { handoff.getChildren().add(styled("Loading approved handover contact…", "body")); runTask(null, new Label(), "Loading…", () -> itemService.find(c.itemId()), item -> { handoff.getChildren().clear(); if (item.isEmpty()) { handoff.getChildren().add(styled("Report contact unavailable.", "error")); return; } profileService.find(item.get().reporterId()).ifPresentOrElse(p -> handoff.getChildren().addAll(styled("HANDOVER CONTACT", "eyebrow"), styled(p.fullName(), "card-title"), styled("Email: " + item.get().reporterEmail(), "body"), styled("Phone: " + p.phone(), "body"), styled("University details: " + p.universityDetails(), "body")), () -> handoff.getChildren().add(styled("Reporter has not completed a contact profile yet.", "body"))); }); } box.getChildren().add(card(styled(c.status().name(), "status"), styled("Claim #" + c.id() + " • " + c.itemTitle(), "card-title"), styled(c.status() == ClaimStatus.APPROVED ? "Approved. Use the handover contact below." : c.status() == ClaimStatus.PENDING ? "Awaiting administrator review" : c.reviewReason(), "body"), handoff)); } }
    private void renderAdminClaims(VBox box, List<Claim> claims) { box.getChildren().clear(); if (claims.isEmpty()) { box.getChildren().add(styled("No pending claims. The queue is clear.", "empty")); return; } for (Claim c : claims) renderComparisonCard(box, c, true); }
    private void renderComparisonCard(VBox box, Claim c, boolean actionable) { var review = new VBox(8); review.getChildren().add(styled("Loading original report…", "body")); box.getChildren().add(card(styled(c.status().name() + " • CLAIM #" + c.id(), "status"), styled(c.itemTitle() + " • claimant: " + c.claimantEmail(), "card-title"), review)); runTask(null, new Label(), "Loading…", () -> itemService.find(c.itemId()), item -> { review.getChildren().clear(); if (item.isEmpty()) { review.getChildren().add(styled("Original report unavailable.", "error")); return; } var reported = item.get(); review.getChildren().addAll(styled("ORIGINAL REPORT", "eyebrow"), styled(reported.reportType() + " • " + reported.category() + " • " + reported.location() + " • " + reported.eventDate(), "body"), styled("Description: " + reported.description(), "body"), styled("Identifying marks: " + reported.identifyingMarks(), "body"), styled("Reporter contact: " + reported.contactDetails(), "body"), styled("CLAIM EVIDENCE", "eyebrow"), styled("Ownership: " + c.ownershipExplanation(), "body"), styled("Claim identifiers: " + c.identifyingDetails(), "body"), styled("Possession: " + c.possessionInformation(), "body"), styled("Notes: " + c.additionalNotes(), "body")); if (actionable) { var approve = primaryButton("Approve after comparison"); var reject = secondaryButton("Reject after comparison"); approve.setOnAction(e -> reviewClaim(c, true, box)); reject.setOnAction(e -> reviewClaim(c, false, box)); review.getChildren().add(new HBox(8, approve, reject)); } }); }
    private void renderClaimResults(VBox box, List<Claim> claims) { box.getChildren().clear(); if (claims.isEmpty()) { box.getChildren().add(styled("No claims have been submitted.", "empty")); return; } for (Claim c : claims) box.getChildren().add(card(styled(c.status().name(), "status"), styled("Claim #" + c.id() + " • " + c.itemTitle(), "card-title"), styled("Claimant: " + c.claimantEmail(), "body"), styled(c.reviewReason() == null || c.reviewReason().isBlank() ? "No decision note recorded." : c.reviewReason(), "body"))); }
    private void renderAdminItems(VBox box, List<Item> items) { box.getChildren().clear(); for (Item i : items) { var actions = new HBox(8); var archive = secondaryButton("Archive"); archive.setOnAction(e -> runTask(archive, new Label(), "Archiving…", () -> { itemService.archive(i.id()); return true; }, ok -> showAllReports((StackPane) box.getParent().getParent()))); actions.getChildren().add(archive); if (i.status() == ItemStatus.CLAIM_APPROVED) { var returned = primaryButton("Mark returned"); returned.setOnAction(e -> runTask(returned, new Label(), "Updating…", () -> { itemService.transition(i, ItemStatus.RETURNED); return true; }, ok -> showAllReports((StackPane) box.getParent().getParent()))); actions.getChildren().add(returned); } else if (i.status() == ItemStatus.RETURNED) { var closed = primaryButton("Close record"); closed.setOnAction(e -> runTask(closed, new Label(), "Updating…", () -> { itemService.transition(i, ItemStatus.CLOSED); return true; }, ok -> showAllReports((StackPane) box.getParent().getParent()))); actions.getChildren().add(closed); } box.getChildren().add(card(styled(i.status().name(), "status"), styled("#" + i.id() + "  " + i.title(), "card-title"), styled(i.reportType() + " • " + i.category() + " • " + i.location() + " • reporter " + i.reporterEmail(), "body"), actions)); } }
    private void renderUsers(VBox box, List<User> users) { box.getChildren().clear(); for (User user : users) { var toggle = secondaryButton(user.active() ? "Deactivate" : "Reactivate"); toggle.setDisable(user.id() == currentUser.id()); toggle.setOnAction(e -> runTask(toggle, new Label(), "Updating…", () -> { adminService.setActive(user.id(), !user.active()); return true; }, ok -> showUsers((StackPane) box.getParent().getParent()))); box.getChildren().add(card(styled(user.role().name(), "status"), styled(user.email(), "card-title"), styled(user.active() ? "Active account" : "Deactivated account", "body"), toggle)); } }
    private void claimDialog(Item item) { var explanation = new TextArea(); var details = new TextArea(); var possession = new TextArea(); var notes = new TextArea(); explanation.setPromptText("Describe private facts that prove ownership."); details.setPromptText("Include marks, contents, serial details, or other private identifiers."); possession.setPromptText("When and how did you buy or possess it?"); notes.setPromptText("Anything else the administrator should consider."); for (var area : List.of(explanation, details, possession, notes)) area.setPrefRowCount(3); var dialog = new Dialog<ButtonType>(); dialog.setTitle("Private ownership claim"); dialog.setHeaderText("Evidence is visible only to you and administrators."); dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm()); dialog.getDialogPane().getStyleClass().add("claim-dialog"); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK); var form = new VBox(10, label("Why is this yours? *"), explanation, label("Private identifying details *"), details, label("Approximate purchase or possession information *"), possession, label("Additional notes"), notes); var scroll = new ScrollPane(form); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(500); dialog.getDialogPane().setContent(scroll); dialog.getDialogPane().setPrefWidth(650); dialog.showAndWait().filter(button -> button == ButtonType.OK).ifPresent(button -> { try { claimService.submit(new Claim(0, item.id(), item.title(), currentUser.id(), currentUser.email(), explanation.getText(), details.getText(), possession.getText(), notes.getText(), null, ClaimStatus.PENDING, null, null)); showInfo("Claim submitted", "Your private claim was sent to an administrator."); } catch (RuntimeException ex) { showError(messageFor(ex)); } }); }
    private void reviewClaim(Claim claim, boolean approve, VBox box) { var reason = new TextInputDialog(); reason.setTitle(approve ? "Approve claim" : "Reject claim"); reason.setHeaderText("Add a short decision note"); reason.showAndWait().ifPresent(text -> runTask(null, new Label(), "Saving…", () -> { if (approve) claimService.approve(claim.id(), currentUser.id(), text); else claimService.reject(claim.id(), currentUser.id(), text); return true; }, ok -> showAdminClaims((StackPane) box.getParent().getParent()))); }
    private List<String> copyImages(List<Path> source) throws IOException { var destination = Path.of(System.getProperty("user.home"), ".campus-lost-found", "images"); Files.createDirectories(destination); var stored = new ArrayList<String>(); for (Path file : source) { Path target = destination.resolve(System.currentTimeMillis() + "-" + file.getFileName()); Files.copy(file, target); stored.add(target.toString()); } return stored; }
    private void addNav(VBox sidebar, String text, String icon, Runnable action) { var button = sidebarButton(text, icon); button.setOnAction(e -> action.run()); sidebar.getChildren().add(button); }
    private <T> void runTask(Button source, Label feedback, String working,
                             Callable<T> action, Consumer<T> success) {
        if (source != null) {
            source.setDisable(true);
        }
        if (feedback != null) {
            feedback.setText(working);
        }

        var task = new Task<T>() {
            @Override
            protected T call() throws Exception {
                return action.call();
            }
        };
        task.setOnSucceeded(event -> {
            if (source != null) {
                source.setDisable(false);
            }
            success.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            if (source != null) {
                source.setDisable(false);
            }
            String message = messageFor(task.getException());
            if (feedback != null) {
                error(feedback, message);
            } else {
                showError(message);
            }
        });

        var worker = new Thread(task, "campus-db-task");
        worker.setDaemon(true);
        worker.start();
    }

    private String messageFor(Throwable cause) {
        if (cause instanceof ValidationException || cause instanceof AuthenticationException) {
            return cause.getMessage();
        }

        Throwable current = cause;
        while (current != null) {
            if (current instanceof java.sql.SQLException sql) {
                if (sql.getMessage() != null && sql.getMessage().contains("Access denied")) {
                    return "Database authentication failed. Check campus_app credentials.";
                }
                return "Database unavailable. Check that MySQL84 is running.";
            }
            current = current.getCause();
        }

        return cause == null ? "Something went wrong." : String.valueOf(cause.getMessage());
    }
    private void showScene(Node content) { var root = new StackPane(content); root.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm()); stage.setScene(new Scene(root, 1180, 760)); }
    private VBox view(String title, Node... nodes) { var box = new VBox(16, styled(title, "headline")); var titleGap = new Region(); titleGap.setMinHeight(8); titleGap.setPrefHeight(8); titleGap.setMaxHeight(8); box.getChildren().add(titleGap); box.getChildren().addAll(nodes); box.getStyleClass().add("page"); box.setPrefWidth(1080); box.setMaxWidth(1180); box.setMaxHeight(Double.MAX_VALUE); StackPane.setAlignment(box, Pos.TOP_LEFT); return box; }
    private VBox authCard(String title, Node... nodes) { var titleGap = new Region(); titleGap.setMinHeight(8); titleGap.setPrefHeight(8); titleGap.setMaxHeight(8); var box = new VBox(12, styled(title, "headline"), titleGap); box.getChildren().addAll(nodes); box.getStyleClass().add("card"); box.setMaxWidth(560); return box; }
    private Node loginLayout(Node form) {
        var stream = getClass().getResourceAsStream("/images/login-campus.png");
        var image = stream == null ? null : new Image(stream);
        var imageView = image == null ? new ImageView() : new ImageView(image);
        imageView.setManaged(false);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        var scrim = new Region();
        scrim.getStyleClass().add("auth-scrim");
        var brandMark = logo(430, 215, "auth-logo");
        var visual = new StackPane(imageView, scrim, brandMark);
        visual.getStyleClass().add("auth-visual");
        visual.setMinWidth(0);
        var visualClip = new javafx.scene.shape.SVGPath();
        Runnable shapeHolder = () -> {
            var w = visual.getWidth(); var h = visual.getHeight(); var r = Math.min(Math.min(w * 0.22, h * 0.30), 300);
            visualClip.setContent(String.format(Locale.ROOT, "M 0 0 H %.1f V %.1f C %.1f %.1f %.1f %.1f %.1f %.1f H 0 Z", w, h-r, w, h-r*0.45, w-r*0.45, h, w-r, h));
        };
        visual.widthProperty().addListener((obs, oldValue, newValue) -> shapeHolder.run());
        visual.heightProperty().addListener((obs, oldValue, newValue) -> shapeHolder.run());
        visual.setClip(visualClip);
        Runnable cover = () -> fitCover(imageView, image, visual);
        visual.widthProperty().addListener((obs, oldValue, newValue) -> cover.run());
        visual.heightProperty().addListener((obs, oldValue, newValue) -> cover.run());
        scrim.prefWidthProperty().bind(visual.widthProperty());
        scrim.prefHeightProperty().bind(visual.heightProperty());
        StackPane.setAlignment(brandMark, Pos.CENTER);
        var scrollContent = new VBox(form);
        scrollContent.setAlignment(Pos.CENTER);
        scrollContent.setFillWidth(true);
        scrollContent.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        var formScroll = new ScrollPane(scrollContent);
        formScroll.setFitToWidth(true);
        formScroll.setFitToHeight(true);
        formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        formScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        formScroll.setPannable(true);
        var panel = new VBox(formScroll);
        panel.getStyleClass().add("auth-panel");
        panel.setPrefWidth(396);
        panel.setMinWidth(396);
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setFillWidth(true);
        VBox.setVgrow(formScroll, Priority.ALWAYS);
        visual.setMinSize(0, 0);
        visual.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        var shell = new Region() {
            {
                getChildren().addAll(visual, panel);
            }

            @Override protected void layoutChildren() {
                var width = getWidth();
                var height = getHeight();
                if (width >= 900) {
                    var panelWidth = Math.max(396, Math.min(640, width / 3.0));
                    var visualWidth = width - panelWidth;
                    visual.resizeRelocate(0, 0, visualWidth, height);
                    panel.resizeRelocate(visualWidth, 0, panelWidth, height);
                } else {
                    var visualHeight = Math.min(220, height * 0.30);
                    visual.resizeRelocate(0, 0, width, visualHeight);
                    panel.resizeRelocate(0, visualHeight, width, height - visualHeight);
                }
            }

            @Override protected double computePrefWidth(double height) { return 1180; }
            @Override protected double computePrefHeight(double width) { return 760; }
            @Override protected double computeMinWidth(double height) { return 396; }
            @Override protected double computeMinHeight(double width) { return 0; }
        };
        shell.getStyleClass().add("auth-shell");
        shell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return shell;
    }
    private static VBox stackedField(String name, Node node) { var field = new VBox(7, label(name), node); field.getStyleClass().add("stacked-field"); return field; }
    private static HBox field(String name, Node node) { var row = new HBox(12, label(name), node); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("form-field-row"); if (node instanceof Region region) region.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(node, Priority.ALWAYS); return row; }
    private static Label label(String text) { var label = new Label(text); label.getStyleClass().add("field-label"); return label; }
    private static Label feedback() { var label = new Label(); label.getStyleClass().add("feedback"); return label; }
    private static Label styled(String text, String style) { var label = new Label(text); label.getStyleClass().add(style); label.setWrapText(true); return label; }
    private static Button primaryButton(String text) { var button = new Button(text); button.getStyleClass().add("primary-button"); return button; }
    private static Button secondaryButton(String text) { var button = new Button(text); button.getStyleClass().add("secondary-button"); return button; }
    private static <T> ComboBox<T> selectControl() { var combo = new ComboBox<T>(); combo.getStyleClass().add("select-control"); return combo; }
    private static Button searchButton() { var button = new Button(); button.setGraphic(searchIcon()); button.setAccessibleText("Search"); button.setTooltip(new Tooltip("Search listings")); button.setMinWidth(56); button.setPrefWidth(56); button.setMinHeight(46); button.setPrefHeight(46); button.getStyleClass().add("search-submit"); return button; }
    private static HBox searchGroup(ComboBox<String> category, TextField keyword, ComboBox<ReportType> type, Button search) { var group = new HBox(0, category, separator(), keyword, separator(), type, search); group.setAlignment(Pos.CENTER_LEFT); group.setMaxWidth(Double.MAX_VALUE); group.getStyleClass().add("search-group"); category.getStyleClass().add("search-group-select"); keyword.getStyleClass().add("search-group-input"); type.getStyleClass().add("search-group-select"); HBox.setHgrow(keyword, Priority.ALWAYS); keyword.setOnAction(e -> search.fire()); return group; }
    private static Region separator() { var separator = new Region(); separator.getStyleClass().add("search-separator"); return separator; }
    private static javafx.scene.shape.SVGPath searchIcon() { var icon = new javafx.scene.shape.SVGPath(); icon.setContent("M 10 3 A 7 7 0 1 0 10 17 A 7 7 0 1 0 10 3 Z M 15 15 L 21 21"); icon.setFill(null); icon.setStroke(javafx.scene.paint.Color.WHITE); icon.setStrokeWidth(2.2); return icon; }
    private static Button sidebarButton(String text, String iconName) {
        var button = new Button(text, sidebarIcon(iconName));
        button.getStyleClass().add("nav-button");
        button.setGraphicTextGap(16);
        return button;
    }
    private static javafx.scene.shape.SVGPath sidebarIcon(String name) {
        var icon = new javafx.scene.shape.SVGPath();
        icon.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
        icon.setContent(switch (name) {
            case "profile" -> "M 12 12 A 4 4 0 1 0 12 4 A 4 4 0 1 0 12 12 Z M 4 21 C 4 16 20 16 20 21 Z";
            case "claims" -> "M 3 5 H 21 V 19 H 3 Z M 6 8 A 2 2 0 1 0 6 12 A 2 2 0 1 0 6 8 Z M 10 8 H 18 V 10 H 10 Z M 10 12 H 18 V 14 H 10 Z M 5 15 H 8 V 17 H 5 Z";
            case "logout" -> "M 4 4 H 10 V 7 H 7 V 17 H 10 V 20 H 4 Z M 10 10 H 17 V 7 L 22 12 L 17 17 V 14 H 10 Z";
            case "search" -> "M 10 3 A 7 7 0 1 0 10 17 A 7 7 0 1 0 10 3 Z M 15 15 L 21 21";
            case "report" -> "M 5 3 H 15 L 19 7 V 21 H 5 Z M 15 3 V 7 H 19 M 8 12 H 16 M 12 8 V 16";
            case "admin" -> "M 12 3 L 20 6 V 11 C 20 16 16 19 12 21 C 8 19 4 16 4 11 V 6 Z M 9 12 L 11 14 L 15 10";
            case "reports" -> "M 4 5 H 20 V 19 H 4 Z M 7 9 H 17 M 7 12 H 17 M 7 15 H 14";
            case "users" -> "M 8 11 A 3 3 0 1 0 8 5 A 3 3 0 1 0 8 11 Z M 3 19 C 3 15 13 15 13 19 M 17 11 A 2 2 0 1 0 17 7 M 15 16 C 18 14 21 16 21 19";
            default -> "M 3 3 H 21 V 21 H 3 Z";
        });
        icon.getStyleClass().add("sidebar-icon");
        return icon;
    }
    private static VBox card(Node... nodes) { var box = new VBox(8, nodes); box.getStyleClass().add("item-card"); return box; }
    private static HBox profileDatum(String labelText, String valueText) {
        var key = styled(labelText, "profile-data-label");
        key.setMinWidth(104);
        key.setPrefWidth(104);
        key.setMaxWidth(104);
        var value = styled(valueText, "profile-data-value");
        value.setMinWidth(0);
        value.setMaxWidth(Double.MAX_VALUE);
        var row = new HBox(6, key, value);
        row.setAlignment(Pos.TOP_LEFT);
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(value, Priority.ALWAYS);
        return row;
    }
    private static HBox profileContact(String kind, String value) {
        var icon = new javafx.scene.shape.SVGPath();
        icon.setContent(switch (kind) {
            case "phone" -> "M 6.6 10.8 C 8.1 13.7 10.3 15.9 13.2 17.4 L 15.4 15.2 C 15.7 14.9 16.1 14.8 16.5 15 C 17.6 15.4 18.8 15.6 20 15.6 C 20.6 15.6 21 16 21 16.6 V 20 C 21 20.6 20.6 21 20 21 C 10.6 21 3 13.4 3 4 C 3 3.4 3.4 3 4 3 H 7.4 C 8 3 8.4 3.4 8.4 4 C 8.4 5.2 8.6 6.4 9 7.5 C 9.1 7.9 9 8.3 8.8 8.6 Z";
            case "email" -> "M 3 5 H 21 V 19 H 3 Z M 3 7 L 12 13 L 21 7";
            default -> "M 12 12 A 4 4 0 1 0 12 4 A 4 4 0 1 0 12 12 Z M 4 21 C 4 16 20 16 20 21 Z";
        });
        icon.getStyleClass().add("profile-contact-glyph");
        icon.setScaleX(0.62);
        icon.setScaleY(0.62);
        var disc = new javafx.scene.shape.Circle(15);
        disc.getStyleClass().add("profile-contact-disc");
        var badge = new StackPane(disc, icon);
        badge.getStyleClass().add("profile-contact-icon");
        var text = styled(value, "profile-contact-value");
        text.setMaxWidth(Double.MAX_VALUE);
        var row = new HBox(12, badge, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);
        return row;
    }
    private Node logo() {
        return logo(195, 72, "logo-frame");
    }
    private Node logo(double width, double height, String style) {
        var asset = style.equals("logo-frame") ? "/images/srmap-logo-white.png" : "/images/srm-ap-login-logo.png";
        var stream = getClass().getResourceAsStream(asset);
        if (stream == null) return styled("SRM AP LFM", "brand");
        var image = new Image(stream);
        var view = new ImageView(image);
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        var frame = new StackPane(view);
        frame.getStyleClass().add(style);
        return frame;
    }
    private static void fitCover(ImageView imageView, Image image, Region viewport) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0 || viewport.getWidth() <= 0 || viewport.getHeight() <= 0) return;
        var iw = image.getWidth(); var ih = image.getHeight(); var ratio = viewport.getWidth() / viewport.getHeight();
        var cw = iw; var ch = ih; var x = 0.0; var y = 0.0;
        if (iw / ih > ratio) { cw = ih * ratio; x = (iw - cw) / 2; } else { ch = iw / ratio; y = (ih - ch) / 2; }
        imageView.setViewport(new javafx.geometry.Rectangle2D(x, y, cw, ch));
        imageView.setFitWidth(viewport.getWidth()); imageView.setFitHeight(viewport.getHeight()); imageView.relocate(0, 0);
    }
    private static void clear(TextInputControl... controls) { for (var control : controls) control.clear(); }
    private static void error(Label label, String message) { label.getStyleClass().add("error"); label.setText(message); }
    private static void showInfo(String title, String message) { new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait(); }
    private static void showError(String message) { new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait(); }
    private static PasswordInput passwordInput(String prompt) { return new PasswordInput(prompt); }
    private static final class PasswordInput {
        private final PasswordField hidden = new PasswordField(); private final TextField visible = new TextField(); private final StackPane view = new StackPane();
        private PasswordInput(String prompt) {
            hidden.setPromptText(prompt);
            visible.setPromptText(prompt);
            visible.setVisible(false);
            visible.setManaged(false);
            visible.textProperty().bindBidirectional(hidden.textProperty());

            var eye = new javafx.scene.shape.SVGPath();
            eye.setContent("M 2 12 C 5 6 19 6 22 12 C 19 18 5 18 2 12 Z M 12 9 A 3 3 0 1 0 12 15 A 3 3 0 1 0 12 9 Z");
            eye.getStyleClass().add("password-icon");
            var eyeOff = new javafx.scene.shape.SVGPath();
            eyeOff.setContent("M 3 3 L 21 21 M 10.6 10.6 A 3 3 0 0 0 15.4 15.4 M 2 12 C 5 6 19 6 22 12 C 19 18 5 18 2 12 Z");
            eyeOff.getStyleClass().add("password-icon");

            var peek = new Button();
            peek.getStyleClass().add("password-toggle");
            peek.setGraphic(eye);
            peek.setAccessibleText("Show password");
            peek.setTooltip(new Tooltip("Show password"));
            peek.setOnAction(e -> {
                boolean show = !visible.isVisible();
                visible.setVisible(show);
                visible.setManaged(show);
                hidden.setVisible(!show);
                hidden.setManaged(!show);
                peek.setGraphic(show ? eyeOff : eye);
                peek.setAccessibleText(show ? "Hide password" : "Show password");
                peek.setTooltip(new Tooltip(show ? "Hide password" : "Show password"));
            });
            view.getChildren().addAll(hidden, visible, peek);
            StackPane.setAlignment(peek, Pos.CENTER_RIGHT);
        }
        private String text() { return hidden.getText(); }
    }
    public static void main(String[] args) { launch(args); }
}
