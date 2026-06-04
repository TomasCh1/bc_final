package com.basketball.app.service;

import com.basketball.app.model.Attendance;
import com.basketball.app.model.User;
import com.basketball.app.repository.CategoryRepository;
import com.basketball.app.repository.EventRepository;
import com.basketball.app.repository.UserRepository;
import com.basketball.app.util.PlayerNameFormatter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.YearMonth;

@Service
public class AttendanceExportService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceExportService.class);
    /** DejaVu Sans from jasperreports-fonts */
    private static final String FONT_RESOURCE = "/net/sf/jasperreports/fonts/dejavu/DejaVuSans.ttf";

    private final AttendanceService attendanceService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;

    public AttendanceExportService(AttendanceService attendanceService, UserRepository userRepository,
                                   EventRepository eventRepository, CategoryRepository categoryRepository) {
        this.attendanceService = attendanceService;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
    }

    /** Resolve trainer ID to coach/trainer name for "Recorded By" column. */
    private String getTrainerName(Long trainerId) {
        if (trainerId == null) return "";
        return userRepository.findById(trainerId)
                .map(PlayerNameFormatter::toSurnameAndGivenName)
                .orElse("");
    }

    private static String formatPlayerNameForExport(Object playerName) {
        if (playerName == null) {
            return "";
        }
        return PlayerNameFormatter.toSurnameAndGivenName(playerName.toString());
    }

    /** Resolve category ID to category name. */
    private String getCategoryName(Long categoryId) {
        if (categoryId == null) return "";
        return categoryRepository.findById(categoryId)
                .map(c -> c.getName())
                .orElse("");
    }


    private String translateStatus(Attendance.AttendanceStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PRESENT -> "Prítomný";
            case ABSENT -> "Neprítomný";
            case LATE -> "Meškajúci";
            case EXCUSED -> "Ospravedlnený";
        };
    }

    private static final String FONT_FALLBACK = "/fonts/DejaVuSans.ttf";

    /** Create a PDF font that supports Slovak, or null if not available. */
    private PdfFont createSlovakPdfFont() {
        for (String path : new String[]{ FONT_RESOURCE, FONT_FALLBACK }) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    byte[] fontBytes = is.readAllBytes();
                    return PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                }
            } catch (Exception e) {
                log.debug("Could not load font from {}: {}", path, e.getMessage());
            }
        }
        log.warn("No font for Slovak characters found; PDF may not display ť, č, etc. correctly.");
        return null;
    }

    /**
     * Export attendance data to Excel format
     */
    public byte[] exportToExcel(Long playerId, Long eventId, Long categoryId, 
                                 LocalDate startDate, LocalDate endDate) throws IOException {
        List<Attendance> attendanceRecords = getAttendanceRecords(playerId, eventId, categoryId, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Účast");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Dátum", "Udalosť", "Kategória", "Hráč", "Stav", "Poznámky", "Zaznamenal"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Attendance attendance : attendanceRecords) {
                Row row = sheet.createRow(rowNum++);
                
                Map<String, Object> attendanceMap = attendanceService.toAttendanceMap(attendance);
                Long attEventId = attendance.getEventId();
                Long attCategoryId = eventRepository.findById(attEventId)
                        .map(e -> e.getCategoryId())
                        .orElse(null);
                
                row.createCell(0).setCellValue(attendanceMap.get("eventDate") != null ? 
                    attendanceMap.get("eventDate").toString() : "");
                row.createCell(1).setCellValue(attendanceMap.get("eventName") != null ? 
                    attendanceMap.get("eventName").toString() : "");
                row.createCell(2).setCellValue(getCategoryName(attCategoryId));
                row.createCell(3).setCellValue(formatPlayerNameForExport(attendanceMap.get("playerName")));
                row.createCell(4).setCellValue(translateStatus(attendance.getStatus()));
                row.createCell(5).setCellValue(attendance.getNotes() != null ? attendance.getNotes() : "");
                row.createCell(6).setCellValue(getTrainerName(attendance.getTrainerId()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Export attendance data to PDF format
     */
    public byte[] exportToPdf(Long playerId, Long eventId, Long categoryId, 
                              LocalDate startDate, LocalDate endDate) throws IOException {
        List<Attendance> attendanceRecords = getAttendanceRecords(playerId, eventId, categoryId, startDate, endDate);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        PdfFont slovakFont = createSlovakPdfFont();
        if (slovakFont != null) {
            document.setFont(slovakFont);
        }

        Paragraph title = new Paragraph("Prehľad účasti").setBold().setFontSize(16);
        if (slovakFont != null) {
            title.setFont(slovakFont);
        }
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3, 2, 2, 1.5f, 2, 2}))
                .useAllAvailableWidth();

        table.addHeaderCell(cell("Dátum", slovakFont));
        table.addHeaderCell(cell("Udalosť", slovakFont));
        table.addHeaderCell(cell("Kategória", slovakFont));
        table.addHeaderCell(cell("Hráč", slovakFont));
        table.addHeaderCell(cell("Stav", slovakFont));
        table.addHeaderCell(cell("Poznámky", slovakFont));
        table.addHeaderCell(cell("Zaznamenal", slovakFont));

        for (Attendance attendance : attendanceRecords) {
            Map<String, Object> attendanceMap = attendanceService.toAttendanceMap(attendance);
            Long attEventId = attendance.getEventId();
            Long attCategoryId = eventRepository.findById(attEventId)
                    .map(e -> e.getCategoryId())
                    .orElse(null);

            table.addCell(cell(attendanceMap.get("eventDate") != null ?
                    attendanceMap.get("eventDate").toString() : "", slovakFont));
            table.addCell(cell(attendanceMap.get("eventName") != null ?
                    attendanceMap.get("eventName").toString() : "", slovakFont));
            table.addCell(cell(getCategoryName(attCategoryId), slovakFont));
            table.addCell(cell(formatPlayerNameForExport(attendanceMap.get("playerName")), slovakFont));
            table.addCell(cell(translateStatus(attendance.getStatus()), slovakFont));
            table.addCell(cell(attendance.getNotes() != null ? attendance.getNotes() : "", slovakFont));
            table.addCell(cell(getTrainerName(attendance.getTrainerId()), slovakFont));
        }

        document.add(table);
        document.close();

        return outputStream.toByteArray();
    }

    private static Cell cell(String text, PdfFont font) {
        Cell cell = new Cell().add(new Paragraph(text));
        if (font != null) {
            cell.setFont(font);
        }
        return cell;
    }

    /**
     * Get attendance records based on filters.

     */
    private List<Attendance> getAttendanceRecords(Long playerId, Long eventId, Long categoryId,
                                                   LocalDate startDate, LocalDate endDate) {
        if (playerId != null) {
            if (startDate != null && endDate != null) {
                return attendanceService.findByPlayerAndDateRange(playerId, startDate, endDate);
            } else {
                return attendanceService.findByPlayer(playerId);
            }
        }
        if (eventId != null) {
            return attendanceService.findByEvent(eventId);
        }
        if (categoryId != null) {
            List<Attendance> byCategory = attendanceService.findByCategory(categoryId);
            if (startDate != null && endDate != null) {
                Set<Long> eventIdsInRange = eventRepository.findByCategoryIdAndDateBetween(categoryId, startDate, endDate)
                        .stream()
                        .map(e -> e.getId())
                        .collect(Collectors.toSet());
                return byCategory.stream()
                        .filter(a -> eventIdsInRange.contains(a.getEventId()))
                        .toList();
            }
            return byCategory;
        }
        LocalDate from = startDate;
        LocalDate to = endDate;
        if (from == null || to == null) {
            YearMonth now = YearMonth.now();
            from = from != null ? from : now.atDay(1);
            to = to != null ? to : now.atEndOfMonth();
        }
        return attendanceService.findByDateRange(from, to);
    }
}


