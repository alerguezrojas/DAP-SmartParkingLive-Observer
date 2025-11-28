package smartparking;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import smartparking.model.ParkingLot;
import smartparking.observers.MobileNotifierObserver;
import smartparking.observers.SecurityModuleObserver;
import smartparking.observers.StatisticsModuleObserver;
import smartparking.observers.WebDashboardObserver;
import smartparking.observers.WebSocketObserver;
import smartparking.service.ParkingService;

@SpringBootApplication
@EnableScheduling
public class SmartParkingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartParkingApplication.class, args);
    }

    @Bean
    public CommandLineRunner init(ParkingService parkingService, SimpMessagingTemplate messagingTemplate) {
        return args -> {
            ParkingLot parkingLot = parkingService.getParkingLot();

            // Observers for UI, notifications and metrics
            WebDashboardObserver webDashboard = new WebDashboardObserver(parkingLot);
            SecurityModuleObserver securityModule = new SecurityModuleObserver();
            StatisticsModuleObserver statisticsModule = new StatisticsModuleObserver(parkingLot);
            MobileNotifierObserver mobileNotifier = new MobileNotifierObserver(3);
            WebSocketObserver webSocketObserver = new WebSocketObserver(messagingTemplate);

            parkingLot.attachObserverToAllSpots(webDashboard);
            parkingLot.attachObserverToAllSpots(securityModule);
            parkingLot.attachObserverToAllSpots(statisticsModule);
            parkingLot.attachObserverToAllSpots(mobileNotifier);
            parkingLot.attachObserverToAllSpots(webSocketObserver);

            System.out.println("\n========================================");
            System.out.println("SmartParking Live - Servidor iniciado");
            System.out.println("========================================");
            System.out.println("Interfaz Web: http://localhost:8080");
            System.out.println("WebSocket: ws://localhost:8080/ws-parking");
            System.out.println("API REST: http://localhost:8080/api/parking");
            System.out.println("========================================\n");
        };
    }
}

