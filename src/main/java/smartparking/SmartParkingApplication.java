package smartparking;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import smartparking.model.ParkingLot;
import smartparking.model.SpotStatus;
import smartparking.observers.*;
import smartparking.service.ParkingService;

@SpringBootApplication
public class SmartParkingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartParkingApplication.class, args);
    }

    @Bean
    public CommandLineRunner init(ParkingService parkingService, SimpMessagingTemplate messagingTemplate) {
        return args -> {
            ParkingLot parkingLot = parkingService.getParkingLot();

            // Creamos los observadores
            WebDashboardObserver webDashboard = new WebDashboardObserver(parkingLot);
            SecurityModuleObserver securityModule = new SecurityModuleObserver();
            StatisticsModuleObserver statisticsModule = new StatisticsModuleObserver(parkingLot);
            MobileNotifierObserver mobileNotifier = new MobileNotifierObserver(3);
            WebSocketObserver webSocketObserver = new WebSocketObserver(messagingTemplate);

            // Registramos los observadores en todas las plazas
            parkingLot.attachObserverToAllSpots(webDashboard);
            parkingLot.attachObserverToAllSpots(securityModule);
            parkingLot.attachObserverToAllSpots(statisticsModule);
            parkingLot.attachObserverToAllSpots(mobileNotifier);
            parkingLot.attachObserverToAllSpots(webSocketObserver);

            System.out.println("\n========================================");
            System.out.println("🚗 SmartParking Live - Servidor iniciado");
            System.out.println("========================================");
            System.out.println("📊 Interfaz Web: http://localhost:8080");
            System.out.println("🔌 WebSocket: ws://localhost:8080/ws-parking");
            System.out.println("🌐 API REST: http://localhost:8080/api/parking");
            System.out.println("========================================\n");

            // Simulación opcional en un hilo separado
            Thread simulationThread = new Thread(() -> {
                try {
                    Thread.sleep(5000); // Esperar 5 segundos antes de comenzar
                    System.out.println("\n=== Iniciando simulación automática ===\n");

                    Thread.sleep(2000);
                    parkingLot.changeSpotStatus(1, SpotStatus.OCCUPIED);

                    Thread.sleep(2000);
                    parkingLot.changeSpotStatus(2, SpotStatus.OCCUPIED);

                    Thread.sleep(2000);
                    parkingLot.changeSpotStatus(3, SpotStatus.OCCUPIED);

                    Thread.sleep(2000);
                    parkingLot.changeSpotStatus(3, SpotStatus.FREE);

                    Thread.sleep(2000);
                    parkingLot.changeSpotStatus(4, SpotStatus.OUT_OF_SERVICE);

                    Thread.sleep(2000);
                    parkingLot.changeSpotStatus(1, SpotStatus.FREE);

                    System.out.println("\n=== Simulación completada ===\n");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            simulationThread.start();
        };
    }
}

