package com.basketball.app.service;

import com.basketball.app.model.Category;
import com.basketball.app.model.Event;
import com.basketball.app.model.EventNominatedPlayer;
import com.basketball.app.model.Statistics;
import com.basketball.app.model.User;
import com.basketball.app.repository.CategoryRepository;
import com.basketball.app.repository.EventNominatedPlayerRepository;
import com.basketball.app.repository.EventRepository;
import com.basketball.app.repository.StatisticsRepository;
import com.basketball.app.repository.UserRepository;
import com.basketball.app.util.UserNames;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MatchStatsTemplateExportService {

    private static final String TEMPLATE_PATH = "static/XLSX/2025_26 - VYHODNOTENIE HERNEJ ŠTATISTIKY.xlsx";
    private static final String STATS_SHEET_NAME = "Štatistiky";
    private static final int PLAYER_START_ROW = 15; // Excel row 16 (0-based index)
    private static final int PLAYER_END_ROW = 34;   // Excel row 35 (0-based index)

    private static final int REFEREE_NAME_COL = CellReference.convertColStringToIndex("N");
    private static final int REFEREE_GRADE_COL = CellReference.convertColStringToIndex("Y");
    private static final int MAX_REFEREES = 3;
    private static final int REFEREE_DATA_START_ROW = 51;

    private final EventRepository eventRepository;
    private final StatisticsRepository statisticsRepository;
    private final EventNominatedPlayerRepository nominatedPlayerRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRefereeService eventRefereeService;

    public MatchStatsTemplateExportService(
            EventRepository eventRepository,
            StatisticsRepository statisticsRepository,
            EventNominatedPlayerRepository nominatedPlayerRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            EventRefereeService eventRefereeService
    ) {
        this.eventRepository = eventRepository;
        this.statisticsRepository = statisticsRepository;
        this.nominatedPlayerRepository = nominatedPlayerRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.eventRefereeService = eventRefereeService;
    }

    /**
     * @param preparedByExportName already formatted as "priezvisko meno" (see {@link UserNames#toSurnameAndGivenName(User)})
     */
    public byte[] exportMatchTemplate(Long eventId, String preparedByExportName) throws IOException {
        if (eventId == null || eventId <= 0) {
            throw new RuntimeException("Event ID is required.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
        if (event.getType() != Event.EventType.MATCH) {
            throw new RuntimeException("Template export is allowed only for MATCH events.");
        }

        try (InputStream templateStream = new ClassPathResource(TEMPLATE_PATH).getInputStream();
             Workbook workbook = new XSSFWorkbook(templateStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = Optional.ofNullable(workbook.getSheet(STATS_SHEET_NAME))
                    .orElse(workbook.getSheetAt(0));

            fillHeader(sheet, event, preparedByExportName);
            fillReferees(sheet, eventId);
            fillPlayerRows(sheet, eventId);

            workbook.setForceFormulaRecalculation(true);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void fillHeader(Sheet sheet, Event event, String preparedByExportName) {
        String opponent = defaultString(event.getOpponent());
        String ownTeamName = "MBK MTF AŠK Slávia Trnava";
        boolean homeMatch = event.getIsHomeMatch() == null || Boolean.TRUE.equals(event.getIsHomeMatch());

        String homeTeam = homeMatch ? ownTeamName : opponent;
        String awayTeam = homeMatch ? opponent : ownTeamName;

        setCellText(sheet, 3, 3, homeTeam);   // D4
        setCellText(sheet, 3, 13, awayTeam);  // N4


        Integer homeScore = homeMatch ? event.getOurScore() : event.getOpponentScore();
        Integer awayScore = homeMatch ? event.getOpponentScore() : event.getOurScore();
        if (homeScore != null) {
            setCellNumber(sheet, 4, 3, homeScore);  // D5 – skóre domácich
        }
        if (awayScore != null) {
            setCellNumber(sheet, 4, 13, awayScore);  // N5 – skóre hostí
        }

        setCellDate(sheet, 7, 4, event.getDate());               // E8


        setCellText(sheet, 9, 4, defaultString(preparedByExportName));   // E10 – priezvisko meno
        setCellText(sheet, 10, 4, ownTeamName); // E11
    }


    private void fillReferees(Sheet sheet, Long eventId) {
        Map<String, Object> data = eventRefereeService.getRefereesForEvent(eventId);
        Object refObj = data.get("referees");
        if (!(refObj instanceof List<?> list) || list.isEmpty()) {
            return;
        }
        for (int i = 0; i < list.size() && i < MAX_REFEREES; i++) {
            Object item = list.get(i);
            if (!(item instanceof Map<?, ?> r)) {
                continue;
            }
            Object name = r.get("name");
            Object grade = r.get("grade");
            int rowIdx = REFEREE_DATA_START_ROW + i;
            setCellText(sheet, rowIdx, REFEREE_NAME_COL, defaultString(name != null ? String.valueOf(name) : ""));
            setCellText(sheet, rowIdx, REFEREE_GRADE_COL, defaultString(grade != null ? String.valueOf(grade) : ""));
        }
    }
    private void fillPlayerRows(Sheet sheet, Long eventId) {
        List<EventNominatedPlayer> nominations = nominatedPlayerRepository.findByEventId(eventId);
        Map<Long, EventNominatedPlayer> nominationByPlayerId = new HashMap<>();
        for (EventNominatedPlayer nomination : nominations) {
            nominationByPlayerId.put(nomination.getPlayerId(), nomination);
        }

        Map<Long, Statistics> statsByPlayerId = statisticsRepository.findByEventId(eventId).stream()
                .collect(Collectors.toMap(Statistics::getPlayerId, s -> s, (first, second) -> first));

        List<EventNominatedPlayer> startingFive = new ArrayList<>();
        List<EventNominatedPlayer> bench = new ArrayList<>();
        for (EventNominatedPlayer nomination : nominations) {
            if (Boolean.TRUE.equals(nomination.getStartingFive())) {
                startingFive.add(nomination);
            } else {
                bench.add(nomination);
            }
        }
        bench.sort(Comparator.comparing(EventNominatedPlayer::getJerseyNumber,
                Comparator.nullsLast(Integer::compareTo)));

        List<Long> playerOrder = new ArrayList<>();
        for (EventNominatedPlayer nomination : startingFive) {
            playerOrder.add(nomination.getPlayerId());
        }
        for (EventNominatedPlayer nomination : bench) {
            playerOrder.add(nomination.getPlayerId());
        }

        if (playerOrder.isEmpty()) {
            playerOrder.addAll(statsByPlayerId.keySet());
            playerOrder.sort(Long::compareTo);
        }

        Set<Long> playerIdsToLoad = new HashSet<>(playerOrder);
        playerIdsToLoad.addAll(statsByPlayerId.keySet());
        Map<Long, User> usersById = new HashMap<>();
        if (!playerIdsToLoad.isEmpty()) {
            userRepository.findAllById(playerIdsToLoad).forEach(user -> usersById.put(user.getId(), user));
        }

        int rowIdx = PLAYER_START_ROW;
        for (Long playerId : playerOrder) {
            if (rowIdx > PLAYER_END_ROW) break;
            User user = usersById.get(playerId);
            Statistics stat = statsByPlayerId.get(playerId);
            EventNominatedPlayer nomination = nominationByPlayerId.get(playerId);

            if (nomination != null && nomination.getJerseyNumber() != null) {
                setCellNumber(sheet, rowIdx, 1, nomination.getJerseyNumber()); // B
            }
            setCellText(sheet, rowIdx, 2, user != null
                    ? defaultString(UserNames.toSurnameAndGivenName(user)) : ""); // C – priezvisko meno

            setCellDouble(sheet, rowIdx, 3, stat != null && stat.getMin() != null ? Math.ceil(stat.getMin()) : 0d); // D MIN
            setCellInteger(sheet, rowIdx, 4, stat != null && stat.getTwoPMade() != null ? stat.getTwoPMade() : 0); // E 2P made
            setCellInteger(sheet, rowIdx, 5, stat != null && stat.getTwoPAttempts() != null ? stat.getTwoPAttempts() : 0); // F 2P attempts
            setCellInteger(sheet, rowIdx, 7, stat != null && stat.getThreePMade() != null ? stat.getThreePMade() : 0); // H 3P made
            setCellInteger(sheet, rowIdx, 8, stat != null && stat.getThreePAttempts() != null ? stat.getThreePAttempts() : 0); // I 3P attempts
            setCellInteger(sheet, rowIdx, 10, stat != null && stat.getFgMade() != null ? stat.getFgMade() : 0); // K FG made
            setCellInteger(sheet, rowIdx, 11, stat != null && stat.getFgAttempts() != null ? stat.getFgAttempts() : 0); // L FG attempts
            setCellInteger(sheet, rowIdx, 13, stat != null && stat.getFtMade() != null ? stat.getFtMade() : 0); // N FT made
            setCellInteger(sheet, rowIdx, 14, stat != null && stat.getFtAttempts() != null ? stat.getFtAttempts() : 0); // O FT attempts
            setCellInteger(sheet, rowIdx, 17, stat != null && stat.getOffReb() != null ? stat.getOffReb() : 0); // R OFF
            setCellInteger(sheet, rowIdx, 18, stat != null && stat.getDefReb() != null ? stat.getDefReb() : 0); // S DEF
            setCellInteger(sheet, rowIdx, 19, stat != null && stat.getAst() != null ? stat.getAst() : 0); // T AST
            setCellInteger(sheet, rowIdx, 20, stat != null && stat.getTurnovers() != null ? stat.getTurnovers() : 0); // U TO
            setCellInteger(sheet, rowIdx, 21, stat != null && stat.getStl() != null ? stat.getStl() : 0); // V STL
            setCellInteger(sheet, rowIdx, 22, stat != null && stat.getBlk() != null ? stat.getBlk() : 0); // W BLK
            setCellInteger(sheet, rowIdx, 23, stat != null && stat.getFoulsPlus() != null ? stat.getFoulsPlus() : 0); // X PF
            setCellInteger(sheet, rowIdx, 24, stat != null && stat.getFoulsMinus() != null ? stat.getFoulsMinus() : 0); // Y FLS ON
            rowIdx++;
        }
    }

    private String resolveCompetition(Long categoryId) {
        if (categoryId == null) return "";
        return categoryRepository.findById(categoryId)
                .map(Category::getName)
                .orElse("");
    }

    private void setCellText(Sheet sheet, int rowIdx, int colIdx, String value) {
        Row row = getOrCreateRow(sheet, rowIdx);
        Cell cell = getOrCreateCell(row, colIdx);
        cell.setCellValue(defaultString(value));
    }

    private void setCellNumber(Sheet sheet, int rowIdx, int colIdx, Number value) {
        if (value == null) return;
        Row row = getOrCreateRow(sheet, rowIdx);
        Cell cell = getOrCreateCell(row, colIdx);
        cell.setCellValue(value.doubleValue());
    }

    private void setCellInteger(Sheet sheet, int rowIdx, int colIdx, Integer value) {
        if (value == null) return;
        setCellNumber(sheet, rowIdx, colIdx, value);
    }

    private void setCellDouble(Sheet sheet, int rowIdx, int colIdx, Double value) {
        if (value == null) return;
        setCellNumber(sheet, rowIdx, colIdx, value);
    }

    private void setCellDate(Sheet sheet, int rowIdx, int colIdx, LocalDate value) {
        if (value == null) return;
        Row row = getOrCreateRow(sheet, rowIdx);
        Cell cell = getOrCreateCell(row, colIdx);
        cell.setCellValue(value);
    }

    private Row getOrCreateRow(Sheet sheet, int rowIdx) {
        Row row = sheet.getRow(rowIdx);
        return row != null ? row : sheet.createRow(rowIdx);
    }

    private Cell getOrCreateCell(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        return cell != null ? cell : row.createCell(colIdx);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
