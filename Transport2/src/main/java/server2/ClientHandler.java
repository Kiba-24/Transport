package server2;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final RouteRepository repository;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.repository = new RouteRepository();
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Client connected: " + socket.getInetAddress());
            String request;
            while ((request = in.readLine()) != null) {
                System.out.println("Received request: " + request);
                String[] parts = request.split("\\|");
                if (parts.length < 2) {
                    out.println("ERROR|Invalid request format");
                    continue;
                }
                switch (parts[0]) {
                    case "SEARCH_CLOSEST":
                        handleSearchClosest(parts, out);
                        break;
                    case "SEARCH_ALL":
                        handleSearchAll(parts, out);
                        break;
                    case "BOOK":
                        handleBooking(parts, out);
                        break;
                    case "CANCEL":
                        handleCancel(parts, out);
                        break;
                    default:
                        out.println("ERROR|Unknown request");
                }
            }
        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
        }
    }

    private void handleSearchClosest(String[] params, PrintWriter out) {
        if (params.length < 5) {
            out.println("ERROR|Invalid parameters");
            return;
        }
        String departure = params[1];
        String arrival = params[2];
        LocalDateTime desiredTime = LocalDateTime.parse(params[3], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String transportType = params[4];

        List<Schedule> closestRoutes = repository.getAllSchedules().stream()
                .filter(s -> s.getRoute().getDepartureCity().getName().equalsIgnoreCase(departure))
                .filter(s -> s.getRoute().getArrivalCity().getName().equalsIgnoreCase(arrival))
                .filter(s -> transportType.equals("mix") || s.getRoute().getTransportType().equalsIgnoreCase(transportType))
                .sorted((s1, s2) -> s1.getDepartureTime().compareTo(s2.getDepartureTime()))
                .collect(Collectors.toList());

        sendSchedules(out, closestRoutes);
    }

    private void handleSearchAll(String[] params, PrintWriter out) {
        if (params.length < 4) {
            out.println("ERROR|Invalid parameters");
            return;
        }
        String departure = params[1];
        String arrival = params[2];
        String transportType = params[3];

        List<Schedule> allRoutes = repository.getAllSchedules().stream()
                .filter(s -> s.getRoute().getDepartureCity().getName().equalsIgnoreCase(departure))
                .filter(s -> s.getRoute().getArrivalCity().getName().equalsIgnoreCase(arrival))
                .filter(s -> transportType.equals("mix") || s.getRoute().getTransportType().equalsIgnoreCase(transportType))
                .collect(Collectors.toList());

        sendSchedules(out, allRoutes);
    }

    private void sendSchedules(PrintWriter out, List<Schedule> schedules) {
        for (Schedule schedule : schedules) {
            out.println(scheduleToString(schedule));
        }
        out.println("END");
    }

    private String scheduleToString(Schedule schedule) {
        return String.join("|",
                String.valueOf(schedule.getRoute().getId()),
                schedule.getRoute().getTransportType(),
                schedule.getRoute().getDepartureCity().getName(),
                schedule.getRoute().getArrivalCity().getName(),
                schedule.getDepartureTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                schedule.getArrivalTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                String.valueOf(schedule.getAvailableSeats()));
    }

    private void handleBooking(String[] params, PrintWriter out) {
        if (params.length < 2) {
            out.println("ERROR|Invalid parameters");
            return;
        }
        int routeId = Integer.parseInt(params[1]);
        Schedule schedule = repository.findScheduleByRouteId(routeId);
        if (schedule != null && schedule.getAvailableSeats() > 0) {
            schedule.setAvailableSeats(schedule.getAvailableSeats() - 1);
            repository.saveSchedules();
            out.println("SUCCESS");
        } else {
            out.println("ERROR|No available seats");
        }
    }

    private void handleCancel(String[] params, PrintWriter out) {
        if (params.length < 2) {
            out.println("ERROR|Invalid parameters");
            return;
        }
        int routeId = Integer.parseInt(params[1]);
        Schedule schedule = repository.findScheduleByRouteId(routeId);
        if (schedule != null) {
            schedule.setAvailableSeats(schedule.getAvailableSeats() + 1);
            repository.saveSchedules();
            out.println("SUCCESS");
        } else {
            out.println("ERROR|Route not found");
        }
    }
}