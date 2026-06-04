package com.basketball.app.config;

import com.basketball.app.model.Attendance;
import com.basketball.app.model.BehStats;
import com.basketball.app.model.Category;
import com.basketball.app.model.Event;
import com.basketball.app.model.EventNominatedPlayer;
import com.basketball.app.model.EventOpponentPlayer;
import com.basketball.app.model.EventReferee;
import com.basketball.app.model.PoslnovanieStats;
import com.basketball.app.model.Statistics;
import com.basketball.app.model.StrelbaStats;
import com.basketball.app.model.User;
import com.basketball.app.repository.AttendanceRepository;
import com.basketball.app.repository.CategoryRepository;
import com.basketball.app.repository.EventRepository;
import com.basketball.app.repository.EventNominatedPlayerRepository;
import com.basketball.app.repository.EventOpponentPlayerRepository;
import com.basketball.app.repository.EventRefereeRepository;
import com.basketball.app.repository.StatisticsRepository;
import com.basketball.app.repository.TrainingStatisticsRepository;
import com.basketball.app.repository.UserRepository;
import com.basketball.app.util.UserNames;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Creates deterministic development data on startup for local and docker profiles.
 * In prod profile, ensures a single admin account exists when configured via env vars.
 */
@Component
@Profile({"local", "docker", "prod"})
public class DataInitializer implements CommandLineRunner {
    private static final String SEASON = "2025/2026";
    private static final String DEFAULT_LOCATION = "Mestska sportova hala Trnava";
    private static final Random RANDOM = new Random(20260502L);
    private static final int PLAYERS_PER_CATEGORY = 12;
    private static final List<String> CATEGORY_NAMES = List.of("U11", "U13", "U15", "U17", "U19");

