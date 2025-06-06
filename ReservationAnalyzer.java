import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

class Reservation {
    int capacity;
    double price;
    LocalDate start;
    LocalDate end; // Can be null

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Reservation(int capacity, double price, LocalDate start, LocalDate end) {
        this.capacity = capacity;
        this.price = price;
        this.start = start;
        this.end = end;
    }

    // Parses a string into a LocalDate using the predefined format
    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, FORMATTER);
    }

    // Checks if the reservation overlaps with a given month
    public boolean overlaps(YearMonth ym) {
        LocalDate mStart = ym.atDay(1);
        LocalDate mEnd = ym.atEndOfMonth();
        LocalDate effectiveEnd = end != null ? end : LocalDate.MAX;
        return !start.isAfter(mEnd) && !effectiveEnd.isBefore(mStart);
    }

    // Calculates the revenue generated from this reservation during a specific month
    public double revenueForMonth(YearMonth ym) {
        LocalDate mStart = ym.atDay(1);
        LocalDate mEnd = ym.atEndOfMonth();
        LocalDate activeStart = start.isBefore(mStart) ? mStart : start;
        LocalDate activeEnd = (end == null || end.isAfter(mEnd)) ? mEnd : end;

        if (activeEnd.isBefore(activeStart)) return 0;

        long daysInMonth = ChronoUnit.DAYS.between(mStart, mEnd.plusDays(1));
        long activeDays = ChronoUnit.DAYS.between(activeStart, activeEnd.plusDays(1));

        return price * ((double) activeDays / daysInMonth);
    }
}

public class ReservationAnalyzer {

    public static List<String> readFileInList(String fileName) {
        List<String> lines = Collections.emptyList();
        try {
            lines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }


    public static List<Reservation> parseReservations(List<String> lines) {
        List<Reservation> reservations = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("Capacity")) continue;
            String[] parts = line.split(",");
            if (parts.length < 3) continue;
            try {
                int capacity = Integer.parseInt(parts[0].trim());
                double price = Double.parseDouble(parts[1].trim());
                LocalDate start = LocalDate.parse(parts[2].trim(), formatter);
                LocalDate end = null;
                if (parts.length > 3 && !parts[3].trim().isEmpty()) {
                    end = LocalDate.parse(parts[3].trim(), formatter);
                }
                reservations.add(new Reservation(capacity, price, start, end));
            } catch (Exception e) {
                System.err.println("Skipping invalid line: " + line);
            }
        }

        return reservations;
    }


    public static void analyze(List<Reservation> reservations, String monthYear) {
        YearMonth ym = YearMonth.parse(monthYear);
        double revenue = 0;
        int reservedCapacity = 0;
        Set<Reservation> used = new HashSet<>();

        for (Reservation r : reservations) {
            if (r.overlaps(ym)) {
                revenue += r.revenueForMonth(ym);
                used.add(r);
            }
        }

        int unreservedCapacity = reservations.stream()
                .filter(r -> !used.contains(r))
                .mapToInt(r -> r.capacity)
                .sum();

        System.out.printf("%s: expected revenue: $%.2f, expected total capacity of the unreserved offices: %d%n",
                monthYear, revenue, unreservedCapacity);
    }

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter the path to rent_data_.txt: ");
        String path = br.readLine();

        List<String> lines = readFileInList(path);
        List<Reservation> reservations = parseReservations(lines);

        // Required months
        analyze(reservations, "2013-01");
        analyze(reservations, "2013-06");
        analyze(reservations, "2014-03");
        analyze(reservations, "2014-09");
        analyze(reservations, "2015-07");
        analyze(reservations, "2000-01");
        analyze(reservations, "2018-01");
    }

}
