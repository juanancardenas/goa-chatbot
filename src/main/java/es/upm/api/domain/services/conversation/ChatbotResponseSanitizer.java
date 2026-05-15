package es.upm.api.domain.services.conversation;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ChatbotResponseSanitizer {

    public String normalizeReplyForFrontend(String reply) {
        if (reply == null || reply.isBlank()) {
            return reply;
        }

        List<String> lines = Arrays.asList(reply.split("\\R"));
        boolean hasPipes = lines.stream().anyMatch(line -> line.contains("|"));

        if (!hasPipes) {
            return reply;
        }

        StringBuilder sanitized = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();

            if (line.isBlank()) {
                if (!sanitized.isEmpty()) {
                    sanitized.append(System.lineSeparator());
                }
                continue;
            }

            if (!line.contains("|")) {
                sanitized.append(line).append(System.lineSeparator());
                continue;
            }

            String compact = line.replace(" ", "");
            if (compact.matches("[|:\\-]+")) {
                continue;
            }

            String normalizedLine = line;

            if (normalizedLine.startsWith("|")) {
                normalizedLine = normalizedLine.substring(1);
            }

            if (normalizedLine.endsWith("|")) {
                normalizedLine = normalizedLine.substring(0, normalizedLine.length() - 1);
            }

            String[] cells = Arrays.stream(normalizedLine.split("\\|"))
                    .map(String::trim)
                    .filter(cell -> !cell.isBlank())
                    .toArray(String[]::new);

            if (cells.length == 0) {
                continue;
            }

            if (cells.length == 1) {
                sanitized.append("- ").append(cells[0]).append(System.lineSeparator());
                continue;
            }

            sanitized.append("- ").append(cells[0]).append(": ");

            for (int i = 1; i < cells.length; i++) {
                if (i > 1) {
                    sanitized.append("; ");
                }

                sanitized.append(cells[i]);
            }

            sanitized.append(System.lineSeparator());
        }

        return sanitized.toString().trim();
    }
}