    private static final String[][] ADMIN_ACCOUNTS = {
        {"System Admin", "admin@basketball.com", "admin123"},
        {"Operations Admin", "ops.admin@basketball.com", "opsAdmin123"}
    };

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final AttendanceRepository attendanceRepository;
    private final TrainingStatisticsRepository trainingStatisticsRepository;
    private final StatisticsRepository statisticsRepository;
    private final EventNominatedPlayerRepository eventNominatedPlayerRepository;
    private final EventOpponentPlayerRepository eventOpponentPlayerRepository;
    private final EventRefereeRepository eventRefereeRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    private static final String PROD_ADMIN_NAME_KEY = "app.bootstrap.admin.name";
    private static final String PROD_ADMIN_SURNAME_KEY = "app.bootstrap.admin.surname";
    private static final String PROD_ADMIN_EMAIL_KEY = "app.bootstrap.admin.email";
    private static final String PROD_ADMIN_PASSWORD_KEY = "app.bootstrap.admin.password";

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           EventRepository eventRepository,
                           AttendanceRepository attendanceRepository,
                           TrainingStatisticsRepository trainingStatisticsRepository,
                           StatisticsRepository statisticsRepository,
                           EventNominatedPlayerRepository eventNominatedPlayerRepository,
                           EventOpponentPlayerRepository eventOpponentPlayerRepository,
                           EventRefereeRepository eventRefereeRepository,
                           PasswordEncoder passwordEncoder,
                           Environment environment) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.attendanceRepository = attendanceRepository;
        this.trainingStatisticsRepository = trainingStatisticsRepository;
        this.statisticsRepository = statisticsRepository;
        this.eventNominatedPlayerRepository = eventNominatedPlayerRepository;
        this.eventOpponentPlayerRepository = eventOpponentPlayerRepository;
        this.eventRefereeRepository = eventRefereeRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (isProdProfile()) {
            createProdAdmin();
            return;
        }

        createFixedAdmins();
        List<User> trainers = ensureTrainers();
        List<Category> categories = ensureCategories(trainers);
        Map<Long, List<User>> playersByCategory = ensurePlayers(categories);
        seedTrainings(categories);
        seedMatches(categories);
        seedTrainingAttendance(categories, playersByCategory, trainers);
        seedTrainingStatistics();
        seedMatchDetailsAndStatistics(playersByCategory);
    }

    private void createFixedAdmins() {
        for (String[] account : ADMIN_ACCOUNTS) {
            if (userRepository.existsByEmail(account[1])) {
                continue;
            }
            User admin = new User();
            UserNames.applyFullName(admin, account[0]);
            admin.setEmail(account[1]);
            admin.setPassword(passwordEncoder.encode(account[2]));
            admin.setRole(User.Role.ADMIN);
            admin.setIsActive(true);
            admin.setMustChangePassword(false);
            admin.setDeleted(false);
            userRepository.save(admin);
        }

        System.out.println("==========================================");
        System.out.println("Local seed admin accounts:");
        for (String[] account : ADMIN_ACCOUNTS) {
            System.out.printf("Email: %s | Password: %s%n", account[1], account[2]);
        }
        System.out.println("==========================================");
    }

    private boolean isProdProfile() {
        if (environment.matchesProfiles("prod")) {
            return true;
        }
        String active = environment.getProperty("spring.profiles.active", "");
        return Arrays.stream(active.split(","))
            .map(String::trim)
            .anyMatch("prod"::equalsIgnoreCase);
    }

    private void createProdAdmin() {
        String email = resolveBootstrap(PROD_ADMIN_EMAIL_KEY, "PROD_ADMIN_EMAIL");
        String password = resolveBootstrap(PROD_ADMIN_PASSWORD_KEY, "PROD_ADMIN_PASSWORD");
        String name = resolveBootstrap(PROD_ADMIN_NAME_KEY, "PROD_ADMIN_NAME", "System");
        String surname = resolveBootstrap(PROD_ADMIN_SURNAME_KEY, "PROD_ADMIN_SURNAME", "Admin");

        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            System.out.println(
                "Prod admin bootstrap skipped: set PROD_ADMIN_EMAIL and PROD_ADMIN_PASSWORD "
                    + "(or app.bootstrap.admin.email / app.bootstrap.admin.password)."
            );
            return;
        }

        email = email.trim();
        if (userRepository.existsByEmailAndDeletedFalse(email)) {
            System.out.println("Prod admin bootstrap skipped: active user already exists for " + email);
            return;
        }

        User admin = new User();
        admin.setName(UserNames.normalize(name));
        admin.setSurname(UserNames.normalize(surname));
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        admin.setIsActive(true);
        admin.setMustChangePassword(false);
        admin.setDeleted(false);
        userRepository.save(admin);

        System.out.println("Prod admin account created: " + email);
    }

    private String resolveBootstrap(String propertyKey, String legacyEnvKey) {
        return resolveBootstrap(propertyKey, legacyEnvKey, null);
    }

    private String resolveBootstrap(String propertyKey, String legacyEnvKey, String defaultValue) {
        String fromProperty = environment.getProperty(propertyKey);
        if (StringUtils.hasText(fromProperty)) {
            return fromProperty.trim();
        }
        String fromLegacyProperty = environment.getProperty(legacyEnvKey);
        if (StringUtils.hasText(fromLegacyProperty)) {
            return fromLegacyProperty.trim();
        }
        String fromLegacyEnv = System.getenv(legacyEnvKey);
        if (StringUtils.hasText(fromLegacyEnv)) {
            return fromLegacyEnv.trim();
        }
        String fromNestedEnv = System.getenv(toEnvKey(propertyKey));
        if (StringUtils.hasText(fromNestedEnv)) {
            return fromNestedEnv.trim();
        }
        return defaultValue != null ? defaultValue : "";
    }

    private static String toEnvKey(String propertyKey) {
        return propertyKey.toUpperCase(Locale.ROOT).replace('.', '_');
    }

    private List<User> ensureTrainers() {
        String[][] seedTrainers = {
            {"Head Coach Peter Novak", "coach.peter@basketball.com"},
            {"Assistant Coach Milan Hruska", "coach.milan@basketball.com"},
            {"Coach Tomas Varga", "coach.tomas@basketball.com"},
            {"Coach Martin Sladky", "coach.martin@basketball.com"},
            {"Coach Filip Cerny", "coach.filip@basketball.com"},
            {"Coach Lukas Bielik", "coach.lukas@basketball.com"}
        };

        for (String[] t : seedTrainers) {
            String name = t[0];
            String email = t[1];
            if (!userRepository.existsByEmail(email)) {
                User trainer = new User();
                UserNames.applyFullName(trainer, name);
                trainer.setEmail(email);
                trainer.setPassword(passwordEncoder.encode("trainer123"));
                trainer.setRole(User.Role.TRAINER);
                trainer.setIsActive(true);
                trainer.setMustChangePassword(false);
                trainer.setDeleted(false);
                userRepository.save(trainer);
            }
        }

        return userRepository.findByRole(User.Role.TRAINER);
    }

    private List<Category> ensureCategories(List<User> trainers) {
        int trainerIndex = 0;
        for (String name : CATEGORY_NAMES) {
            Category category = categoryRepository.findByName(name).orElse(null);
            if (category == null) {
                category = new Category();
                category.setName(name);
                category.setSeason(SEASON);
            } else if (category.getSeason() == null || category.getSeason().isBlank()) {
                category.setSeason(SEASON);
            }

            category.getCoaches().clear();
            if (!trainers.isEmpty()) {
                User primary = trainers.get(trainerIndex % trainers.size());
                User secondary = trainers.get((trainerIndex + 1) % trainers.size());
                category.setCoachId(primary.getId());
                category.getCoaches().add(primary);
                category.getCoaches().add(secondary);
                trainerIndex += 2;
            }

            categoryRepository.save(category);
        }

        return categoryRepository.findAll().stream()
            .filter(c -> CATEGORY_NAMES.contains(c.getName()))
            .toList();
    }

    private Map<Long, List<User>> ensurePlayers(List<Category> categories) {
        Map<Long, List<User>> playersByCategory = new HashMap<>();
        if (categories == null || categories.isEmpty()) {
            return playersByCategory;
        }

        int globalPlayerCounter = 1;

        for (Category category : categories) {
            List<User> existingPlayersForCategory =
                userRepository.findByRoleAndCategoryInCategoriesOrCoached(User.Role.PLAYER, category.getId());

            int toCreate = Math.max(0, PLAYERS_PER_CATEGORY - existingPlayersForCategory.size());
            Set<String> existingEmails = new HashSet<>();
            for (User existing : existingPlayersForCategory) {
                existingEmails.add(existing.getEmail().toLowerCase(Locale.ROOT));
            }

            for (int i = 0; i < toCreate; i++) {
                String base = category.getName()
                    .toLowerCase(Locale.ROOT)
                    .replace(" ", "")
                    .replace("/", "");
                String email = "player." + base + "." + globalPlayerCounter + "@basketball.com";

                if (existingEmails.contains(email.toLowerCase(Locale.ROOT)) || userRepository.existsByEmail(email)) {
                    globalPlayerCounter++;
                    continue;
                }

                User player = new User();
                String[] playerNames = randomMaleNameParts(globalPlayerCounter);
                player.setName(playerNames[0]);
                player.setSurname(playerNames[1]);
                player.setEmail(email);
                player.setPassword(passwordEncoder.encode("player123"));
                player.setRole(User.Role.PLAYER);
                player.setIsActive(true);
                player.setMustChangePassword(false);
                player.setDeleted(false);

                player.setDateOfBirth(randomBirthDateForCategory(category.getName(), globalPlayerCounter));

                player.getCategories().add(category);

                userRepository.save(player);
                globalPlayerCounter++;
            }

            playersByCategory.put(
                category.getId(),
                userRepository.findByRoleAndCategoryInCategoriesOrCoached(User.Role.PLAYER, category.getId())
            );
        }

        return playersByCategory;
    }

    private LocalDate randomBirthDateForCategory(String categoryName, int playerIndex) {
        int minYear;
        int maxYear;
        String name = categoryName != null ? categoryName.trim() : "";
        switch (name) {
            case "U11" -> {
                minYear = 2013;
                maxYear = 2014;
            }
            case "U13" -> {
                minYear = 2011;
                maxYear = 2012;
            }
            case "U15" -> {
                minYear = 2009;
                maxYear = 2010;
            }
            case "U17" -> {
                minYear = 2007;
                maxYear = 2008;
            }
            case "U19" -> {
                minYear = 2005;
                maxYear = 2006;
            }
            default -> {
                minYear = 2004;
                maxYear = 2010;
            }
        }
        Random rnd = new Random(playerIndex + name.hashCode());
        int year = minYear + rnd.nextInt(maxYear - minYear + 1);
        int month = 1 + rnd.nextInt(12);
        int day = 1 + rnd.nextInt(Math.min(28, LocalDate.of(year, month, 1).lengthOfMonth()));
        return LocalDate.of(year, month, day);
    }

    private String[] randomMaleNameParts(int index) {
        String[] firstNames = {
            "Adam", "Matej", "Samuel", "Lukas", "Filip", "Marek", "Jakub", "Peter",
            "Tobias", "Daniel", "Oliver", "Patrik", "Dominik", "Richard", "Sebastian"
        };
        String[] lastNames = {
            "Novak", "Kral", "Mikula", "Holes", "Urban", "Varga", "Sklenar", "Bartos",
            "Ziak", "Pavlik", "Cerny", "Kovac", "Bielik", "Polak", "Filo"
        };
        return new String[]{
            firstNames[index % firstNames.length],
            lastNames[(index * 3) % lastNames.length]
        };
    }

    private void seedTrainings(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate rangeStart = today.minusWeeks(10).with(DayOfWeek.MONDAY);
        LocalDate rangeEnd = today.plusWeeks(6).with(DayOfWeek.SUNDAY);

        DayOfWeek[][] trainingPatterns = new DayOfWeek[][]{
            {DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY},
            {DayOfWeek.TUESDAY, DayOfWeek.THURSDAY},
            {DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY},
            {DayOfWeek.MONDAY, DayOfWeek.THURSDAY},
            {DayOfWeek.TUESDAY, DayOfWeek.SATURDAY}
        };

        for (int idx = 0; idx < categories.size(); idx++) {
            Category category = categories.get(idx);
            List<Event> existing =
                eventRepository.findByCategoryIdAndDateBetween(category.getId(), rangeStart, rangeEnd);

            Set<LocalDate> existingDates = new HashSet<>();
            for (Event e : existing) {
                if (e.getType() == Event.EventType.TRAINING) {
                    existingDates.add(e.getDate());
                }
            }

            DayOfWeek[] pattern = trainingPatterns[idx % trainingPatterns.length];

            LocalDate date = rangeStart;
            while (!date.isAfter(rangeEnd)) {
                DayOfWeek dow = date.getDayOfWeek();
                boolean isTrainingDay = dow == pattern[0] || dow == pattern[1];

                if (isTrainingDay && !existingDates.contains(date)) {
                    Event training = new Event();
                    training.setName(category.getName() + " Training");
                    training.setType(Event.EventType.TRAINING);
                    training.setDate(date);
                    LocalTime start = LocalTime.of(17 + RANDOM.nextInt(3), RANDOM.nextBoolean() ? 0 : 30);
                    training.setTime(start);
                    training.setEndTime(start.plusMinutes(90));
                    training.setLocation(DEFAULT_LOCATION);
                    training.setCategoryId(category.getId());
                    training.setCancelled(false);
                    eventRepository.save(training);
                }

                date = date.plusDays(1);
            }
        }
    }

    private void seedMatches(List<Category> categories) {
        if (categories.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        String[] referees = {"Ivan Mraz", "Jozef Liska", "Robert Filo", "Andrej Dolezal", "Pavol Hronec"};
        String[] opponents = {
            "BK Bratislava",
            "MBK Nitra",
            "SBK Sered",
            "BC Prievidza",
            "Inter Bratislava",
            "Basket Kosice",
            "Slavia Trencin"
        };

        for (int catIndex = 0; catIndex < categories.size(); catIndex++) {
            Category category = categories.get(catIndex);
            for (int i = -6; i <= 4; i++) {
                LocalDate matchDate = today.plusWeeks(i * 2L).with(DayOfWeek.SATURDAY);
                boolean exists = eventRepository.findByCategoryIdAndDateBetween(category.getId(), matchDate, matchDate)
                    .stream()
                    .anyMatch(e -> e.getType() == Event.EventType.MATCH);
                if (exists) {
                    continue;
                }
                Event match = new Event();
                match.setType(Event.EventType.MATCH);
                match.setDate(matchDate);
                match.setTime(LocalTime.of(10 + (catIndex % 4) * 2, 0));
                match.setName("League Match " + category.getName());
                match.setCategoryId(category.getId());
                match.setCancelled(false);

                boolean isHome = RANDOM.nextBoolean();
                match.setIsHomeMatch(isHome);
                match.setLocation(isHome ? DEFAULT_LOCATION : "Opponent Arena");

                String opponentName = opponents[(catIndex + Math.abs(i)) % opponents.length] + " " + category.getName();
                match.setOpponent(opponentName);

                if (!matchDate.isAfter(today)) {
                    boolean weWon = RANDOM.nextBoolean();
                    int ourScore = 58 + RANDOM.nextInt(33);
                    int oppScore = 55 + RANDOM.nextInt(30);
                    if (ourScore == oppScore) {
                        oppScore = Math.max(40, oppScore - 1);
                    }
                    if (weWon && ourScore < oppScore) {
                        ourScore = oppScore + (2 + RANDOM.nextInt(8));
                    } else if (!weWon && ourScore > oppScore) {
                        oppScore = ourScore + (2 + RANDOM.nextInt(8));
                    }
                    match.setMatchWon(ourScore > oppScore);
                    match.setOurScore(ourScore);
                    match.setOpponentScore(oppScore);
                }

                Event saved = eventRepository.save(match);
                if (eventRefereeRepository.findByEventIdOrderBySortOrderAsc(saved.getId()).isEmpty()) {
                    int refereeCount = 2 + RANDOM.nextInt(2);
                    for (int r = 0; r < refereeCount; r++) {
                        EventReferee referee = new EventReferee();
                        referee.setEventId(saved.getId());
                        referee.setSortOrder(r + 1);
                        referee.setName(referees[(catIndex + r) % referees.length]);
                        referee.setGrade(r == 0 ? "A" : "B");
                        eventRefereeRepository.save(referee);
                    }
                }
            }
        }
    }

    private void seedTrainingAttendance(List<Category> categories, Map<Long, List<User>> playersByCategory, List<User> trainers) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(4);
        LocalDate endDate = today;

        if (categories == null || categories.isEmpty()) {
            return;
        }

        for (Category category : categories) {
            List<Event> trainingsInRange = eventRepository.findByCategoryIdAndDateBetween(
                category.getId(),
                startDate,
                endDate
            );

            // Only trainings (ignore matches and other events)
            List<Event> trainings = new ArrayList<>();
            for (Event e : trainingsInRange) {
                if (e.getType() == Event.EventType.TRAINING) {
                    trainings.add(e);
                }
            }

            if (trainings.isEmpty()) {
                continue;
            }

            List<User> players = playersByCategory.getOrDefault(category.getId(), List.of());

            if (players.isEmpty()) {
                continue;
            }

            Long defaultTrainerId = trainers.isEmpty() ? null : trainers.get(0).getId();

            for (Event training : trainings) {
                for (User player : players) {
                    if (attendanceRepository.findByEventIdAndPlayerId(training.getId(), player.getId()).isPresent()) {
                        continue;
                    }

                    double roll = RANDOM.nextDouble();
                    Attendance.AttendanceStatus status;
                    if (roll < 0.74) {
                        status = Attendance.AttendanceStatus.PRESENT;
                    } else if (roll < 0.86) {
                        status = Attendance.AttendanceStatus.ABSENT;
                    } else if (roll < 0.95) {
                        status = Attendance.AttendanceStatus.LATE;
                    } else {
                        status = Attendance.AttendanceStatus.EXCUSED;
                    }

                    Attendance attendance = new Attendance();
                    attendance.setEventId(training.getId());
                    attendance.setPlayerId(player.getId());
                    attendance.setTrainerId(defaultTrainerId);
                    attendance.setStatus(status);

                    attendanceRepository.save(attendance);
                }
            }
        }
    }

    private void seedTrainingStatistics() {
        if (!trainingStatisticsRepository.findAll().isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(4);
        LocalDate endDate = today.minusDays(1);

        List<Category> categories = categoryRepository.findAll().stream()
            .filter(c -> CATEGORY_NAMES.contains(c.getName()))
            .toList();
        if (categories.isEmpty()) {
            return;
        }

        for (Category category : categories) {
            List<User> players = userRepository.findByRoleAndCategoryInCategoriesOrCoached(
                User.Role.PLAYER,
                category.getId()
            );

            if (players.isEmpty()) {
                continue;
            }

            List<Event> trainingsInRange = eventRepository.findByCategoryIdAndDateBetween(
                category.getId(),
                startDate,
                endDate
            );

            List<Event> trainings = new ArrayList<>();
            for (Event e : trainingsInRange) {
                if (e.getType() == Event.EventType.TRAINING && (e.getCancelled() == null || !e.getCancelled())) {
                    trainings.add(e);
                }
            }

            if (trainings.isEmpty()) {
                continue;
            }

            for (User player : players) {
                // For each player, pick a subset of trainings to attach stats to
                for (Event training : trainings) {
                    if (training.getDate().isAfter(endDate)) {
                        continue;
                    }

                    // Only generate stats if player was present/late (if attendance exists)
                    var attendanceOpt = attendanceRepository.findByEventIdAndPlayerId(training.getId(), player.getId());
                    if (attendanceOpt.isEmpty()) {
                        continue;
                    }
                    Attendance.AttendanceStatus status = attendanceOpt.get().getStatus();
                    if (status != Attendance.AttendanceStatus.PRESENT && status != Attendance.AttendanceStatus.LATE) {
                        continue;
                    }

                    if (RANDOM.nextDouble() > 0.32) {
                        continue;
                    }

                    int exercisesCount = 2 + RANDOM.nextInt(2);
                    for (int i = 0; i < exercisesCount; i++) {
                        int kind = RANDOM.nextInt(3);
                        switch (kind) {
                            case 0 -> {
                                PoslnovanieStats ps = new PoslnovanieStats();
                                ps.setUserId(player.getId());
                                ps.setExerciseName("Strength " + (i + 1));
                                ps.setWorkoutDate(training.getDate());
                                ps.setNote("Auto-seeded strength workout");
                                ps.setWeight(BigDecimal.valueOf(12 + RANDOM.nextInt(39)));
                                ps.setRepetitions(6 + RANDOM.nextInt(10));
                                trainingStatisticsRepository.save(ps);
                            }
                            case 1 -> {
                                BehStats bs = new BehStats();
                                bs.setUserId(player.getId());
                                bs.setExerciseName("Run " + (i + 1));
                                bs.setWorkoutDate(training.getDate());
                                bs.setNote("Auto-seeded running workout");
                                int minutes = 5 + RANDOM.nextInt(18);
                                int seconds = RANDOM.nextInt(60);
                                bs.setTime(String.format("%02d:%02d", minutes, seconds));
                                bs.setDistance(BigDecimal.valueOf(1 + RANDOM.nextInt(4)));
                                trainingStatisticsRepository.save(bs);
                            }
                            case 2 -> {
                                StrelbaStats ss = new StrelbaStats();
                                ss.setUserId(player.getId());
                                ss.setExerciseName("Shooting " + (i + 1));
                                ss.setWorkoutDate(training.getDate());
                                ss.setNote("Auto-seeded shooting practice");
                                int attempts = 20 + RANDOM.nextInt(25);
                                int made = 6 + RANDOM.nextInt(Math.max(1, attempts - 5));
                                ss.setShotsAttempted(attempts);
                                ss.setShotsMade(Math.min(made, attempts));
                                ss.setCourtSpot(RANDOM.nextBoolean() ? "Top key" : "Corner");

                                BigDecimal successRate = BigDecimal
                                    .valueOf(ss.getShotsMade())
                                    .divide(BigDecimal.valueOf(ss.getShotsAttempted()), 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .setScale(2, RoundingMode.HALF_UP);
                                ss.setSuccessRate(successRate);

                                trainingStatisticsRepository.save(ss);
                            }
                        }
                    }
                }
            }
        }
    }

    private void seedMatchDetailsAndStatistics(Map<Long, List<User>> playersByCategory) {
        LocalDate today = LocalDate.now();
        List<Event> matches = eventRepository.findByDateBetween(today.minusMonths(4), today.plusMonths(3)).stream()
            .filter(e -> e.getType() == Event.EventType.MATCH && e.getCategoryId() != null)
            .toList();

        for (Event match : matches) {
            List<User> roster = new ArrayList<>(playersByCategory.getOrDefault(match.getCategoryId(), List.of()));
            if (roster.isEmpty()) {
                continue;
            }

            List<EventNominatedPlayer> existingNominations = eventNominatedPlayerRepository.findByEventId(match.getId());
            List<EventNominatedPlayer> nominations = new ArrayList<>(existingNominations);
            if (existingNominations.isEmpty()) {
                Collections.shuffle(roster, RANDOM);
                int nominatedCount = Math.max(8, Math.min(12, roster.size()));
                for (int i = 0; i < nominatedCount; i++) {
                    User nominatedUser = roster.get(i);
                    EventNominatedPlayer nominated = new EventNominatedPlayer();
                    nominated.setEventId(match.getId());
                    nominated.setPlayerId(nominatedUser.getId());
                    nominated.setJerseyNumber(4 + i);
                    nominated.setStartingFive(i < 5);
                    nominations.add(eventNominatedPlayerRepository.save(nominated));
                }
            }

            if (eventOpponentPlayerRepository.findByEventIdOrderByJerseyNumberAsc(match.getId()).isEmpty()) {
                int opponentsCount = 8 + RANDOM.nextInt(5);
                Set<Integer> usedJerseys = new HashSet<>();
                while (usedJerseys.size() < opponentsCount) {
                    int jersey = 4 + RANDOM.nextInt(40);
                    if (!usedJerseys.add(jersey)) {
                        continue;
                    }
                    EventOpponentPlayer opponentPlayer = new EventOpponentPlayer();
                    opponentPlayer.setEventId(match.getId());
                    opponentPlayer.setJerseyNumber(jersey);
                    eventOpponentPlayerRepository.save(opponentPlayer);
                }
            }

            if (match.getDate().isAfter(today) || match.getOurScore() == null || match.getOpponentScore() == null) {
                continue;
            }
            if (!statisticsRepository.findByEventId(match.getId()).isEmpty()) {
                continue;
            }

            int ourScore = match.getOurScore();
            List<EventNominatedPlayer> participatingPlayers = new ArrayList<>();
            Set<Long> participatingPlayerIds = new HashSet<>();
            for (EventNominatedPlayer nomination : nominations) {
                if (Boolean.TRUE.equals(nomination.getStartingFive())) {
                    participatingPlayers.add(nomination);
                    participatingPlayerIds.add(nomination.getPlayerId());
                }
            }
            for (EventNominatedPlayer nomination : nominations) {
                if (participatingPlayerIds.contains(nomination.getPlayerId())) {
                    continue;
                }
                if (RANDOM.nextDouble() <= 0.82) {
                    participatingPlayers.add(nomination);
                    participatingPlayerIds.add(nomination.getPlayerId());
                }
            }
            if (participatingPlayers.isEmpty() && !nominations.isEmpty()) {
                participatingPlayers.add(nominations.get(0));
                participatingPlayerIds.add(nominations.get(0).getPlayerId());
            }
            for (EventNominatedPlayer nomination : nominations) {
                if (participatingPlayers.size() >= 5) {
                    break;
                }
                if (!participatingPlayerIds.contains(nomination.getPlayerId())) {
                    participatingPlayers.add(nomination);
                    participatingPlayerIds.add(nomination.getPlayerId());
                }
            }

            int remainingScore = ourScore;
            List<Integer> allocatedMinutes = allocateMatchMinutes(participatingPlayers);
            for (int i = 0; i < participatingPlayers.size(); i++) {
                EventNominatedPlayer nomination = participatingPlayers.get(i);
                if (!statisticsRepository.findByEventIdAndPlayerId(match.getId(), nomination.getPlayerId()).isEmpty()) {
                    continue;
                }

                int remainingSlots = participatingPlayers.size() - i;
                int maxKeepForRest = 22 * Math.max(0, remainingSlots - 1);
                int minCurrent = Math.max(0, remainingScore - maxKeepForRest);
                int maxCurrent = Math.min(22, remainingScore);
                int pts;
                if (remainingSlots == 1) {
                    pts = remainingScore;
                } else {
                    pts = minCurrent + RANDOM.nextInt(Math.max(1, maxCurrent - minCurrent + 1));
                }

                remainingScore -= pts;

                Statistics stats = new Statistics();
                stats.setEventId(match.getId());
                stats.setPlayerId(nomination.getPlayerId());
                stats.setStatType(Statistics.StatType.GAME);
                stats.setOpponent(match.getOpponent() != null ? match.getOpponent() : "Unknown opponent");
                stats.setMin((double) allocatedMinutes.get(i));
                stats.setPts(pts);

                populateScoringFields(stats, pts);
                stats.setOffReb(RANDOM.nextInt(4));
                stats.setDefReb(RANDOM.nextInt(7));
                stats.setAst(RANDOM.nextInt(7));
                stats.setStl(RANDOM.nextInt(4));
                stats.setBlk(RANDOM.nextInt(3));
                stats.setTurnovers(RANDOM.nextInt(5));
                stats.setFoulsPlus(RANDOM.nextInt(5));
                stats.setFoulsMinus(RANDOM.nextInt(4));
                stats.setStatIndex(calculateStatIndex(stats));
                statisticsRepository.save(stats);
            }
        }
    }

    private List<Integer> allocateMatchMinutes(List<EventNominatedPlayer> players) {
        if (players.isEmpty()) {
            return List.of();
        }

        final int totalMinutes = 200;
        List<Integer> allocation = new ArrayList<>(Collections.nCopies(players.size(), 0));
        List<Double> fractions = new ArrayList<>(Collections.nCopies(players.size(), 0.0));

        int totalWeight = 0;
        int[] weights = new int[players.size()];
        for (int i = 0; i < players.size(); i++) {
            int weight = Boolean.TRUE.equals(players.get(i).getStartingFive()) ? 5 : 3;
            weights[i] = weight;
            totalWeight += weight;
        }

        int allocated = 0;
        for (int i = 0; i < players.size(); i++) {
            double exact = (double) totalMinutes * weights[i] / totalWeight;
            int floor = (int) Math.floor(exact);
            allocation.set(i, floor);
            fractions.set(i, exact - floor);
            allocated += floor;
        }

        int remainder = totalMinutes - allocated;
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            indexes.add(i);
        }
        indexes.sort(Comparator.comparingDouble((Integer idx) -> fractions.get(idx)).reversed());

        for (int i = 0; i < remainder; i++) {
            int idx = indexes.get(i % indexes.size());
            allocation.set(idx, allocation.get(idx) + 1);
        }

        ensureStartingFiveHaveMinutes(players, allocation);

        return allocation;
    }

    /** Starters must have played time; bench may stay at 0 if not used. */
    private void ensureStartingFiveHaveMinutes(List<EventNominatedPlayer> players, List<Integer> allocation) {
        final int starterFloor = 8;
        for (int i = 0; i < players.size(); i++) {
            if (!Boolean.TRUE.equals(players.get(i).getStartingFive())) {
                continue;
            }
            int current = allocation.get(i);
            if (current >= starterFloor) {
                continue;
            }
            int needed = starterFloor - current;
            allocation.set(i, starterFloor);
            for (int j = 0; j < players.size() && needed > 0; j++) {
                if (Boolean.TRUE.equals(players.get(j).getStartingFive())) {
                    continue;
                }
                int benchMinutes = allocation.get(j);
                if (benchMinutes <= 0) {
                    continue;
                }
                int take = Math.min(needed, benchMinutes);
                allocation.set(j, benchMinutes - take);
                needed -= take;
            }
        }
    }

    /**
     * (PTS + OFF + DEF + AST + STL + BLK) - ((FGC - FGÚ) + (FTC - FTÚ) + TO)
     * Same formula as {@link Statistics} entity / {@link com.basketball.app.service.StatisticsService}.
     */
    private double calculateStatIndex(Statistics stats) {
        double pts = stats.getPts() != null ? stats.getPts() : 0;
        double off = stats.getOffReb() != null ? stats.getOffReb() : 0;
        double def = stats.getDefReb() != null ? stats.getDefReb() : 0;
        double ast = stats.getAst() != null ? stats.getAst() : 0;
        double stl = stats.getStl() != null ? stats.getStl() : 0;
        double blk = stats.getBlk() != null ? stats.getBlk() : 0;
        double fgc = stats.getFgAttempts() != null ? stats.getFgAttempts() : 0;
        double fgu = stats.getFgMade() != null ? stats.getFgMade() : 0;
        double ftc = stats.getFtAttempts() != null ? stats.getFtAttempts() : 0;
        double ftu = stats.getFtMade() != null ? stats.getFtMade() : 0;
        double to = stats.getTurnovers() != null ? stats.getTurnovers() : 0;
        double index = (pts + off + def + ast + stl + blk) - ((fgc - fgu) + (ftc - ftu) + to);
        return Math.round(index * 100.0) / 100.0;
    }

    private void populateScoringFields(Statistics stats, int pts) {
        int maxThrees = Math.min(4, pts / 3);
        int threePMade = maxThrees > 0 ? RANDOM.nextInt(maxThrees + 1) : 0;
        int remainingAfterThrees = pts - (threePMade * 3);
        int ftMade = remainingAfterThrees % 2;
        int twoPMade = (remainingAfterThrees - ftMade) / 2;
        int fgMade = twoPMade + threePMade;
        int twoAttempts = twoPMade + RANDOM.nextInt(4);
        int threeAttempts = threePMade + RANDOM.nextInt(4);

        stats.setTwoPMade(twoPMade);
        stats.setTwoPAttempts(twoAttempts);
        stats.setThreePMade(threePMade);
        stats.setThreePAttempts(threeAttempts);
        stats.setFgMade(fgMade);
        stats.setFgAttempts(Math.max(fgMade, twoAttempts + threeAttempts));
        stats.setFtMade(ftMade);
        stats.setFtAttempts(ftMade + RANDOM.nextInt(ftMade == 0 ? 2 : 3));
    }
}

